# Script de validation de la configuration de déploiement Google Play
# Vérifie que tous les prérequis sont en place pour un déploiement réussi

$ErrorActionPreference = "Continue"

Write-Host "[VALIDATION] Configuration de Deploiement Google Play" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""

$issues = @()
$warnings = @()
$success = @()

# Fonction d'affichage des résultats
function Show-Result {
    param(
        [string]$Test,
        [string]$Status,
        [string]$Message,
        [string]$Color = "White"
    )
    
    $icon = switch ($Status) {
        "OK" { "[OK]" }
        "WARNING" { "[WARN]" }
        "ERROR" { "[ERROR]" }
        default { "[INFO]" }
    }
    
    Write-Host "$icon $Test" -ForegroundColor $Color -NoNewline
    Write-Host " - $Message" -ForegroundColor Gray
}

# 1. Vérification Python
Write-Host "\n[1] Environnement Python" -ForegroundColor Cyan
try {
    $pythonVersion = python --version 2>&1
    if ($pythonVersion -match "Python (\d+\.\d+)" -and [version]$matches[1] -ge [version]"3.8") {
        Show-Result "Python" "OK" "$pythonVersion" "Green"
        $success += "Python installé et compatible"
    } else {
        Show-Result "Python" "WARNING" "Version $pythonVersion (recommandé: 3.8+)" "Yellow"
        $warnings += "Version Python potentiellement incompatible"
    }
} catch {
    Show-Result "Python" "ERROR" "Python non trouvé dans PATH" "Red"
    $issues += "Python non installé ou non accessible"
}

# 2. Vérification des dépendances Google API
Write-Host "\n[2] Dependances Google API" -ForegroundColor Cyan
$requiredPackages = @(
    "google-api-python-client",
    "google-auth-httplib2", 
    "google-auth-oauthlib"
)

foreach ($package in $requiredPackages) {
    try {
        $importName = $package
        if ($package -eq "google-api-python-client") {
            $importName = "googleapiclient"
        } else {
            $importName = $package.Replace('-', '_')
        }
        
        $result = python -c "import $importName; print('OK')" 2>&1
        if ($result -eq "OK") {
            Show-Result $package "OK" "Installé" "Green"
            $success += "$package disponible"
        } else {
            Show-Result $package "ERROR" "Non installé" "Red"
            $issues += "$package manquant"
        }
    } catch {
        Show-Result $package "ERROR" "Erreur de vérification" "Red"
        $issues += "Impossible de vérifier $package"
    }
}

# 3. Vérification des fichiers de configuration
Write-Host "\n[3] Fichiers de Configuration" -ForegroundColor Cyan

# Service Account
if (Test-Path "./service-account.json") {
    try {
        $serviceAccount = Get-Content "./service-account.json" | ConvertFrom-Json
        if ($serviceAccount.type -eq "service_account" -and $serviceAccount.client_email) {
            Show-Result "Service Account" "OK" "Fichier valide ($($serviceAccount.client_email))" "Green"
            $success += "Service account configuré"
        } else {
            Show-Result "Service Account" "ERROR" "Fichier invalide" "Red"
            $issues += "service-account.json mal formaté"
        }
    } catch {
        Show-Result "Service Account" "ERROR" "Erreur de lecture" "Red"
        $issues += "service-account.json illisible"
    }
} else {
    Show-Result "Service Account" "ERROR" "Fichier manquant" "Red"
    $issues += "service-account.json introuvable"
}

# Keystore Properties
if (Test-Path "./keystore.properties") {
    Show-Result "Keystore Properties" "OK" "Fichier présent" "Green"
    $success += "Configuration de signature présente"
} else {
    Show-Result "Keystore Properties" "WARNING" "Fichier manquant (requis pour release)" "Yellow"
    $warnings += "keystore.properties manquant"
}

# 4. Vérification de la structure du projet
Write-Host "\n[4] Structure du Projet" -ForegroundColor Cyan

# Build.gradle.kts
if (Test-Path "./app/build.gradle.kts") {
    $buildGradle = Get-Content "./app/build.gradle.kts" -Raw
    
    # Vérifier buildConfig
    if ($buildGradle -match "buildConfig = true") {
        Show-Result "BuildConfig" "OK" "Activé" "Green"
        $success += "BuildConfig activé"
    } else {
        Show-Result "BuildConfig" "ERROR" "Non activé" "Red"
        $issues += "buildConfig = true manquant"
    }
    
    # Vérifier applicationId
    if ($buildGradle -match 'applicationId = "([^"]+)"') {
        $appId = $matches[1]
        Show-Result "Application ID" "OK" "$appId" "Green"
        $success += "Application ID configuré"
    } else {
        Show-Result "Application ID" "WARNING" "Non trouvé" "Yellow"
        $warnings += "Application ID non détecté"
    }
    
    # Vérifier versionCode
    if ($buildGradle -match 'versionCode = (\d+)') {
        $versionCode = $matches[1]
        Show-Result "Version Code" "OK" "$versionCode" "Green"
        $success += "Version code configuré"
    } else {
        Show-Result "Version Code" "ERROR" "Non trouvé" "Red"
        $issues += "versionCode manquant"
    }
} else {
    Show-Result "Build Gradle" "ERROR" "app/build.gradle.kts manquant" "Red"
    $issues += "Fichier de build principal manquant"
}

# 5. Vérification des scripts de déploiement
Write-Host "\n[5] Scripts de Deploiement" -ForegroundColor Cyan

$deploymentScripts = @(
    "./scripts/deploy-google-play.ps1",
    "./scripts/deploy-all-tracks.ps1",
    "./scripts/direct-upload.ps1"
)

foreach ($script in $deploymentScripts) {
    if (Test-Path $script) {
        Show-Result (Split-Path $script -Leaf) "OK" "Disponible" "Green"
        $success += "$(Split-Path $script -Leaf) présent"
    } else {
        Show-Result (Split-Path $script -Leaf) "WARNING" "Manquant" "Yellow"
        $warnings += "$(Split-Path $script -Leaf) non trouvé"
    }
}

# 6. Test de construction
Write-Host "\n[6] Test de Construction" -ForegroundColor Cyan
try {
    Write-Host "   Tentative de construction de test..." -ForegroundColor Gray
    $buildResult = ./gradlew tasks --quiet 2>&1
    if ($LASTEXITCODE -eq 0) {
        Show-Result "Gradle" "OK" "Fonctionnel" "Green"
        $success += "Gradle opérationnel"
    } else {
        Show-Result "Gradle" "ERROR" "Erreur de configuration" "Red"
        $issues += "Problème de configuration Gradle"
    }
} catch {
    Show-Result "Gradle" "ERROR" "Non accessible" "Red"
    $issues += "Gradle non accessible"
}

# 7. Résumé et recommandations
Write-Host "\n[RESUME] VALIDATION" -ForegroundColor Green
Write-Host "===========================" -ForegroundColor Green

Write-Host "\n[SUCCESS] Elements Valides ($($success.Count)):" -ForegroundColor Green
foreach ($item in $success) {
    Write-Host "   • $item" -ForegroundColor White
}

if ($warnings.Count -gt 0) {
    Write-Host "\n[WARNING] Avertissements ($($warnings.Count)):" -ForegroundColor Yellow
    foreach ($warning in $warnings) {
        Write-Host "   • $warning" -ForegroundColor White
    }
}

if ($issues.Count -gt 0) {
    Write-Host "\n[ERROR] Problemes a Resoudre ($($issues.Count)):" -ForegroundColor Red
    foreach ($issue in $issues) {
        Write-Host "   • $issue" -ForegroundColor White
    }
}

# 8. Recommandations
Write-Host "\n[RECOMMANDATIONS]" -ForegroundColor Cyan
Write-Host "==================" -ForegroundColor Cyan

if ($issues.Count -eq 0 -and $warnings.Count -eq 0) {
    Write-Host "\n[PERFECT] Configuration parfaite ! Vous pouvez deployer en toute confiance." -ForegroundColor Green
    Write-Host "\n[COMMANDS] Commandes suggerees:" -ForegroundColor Cyan
    Write-Host "   .\\scripts\\deploy-google-play.ps1 -Track internal -AutoIncrement" -ForegroundColor White
    Write-Host "   .\\scripts\\deploy-all-tracks.ps1 -Internal" -ForegroundColor White
} elseif ($issues.Count -eq 0) {
    Write-Host "\n[READY] Configuration fonctionnelle avec quelques avertissements." -ForegroundColor Green
    Write-Host "   Vous pouvez procéder au déploiement." -ForegroundColor White
} else {
    Write-Host "\n[ACTION] Veuillez resoudre les problemes avant de deployer:" -ForegroundColor Yellow
    
    if ($issues -contains "Python non installé ou non accessible") {
        Write-Host "   1. Installez Python 3.8+ depuis https://python.org" -ForegroundColor White
    }
    
    if ($issues | Where-Object { $_ -like "*google-*" }) {
        Write-Host "   2. Installez les dépendances: python -m pip install google-api-python-client google-auth-httplib2 google-auth-oauthlib" -ForegroundColor White
    }
    
    if ($issues -contains "service-account.json introuvable") {
        Write-Host "   3. Ajoutez le fichier service-account.json depuis Google Cloud Console" -ForegroundColor White
    }
    
    if ($issues -contains "buildConfig = true manquant") {
        Write-Host "   4. Ajoutez 'buildConfig = true' dans app/build.gradle.kts" -ForegroundColor White
    }
}

Write-Host "\n[DOC] Documentation: ./DEPLOYMENT-GUIDE.md" -ForegroundColor Gray
Write-Host "\n[END] Validation terminee" -ForegroundColor Green

# Code de sortie
if ($issues.Count -gt 0) {
    exit 1
} else {
    exit 0
}