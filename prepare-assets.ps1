$ErrorActionPreference = 'Stop'
$assetRoot = Join-Path $env:USERPROFILE '.gradle\caches\minecraft\assets'
$indexFile = Join-Path $assetRoot 'indexes\1.12.json'
if (-not (Test-Path $indexFile)) { throw 'Run dev.ps1 setupDecompWorkspace first to obtain the asset index.' }
$index = Get-Content $indexFile -Raw | ConvertFrom-Json
$missing = @($index.objects.PSObject.Properties | ForEach-Object { $_.Value } | Where-Object {
    $path = Join-Path $assetRoot ('objects\' + $_.hash.Substring(0,2) + '\' + $_.hash)
    -not (Test-Path $path) -or (Get-Item $path).Length -ne $_.size
} | Sort-Object hash -Unique)
Write-Output "Downloading $($missing.Count) missing Minecraft 1.12 assets over HTTPS."
# Sequential intentionally: works in both Windows PowerShell 5.1 and PowerShell 7.
$completed = 0
foreach ($asset in $missing) {
    $prefix = $asset.hash.Substring(0,2)
    $folder = Join-Path $assetRoot "objects\$prefix"
    New-Item -ItemType Directory -Force $folder | Out-Null
    $target = Join-Path $folder $asset.hash
    $temp = "$target.download-$PID"
    Invoke-WebRequest "https://resources.download.minecraft.net/$prefix/$($asset.hash)" -OutFile $temp -UseBasicParsing
    if ((Get-FileHash $temp -Algorithm SHA1).Hash.ToLowerInvariant() -ne $asset.hash) {
        throw "Asset hash mismatch: $($asset.hash)"
    }
    Move-Item -LiteralPath $temp -Destination $target -Force
    $completed++
    if ($completed % 25 -eq 0) { Write-Output "$completed / $($missing.Count) assets downloaded." }
}
Write-Output 'Asset download complete.'
