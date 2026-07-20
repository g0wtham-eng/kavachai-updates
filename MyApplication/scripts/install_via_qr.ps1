# install_via_qr.ps1
# PowerShell script to host the APK via a local HTTP server and generate a QR code for download.
# Prerequisites: Python (for http.server) and 'qrencode' (or an online API) installed on the dev machine.

# 1. Start a simple HTTP server serving the app's build outputs.
$port = 8000
$projectRoot = "C:\kavachai_final\MyApplication"
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
Write-Host "Starting HTTP server on port $port serving $projectRoot"
Push-Location $projectRoot
Start-Process -NoNewWindow -FilePath "python" -ArgumentList "-m", "http.server", "$port"

# 2. Determine the local IP address (assumes Wi‑Fi interface).
$ip = (Get-NetIPConfiguration | Where-Object {$_.IPv4DefaultGateway -ne $null}).IPv4Address.IPAddress
Write-Host "Local IP address: $ip"

# 3. Build the download URL.
$downloadUrl = "http://$ip:$port/$apkPath"
Write-Host "Download URL: $downloadUrl"

# 4. Generate QR code. If 'qrencode' is installed, use it; otherwise fall back to online API.
if (Get-Command qrencode -ErrorAction SilentlyContinue) {
    qrencode -o install_qr.png "$downloadUrl"
    Write-Host "QR code generated: install_qr.png"
} else {
    # Using Google Chart API as a simple fallback.
    $qrUrl = "https://chart.googleapis.com/chart?chs=300x300&cht=qr&chl=$([uri]::EscapeDataString($downloadUrl))"
    Invoke-WebRequest -Uri $qrUrl -OutFile "install_qr.png"
    Write-Host "QR code generated via Google Chart API: install_qr.png"
}

Pop-Location
Write-Host "Done. Scan 'install_qr.png' on your phone to download and install the APK."
