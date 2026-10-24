param(
    [string] $Version = "",
    [switch] $Build,
    [switch] $NoCache,
    [string[]] $Services = @(),
    [switch] $Changed
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..\..")
$composeFile = Join-Path $scriptDir "docker-compose.yml"
Set-Location $repoRoot

$allServices = @("core", "market", "ai", "gateway", "knowledgebase", "wechat", "ui")
$backendProjects = @{
    core = "cynexus-core/cynexus-core-starter"
    market = "cynexus-market/cynexus-market-starter"
    ai = "cynexus-ai/cynexus-ai-starter"
    gateway = "cynexus-gateway/cynexus-gateway-starter"
    knowledgebase = "cynexus-knowledgebase/cynexus-knowledgebase-starter"
    wechat = "cynexus-wechat/cynexus-wechat-starter"
}

if ($Services.Count -gt 0 -and $Changed) {
    throw "Use either -Services or -Changed, not both."
}

function Test-MeaningfulChange {
    param(
        [string] $File,
        [string[]] $UntrackedFiles
    )

    if ($File -in $UntrackedFiles -or $File -notmatch "(^|/)(pom.xml|package.json)$") {
        return $true
    }

    $diffLines = @(git diff --unified=0 HEAD -- $File 2>$null)
    $meaningfulLines = @($diffLines | Where-Object {
        $_ -match "^[+-]" -and
        $_ -notmatch "^(---|\+\+\+)" -and
        $_ -notmatch "<version>[^<]+</version>" -and
        $_ -notmatch '"version"\s*:'
    })
    return $meaningfulLines.Count -gt 0
}

function Get-ChangedServices {
    $changedFiles = @()
    $changedFiles += @(git diff --name-only HEAD 2>$null)
    $untrackedFiles = @(git ls-files --others --exclude-standard 2>$null)
    $changedFiles += $untrackedFiles
    $changedFiles = @($changedFiles | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique)

    $services = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
    foreach ($file in $changedFiles) {
        if (-not (Test-MeaningfulChange -File $file -UntrackedFiles $untrackedFiles)) { continue }
        $normalized = $file.Replace("/", "\\")
        if ($normalized -like "cynexus-ui\\*") {
            [void]$services.Add("ui")
        } elseif ($normalized -like "cynexus-core\\*") {
            [void]$services.Add("core")
        } elseif ($normalized -like "cynexus-market\\*") {
            [void]$services.Add("market")
        } elseif ($normalized -like "cynexus-ai\\*") {
            [void]$services.Add("ai")
        } elseif ($normalized -like "cynexus-gateway\\*") {
            [void]$services.Add("gateway")
        } elseif ($normalized -like "cynexus-knowledgebase\\*") {
            [void]$services.Add("knowledgebase")
        } elseif ($normalized -like "cynexus-wechat\\*") {
            [void]$services.Add("wechat")
        } elseif ($normalized -like "cynexus-common\\*" -or $normalized -eq "pom.xml") {
            foreach ($service in $backendProjects.Keys) { [void]$services.Add($service) }
        } elseif ($normalized -like "deploy\\release\\Dockerfile.ui" -or $normalized -like "deploy\\release\\nginx.conf") {
            [void]$services.Add("ui")
        } elseif ($normalized -like "deploy\\release\\apply-agent-migrations.ps1") {
            [void]$services.Add("ai")
        } elseif ($normalized -like "deploy\\release\\apply-market-migrations.ps1") {
            [void]$services.Add("market")
        } elseif ($normalized -like "deploy\\release\\docker-compose.yml") {
            foreach ($service in $allServices) { [void]$services.Add($service) }
        } elseif ($normalized -like "deploy\\local\\Dockerfile.app") {
            foreach ($service in $backendProjects.Keys) { [void]$services.Add($service) }
        } elseif ($normalized -like "deploy\\local\\postgres\\migrations\\*market*" -or $normalized -like "deploy\\local\\postgres\\migrations\\*stock*" -or $normalized -like "deploy\\local\\postgres\\migrations\\*scheduler*") {
            [void]$services.Add("market")
        } elseif ($normalized -like "deploy\\local\\postgres\\migrations\\*wechat*") {
            [void]$services.Add("ai")
            [void]$services.Add("wechat")
        } elseif ($normalized -like "deploy\\local\\postgres\\migrations\\*ai*" -or $normalized -like "deploy\\local\\postgres\\migrations\\*agent*") {
            [void]$services.Add("ai")
        } elseif ($normalized -like "deploy\\local\\postgres\\migrations\\*knowledge*") {
            [void]$services.Add("knowledgebase")
            [void]$services.Add("ai")
        }
    }

    return @($services)
}

if ($Services.Count -gt 0) {
    $unknownServices = @($Services | Where-Object { $_ -notin $allServices })
    if ($unknownServices.Count -gt 0) {
        throw "Unknown service(s): $($unknownServices -join ', '). Valid services: $($allServices -join ', ')."
    }
    $selectedServices = @($Services | Select-Object -Unique)
} elseif ($Changed) {
    $selectedServices = @(Get-ChangedServices)
    if ($selectedServices.Count -eq 0) {
        Write-Host "[cynexus] No release service changes detected; nothing to deploy."
        exit 0
    }
} else {
    $selectedServices = $allServices
}

$selectedBackendServices = @($selectedServices | Where-Object { $_ -in $backendProjects.Keys })
$fullDeployment = $selectedServices.Count -eq $allServices.Count -and ($selectedServices | Where-Object { $_ -notin $allServices }).Count -eq 0
Write-Host "[cynexus] Services: $($selectedServices -join ', ')"

# Stop the local Dev application before claiming the shared data stores.
& (Join-Path $repoRoot "deploy\dev\stop-dev.ps1")

# Apply only the schema migrations associated with services being deployed.
# Migration scripts are idempotent and run before the selected containers start.
if ($selectedServices -contains "ai") {
    & (Join-Path $repoRoot "deploy\release\apply-agent-migrations.ps1")
}
if ($selectedServices -contains "market") {
    & (Join-Path $repoRoot "deploy\release\apply-market-migrations.ps1")
    & (Join-Path $repoRoot "deploy\release\apply-market-clickhouse-migrations.ps1")
}
if ($selectedServices -contains "wechat") {
    & (Join-Path $repoRoot "deploy\release\apply-wechat-migrations.ps1")
}
# Core and the mobile UI consume the Life food catalog and artwork mapping.
# Apply the idempotent bundle whenever either consumer is deployed, including
# narrow service deployments on a fresh environment.
if ($selectedServices -contains "core" -or $selectedServices -contains "ui" -or $fullDeployment) {
    & (Join-Path $repoRoot "deploy\release\apply-life-migrations.ps1")
}

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

$ownerAuthPath = Join-Path $scriptDir ".owner-auth.json"
if ([string]::IsNullOrWhiteSpace($env:CYNEXUS_OWNER_PASSWORD_HASH)) {
    if (-not (Test-Path $ownerAuthPath)) {
        throw "Initialize authentication with deploy/release/initialize-owner-auth.ps1 first."
    }
    $env:CYNEXUS_OWNER_PASSWORD_HASH = (Get-Content $ownerAuthPath -Raw | ConvertFrom-Json).passwordHash
}

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
if ($env:CYNEXUS_TRUSTED_IDENTITY_SECRET.Length -lt 32) {
    throw "CYNEXUS_TRUSTED_IDENTITY_SECRET must contain at least 32 characters."
}

# The local Windows runtime may expose outbound HTTPS through a host proxy.
# Translate loopback proxy addresses so containers can reach the same proxy.
foreach ($proxyName in @("HTTP_PROXY", "HTTPS_PROXY")) {
    $sourceValue = [Environment]::GetEnvironmentVariable($proxyName)
    if (-not [string]::IsNullOrWhiteSpace($sourceValue)) {
        $containerValue = $sourceValue -replace "127\.0\.0\.1", "host.docker.internal" -replace "localhost", "host.docker.internal"
        $envName = "CYNEXUS_$proxyName"
        [Environment]::SetEnvironmentVariable($envName, $containerValue, "Process")
        Set-Item -Path "Env:$envName" -Value $containerValue
    }
}
if ([string]::IsNullOrWhiteSpace($env:CYNEXUS_NO_PROXY)) {
    $env:CYNEXUS_NO_PROXY = "localhost,127.0.0.1,cynexus-postgres,cynexus-redis,cynexus-nacos,cynexus-market,cynexus-core,cynexus-data-job-executor"
}
Write-Host "[cynexus] Release version: $releaseVersion"

if ($Build) {
    if ($selectedBackendServices.Count -gt 0) {
        $mavenProjects = @($selectedBackendServices | ForEach-Object { $backendProjects[$_] })
        $mavenArgs = @("-pl", ($mavenProjects -join ","), "-am", "package", "-DskipTests")
        & mvn @mavenArgs
        if ($LASTEXITCODE -ne 0) { throw "Maven build failed for: $($selectedBackendServices -join ', ')" }
    }
    if ($selectedServices -contains "ui") {
        Push-Location (Join-Path $repoRoot "cynexus-ui")
        try {
            & pnpm.cmd run build
            if ($LASTEXITCODE -ne 0) { throw "UI build failed." }
        } finally {
            Pop-Location
        }
    }
}

foreach ($service in $selectedBackendServices) {
    $artifactName = ($backendProjects[$service].Split("/")[-1]) + "-$mavenVersion.jar"
    $jar = Join-Path $repoRoot (Join-Path $backendProjects[$service] (Join-Path "target" $artifactName))
    if (-not (Test-Path $jar)) { throw "Missing release artifact for $service`: $jar. Run with -Build or build the requested version first." }
}

$network = docker network ls --format '{{.Name}}' | Select-String '^cynexus-dev_default$'
if (-not $network) { throw "Shared Docker network cynexus-dev_default is missing. Start deploy/local infrastructure first." }

foreach ($service in $selectedServices) {
    $imageTag = "cynexus-{0}:{1}" -f $service, $releaseVersion
    if ($service -eq "ui") {
        $buildArgs = @("build", "-f", (Join-Path $scriptDir "Dockerfile.ui"), "-t", $imageTag)
    } else {
        $artifactName = ($backendProjects[$service].Split("/")[-1]) + "-$mavenVersion.jar"
        $buildArgs = @(
            "build",
            "-f", (Join-Path $repoRoot "deploy\local\Dockerfile.app"),
            "--build-arg", "MAVEN_PROJECT=$($backendProjects[$service])",
            "--build-arg", "JAR_NAME=$artifactName",
            "-t", $imageTag
        )
    }
    if ($NoCache) { $buildArgs += "--no-cache" }
    $buildArgs += $repoRoot
    Write-Host "[cynexus] Building image: $imageTag"
    & docker @buildArgs
    if ($LASTEXITCODE -ne 0) { throw "Docker image build failed for $service." }
}

if ($fullDeployment) {
    $upArgs = @("-f", $composeFile, "up", "-d", "--force-recreate") + $selectedServices
} else {
    $upArgs = @("-f", $composeFile, "up", "-d", "--no-deps", "--force-recreate") + $selectedServices
}
& docker compose @upArgs
if ($LASTEXITCODE -ne 0) { throw "Docker service restart failed." }
Write-Host "Cynexus release is running in Docker. UI: http://localhost:5173"
