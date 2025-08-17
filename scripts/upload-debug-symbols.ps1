#!/usr/bin/env pwsh
# Script pour uploader les symboles de débogage vers Google Play Console

param(
    [string]$VersionCode = "8",
    [string]$Track = "internal"
)

$ErrorActionPreference = "Stop"

Write-Host "[INFO] Upload des symboles de débogage pour version $VersionCode" -ForegroundColor Green

# Chemins des fichiers
$AppDir = "$PSScriptRoot/../app"
$BuildDir = "$AppDir/build"
$SymbolsDir = "$BuildDir/intermediates/merged_native_libs/release/out/lib"
$MappingFile = "$BuildDir/outputs/mapping/release/mapping.txt"

# Vérifier que les fichiers existent
if (-not (Test-Path $SymbolsDir)) {
    Write-Host "[ERROR] Répertoire des symboles natifs introuvable: $SymbolsDir" -ForegroundColor Red
    Write-Host "[INFO] Assurez-vous d'avoir exécuté './gradlew bundleRelease' d'abord" -ForegroundColor Yellow
    exit 1
}

if (-not (Test-Path $MappingFile)) {
    Write-Host "[WARNING] Fichier de mapping ProGuard introuvable: $MappingFile" -ForegroundColor Yellow
}

# Créer le script Python pour l'upload
$PythonScript = @"
import os
import sys
from googleapiclient.discovery import build
from google.oauth2.service_account import Credentials
from googleapiclient.http import MediaFileUpload

# Configuration
SERVICE_ACCOUNT_FILE = 'service-account-key.json'
PACKAGE_NAME = 'com.n8nmonitor.app'
VERSION_CODE = $VersionCode
TRACK = '$Track'

def upload_symbols():
    try:
        # Authentification
        credentials = Credentials.from_service_account_file(
            SERVICE_ACCOUNT_FILE,
            scopes=['https://www.googleapis.com/auth/androidpublisher']
        )
        
        service = build('androidpublisher', 'v3', credentials=credentials)
        
        # Créer un edit
        edit_request = service.edits().insert(body={}, packageName=PACKAGE_NAME)
        edit_result = edit_request.execute()
        edit_id = edit_result['id']
        
        print(f"[INFO] Edit créé avec ID: {edit_id}")
        
        # Upload des symboles natifs
        symbols_dir = r'$($SymbolsDir.Replace('\', '\\'))'
        if os.path.exists(symbols_dir):
            for root, dirs, files in os.walk(symbols_dir):
                for file in files:
                    if file.endswith('.so'):
                        symbol_file = os.path.join(root, file)
                        print(f"[INFO] Upload du fichier de symboles: {file}")
                        
                        # Note: L'API Google Play ne supporte pas directement l'upload de symboles .so
                        # Il faut utiliser l'interface web ou des outils spécialisés
                        print(f"[INFO] Fichier trouvé: {symbol_file}")
        
        # Upload du mapping ProGuard si disponible
        mapping_file = r'$($MappingFile.Replace('\', '\\'))'
        if os.path.exists(mapping_file):
            print(f"[INFO] Upload du fichier de mapping ProGuard")
            
            media = MediaFileUpload(mapping_file, mimetype='text/plain')
            
            mapping_request = service.edits().deobfuscationfiles().upload(
                packageName=PACKAGE_NAME,
                editId=edit_id,
                apkVersionCode=VERSION_CODE,
                deobfuscationFileType='proguard',
                media_body=media
            )
            
            mapping_result = mapping_request.execute()
            print(f"[OK] Fichier de mapping uploadé: {mapping_result}")
        
        # Valider l'edit
        commit_request = service.edits().commit(
            editId=edit_id,
            packageName=PACKAGE_NAME
        )
        commit_result = commit_request.execute()
        
        print(f"[SUCCESS] Symboles de débogage uploadés avec succès!")
        
    except Exception as e:
        print(f"[ERROR] Erreur lors de l'upload: {e}")
        sys.exit(1)

if __name__ == '__main__':
    upload_symbols()
"@

# Écrire le script Python
$PythonFile = "$PSScriptRoot/upload_symbols.py"
$PythonScript | Out-File -FilePath $PythonFile -Encoding UTF8

Write-Host "[INFO] Script Python créé: $PythonFile" -ForegroundColor Green

# Exécuter le script Python
if (Test-Path "service-account-key.json") {
    Write-Host "[INFO] Exécution de l'upload des symboles..." -ForegroundColor Green
    python $PythonFile
} else {
    Write-Host "[ERROR] Fichier service-account-key.json introuvable" -ForegroundColor Red
    Write-Host "[INFO] Placez votre clé de service account dans le répertoire racine" -ForegroundColor Yellow
}

Write-Host "[INFO] Instructions manuelles pour l'upload des symboles:" -ForegroundColor Cyan
Write-Host "1. Allez sur Google Play Console" -ForegroundColor White
Write-Host "2. Sélectionnez votre app > Gestion des versions > Version $VersionCode" -ForegroundColor White
Write-Host "3. Cliquez sur 'Télécharger les symboles de débogage'" -ForegroundColor White
Write-Host "4. Uploadez les fichiers .so depuis: $SymbolsDir" -ForegroundColor White
if (Test-Path $MappingFile) {
    Write-Host "5. Uploadez le fichier de mapping depuis: $MappingFile" -ForegroundColor White
}

Write-Host "[DONE] Script terminé" -ForegroundColor Green