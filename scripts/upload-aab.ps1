# Upload AAB PowerShell Script for n8n Monitor Android App
# This script builds and uploads the Android App Bundle to Google Play Store on Windows

param(
    [string]$Track = "internal",
    [switch]$SkipBuild,
    [switch]$SkipTests,
    [switch]$Verbose,
    [switch]$Help
)

if ($Help) {
    Write-Host "Usage: .\upload-aab.ps1 [OPTIONS]"
    Write-Host "Options:"
    Write-Host "  -Track TRACK       Upload track (internal, alpha, beta, production) [default: internal]"
    Write-Host "  -SkipBuild         Skip the build process and use existing AAB"
    Write-Host "  -SkipTests         Skip running tests before build"
    Write-Host "  -Verbose           Enable verbose output"
    Write-Host "  -Help              Show this help message"
    exit 0
}

Write-Host "[START] Starting AAB build and upload process..." -ForegroundColor Green

# Validate track
if ($Track -notin @("internal", "alpha", "beta", "production")) {
    Write-Host "[ERROR] Invalid track '$Track'. Must be one of: internal, alpha, beta, production" -ForegroundColor Red
    exit 1
}

Write-Host "[OK] Using track: $Track" -ForegroundColor Green

# Check required environment variables
Write-Host "[ENV] Checking environment variables..." -ForegroundColor Yellow
$googlePlayKeyPath = $env:GOOGLE_PLAY_JSON_KEY_PATH
$googlePlayKeyData = $env:GOOGLE_PLAY_JSON_KEY_DATA

if (-not $googlePlayKeyPath -and -not $googlePlayKeyData) {
    Write-Host "[ERROR] Google Play service account key not configured!" -ForegroundColor Red
    Write-Host "Please set either:"
    Write-Host "  GOOGLE_PLAY_JSON_KEY_PATH - path to service account JSON file"
    Write-Host "  GOOGLE_PLAY_JSON_KEY_DATA - service account JSON data"
    exit 1
}

if ($googlePlayKeyPath -and -not (Test-Path $googlePlayKeyPath)) {
    Write-Host "[ERROR] Google Play service account JSON file not found: $googlePlayKeyPath" -ForegroundColor Red
    exit 1
}

Write-Host "[OK] Google Play credentials configured" -ForegroundColor Green

# Check if fastlane is installed
try {
    $null = Get-Command fastlane -ErrorAction Stop
    Write-Host "[OK] fastlane is installed" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] fastlane is not installed!" -ForegroundColor Red
    Write-Host "Install it with: gem install fastlane"
    exit 1
}

# Check Java version
Write-Host "[JAVA] Checking Java version..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-String '"(\d+)' | ForEach-Object { $_.Matches[0].Groups[1].Value }
    if ($javaVersion -eq "17") {
        Write-Host "[OK] Java 17 is configured" -ForegroundColor Green
    } else {
        Write-Host "[WARNING] Java version is $javaVersion, but Java 17 is recommended" -ForegroundColor Yellow
    }
} catch {
    Write-Host "[ERROR] Java not found in PATH!" -ForegroundColor Red
    exit 1
}

# Run security audit
if (-not $SkipTests) {
    Write-Host "[SECURITY] Running security audit..." -ForegroundColor Yellow
    if (Test-Path "scripts\security-audit.sh") {
        try {
            bash scripts/security-audit.sh
        } catch {
            Write-Host "[WARNING] Security audit failed or bash not available" -ForegroundColor Yellow
        }
    } else {
        Write-Host "[WARNING] Security audit script not found" -ForegroundColor Yellow
    }
}

# Run tests
if (-not $SkipTests) {
    Write-Host "[TEST] Running tests..." -ForegroundColor Yellow
    try {
        if ($Verbose) {
            .\gradlew.bat test --info
        } else {
            .\gradlew.bat test
        }
        Write-Host "[OK] Tests passed" -ForegroundColor Green
    } catch {
        Write-Host "[ERROR] Tests failed!" -ForegroundColor Red
        exit 1
    }
}

# Build AAB
if (-not $SkipBuild) {
    Write-Host "[BUILD] Building release AAB..." -ForegroundColor Yellow
    try {
        if ($Verbose) {
            .\gradlew.bat bundleRelease --info
        } else {
            .\gradlew.bat bundleRelease
        }
        Write-Host "[OK] AAB built successfully" -ForegroundColor Green
    } catch {
        Write-Host "[ERROR] Build failed!" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "[SKIP] Skipping build, using existing AAB" -ForegroundColor Yellow
}

# Check if AAB exists
$aabPath = "app\build\outputs\bundle\release\app-release.aab"
if (-not (Test-Path $aabPath)) {
    Write-Host "[ERROR] AAB file not found at $aabPath" -ForegroundColor Red
    Write-Host "Make sure the build completed successfully"
    exit 1
}

Write-Host "[OK] AAB found at: $aabPath" -ForegroundColor Green
$aabSize = (Get-Item $aabPath).Length / 1MB
Write-Host "[INFO] AAB size: $([math]::Round($aabSize, 2)) MB" -ForegroundColor Cyan

# Upload to Google Play Store
Write-Host "[UPLOAD] Uploading to Google Play Store ($Track track)..." -ForegroundColor Yellow
try {
    switch ($Track) {
        "internal" { fastlane android internal }
        "alpha" { fastlane android alpha }
        "beta" { fastlane android beta }
        "production" { fastlane android production }
    }
    Write-Host "[OK] Upload completed successfully!" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Upload failed!" -ForegroundColor Red
    exit 1
}

Write-Host "[SUCCESS] AAB has been uploaded to Google Play Store ($Track track)" -ForegroundColor Green
Write-Host "[APP] You can now test the app or promote it to the next track in the Google Play Console" -ForegroundColor Cyan

# Optional: Open Google Play Console
try {
    Start-Process "https://play.google.com/console/developers"
    Write-Host "[WEB] Opening Google Play Console..." -ForegroundColor Cyan
} catch {
    Write-Host "[WEB] Google Play Console: https://play.google.com/console/developers" -ForegroundColor Cyan
}