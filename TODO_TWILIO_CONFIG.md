# 📋 TODO : Compléter la configuration Twilio

## ⚠️ IMPORTANT
La console Twilio avait des problèmes de chargement lors de votre première connexion.
Le SMS est **temporairement désactivé** (`sms.enabled=false`).

## 🔄 Une fois que la console Twilio fonctionne

### Étape 1 : Accéder à la console
Allez sur : **https://console.twilio.com/**

**Astuce** : Si le menu ne charge pas, essayez ces URLs directes :
- https://console.twilio.com/us1/develop/explore
- https://console.twilio.com/us1/account/manage-account/general-settings

### Étape 2 : Récupérer vos 3 credentials

#### 1️⃣ Account SID
- **Où ?** Dashboard principal (page d'accueil)
- **Format :** Commence par `AC...`
- **Exemple :** `AC1234567890abcdef1234567890abcd`
- **Ma valeur :** ________________________________

#### 2️⃣ Auth Token
- **Où ?** Juste sous le Account SID
- **Action :** Cliquez sur le bouton **"Show"** pour le révéler
- **Exemple :** `1234567890abcdef1234567890abcd`
- **Ma valeur :** ________________________________

#### 3️⃣ Phone Number (Numéro Twilio)
- **Où ?** Phone Numbers → Manage → Active numbers
- **Ou :** Cliquez sur "Get a trial phone number" (gratuit)
- **Format :** `+1234567890` (avec le +)
- **Ma valeur :** ________________________________

### Étape 3 : Modifier config.properties

Ouvrez : `src/main/resources/config.properties`

Remplacez :
```properties
twilio.account.sid=YOUR_ACCOUNT_SID_HERE
twilio.auth.token=YOUR_AUTH_TOKEN_HERE
twilio.phone.number=YOUR_TWILIO_PHONE_NUMBER_HERE
sms.enabled=false
```

Par :
```properties
twilio.account.sid=AC1234567890abcdef1234567890abcd
twilio.auth.token=1234567890abcdef1234567890abcd
twilio.phone.number=+1234567890
sms.enabled=true
```

**⚠️ Important :**
- Remplacez les valeurs par les vôtres
- Pas d'espaces avant/après le `=`
- Le numéro doit commencer par `+`
- Changez `sms.enabled=false` en `sms.enabled=true`

### Étape 4 : Vérifier votre numéro personnel

Pour recevoir des SMS de test :

1. Console Twilio → **Phone Numbers** → **Verified Caller IDs**
2. Cliquez sur **"Add a new Caller ID"**
3. Entrez votre numéro : `+216XXXXXXXX` (Tunisie)
4. Entrez le code reçu par SMS

### Étape 5 : Tester !

1. Relancez l'application
2. Vérifiez le log : `✓ Service SMS Twilio initialisé avec succès`
3. Postulez à une offre avec votre numéro vérifié
4. Recevez votre SMS ! 📱

---

## 🆘 Si la console ne charge toujours pas

### Option 1 : Vider le cache
- Ctrl+Shift+Delete → Effacer cookies et cache
- Réessayer

### Option 2 : Autre navigateur
- Essayez Chrome, Firefox, ou mode navigation privée

### Option 3 : Vérifier le statut
- https://status.twilio.com/
- Si incident en cours, attendez qu'il soit résolu

### Option 4 : Support Twilio
- https://support.twilio.com/
- Demandez vos credentials par email

---

## ✅ Checklist

- [ ] Console Twilio accessible
- [ ] Account SID récupéré
- [ ] Auth Token récupéré (après avoir cliqué "Show")
- [ ] Numéro Twilio obtenu (Get a trial number)
- [ ] config.properties modifié avec les 3 valeurs
- [ ] sms.enabled changé de false à true
- [ ] Mon numéro personnel vérifié dans Twilio
- [ ] Application relancée
- [ ] Log "Service SMS initialisé" visible
- [ ] Test d'envoi SMS réussi ✅

---

## 📱 Pour l'instant

Votre application fonctionne **sans SMS** (`sms.enabled=false`).

Les candidats peuvent :
- ✅ Postuler normalement
- ✅ Tout fonctionne sauf l'envoi de SMS
- ℹ️ Le champ téléphone est optionnel de toute façon

**Une fois que vous aurez complété la configuration Twilio, les SMS seront automatiquement activés !**

---

**Supprimez ce fichier après avoir complété la configuration ✨**

