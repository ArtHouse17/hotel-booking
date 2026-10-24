$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$pidFile = Join-Path $projectRoot '.runtime\pids.json'
if (-not (Test-Path -LiteralPath $pidFile)) { Write-Host 'No processes recorded'; return }
foreach ($entry in @(Get-Content -LiteralPath $pidFile -Raw | ConvertFrom-Json)) {
    $process = Get-Process -Id $entry.pid -ErrorAction SilentlyContinue
    if ($process -and $process.ProcessName -eq 'java' -and
            $process.StartTime.ToUniversalTime() -eq ([datetime]$entry.started).ToUniversalTime()) {
        Stop-Process -Id $entry.pid
        Write-Host "Stopped $($entry.service)"
    }
}
Remove-Item -LiteralPath $pidFile
