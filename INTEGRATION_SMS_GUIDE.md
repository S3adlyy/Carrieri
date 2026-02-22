# 📱 Intégration API SMS - Guide Complet

## ✅ Ce qui a été fait

### 1. **Dépendance Twilio ajoutée** (`pom.xml`)
```xml
<dependency>
    <groupId>com.twilio.sdk</groupId>
    <artifactId>twilio</artifactId>
    <version>9.14.1</version>
</dependency>
```

### 2. **Service SMS créé** (`SMSService.java`)
Service singleton qui gère :
- ✅ Chargement de la configuration depuis `config.properties`
- ✅ Initialisation du client Twilio
- ✅ Envoi de SMS de confirmation de candidature
- ✅ Gestion des erreurs et logs détaillés
- ✅ Mode désactivé si configuration incomplète

### 3. **Interface de postulation mise à jour** (`postuler.fxml`)
- ✅ Nouveau champ "Numéro de téléphone" (optionnel)
- ✅ Badge bleu "Pour SMS de confirmation"
- ✅ Validation du format de téléphone
- ✅ Messages d'erreur en rouge sous le champ

### 4. **Contrôleur enrichi** (`PostulerPopupController.java`)
- ✅ Validation du numéro de téléphone (format international)
- ✅ Conversion automatique du format français (0612... → +336...)
- ✅ Récupération du titre de l'offre pour personnaliser le SMS
- ✅ Envoi SMS après succès de postulation
- ✅ Messages de confirmation adaptés (avec/sans SMS)

### 5. **Fichiers de configuration**
- ✅ `config.properties` - Configuration Twilio (gitignored)
- ✅ `config.properties.example` - Template pour les développeurs
- ✅ `.gitignore` mis à jour pour protéger les credentials

### 6. **Documentation complète**
- ✅ `SMS_CONFIGURATION.md` - Guide détaillé d'installation
- ✅ Instructions pour compte Trial gratuit
- ✅ FAQ et dépannage

## 🎯 Fonctionnement

### Workflow complet

```
1. Candidat remplit le formulaire de postulation
   ├─ Lettre de motivation (requis, min 25 mots)
   └─ Numéro de téléphone (optionnel)

2. Click "Envoyer ma candidature"
   ├─ Validation des champs
   └─ Si validé ↓

3. Sauvegarde dans la base de données
   └─ INSERT dans table postulation

4. Récupération du titre de l'offre
   └─ SELECT depuis table offre_emploi

5. Si numéro fourni ET service SMS activé
   ├─ Normalisation du numéro (+33...)
   ├─ Envoi SMS via API Twilio
   └─ Message de confirmation personnalisé

6. Affichage du résultat à l'utilisateur
   ├─ ✅ "Candidature envoyée + SMS envoyé au +336..."
   ├─ ⚠️ "Candidature envoyée + SMS non envoyé"
   └─ ℹ️ "Candidature envoyée + pas de numéro"
```

## 📝 Exemple de SMS envoyé

```
Bonjour Candidat,

Votre candidature pour l'offre 'Développeur Full Stack' a bien été envoyée ✓

Nous reviendrons vers vous prochainement.
Bonne chance!

- Équipe Goffres
```

## 🔧 Configuration Twilio (Résumé)

### Étapes rapides :

1. **Créer compte** : https://www.twilio.com/try-twilio
2. **Obtenir credentials** : https://console.twilio.com/
3. **Obtenir numéro** : Console → Phone Numbers → Get a number
4. **Configurer l'app** : Éditer `config.properties`

### config.properties

```properties
twilio.account.sid=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
twilio.auth.token=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
twilio.phone.number=+1234567890
sms.enabled=true
```

## 🧪 Tests

### Test 1 : Service activé avec bon numéro

**Entrée candidat :**
- Téléphone : `+33612345678` (vérifié dans Twilio)
- Motivation : "Je suis passionné par le développement..." (25+ mots)

**Résultat attendu :**
- ✅ Postulation sauvegardée en DB
- ✅ SMS envoyé avec succès
- ✅ Message : "Candidature envoyée ! ✓ SMS envoyé au +33612345678"
- ✅ Log console : `✓ SMS envoyé avec succès - SID: SMxxx`

### Test 2 : Service activé sans numéro

**Entrée candidat :**
- Téléphone : (vide)
- Motivation : "Je suis passionné..." (25+ mots)

**Résultat attendu :**
- ✅ Postulation sauvegardée en DB
- ℹ️ Aucun SMS envoyé
- ℹ️ Message : "Candidature envoyée ! ℹ Aucun numéro fourni"

### Test 3 : Service désactivé

**config.properties :**
```properties
sms.enabled=false
```

**Résultat attendu :**
- ✅ Postulation sauvegardée en DB
- ℹ️ Message : "Candidature envoyée ! ℹ Service SMS non configuré"
- ℹ️ Log console : `ℹ Service SMS désactivé`

### Test 4 : Numéro invalide

**Entrée candidat :**
- Téléphone : `123` (trop court)

**Résultat attendu :**
- ❌ Validation échoue
- ❌ Message rouge : "⚠ Format invalide. Utilisez le format international"
- ❌ Champ bordé en rouge
- ❌ Pas de sauvegarde

### Test 5 : Format français converti

**Entrée candidat :**
- Téléphone : `0612345678`

**Résultat converti automatiquement :**
- ✅ Envoyé à : `+33612345678`

## 📊 Logs et Monitoring

### Logs dans la console

```
✓ Service SMS Twilio initialisé avec succès
✓ SMS envoyé avec succès - SID: SM1234567890abcdef
```

### Dashboard Twilio

Consultez tous vos SMS : https://console.twilio.com/us1/monitor/logs/sms

Vous y verrez :
- 📊 Statut (Delivered, Failed, etc.)
- 🕐 Date/heure d'envoi
- 📱 Numéro destinataire
- 💰 Coût
- ❌ Erreurs éventuelles

## 💰 Tarification Twilio

### Mode Trial (Gratuit)
- 🎁 **$15 USD de crédit**
- 📱 **~200-300 SMS gratuits**
- ⚠️ SMS uniquement vers numéros vérifiés
- ⚠️ Préfixe "Sent from trial account"

### Mode Production (Payant)
- 💳 Carte bancaire requise
- 📱 SMS vers tous numéros
- 💰 **~0.07€ par SMS en France**
- 💰 **~0.05-0.10€ par SMS à l'international**

## 🔐 Sécurité

### ✅ Bonnes pratiques appliquées

1. **Credentials protégés**
   - `config.properties` dans `.gitignore`
   - Pas de credentials en dur dans le code

2. **Validation côté client**
   - Format de téléphone vérifié
   - Messages d'erreur clairs

3. **Gestion des erreurs**
   - Try-catch sur l'envoi SMS
   - Logs détaillés pour debug
   - Application continue même si SMS échoue

4. **Service optionnel**
   - SMS désactivable via config
   - Numéro de téléphone optionnel
   - Pas de blocage si Twilio indisponible

## 🚀 Évolutions possibles

### Futures améliorations

1. **SMS multi-langues**
   ```java
   public boolean envoyerSMS(String phone, String candidat, 
                             String offre, String langue) {
       String template = getTemplate(langue);
       // ...
   }
   ```

2. **SMS aux recruteurs**
   - Notifier le recruteur d'une nouvelle candidature
   - "Nouvelle candidature reçue pour 'Dev Full Stack'"

3. **Templates personnalisables**
   - Stocker templates en DB
   - Personnalisation par entreprise

4. **Statistiques SMS**
   - Taux de délivrance
   - Coûts par mois
   - Dashboard dans l'admin

5. **File d'attente SMS**
   - Envoi asynchrone
   - Retry en cas d'échec
   - Batch processing

6. **Autres providers**
   - Support de SMS.to, Nexmo, etc.
   - Configuration multi-providers

## 📚 Ressources

- **Twilio Java SDK** : https://www.twilio.com/docs/libraries/java
- **Twilio Console** : https://console.twilio.com/
- **Twilio Pricing** : https://www.twilio.com/pricing
- **Twilio Support** : https://support.twilio.com/

## ❓ FAQ

**Q: Puis-je tester sans créer de compte Twilio ?**
R: Oui, mettez `sms.enabled=false` dans config.properties

**Q: Combien coûte Twilio ?**
R: Compte Trial gratuit avec $15 de crédit. Production : ~0.07€/SMS

**Q: Puis-je utiliser un autre fournisseur SMS ?**
R: Oui, il suffit de modifier SMSService.java pour utiliser une autre API

**Q: Le SMS est-il obligatoire pour postuler ?**
R: Non, le numéro de téléphone est optionnel

**Q: Où voir si le SMS a été envoyé ?**
R: Dans la console IntelliJ et sur le dashboard Twilio

---

**✨ Intégration complétée avec succès !**

Testez dès maintenant en :
1. Configurant votre compte Twilio
2. Lançant l'application
3. Postulant à une offre avec votre numéro vérifié

