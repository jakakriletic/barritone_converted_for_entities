# M0.11 / merilo A3 — dedicated server smoke: runServer naloži npcbaritone in se čisto ustavi.
#
# Zagon:  .\smoke-server.ps1 -AcceptEula     (prvič; Minecraft EULA potrdiš samo ti)
#         .\smoke-server.ps1
#
# Izpis: docs\build-logs\m0-server-smoke.log (gitignore). Exit code 0 = vsa preverjanja OK.
param([switch]$AcceptEula, [int]$TimeoutSec = 300)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run  = Join-Path $root 'mod\run'
$logDir = Join-Path $root 'docs\build-logs'
New-Item -ItemType Directory -Force -Path $run, $logDir | Out-Null
$outLog = Join-Path $logDir 'm0-server-smoke.log'
if (Test-Path $outLog) { Remove-Item $outLog -Force }

# Java 8 enako kot dev.ps1
$candidates = @($env:NPCB_JAVA8_HOME, (Join-Path $root '..\customNPC_rework\.tools\jdk8'), 'C:\Program Files\Java\jdk1.8.0_202') | Where-Object { $_ }
$jdk = $candidates | Where-Object { Test-Path (Join-Path $_ 'bin\javac.exe') } | Select-Object -First 1
if (-not $jdk) { throw 'Java 8 JDK ni najden (glej dev.ps1).' }
$env:JAVA_HOME = (Resolve-Path $jdk).Path
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

$eula = Join-Path $run 'eula.txt'
if (-not ((Test-Path $eula) -and ((Get-Content $eula -Raw) -match 'eula\s*=\s*true'))) {
    if (-not $AcceptEula) { throw "mod\run\eula.txt ni sprejet. Potrdi Minecraft EULA z '.\smoke-server.ps1 -AcceptEula'." }
    Set-Content -Path $eula -Value "# https://account.mojang.com/documents/minecraft_eula`r`neula=true" -Encoding ASCII
}
$props = @('level-name=m0-smoke', 'level-seed=20260924', 'level-type=FLAT', 'generate-structures=false',
           'online-mode=false', 'spawn-protection=0', 'view-distance=4', 'spawn-npcs=false',
           'spawn-animals=false', 'spawn-monsters=false', 'max-tick-time=-1')
Set-Content -Path (Join-Path $run 'server.properties') -Value $props -Encoding ASCII

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = $env:ComSpec
$psi.Arguments = "/c gradlew.bat runServer --offline --no-daemon --console=plain > `"$outLog`" 2>&1"
$psi.WorkingDirectory = Join-Path $root 'mod'
$psi.UseShellExecute = $false
$psi.RedirectStandardInput = $true
$psi.CreateNoWindow = $true
$proc = [System.Diagnostics.Process]::Start($psi)

$deadline = (Get-Date).AddSeconds($TimeoutSec)
$done = $false
while ((Get-Date) -lt $deadline -and -not $proc.HasExited) {
    Start-Sleep -Seconds 2
    $c = Get-Content $outLog -Raw -ErrorAction SilentlyContinue
    if ($c -and $c -match 'Done \(') { $done = $true; break }
}
if (-not $proc.HasExited) {
    $proc.StandardInput.WriteLine('stop'); $proc.StandardInput.Flush()
    if (-not $proc.WaitForExit(120000)) { try { $proc.Kill() } catch { } }
}

$log = Get-Content $outLog -Raw -ErrorAction SilentlyContinue
$fails = @()
function Check([string]$what, [bool]$ok) {
    if ($ok) { Write-Output "  OK      $what" } else { Write-Output "  NAPAKA  $what"; $script:fails += $what }
}
Check 'strežnik je prišel do "Done ("'                         $done
Check 'FML je naložil mode (successfully loaded)'               ($log -match 'Forge Mod Loader has successfully loaded \d+ mods')
Check 'npcbaritone preInit (side=SERVER)'                       ($log -match 'NPC Baritone .* loaded \(side=SERVER')
Check 'npcbaritone ready (dedicated=true)'                      ($log -match 'npcbaritone ready on server \(dedicated=true\)')
Check 'config\npcbaritone.cfg obstaja'                          (Test-Path (Join-Path $run 'config\npcbaritone.cfg'))
Check 'brez izjem v logu'                                        (-not ($log -match 'Exception in server tick loop|Encountered an unexpected exception'))
Write-Output "log: $outLog"
if ($fails.Count -gt 0) { exit 1 }
