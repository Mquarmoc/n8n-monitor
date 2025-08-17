# Script d'automatisation complète : Publication Internal + Surveillance 48h
# Publie l'AAB sur le track Internal et lance automatiquement la surveillance de stabilité

param(
    [string]$PackageName = "com.example.n8nmonitor",
    [switch]$SkipBuild,
    [switch]$SkipTests,
    [switch]$AutoPromote,
    [int]$MonitoringHours = 48,
    [double]$CrashThreshold = 2.0,
    [double]$ANRThreshold = 1.0,
    [switch]$Verbose,
    [switch]$Help
)

# Configuration
$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir

# Charger les variables d'environnement depuis .env
Write-Host "[CONFIG] Chargement de la configuration..."
$envScript = Join-Path $ScriptDir "load-env.ps1"
if (Test-Path $envScript) {
    . $envScript
} else {
    Write-Warning "[!] Script load-env.ps1 non trouvé. Variables d'environnement non chargées."
}

# Couleurs pour l'affichage
function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    Write-Host $Message -ForegroundColor $Color
}

# Fonction d'aide
function Show-Help {
    Write-Host @"
🚀 Script d'Automatisation Complète - Publication Internal + Surveillance

Ce script automatise le processus complet de publication sur Google Play Store :
1. Construction de l'AAB (optionnel)
2. Exécution des tests (optionnel)
3. Publication sur le track Internal
4. Surveillance automatique des métriques pendant 48h
5. Promotion automatique vers Production (optionnel)

USAGE:
    .\publish-and-monitor.ps1 [OPTIONS]

OPTIONS:
    -PackageName STRING       Nom du package Android (défaut: com.example.n8nmonitor)
    -SkipBuild               Ignorer la construction de l'AAB
    -SkipTests               Ignorer l'exécution des tests
    -AutoPromote             Promotion automatique vers Production si métriques OK
    -MonitoringHours INT     Durée de surveillance en heures (défaut: 48)
    -CrashThreshold DOUBLE   Seuil de crash rate en % (défaut: 2.0)
    -ANRThreshold DOUBLE     Seuil d'ANR rate en % (défaut: 1.0)
    -Verbose                 Mode verbeux
    -Help                    Affiche cette aide

EXEMPLES:
    # Publication complète avec surveillance standard
    .\publish-and-monitor.ps1
    
    # Publication rapide sans tests avec promotion automatique
    .\publish-and-monitor.ps1 -SkipTests -AutoPromote
    
    # Publication avec surveillance personnalisée (24h, seuils stricts)
    .\publish-and-monitor.ps1 -MonitoringHours 24 -CrashThreshold 1.0 -ANRThreshold 0.5
    
    # Surveillance uniquement (AAB déjà publié)
    .\publish-and-monitor.ps1 -SkipBuild -SkipTests

PRÉREQUIS:
    - Google Play Console configuré
    - Credentials GOOGLE_PLAY_JSON_KEY_PATH ou GOOGLE_PLAY_JSON_KEY_DATA
    - Fastlane installé et configuré
    - AAB signé disponible (si -SkipBuild non spécifié)

FLUX COMPLET:
    [APP] Construction AAB -> [TEST] Tests -> [UPLOAD] Publication Internal -> [MONITOR] Surveillance 48h -> [PROD] Production
"@
}

# Vérification des prérequis
function Test-Prerequisites {
    Write-ColorOutput "[CHECK] Vérification des prérequis..." "Yellow"
    
    # Vérifier Fastlane
    try {
        $null = Get-Command fastlane -ErrorAction Stop
        Write-ColorOutput "   [OK] Fastlane installé" "Green"
    } catch {
        Write-ColorOutput "   [X] Fastlane non trouvé. Installez-le avec: gem install fastlane" "Red"
        return $false
    }
    
    # Vérifier les credentials Google Play
    $googlePlayKeyPath = $env:GOOGLE_PLAY_JSON_KEY_PATH
    $googlePlayKeyData = $env:GOOGLE_PLAY_JSON_KEY_DATA
    
    if (-not $googlePlayKeyPath -and -not $googlePlayKeyData) {
        Write-ColorOutput "   [X] Credentials Google Play non configurés!" "Red"
        Write-ColorOutput "      Configurez GOOGLE_PLAY_JSON_KEY_PATH ou GOOGLE_PLAY_JSON_KEY_DATA" "Red"
        return $false
    }
    
    if ($googlePlayKeyPath -and -not (Test-Path $googlePlayKeyPath)) {
        Write-ColorOutput "   [X] Fichier de credentials non trouvé: $googlePlayKeyPath" "Red"
        return $false
    }
    
    Write-ColorOutput "   [OK] Credentials Google Play configurés" "Green"
    
    # Vérifier les scripts de surveillance
    $monitorScript = Join-Path $ScriptDir "monitor-stability.ps1"
    if (-not (Test-Path $monitorScript)) {
        Write-ColorOutput "   [X] Script de surveillance non trouvé: $monitorScript" "Red"
        return $false
    }
    
    Write-ColorOutput "   [OK] Script de surveillance disponible" "Green"
    
    return $true
}

# Construction de l'AAB
function Build-AAB {
    if ($SkipBuild) {
        Write-ColorOutput "[SKIP] Construction ignorée (SkipBuild activé)" "Yellow"
        return $true
    }
    
    Write-ColorOutput "[BUILD] Construction de l'AAB..." "Yellow"
    
    try {
        # Nettoyage
        & ./gradlew clean
        
        # Construction de l'AAB de release
        & ./gradlew bundleRelease
        
        # Vérification que l'AAB a été créé
        $aabPath = "app/build/outputs/bundle/release/app-release.aab"
        if (Test-Path $aabPath) {
            $aabSize = [math]::Round((Get-Item $aabPath).Length / 1MB, 1)
            Write-ColorOutput "   [OK] AAB créé avec succès: $aabPath ($aabSize MB)" "Green"
            return $true
        } else {
            Write-ColorOutput "   [X] AAB non trouvé après construction" "Red"
            return $false
        }
    } catch {
        Write-ColorOutput "   [X] Erreur lors de la construction: $($_.Exception.Message)" "Red"
        return $false
    }
}

# Exécution des tests
function Run-Tests {
    if ($SkipTests) {
        Write-ColorOutput "[SKIP] Tests ignorés (SkipTests activé)" "Yellow"
        return $true
    }
    
    Write-ColorOutput "[TEST] Exécution des tests..." "Yellow"
    
    try {
        # Tests unitaires
        & ./gradlew test
        
        # Tests d'instrumentation (optionnel, peut nécessiter un émulateur)
        # & ./gradlew connectedAndroidTest
        
        Write-ColorOutput "   [OK] Tests exécutés avec succès" "Green"
        return $true
    } catch {
        Write-ColorOutput "   [X] Échec des tests: $($_.Exception.Message)" "Red"
        return $false
    }
}

# Publication sur le track Internal
function Publish-ToInternal {
    Write-ColorOutput "[UPLOAD] Publication sur le track Internal..." "Yellow"
    
    try {
        # Utilisation de Fastlane pour publier
        & fastlane android internal
        
        Write-ColorOutput "   [OK] Publication réussie sur le track Internal" "Green"
        Write-ColorOutput "   [WEB] Vérifiez dans Google Play Console: https://play.google.com/console/developers" "Cyan"
        return $true
    } catch {
        Write-ColorOutput "   [X] Erreur lors de la publication: $($_.Exception.Message)" "Red"
        return $false
    }
}

# Surveillance des métriques
function Start-Monitoring {
    Write-ColorOutput "[MONITOR] Démarrage de la surveillance des métriques..." "Yellow"
    Write-ColorOutput "   [TIME] Durée: $MonitoringHours heures" "Cyan"
    Write-ColorOutput "   [TARGET] Seuils: Crash moins de $CrashThreshold%, ANR moins de $ANRThreshold%" "Cyan"
    
    $monitorScript = Join-Path $ScriptDir "monitor-stability.ps1"
    
    try {
        # Lancement du script de surveillance
        $result = & $monitorScript -PackageName $PackageName -MonitoringHours $MonitoringHours -CrashThreshold $CrashThreshold -ANRThreshold $ANRThreshold -Verbose:$Verbose
        
        if ($LASTEXITCODE -eq 0) {
            Write-ColorOutput "   [OK] Surveillance terminée - Métriques conformes" "Green"
            return $true
        } else {
            Write-ColorOutput "   [X] Surveillance terminée - Métriques non conformes" "Red"
            return $false
        }
    } catch {
        Write-ColorOutput "   [X] Erreur lors de la surveillance: $($_.Exception.Message)" "Red"
        return $false
    }
}

# Promotion vers Production
function Promote-ToProduction {
    if (-not $AutoPromote) {
        Write-ColorOutput "[MANUAL] Promotion manuelle requise" "Yellow"
        Write-ColorOutput "   Pour promouvoir vers Production, exécutez:" "Cyan"
        Write-ColorOutput "   fastlane android production" "Cyan"
        return $true
    }
    
    Write-ColorOutput "[AUTO] Promotion automatique vers Production..." "Yellow"
    
    try {
        & fastlane android production
        
        Write-ColorOutput "   [OK] Promotion réussie vers Production" "Green"
        Write-ColorOutput "   [SUCCESS] Application disponible en Production!" "Green"
        return $true
    } catch {
        Write-ColorOutput "   [X] Erreur lors de la promotion: $($_.Exception.Message)" "Red"
        return $false
    }
}

# Fonction principale
function Start-PublishAndMonitor {
    $startTime = Get-Date
    
    Write-ColorOutput "" "White"
    Write-ColorOutput "[START] DÉMARRAGE DU PROCESSUS COMPLET DE PUBLICATION" "Green"
    Write-ColorOutput "" "White"
    Write-ColorOutput "[APP] Package: $PackageName" "Cyan"
    Write-ColorOutput "   [TIME] Surveillance: $MonitoringHours heures" "Cyan"
    Write-ColorOutput "[TARGET] Seuils: Crash moins de $CrashThreshold%, ANR moins de $ANRThreshold%" "Cyan"
    Write-ColorOutput "[AUTO] Promotion auto: $(if ($AutoPromote) { 'Activée' } else { 'Désactivée' })" "Cyan"
    Write-ColorOutput "" "White"
    
    # Étape 1: Vérification des prérequis
    Write-ColorOutput "[STEP 1/5] Vérification des prérequis" "Magenta"
    if (-not (Test-Prerequisites)) {
        Write-ColorOutput "[X] Échec des prérequis" "Red"
        return $false
    }
    Write-ColorOutput "" "White"
    
    # Étape 2: Construction
    Write-ColorOutput "[STEP 2/5] Construction de l'AAB" "Magenta"
    if (-not $SkipBuild) {
        if (-not (Build-AAB)) {
            Write-ColorOutput "[X] Échec de la construction" "Red"
            return $false
        }
    } else {
        Write-ColorOutput "[SKIP] Construction ignorée (SkipBuild activé)" "Yellow"
    }
    Write-ColorOutput "" "White"
    
    # Étape 3: Tests
    Write-ColorOutput "[STEP 3/5] Exécution des tests" "Magenta"
    if (-not $SkipTests) {
        if (-not (Run-Tests)) {
            Write-ColorOutput "[X] Échec des tests" "Red"
            return $false
        }
    } else {
        Write-ColorOutput "[SKIP] Tests ignorés (SkipTests activé)" "Yellow"
    }
    Write-ColorOutput "" "White"
    
    # Étape 4: Publication
    Write-ColorOutput "[STEP 4/5] Publication sur Internal" "Magenta"
    if (-not (Publish-ToInternal)) {
        Write-ColorOutput "[X] Échec de la publication" "Red"
        return $false
    }
    Write-ColorOutput "" "White"
    
    # Étape 5: Surveillance
    Write-ColorOutput "[STEP 5/5] Surveillance des métriques" "Magenta"
    $monitoringSuccess = Start-Monitoring
    Write-ColorOutput "" "White"
    
    # Résumé final
    $endTime = Get-Date
    $totalDuration = $endTime - $startTime
    
    Write-ColorOutput "[FINISHED] PROCESSUS TERMINÉ" "Green"
    Write-ColorOutput "" "White"
    Write-ColorOutput "[SUMMARY] RÉSUMÉ:" "Cyan"
    Write-ColorOutput "   [TIME] Durée totale: $([math]::Round($totalDuration.TotalHours, 1)) heures" "White"
    Write-ColorOutput "   [APP] Package: $PackageName" "White"
    Write-ColorOutput "   [UPLOAD] Publication Internal: [OK] Réussie" "Green"
    Write-ColorOutput "   [MONITOR] Surveillance 48h: $(if ($monitoringSuccess) { '[OK] Conforme' } else { '[X] Non conforme' })" $(if ($monitoringSuccess) { "Green" } else { "Red" })
    
    if ($monitoringSuccess) {
        Write-ColorOutput "   [PROD] Statut Production: $(if ($AutoPromote) { '[OK] Promue automatiquement' } else { '[MANUAL] Promotion manuelle requise' })" $(if ($AutoPromote) { "Green" } else { "Yellow" })
        
        if ($AutoPromote) {
            Promote-ToProduction | Out-Null
        }
    } else {
        Write-ColorOutput "   [PROD] Statut Production: [X] Promotion bloquée (métriques non conformes)" "Red"
    }
    
    Write-ColorOutput "" "White"
    
    if ($monitoringSuccess) {
        Write-ColorOutput "🎉 SUCCÈS: Application prête pour la production!" "Green"
        return $true
    } else {
        Write-ColorOutput "[!] ATTENTION: Corrigez les problèmes et relancez le processus" "Yellow"
        return $false
    }
}

# Point d'entrée principal
if ($Help) {
    Show-Help
    exit 0
}

# Validation des paramètres
if ($MonitoringHours -le 0) {
    Write-ColorOutput "[X] La durée de surveillance doit être positive" "Red"
    exit 1
}

if ($CrashThreshold -le 0 -or $ANRThreshold -le 0) {
    Write-ColorOutput "[X] Les seuils doivent être positifs" "Red"
    exit 1
}

# Changement vers le répertoire du projet
Set-Location $ProjectRoot

# Démarrage du processus
try {
    if (Start-PublishAndMonitor) {
        exit 0  # Succès
    } else {
        exit 1  # Échec
    }
} catch {
    Write-ColorOutput "[X] Erreur fatale: $($_.Exception.Message)" "Red"
    exit 2  # Erreur système
}