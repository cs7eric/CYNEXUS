$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "../..")
Set-Location $repoRoot

$changed = @(git diff --name-only; git ls-files --others --exclude-standard)
$rootJunk = $changed | Where-Object {
    $_ -match '^(?:_.*|\.tmp-.*|.*\.jar|restore_.*\.sql|replay.*|provider-key\.txt|.*\.log)$'
}
if ($rootJunk) {
    Write-Warning ("Repository hygiene: inspect root-level generated files: " + ($rootJunk -join ", "))
}

$versionFilesChanged = $changed | Where-Object { $_ -match '(^|/)pom\.xml$|^cynexus-ui/package\.json$' }
if ($versionFilesChanged) {
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $repoRoot "ci/verify-version-sync.ps1") -Quiet
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Maven and UI versions are not synchronized. Run the version-sync skill before finishing."
    }
}

# This hook is advisory. Full compilation and Release verification remain explicit workflows.
exit 0
