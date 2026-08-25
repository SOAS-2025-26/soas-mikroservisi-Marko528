# Pravi Docker slike za sve mikroservise.
#
# Pokretanje iz korena projekta:  .\scripts\docker-build.ps1

$ErrorActionPreference = 'Stop'

$hubUser = 'marko528'
$services = @(
    'naming-server',
    'users-service',
    'currency-exchange',
    'currency-conversion',
    'bank-account',
    'crypto-wallet',
    'crypto-exchange',
    'trade-service',
    'api-gateway'
)

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

Write-Host "1/2  Pravim JAR fajlove Maven-om..." -ForegroundColor Cyan
mvn clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "Maven build nije uspeo - prekidam." -ForegroundColor Red
    exit 1
}
Write-Host "     JAR fajlovi su spremni." -ForegroundColor Green

Write-Host "2/2  Pravim Docker slike..." -ForegroundColor Cyan
foreach ($service in $services) {
    $tag = "$hubUser/soas-$service`:latest"
    Write-Host "     -> $tag" -ForegroundColor Gray
    docker build -t $tag ".\$service"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Izgradnja slike $tag nije uspela - prekidam." -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "Gotovo. Napravljene slike:" -ForegroundColor Green
docker images --filter "reference=$hubUser/soas-*" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
