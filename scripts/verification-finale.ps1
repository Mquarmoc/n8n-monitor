# Script de Vérification Finale - n8n Monitor v1.0.1
# Automatise les vérifications et actions restantes pour la production

param(
    [switch]$SkipBuild,
    [switch]$CheckOnly,
    [string]$ServiceAccountPath = "",
    [switch]$Help
)

if ($Help) {
    Write-Host "Usage: .\verification-finale.ps1 [OPTIONS]"
    Write-Host "Options:"
    Write-Host "  -SkipBuild              Skip AAB build verification"
    Write-Host "  -CheckOnly              Only run checks, no actions"
    Write-Host "  -ServiceAccountPath     Path to service account JSON"
    Write-Host "  -Help                   Show this help"
    exit 0
}

Write-Host "[DEPLOY] n8n Monitor - Verification Finale v1.0.1" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

$ErrorCount = 0
$WarningCount = 0

# Fonction pour afficher les résultats
function Show-Status {
    param(
        [string]$Item,
        [string]$Status,
        [string]$Color = "Green",
        [string]$Details = ""
    )
    
    $Icon = switch ($Status) {
        "OK" { "[OK]" }
        "WARNING" { "[WARN]" }
        "ERROR" { "[ERROR]" }
        "INFO" { "[INFO]" }
        default { "[CHECK]" }
    }
    
    Write-Host "$Icon $Item" -ForegroundColor $Color
    if ($Details) {
        Write-Host "   $Details" -ForegroundColor Gray
    }
}

# 1. Verification Build AAB
Write-Host "[BUILD] 1. Verification Build Release AAB" -ForegroundColor Yellow
if (-not $SkipBuild) {
    if (Test-Path "app\build\outputs\bundle\release\app-release.aab") {
        $aabSize = (Get-Item "app\build\outputs\bundle\release\app-release.aab").Length / 1MB
        Show-Status "AAB généré" "OK" "Green" "Taille: $([math]::Round($aabSize, 1)) MB"
    } else {
        Show-Status "AAB manquant" "ERROR" "Red" "Exécuter: ./gradlew bundleRelease"
        $ErrorCount++
    }
} else {
    Show-Status "Build AAB" "INFO" "Cyan" "Vérification ignorée (-SkipBuild)"
}

# 2. Verification Keystore
Write-Host "\n[KEYSTORE] 2. Verification Keystore" -ForegroundColor Yellow
if (Test-Path "release.keystore") {
    Show-Status "Keystore présent" "OK" "Green" "release.keystore trouvé"
} else {
    Show-Status "Keystore manquant" "ERROR" "Red" "Créer avec: keytool -genkey..."
    $ErrorCount++
}

if (Test-Path "keystore.properties") {
    Show-Status "Configuration keystore" "OK" "Green" "keystore.properties trouvé"
} else {
    Show-Status "Configuration manquante" "ERROR" "Red" "Créer keystore.properties"
    $ErrorCount++
}

# 3. Verification Configuration JDK
Write-Host "\n[JDK] 3. Verification Configuration JDK" -ForegroundColor Yellow
$gradleProps = Get-Content "gradle.properties" -ErrorAction SilentlyContinue
if ($gradleProps -match "org.gradle.java.home.*jdk-17") {
    Show-Status "JDK 17 configuré" "OK" "Green" "gradle.properties OK"
} else {
    Show-Status "Configuration JDK" "WARNING" "Yellow" "Vérifier org.gradle.java.home"
    $WarningCount++
}

# 4. Verification CI/CD
Write-Host "\n[CI/CD] 4. Verification CI/CD" -ForegroundColor Yellow
$ciConfig = Get-Content ".github\workflows\ci.yml" -ErrorAction SilentlyContinue
if ($ciConfig -match "java-version.*17" -and $ciConfig -match "bundleRelease") {
    Show-Status "Configuration CI" "OK" "Green" "JDK 17 + bundleRelease configurés"
} else {
    Show-Status "Configuration CI" "WARNING" "Yellow" "Vérifier .github/workflows/ci.yml"
    $WarningCount++
}

# 5. Verification Service Account
Write-Host "\n[AUTH] 5. Verification Service Account" -ForegroundColor Yellow
if ($ServiceAccountPath -and (Test-Path $ServiceAccountPath)) {
    Show-Status "Service Account" "OK" "Green" "Fichier JSON trouvé: $ServiceAccountPath"
} elseif (Test-Path ".env") {
    $envContent = Get-Content ".env" -ErrorAction SilentlyContinue
    if ($envContent -match "GOOGLE_PLAY_JSON_KEY") {
        Show-Status "Service Account" "OK" "Green" "Configuration trouvée dans .env"
    } else {
        Show-Status "Service Account" "ERROR" "Red" "Configurer GOOGLE_PLAY_JSON_KEY dans .env"
        $ErrorCount++
    }
} else {
    Show-Status "Service Account" "ERROR" "Red" "Créer .env avec GOOGLE_PLAY_JSON_KEY_PATH"
    $ErrorCount++
}

# 6. Verification Fastlane
Write-Host "\n[FASTLANE] 6. Verification Fastlane" -ForegroundColor Yellow
if (Test-Path "fastlane\Fastfile") {
    Show-Status "Fastlane configuré" "OK" "Green" "Fastfile trouvé"
} else {
    Show-Status "Fastlane manquant" "WARNING" "Yellow" "Installation recommandée"
    $WarningCount++
}

# 7. Verifications de securite
Write-Host "\n[SECURITY] 7. Verifications Securite" -ForegroundColor Yellow
$buildGradle = Get-Content "app\build.gradle.kts" -ErrorAction SilentlyContinue
if ($buildGradle -match "debugSymbolLevel.*FULL") {
    Show-Status "Symboles debug" "OK" "Green" "debugSymbolLevel = FULL configuré"
} else {
    Show-Status "Symboles debug" "WARNING" "Yellow" "Configurer debugSymbolLevel = FULL"
    $WarningCount++
}

if ($buildGradle -match "isMinifyEnabled.*true") {
    Show-Status "Obfuscation" "OK" "Green" "isMinifyEnabled = true"
} else {
    Show-Status "Obfuscation" "WARNING" "Yellow" "Activer isMinifyEnabled pour release"
    $WarningCount++
}

# Resume final
Write-Host "\n[SUMMARY] RESUME FINAL" -ForegroundColor Cyan
Write-Host "===============" -ForegroundColor Cyan

if ($ErrorCount -eq 0 -and $WarningCount -eq 0) {
    Show-Status "Status Global" "OK" "Green" "Pret pour la production!"
} elseif ($ErrorCount -eq 0) {
    Show-Status "Status Global" "WARNING" "Yellow" "Pret avec $WarningCount avertissement(s)"
} else {
    Show-Status "Status Global" "ERROR" "Red" "$ErrorCount erreur(s), $WarningCount avertissement(s)"
}

Write-Host "\n[TODO] ACTIONS RECOMMANDEES:" -ForegroundColor Cyan

if ($ErrorCount -gt 0) {
    Write-Host "1. Corriger les erreurs bloquantes ci-dessus" -ForegroundColor Red
}

if (-not $CheckOnly) {
    Write-Host "2. Publier en Internal Testing:" -ForegroundColor Yellow
    Write-Host "   fastlane internal" -ForegroundColor Gray
    
    Write-Host "3. Inviter 20+ testeurs et attendre 48h" -ForegroundColor Yellow
    
    Write-Host "4. Compléter Data Safety dans Play Console" -ForegroundColor Yellow
    
    Write-Host "5. Configurer Certificate Pinning (optionnel)" -ForegroundColor Yellow
}

Write-Host "\n[SCORE] Score de preparation: $([math]::Max(0, 100 - ($ErrorCount * 20) - ($WarningCount * 5)))%" -ForegroundColor Cyan

if ($ErrorCount -gt 0) {
    exit 1
} else {
    exit 0
}