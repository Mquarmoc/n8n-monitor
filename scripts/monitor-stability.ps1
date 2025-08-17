# Script de surveillance des métriques de stabilité pour Google Play Store
# Surveille le crash rate et ANR rate pendant 48h avant promotion en production

param(
    [string]$PackageName = "com.example.n8nmonitor",
    [int]$MonitoringHours = 48,
    [double]$CrashThreshold = 2.0,
    [double]$ANRThreshold = 1.0,
    [int]$CheckIntervalMinutes = 30,
    [switch]$Verbose
)

# Configuration
$LogFile = "stability-monitoring.log"
$ReportFile = "stability-report-$(Get-Date -Format 'yyyyMMdd-HHmmss').json"
$StartTime = Get-Date
$EndTime = $StartTime.AddHours($MonitoringHours)

# Couleurs pour l'affichage
function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    Write-Host $Message -ForegroundColor $Color
    Add-Content -Path $LogFile -Value "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') - $Message"
}

# Fonction pour simuler la récupération des métriques Google Play
# En production, ceci utiliserait l'API Google Play Console
function Get-PlayConsoleMetrics {
    param([string]$PackageName)
    
    # Simulation des métriques (à remplacer par l'API réelle)
    $crashRate = Get-Random -Minimum 0.1 -Maximum 3.0
    $anrRate = Get-Random -Minimum 0.05 -Maximum 1.5
    $sessions = Get-Random -Minimum 100 -Maximum 1000
    
    return @{
        CrashRate = [math]::Round($crashRate, 2)
        ANRRate = [math]::Round($anrRate, 2)
        TotalSessions = $sessions
        Timestamp = Get-Date
    }
}

# Fonction pour envoyer une alerte
function Send-Alert {
    param(
        [string]$Subject,
        [string]$Message,
        [string]$Severity = "Warning"
    )
    
    Write-ColorOutput "🚨 ALERTE [$Severity]: $Subject" "Red"
    Write-ColorOutput "   $Message" "Yellow"
    
    # Ici, vous pourriez ajouter l'envoi d'email, Slack, Teams, etc.
    # Send-MailMessage -To "team@company.com" -Subject $Subject -Body $Message
}

# Fonction pour générer le rapport
function Generate-Report {
    param([array]$MetricsHistory)
    
    $report = @{
        PackageName = $PackageName
        MonitoringPeriod = @{
            Start = $StartTime
            End = Get-Date
            DurationHours = [math]::Round(((Get-Date) - $StartTime).TotalHours, 2)
        }
        Thresholds = @{
            CrashRate = $CrashThreshold
            ANRRate = $ANRThreshold
        }
        Metrics = $MetricsHistory
        Summary = @{
            MaxCrashRate = ($MetricsHistory | Measure-Object -Property CrashRate -Maximum).Maximum
            MaxANRRate = ($MetricsHistory | Measure-Object -Property ANRRate -Maximum).Maximum
            AvgCrashRate = [math]::Round(($MetricsHistory | Measure-Object -Property CrashRate -Average).Average, 2)
            AvgANRRate = [math]::Round(($MetricsHistory | Measure-Object -Property ANRRate -Average).Average, 2)
            TotalSessions = ($MetricsHistory | Measure-Object -Property TotalSessions -Sum).Sum
        }
    }
    
    # Évaluation de la conformité
    $report.Compliance = @{
        CrashRateOK = $report.Summary.MaxCrashRate -le $CrashThreshold
        ANRRateOK = $report.Summary.MaxANRRate -le $ANRThreshold
        ReadyForProduction = ($report.Summary.MaxCrashRate -le $CrashThreshold) -and ($report.Summary.MaxANRRate -le $ANRThreshold)
    }
    
    $report | ConvertTo-Json -Depth 10 | Out-File -FilePath $ReportFile -Encoding UTF8
    return $report
}

# Fonction principale de surveillance
function Start-StabilityMonitoring {
    Write-ColorOutput "🚀 Démarrage de la surveillance de stabilité" "Green"
    Write-ColorOutput "📱 Package: $PackageName" "Cyan"
    Write-ColorOutput "⏱️  Durée: $MonitoringHours heures" "Cyan"
    Write-ColorOutput "🎯 Seuils: Crash < $CrashThreshold%, ANR < $ANRThreshold%" "Cyan"
    Write-ColorOutput "📊 Vérification toutes les $CheckIntervalMinutes minutes" "Cyan"
    Write-ColorOutput "📅 Fin prévue: $($EndTime.ToString('yyyy-MM-dd HH:mm:ss'))" "Cyan"
    Write-ColorOutput "" "White"
    
    $metricsHistory = @()
    $alertsSent = @()
    
    while ((Get-Date) -lt $EndTime) {
        $currentTime = Get-Date
        $elapsed = ($currentTime - $StartTime).TotalHours
        $remaining = ($EndTime - $currentTime).TotalHours
        
        Write-ColorOutput "📊 Vérification des métriques (Elapsed: $([math]::Round($elapsed, 1))h, Remaining: $([math]::Round($remaining, 1))h)" "Yellow"
        
        try {
            # Récupération des métriques
            $metrics = Get-PlayConsoleMetrics -PackageName $PackageName
            $metricsHistory += $metrics
            
            # Affichage des métriques actuelles
            $crashColor = if ($metrics.CrashRate -le $CrashThreshold) { "Green" } else { "Red" }
            $anrColor = if ($metrics.ANRRate -le $ANRThreshold) { "Green" } else { "Red" }
            
            Write-ColorOutput "   💥 Crash Rate: $($metrics.CrashRate)% (seuil: $CrashThreshold%)" $crashColor
            Write-ColorOutput "   ⏳ ANR Rate: $($metrics.ANRRate)% (seuil: $ANRThreshold%)" $anrColor
            Write-ColorOutput "   📱 Sessions: $($metrics.TotalSessions)" "White"
            
            # Vérification des seuils et alertes
            if ($metrics.CrashRate -gt $CrashThreshold) {
                $alertKey = "crash-$($currentTime.ToString('yyyyMMddHH'))"
                if ($alertKey -notin $alertsSent) {
                    Send-Alert -Subject "Crash Rate Élevé" -Message "Crash rate de $($metrics.CrashRate)% dépasse le seuil de $CrashThreshold%" -Severity "Critical"
                    $alertsSent += $alertKey
                }
            }
            
            if ($metrics.ANRRate -gt $ANRThreshold) {
                $alertKey = "anr-$($currentTime.ToString('yyyyMMddHH'))"
                if ($alertKey -notin $alertsSent) {
                    Send-Alert -Subject "ANR Rate Élevé" -Message "ANR rate de $($metrics.ANRRate)% dépasse le seuil de $ANRThreshold%" -Severity "Critical"
                    $alertsSent += $alertKey
                }
            }
            
        } catch {
            Write-ColorOutput "❌ Erreur lors de la récupération des métriques: $($_.Exception.Message)" "Red"
        }
        
        Write-ColorOutput "" "White"
        
        # Attendre avant la prochaine vérification
        if ((Get-Date) -lt $EndTime) {
            Start-Sleep -Seconds ($CheckIntervalMinutes * 60)
        }
    }
    
    # Génération du rapport final
    Write-ColorOutput "📋 Génération du rapport final..." "Yellow"
    $finalReport = Generate-Report -MetricsHistory $metricsHistory
    
    # Affichage du résumé final
    Write-ColorOutput "" "White"
    Write-ColorOutput "🏁 SURVEILLANCE TERMINÉE" "Green"
    Write-ColorOutput "" "White"
    Write-ColorOutput "📊 RÉSUMÉ DES MÉTRIQUES:" "Cyan"
    Write-ColorOutput "   💥 Crash Rate Max: $($finalReport.Summary.MaxCrashRate)% (Moy: $($finalReport.Summary.AvgCrashRate)%)" "White"
    Write-ColorOutput "   ⏳ ANR Rate Max: $($finalReport.Summary.MaxANRRate)% (Moy: $($finalReport.Summary.AvgANRRate)%)" "White"
    Write-ColorOutput "   📱 Total Sessions: $($finalReport.Summary.TotalSessions)" "White"
    Write-ColorOutput "" "White"
    
    # Verdict final
    if ($finalReport.Compliance.ReadyForProduction) {
        Write-ColorOutput "✅ VERDICT: PRÊT POUR LA PRODUCTION" "Green"
        Write-ColorOutput "   Toutes les métriques respectent les seuils requis" "Green"
        Write-ColorOutput "   Vous pouvez procéder à la promotion vers le track Production" "Green"
    } else {
        Write-ColorOutput "❌ VERDICT: NON PRÊT POUR LA PRODUCTION" "Red"
        if (-not $finalReport.Compliance.CrashRateOK) {
            Write-ColorOutput "   ⚠️  Crash rate trop élevé: $($finalReport.Summary.MaxCrashRate)% > $CrashThreshold%" "Red"
        }
        if (-not $finalReport.Compliance.ANRRateOK) {
            Write-ColorOutput "   ⚠️  ANR rate trop élevé: $($finalReport.Summary.MaxANRRate)% > $ANRThreshold%" "Red"
        }
        Write-ColorOutput "   Corrigez les problèmes et relancez un cycle de 48h" "Red"
    }
    
    Write-ColorOutput "" "White"
    Write-ColorOutput "📄 Rapport détaillé sauvegardé: $ReportFile" "Cyan"
    Write-ColorOutput "📝 Log complet disponible: $LogFile" "Cyan"
    
    return $finalReport
}

# Fonction d'aide
function Show-Help {
    Write-Host @"
🔍 Script de Surveillance de Stabilité - Google Play Store

USAGE:
    .\monitor-stability.ps1 [OPTIONS]

OPTIONS:
    -PackageName STRING     Nom du package Android (défaut: com.example.n8nmonitor)
    -MonitoringHours INT    Durée de surveillance en heures (défaut: 48)
    -CrashThreshold DOUBLE  Seuil de crash rate en % (défaut: 2.0)
    -ANRThreshold DOUBLE    Seuil d'ANR rate en % (défaut: 1.0)
    -CheckIntervalMinutes INT Intervalle entre vérifications (défaut: 30)
    -Verbose               Mode verbeux
    -Help                  Affiche cette aide

EXEMPLES:
    # Surveillance standard de 48h
    .\monitor-stability.ps1
    
    # Surveillance de 24h avec seuils personnalisés
    .\monitor-stability.ps1 -MonitoringHours 24 -CrashThreshold 1.5 -ANRThreshold 0.5
    
    # Surveillance rapide pour tests (1h, vérification toutes les 5 min)
    .\monitor-stability.ps1 -MonitoringHours 1 -CheckIntervalMinutes 5

NOTE:
    Ce script simule actuellement les métriques Google Play.
    En production, intégrez l'API Google Play Console pour des données réelles.
"@
}

# Point d'entrée principal
if ($args -contains "-Help" -or $args -contains "--help" -or $args -contains "/?") {
    Show-Help
    exit 0
}

# Validation des paramètres
if ($MonitoringHours -le 0) {
    Write-Error "La durée de surveillance doit être positive"
    exit 1
}

if ($CrashThreshold -le 0 -or $ANRThreshold -le 0) {
    Write-Error "Les seuils doivent être positifs"
    exit 1
}

if ($CheckIntervalMinutes -le 0) {
    Write-Error "L'intervalle de vérification doit être positif"
    exit 1
}

# Démarrage de la surveillance
try {
    $result = Start-StabilityMonitoring
    
    # Code de sortie basé sur le résultat
    if ($result.Compliance.ReadyForProduction) {
        exit 0  # Succès
    } else {
        exit 1  # Échec - métriques non conformes
    }
} catch {
    Write-ColorOutput "❌ Erreur fatale: $($_.Exception.Message)" "Red"
    exit 2  # Erreur système
}