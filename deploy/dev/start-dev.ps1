param(
    [switch] $SkipBuild,
    [switch] $SkipUi
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..\..")
$pidFile = Join-Path $scriptDir "dev.pids.json"
Set-Location $repoRoot

$envFile = Join-Path $repoRoot ".env"
if (Test-Path $envFile) {
    foreach ($line in Get-Content $envFile) {
        if ($line -match '^\s*#' -or $line -notmatch '^\s*([^=]+)=(.*)$') { continue }
        $name = $matches[1].Trim()
        $value = $matches[2].Trim().Trim('"').Trim("'")
        if ($name) { Set-Item -Path "Env:$name" -Value $value }
    }
}

# Dev services do not register with Nacos. Pin AI's market Feign calls to the
# local Market process instead of allowing a stale service-discovery lookup.
$env:CYNEXUS_AI_MARKET_BASE_URL = "http://127.0.0.1:28082"

$identitySecretPath = Join-Path $scriptDir ".trusted-identity-secret"
if ([string]::IsNullOrWhiteSpace($env:CYNEXUS_TRUSTED_IDENTITY_SECRET)) {
    if (Test-Path $identitySecretPath) {
        $env:CYNEXUS_TRUSTED_IDENTITY_SECRET = (Get-Content $identitySecretPath -Raw).Trim()
    } else {
        $bytes = New-Object byte[] 48
        $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
        $generator.GetBytes($bytes)
        $generator.Dispose()
        $env:CYNEXUS_TRUSTED_IDENTITY_SECRET = [Convert]::ToBase64String($bytes)
        Set-Content -Path $identitySecretPath -Value $env:CYNEXUS_TRUSTED_IDENTITY_SECRET -NoNewline -Encoding ASCII
    }
}
$env:CYNEXUS_AI_AGENT_ALLOW_ANONYMOUS_OWNER = "false"

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
    $process = Start-Process -FilePath $java -ArgumentList @("-jar", $jar, "--spring.profiles.active=dev") -WorkingDirectory $repoRoot -WindowStyle Hidden -RedirectStandardOutput $log -RedirectStandardError $errorLog -PassThru
    $started += [PSCustomObject]@{ Name = $service.Name; ProcessId = $process.Id }
}

if (-not $SkipUi) {
    $uiLog = Join-Path $scriptDir "ui.log"
    $uiErrorLog = Join-Path $scriptDir "ui.error.log"
    $previousBackendUrl = $env:VITE_CYNEXUS_BACKEND_URL
    $env:VITE_CYNEXUS_BACKEND_URL = "http://127.0.0.1:28000"
    $node = (Get-Command node.exe -ErrorAction Stop).Source
    $viteCli = Join-Path $repoRoot "cynexus-ui\node_modules\vite\bin\vite.js"
    $ui = Start-Process -FilePath $node -ArgumentList @($viteCli, "--port", "5174") -WorkingDirectory (Join-Path $repoRoot "cynexus-ui") -WindowStyle Hidden -RedirectStandardOutput $uiLog -RedirectStandardError $uiErrorLog -PassThru
    if ($null -eq $previousBackendUrl) { Remove-Item Env:VITE_CYNEXUS_BACKEND_URL -ErrorAction SilentlyContinue } else { $env:VITE_CYNEXUS_BACKEND_URL = $previousBackendUrl }
    $started += [PSCustomObject]@{ Name = "ui"; ProcessId = $ui.Id }
}

$started | ConvertTo-Json | Set-Content -Path $pidFile -Encoding UTF8
Write-Host "Cynexus dev started. UI: http://localhost:5174, Gateway: http://127.0.0.1:28000"
