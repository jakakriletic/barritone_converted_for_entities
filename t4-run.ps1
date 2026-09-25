# M5.7 — stresni tečaj T4 na dedicated strežniku, merila M5 A1–A5 (D-027).
#
# Zagon:  .\t4-run.ps1                          (3 ponovitve × 50 in 200 mobov, 120 s meritve)
#         .\t4-run.ps1 -Repeats 1 -Seconds 60   (hitra preverba)
#         .\t4-run.ps1 -Mobs 200 -Repeats 1 -Seconds 3600   (A4: 1 h stresa z rušenjem)
#
# Vsak tek: svež superflat svet 'm5-t4', nov strežnik (protokol §4: en tek = ena ponovitev),
# /npcb stress start <n> 48 <s> 5 0 4 0 → ovire, n huskov, naključni cilji, rušenje vsakih 5 s,
# 10 s ogrevanja, nato meritev. CSV: docs\meritve\m5\t4-<n>-*.csv, povzetek t4-summary-*.csv,
# logi docs\build-logs\m5-t4-<n>-<i>.log.
# Exit code 0 = A1 stopnja B (p95 glavne niti pri 200 ≤ 5 ms), A2 (posnetek p95 < 100 µs),
# A3 (0 niti izven bazena), A4 (0 izjem). A1+ (≤ 2 ms, pogoj za M7) se samo poroča.
param([int[]]$Mobs = @(50, 200), [int]$Repeats = 3, [int]$Seconds = 120, [int]$Radius = 48, [int]$BreakEvery = 5)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run  = Join-Path $root 'mod\run'
$logDir = Join-Path $root 'docs\build-logs'
$outDir = Join-Path $root 'docs\meritve\m5'
New-Item -ItemType Directory -Force -Path $run, $logDir, $outDir | Out-Null

$candidates = @($env:NPCB_JAVA8_HOME, (Join-Path $root '..\customNPC_rework\.tools\jdk8'), 'C:\Program Files\Java\jdk1.8.0_202') | Where-Object { $_ }
$jdk = $candidates | Where-Object { Test-Path (Join-Path $_ 'bin\javac.exe') } | Select-Object -First 1
if (-not $jdk) { throw 'Java 8 JDK ni najden (glej dev.ps1).' }
$env:JAVA_HOME = (Resolve-Path $jdk).Path
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$eula = Join-Path $run 'eula.txt'
if (-not ((Test-Path $eula) -and ((Get-Content $eula -Raw) -match 'eula\s*=\s*true'))) {
    throw "mod\run\eula.txt ni sprejet: najprej '.\smoke-server.ps1 -AcceptEula'."
}
$inv = [Globalization.CultureInfo]::InvariantCulture

# Klik v okno konzole (QuickEdit) zamrzne ta skript, dokler ne pritisneš tipke — strežnik pa
# teče naprej in meritev se podaljša (2026-09-25: 36 min). Med tekom QuickEdit izklopimo.
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
$level = 'm5-t4'
$results = @()
$exceptions = 0

function RunOnce([int]$n, [int]$rep) {
    $outLog = Join-Path $logDir "m5-t4-$n-$rep.log"
    if (Test-Path $outLog) {
        try { Remove-Item $outLog -Force -ErrorAction Stop }
        catch { throw "Log $outLog je zaklenjen: prejšnji strežnik še teče. Ustavi ga: Get-Process java | Stop-Process -Force, nato znova." }
    }
    $worldDir = Join-Path $run $level
    if (Test-Path $worldDir) { Remove-Item $worldDir -Recurse -Force }
    $props = @("level-name=$level", 'level-seed=20260925', 'level-type=FLAT', 'generate-structures=false',
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
    $deadline = (Get-Date).AddSeconds(240 + $Seconds + 60)
    $send = { param($cmd) Write-Host "  > $cmd"; $proc.StandardInput.WriteLine($cmd); $proc.StandardInput.Flush() }
    $wait = { param($pattern)
        while ((Get-Date) -lt $deadline -and -not $proc.HasExited) {
            $t = Get-Content $outLog -Raw -ErrorAction SilentlyContinue
            if ($t -and $t -match $pattern) { return $true }
            Start-Sleep -Seconds 2
        }
        return $false }
    try {
        if (-not (& $wait 'Done \(')) { throw "strežnik se ni zagnal ($outLog)" }
        & $send 'gamerule doMobSpawning false'
        & $send 'gamerule doDaylightCycle false'
        & $send 'time set 6000'
        & $send "npcb stress start $n $Radius $Seconds $BreakEvery 0 4 0"
        if (-not (& $wait 'NPCB-STRESS-DONE')) { Write-Host "  NAPAKA  T4 $n/$rep se ni končal v času" }
        & $send 'npcb perf'
        Start-Sleep -Seconds 2
    } finally {
        if (-not $proc.HasExited) {
            & $send 'stop'
            if (-not $proc.WaitForExit(120000)) { try { $proc.Kill() } catch { } }
        }
    }
    $log = Get-Content $outLog -Raw -ErrorAction SilentlyContinue
    if ($log -match 'Exception in server tick loop|Encountered an unexpected exception|Pathing exception|\sat si\.ladja\.npcbaritone\.') {
        $script:exceptions++
        Write-Host "  NAPAKA  izjema v $outLog"
    }
    $csv = Get-ChildItem (Join-Path $run 'npcbaritone\runs') -Filter "t4-$n-*.csv" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime | Select-Object -Last 1
    if (-not $csv) { return $null }
    $dest = Join-Path $outDir $csv.Name
    Copy-Item $csv.FullName $dest
    $row = Import-Csv $dest | Select-Object -First 1
    $row | Add-Member -NotePropertyName rep -NotePropertyValue $rep
    return $row
}

foreach ($n in $Mobs) {
    for ($i = 1; $i -le $Repeats; $i++) {
        Write-Host "== T4 $n mobov, ponovitev $i/$Repeats"
        $r = RunOnce $n $i
        if ($r) {
            $results += $r
            Write-Output ("   glavna nit µs/tick p50={0} p95={1} max={2}; iskanje µs p95={3}; posnetek µs p95={4}; MSPT p95={5}; ciljev={6} doseženih={7} neuspelih={8}" -f `
                $r.main_us_p50, $r.main_us_p95, $r.main_us_max, $r.search_us_p95, $r.snapshot_us_p95, $r.mspt_p95, $r.goals, $r.reached, $r.failed)
        }
    }
}

function Stat($rows, [string]$col) {
    $v = @($rows | ForEach-Object { [double]::Parse($_.$col, $inv) } | Sort-Object)
    if ($v.Count -eq 0) { return $null }
    [pscustomobject]@{ median = $v[[int][math]::Floor(($v.Count - 1) / 2)]; min = $v[0]; max = $v[-1] }
}

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$cols = 'main_us_p50', 'main_us_p95', 'main_us_p99', 'mspt_p95', 'searches_per_s', 'search_us_p50', 'search_us_p95', 'queue_us_p95', 'snapshot_us_p95', 'rejected', 'shared'
$summary = foreach ($n in $Mobs) {
    $rows = @($results | Where-Object { [int]$_.mobs -ge [int]($n * 0.9) -and [int]$_.mobs -le $n })
    foreach ($c in $cols) {
        $s = Stat $rows $c
        if ($s) { [pscustomobject]@{ mobs = $n; metric = $c; median = $s.median; min = $s.min; max = $s.max; repeats = $rows.Count } }
    }
}
$summary | Export-Csv (Join-Path $outDir "t4-summary-$stamp.csv") -NoTypeInformation -Encoding UTF8
$summary | Format-Table mobs, metric, median, min, max, repeats -AutoSize | Out-String | Write-Output

$fails = @()
function Check([string]$what, [bool]$pass) {
    if ($pass) { Write-Output "  OK      $what" } else { Write-Output "  NAPAKA  $what"; $script:fails += $what }
}
$big = [int]($Mobs | Measure-Object -Maximum).Maximum
$main = Stat @($results | Where-Object { [int]$_.mobs -ge [int]($big * 0.9) }) 'main_us_p95'
$snap = Stat $results 'snapshot_us_p95'
$maxThreads = ($results | ForEach-Object { [int]$_.threads_created - [int]$_.threads } | Measure-Object -Maximum).Maximum
if ($main) {
    Check ("A1  (stopnja B) glavna nit p95 pri {0} mobih: mediana {1} µs [{2}, {3}] (<= 5000)" -f $big, $main.median, $main.min, $main.max) ($main.median -le 5000)
    if ($main.median -le 2000) { Write-Output ("  OK      A1+ (stopnja A, pogoj za M7): {0} µs <= 2000" -f $main.median) }
    else { Write-Output ("  ODPRTO  A1+ (stopnja A, pogoj za M7): {0} µs > 2000 — zapisano, ne blokira M6 (D-027)" -f $main.median) }
} else { Check 'A1 rezultati obstajajo' $false }
if ($snap) { Check ("A2  posnetek p95: mediana {0} µs [{1}, {2}] (< 100)" -f $snap.median, $snap.min, $snap.max) ($snap.median -lt 100) }
$noArrivals = @($results | Where-Object { [int]$_.reached -eq 0 }).Count
Check "T4 smiselnost: tekov brez doseženega cilja: $noArrivals (= 0)" ($noArrivals -eq 0)
Check "A3  niti izven bazena: $maxThreads (= 0)" ($maxThreads -le 0)
Check "A4  izjeme v logih: $exceptions (= 0)" ($exceptions -eq 0)
Check "A5  tabela zapisana: docs\meritve\m5\t4-summary-$stamp.csv" ($summary.Count -gt 0)
if ($quickEdit -ne $null) { [void][Npcb.ConsoleMode]::SetConsoleMode($hIn, [uint32]$quickEdit) }
if ($fails.Count -gt 0) { exit 1 }
