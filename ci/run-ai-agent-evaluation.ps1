$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

Write-Host "Running Agent workflow, capability contract, and evaluation tests..."
mvn -pl cynexus-ai/cynexus-ai-application/cynexus-ai-application-controller,cynexus-ai/cynexus-ai-infra -am test -DskipITs

$reports = @(
    (Join-Path $repoRoot "cynexus-ai/cynexus-ai-application/cynexus-ai-application-controller/target/agent-evaluation-results.json")
)
$existingReports = $reports | Where-Object { Test-Path $_ }
if (-not $existingReports) {
    throw "Agent evaluation did not produce an evaluation report."
}

foreach ($report in $existingReports) {
    $payload = Get-Content -Raw $report | ConvertFrom-Json
    if ($payload.passed -ne $payload.total) {
        throw "Agent evaluation failed: $($payload.passed)/$($payload.total) cases passed."
    }
    Write-Host "Agent evaluation $($payload.version): $($payload.passed)/$($payload.total) passed"
}
