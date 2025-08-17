param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("internal", "alpha", "beta", "production")]
    [string]$Track,
    
    [switch]$SkipBuild,
    [switch]$AutoIncrement,
    [string]$ReleaseNotes = "Nouvelle version avec améliorations et corrections de bugs"
)

# Configuration
$ErrorActionPreference = "Stop"
$aabPath = "app\build\outputs\bundle\release\app-release.aab"
$packageName = "com.n8nmonitor.app"
$buildGradlePath = "app\build.gradle.kts"
$serviceAccountPath = "./service-account.json"

Write-Host "[START] Déploiement Google Play Store - Solution Propre" -ForegroundColor Green
Write-Host "[INFO] Track: $Track" -ForegroundColor Cyan
Write-Host "[INFO] Auto-increment: $AutoIncrement" -ForegroundColor Cyan

# Fonction pour incrémenter automatiquement le versionCode
function Update-VersionCode {
    if ($AutoIncrement) {
        Write-Host "[INFO] Incrémentation automatique du versionCode..." -ForegroundColor Yellow
        
        $buildGradleContent = Get-Content $buildGradlePath -Raw
        if ($buildGradleContent -match 'versionCode = (\d+)') {
            $currentVersion = [int]$matches[1]
            $newVersion = $currentVersion + 1
            $buildGradleContent = $buildGradleContent -replace "versionCode = $currentVersion", "versionCode = $newVersion"
            
            # Mise à jour du versionName aussi
            if ($buildGradleContent -match 'versionName = "([^"]+)"') {
                $currentVersionName = $matches[1]
                $versionParts = $currentVersionName.Split('.')
                if ($versionParts.Length -eq 3) {
                    $versionParts[2] = [string]([int]$versionParts[2] + 1)
                    $newVersionName = $versionParts -join '.'
                    $buildGradleContent = $buildGradleContent -replace "versionName = `"$currentVersionName`"", "versionName = `"$newVersionName`""
                    Write-Host "[OK] Version mise à jour: $currentVersionName -> $newVersionName (code: $currentVersion -> $newVersion)" -ForegroundColor Green
                }
            }
            
            Set-Content -Path $buildGradlePath -Value $buildGradleContent -Encoding UTF8
            return $newVersion
        } else {
            Write-Host "[ERROR] Impossible de trouver versionCode dans build.gradle.kts" -ForegroundColor Red
            exit 1
        }
    }
    return $null
}

# Vérifications préliminaires
Write-Host "[CHECK] Vérifications préliminaires..." -ForegroundColor Yellow

# Vérifier service account
if (-not (Test-Path $serviceAccountPath)) {
    Write-Host "[ERROR] Fichier service account introuvable: $serviceAccountPath" -ForegroundColor Red
    exit 1
}
Write-Host "[OK] Service account trouvé" -ForegroundColor Green

# Vérifier Python et dépendances
try {
    $pythonVersion = python --version 2>&1
    Write-Host "[OK] Python disponible: $pythonVersion" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Python non trouvé. Veuillez installer Python." -ForegroundColor Red
    exit 1
}

# Installer/vérifier les dépendances Google API
Write-Host "[INFO] Vérification des dépendances Google API..." -ForegroundColor Yellow
python -m pip install --quiet google-api-python-client google-auth-httplib2 google-auth-oauthlib
Write-Host "[OK] Dépendances Google API prêtes" -ForegroundColor Green

# Incrémenter la version si demandé
$newVersionCode = Update-VersionCode

# Construction de l'AAB
if (-not $SkipBuild) {
    Write-Host "[BUILD] Construction de l'AAB..." -ForegroundColor Yellow
    
    # Clean build pour s'assurer de la fraîcheur
    ./gradlew clean bundleRelease
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Échec de la construction" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "[OK] Construction réussie" -ForegroundColor Green
} else {
    Write-Host "[SKIP] Construction ignorée" -ForegroundColor Yellow
}

# Vérifier que l'AAB existe
if (-not (Test-Path $aabPath)) {
    Write-Host "[ERROR] AAB introuvable: $aabPath" -ForegroundColor Red
    Write-Host "[INFO] Lancez d'abord: ./gradlew bundleRelease" -ForegroundColor Yellow
    exit 1
}

$aabSize = (Get-Item $aabPath).Length / 1MB
Write-Host "[OK] AAB trouvé: $([math]::Round($aabSize, 2)) MB" -ForegroundColor Green

# Script Python optimisé pour le déploiement
$pythonScript = @"
import json
import sys
import os
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload
from googleapiclient.errors import HttpError

def upload_aab(service_account_file, package_name, aab_file, track, release_notes):
    try:
        print('[INFO] Initialisation de l\'authentification Google Play...')
        credentials = service_account.Credentials.from_service_account_file(
            service_account_file,
            scopes=['https://www.googleapis.com/auth/androidpublisher']
        )
        
        service = build('androidpublisher', 'v3', credentials=credentials)
        
        print('[INFO] Création d\'une nouvelle édition...')
        edit_request = service.edits().insert(body={}, packageName=package_name)
        edit_result = edit_request.execute()
        edit_id = edit_result['id']
        print(f'[OK] Édition créée: {edit_id}')
        
        print('[UPLOAD] Téléchargement de l\'AAB...')
        media = MediaFileUpload(aab_file, mimetype='application/octet-stream')
        upload_request = service.edits().bundles().upload(
            editId=edit_id,
            packageName=package_name,
            media_body=media
        )
        upload_result = upload_request.execute()
        version_code = upload_result['versionCode']
        print(f'[OK] AAB téléchargé avec succès. Code de version: {version_code}')
        
        print(f'[TRACK] Attribution au track {track}...')
        track_body = {
            'track': track,
            'releases': [{
                'versionCodes': [version_code],
                'status': 'draft',
                'releaseNotes': [{
                    'language': 'fr-FR',
                    'text': release_notes
                }, {
                    'language': 'en-US', 
                    'text': release_notes
                }]
            }]
        }
        
        track_request = service.edits().tracks().update(
            editId=edit_id,
            packageName=package_name,
            track=track,
            body=track_body
        )
        track_result = track_request.execute()
        print(f'[OK] Assigné au track {track}')
        
        print('[COMMIT] Validation des changements...')
        commit_request = service.edits().commit(
            editId=edit_id,
            packageName=package_name
        )
        commit_result = commit_request.execute()
        
        print('[SUCCESS] 🎉 Déploiement réussi!')
        print(f'[INFO] Version {version_code} déployée sur le track {track}')
        return True, version_code
        
    except HttpError as e:
        error_content = e.content.decode('utf-8') if e.content else str(e)
        print(f'[ERROR] Erreur HTTP Google Play: {error_content}')
        
        if 'already been used' in error_content:
            print('[SOLUTION] Le code de version existe déjà. Utilisez -AutoIncrement pour l\'incrémenter automatiquement.')
        elif 'does not have permission' in error_content:
            print('[SOLUTION] Vérifiez les permissions du compte de service dans Google Play Console.')
        
        return False, None
    except Exception as e:
        print(f'[ERROR] Erreur inattendue: {str(e)}')
        return False, None

if __name__ == '__main__':
    if len(sys.argv) != 6:
        print('Usage: python deploy.py <service_account> <package_name> <aab_file> <track> <release_notes>')
        sys.exit(1)
    
    service_account_file = sys.argv[1]
    package_name = sys.argv[2]
    aab_file = sys.argv[3]
    track = sys.argv[4]
    release_notes = sys.argv[5]
    
    success, version_code = upload_aab(service_account_file, package_name, aab_file, track, release_notes)
    sys.exit(0 if success else 1)
"@

# Écriture et exécution du script Python
$pythonScriptPath = "deploy_helper.py"
$pythonScript | Out-File -FilePath $pythonScriptPath -Encoding UTF8

Write-Host "[DEPLOY] Lancement du déploiement..." -ForegroundColor Yellow

try {
    python $pythonScriptPath $serviceAccountPath $packageName $aabPath $Track $ReleaseNotes
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[SUCCESS] 🎉 Déploiement terminé avec succès!" -ForegroundColor Green
        Write-Host "" 
        Write-Host "[NEXT STEPS] Étapes suivantes:" -ForegroundColor Cyan
        Write-Host "  1. 🔍 Vérifiez Google Play Console: https://play.google.com/console" -ForegroundColor White
        Write-Host "  2. 📊 Surveillez les métriques et rapports de plantage" -ForegroundColor White
        Write-Host "  3. 👥 Testez avec votre équipe (track internal/alpha)" -ForegroundColor White
        Write-Host "  4. 🚀 Promouvez vers le track suivant si tout va bien" -ForegroundColor White
        Write-Host ""
        Write-Host "[INFO] Package: $packageName" -ForegroundColor Gray
        Write-Host "[INFO] Track: $Track" -ForegroundColor Gray
        if ($newVersionCode) {
            Write-Host "[INFO] Nouveau code de version: $newVersionCode" -ForegroundColor Gray
        }
    } else {
        Write-Host "[ERROR] ❌ Échec du déploiement" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "[ERROR] Erreur lors de l'exécution: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
} finally {
    # Nettoyage
    Remove-Item $pythonScriptPath -Force -ErrorAction SilentlyContinue
}

Write-Host "[END] Script terminé" -ForegroundColor Green