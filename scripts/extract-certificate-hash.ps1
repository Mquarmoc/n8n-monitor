# Script pour extraire le hash SHA-256 du certificat SSL d'un domaine
# Usage: ./extract-certificate-hash.ps1 [domaine]

param(
    [Parameter(Mandatory=$true, Position=0)]
    [string]$Domain
)

Write-Host "🔐 Extraction du hash SHA-256 pour le domaine: $Domain" -ForegroundColor Cyan

# Vérifier si OpenSSL est disponible
try {
    $opensslVersion = openssl version
    Write-Host "OpenSSL détecté: $opensslVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Erreur: OpenSSL n'est pas installé ou n'est pas dans le PATH." -ForegroundColor Red
    Write-Host "Veuillez installer OpenSSL pour Windows et l'ajouter au PATH." -ForegroundColor Yellow
    exit 1
}

# Extraire le hash SHA-256 du certificat
try {
    # Créer un fichier temporaire pour stocker la sortie
    $tempCert = [System.IO.Path]::GetTempFileName()
    
    # Exécuter la commande OpenSSL pour extraire le certificat
    $null = openssl s_client -connect ${Domain}:443 -servername ${Domain} -showcerts </dev/null 2>$null | Out-File -FilePath $tempCert
    
    # Extraire la clé publique et calculer le hash
    $pubkey = Get-Content -Path $tempCert | openssl x509 -pubkey -noout
    $pubkey | Out-File -FilePath "$tempCert.pubkey"
    $derKey = Get-Content -Path "$tempCert.pubkey" | openssl pkey -pubin -outform der
    $derKey | Out-File -FilePath "$tempCert.der" -Encoding Byte
    $hash = Get-Content -Path "$tempCert.der" -Encoding Byte | openssl dgst -sha256 -binary | openssl base64
    
    # Nettoyer les fichiers temporaires
    Remove-Item -Path $tempCert -Force
    Remove-Item -Path "$tempCert.pubkey" -Force
    Remove-Item -Path "$tempCert.der" -Force
    
    if ([string]::IsNullOrEmpty($hash)) {
        throw "Hash vide"
    }
    
    # Nettoyer le hash (supprimer les espaces et les retours à la ligne)
    $hash = $hash.Trim()
} catch {
    Write-Host "❌ Erreur: Impossible d'extraire le hash du certificat." -ForegroundColor Red
    Write-Host "Détails: $_" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Hash SHA-256 extrait avec succès!" -ForegroundColor Green
Write-Host ""
Write-Host "Hash complet: $hash" -ForegroundColor Yellow
Write-Host ""
Write-Host "Pour l'utiliser dans NetworkModule.kt:" -ForegroundColor Cyan
Write-Host ""
Write-Host "CertificatePinner.Builder()" -ForegroundColor White
Write-Host "    .add(\"${Domain}\", \"sha256/${hash}\")" -ForegroundColor White
Write-Host "    .build()" -ForegroundColor White
Write-Host ""
Write-Host "🔧 Pensez à reconstruire l'application avec ./gradlew clean bundleRelease après la modification." -ForegroundColor Cyan