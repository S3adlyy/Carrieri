# ============================================================
# 🚀 DÉMARRAGE RAPIDE - GÉNÉRATEUR IA GEMINI
# ============================================================

Write-Host "=" * 80
Write-Host "🚀 INSTALLATION ET CONFIGURATION GEMINI" -ForegroundColor Cyan
Write-Host "=" * 80

# Étape 1 : Installation
Write-Host "`n1️⃣ Installation de google-generativeai..." -ForegroundColor Yellow
C:\Users\onsna\AppData\Local\Programs\Python\Python310\python.exe -m pip install google-generativeai flask flask-cors

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Installation réussie`n" -ForegroundColor Green
} else {
    Write-Host "❌ Erreur d'installation`n" -ForegroundColor Red
    exit 1
}

# Étape 2 : Vérification
Write-Host "2️⃣ Vérification de l'installation..." -ForegroundColor Yellow
$verification = C:\Users\onsna\AppData\Local\Programs\Python\Python310\python.exe -c "import google.generativeai; print('OK')" 2>&1

if ($verification -eq "OK") {
    Write-Host "✅ Vérification réussie`n" -ForegroundColor Green
} else {
    Write-Host "❌ Erreur de vérification`n" -ForegroundColor Red
    exit 1
}

# Étape 3 : Configuration de la clé API
Write-Host "3️⃣ Configuration de la clé API Gemini..." -ForegroundColor Yellow
Write-Host ""
Write-Host "📝 Pour obtenir votre clé API GRATUITE :" -ForegroundColor Cyan
Write-Host "   1. Ouvrez : https://makersuite.google.com/app/apikey"
Write-Host "   2. Connectez-vous avec Google"
Write-Host "   3. Cliquez sur 'Create API Key'"
Write-Host "   4. Copiez la clé (format : AIza...)"
Write-Host ""

$apiKey = Read-Host "Collez votre clé API Gemini ici"

if ($apiKey -ne "") {
    # Définir la variable d'environnement pour la session actuelle
    $env:GEMINI_API_KEY = $apiKey
    Write-Host "✅ Clé API configurée pour cette session`n" -ForegroundColor Green

    # Proposer de sauvegarder de manière permanente
    $save = Read-Host "Voulez-vous sauvegarder cette clé de manière permanente? (o/n)"
    if ($save -eq "o") {
        [Environment]::SetEnvironmentVariable("GEMINI_API_KEY", $apiKey, "User")
        Write-Host "✅ Clé API sauvegardée de manière permanente`n" -ForegroundColor Green
    }
} else {
    Write-Host "⚠️  Aucune clé API fournie. Vous devrez la configurer manuellement." -ForegroundColor Yellow
    Write-Host "   Utilisez : `$env:GEMINI_API_KEY = 'votre_cle'`n"
}

# Étape 4 : Lancer le service
Write-Host "4️⃣ Démarrage du service API..." -ForegroundColor Yellow
Write-Host ""
Write-Host "Le service va démarrer sur http://localhost:5000" -ForegroundColor Cyan
Write-Host "Appuyez sur Ctrl+C pour l'arrêter" -ForegroundColor Cyan
Write-Host ""
Write-Host "=" * 80
Write-Host ""

cd C:\Users\onsna\OneDrive\Desktop\Goffres\ml_service
C:\Users\onsna\AppData\Local\Programs\Python\Python310\python.exe api_service_gemini.py

