<# Run one Java service on Windows against the Compose databases and RabbitMQ. #>
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('account-service', 'driver-vehicle-service', 'ride-management-service', 'fare-payment-service')]
    [string]$Service
)

$ErrorActionPreference = 'Stop'
$envPath = Join-Path $PSScriptRoot '.env'
if (-not (Test-Path -LiteralPath $envPath)) { throw 'Create the root .env before starting services.' }

# Treat .env as literal data; never execute it or print credentials.
$settings = @{}
$lineNumber = 0
foreach ($line in [System.IO.File]::ReadAllLines($envPath)) {
    $lineNumber++
    $entry = $line.Trim()
    if (-not $entry -or $entry.StartsWith('#')) { continue }
    if ($entry -notmatch '^(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=(.*)$') {
        throw "Invalid .env assignment at line $lineNumber."
    }
    $name = $matches[1]
    $value = $matches[2].Trim()
    if ($value.StartsWith('"') -or $value.StartsWith("'")) {
        $quote = $value.Substring(0, 1)
        $closing = $value.IndexOf($quote, 1)
        if ($closing -lt 0 -or $value.Substring($closing + 1).Trim() -notmatch '^(#.*)?$') {
            throw "Invalid quoted .env value at line $lineNumber."
        }
        $value = $value.Substring(1, $closing - 1)
    } else {
        $value = ($value -replace '\s+#.*$', '').TrimEnd()
    }
    $settings[$name] = $value
}
foreach ($required in @('JWT_SECRET', 'MYSQL_ROOT_PASSWORD', 'RIDE_DB_PASSWORD', 'RABBITMQ_USERNAME', 'RABBITMQ_PASSWORD')) {
    if ([string]::IsNullOrWhiteSpace($settings[$required])) { throw "Set $required in the root .env." }
}

$mysqlPort = if ($settings['MYSQL_HOST_PORT']) { $settings['MYSQL_HOST_PORT'] } else { '3306' }
foreach ($prefix in @('ACCOUNT', 'DRIVER', 'FARE')) {
    $schema = 'ridelink_' + $prefix.ToLowerInvariant() + '_db'
    $settings["${prefix}_DB_URL"] = "jdbc:mysql://localhost:${mysqlPort}/${schema}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
    $settings["${prefix}_DB_USERNAME"] = 'root'
    $settings["${prefix}_DB_PASSWORD"] = $settings['MYSQL_ROOT_PASSWORD']
}
$settings['RIDE_DB_URL'] = 'jdbc:postgresql://localhost:5432/ridelink_ride_db'
$settings['RIDE_DB_USERNAME'] = 'ridelink'
$settings['ACCOUNT_SERVICE_URL'] = 'http://localhost:8081'
$settings['DRIVER_SERVICE_URL'] = 'http://localhost:3002'
$settings['RIDE_SERVICE_URL'] = 'http://localhost:3003'
$settings['FARE_PAYMENT_SERVICE_URL'] = 'http://localhost:3004'
$settings['RABBITMQ_HOST'] = 'localhost'
$settings['RABBITMQ_PORT'] = '5672'
$ports = @{'account-service' = '8081'; 'driver-vehicle-service' = '3002'; 'ride-management-service' = '3003'; 'fare-payment-service' = '3004'}
$settings['SERVER_PORT'] = $ports[$Service]

# Avoid starting a second instance on the same port.
$servicePort = [int]$ports[$Service]
$existingHealth = $null
try {
    $existingHealth = Invoke-RestMethod -Uri "http://localhost:$servicePort/health" -TimeoutSec 2
} catch { }
if ($existingHealth.status -eq 'UP') {
    Write-Host "$Service is already healthy at http://localhost:$servicePort. Keep its existing terminal open; no second instance was started."
    return
}
$portProbe = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Any, $servicePort)
try {
    $portProbe.Start()
} catch {
    throw "Port $servicePort is unavailable. Stop the existing service with Ctrl+C in its terminal (or stop its Docker container), then retry .\run-service.ps1 $Service."
} finally {
    $portProbe.Stop()
}

$previous = @{}
Push-Location (Join-Path $PSScriptRoot $Service)
try {
    foreach ($name in $settings.Keys) {
        $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
        [Environment]::SetEnvironmentVariable($name, $settings[$name], 'Process')
    }
    Write-Host "Starting $Service on port $($ports[$Service]). Press Ctrl+C to stop."
    & .\gradlew.bat --no-daemon bootRun
    if ($LASTEXITCODE -ne 0) { throw "$Service exited with code $LASTEXITCODE." }
} finally {
    foreach ($name in $previous.Keys) {
        [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process')
    }
    Pop-Location
}
