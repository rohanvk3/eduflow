$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
$toolchains = Join-Path $workspace '.verification\toolchains'
New-Item -ItemType Directory -Path $toolchains -Force | Out-Null
$api = 'https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse'
$assets = @(Invoke-RestMethod -Uri $api)
$asset = $assets | Where-Object { $_.version.major -eq 21 -and $_.binary.image_type -eq 'jdk' } | Select-Object -First 1
if (!$asset) { throw 'Adoptium did not return a Windows x64 JDK 21' }
$package = $asset.binary.package
if ($package.name -notmatch '^OpenJDK21U-jdk_x64_windows_hotspot_[a-zA-Z0-9_.]+\.zip$' -or $package.checksum -notmatch '^[a-fA-F0-9]{64}$') { throw 'Unexpected package metadata' }
$archive = Join-Path $toolchains $package.name
if (!(Test-Path -LiteralPath $archive) -or (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash -ne $package.checksum) {
    $temporaryArchive = Join-Path $toolchains ([guid]::NewGuid().ToString('N') + '.zip')
    & curl.exe --silent --show-error --fail --location --retry 3 --output $temporaryArchive $package.link
    if ($LASTEXITCODE -ne 0) { throw 'JDK download failed' }
    if ((Get-FileHash -LiteralPath $temporaryArchive -Algorithm SHA256).Hash -ne $package.checksum) { throw 'Downloaded JDK checksum mismatch' }
    Move-Item -LiteralPath $temporaryArchive -Destination $archive -Force
}
$hash = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash
if ($hash -ne $package.checksum) { throw 'Portable JDK SHA-256 does not match official metadata; refusing extraction' }
$destination = Join-Path $toolchains $package.checksum.Substring(0,16)
if (!(Test-Path -LiteralPath $destination)) { Expand-Archive -LiteralPath $archive -DestinationPath $destination }
$jdk = Get-ChildItem -LiteralPath $destination -Directory | Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\javac.exe') } | Select-Object -First 1
if (!$jdk) { throw 'Downloaded archive does not contain a JDK' }
@{ jdkHome=$jdk.FullName; version=$asset.version.openjdk_version; source=$api; download=$package.link; archiveSHA256=$hash; checksumVerified=$true } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $workspace '.verification\jdk21.json') -Encoding UTF8
Write-Output "Verified portable JDK: $($asset.version.openjdk_version)"
Write-Output "JDK directory: $($jdk.FullName)"
Write-Output "SHA-256: $hash"
& "$($jdk.FullName)\bin\java.exe" --version
if ($LASTEXITCODE -ne 0) { throw 'Portable Java version check failed' }
