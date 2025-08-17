#!/bin/bash
# Script de surveillance des métriques de stabilité pour Google Play Store
# Surveille le crash rate et ANR rate pendant 48h avant promotion en production

set -euo pipefail

# Configuration par défaut
PACKAGE_NAME="com.example.n8nmonitor"
MONITORING_HOURS=48
CRASH_THRESHOLD=2.0
ANR_THRESHOLD=1.0
CHECK_INTERVAL_MINUTES=30
VERBOSE=false

# Fichiers de sortie
LOG_FILE="stability-monitoring.log"
REPORT_FILE="stability-report-$(date +%Y%m%d-%H%M%S).json"
START_TIME=$(date +%s)

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Fonction d'affichage avec couleur et log
log_message() {
    local message="$1"
    local color="${2:-$NC}"
    echo -e "${color}${message}${NC}"
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $message" >> "$LOG_FILE"
}

# Fonction d'aide
show_help() {
    cat << EOF
🔍 Script de Surveillance de Stabilité - Google Play Store

USAGE:
    $0 [OPTIONS]

OPTIONS:
    --package-name STRING     Nom du package Android (défaut: com.example.n8nmonitor)
    --monitoring-hours INT    Durée de surveillance en heures (défaut: 48)
    --crash-threshold FLOAT   Seuil de crash rate en % (défaut: 2.0)
    --anr-threshold FLOAT     Seuil d'ANR rate en % (défaut: 1.0)
    --check-interval INT      Intervalle entre vérifications en minutes (défaut: 30)
    --verbose                 Mode verbeux
    --help                    Affiche cette aide

EXEMPLES:
    # Surveillance standard de 48h
    $0
    
    # Surveillance de 24h avec seuils personnalisés
    $0 --monitoring-hours 24 --crash-threshold 1.5 --anr-threshold 0.5
    
    # Surveillance rapide pour tests (1h, vérification toutes les 5 min)
    $0 --monitoring-hours 1 --check-interval 5

NOTE:
    Ce script simule actuellement les métriques Google Play.
    En production, intégrez l'API Google Play Console pour des données réelles.
EOF
}

# Parsing des arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --package-name)
            PACKAGE_NAME="$2"
            shift 2
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
        --check-interval)
            CHECK_INTERVAL_MINUTES="$2"
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
    echo "❌ La durée de surveillance doit être positive"
    exit 1
fi

if [[ $(echo "$CRASH_THRESHOLD <= 0" | bc -l) -eq 1 ]] || [[ $(echo "$ANR_THRESHOLD <= 0" | bc -l) -eq 1 ]]; then
    echo "❌ Les seuils doivent être positifs"
    exit 1
fi

if [[ $CHECK_INTERVAL_MINUTES -le 0 ]]; then
    echo "❌ L'intervalle de vérification doit être positif"
    exit 1
fi

# Fonction pour générer des métriques simulées
# En production, ceci utiliserait l'API Google Play Console
get_play_console_metrics() {
    local package_name="$1"
    
    # Simulation des métriques (à remplacer par l'API réelle)
    local crash_rate=$(echo "scale=2; $(shuf -i 1-300 -n 1) / 100" | bc -l)
    local anr_rate=$(echo "scale=2; $(shuf -i 5-150 -n 1) / 100" | bc -l)
    local sessions=$(shuf -i 100-1000 -n 1)
    local timestamp=$(date -Iseconds)
    
    cat << EOF
{
    "crash_rate": $crash_rate,
    "anr_rate": $anr_rate,
    "total_sessions": $sessions,
    "timestamp": "$timestamp"
}
EOF
}

# Fonction pour envoyer une alerte
send_alert() {
    local subject="$1"
    local message="$2"
    local severity="${3:-Warning}"
    
    log_message "🚨 ALERTE [$severity]: $subject" "$RED"
    log_message "   $message" "$YELLOW"
    
    # Ici, vous pourriez ajouter l'envoi d'email, Slack, Teams, etc.
    # mail -s "$subject" team@company.com <<< "$message"
    # curl -X POST -H 'Content-type: application/json' --data '{"text":"'$message'"}' YOUR_SLACK_WEBHOOK
}

# Fonction pour calculer les statistiques
calculate_stats() {
    local metrics_file="$1"
    local field="$2"
    
    # Extraction des valeurs et calcul des statistiques
    local values=$(jq -r ".[].${field}" "$metrics_file" | tr '\n' ' ')
    local max_val=$(echo "$values" | tr ' ' '\n' | sort -nr | head -1)
    local avg_val=$(echo "$values" | tr ' ' '\n' | awk '{sum+=$1} END {printf "%.2f", sum/NR}')
    
    echo "{\"max\": $max_val, \"avg\": $avg_val}"
}

# Fonction principale de surveillance
start_stability_monitoring() {
    log_message "🚀 Démarrage de la surveillance de stabilité" "$GREEN"
    log_message "📱 Package: $PACKAGE_NAME" "$CYAN"
    log_message "⏱️  Durée: $MONITORING_HOURS heures" "$CYAN"
    log_message "🎯 Seuils: Crash < $CRASH_THRESHOLD%, ANR < $ANR_THRESHOLD%" "$CYAN"
    log_message "📊 Vérification toutes les $CHECK_INTERVAL_MINUTES minutes" "$CYAN"
    
    local end_time=$((START_TIME + MONITORING_HOURS * 3600))
    local end_time_readable=$(date -d "@$end_time" '+%Y-%m-%d %H:%M:%S')
    log_message "📅 Fin prévue: $end_time_readable" "$CYAN"
    log_message "" "$NC"
    
    # Fichier temporaire pour stocker les métriques
    local metrics_temp_file="/tmp/metrics_$$.json"
    echo "[]" > "$metrics_temp_file"
    
    local alerts_sent=()
    
    while [[ $(date +%s) -lt $end_time ]]; do
        local current_time=$(date +%s)
        local elapsed_hours=$(echo "scale=1; ($current_time - $START_TIME) / 3600" | bc -l)
        local remaining_hours=$(echo "scale=1; ($end_time - $current_time) / 3600" | bc -l)
        
        log_message "📊 Vérification des métriques (Elapsed: ${elapsed_hours}h, Remaining: ${remaining_hours}h)" "$YELLOW"
        
        # Récupération des métriques
        local metrics_json
        if metrics_json=$(get_play_console_metrics "$PACKAGE_NAME"); then
            # Ajout des métriques au fichier temporaire
            local temp_metrics=$(mktemp)
            jq ". + [$metrics_json]" "$metrics_temp_file" > "$temp_metrics" && mv "$temp_metrics" "$metrics_temp_file"
            
            # Extraction des valeurs
            local crash_rate=$(echo "$metrics_json" | jq -r '.crash_rate')
            local anr_rate=$(echo "$metrics_json" | jq -r '.anr_rate')
            local sessions=$(echo "$metrics_json" | jq -r '.total_sessions')
            
            # Affichage des métriques actuelles
            local crash_color="$GREEN"
            local anr_color="$GREEN"
            
            if [[ $(echo "$crash_rate > $CRASH_THRESHOLD" | bc -l) -eq 1 ]]; then
                crash_color="$RED"
            fi
            
            if [[ $(echo "$anr_rate > $ANR_THRESHOLD" | bc -l) -eq 1 ]]; then
                anr_color="$RED"
            fi
            
            log_message "   💥 Crash Rate: ${crash_rate}% (seuil: ${CRASH_THRESHOLD}%)" "$crash_color"
            log_message "   ⏳ ANR Rate: ${anr_rate}% (seuil: ${ANR_THRESHOLD}%)" "$anr_color"
            log_message "   📱 Sessions: $sessions" "$NC"
            
            # Vérification des seuils et alertes
            local current_hour=$(date +%Y%m%d%H)
            
            if [[ $(echo "$crash_rate > $CRASH_THRESHOLD" | bc -l) -eq 1 ]]; then
                local alert_key="crash-$current_hour"
                if [[ ! " ${alerts_sent[@]} " =~ " ${alert_key} " ]]; then
                    send_alert "Crash Rate Élevé" "Crash rate de ${crash_rate}% dépasse le seuil de ${CRASH_THRESHOLD}%" "Critical"
                    alerts_sent+=("$alert_key")
                fi
            fi
            
            if [[ $(echo "$anr_rate > $ANR_THRESHOLD" | bc -l) -eq 1 ]]; then
                local alert_key="anr-$current_hour"
                if [[ ! " ${alerts_sent[@]} " =~ " ${alert_key} " ]]; then
                    send_alert "ANR Rate Élevé" "ANR rate de ${anr_rate}% dépasse le seuil de ${ANR_THRESHOLD}%" "Critical"
                    alerts_sent+=("$alert_key")
                fi
            fi
            
        else
            log_message "❌ Erreur lors de la récupération des métriques" "$RED"
        fi
        
        log_message "" "$NC"
        
        # Attendre avant la prochaine vérification
        if [[ $(date +%s) -lt $end_time ]]; then
            sleep $((CHECK_INTERVAL_MINUTES * 60))
        fi
    done
    
    # Génération du rapport final
    log_message "📋 Génération du rapport final..." "$YELLOW"
    
    # Calcul des statistiques
    local crash_stats=$(calculate_stats "$metrics_temp_file" "crash_rate")
    local anr_stats=$(calculate_stats "$metrics_temp_file" "anr_rate")
    local total_sessions=$(jq '[.[].total_sessions] | add' "$metrics_temp_file")
    
    local max_crash_rate=$(echo "$crash_stats" | jq -r '.max')
    local avg_crash_rate=$(echo "$crash_stats" | jq -r '.avg')
    local max_anr_rate=$(echo "$anr_stats" | jq -r '.max')
    local avg_anr_rate=$(echo "$anr_stats" | jq -r '.avg')
    
    # Génération du rapport JSON
    local final_report=$(cat << EOF
{
    "package_name": "$PACKAGE_NAME",
    "monitoring_period": {
        "start": "$(date -d "@$START_TIME" -Iseconds)",
        "end": "$(date -Iseconds)",
        "duration_hours": $(echo "scale=2; ($(date +%s) - $START_TIME) / 3600" | bc -l)
    },
    "thresholds": {
        "crash_rate": $CRASH_THRESHOLD,
        "anr_rate": $ANR_THRESHOLD
    },
    "metrics": $(cat "$metrics_temp_file"),
    "summary": {
        "max_crash_rate": $max_crash_rate,
        "max_anr_rate": $max_anr_rate,
        "avg_crash_rate": $avg_crash_rate,
        "avg_anr_rate": $avg_anr_rate,
        "total_sessions": $total_sessions
    },
    "compliance": {
        "crash_rate_ok": $(echo "$max_crash_rate <= $CRASH_THRESHOLD" | bc -l),
        "anr_rate_ok": $(echo "$max_anr_rate <= $ANR_THRESHOLD" | bc -l),
        "ready_for_production": $(echo "($max_crash_rate <= $CRASH_THRESHOLD) && ($max_anr_rate <= $ANR_THRESHOLD)" | bc -l)
    }
}
EOF
)
    
    echo "$final_report" > "$REPORT_FILE"
    
    # Affichage du résumé final
    log_message "" "$NC"
    log_message "🏁 SURVEILLANCE TERMINÉE" "$GREEN"
    log_message "" "$NC"
    log_message "📊 RÉSUMÉ DES MÉTRIQUES:" "$CYAN"
    log_message "   💥 Crash Rate Max: ${max_crash_rate}% (Moy: ${avg_crash_rate}%)" "$NC"
    log_message "   ⏳ ANR Rate Max: ${max_anr_rate}% (Moy: ${avg_anr_rate}%)" "$NC"
    log_message "   📱 Total Sessions: $total_sessions" "$NC"
    log_message "" "$NC"
    
    # Verdict final
    local ready_for_production=$(echo "$final_report" | jq -r '.compliance.ready_for_production')
    local crash_rate_ok=$(echo "$final_report" | jq -r '.compliance.crash_rate_ok')
    local anr_rate_ok=$(echo "$final_report" | jq -r '.compliance.anr_rate_ok')
    
    if [[ "$ready_for_production" == "1" ]]; then
        log_message "✅ VERDICT: PRÊT POUR LA PRODUCTION" "$GREEN"
        log_message "   Toutes les métriques respectent les seuils requis" "$GREEN"
        log_message "   Vous pouvez procéder à la promotion vers le track Production" "$GREEN"
        local exit_code=0
    else
        log_message "❌ VERDICT: NON PRÊT POUR LA PRODUCTION" "$RED"
        if [[ "$crash_rate_ok" == "0" ]]; then
            log_message "   ⚠️  Crash rate trop élevé: ${max_crash_rate}% > ${CRASH_THRESHOLD}%" "$RED"
        fi
        if [[ "$anr_rate_ok" == "0" ]]; then
            log_message "   ⚠️  ANR rate trop élevé: ${max_anr_rate}% > ${ANR_THRESHOLD}%" "$RED"
        fi
        log_message "   Corrigez les problèmes et relancez un cycle de 48h" "$RED"
        local exit_code=1
    fi
    
    log_message "" "$NC"
    log_message "📄 Rapport détaillé sauvegardé: $REPORT_FILE" "$CYAN"
    log_message "📝 Log complet disponible: $LOG_FILE" "$CYAN"
    
    # Nettoyage
    rm -f "$metrics_temp_file"
    
    return $exit_code
}

# Vérification des dépendances
command -v jq >/dev/null 2>&1 || { echo >&2 "❌ jq est requis mais non installé. Installez-le avec: apt-get install jq ou brew install jq"; exit 1; }
command -v bc >/dev/null 2>&1 || { echo >&2 "❌ bc est requis mais non installé. Installez-le avec: apt-get install bc ou brew install bc"; exit 1; }

# Démarrage de la surveillance
if start_stability_monitoring; then
    exit 0  # Succès
else
    exit 1  # Échec - métriques non conformes
fi