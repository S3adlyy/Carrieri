# ✅ CHECKLIST - Test SMS Twilio

## 🎯 AVANT DE LANCER L'APPLICATION

### ☑️ 1. Configuration Twilio
- [x] Account SID : `AC_REDACTED`
- [x] Auth Token : `TOKEN_REDACTED`
- [x] Phone Number : `+19853364277`
- [x] sms.enabled = true

✅ **Configuration complète !**

---

### ☐ 2. Vérifier votre numéro personnel

**⚠️ OBLIGATOIRE en mode Trial !**

1. Allez sur : https://console.twilio.com/us1/develop/phone-numbers/manage/verified
2. Votre numéro +216... est-il dans la liste ?
   - ☐ OUI → Parfait ! Passez à l'étape 3
   - ☐ NON → Ajoutez-le maintenant :
     - Cliquez sur "+"
     - Entrez +216XXXXXXXX
     - Recevez le code par SMS
     - Validez

---

### ☐ 3. MySQL démarré

- ☐ XAMPP/WAMP démarré
- ☐ MySQL service running
- ☐ Port 3306 disponible

---

## 🚀 LANCEMENT DE L'APPLICATION

### Étape 1 : Lancer
- ☐ Ouvrir `HelloApplication.java` dans IntelliJ
- ☐ Cliquer sur Run ▶️
- ☐ Attendre le chargement de l'application

### Étape 2 : Vérifier le log
Dans la console IntelliJ, cherchez :
```
✓ Service SMS Twilio initialisé avec succès
```

- ☐ Log visible → **PARFAIT ! Continuez**
- ☐ Log absent → Vérifiez config.properties

---

## 📱 TEST D'ENVOI DE SMS

### Étape 1 : Navigation
- ☐ Dans l'application, cliquez sur "Liste des offres"
- ☐ Choisissez une offre
- ☐ Cliquez sur le bouton "Postuler"

### Étape 2 : Remplir le formulaire
- ☐ **Téléphone** : Entrez votre numéro vérifié `+216XXXXXXXX`
- ☐ **Motivation** : Écrivez au moins 25 mots (5 lignes environ)
- ☐ Vérifiez qu'il n'y a pas d'erreurs en rouge

### Étape 3 : Envoyer
- ☐ Cliquez sur "Envoyer ma candidature"
- ☐ Attendez le message de confirmation

### Étape 4 : Vérifications

#### Dans l'application :
- ☐ Message "Candidature envoyée ! ✓ SMS envoyé au +216..." affiché

#### Dans la console IntelliJ :
- ☐ Log "✓ SMS envoyé avec succès - SID: SMxxx" visible

#### Sur votre téléphone (5-30 secondes) :
- ☐ SMS reçu avec le texte :
```
Bonjour Candidat,
Votre candidature pour l'offre '...' a bien été envoyée ✓
...
```

#### Dans le dashboard Twilio :
- ☐ Allez sur : https://console.twilio.com/us1/monitor/logs/sms
- ☐ SMS visible avec statut "Delivered" ✓

---

## 🎉 SI TOUT EST COCHÉ

**FÉLICITATIONS ! 🎊**

Votre intégration SMS fonctionne parfaitement !

Vous pouvez maintenant :
- ✅ Tester avec d'autres offres
- ✅ Vérifier d'autres numéros (max 5 en Trial)
- ✅ Personnaliser le message SMS
- ✅ Passer en mode Production si besoin

---

## ❌ EN CAS DE PROBLÈME

### Problème : "The number is unverified"
**Solution :** Retournez à l'étape 2 et vérifiez votre numéro

### Problème : Pas de log "Service SMS initialisé"
**Solution :** Vérifiez config.properties, rechargez l'application

### Problème : "Communications link failure"
**Solution :** Démarrez MySQL

### Problème : SMS pas reçu après 1 minute
**Solutions :**
1. Vérifiez le dashboard Twilio (SMS envoyé ?)
2. Vérifiez votre réseau mobile
3. Attendez quelques minutes (parfois retardé)

---

## 📞 SUPPORT

**Documentation complète :**
- `QUICK_START_SMS.md` - Guide rapide
- `SMS_CONFIGURATION.md` - Configuration détaillée
- `INTEGRATION_SMS_GUIDE.md` - Documentation technique

**Twilio :**
- Console : https://console.twilio.com/
- Status : https://status.twilio.com/
- Support : https://support.twilio.com/

---

## 💰 CRÉDIT TWILIO

Vérifiez votre balance dans la console (en haut à droite) :
- Crédit initial : $15.00
- Coût par SMS : ~$0.02-0.05
- Total SMS possibles : ~200-300

---

**Prêt à tester ? Lancez l'application ! 🚀📱**

