# Script de déploiement automatisé pour tous les tracks Google Play
# Solution propre et complète pour le déploiement

param(
    [switch]$Internal,
    [switch]$Alpha, 
    [switch]$Beta,
    [switch]$Production,
    [switch]$All,
    [string]$ReleaseNotes = "Nouvelle version avec améliorations et corrections de bugs"
)

$ErrorActionPreference = "Stop"

Write-Host "🚀 Déploiement Automatisé Google Play Store" -ForegroundColor Green
Write-Host "===========================================" -ForegroundColor Green
Write-Host ""

# Fonction de déploiement
function Deploy-ToTrack {
    param(
        [string]$Track,
        [string]$Notes
    )
    
    Write-Host "📦 Déploiement vers le track: $Track" -ForegroundColor Cyan
    Write-Host "📝 Notes de version: $Notes" -ForegroundColor Gray
    Write-Host ""
    
    try {
        .\scripts\deploy-google-play.ps1 -Track $Track -AutoIncrement -ReleaseNotes $Notes
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Succès: $Track" -ForegroundColor Green
            return $true
        } else {
            Write-Host "❌ Échec: $Track" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host "❌ Erreur lors du déploiement vers $Track : $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# Validation des paramètres
if (-not ($Internal -or $Alpha -or $Beta -or $Production -or $All)) {
    Write-Host "❌ Erreur: Vous devez spécifier au moins un track" -ForegroundColor Red
    Write-Host ""
    Write-Host "Usage:" -ForegroundColor Yellow
    Write-Host "  .\scripts\deploy-all-tracks.ps1 -Internal" -ForegroundColor White
    Write-Host "  .\scripts\deploy-all-tracks.ps1 -Alpha" -ForegroundColor White
    Write-Host "  .\scripts\deploy-all-tracks.ps1 -Beta" -ForegroundColor White
    Write-Host "  .\scripts\deploy-all-tracks.ps1 -Production" -ForegroundColor White
    Write-Host "  .\scripts\deploy-all-tracks.ps1 -All" -ForegroundColor White
    Write-Host ""
    Write-Host "Options:" -ForegroundColor Yellow
    Write-Host "  -ReleaseNotes \"Vos notes de version\"" -ForegroundColor White
    exit 1
}

$deployments = @()
$results = @()

# Définir les déploiements à effectuer
if ($All) {
    $deployments = @('internal', 'alpha', 'beta')
    Write-Host "🎯 Déploiement complet: internal → alpha → beta" -ForegroundColor Yellow
} else {
    if ($Internal) { $deployments += 'internal' }
    if ($Alpha) { $deployments += 'alpha' }
    if ($Beta) { $deployments += 'beta' }
    if ($Production) { $deployments += 'production' }
}

Write-Host "📋 Tracks sélectionnés: $($deployments -join ', ')" -ForegroundColor Cyan
Write-Host ""

# Exécuter les déploiements
foreach ($track in $deployments) {
    Write-Host "" 
    Write-Host "🔄 Démarrage du déploiement: $track" -ForegroundColor Yellow
    Write-Host "----------------------------------------" -ForegroundColor Gray
    
    $success = Deploy-ToTrack -Track $track -Notes $ReleaseNotes
    $results += @{
        Track = $track
        Success = $success
        Timestamp = Get-Date
    }
    
    if ($success) {
        Write-Host "✅ $track : SUCCÈS" -ForegroundColor Green
    } else {
        Write-Host "❌ $track : ÉCHEC" -ForegroundColor Red
        
        # Demander si on continue en cas d'échec
        if ($deployments.IndexOf($track) -lt ($deployments.Count - 1)) {
            $continue = Read-Host "Continuer avec les tracks suivants? (o/N)"
            if ($continue -ne 'o' -and $continue -ne 'O' -and $continue -ne 'oui') {
                Write-Host "🛑 Arrêt demandé par l'utilisateur" -ForegroundColor Yellow
                break
            }
        }
    }
    
    # Pause entre les déploiements
    if ($deployments.IndexOf($track) -lt ($deployments.Count - 1)) {
        Write-Host "⏳ Pause de 5 secondes avant le prochain déploiement..." -ForegroundColor Gray
        Start-Sleep -Seconds 5
    }
}

# Résumé final
Write-Host ""
Write-Host "📊 RÉSUMÉ DES DÉPLOIEMENTS" -ForegroundColor Green
Write-Host "==========================" -ForegroundColor Green

$successCount = 0
$failureCount = 0

foreach ($result in $results) {
    $status = if ($result.Success) { "✅ SUCCÈS" } else { "❌ ÉCHEC" }
    $color = if ($result.Success) { "Green" } else { "Red" }
    
    Write-Host "$($result.Track.PadRight(12)) : $status" -ForegroundColor $color
    
    if ($result.Success) {
        $successCount++
    } else {
        $failureCount++
    }
}

Write-Host ""
Write-Host "📈 Statistiques:" -ForegroundColor Cyan
Write-Host "   Succès: $successCount" -ForegroundColor Green
Write-Host "   Échecs: $failureCount" -ForegroundColor Red
Write-Host "   Total:  $($results.Count)" -ForegroundColor Gray

if ($failureCount -eq 0) {
    Write-Host ""
    Write-Host "🎉 Tous les déploiements ont réussi!" -ForegroundColor Green
    Write-Host "🔗 Vérifiez Google Play Console: https://play.google.com/console" -ForegroundColor Cyan
} else {
    Write-Host ""
    Write-Host "⚠️  Certains déploiements ont échoué. Vérifiez les logs ci-dessus." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "✨ Déploiement terminé" -ForegroundColor Green