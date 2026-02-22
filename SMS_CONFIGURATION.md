# Configuration de l'API SMS (Twilio)

## 📱 Fonctionnalité SMS

L'application envoie automatiquement un SMS de confirmation au candidat lorsqu'il soumet une candidature.

## 🚀 Configuration Twilio

### 1. Créer un compte Twilio (GRATUIT)

1. Allez sur https://www.twilio.com/try-twilio
2. Créez un compte gratuit
3. Vérifiez votre email et numéro de téléphone

### 2. Obtenir vos credentials

Une fois connecté à votre console Twilio (https://console.twilio.com/) :

1. **Account SID** : Affiché sur votre dashboard
2. **Auth Token** : Cliquez sur "Show" pour le révéler
3. **Numéro Twilio** : Allez dans "Phone Numbers" → "Manage" → "Active numbers"
   - Si vous n'avez pas de numéro, cliquez sur "Get a number" (gratuit avec compte trial)

### 3. Configurer l'application

Éditez le fichier `src/main/resources/config.properties` :

```properties
# Remplacez ces valeurs par vos propres credentials Twilio
twilio.account.sid=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
twilio.auth.token=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
twilio.phone.number=+1234567890

# Active/désactive le service SMS
sms.enabled=true
```

**⚠️ Important :**
- Le numéro Twilio doit être au format international avec le `+` (ex: `+15551234567`)
- En mode Trial, vous ne pouvez envoyer des SMS qu'aux numéros vérifiés dans votre compte
- Pour vérifier un numéro : Console Twilio → "Phone Numbers" → "Verified Caller IDs"

### 4. Format du numéro de téléphone candidat

Les candidats doivent entrer leur numéro au format international :
- ✅ **France** : `+33612345678` ou `0612345678` (converti automatiquement)
- ✅ **Tunisie** : `+21612345678`
- ✅ **Maroc** : `+212612345678`
- ✅ **Algérie** : `+213612345678`

## 🧪 Test de l'intégration

### Mode Trial Twilio (Gratuit)

Avec un compte Trial gratuit :
- ✅ Vous recevez un crédit de **$15 USD**
- ✅ Environ **200-300 SMS gratuits** (selon destination)
- ⚠️ Vous ne pouvez envoyer qu'aux numéros vérifiés
- ⚠️ Le SMS contiendra un préfixe "Sent from your Twilio trial account"

### Test de l'application

1. Lancez l'application
2. Allez dans "Liste des offres"
3. Cliquez sur "Postuler" sur une offre
4. Remplissez le formulaire :
   - **Numéro de téléphone** : Entrez votre numéro vérifié dans Twilio
   - **Lettre de motivation** : Minimum 25 mots
5. Cliquez sur "Envoyer ma candidature"
6. Vous devriez recevoir un SMS de confirmation dans quelques secondes !

### Vérifier les logs

Dans la console IntelliJ, vous verrez :
- ✅ `✓ Service SMS Twilio initialisé avec succès` (au démarrage)
- ✅ `✓ SMS envoyé avec succès - SID: SMxxxxxxxxx` (après envoi)
- ❌ `❌ Erreur lors de l'envoi du SMS: ...` (en cas d'erreur)

## 🔧 Désactiver temporairement les SMS

Si vous voulez tester sans configurer Twilio :

```properties
sms.enabled=false
```

L'application fonctionnera normalement sans envoyer de SMS.

## 📊 Dashboard Twilio

Consultez l'historique de vos SMS :
- https://console.twilio.com/us1/monitor/logs/sms
- Vous y verrez tous les SMS envoyés, leur statut, et les éventuelles erreurs

## ❓ Dépannage

### "Service SMS désactivé"
➡️ Vérifiez que `sms.enabled=true` dans config.properties

### "Configuration Twilio non complétée"
➡️ Remplacez `YOUR_ACCOUNT_SID_HERE` par vos vraies credentials

### "Error 21608: The number +33... is unverified"
➡️ En mode Trial, vérifiez le numéro dans votre console Twilio

### "Communications link failure" (erreur DB)
➡️ Ce n'est pas lié à Twilio, vérifiez que votre serveur MySQL est démarré

## 💡 Passer en mode Production

Pour envoyer à tous les numéros (pas seulement vérifiés) :

1. Allez dans votre console Twilio
2. Cliquez sur "Upgrade" en haut
3. Ajoutez un mode de paiement
4. Votre compte est maintenant en mode Production !
5. **Tarifs** : ~0.07€ par SMS en France

## 📝 Exemple de SMS reçu

```
Bonjour Candidat,

Votre candidature pour l'offre 'Développeur Full Stack' a bien été envoyée ✓

Nous reviendrons vers vous prochainement.
Bonne chance!

- Équipe Goffres
```

---

**💼 Développé pour Goffres - Plateforme de Gestion d'Offres d'Emploi**

