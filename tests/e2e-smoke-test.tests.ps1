# Offline regression checks. These mocks do not establish real E2E success.
$ErrorActionPreference = 'Stop'
$source = Join-Path (Split-Path $PSScriptRoot -Parent) 'e2e-smoke-test.ps1'
$fixture = Join-Path ([IO.Path]::GetTempPath()) ('ridelink-smoke-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $fixture | Out-Null
$scriptFile = Join-Path $fixture 'e2e-smoke-test.ps1'
$envFile = Join-Path $fixture '.env'
Copy-Item -LiteralPath $source -Destination $scriptFile
$savedUsername = $env:ADMIN_BOOTSTRAP_USERNAME
$savedPassword = $env:ADMIN_BOOTSTRAP_PASSWORD
$testState = @{ checks = 0; requests = 0; logins = 0; paymentReads = 0; mode = ''; expectedUsername = ''; expectedPassword = '' }

function Assert([bool]$condition, [string]$message) {
    if (-not $condition) { throw $message }
    $testState.checks++
}

function Invoke-RestMethod {
    param($Method, $Uri, $Headers, $ContentType, $Body, $TimeoutSec)
    $testState.requests++
    if ($Uri -match '/health$') {
        if ($testState.mode -eq 'unreachable' -and $Uri -match ':3003/') { throw 'offline' }
        return @{ status = 'UP' }
    }
    if ($Uri -match '/login$') {
        $credentials = $Body | ConvertFrom-Json
        if ($testState.logins -eq 0) {
            Assert ($credentials.username -ceq $testState.expectedUsername) 'Credential username precedence failed.'
            Assert ($credentials.password -ceq $testState.expectedPassword) 'Credential password precedence/parsing failed.'
        }
        $testState.logins++
        return @{ token = 'offline-token' }
    }
    if ($Uri -match '/register$') { return @{ role = 'PASSENGER'; userId = 'passenger-id' } }
    if ($Uri -match '/admin/drivers$') { return @{ role = 'DRIVER'; userId = 'driver-id' } }
    if ($Uri -match '/api/drivers$') { return @{ id = 1; accountId = 'driver-id'; availability = 'AVAILABLE' } }
    if ($Uri -match '/api/drivers/available\?') { return @(@{ id = 'driver-id' }) }
    if ($Uri -match '/api/rides$') { return @{ id = 'ride-id'; status = 'REQUESTED' } }
    if ($Uri -match '/assign$') {
        Assert (($Body | ConvertFrom-Json).driverId -eq 'driver-id') 'Must assign this run driver.'
        return @{ id = 'ride-id'; driverId = 'driver-id'; status = 'ASSIGNED' }
    }
    foreach ($transition in @{ accept = 'ACCEPTED'; start = 'IN_PROGRESS'; complete = 'COMPLETED' }.GetEnumerator()) {
        if ($Uri.EndsWith('/' + $transition.Key)) { return @{ id = 'ride-id'; status = $transition.Value } }
    }
    if ($Uri -match '/payments/ride/') {
        $testState.paymentReads++
        # Invoke-RestMethod emits a JSON array as a single pipeline object.
        if ($testState.mode -eq 'timeout') { return ,@() }
        if ($testState.mode -eq 'delayed' -and $testState.paymentReads -eq 1) { return ,@() }
        $payment = @{ id = 'payment-id'; rideId = 'ride-id'; status = 'SUCCESS'; paymentMethod = 'CASH'; currency = 'LKR'; transactionRef = 'tx' }
        if ($testState.mode -eq 'duplicate') { return ,@($payment, $payment) }
        if ($testState.mode -eq 'failed') { $payment.status = 'FAILED' }
        return ,@($payment)
    }
    if ($Uri -match '/receipt$') { return @{ rideId = 'ride-id'; status = 'SUCCESS'; transactionRef = 'tx' } }
    throw 'Unexpected mocked API route.'
}

function Run-Scenario($parameters = @{}, [string]$failure = '') {
    $testState.requests = 0
    $testState.logins = 0
    $testState.paymentReads = 0
    $caught = ''
    try { & $scriptFile @parameters -HealthTimeoutSeconds 1 -PaymentTimeoutSeconds 2 6>$null }
    catch { $caught = $_.Exception.Message }
    if ($failure) { Assert ($caught -like "*$failure*") 'Expected clear scenario failure was missing.' }
    else { Assert (-not $caught) ('Offline scenario failed: ' + $caught) }
}

try {
    $env:ADMIN_BOOTSTRAP_USERNAME = $null
    $env:ADMIN_BOOTSTRAP_PASSWORD = $null
    [IO.File]::WriteAllText($envFile, @'
# ignored comment

ADMIN_BOOTSTRAP_USERNAME = "fixture-admin" # comment
ADMIN_BOOTSTRAP_PASSWORD='literal=$value=#private' # comment
UNRELATED=value=with=equals
'@)
    $testState.expectedUsername = 'fixture-admin'
    $testState.expectedPassword = 'literal=$value=#private'
    $testState.mode = 'delayed'
    Run-Scenario
    Assert ($testState.paymentReads -eq 3) 'Bounded polling and final single-payment read did not execute.'

    $env:ADMIN_BOOTSTRAP_USERNAME = 'process-admin'
    $testState.expectedUsername = 'process-admin'
    $testState.mode = 'normal'
    Run-Scenario # Only the password falls back to .env.

    $env:ADMIN_BOOTSTRAP_USERNAME = 'process-admin'
    $env:ADMIN_BOOTSTRAP_PASSWORD = 'process-password'
    $testState.expectedUsername = 'process-admin'
    $testState.expectedPassword = 'process-password'
    $testState.mode = 'normal'
    Run-Scenario

    $testState.expectedUsername = 'parameter-admin'
    $testState.expectedPassword = 'parameter-password'
    Run-Scenario @{ AdminUsername = 'parameter-admin'; AdminPassword = 'parameter-password' }
    Run-Scenario @{ AdminUsername = ''; AdminPassword = '' } 'Set ADMIN_BOOTSTRAP_USERNAME'
    Assert ($testState.requests -eq 0) 'Missing credentials must fail before API requests.'

    $testState.expectedUsername = 'process-admin'
    $testState.expectedPassword = 'process-password'
    foreach ($case in @(@('duplicate', 'exactly ONE'), @('failed', 'successful simulated CASH'), @('timeout', 'within 2 seconds'), @('unreachable', 'Ride Management Service'))) {
        $testState.mode = $case[0]
        Run-Scenario @{} $case[1]
        if ($testState.mode -eq 'unreachable') { Assert ($testState.logins -eq 0) 'Unhealthy service must stop the workflow.' }
    }
    [IO.File]::WriteAllText($envFile, 'ADMIN_BOOTSTRAP_PASSWORD="unterminated')
    $testState.mode = 'normal'
    Run-Scenario
    $env:ADMIN_BOOTSTRAP_USERNAME = $null
    $env:ADMIN_BOOTSTRAP_PASSWORD = $null
    Run-Scenario @{} 'Invalid quoted .env value'
    Assert ($testState.requests -eq 0) 'Malformed dotenv must stop before API requests.'
    Remove-Item -LiteralPath $envFile
    Run-Scenario @{} 'Set ADMIN_BOOTSTRAP_USERNAME'
    Assert ($testState.requests -eq 0) 'Absent dotenv and credentials must stop before API requests.'
    Write-Host "PASS: $($testState.checks) offline smoke-script checks."
} finally {
    $env:ADMIN_BOOTSTRAP_USERNAME = $savedUsername
    $env:ADMIN_BOOTSTRAP_PASSWORD = $savedPassword
    Remove-Item -LiteralPath $scriptFile, $envFile -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $fixture -ErrorAction SilentlyContinue
}
