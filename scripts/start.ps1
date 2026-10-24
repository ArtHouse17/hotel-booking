param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
if (-not $env:JAVA_HOME -and (Test-Path -LiteralPath 'C:\Program Files\Java\jdk-17')) {
    $env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
}
$javaExecutable = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { 'java' }
if (-not $SkipBuild) {
    & (Join-Path $projectRoot 'mvnw.cmd') package
    if ($LASTEXITCODE -ne 0) { throw 'Maven build failed' }
}
$runtimeDirectory = Join-Path $projectRoot '.runtime'
New-Item -ItemType Directory -Force -Path $runtimeDirectory | Out-Null
$pidFile = Join-Path $runtimeDirectory 'pids.json'
if (Test-Path -LiteralPath $pidFile) { throw 'Run scripts/stop.ps1 before starting again' }
$processes = @()
try {
    foreach ($service in @('discovery-server', 'hotel-service', 'booking-service', 'api-gateway')) {
        $jar = Join-Path $projectRoot "$service\target\$service-1.0.0.jar"
        $process = Start-Process -FilePath $javaExecutable -ArgumentList @('-jar', ('"' + $jar + '"')) -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput (Join-Path $runtimeDirectory "$service.out.log") `
            -RedirectStandardError (Join-Path $runtimeDirectory "$service.err.log")
        $processes += @{ service = $service; pid = $process.Id; started = $process.StartTime.ToUniversalTime().ToString('o') }
    }
} finally {
    ConvertTo-Json -InputObject @($processes) | Set-Content -LiteralPath $pidFile -Encoding UTF8
}
Write-Host 'Started. Allow 30-60 seconds for Eureka registration.'
Write-Host 'Swagger: http://localhost:8080/swagger-ui.html'
Write-Host 'Logs: .runtime/'
