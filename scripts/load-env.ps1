# Script PowerShell pour charger les variables d'environnement depuis .env
# Usage: . .\scripts\load-env.ps1

param(
    [string]$EnvFile = ".env"
)

# Fonction pour charger les variables d'environnement
function Load-EnvFile {
    param([string]$Path)
    
    if (-not (Test-Path $Path)) {
        Write-Warning "Fichier .env non trouve: $Path"
        Write-Host "Creez un fichier .env base sur .env.example"
        return
    }
    
    Write-Host "Chargement des variables d'environnement depuis: $Path"
    
    $envVars = 0
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        
        # Ignorer les lignes vides et les commentaires
        if ($line -and -not $line.StartsWith('#')) {
            $parts = $line.Split('=', 2)
            if ($parts.Length -eq 2) {
                $name = $parts[0].Trim()
                $value = $parts[1].Trim()
                
                # Supprimer les guillemets si presents
                if ($value.StartsWith('"') -and $value.EndsWith('"')) {
                    $value = $value.Substring(1, $value.Length - 2)
                }
                
                # Definir la variable d'environnement
                [Environment]::SetEnvironmentVariable($name, $value, 'Process')
                $envVars++
                
                # Afficher la variable (masquer les cles sensibles)
                if ($name -like '*KEY*' -or $name -like '*PASSWORD*' -or $name -like '*SECRET*' -or $name -like '*TOKEN*') {
                    $maskedLength = [Math]::Min(8, $value.Length)
                    $maskedValue = $value.Substring(0, $maskedLength) + "***"
                    Write-Host "  $name = $maskedValue"
                } else {
                    Write-Host "  $name = $value"
                }
            }
        }
    }
    
    Write-Host "$envVars variables d'environnement chargees avec succes!"
    
    # Verifier les variables critiques
    $criticalVars = @('GOOGLE_API_KEY', 'GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL', 'GOOGLE_PLAY_JSON_KEY_PATH')
    $missing = @()
    
    foreach ($var in $criticalVars) {
        if (-not [Environment]::GetEnvironmentVariable($var, 'Process')) {
            $missing += $var
        }
    }
    
    if ($missing.Count -gt 0) {
        Write-Warning "Variables manquantes: $($missing -join ', ')"
        Write-Host "Verifiez votre fichier .env"
    } else {
        Write-Host "Toutes les variables critiques sont configurees"
    }
}

# Charger le fichier .env
Load-EnvFile -Path $EnvFile

# Exporter la fonction pour utilisation dans d'autres scripts
if ($MyInvocation.InvocationName -ne '.') {
    Write-Host ""
    Write-Host "Pour utiliser ce script dans d'autres scripts PowerShell:"
    Write-Host "   . .\scripts\load-env.ps1"
    Write-Host ""
    Write-Host "Variables d'environnement disponibles:"
    Write-Host "   `$env:GOOGLE_API_KEY"
    Write-Host "   `$env:GOOGLE_PLAY_JSON_KEY_PATH"
    Write-Host "   `$env:GOOGLE_PLAY_JSON_KEY_DATA"
}