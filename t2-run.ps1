# M4.8 — tečaj T2 (interakcije) na dedicated strežniku, merila M4 A1–A4.
#
# Zagon:  .\t2-run.ps1              (po prvem .\smoke-server.ps1 -AcceptEula)
#
# Svež superflat svet 'm4-t2'; husk (puppet) prehodi 10 zaprtih prog T2: lesena vrata,
# železna vrata (mora končati FAILED), ograjna vrata, lestev gor/dol 5, voda 1 in 3,
# lava z mostom, kaktusi z režami, padec 10 v vodo.
# Rezultati: docs\meritve\m4\t2-*.csv (+ -trace.csv); log docs\build-logs\m4-t2.log.
# Exit code 0 = A1 (>= 9/10, odsek 2 FAILED), A2 (0 spremenjenih blokov razen odpiranja vrat),
# A3 (0 škode), A4 (vrata za NPC-jem zaprta).
param([int]$TimeoutSec = 900)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run  = Join-Path $root 'mod\run'
$logDir = Join-Path $root 'docs\build-logs'
$outDir = Join-Path $root 'docs\meritve\m4'
New-Item -ItemType Directory -Force -Path $run, $logDir, $outDir | Out-Null
$outLog = Join-Path $logDir 'm4-t2.log'
if (Test-Path $outLog) {
    try { Remove-Item $outLog -Force -ErrorAction Stop }
    catch { throw "Log $outLog je zaklenjen: prejšnji strežnik še teče. Ustavi ga: Get-Process java | Stop-Process -Force (zapri tudi Minecraft, če teče), nato znova." }
}

$candidates = @($env:NPCB_JAVA8_HOME, (Join-Path $root '..\customNPC_rework\.tools\jdk8'), 'C:\Program Files\Java\jdk1.8.0_202') | Where-Object { $_ }
$jdk = $candidates | Where-Object { Test-Path (Join-Path $_ 'bin\javac.exe') } | Select-Object -First 1
if (-not $jdk) { throw 'Java 8 JDK ni najden (glej dev.ps1).' }
$env:JAVA_HOME = (Resolve-Path $jdk).Path
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

$eula = Join-Path $run 'eula.txt'
if (-not ((Test-Path $eula) -and ((Get-Content $eula -Raw) -match 'eula\s*=\s*true'))) {
    throw "mod\run\eula.txt ni sprejet: najprej '.\smoke-server.ps1 -AcceptEula'."
}
$level = 'm4-t2'
$worldDir = Join-Path $run $level
if (Test-Path $worldDir) { Remove-Item $worldDir -Recurse -Force }
$props = @("level-name=$level", 'level-seed=20260924', 'level-type=FLAT', 'generate-structures=false',
           'online-mode=false', 'spawn-protection=0', 'view-distance=4', 'spawn-npcs=false',
           'spawn-animals=false', 'spawn-monsters=false', 'difficulty=1', 'max-tick-time=-1')
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

function LogText { Get-Content $outLog -Raw -ErrorAction SilentlyContinue }
function Send([string]$cmd) { Write-Output "  > $cmd"; $proc.StandardInput.WriteLine($cmd); $proc.StandardInput.Flush() }
function WaitFor([string]$pattern, [int]$count = 1) {
    while ((Get-Date) -lt $deadline -and -not $proc.HasExited) {
        $t = LogText
        if ($t -and ([regex]::Matches($t, $pattern)).Count -ge $count) { return $true }
        Start-Sleep -Seconds 2
    }
    return $false
}

$ok = $true
try {
    if (-not (WaitFor 'Done \(')) { throw 'strežnik se ni zagnal' }
    Send 'gamerule doMobSpawning false'
    Send 'gamerule doDaylightCycle false'
    Send 'gamerule doFireTick false'
    Send 'time set 6000'
    Send 'npcb course t2 build 0 4 0'
    if (-not (WaitFor 'T2 postavljen pri')) { throw 'T2 ni bilo mogoče postaviti' }
    Send 'summon husk 0.5 4 0.5 {PersistenceRequired:1b}'
    Start-Sleep -Seconds 2
    Send 'npcb course t2 run @e[type=husk,c=1] 0 4 0'
    if (-not (WaitFor 'NPCB-COURSE-DONE')) { $ok = $false; Write-Output '  NAPAKA  T2 se ni končal v času' }
    Send 'npcb status @e[type=husk,c=1]'
    Start-Sleep -Seconds 1
} finally {
    if (-not $proc.HasExited) {
        Send 'stop'
        if (-not $proc.WaitForExit(120000)) { try { $proc.Kill() } catch { } }
    }
}

$log = LogText
$fails = @()
function Check([string]$what, [bool]$pass) {
    if ($pass) { Write-Output "  OK      $what" } else { Write-Output "  NAPAKA  $what"; $script:fails += $what }
}

$csv = Get-ChildItem (Join-Path $run 'npcbaritone\runs') -Filter 't2-*.csv' -ErrorAction SilentlyContinue | Where-Object { $_.Name -notlike '*-trace.csv' } | Sort-Object LastWriteTime | Select-Object -Last 1
if ($csv) {
    Copy-Item $csv.FullName (Join-Path $outDir $csv.Name)
    $trace = Join-Path $csv.DirectoryName ($csv.BaseName + '-trace.csv')
    if (Test-Path $trace) { Copy-Item $trace (Join-Path $outDir ($csv.BaseName + '-trace.csv')) }
    $rows = Import-Csv $csv.FullName
    $passed = @($rows | Where-Object { $_.pass -eq 'true' }).Count
    $seg2 = $rows | Where-Object { $_.segment -eq '2' }
    $damage = ($rows | ForEach-Object { [double]::Parse($_.damage, [Globalization.CultureInfo]::InvariantCulture) } | Measure-Object -Sum).Sum
    $doors = @($rows | Where-Object { $_.openables_closed -ne '' })
    $open = @($doors | Where-Object { $_.openables_closed -ne 'true' })
    Check "A1 T2 odsekov OK: $passed/10 (>= 9)" ($passed -ge 9)
    Check "A1 železna vrata končajo FAILED: $($seg2.result)" ($seg2 -and $seg2.result -eq 'FAILED')
    Check "A3 škoda v T2: $damage (= 0)" ($damage -eq 0)
    Check "A4 vrata za NPC-jem zaprta: $($doors.Count - $open.Count)/$($doors.Count)" (($doors.Count -gt 0) -and ($open.Count -eq 0))
    $reachRows = @($rows | Where-Object { $_.result -eq 'REACHED' })
    $notArrived = @($reachRows | Where-Object { $_.nav_state -ne 'ARRIVED' })
    Check "M5 NavStatus ARRIVED za dosežene odseke: $($reachRows.Count - $notArrived.Count)/$($reachRows.Count)" (($reachRows.Count -gt 0) -and ($notArrived.Count -eq 0))
    $failRows = @($rows | Where-Object { $_.result -eq 'FAILED' })
    Check "M5 NavStatus FAILED za neuspele odseke: $(@($failRows | Where-Object { $_.nav_state -eq 'FAILED' }).Count)/$($failRows.Count)" (@($failRows | Where-Object { $_.nav_state -ne 'FAILED' }).Count -eq 0)
    $rows | Format-Table segment, name, result, pass, ticks, meters, max_fall, damage, openables_closed -AutoSize | Out-String | Write-Output
} else {
    Check 'T2 CSV obstaja' $false
}
$changed = [regex]::Match($log, 'NPCB-COURSE-DONE course=T2 .*?blocks_changed=(\d+)')
Check "A2 spremenjenih blokov (razen vrat): $($changed.Groups[1].Value) (= 0)" ($changed.Success -and $changed.Groups[1].Value -eq '0')
Check 'brez izjem v logu' (-not ($log -match 'Exception in server tick loop|Encountered an unexpected exception|Pathing exception'))
Write-Output "log: $outLog"
Write-Output "rezultati: $outDir"
if (-not $ok -or $fails.Count -gt 0) { exit 1 }
