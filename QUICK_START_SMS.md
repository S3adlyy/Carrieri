# 🚀 Démarrage Rapide - Intégration SMS

## ⏱️ 5 minutes pour tester l'envoi de SMS

### Étape 1 : Créer un compte Twilio (2 min)

1. Allez sur **https://www.twilio.com/try-twilio**
2. Remplissez le formulaire d'inscription
3. Vérifiez votre email
4. Vérifiez votre numéro de téléphone (vous recevrez un code par SMS)

✅ Vous avez maintenant **$15 USD de crédit gratuit** !

---

### Étape 2 : Obtenir vos credentials (1 min)

1. Allez sur **https://console.twilio.com/**
2. Sur le dashboard, notez :
   - **Account SID** : `ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`
   - **Auth Token** : Cliquez sur "Show" pour le voir

---

### Étape 3 : Obtenir un numéro Twilio (1 min)

1. Dans le menu : **Phone Numbers** → **Manage** → **Buy a number**
2. Ou cliquez sur **"Get a trial phone number"** (plus rapide)
3. Acceptez le numéro proposé (gratuit)
4. Notez votre numéro : `+1234567890`

---

### Étape 4 : Configurer l'application (30 sec)

Éditez `src/main/resources/config.properties` :

```properties
twilio.account.sid=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
twilio.auth.token=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
twilio.phone.number=+1234567890
sms.enabled=true
```

⚠️ Remplacez les `xxx` par vos vraies valeurs !

---

### Étape 5 : Vérifier votre numéro personnel (30 sec)

**Important** : En mode Trial, vous ne pouvez envoyer qu'à des numéros vérifiés.

1. Console Twilio → **Phone Numbers** → **Verified Caller IDs**
2. Cliquez sur **Add a new Caller ID**
3. Entrez votre numéro (ex: `+33612345678`)
4. Entrez le code reçu par SMS

---

### Étape 6 : Installer les dépendances Maven (30 sec)

Dans IntelliJ IDEA :
1. Clic droit sur `pom.xml`
2. **Maven** → **Reload Project**

Ou en ligne de commande :
```bash
mvn clean install -DskipTests
```

---

### Étape 7 : Lancer l'application et tester ! (30 sec)

1. **Lancez** l'application (`HelloApplication.java`)
2. Vérifiez le log dans la console :
   ```
   ✓ Service SMS Twilio initialisé avec succès
   ```

3. Allez dans **"Liste des offres"**
4. Cliquez sur **"Postuler"** sur une offre
5. Remplissez :
   - **Téléphone** : Votre numéro vérifié (ex: `+33612345678`)
   - **Motivation** : Au moins 25 mots
6. Cliquez sur **"Envoyer ma candidature"**

📱 **Vous devriez recevoir un SMS dans les 5 secondes !**

---

## 🎉 Félicitations !

Votre intégration SMS fonctionne ! 

### Vérifiez le log :
```
✓ SMS envoyé avec succès - SID: SM1234567890abcdef
```

### Vérifiez le dashboard Twilio :
https://console.twilio.com/us1/monitor/logs/sms

---

## ❌ Problèmes courants

### "Configuration Twilio non complétée"
➡️ Vous avez oublié de remplacer `YOUR_ACCOUNT_SID_HERE` dans `config.properties`

### "The number +33... is unverified"
➡️ Vérifiez votre numéro dans la console Twilio (Verified Caller IDs)

### "Communications link failure"
➡️ Votre serveur MySQL n'est pas démarré (pas lié à Twilio)

### Pas de log "Service SMS initialisé"
➡️ Le fichier `config.properties` n'existe pas ou est mal placé

---

## 🔧 Commandes utiles

### Désactiver temporairement les SMS
```properties
sms.enabled=false
```

### Tester sans numéro
Laissez le champ téléphone vide lors de la postulation

### Voir les logs Twilio
```
Console → Monitor → Logs → Messaging
```

---

## 📚 Documentation complète

- **Guide détaillé** : Voir `SMS_CONFIGURATION.md`
- **Guide technique** : Voir `INTEGRATION_SMS_GUIDE.md`

---

## 💡 Astuce

Pour tester sans limite de numéros vérifiés :
1. Ajoutez une carte bancaire dans Twilio
2. Votre compte passe en mode Production
3. Vous pouvez envoyer à n'importe quel numéro !
4. **Coût** : ~0.07€ par SMS en France

---

**✨ Prêt à envoyer des SMS ! 🚀**

