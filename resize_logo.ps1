Add-Type -AssemblyName System.Drawing
$sourcePath = "C:\Users\PC\.gemini\antigravity\brain\c4869e02-ebad-4e27-823e-9c97d46b5af5\nova_assistant_logo_1789135280658.jpg"
$baseDir = "C:\kavachai_final\MyApplication\app\src\main\res"

$sizes = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

$img = [System.Drawing.Image]::FromFile($sourcePath)

foreach ($folder in $sizes.Keys) {
    $size = $sizes[$folder]
    $destFolder = Join-Path $baseDir $folder
    if (!(Test-Path $destFolder)) { New-Item -ItemType Directory -Path $destFolder | Out-Null }
    
    $bmp = New-Object System.Drawing.Bitmap $size, $size
    $graph = [System.Drawing.Graphics]::FromImage($bmp)
    $graph.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graph.DrawImage($img, 0, 0, $size, $size)
    
    $iconPath1 = Join-Path $destFolder "ic_launcher.png"
    $iconPath2 = Join-Path $destFolder "ic_launcher_round.png"
    
    $bmp.Save($iconPath1, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Save($iconPath2, [System.Drawing.Imaging.ImageFormat]::Png)
    
    $graph.Dispose()
    $bmp.Dispose()
}

$img.Dispose()
Write-Host "Icons updated successfully"
