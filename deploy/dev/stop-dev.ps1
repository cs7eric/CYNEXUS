$ErrorActionPreference = "Stop"
$pidFile = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "dev.pids.json"
if (-not (Test-Path $pidFile)) {
    Write-Host "No dev process file found."
    exit 0
}

$processes = Get-Content $pidFile -Raw | ConvertFrom-Json
foreach ($entry in @($processes)) {
    Stop-Process -Id ([int]$entry.ProcessId) -Force -ErrorAction SilentlyContinue
}
Remove-Item $pidFile -Force
Write-Host "Cynexus dev stopped."
