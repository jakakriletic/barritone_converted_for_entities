# Ročni preverbi z launcherjem, čim bolj samodejno (glej docs/04-STANJE.md, 2026-09-26).
#
#   .\launcher-test.ps1           build + mapa launcher-test\forge (naš izdani jar + pravi Baritone 1.2.19)
#                                 za M2 A6; v igri: nov superflat svet s cheati, /npcb selftest
#   .\launcher-test.ps1 -Server   dedicated strežnik za M3 A2 (vanilla klient): selftest se začne
#                                 sam 5 s po prijavi, strežnik se po koncu ustavi sam
param([switch]$Server)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$lt = Join-Path $root 'launcher-test'

if (-not $Server) {
    & (Join-Path $root 'dev.ps1') build --offline
    $mods = Join-Path $lt 'forge\mods'
    New-Item -ItemType Directory -Force -Path $mods, (Join-Path $lt 'vanilla') | Out-Null
    Get-ChildItem $mods -Filter 'npcbaritone-*.jar' | Remove-Item -Force
    $jar = Get-ChildItem (Join-Path $root 'mod\build\libs') -Filter 'npcbaritone-*.jar' |
        Where-Object { $_.Name -notmatch '-(api|sources|dev)\.jar$' } | Sort-Object LastWriteTime | Select-Object -Last 1
    if (-not $jar) { throw 'mod\build\libs nima npcbaritone jarja' }
    Copy-Item $jar.FullName $mods
    Copy-Item (Join-Path $root 'tools\cache\baritone-standalone-forge-1.2.19.jar') $mods
    Write-Output ''
    Write-Output "Pripravljeno: $mods"
    Get-ChildItem $mods | ForEach-Object { Write-Output ("   " + $_.Name) }
    Write-Output ''
    Write-Output 'Launcher -> Installations -> New installation:'
    Write-Output '   Version:        tvoj Forge 1.12.2 (npr. release 1.12.2-forge-14.23.5.2847)'
    Write-Output "   Game directory: $(Join-Path $lt 'forge')"
    Write-Output 'Play -> Singleplayer -> nov svet (Allow Cheats: ON, Superflat) -> /npcb selftest'
    Write-Output ''
    Write-Output 'Za vanilla klient (M3 A2) nato: .\launcher-test.ps1 -Server'
    Write-Output "   in v launcherju installation 'release 1.12.2' z Game directory: $(Join-Path $lt 'vanilla')"
    exit 0
}

$run = Join-Path $root 'mod\run'
$eula = Join-Path $run 'eula.txt'
if (-not ((Test-Path $eula) -and ((Get-Content $eula -Raw) -match 'eula\s*=\s*true'))) {
    throw "mod\run\eula.txt ni sprejet: najprej '.\smoke-server.ps1 -AcceptEula'."
}
$level = 'm3-vanilla'
$worldDir = Join-Path $run $level
if (Test-Path $worldDir) { Remove-Item $worldDir -Recurse -Force }
# spawn-animals/spawn-npcs=false bi volka in vaščana odstranil ob prvem ticku (WorldServer); spawn ustavi selftest (doMobSpawning)
$props = @("level-name=$level", 'level-seed=20260926', 'level-type=FLAT', 'generate-structures=false',
           'online-mode=false', 'spawn-protection=0', 'view-distance=6', 'spawn-npcs=true',
           'spawn-animals=true', 'spawn-monsters=false', 'difficulty=1', 'max-tick-time=-1')
Set-Content -Path (Join-Path $run 'server.properties') -Value $props -Encoding ASCII
$env:NPCB_SELFTEST_ON_JOIN = '1'
Write-Output 'Strežnik se zaganja. Ko se v konzoli pokaže "Done", v launcherju zaženi vanilla 1.12.2:'
Write-Output '   Multiplayer -> Direct Connect -> localhost'
Write-Output 'Selftest se začne sam 5 s po prijavi; strežnik se po koncu ustavi sam.'
try {
    & (Join-Path $root 'dev.ps1') runServer --offline --no-daemon
} finally {
    Remove-Item Env:\NPCB_SELFTEST_ON_JOIN -ErrorAction SilentlyContinue
}
