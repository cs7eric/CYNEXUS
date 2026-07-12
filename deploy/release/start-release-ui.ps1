$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..\..")
$uiDir = Join-Path $repoRoot "cynexus-ui"
$pidFile = Join-Path $scriptDir "ui.pids.json"
$log = Join-Path $scriptDir "ui.log"
$errorLog = Join-Path $scriptDir "ui.error.log"

$process = Start-Process -FilePath "cmd.exe" -ArgumentList @(
    "/d", "/c", "pnpm.cmd dev -- --host 0.0.0.0 --port 5173 --strictPort"
) -WorkingDirectory $uiDir -RedirectStandardOutput $log -RedirectStandardError $errorLog -PassThru

[PSCustomObject]@{ Name = "ui"; ProcessId = $process.Id } | ConvertTo-Json | Set-Content $pidFile -Encoding UTF8
Write-Host "Cynexus release UI started: http://localhost:5173"
