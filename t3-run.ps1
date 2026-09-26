# M8.9 — tečaj T3 (velikosti) na dedicated strežniku, merila M8 A1–A2.
#
# Zagon:  .\t3-run.ps1              (po prvem .\smoke-server.ps1 -AcceptEula)
#
# Svež superflat svet 'm8-t3'; T1 pri (0 4 0), T2 pri (60 4 0). Husk (puppet) prehodi T1 in
# T2 petkrat, pred vsakim odsekom dobi CNPC velikost 1, 3, 5, 7 ali 10
# (0,6/5*size x 1,8/5*size). Pričakovani izidi po velikosti so v CourseT3 (D-028).
# Rezultati: docs\meritve\m8\t3-*.csv (+ -trace.csv); log docs\build-logs\m8-t3.log.
# Exit code 0 = A1 (za vsako velikost T1 >= 9/10 in T2 >= 9/10), A2 (noben pričakovan neuspeh
# ni TIMEOUT — velikost, ki ne gre skozi, konča FAILED), velikosti nastavljene, 0 spremenjenih blokov.
param([int]$TimeoutSec = 3600)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run  = Join-Path $root 'mod\run'
$logDir = Join-Path $root 'docs\build-logs'
$outDir = Join-Path $root 'docs\meritve\m8'
New-Item -ItemType Directory -Force -Path $run, $logDir, $outDir | Out-Null
$outLog = Join-Path $logDir 'm8-t3.log'
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
# Klik v okno konzole (QuickEdit) zamrzne ta skript (glej t4-run.ps1); med tekom ga izklopimo.
$quickEdit = $null
try {
    Add-Type -Namespace Npcb -Name ConsoleMode -MemberDefinition @'
[DllImport("kernel32.dll")] public static extern System.IntPtr GetStdHandle(int n);
[DllImport("kernel32.dll")] public static extern bool GetConsoleMode(System.IntPtr h, out uint m);
[DllImport("kernel32.dll")] public static extern bool SetConsoleMode(System.IntPtr h, uint m);
'@ -ErrorAction Stop
    $hIn = [Npcb.ConsoleMode]::GetStdHandle(-10)
    $mode = [uint32]0
    if ([Npcb.ConsoleMode]::GetConsoleMode($hIn, [ref]$mode)) {
        $quickEdit = $mode
        [void][Npcb.ConsoleMode]::SetConsoleMode($hIn, [uint32](($mode -band (-bnot 0x40)) -bor 0x80))
    }
} catch { Write-Host "  (QuickEdit ni bilo mogoče izklopiti: ne klikaj v okno med tekom)" }
# T3 preverja D-028, ki velja samo s stikalom movement.largeEntities; brez njega navigator
# size 7 in 10 ne vodi (drugi tek 2026-09-26). Med tekom stikalo vklopimo, nato vrnemo.
$cfg = Join-Path $run 'config\npcbaritone.cfg'
$utf8 = New-Object System.Text.UTF8Encoding($false)
$cfgOriginal = $null
if (Test-Path $cfg) {
    $cfgOriginal = [IO.File]::ReadAllText($cfg, $utf8)
    if ($cfgOriginal -notmatch 'B:largeEntities=(true|false)') { throw "$cfg nima B:largeEntities" }
    [IO.File]::WriteAllText($cfg, ($cfgOriginal -replace 'B:largeEntities=false', 'B:largeEntities=true'), $utf8)
} else {
    New-Item -ItemType Directory -Force -Path (Split-Path $cfg) | Out-Null
    [IO.File]::WriteAllText($cfg, "movement {`r`n    B:largeEntities=true`r`n}`r`n", $utf8)
}
$level = 'm8-t3'
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
    Send 'npcb course t3 build 0 4 0'
    if (-not (WaitFor 'T3 postavljen pri')) { throw 'T3 ni bilo mogoče postaviti' }
    Send 'summon husk 0.5 4 0.5 {PersistenceRequired:1b}'
    Start-Sleep -Seconds 2
    Send 'npcb course t3 run @e[type=husk,c=1] 0 4 0'
    if (-not (WaitFor 'NPCB-COURSE-DONE')) { $ok = $false; Write-Output '  NAPAKA  T3 se ni končal v času' }
    Send 'npcb status @e[type=husk,c=1]'
    Start-Sleep -Seconds 1
} finally {
    if (-not $proc.HasExited) {
        Send 'stop'
        if (-not $proc.WaitForExit(120000)) { try { $proc.Kill() } catch { } }
    }
    if ($cfgOriginal -ne $null) { [IO.File]::WriteAllText($cfg, $cfgOriginal, $utf8) } else { Remove-Item $cfg -Force -ErrorAction SilentlyContinue }
}

$log = LogText
$fails = @()
function Check([string]$what, [bool]$pass) {
    if ($pass) { Write-Output "  OK      $what" } else { Write-Output "  NAPAKA  $what"; $script:fails += $what }
}
$inv = [Globalization.CultureInfo]::InvariantCulture

$csv = Get-ChildItem (Join-Path $run 'npcbaritone\runs') -Filter 't3-*.csv' -ErrorAction SilentlyContinue | Where-Object { $_.Name -notlike '*-trace.csv' } | Sort-Object LastWriteTime | Select-Object -Last 1
if ($csv) {
    Copy-Item $csv.FullName (Join-Path $outDir $csv.Name)
    $trace = Join-Path $csv.DirectoryName ($csv.BaseName + '-trace.csv')
    if (Test-Path $trace) { Copy-Item $trace (Join-Path $outDir ($csv.BaseName + '-trace.csv')) }
    $rows = Import-Csv $csv.FullName
    Check "T3 odsekov v CSV: $(@($rows).Count) (= 100)" (@($rows).Count -eq 100)
    foreach ($size in 1, 3, 5, 7, 10) {
        $bySize = @($rows | Where-Object { $_.npc_size -eq "$size" })
        $w = 0.6 / 5 * $size; $h = 1.8 / 5 * $size
        $sized = @($bySize | Where-Object { [math]::Abs([double]::Parse($_.width, $inv) - $w) -lt 0.02 -and [math]::Abs([double]::Parse($_.height, $inv) - $h) -lt 0.02 })
        Check ("size {0,2}: velikost nastavljena {1:0.00} x {2:0.00} na {3}/{4} odsekih" -f $size, $w, $h, $sized.Count, $bySize.Count) (($bySize.Count -eq 20) -and ($sized.Count -eq 20))
        foreach ($course in 'T1', 'T2') {
            $part = @($bySize | Where-Object { $_.name -like "* $course/*" })
            $passed = @($part | Where-Object { $_.pass -eq 'true' }).Count
            Check ("A1 size {0,2} {1}: {2}/{3} odsekov OK (>= 9)" -f $size, $course, $passed, $part.Count) (($part.Count -eq 10) -and ($passed -ge 9))
        }
    }
    $stuck = @($rows | Where-Object { $_.expect -eq 'FAIL' -and $_.result -ne 'FAILED' })
    Check "A2 pričakovani neuspehi končajo FAILED (ne tavajo): $(@($rows | Where-Object { $_.expect -eq 'FAIL' }).Count - $stuck.Count)/$(@($rows | Where-Object { $_.expect -eq 'FAIL' }).Count)" ($stuck.Count -eq 0)
    $timeouts = @($rows | Where-Object { $_.result -eq 'TIMEOUT' })
    Check "TIMEOUT odsekov: $($timeouts.Count) (= 0)" ($timeouts.Count -eq 0)
    $damage = ($rows | ForEach-Object { [double]::Parse($_.damage, $inv) } | Measure-Object -Sum).Sum
    Write-Output "  info    škoda skupaj: $damage"
    $bad = @($rows | Where-Object { $_.pass -ne 'true' })
    if ($bad.Count -gt 0) {
        Write-Output '  neuspešni odseki:'
        $bad | Format-Table segment, name, expect, result, ticks, meters, max_fall, damage, forbidden_entered, nav_state -AutoSize | Out-String -Width 220 | Write-Output
    }
} else {
    Check 'T3 CSV obstaja' $false
}
$changed = [regex]::Match($log, 'NPCB-COURSE-DONE course=T3 .*?blocks_changed=(\d+)')
Check "spremenjenih blokov (razen vrat): $($changed.Groups[1].Value) (= 0)" ($changed.Success -and $changed.Groups[1].Value -eq '0')
Check 'movement.largeEntities vklopljen' ($log -match 'largeEntities=true')
Check 'brez izjem v logu' (-not ($log -match 'Exception in server tick loop|Encountered an unexpected exception|Pathing exception|cannot resize'))
Write-Output "log: $outLog"
Write-Output "rezultati: $outDir"
if ($quickEdit -ne $null) { [void][Npcb.ConsoleMode]::SetConsoleMode($hIn, [uint32]$quickEdit) }
if (-not $ok -or $fails.Count -gt 0) { exit 1 }
