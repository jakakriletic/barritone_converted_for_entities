# NPC Baritone — Gradle z Javo 8 (D-007, M0.2). Vzorec: customNPC_rework\dev.ps1.
#
#   .\dev.ps1 setupDecompWorkspace          # prvič (brez --offline)
#   .\dev.ps1 build --offline               # prevod + JUnit + reobf jar -> mod\build\libs
#   .\dev.ps1 test --offline
#   .\dev.ps1 runClient --offline           # pred zagonom prenese manjkajoče assets
#   .\dev.ps1 runServer --offline --no-daemon
#
# Java 8 po vrsti: $env:NPCB_JAVA8_HOME, ..\customNPC_rework\.tools\jdk8,
# C:\Program Files\Java\jdk1.8.0_202. Sistemske Java nastavitve se ne spreminjajo.
param([Parameter(ValueFromRemainingArguments=$true)][string[]]$GradleArgs)
$ErrorActionPreference = 'Stop'

$candidates = @(
    $env:NPCB_JAVA8_HOME,
    (Join-Path $PSScriptRoot '..\customNPC_rework\.tools\jdk8'),
    'C:\Program Files\Java\jdk1.8.0_202'
) | Where-Object { $_ }
$jdk = $candidates | Where-Object { Test-Path (Join-Path $_ 'bin\javac.exe') } | Select-Object -First 1
if (-not $jdk) { throw "Java 8 JDK ni najden. Nastavi NPCB_JAVA8_HOME. Preverjeno: $($candidates -join '; ')" }
$env:JAVA_HOME = (Resolve-Path $jdk).Path
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Write-Output "JAVA_HOME = $env:JAVA_HOME"

if ($GradleArgs -contains 'runClient') { & (Join-Path $PSScriptRoot 'prepare-assets.ps1') }

Push-Location (Join-Path $PSScriptRoot 'mod')
try {
    if (-not $GradleArgs) { $GradleArgs = @('tasks', '--offline') }
    & .\gradlew.bat @GradleArgs
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
} finally { Pop-Location }
