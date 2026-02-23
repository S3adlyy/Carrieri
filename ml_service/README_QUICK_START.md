# 🚀 DÉMARRAGE RAPIDE - GÉNÉRATEUR IA GEMINI

## ⚡ Installation en 3 commandes (5 minutes)

### 1️⃣ Installer la dépendance

```powershell
cd C:\Users\onsna\OneDrive\Desktop\Goffres\ml_service
C:\Users\onsna\AppData\Local\Programs\Python\Python310\python.exe -m pip install google-generativeai
```

### 2️⃣ Obtenir une clé API (GRATUIT)

1. Aller sur : **https://makersuite.google.com/app/apikey**
2. Se connecter avec Google
3. Cliquer sur **"Create API Key"**
4. Copier la clé (ex: `AIzaSy...`)

### 3️⃣ Configurer et lancer

```powershell
# Définir la clé API
$env:GEMINI_API_KEY = "VOTRE_CLE_ICI"

# Lancer le service
python api_service_gemini.py
```

**✅ C'EST TOUT !** Le service est prêt sur `http://localhost:5000`

---

## 🧪 Tester

**Dans un navigateur :**
```
http://localhost:5000/test
```

**Ou via PowerShell :**
```powershell
Invoke-RestMethod -Uri "http://localhost:5000/test"
```

---

## 🔗 Utiliser dans JavaFX

```java
import services.AIGeneratorService;

// Générer une description
String description = AIGeneratorService.generateDescription(
    "Développeur Java",
    "Informatique", 
    "Java, Spring Boot, MySQL",
    "Bac+5",
    "3 ans"
);

// Afficher dans un TextArea
descriptionTextArea.setText(description);
```

---

## 📚 Documentation complète

Voir **GUIDE_GEMINI_COMPLET.md** pour :
- Intégration JavaFX complète
- Troubleshooting
- FAQ
- Conseils pour la soutenance

---

## ⚠️ Problèmes courants

**"Service non disponible"** → Vérifier que Python tourne  
**"API key invalid"** → Vérifier la clé copiée correctement  
**"Module not found"** → Réinstaller : `pip install google-generativeai`

---

## 🎉 Avantages

✅ **Gratuit** - 60 requêtes/minute  
✅ **Rapide** - 1-3 secondes  
✅ **Qualité pro** - Descriptions cohérentes  
✅ **Aucun entraînement** - Prêt à l'emploi  
✅ **0MB** - Pas de gros fichiers

---

**C'est parti ! 🚀**

