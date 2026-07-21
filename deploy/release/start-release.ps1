param(
    [string] $Version = "",
    [switch] $Build,
    [switch] $NoCache
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..\..")
$composeFile = Join-Path $scriptDir "docker-compose.yml"
Set-Location $repoRoot

# Stop the local Dev application before claiming the shared data stores.
& (Join-Path $repoRoot "deploy\dev\stop-dev.ps1")

$pom = [xml](Get-Content (Join-Path $repoRoot "pom.xml") -Raw)
$currentMavenVersion = [string]$pom.project.version
$todaySuffix = Get-Date -Format "yyMMdd"
$baseVersion = $currentMavenVersion -replace "-SNAPSHOT$", ""
$mavenVersion = if ([string]::IsNullOrWhiteSpace($Version)) {
    $currentMavenVersion
} elseif ($Version.EndsWith("-SNAPSHOT")) {
    $Version
} elseif ($Version -match "^(.*)-\d{6}-RELEASE$") {
    "$($Matches[1])-SNAPSHOT"
} else {
    (($Version -replace "-RELEASE$", "") + "-SNAPSHOT")
}
$releaseVersion = if ([string]::IsNullOrWhiteSpace($Version)) {
    if ($baseVersion -match "-\d{6}$") { "$baseVersion-RELEASE" } else { "$baseVersion-$todaySuffix-RELEASE" }
} elseif ($Version.EndsWith("-RELEASE")) {
    $Version
} else {
    (($Version -replace "-SNAPSHOT$", "") + "-RELEASE")
}
$releasePattern = '^\d+\.\d+\.\d+(?:-[A-Za-z0-9]+)*-\d{6}-RELEASE$'
if ($releaseVersion -notmatch $releasePattern) { throw "Release version must match x.y.z[-QUALIFIER]-YYMMDD-RELEASE, for example 1.1.1-ALPHA-260716-RELEASE." }
$env:CYNEXUS_MAVEN_VERSION = $mavenVersion
$env:CYNEXUS_RELEASE_VERSION = $releaseVersion

# The local Windows runtime may expose outbound HTTPS through a host proxy.
# Translate loopback proxy addresses so containers can reach the same proxy.
foreach ($proxyName in @("HTTP_PROXY", "HTTPS_PROXY")) {
    $sourceValue = [Environment]::GetEnvironmentVariable($proxyName)
    if (-not [string]::IsNullOrWhiteSpace($sourceValue)) {
        $containerValue = $sourceValue -replace "127\.0\.0\.1", "host.docker.internal" -replace "localhost", "host.docker.internal"
        [Environment]::SetEnvironmentVariable("CYNEXUS_$proxyName", $containerValue, "Process")
    }
}
if ([string]::IsNullOrWhiteSpace($env:CYNEXUS_NO_PROXY)) {
    $env:CYNEXUS_NO_PROXY = "localhost,127.0.0.1,cynexus-postgres,cynexus-redis,cynexus-nacos,cynexus-market,cynexus-core,cynexus-data-job-executor"
}
Write-Host "[cynexus] Release version: $releaseVersion"

if ($Build) {
    mvn clean package -DskipTests
}

$requiredJars = @(
    "cynexus-core\cynexus-core-starter\target\cynexus-core-starter-$mavenVersion.jar",
    "cynexus-market\cynexus-market-starter\target\cynexus-market-starter-$mavenVersion.jar",
    "cynexus-ai\cynexus-ai-starter\target\cynexus-ai-starter-$mavenVersion.jar",
    "cynexus-gateway\cynexus-gateway-starter\target\cynexus-gateway-starter-$mavenVersion.jar"
)
foreach ($jar in $requiredJars) {
    if (-not (Test-Path (Join-Path $repoRoot $jar))) { throw "Missing release artifact: $jar. Run with -Build or build the requested version first." }
}

$network = docker network ls --format '{{.Name}}' | Select-String '^cynexus-dev_default$'
if (-not $network) { throw "Shared Docker network cynexus-dev_default is missing. Start deploy/local infrastructure first." }

if ($NoCache) {
    docker compose -f $composeFile build --no-cache
} else {
    docker compose -f $composeFile build
}
docker compose -f $composeFile up -d --force-recreate
Write-Host "Cynexus release is running in Docker. UI: http://localhost:5173"
