# M2.9 — tečaj T1 + speedtest na dedicated strežniku, merila M2 A1–A5.
#
# Zagon:  .\t1-run.ps1              (po prvem .\smoke-server.ps1 -AcceptEula)
#
# Svež superflat svet 'm2-t1'; husk (ne gori na soncu, 0,6 x 1,95 kot zombi) se pripne kot
# puppet in prehodi 10 odsekov T1 (/npcb course t1 run), nato 10 s hoje in 10 s sprinta
# (/npcb speedtest). Rezultati: docs\meritve\m2\t1-*.csv, speed-*.csv; log docs\build-logs\m2-t1.log.
# Exit code 0 = merila A1–A5 izpolnjena.
param([int]$TimeoutSec = 900)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run  = Join-Path $root 'mod\run'
$logDir = Join-Path $root 'docs\build-logs'
$outDir = Join-Path $root 'docs\meritve\m2'
New-Item -ItemType Directory -Force -Path $run, $logDir, $outDir | Out-Null
$outLog = Join-Path $logDir 'm2-t1.log'
if (Test-Path $outLog) { Remove-Item $outLog -Force }

$candidates = @($env:NPCB_JAVA8_HOME, (Join-Path $root '..\customNPC_rework\.tools\jdk8'), 'C:\Program Files\Java\jdk1.8.0_202') | Where-Object { $_ }
$jdk = $candidates | Where-Object { Test-Path (Join-Path $_ 'bin\javac.exe') } | Select-Object -First 1
if (-not $jdk) { throw 'Java 8 JDK ni najden (glej dev.ps1).' }
$env:JAVA_HOME = (Resolve-Path $jdk).Path
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

$eula = Join-Path $run 'eula.txt'
if (-not ((Test-Path $eula) -and ((Get-Content $eula -Raw) -match 'eula\s*=\s*true'))) {
    throw "mod\run\eula.txt ni sprejet: najprej '.\smoke-server.ps1 -AcceptEula'."
}
$level = 'm2-t1'
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
    Send 'time set 6000'
    # Brez igralca spawn chunk ni nujno naložen; postavitev T1 ga naloži pred /summon.
    Send 'npcb course t1 build 0 4 0'
    if (-not (WaitFor 'T1 postavljen pri')) { throw 'T1 ni bilo mogoče postaviti' }
    Send 'summon husk 0.5 4 0.5 {PersistenceRequired:1b}'
    Start-Sleep -Seconds 2
    Send 'npcb course t1 run @e[type=husk,c=1] 0 4 0'
    if (-not (WaitFor 'NPCB-COURSE-DONE')) { $ok = $false; Write-Output '  NAPAKA  T1 se ni končal v času' }
    Send 'tp @e[type=husk,c=1] 0.5 4 -20.5 -90 0'
    Start-Sleep -Seconds 1
    Send 'npcb speedtest @e[type=husk,c=1] walk'
    if (-not (WaitFor 'NPCB-SPEEDTEST-DONE' 1)) { $ok = $false; Write-Output '  NAPAKA  speedtest hoja se ni končal' }
    Send 'tp @e[type=husk,c=1] 0.5 4 -30.5 -90 0'
    Start-Sleep -Seconds 1
    Send 'npcb speedtest @e[type=husk,c=1] sprint'
    if (-not (WaitFor 'NPCB-SPEEDTEST-DONE' 2)) { $ok = $false; Write-Output '  NAPAKA  speedtest sprint se ni končal' }
    Send 'npcb chunks'
} finally {
    if (-not $proc.HasExited) {
        Send 'stop'
        if (-not $proc.WaitForExit(120000)) { try { $proc.Kill() } catch { } }
    }
}

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$log = LogText
$fails = @()
function Check([string]$what, [bool]$pass) {
    if ($pass) { Write-Output "  OK      $what" } else { Write-Output "  NAPAKA  $what"; $script:fails += $what }
}

# T1 CSV
$csv = Get-ChildItem (Join-Path $run 'npcbaritone\runs') -Filter 't1-*.csv' -ErrorAction SilentlyContinue | Sort-Object LastWriteTime | Select-Object -Last 1
if ($csv) {
    Copy-Item $csv.FullName (Join-Path $outDir $csv.Name)
    $rows = Import-Csv $csv.FullName
    $passed = @($rows | Where-Object { $_.pass -eq 'true' }).Count
    $loads = ($rows | Measure-Object -Property chunk_loads -Sum).Sum
    $seg10 = $rows | Where-Object { $_.segment -eq '10' }
    Check "A1 T1 odsekov OK: $passed/10 (>= 9)" ($passed -ge 9)
    Check "A1 odsek 10 konča FAILED, ne tava: $($seg10.result)" ($seg10 -and $seg10.result -eq 'FAILED')
    Check "A4 naloženih chunkov med T1: $loads (= 0)" ($loads -eq 0)
    $rows | Format-Table segment, name, result, pass, ticks, meters, max_fall, chunk_loads, yaw_jitter -AutoSize | Out-String | Write-Output
} else {
    Check 'T1 CSV obstaja' $false
}

# speedtest
$speed = @([regex]::Matches($log, 'NPCB-SPEEDTEST-DONE speedtest (hoja|sprint): ([0-9.]+) m/s.*?tresenje yaw>90/5t: (\d+)') | ForEach-Object {
    [pscustomobject]@{ mode = $_.Groups[1].Value; mps = [double]::Parse($_.Groups[2].Value, [Globalization.CultureInfo]::InvariantCulture); jitter = [int]$_.Groups[3].Value }
})
$speed | Export-Csv (Join-Path $outDir "speed-$stamp.csv") -NoTypeInformation -Encoding UTF8
$walk = $speed | Where-Object { $_.mode -eq 'hoja' } | Select-Object -First 1
$sprint = $speed | Where-Object { $_.mode -eq 'sprint' } | Select-Object -First 1
Check ("A2 hoja {0:N3} m/s (4,32 +- 0,2)" -f $walk.mps) ($walk -and [math]::Abs($walk.mps - 4.32) -le 0.2)
Check ("A3 sprint {0:N3} m/s (5,6 +- 0,25)" -f $sprint.mps) ($sprint -and [math]::Abs($sprint.mps - 5.6) -le 0.25)
Check "A5 tresenje yaw (hoja+sprint): $(($speed | Measure-Object -Property jitter -Sum).Sum) (= 0)" ((($speed | Measure-Object -Property jitter -Sum).Sum) -eq 0)
Check 'brez izjem v logu' (-not ($log -match 'Exception in server tick loop|Encountered an unexpected exception|Pathing exception'))
Write-Output "log: $outLog"
Write-Output "rezultati: $outDir"
if (-not $ok -or $fails.Count -gt 0) { exit 1 }
