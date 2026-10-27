$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$destination = Join-Path $projectRoot 'libs'
New-Item -ItemType Directory -Force -Path $destination | Out-Null
# Exact Modrinth release IDs, all from dependencies already used by the reference project.
$releases = [ordered]@{
    accessories = 'WUQZDiqP'
    geckolib = 'aC5KMoNg'
    playeranimator = 'xe2EVE6q'
    shouldersurfing = 'dyrE1o1x'
    combat_roll = '532t65Zz'
    cloth_config = 't8TXrZvZ'
}
$locked = @()
foreach ($name in $releases.Keys) {
    $metadata = Invoke-RestMethod -Uri ('https://api.modrinth.com/v2/version/' + $releases[$name])
    if ('forge' -notin $metadata.loaders -or '1.20.1' -notin $metadata.game_versions) {
        throw "Release does not declare Forge 1.20.1: $name"
    }
    $artifact = @($metadata.files | Where-Object primary)[0]
    if ([IO.Path]::GetFileName($artifact.filename) -ne $artifact.filename) { throw 'Invalid artifact filename' }
    $target = Join-Path $destination $artifact.filename
    if (!(Test-Path -LiteralPath $target)) { Invoke-WebRequest -Uri $artifact.url -OutFile $target }
    $hash = (Get-FileHash -LiteralPath $target -Algorithm SHA512).Hash.ToLowerInvariant()
    if ($hash -ne $artifact.hashes.sha512) { throw "Hash mismatch: $target" }
    $locked += [ordered]@{
        name = $name; version = $metadata.version_number; releaseId = $metadata.id
        filename = $artifact.filename; url = $artifact.url; sha512 = $hash
        sha256 = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant()
        declaredDependencies = $metadata.dependencies
    }
    Write-Output "Verified $name $($metadata.version_number)"
}
$locked | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $projectRoot 'docs/DEPENDENCIES.json') -Encoding utf8
