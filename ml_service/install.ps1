# Script PowerShell d'installation automatique
# Exécutez ce script après avoir installé Python 64-bit

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "🚀 INSTALLATION AUTOMATIQUE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 1. Vérifier Python
Write-Host "`n1️⃣ Vérification de Python..." -ForegroundColor Yellow
python --version
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Python non trouvé !" -ForegroundColor Red
    Write-Host "Veuillez installer Python 64-bit d'abord" -ForegroundColor Red
    exit 1
}

# 2. Mettre à jour pip
Write-Host "`n2️⃣ Mise à jour de pip..." -ForegroundColor Yellow
python -m pip install --upgrade pip
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erreur lors de la mise à jour de pip" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Pip mis à jour" -ForegroundColor Green

# 3. Installer PyTorch
Write-Host "`n3️⃣ Installation de PyTorch (cela peut prendre 5-10 minutes)..." -ForegroundColor Yellow
pip install torch torchvision --index-url https://download.pytorch.org/whl/cpu
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erreur lors de l'installation de PyTorch" -ForegroundColor Red
    exit 1
}
Write-Host "✅ PyTorch installé" -ForegroundColor Green

# 4. Aller dans le dossier ml_service
Write-Host "`n4️⃣ Changement de dossier..." -ForegroundColor Yellow
Set-Location -Path "C:\Users\onsna\OneDrive\Desktop\Goffres\ml_service"
Write-Host "✅ Dans ml_service/" -ForegroundColor Green

# 5. Installer les autres dépendances
Write-Host "`n5️⃣ Installation des autres dépendances..." -ForegroundColor Yellow
pip install -r requirements.txt
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erreur lors de l'installation des dépendances" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Toutes les dépendances installées" -ForegroundColor Green

# 6. Vérifier l'installation
Write-Host "`n6️⃣ Vérification de l'installation..." -ForegroundColor Yellow
python check_installation.py

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "✅ INSTALLATION RÉUSSIE !" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "`n🚀 Prochaines étapes :" -ForegroundColor Yellow
    Write-Host "   1. python prepare_data.py" -ForegroundColor White
    Write-Host "   2. python train_model.py" -ForegroundColor White
    Write-Host "   3. python test_generation.py" -ForegroundColor White
    Write-Host "   4. python api_service.py" -ForegroundColor White
} else {
    Write-Host "`n❌ Des problèmes ont été détectés" -ForegroundColor Red
    Write-Host "Consultez le fichier APRES_INSTALLATION.md" -ForegroundColor Yellow
}

