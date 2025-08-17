param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("internal", "alpha", "beta", "production")]
    [string]$Track,
    
    [switch]$SkipBuild,
    [switch]$SkipTests
)

# Configuration
$ErrorActionPreference = "Stop"
$aabPath = "app\build\outputs\bundle\release\app-release.aab"
$packageName = "com.n8nmonitor.app"

Write-Host "[START] Direct upload to Google Play Store" -ForegroundColor Green
Write-Host "[INFO] Track: $Track" -ForegroundColor Cyan

# Check if AAB exists
if (-not (Test-Path $aabPath)) {
    Write-Host "[ERROR] AAB file not found at $aabPath" -ForegroundColor Red
    Write-Host "Please build the AAB first or use -SkipBuild $false" -ForegroundColor Yellow
    exit 1
}

Write-Host "[OK] AAB found at: $aabPath" -ForegroundColor Green
$aabSize = (Get-Item $aabPath).Length / 1MB
Write-Host "[INFO] AAB size: $([math]::Round($aabSize, 2)) MB" -ForegroundColor Cyan

# Check service account
$serviceAccountPath = "./service-account.json"
if (-not (Test-Path $serviceAccountPath)) {
    Write-Host "[ERROR] Service account file not found at $serviceAccountPath" -ForegroundColor Red
    exit 1
}

Write-Host "[OK] Service account found" -ForegroundColor Green

# Install Google Play Console API client if not available
try {
    Import-Module GooglePlayConsole -ErrorAction Stop
    Write-Host "[OK] Google Play Console module available" -ForegroundColor Green
} catch {
    Write-Host "[INFO] Installing Google Play Console tools..." -ForegroundColor Yellow
    
    # Try to install via pip if available
    try {
        python -m pip install google-api-python-client google-auth-httplib2 google-auth-oauthlib
        Write-Host "[OK] Google API client installed" -ForegroundColor Green
    } catch {
        Write-Host "[WARNING] Could not install Google API client via pip" -ForegroundColor Yellow
        Write-Host "[INFO] Attempting manual upload process..." -ForegroundColor Yellow
    }
}

# Create a simple Python script for upload
$pythonScript = @"
import json
import sys
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

def upload_aab(service_account_file, package_name, aab_file, track):
    try:
        # Load service account credentials
        credentials = service_account.Credentials.from_service_account_file(
            service_account_file,
            scopes=['https://www.googleapis.com/auth/androidpublisher']
        )
        
        # Build the service
        service = build('androidpublisher', 'v3', credentials=credentials)
        
        # Start edit
        edit_request = service.edits().insert(body={}, packageName=package_name)
        edit_result = edit_request.execute()
        edit_id = edit_result['id']
        
        print(f'[INFO] Created edit with ID: {edit_id}')
        
        # Upload AAB
        media = MediaFileUpload(aab_file, mimetype='application/octet-stream')
        upload_request = service.edits().bundles().upload(
            editId=edit_id,
            packageName=package_name,
            media_body=media
        )
        upload_result = upload_request.execute()
        
        print(f'[OK] AAB uploaded successfully. Version code: {upload_result["versionCode"]}')
        
        # Assign to track
        track_request = service.edits().tracks().update(
            editId=edit_id,
            packageName=package_name,
            track=track,
            body={
                'track': track,
                'releases': [{
                    'versionCodes': [upload_result['versionCode']],
                    'status': 'draft'
                }]
            }
        )
        track_result = track_request.execute()
        
        print(f'[OK] Assigned to {track} track')
        
        # Commit the edit
        commit_request = service.edits().commit(
            editId=edit_id,
            packageName=package_name
        )
        commit_result = commit_request.execute()
        
        print('[SUCCESS] Upload completed successfully!')
        return True
        
    except Exception as e:
        print(f'[ERROR] Upload failed: {str(e)}')
        return False

if __name__ == '__main__':
    if len(sys.argv) != 5:
        print('Usage: python upload.py <service_account_file> <package_name> <aab_file> <track>')
        sys.exit(1)
    
    service_account_file = sys.argv[1]
    package_name = sys.argv[2]
    aab_file = sys.argv[3]
    track = sys.argv[4]
    
    success = upload_aab(service_account_file, package_name, aab_file, track)
    sys.exit(0 if success else 1)
"@

# Write Python script
$pythonScriptPath = "upload_helper.py"
$pythonScript | Out-File -FilePath $pythonScriptPath -Encoding UTF8

Write-Host "[INFO] Created Python upload helper script" -ForegroundColor Yellow

# Try to upload using Python
try {
    Write-Host "[UPLOAD] Starting upload to $Track track..." -ForegroundColor Yellow
    python $pythonScriptPath $serviceAccountPath $packageName $aabPath $Track
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[SUCCESS] Upload completed successfully!" -ForegroundColor Green
        
        # Clean up
        Remove-Item $pythonScriptPath -Force
        
        Write-Host "[INFO] Next steps:" -ForegroundColor Cyan
        Write-Host "  1. Check Google Play Console for the new release" -ForegroundColor White
        Write-Host "  2. Monitor crash reports and user feedback" -ForegroundColor White
        Write-Host "  3. Consider promoting to next track if metrics are good" -ForegroundColor White
    } else {
        Write-Host "[ERROR] Upload failed" -ForegroundColor Red
        Remove-Item $pythonScriptPath -Force -ErrorAction SilentlyContinue
        exit 1
    }
} catch {
    Write-Host "[ERROR] Python execution failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "[INFO] Make sure Python is installed and Google API libraries are available" -ForegroundColor Yellow
    Remove-Item $pythonScriptPath -Force -ErrorAction SilentlyContinue
    exit 1
}