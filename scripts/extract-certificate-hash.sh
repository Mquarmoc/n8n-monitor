#!/bin/bash

# Script pour extraire le hash SHA-256 du certificat SSL d'un domaine
# Usage: ./extract-certificate-hash.sh [domaine]

set -e

# Vérifier si un domaine a été fourni
if [ -z "$1" ]; then
    echo "Erreur: Veuillez spécifier un domaine."
    echo "Usage: $0 [domaine]"
    echo "Exemple: $0 your-n8n-domain.com"
    exit 1
fi

DOMAIN=$1

echo "🔐 Extraction du hash SHA-256 pour le domaine: $DOMAIN"

# Extraire le hash SHA-256 du certificat
HASH=$(openssl s_client -connect ${DOMAIN}:443 -servername ${DOMAIN} \
       < /dev/null 2>/dev/null | openssl x509 -pubkey -noout | \
       openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | base64)

if [ -z "$HASH" ]; then
    echo "❌ Erreur: Impossible d'extraire le hash du certificat."
    exit 1
fi

echo "✅ Hash SHA-256 extrait avec succès!"
echo ""
echo "Hash complet: $HASH"
echo ""
echo "Pour l'utiliser dans NetworkModule.kt:"
echo ""
echo "CertificatePinner.Builder()"
echo "    .add(\"${DOMAIN}\", \"sha256/${HASH}\")"
echo "    .build()"
echo ""
echo "🔧 Pensez à reconstruire l'application avec ./gradlew clean bundleRelease après la modification."