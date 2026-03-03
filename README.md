# Carrieri-integration

Plateforme desktop JavaFX pour centraliser le cycle candidat-recruteur: gestion des utilisateurs, offres d'emploi, formation, reclamations, communication et suivi des missions.

> 🚀 Une plateforme tout-en-un pour connecter talents, recruteurs et parcours de montee en competences.

Projet realise par l'equipe Carrieri:
- Mohamed Islem Athmani
- Ons Nagara
- Bilal El Eter
- Selim Ben Abdelkader
- Wassim Saadli
- Youssef Ben Said

<img width="174" height="212" alt="logo" src="https://github.com/user-attachments/assets/b610f87f-2da6-4d2f-8f79-574791eae73a" />


## Table des matieres

- [1) Contexte et objectifs](#1-contexte-et-objectifs)
- [2) Fonctionnalites principales](#2-fonctionnalites-principales)
- [3) Architecture du projet](#3-architecture-du-projet)
- [4) Stack technique](#4-stack-technique)
- [5) Structure du depot](#5-structure-du-depot)
- [6) Prerequis](#6-prerequis)
- [7) Installation et lancement](#7-installation-et-lancement)
- [8) Configuration](#8-configuration)
- [9) Modules metier en detail](#9-modules-metier-en-detail)
- [10) Troubleshooting](#10-troubleshooting)

## 1) Contexte et objectifs

`Carrieri-integration` est un projet d'integration multi-modules qui unifie des besoins RH et formation dans une seule application Java 17:

- simplifier le parcours candidat (inscription, profil, candidatures, suivi),
- outiller les recruteurs (offres, postulations, missions, entretiens),
- enrichir l'experience avec des services intelligents (recommandation, chatbot, traduction),
- centraliser la communication et la gestion des reclamations.

Le projet est organise par domaines metier (`guser`, `goffre`, `getude`, `greclam`, `grecru`, `gcommu`) pour faciliter la maintenance et l'evolution.

## 2) Fonctionnalites principales

- 👤 Authentification, gestion des profils et navigation par role.
- 💼 Gestion des offres, favoris, postulations et suivi candidat.
- 🎓 Parcours de formation: cours, modules, lecons, quiz/tests, certifications.
- 💳 Paiement et services externes (ex: Stripe) selon les modules actives.
- 🛠️ Reclamations, feedbacks, priorisation et traitement.
- 💬 Messagerie/conversations avec composants audio/video.
- 📊 Outils de reporting/statistiques selon les espaces fonctionnels.

## 3) Architecture du projet

L'architecture suit une separation en couches et en domaines:

- **Presentation (JavaFX/FXML)**
  - Controllers: `src/main/java/com/example/guser/controllers/*`
  - Vues/ressources: `src/main/resources/com/example/guser/*`
- **Metier (Services)**
  - Services par domaine: `src/services/*`
- **Modele de donnees (Entities)**
  - Entites par domaine: `src/entities/*`
- **Infrastructure transversale**
  - DB/utilitaires: `src/utils/*`
  - Securite/session: `src/security/*`, `src/session/*`
- **Module Java**
  - Declaration des modules et exports: `src/main/java/module-info.java`

Entree de l'application:

- Application JavaFX: `src/main/java/com/example/guser/HelloApplication.java`
- Lanceur: `src/main/java/com/example/guser/Launcher.java`

## 4) Stack technique

- **Langage**: Java 17
- **Build**: Maven (`pom.xml`) + wrapper (`mvnw`, `mvnw.cmd`)
- **UI**: JavaFX 17 (FXML, controls, web, media, swing)
- **BDD**: MySQL via `mysql-connector-j`
- **Tests**: JUnit 5 (configuration presente dans le `pom.xml`)
- **Bibliotheques majeures**:
  - JSON/HTTP: Jackson, Gson, OkHttp, Apache HttpClient
  - Documents: iText7, Apache POI
  - Integrations: AWS S3, Twilio, Stripe
  - Utilitaires: ZXing (QR), RichTextFX, SLF4J

## 5) Structure du depot

```text
Carrieri-integration/
|- pom.xml
|- mvnw / mvnw.cmd
|- src/
|  |- main/
|  |  |- java/
|  |  |  |- module-info.java
|  |  |  `- com/example/guser/... (app, routing, controllers)
|  |  `- resources/
|  |     `- com/example/guser/... (fxml, assets, config)
|  |- entities/ (guser, getude, goffre, greclam, grecru, gcommu)
|  |- services/ (guser, getude, goffre, greclam, grecru, gcommu)
|  |- utils/
|  |- security/
|  |- session/
|  `- config/
`- uploads/
```

## 6) Prerequis

- JDK 17 installe (JAVA_HOME recommande)
- Maven optionnel (wrapper fourni)
- Serveur MySQL disponible
- Connexion internet si utilisation des APIs externes

## 7) Installation et lancement

### Cloner le projet

```powershell
git clone <url-du-repo>
cd Carrieri-integration
```

### Lancer l'application

```powershell
.\mvnw.cmd clean javafx:run
```

### Lancer les tests

```powershell
.\mvnw.cmd test
```

Note: la configuration JUnit est presente dans le `pom.xml`. La couverture de tests depend de l'etat actuel des classes de test du depot.

## 8) Configuration

### 8.1 Base de donnees

La connexion MySQL est centralisee dans `src/utils/MyDatabase.java`.

Valeurs actuellement visibles dans le code:

- URL: `jdbc:mysql://localhost:3306/carrieri`
- USER: `root`
- PASSWORD: vide

Adaptez ces informations avant execution en environnement reel.

### 8.2 Configurations externes

Fichiers identifies:

- `src/main/resources/com/example/guser/goffre/config.properties`
- `src/main/resources/com/example/guser/getude/config.properties`

Ces fichiers pilotent notamment:

- Twilio (SMS)
- Microsoft Translator
- Groq API
- Stripe

### 8.3 Recommandation de securisation (fortement conseillee)

- Ne pas conserver des secrets reels dans Git.
- Remplacer les valeurs sensibles par des placeholders.
- Charger les secrets depuis variables d'environnement ou fichiers non versionnes.
- Faire une rotation immediate de toute cle exposee.

Exemple de variables d'environnement a standardiser:

```text
DB_URL
DB_USER
DB_PASSWORD
TWILIO_ACCOUNT_SID
TWILIO_AUTH_TOKEN
STRIPE_SECRET_KEY
GROQ_API_TOKEN
TRANSLATOR_API_KEY
```

## 9) Modules metier en detail

### GUser

- Authentification et gestion des comptes.
- Espace profil et navigation par role.
- Base de l'experience utilisateur globale.

### GOffre

- CRUD d'offres d'emploi.
- Postulation et suivi des candidats.
- Favoris, statistiques, QR code, notifications SMS (selon configuration).

### GEtude

- Gestion de cours, modules, lecons.
- Evaluation via quiz/tests et suivi de progression.
- Certification, recommandation, chatbot/traduction/paiement selon flux.

### GReclam

- Collecte des reclamations et feedbacks.
- Priorisation, visualisation, traitement administratif.

### GRecru

- Gestion de missions et rendus.
- Entretiens et tableaux statistiques associes.

### GCommu

- Conversations/messagerie.
- Composants audio/video et outils de communication temps reel.

## 10) Troubleshooting

- **Erreur de connexion MySQL**
  - Verifier URL, port, credentials et existence de la base `carrieri`.
- **Echec au lancement JavaFX**
  - Verifier Java 17 et execution depuis la racine avec `mvnw.cmd`.
- **API externe indisponible**
  - Verifier cles, quotas, region/endpoints dans les `config.properties`.
- **Probleme de ressources FXML**
  - Verifier les chemins de fichiers dans les controllers et le classpath Maven.

---

✨ README modernise: plus lisible, plus visuel, et pret pour une presentation ou une soutenance.
