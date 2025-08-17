#!/bin/bash
# Script d'automatisation complète : Publication Internal + Surveillance 48h
# Publie l'AAB sur le track Internal et lance automatiquement la surveillance de stabilité

set -euo pipefail

# Configuration par défaut
PACKAGE_NAME="com.example.n8nmonitor"
SKIP_BUILD=false
SKIP_TESTS=false
AUTO_PROMOTE=false
MONITORING_HOURS=48
CRASH_THRESHOLD=2.0
ANR_THRESHOLD=1.0
VERBOSE=false

# Répertoires
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Charger les variables d'environnement depuis .env
echo "🔧 Chargement de la configuration..."
if [[ -f "$SCRIPT_DIR/load-env.sh" ]]; then
    source "$SCRIPT_DIR/load-env.sh"
else
    echo "⚠️  Script load-env.sh non trouvé. Variables d'environnement non chargées." >&2
fi

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# Fonction d'affichage avec couleur
log_message() {
    local message="$1"
    local color="${2:-$NC}"
    echo -e "${color}${message}${NC}"
}

# Fonction d'aide
show_help() {
    cat << EOF
🚀 Script d'Automatisation Complète - Publication Internal + Surveillance

Ce script automatise le processus complet de publication sur Google Play Store :
1. Construction de l'AAB (optionnel)
2. Exécution des tests (optionnel)
3. Publication sur le track Internal
4. Surveillance automatique des métriques pendant 48h
5. Promotion automatique vers Production (optionnel)

USAGE:
    $0 [OPTIONS]

OPTIONS:
    --package-name STRING     Nom du package Android (défaut: com.example.n8nmonitor)
    --skip-build             Ignorer la construction de l'AAB
    --skip-tests             Ignorer l'exécution des tests
    --auto-promote           Promotion automatique vers Production si métriques OK
    --monitoring-hours INT   Durée de surveillance en heures (défaut: 48)
    --crash-threshold FLOAT  Seuil de crash rate en % (défaut: 2.0)
    --anr-threshold FLOAT    Seuil d'ANR rate en % (défaut: 1.0)
    --verbose                Mode verbeux
    --help                   Affiche cette aide

EXEMPLES:
    # Publication complète avec surveillance standard
    $0
    
    # Publication rapide sans tests avec promotion automatique
    $0 --skip-tests --auto-promote
    
    # Publication avec surveillance personnalisée (24h, seuils stricts)
    $0 --monitoring-hours 24 --crash-threshold 1.0 --anr-threshold 0.5
    
    # Surveillance uniquement (AAB déjà publié)
    $0 --skip-build --skip-tests

PRÉREQUIS:
    - Google Play Console configuré
    - Credentials GOOGLE_PLAY_JSON_KEY_PATH ou GOOGLE_PLAY_JSON_KEY_DATA
    - Fastlane installé et configuré
    - AAB signé disponible (si --skip-build non spécifié)

FLUX COMPLET:
    📱 Construction AAB → 🧪 Tests → 📤 Publication Internal → 📊 Surveillance 48h → 🚀 Production
EOF
}

# Parsing des arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --package-name)
            PACKAGE_NAME="$2"
            shift 2
            ;;
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --auto-promote)
            AUTO_PROMOTE=true
            shift
            ;;
        --monitoring-hours)
            MONITORING_HOURS="$2"
            shift 2
            ;;
        --crash-threshold)
            CRASH_THRESHOLD="$2"
            shift 2
            ;;
        --anr-threshold)
            ANR_THRESHOLD="$2"
            shift 2
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --help)
            show_help
            exit 0
            ;;
        *)
            echo "Option inconnue: $1"
            show_help
            exit 1
            ;;
    esac
done

# Validation des paramètres
if [[ $MONITORING_HOURS -le 0 ]]; then
    log_message "❌ La durée de surveillance doit être positive" "$RED"
    exit 1
fi

if [[ $(echo "$CRASH_THRESHOLD <= 0" | bc -l) -eq 1 ]] || [[ $(echo "$ANR_THRESHOLD <= 0" | bc -l) -eq 1 ]]; then
    log_message "❌ Les seuils doivent être positifs" "$RED"
    exit 1
fi

# Vérification des prérequis
test_prerequisites() {
    log_message "🔍 Vérification des prérequis..." "$YELLOW"
    
    # Vérifier Fastlane
    if command -v fastlane >/dev/null 2>&1; then
        log_message "   ✅ Fastlane installé" "$GREEN"
    else
        log_message "   ❌ Fastlane non trouvé. Installez-le avec: gem install fastlane" "$RED"
        return 1
    fi
    
    # Vérifier les credentials Google Play
    if [[ -z "${GOOGLE_PLAY_JSON_KEY_PATH:-}" && -z "${GOOGLE_PLAY_JSON_KEY_DATA:-}" ]]; then
        log_message "   ❌ Credentials Google Play non configurés!" "$RED"
        log_message "      Configurez GOOGLE_PLAY_JSON_KEY_PATH ou GOOGLE_PLAY_JSON_KEY_DATA" "$RED"
        return 1
    fi
    
    if [[ -n "${GOOGLE_PLAY_JSON_KEY_PATH:-}" && ! -f "$GOOGLE_PLAY_JSON_KEY_PATH" ]]; then
        log_message "   ❌ Fichier de credentials non trouvé: $GOOGLE_PLAY_JSON_KEY_PATH" "$RED"
        return 1
    fi
    
    log_message "   ✅ Credentials Google Play configurés" "$GREEN"
    
    # Vérifier les scripts de surveillance
    local monitor_script="$SCRIPT_DIR/monitor-stability.sh"
    if [[ ! -f "$monitor_script" ]]; then
        log_message "   ❌ Script de surveillance non trouvé: $monitor_script" "$RED"
        return 1
    fi
    
    log_message "   ✅ Script de surveillance disponible" "$GREEN"
    
    # Vérifier les dépendances
    local missing_deps=()
    command -v jq >/dev/null 2>&1 || missing_deps+=("jq")
    command -v bc >/dev/null 2>&1 || missing_deps+=("bc")
    
    if [[ ${#missing_deps[@]} -gt 0 ]]; then
        log_message "   ❌ Dépendances manquantes: ${missing_deps[*]}" "$RED"
        log_message "      Installez avec: apt-get install ${missing_deps[*]} ou brew install ${missing_deps[*]}" "$RED"
        return 1
    fi
    
    log_message "   ✅ Toutes les dépendances sont installées" "$GREEN"
    
    return 0
}

# Construction de l'AAB
build_aab() {
    if [[ "$SKIP_BUILD" == "true" ]]; then
        log_message "⏭️  Construction ignorée (--skip-build activé)" "$YELLOW"
        return 0
    fi
    
    log_message "🔨 Construction de l'AAB..." "$YELLOW"
    
    # Nettoyage
    ./gradlew clean
    
    # Construction de l'AAB de release
    ./gradlew bundleRelease
    
    # Vérification que l'AAB a été créé
    local aab_path="app/build/outputs/bundle/release/app-release.aab"
    if [[ -f "$aab_path" ]]; then
        local aab_size=$(du -h "$aab_path" | cut -f1)
        log_message "   ✅ AAB créé avec succès: $aab_path ($aab_size)" "$GREEN"
        return 0
    else
        log_message "   ❌ AAB non trouvé après construction" "$RED"
        return 1
    fi
}

# Exécution des tests
run_tests() {
    if [[ "$SKIP_TESTS" == "true" ]]; then
        log_message "⏭️  Tests ignorés (--skip-tests activé)" "$YELLOW"
        return 0
    fi
    
    log_message "🧪 Exécution des tests..." "$YELLOW"
    
    # Tests unitaires
    ./gradlew test
    
    # Tests d'instrumentation (optionnel, peut nécessiter un émulateur)
    # ./gradlew connectedAndroidTest
    
    log_message "   ✅ Tests exécutés avec succès" "$GREEN"
    return 0
}

# Publication sur le track Internal
publish_to_internal() {
    log_message "📤 Publication sur le track Internal..." "$YELLOW"
    
    # Utilisation de Fastlane pour publier
    fastlane android internal
    
    log_message "   ✅ Publication réussie sur le track Internal" "$GREEN"
    log_message "   🌐 Vérifiez dans Google Play Console: https://play.google.com/console/developers" "$CYAN"
    return 0
}

# Surveillance des métriques
start_monitoring() {
    log_message "📊 Démarrage de la surveillance des métriques..." "$YELLOW"
    log_message "   ⏱️  Durée: $MONITORING_HOURS heures" "$CYAN"
    log_message "   🎯 Seuils: Crash < $CRASH_THRESHOLD%, ANR < $ANR_THRESHOLD%" "$CYAN"
    
    local monitor_script="$SCRIPT_DIR/monitor-stability.sh"
    local monitor_args=("--package-name" "$PACKAGE_NAME" "--monitoring-hours" "$MONITORING_HOURS" "--crash-threshold" "$CRASH_THRESHOLD" "--anr-threshold" "$ANR_THRESHOLD")
    
    if [[ "$VERBOSE" == "true" ]]; then
        monitor_args+=("--verbose")
    fi
    
    # Lancement du script de surveillance
    if "$monitor_script" "${monitor_args[@]}"; then
        log_message "   ✅ Surveillance terminée - Métriques conformes" "$GREEN"
        return 0
    else
        log_message "   ❌ Surveillance terminée - Métriques non conformes" "$RED"
        return 1
    fi
}

# Promotion vers Production
promote_to_production() {
    if [[ "$AUTO_PROMOTE" != "true" ]]; then
        log_message "🤔 Promotion manuelle requise" "$YELLOW"
        log_message "   Pour promouvoir vers Production, exécutez:" "$CYAN"
        log_message "   fastlane android production" "$CYAN"
        return 0
    fi
    
    log_message "🚀 Promotion automatique vers Production..." "$YELLOW"
    
    fastlane android production
    
    log_message "   ✅ Promotion réussie vers Production" "$GREEN"
    log_message "   🎉 Application disponible en Production!" "$GREEN"
    return 0
}

# Fonction principale
start_publish_and_monitor() {
    local start_time=$(date +%s)
    
    log_message "" "$NC"
    log_message "🚀 DÉMARRAGE DU PROCESSUS COMPLET DE PUBLICATION" "$GREEN"
    log_message "" "$NC"
    log_message "📱 Package: $PACKAGE_NAME" "$CYAN"
    log_message "⏱️  Surveillance: $MONITORING_HOURS heures" "$CYAN"
    log_message "🎯 Seuils: Crash < $CRASH_THRESHOLD%, ANR < $ANR_THRESHOLD%" "$CYAN"
    log_message "🤖 Promotion auto: $(if [[ "$AUTO_PROMOTE" == "true" ]]; then echo 'Activée'; else echo 'Désactivée'; fi)" "$CYAN"
    log_message "" "$NC"
    
    # Étape 1: Vérification des prérequis
    log_message "📋 ÉTAPE 1/5: Vérification des prérequis" "$MAGENTA"
    if ! test_prerequisites; then
        log_message "❌ Échec des prérequis" "$RED"
        return 1
    fi
    log_message "" "$NC"
    
    # Étape 2: Construction
    log_message "📋 ÉTAPE 2/5: Construction de l'AAB" "$MAGENTA"
    if ! build_aab; then
        log_message "❌ Échec de la construction" "$RED"
        return 1
    fi
    log_message "" "$NC"
    
    # Étape 3: Tests
    log_message "📋 ÉTAPE 3/5: Exécution des tests" "$MAGENTA"
    if ! run_tests; then
        log_message "❌ Échec des tests" "$RED"
        return 1
    fi
    log_message "" "$NC"
    
    # Étape 4: Publication
    log_message "📋 ÉTAPE 4/5: Publication sur Internal" "$MAGENTA"
    if ! publish_to_internal; then
        log_message "❌ Échec de la publication" "$RED"
        return 1
    fi
    log_message "" "$NC"
    
    # Étape 5: Surveillance
    log_message "📋 ÉTAPE 5/5: Surveillance des métriques" "$MAGENTA"
    local monitoring_success=false
    if start_monitoring; then
        monitoring_success=true
    fi
    log_message "" "$NC"
    
    # Résumé final
    local end_time=$(date +%s)
    local total_duration=$(echo "scale=1; ($end_time - $start_time) / 3600" | bc -l)
    
    log_message "🏁 PROCESSUS TERMINÉ" "$GREEN"
    log_message "" "$NC"
    log_message "📊 RÉSUMÉ:" "$CYAN"
    log_message "   ⏱️  Durée totale: ${total_duration} heures" "$NC"
    log_message "   📱 Package: $PACKAGE_NAME" "$NC"
    log_message "   📤 Publication Internal: ✅ Réussie" "$GREEN"
    
    if [[ "$monitoring_success" == "true" ]]; then
        log_message "   📊 Surveillance 48h: ✅ Conforme" "$GREEN"
        if [[ "$AUTO_PROMOTE" == "true" ]]; then
            log_message "   🚀 Statut Production: ✅ Promue automatiquement" "$GREEN"
            promote_to_production
        else
            log_message "   🚀 Statut Production: 🤔 Promotion manuelle requise" "$YELLOW"
        fi
    else
        log_message "   📊 Surveillance 48h: ❌ Non conforme" "$RED"
        log_message "   🚀 Statut Production: ❌ Promotion bloquée (métriques non conformes)" "$RED"
    fi
    
    log_message "" "$NC"
    
    if [[ "$monitoring_success" == "true" ]]; then
        log_message "🎉 SUCCÈS: Application prête pour la production!" "$GREEN"
        return 0
    else
        log_message "⚠️  ATTENTION: Corrigez les problèmes et relancez le processus" "$YELLOW"
        return 1
    fi
}

# Changement vers le répertoire du projet
cd "$PROJECT_ROOT"

# Démarrage du processus
if start_publish_and_monitor; then
    exit 0  # Succès
else
    exit 1  # Échec
fi