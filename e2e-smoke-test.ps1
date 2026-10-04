<#
.SYNOPSIS
  Exercises Account, Driver & Vehicle, Ride Management, RabbitMQ, and Fare & Payment.

.DESCRIPTION
  Start the full stack with `docker compose up -d --build` and configure the
  ADMIN_BOOTSTRAP_* credentials in the root .env, process environment, or parameters.
#>
param(
    [string]$AccountUrl = "http://localhost:8081",
    [string]$DriverUrl = "http://localhost:3002",
    [string]$RideUrl = "http://localhost:3003",
    [string]$FareUrl = "http://localhost:3004",
    [string]$AdminUsername = $env:ADMIN_BOOTSTRAP_USERNAME,
    [string]$AdminPassword = $env:ADMIN_BOOTSTRAP_PASSWORD,
    [ValidateRange(1, 600)][int]$HealthTimeoutSeconds = 60,
    [ValidateRange(1, 600)][int]$PaymentTimeoutSeconds = 60
)

$ErrorActionPreference = "Stop"

# Parse data only: never execute .env or put its secrets into the process environment.
function Read-DotEnv([string]$path) {
    $values = @{}
    if (-not (Test-Path -LiteralPath $path)) { return $values }
    $lineNumber = 0
    foreach ($line in [System.IO.File]::ReadAllLines($path)) {
        $lineNumber++
        $entry = $line.Trim()
        if (-not $entry -or $entry.StartsWith('#')) { continue }
        if ($entry -notmatch '^(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=(.*)$') {
            throw "Invalid .env assignment at line $lineNumber. Use NAME=value."
        }
        $name = $matches[1]
        $value = $matches[2].Trim()
        if ($value.StartsWith('"') -or $value.StartsWith("'")) {
            $quote = $value.Substring(0, 1)
            $closing = $value.IndexOf($quote, 1)
            if ($closing -lt 0 -or $value.Substring($closing + 1).Trim() -notmatch '^(#.*)?$') {
                throw "Invalid quoted .env value at line $lineNumber. Use simple single or double quotes."
            }
            $value = $value.Substring(1, $closing - 1)
        } else {
            # Compose treats a whitespace-prefixed # as an inline comment.
            $value = ($value -replace '\s+#.*$', '').TrimEnd()
        }
        $values[$name] = $value
    }
    return $values
}

Write-Host '[1] Checking configuration...'
$dotenv = @{}
if ((-not $PSBoundParameters.ContainsKey('AdminUsername') -and [string]::IsNullOrWhiteSpace($AdminUsername)) -or
    (-not $PSBoundParameters.ContainsKey('AdminPassword') -and [string]::IsNullOrWhiteSpace($AdminPassword))) {
    $dotenv = Read-DotEnv (Join-Path $PSScriptRoot '.env')
}
if (-not $PSBoundParameters.ContainsKey('AdminUsername') -and [string]::IsNullOrWhiteSpace($AdminUsername)) {
    $AdminUsername = $dotenv['ADMIN_BOOTSTRAP_USERNAME']
}
if (-not $PSBoundParameters.ContainsKey('AdminPassword') -and [string]::IsNullOrWhiteSpace($AdminPassword)) {
    $AdminPassword = $dotenv['ADMIN_BOOTSTRAP_PASSWORD']
}
if ([string]::IsNullOrWhiteSpace($AdminUsername) -or [string]::IsNullOrWhiteSpace($AdminPassword)) {
    throw "Set ADMIN_BOOTSTRAP_USERNAME and ADMIN_BOOTSTRAP_PASSWORD in the root .env or process environment, or run .\e2e-smoke-test.ps1 -AdminUsername 'admin' -AdminPassword '<your-password>'. Account bootstrap also requires ADMIN_BOOTSTRAP_EMAIL and a password of at least 12 characters."
}

function Wait-Service([string]$name, [string]$url) {
    $deadline = [DateTime]::UtcNow.AddSeconds($HealthTimeoutSeconds)
    do {
        try {
            $health = Invoke-RestMethod -Uri "$($url.TrimEnd('/'))/health" -TimeoutSec 5
            if ($health.status -eq 'UP') { return }
        } catch { } # Report only the service and URL, never request credentials.
        Start-Sleep -Seconds 1
    } while ([DateTime]::UtcNow -lt $deadline)
    throw "ERROR: $name is not reachable or healthy at $url (/health) within $HealthTimeoutSeconds seconds. Inspect docker compose ps and docker compose logs."
}

Write-Host '[2] Checking Account Service...'
Wait-Service 'Account Service' $AccountUrl
Write-Host '[3] Checking Driver Service...'
Wait-Service 'Driver & Vehicle Service' $DriverUrl
Write-Host '[4] Checking Ride Service...'
Wait-Service 'Ride Management Service' $RideUrl
Write-Host '[5] Checking Fare & Payment Service...'
Wait-Service 'Fare & Payment Service' $FareUrl
Write-Host '[6] Running E2E workflow...'

function Bearer([string]$token) { return @{ Authorization = "Bearer $token" } }
function Post([string]$url, $body, $headers = @{}) {
    Invoke-RestMethod -Method Post -Uri $url -Headers $headers -ContentType "application/json" `
        -Body ($body | ConvertTo-Json -Depth 6) -TimeoutSec 10
}
function Register([string]$username, [string]$email, [string]$path, $headers = @{}) {
    Post "$AccountUrl$path" @{ username = $username; email = $email; password = "Local-Test-Password-123!" } $headers
}
function Login([string]$username, [string]$password) {
    Post "$AccountUrl/api/accounts/login" @{ username = $username; password = $password }
}

$suffix = [guid]::NewGuid().ToString("N").Substring(0, 10)
try { $admin = Login $AdminUsername $AdminPassword }
catch { throw 'Admin login failed. Use the credentials supplied to Account Service. Bootstrap does not reset an existing admin password.' }
$adminHeaders = Bearer $admin.token
$passengerName = "e2e_passenger_$suffix"
$driverName = "e2e_driver_$suffix"

Write-Host "1. Create passenger and driver accounts"
$passenger = Register $passengerName "$passengerName@localhost" "/api/accounts/register"
$driverAccount = Register $driverName "$driverName@localhost" "/api/accounts/admin/drivers" $adminHeaders
$passengerLogin = Login $passengerName "Local-Test-Password-123!"
$driverLogin = Login $driverName "Local-Test-Password-123!"
if ($passenger.role -ne "PASSENGER" -or $driverAccount.role -ne "DRIVER") {
    throw "Account roles were not created as expected."
}

Write-Host "2. Create the operational driver profile"
$profile = Post "$DriverUrl/api/drivers" @{
    accountId = $driverAccount.userId
    name = "RideLink E2E Driver"
    phone = "+94770000000"
    licenseNo = "E2E-$suffix"
    availability = "AVAILABLE"
    serviceArea = "Colombo"
    latitude = 6.9271
    longitude = 79.8612
} $adminHeaders
if ($profile.accountId -ne $driverAccount.userId -or $profile.availability -ne 'AVAILABLE') {
    throw 'Driver profile is not linked to this run account or is not AVAILABLE.'
}
$available = @(Invoke-RestMethod -Uri "$DriverUrl/api/drivers/available?lat=6.9271&lng=79.8612&radius=10" `
    -Headers $adminHeaders -TimeoutSec 10 | ForEach-Object { $_ })
if (-not ($available | Where-Object { $_.id -eq $driverAccount.userId })) {
    throw 'This run driver is missing from Driver Service available-driver discovery.'
}

Write-Host "3. Create, assign, and complete a ride"
$ride = Post "$RideUrl/api/rides" @{
    pickupLat = 6.9271
    pickupLng = 79.8612
    pickupAddress = "Colombo Fort"
    destLat = 6.9147
    destLng = 79.9729
    destAddress = "Rajagiriya"
    serviceArea = "Colombo"
} (Bearer $passengerLogin.token)
# Select this run's driver explicitly so earlier smoke runs cannot steal assignment.
$ride = Post "$RideUrl/api/rides/$($ride.id)/assign" @{ driverId = $driverAccount.userId } $adminHeaders
if ($ride.driverId -ne $driverAccount.userId) { throw 'Ride was assigned to an unexpected driver.' }
foreach ($step in "accept", "start", "complete") {
    $ride = Invoke-RestMethod -Method Post -Uri "$RideUrl/api/rides/$($ride.id)/$step" `
        -Headers (Bearer $driverLogin.token) -TimeoutSec 10
    Write-Host "  $step -> $($ride.status)"
}
if ($ride.status -ne "COMPLETED") { throw "Ride did not complete." }

Write-Host "4. Wait for outbox publish, RabbitMQ delivery, and Fare & Payment processing"
$payment = $null
$deadline = [DateTime]::UtcNow.AddSeconds($PaymentTimeoutSeconds)
do {
    $records = @(Invoke-RestMethod -Uri "$FareUrl/api/v1/payments/ride/$($ride.id)" `
        -Headers (Bearer $passengerLogin.token) -TimeoutSec 5 | ForEach-Object { $_ })
    if ($records.Count -gt 1) { throw 'Expected exactly ONE payment for the completed ride; found duplicate payments.' }
    if ($records.Count -gt 0) { $payment = $records[0] }
    if ($null -ne $payment) { break }
    Start-Sleep -Seconds 1
} while ([DateTime]::UtcNow -lt $deadline)
if ($null -eq $payment) { throw "Fare & Payment did not create a payment within $PaymentTimeoutSeconds seconds. Inspect Ride outbox publisher and Fare consumer logs, RabbitMQ queue and dead-letter queue." }
if ($payment.status -ne 'SUCCESS' -or $payment.paymentMethod -ne 'CASH' -or $payment.currency -ne 'LKR') {
    throw 'Expected a successful simulated CASH payment in LKR. Check PAYMENT_TEST_MODE and PAYMENT_TEST_OUTCOME_SUCCESS.'
}

$receipt = Invoke-RestMethod -Uri "$RideUrl/api/rides/$($ride.id)/receipt" `
    -Headers (Bearer $passengerLogin.token) -TimeoutSec 10
if ($payment.rideId -ne $ride.id -or $receipt.rideId -ne $ride.id) {
    throw "Payment or receipt is linked to the wrong ride."
}
if ($receipt.status -ne 'SUCCESS' -or $receipt.transactionRef -ne $payment.transactionRef) {
    throw 'Receipt does not match the successful payment.'
}

# Re-read after receipt retrieval to detect extra records without mutating payment state.
$records = @(Invoke-RestMethod -Uri "$FareUrl/api/v1/payments/ride/$($ride.id)" `
    -Headers (Bearer $passengerLogin.token) -TimeoutSec 5 | ForEach-Object { $_ })
if ($records.Count -ne 1 -or $records[0].id -ne $payment.id) { throw 'Expected the same single payment after receipt retrieval.' }

Write-Host "SUCCESS: Ride $($ride.id) completed; Fare & Payment recorded $($payment.status) $($payment.currency) payment; receipt is available."
