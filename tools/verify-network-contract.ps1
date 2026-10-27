$ErrorActionPreference = 'Stop'
$projectPath = Split-Path -Parent $PSScriptRoot
$networkPath = Join-Path $projectPath 'src/main/java/com/mechaniciancombat/net/ModNetwork.java'
$serverPath = Join-Path $projectPath 'src/main/java/com/mechaniciancombat/TkGunServer.java'
$clientPath = Join-Path $projectPath 'src/main/java/com/mechaniciancombat/client/TkGunClient.java'
$networkSource = Get-Content -LiteralPath $networkPath -Raw
$serverSource = Get-Content -LiteralPath $serverPath -Raw
$clientSource = Get-Content -LiteralPath $clientPath -Raw

$expectedDirections = [ordered]@{
    0  = 'GunFirePayload:PLAY_TO_SERVER'
    1  = 'GunFocusPayload:PLAY_TO_SERVER'
    2  = 'GunReloadPayload:PLAY_TO_SERVER'
    3  = 'GunRollPayload:PLAY_TO_SERVER'
    4  = 'OpenAmmoStoragePayload:PLAY_TO_SERVER'
    5  = 'SwitchAmmoPayload:PLAY_TO_SERVER'
    6  = 'CombatBackpackFirePayload:PLAY_TO_SERVER'
    7  = 'GunStatePayload:PLAY_TO_CLIENT'
    8  = 'GunHitMarkerPayload:PLAY_TO_CLIENT'
    9  = 'GunDemoPayload:PLAY_TO_CLIENT'
    10 = 'CombatBackpackFireResultPayload:PLAY_TO_CLIENT'
    11 = 'CombatBackpackStatusPayload:PLAY_TO_CLIENT'
    12 = 'GunSoundPayload:PLAY_TO_SERVER'
}

$registered = [regex]::Matches(
    $networkSource,
    '(?m)^\s*register\((\d+),\s*(\w+)\.ID,\s*\w+\.CODEC,\s*(NetworkDirection\.\w+|null)\);')
if ($registered.Count -ne $expectedDirections.Count) {
    throw "Expected $($expectedDirections.Count) explicit packet registrations; found $($registered.Count)."
}

foreach ($entry in $registered) {
    $id = [int]$entry.Groups[1].Value
    $actual = "$($entry.Groups[2].Value):$($entry.Groups[3].Value -replace '^NetworkDirection\.', '')"
    if (!$expectedDirections.Contains($id)) {
        throw "Unexpected packet id $id ($actual)."
    }
    if ($actual -ne $expectedDirections[$id]) {
        throw "Packet $id direction mismatch: expected '$($expectedDirections[$id])', got '$actual'."
    }

    $payloadType = $entry.Groups[2].Value
    if ($expectedDirections[$id].EndsWith('PLAY_TO_SERVER')) {
        if ($serverSource -notmatch "ModNetwork\.registerGlobalReceiver\((?:com\.mechaniciancombat\.net\.)?$payloadType\.ID") {
            throw "Missing server receiver for $payloadType."
        }
    } else {
        if ($clientSource -notmatch "ClientNetwork\.registerGlobalReceiver\((?:com\.mechaniciancombat\.net\.)?$payloadType\.ID") {
            throw "Missing client receiver for $payloadType."
        }
    }
}

if ($networkSource -notmatch 'VERSION::equals,\s*VERSION::equals') {
    throw 'SimpleChannel must reject incompatible protocol versions in both directions.'
}

Write-Output "PASS: $($registered.Count) packet IDs, directions, receivers, and protocol checks match the Forge migration contract."
