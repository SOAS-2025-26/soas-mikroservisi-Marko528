# Salje sve Docker slike na Docker Hub nalog.
#
# Pre pokretanja je potrebno se prijaviti:  docker login
# Pokretanje iz korena projekta:            .\scripts\docker-push.ps1

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

Write-Host "Saljem slike na Docker Hub nalog '$hubUser'..." -ForegroundColor Cyan
foreach ($service in $services) {
    $tag = "$hubUser/soas-$service`:latest"
    Write-Host "  -> $tag" -ForegroundColor Gray
    docker push $tag
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Slanje slike $tag nije uspelo." -ForegroundColor Red
        Write-Host "Proveri da li si prijavljen komandom: docker login" -ForegroundColor Yellow
        exit 1
    }
}

Write-Host ""
Write-Host "Sve slike su poslate na https://hub.docker.com/u/$hubUser" -ForegroundColor Green
