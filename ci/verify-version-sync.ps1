param(
    [switch]$Quiet
)

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$rootPomPath = Join-Path $repoRoot "pom.xml"
$uiPackagePath = Join-Path $repoRoot "cynexus-ui/package.json"

[xml]$rootPom = Get-Content -Raw $rootPomPath
$ns = New-Object System.Xml.XmlNamespaceManager($rootPom.NameTable)
$ns.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
$rootVersion = $rootPom.SelectSingleNode("/m:project/m:version", $ns).InnerText
$uiVersion = (Get-Content -Raw $uiPackagePath | ConvertFrom-Json).version

$pomVersions = [ordered]@{ "pom.xml" = $rootVersion }
Get-ChildItem -Path $repoRoot -Recurse -Filter pom.xml -File |
    Where-Object { $_.FullName -notmatch "\\target\\|\\node_modules\\|\\.git\\" } |
    ForEach-Object {
        [xml]$pom = Get-Content -Raw $_.FullName
        $manager = New-Object System.Xml.XmlNamespaceManager($pom.NameTable)
        $manager.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
        $projectVersion = $pom.SelectSingleNode("/m:project/m:version", $manager)
        $parentVersion = $pom.SelectSingleNode("/m:project/m:parent/m:version", $manager)
        $version = if ($projectVersion) { $projectVersion.InnerText } elseif ($parentVersion) { $parentVersion.InnerText } else { $null }
        if ($version -and $version -match "-SNAPSHOT$") {
            $relative = $_.FullName.Substring($repoRoot.Path.Length).TrimStart("\\")
            $pomVersions[$relative] = $version
        }
    }

$values = @($pomVersions.Values) + $uiVersion
$mismatch = $values | Where-Object { $_ -ne $rootVersion }
$invalid = $values | Where-Object { $_ -notmatch '^\d+\.\d+\.\d+-SNAPSHOT$' }

if ($mismatch -or $invalid) {
    if (-not $Quiet) {
        $pomVersions.GetEnumerator() | ForEach-Object { Write-Host ("{0}: {1}" -f $_.Key, $_.Value) }
        Write-Host ("cynexus-ui/package.json: {0}" -f $uiVersion)
    }
    exit 1
}

if (-not $Quiet) {
    Write-Host ("Version synchronized: {0}" -f $rootVersion)
}
exit 0
