$ErrorActionPreference = "Stop"

$socialDataRoot = Split-Path -Parent $PSScriptRoot
$env:CYNEXUS_DATA_PROJECT_ROOT = $socialDataRoot
$env:CYNEXUS_SOCIAL_PROFILE_ROOT = Join-Path $socialDataRoot "var"
$env:PYTHONPATH = Join-Path $socialDataRoot "src"
$agentPort = 18183
$env:CYNEXUS_SOCIAL_LOCAL_AGENT_PORT = [string]$agentPort
$env:CYNEXUS_SOCIAL_BACKEND = "cdp"
$env:CYNEXUS_SOCIAL_CDP_HOST = "127.0.0.1"
$env:CYNEXUS_SOCIAL_CDP_PORT_XIAOHONGSHU = "19221"
$env:CYNEXUS_SOCIAL_CDP_PORT_DOUYIN = "19222"
$reloadEnabled = $env:CYNEXUS_SOCIAL_LOCAL_AGENT_RELOAD

$profileRoot = Join-Path $socialDataRoot "var\social-chrome-profiles"
$xhsProfile = Join-Path $profileRoot "xiaohongshu"
$douyinProfile = Join-Path $profileRoot "douyin"
New-Item -ItemType Directory -Force -Path $xhsProfile, $douyinProfile | Out-Null

$chromeCandidates = @(
    (Join-Path $env:ProgramFiles "Google\Chrome\Application\chrome.exe"),
    (Join-Path $env:LOCALAPPDATA "Google\Chrome\Application\chrome.exe"),
    (Join-Path ${env:ProgramFiles(x86)} "Google\Chrome\Application\chrome.exe")
)
$chrome = $chromeCandidates | Where-Object { $_ -and (Test-Path -LiteralPath $_) } | Select-Object -First 1
if (-not $chrome) {
    throw "Google Chrome was not found. Install Chrome and run this script again."
}

function Start-SocialChrome {
    param(
        [string]$Port,
        [string]$Profile,
        [string]$Url
    )

    $existing = Get-NetTCPConnection -LocalPort ([int]$Port) -State Listen -ErrorAction SilentlyContinue
    if (-not $existing) {
        $arguments = @(
            ("--remote-debugging-port={0}" -f $Port),
            ("--user-data-dir={0}" -f $Profile),
            "--profile-directory=Default",
            "--no-first-run",
            "--no-default-browser-check",
            "--disable-features=Translate",
            $Url
        )
        Start-Process -FilePath $chrome -ArgumentList $arguments | Out-Null
    }
}

Start-SocialChrome -Port "19221" -Profile $xhsProfile -Url "https://www.xiaohongshu.com/"
Start-SocialChrome -Port "19222" -Profile $douyinProfile -Url "https://www.douyin.com/"

$existingAgentConnections = @(Get-NetTCPConnection -LocalPort $agentPort -State Listen -ErrorAction SilentlyContinue)
if ($existingAgentConnections) {
    $agentProcessIds = @($existingAgentConnections | Select-Object -ExpandProperty OwningProcess -Unique)
    foreach ($ownerProcessId in $agentProcessIds) {
        $agentProcess = Get-CimInstance Win32_Process -Filter "ProcessId = $ownerProcessId"
        if ($agentProcess.CommandLine -like "*cynexus_data.interfaces.local_agent.app:app*") {
            Write-Host ("Stopping existing Cynexus social agent process {0}." -f $ownerProcessId)
            Stop-Process -Id $ownerProcessId -Force
        }
        else {
            throw ("Port {0} is already occupied by PID {1}. Stop that process and run this script again." -f $agentPort, $ownerProcessId)
        }
    }
    Start-Sleep -Milliseconds 500
}

Push-Location $socialDataRoot
try {
    Write-Host ("Cynexus local social agent: http://127.0.0.1:{0}" -f $agentPort)
    Write-Host "Xiaohongshu CDP: 19221; Douyin CDP: 19222"
    Write-Host "Complete login in the matching Chrome window."
    if ([string]::IsNullOrWhiteSpace($reloadEnabled) -or $reloadEnabled -in @("1", "true", "yes", "on")) {
        Write-Host "Auto reload: enabled (set CYNEXUS_SOCIAL_LOCAL_AGENT_RELOAD=false to disable)."
        & python -m uvicorn cynexus_data.interfaces.local_agent.app:app `
            --host 127.0.0.1 `
            --port $agentPort `
            --reload `
            --reload-dir (Join-Path $socialDataRoot "src")
    }
    else {
        Write-Host "Auto reload: disabled."
        & python -m uvicorn cynexus_data.interfaces.local_agent.app:app `
            --host 127.0.0.1 `
            --port $agentPort
    }
}
finally {
    Pop-Location
}
