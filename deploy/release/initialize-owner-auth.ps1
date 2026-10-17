param([switch]$ResetPassword)
$ErrorActionPreference = "Stop"
$configPath = Join-Path $PSScriptRoot ".owner-auth.json"
$passwordPath = Join-Path $PSScriptRoot ".owner-initial-password.txt"
if ((Test-Path $configPath) -and -not $ResetPassword) {
    Write-Host "Owner authentication is already initialized. Use -ResetPassword to rotate the password."
    exit 0
}
$password = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
$salt = [Security.Cryptography.RandomNumberGenerator]::GetBytes(16)
$derived = [Security.Cryptography.Rfc2898DeriveBytes]::Pbkdf2($password, $salt, 310000, [Security.Cryptography.HashAlgorithmName]::SHA256, 32)
$saltHex = [Convert]::ToHexString($salt).ToLowerInvariant()
$hashHex = [Convert]::ToHexString($derived).ToLowerInvariant()
$config = @{ passwordHash = "pbkdf2-sha256:310000:${saltHex}:${hashHex}" } | ConvertTo-Json
[IO.File]::WriteAllText($configPath, $config)
[IO.File]::WriteAllText($passwordPath, $password)
# Restrict both local files to the current Windows user and SYSTEM.
if ($IsWindows) {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent().Name
    foreach ($path in @($configPath, $passwordPath)) {
        & icacls $path /inheritance:r /grant:r "${identity}:(F)" "SYSTEM:(F)" | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "Could not restrict credential file permissions: $path" }
    }
}
Write-Host "Owner authentication initialized. Initial password: $passwordPath"
Write-Host "Rebuild/recreate gateway after password rotation; existing sessions will become invalid."
