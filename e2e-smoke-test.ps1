<#
.SYNOPSIS
  End-to-end smoke test for RideLink, replaying the exact HTTP calls that
  ride-management-service makes to Account Service and Driver & Vehicle Service.

.DESCRIPTION
  Start the services first (same RIDELINK_JWT_SECRET everywhere):
    account-service         -> http://localhost:8081
    driver-vehicle-service  -> http://localhost:3002
    ride-management-service -> http://localhost:3003   (optional: full ride flow)
    fare-payment-service    -> http://localhost:3004   (optional)

  Usage:  powershell -ExecutionPolicy Bypass -File .\e2e-smoke-test.ps1
#>
param(
    [string]$AccountUrl = "http://localhost:8081",
    [string]$DriverUrl  = "http://localhost:3002",
    [string]$RideUrl    = "http://localhost:3003"
)

$ErrorActionPreference = "Stop"
$script:failures = 0
function Pass($msg) { Write-Host "  [PASS] $msg" -ForegroundColor Green }
function Fail($msg) { Write-Host "  [FAIL] $msg" -ForegroundColor Red; $script:failures++ }
function Check($cond, $msg) { if ($cond) { Pass $msg } else { Fail $msg } }
function IsUuid($s) { $g = [guid]::Empty; return [guid]::TryParse([string]$s, [ref]$g) }
function Bearer($t) { @{ Authorization = "Bearer $t" } }
function Post($url, $body, $headers = @{}) {
    Invoke-RestMethod -Method Post -Uri $url -Headers $headers -ContentType "application/json" -Body ($body | ConvertTo-Json -Depth 5)
}
function JwtClaims($token) {
    $p = $token.Split('.')[1].Replace('-', '+').Replace('_', '/')
    switch ($p.Length % 4) { 2 { $p += '==' } 3 { $p += '=' } }
    [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($p)) | ConvertFrom-Json
}

$suffix = Get-Random -Maximum 999999

Write-Host "`n1. Account Service: register + login (ride/fare read 'userId' and 'role' from the JWT)"
$users = @{}
foreach ($role in "ADMIN", "RIDER", "DRIVER") {
    $name = "e2e_$($role.ToLower())_$suffix"
    $reg = Post "$AccountUrl/api/accounts/register" @{ username = $name; password = "Passw0rd!"; role = $role; status = "ACTIVE" }
    $login = Post "$AccountUrl/api/accounts/login" @{ username = $name; password = "Passw0rd!" }
    $claims = JwtClaims $login.token
    Check (IsUuid $reg.id) "$role registered with UUID id $($reg.id)"
    Check ($null -eq $reg.password) "$role register response does not expose password"
    Check ($claims.userId -eq $reg.id) "$role JWT userId claim = account id"
    Check ($claims.role -eq $role) "$role JWT role claim = $role"
    $users[$role] = @{ id = $reg.id; token = $login.token }
}

Write-Host "`n2. Account Service: GET /api/users/{id} (ride AccountServiceClient.validateUser)"
$p = Invoke-RestMethod "$AccountUrl/api/users/$($users.RIDER.id)" -Headers (Bearer $users.RIDER.token)
Check ($p.role -eq "PASSENGER") "RIDER account validates as PASSENGER (required by POST /api/rides)"
$d = Invoke-RestMethod "$AccountUrl/api/users/$($users.DRIVER.id)" -Headers (Bearer $users.ADMIN.token)
Check ($d.role -eq "DRIVER") "DRIVER account validates as DRIVER (required by assign/accept)"

Write-Host "`n3. Driver Service: create driver profile linked to the DRIVER account"
$profile = Post "$DriverUrl/api/drivers" @{
    accountId = $users.DRIVER.id; name = "E2E Driver"; phone = "+94770000000"
    licenseNo = "E2E-$suffix"; availability = "AVAILABLE"; serviceArea = "Colombo"
    latitude = 6.9271; longitude = 79.8612
}
Check ($profile.accountId -eq $users.DRIVER.id) "driver profile $($profile.id) stores accountId UUID"

Write-Host "`n4. Driver Service: GET /api/drivers/available?lat&lng&radius (ride DriverServiceClient.available)"
$avail = @(Invoke-RestMethod "$DriverUrl/api/drivers/available?lat=6.9275&lng=79.8615&radius=10" -Headers (Bearer $users.ADMIN.token))
$mine = $avail | Where-Object { $_.id -eq $users.DRIVER.id } | Select-Object -First 1
Check ($null -ne $mine) "new driver is returned as available"
Check ((IsUuid $mine.id) -and $null -ne $mine.lat -and $null -ne $mine.lng) "response has {id: UUID, lat, lng, serviceArea}"

Write-Host "`n5. Full ride flow through ride-management-service ($RideUrl)"
$rideUp = $false
try { $rideUp = (Invoke-RestMethod "$RideUrl/health" -TimeoutSec 3).status -eq "UP" } catch { }
if (-not $rideUp) {
    Write-Host "  [SKIP] ride-management-service is not running" -ForegroundColor Yellow
} else {
    # Explicit assignment of our driver, then nearest-driver assignment on a second ride
    $ride = Post "$RideUrl/api/rides" @{
        pickupLat = 6.9275; pickupLng = 79.8615; pickupAddress = "Galle Face"
        destLat = 6.9010; destLng = 79.8550; destAddress = "Bambalapitiya"
    } (Bearer $users.RIDER.token)
    Check ($ride.status -eq "REQUESTED") "passenger created ride $($ride.id)"
    $ride = Post "$RideUrl/api/rides/$($ride.id)/assign" @{ driverId = $users.DRIVER.id } (Bearer $users.ADMIN.token)
    Check ($ride.status -eq "ASSIGNED") "admin assigned driver (account + driver service validation passed)"
    $nearest = Post "$RideUrl/api/rides" @{
        pickupLat = 6.9275; pickupLng = 79.8615; pickupAddress = "Galle Face"
        destLat = 6.9010; destLng = 79.8550; destAddress = "Bambalapitiya"
    } (Bearer $users.RIDER.token)
    try {
        $nearest = Invoke-RestMethod -Method Post "$RideUrl/api/rides/$($nearest.id)/assign" -Headers (Bearer $users.ADMIN.token)
        Check ($nearest.status -eq "ASSIGNED") "nearest-driver assignment via Driver Service works"
    } catch { Fail "nearest-driver assignment: $($_.ErrorDetails.Message)" }
    foreach ($step in "accept", "start", "complete") {
        $ride = Invoke-RestMethod -Method Post "$RideUrl/api/rides/$($ride.id)/$step" -Headers (Bearer $users.DRIVER.token)
        Pass "driver $step -> $($ride.status)"
    }
    $receipt = Invoke-RestMethod "$RideUrl/api/rides/$($ride.id)/receipt" -Headers (Bearer $users.RIDER.token)
    Check ($null -ne $receipt) "passenger can fetch receipt"
}

Write-Host ""
if ($script:failures -eq 0) { Write-Host "ALL CHECKS PASSED" -ForegroundColor Green; exit 0 }
Write-Host "$script:failures CHECK(S) FAILED" -ForegroundColor Red; exit 1
