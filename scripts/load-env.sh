#!/bin/bash
# Script Bash pour charger les variables d'environnement depuis .env
# Usage: source scripts/load-env.sh

ENV_FILE="${1:-.env}"

# Fonction pour charger les variables d'environnement
load_env_file() {
    local env_file="$1"
    
    if [[ ! -f "$env_file" ]]; then
        echo "⚠️  Fichier .env non trouvé: $env_file" >&2
        echo "💡 Créez un fichier .env basé sur .env.example"
        return 1
    fi
    
    echo "📁 Chargement des variables d'environnement depuis: $env_file"
    
    local env_vars=0
    
    # Lire le fichier ligne par ligne
    while IFS= read -r line || [[ -n "$line" ]]; do
        # Supprimer les espaces en début et fin
        line=$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
        
        # Ignorer les lignes vides et les commentaires
        if [[ -n "$line" && ! "$line" =~ ^# ]]; then
            if [[ "$line" =~ ^([^=]+)=(.*)$ ]]; then
                local name="${BASH_REMATCH[1]}"
                local value="${BASH_REMATCH[2]}"
                
                # Supprimer les guillemets si présents
                if [[ "$value" =~ ^\"(.*)\"$ ]]; then
                    value="${BASH_REMATCH[1]}"
                fi
                
                # Exporter la variable d'environnement
                export "$name"="$value"
                ((env_vars++))
                
                # Afficher la variable (masquer les clés sensibles)
                if [[ "$name" =~ (KEY|PASSWORD|SECRET|TOKEN) ]]; then
                    local masked_value="${value:0:8}***"
                    echo "  ✅ $name = $masked_value"
                else
                    echo "  ✅ $name = $value"
                fi
            fi
        fi
    done < "$env_file"
    
    echo "🎉 $env_vars variables d'environnement chargées avec succès!"
    
    # Vérifier les variables critiques
    local critical_vars=("GOOGLE_API_KEY" "GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL" "GOOGLE_PLAY_JSON_KEY_PATH")
    local missing=()
    
    for var in "${critical_vars[@]}"; do
        if [[ -z "${!var}" ]]; then
            missing+=("$var")
        fi
    done
    
    if [[ ${#missing[@]} -gt 0 ]]; then
        echo "⚠️  Variables manquantes: ${missing[*]}" >&2
        echo "💡 Vérifiez votre fichier .env"
    else
        echo "✅ Toutes les variables critiques sont configurées"
    fi
}

# Charger le fichier .env
load_env_file "$ENV_FILE"

# Si le script est exécuté directement (pas sourcé)
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    echo ""
    echo "💡 Pour utiliser ce script dans d'autres scripts Bash:"
    echo "   source scripts/load-env.sh"
    echo ""
    echo "🔧 Variables d'environnement disponibles:"
    echo "   \$GOOGLE_API_KEY"
    echo "   \$GOOGLE_PLAY_JSON_KEY_PATH"
    echo "   \$GOOGLE_PLAY_JSON_KEY_DATA"
fi