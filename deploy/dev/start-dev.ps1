param(
    [switch] $SkipBuild,
    [switch] $SkipUi
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..\..")
$pidFile = Join-Path $scriptDir "dev.pids.json"
Set-Location $repoRoot

# Only one application environment may use the shared data stores at a time.
& (Join-Path $repoRoot "deploy\release\stop-release.ps1")

$pom = [xml](Get-Content (Join-Path $repoRoot "pom.xml") -Raw)
$mavenVersion = [string]$pom.project.version

if (-not $SkipBuild) {
    mvn -DskipTests package
}

$java = (Get-Command java -ErrorAction Stop).Source
$services = @(
    @{ Name = "core"; Jar = "cynexus-core\cynexus-core-starter\target\cynexus-core-starter-$mavenVersion.jar" },
    @{ Name = "market"; Jar = "cynexus-market\cynexus-market-starter\target\cynexus-market-starter-$mavenVersion.jar" },
    @{ Name = "ai"; Jar = "cynexus-ai\cynexus-ai-starter\target\cynexus-ai-starter-$mavenVersion.jar" },
    @{ Name = "gateway"; Jar = "cynexus-gateway\cynexus-gateway-starter\target\cynexus-gateway-starter-$mavenVersion.jar" }
)
$started = @()
foreach ($service in $services) {
    $jar = Join-Path $repoRoot $service.Jar
    if (-not (Test-Path $jar)) { throw "Missing packaged jar: $jar" }
    $log = Join-Path $scriptDir "$($service.Name).log"
    $errorLog = Join-Path $scriptDir "$($service.Name).error.log"
    $javaCommand = '"{0}" -jar "{1}" --spring.profiles.active=dev' -f $java, $jar
    $process = Start-Process -FilePath "cmd.exe" -ArgumentList @("/d", "/c", "start", "`"$($service.Name)-dev`"", "/b", $javaCommand) -WorkingDirectory $repoRoot -RedirectStandardOutput $log -RedirectStandardError $errorLog -PassThru
    $started += [PSCustomObject]@{ Name = $service.Name; ProcessId = $process.Id }
}

if (-not $SkipUi) {
    $uiLog = Join-Path $scriptDir "ui.log"
    $uiErrorLog = Join-Path $scriptDir "ui.error.log"
    $previousBackendUrl = $env:VITE_CYNEXUS_BACKEND_URL
    $env:VITE_CYNEXUS_BACKEND_URL = "http://127.0.0.1:28000"
    $ui = Start-Process -FilePath "pnpm.cmd" -ArgumentList @("dev", "--", "--port", "5174") -WorkingDirectory (Join-Path $repoRoot "cynexus-ui") -RedirectStandardOutput $uiLog -RedirectStandardError $uiErrorLog -PassThru
    if ($null -eq $previousBackendUrl) { Remove-Item Env:VITE_CYNEXUS_BACKEND_URL -ErrorAction SilentlyContinue } else { $env:VITE_CYNEXUS_BACKEND_URL = $previousBackendUrl }
    $started += [PSCustomObject]@{ Name = "ui"; ProcessId = $ui.Id }
}

$started | ConvertTo-Json | Set-Content -Path $pidFile -Encoding UTF8
Write-Host "Cynexus dev started. UI: http://localhost:5174, Gateway: http://127.0.0.1:28000"
