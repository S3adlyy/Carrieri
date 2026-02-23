"""
Script pour enrichir le dataset avec plus d'exemples variés
"""
import csv
import random

# Nouveaux exemples d'offres d'emploi variées
new_offers = [
    {
        "input": "Développeur Backend Python | Informatique | Python, Django, PostgreSQL, Docker",
        "output": """Rejoignez notre équipe backend pour développer des applications web performantes et scalables.

🎯 Missions principales :
• Concevoir et développer des APIs REST avec Django
• Optimiser les performances des bases de données PostgreSQL
• Implémenter des tests unitaires et d'intégration
• Collaborer avec les équipes frontend et DevOps
• Participer aux code reviews et à l'amélioration continue

💻 Stack technique :
Backend : Python, Django, Django REST Framework
Base de données : PostgreSQL, Redis
DevOps : Docker, GitLab CI/CD
Outils : PyCharm, Git, Postman

👤 Profil recherché :
• Formation : Bac+3/5 en informatique
• Expérience : 3+ ans en développement Python
• Maîtrise de Django et PostgreSQL
• Connaissance des architectures REST
• Qualités : Rigueur, esprit d'équipe, autonomie"""
    },
    {
        "input": "Ingénieur Cloud AWS | Infrastructure | AWS, Terraform, Kubernetes, Python",
        "output": """Nous recherchons un Ingénieur Cloud AWS pour concevoir et maintenir notre infrastructure cloud.

🎯 Responsabilités :
• Concevoir l'architecture cloud sur AWS (EC2, S3, RDS, Lambda)
• Automatiser l'infrastructure avec Terraform
• Déployer et gérer des clusters Kubernetes
• Optimiser les coûts cloud et la performance
• Assurer la sécurité et la conformité
• Mettre en place le monitoring avec CloudWatch

💻 Technologies :
Cloud : AWS (EC2, S3, RDS, Lambda, CloudFront)
IaC : Terraform, CloudFormation
Orchestration : Kubernetes, ECS
Scripting : Python, Bash
Monitoring : CloudWatch, Grafana, Prometheus

👤 Profil recherché :
• Formation : Bac+5 en informatique
• Expérience : 4+ ans sur AWS
• Certifications : AWS Solutions Architect (appréciée)
• Maîtrise de Terraform et Kubernetes
• Qualités : Proactivité, curiosité technique, sens de l'optimisation"""
    },
    {
        "input": "Développeur Frontend Vue.js | Informatique | Vue.js, JavaScript, CSS, HTML",
        "output": """Créez des interfaces web modernes et intuitives avec Vue.js au sein de notre équipe frontend.

🎯 Missions :
• Développer des interfaces utilisateur avec Vue.js 3
• Intégrer les maquettes designers en HTML/CSS responsive
• Optimiser les performances frontend (lazy loading, code splitting)
• Collaborer avec l'équipe backend pour l'intégration API
• Participer aux revues de code et au mentorat
• Maintenir la documentation technique

💻 Stack technique :
Framework : Vue.js 3, Vuex, Vue Router
Langages : JavaScript ES6+, TypeScript, HTML5, CSS3
Outils : Vite, Webpack, npm, Git
UI : Tailwind CSS, Bootstrap, Material Design

👤 Profil recherché :
• Formation : Bac+3 en développement web
• Expérience : 2-3 ans avec Vue.js
• Bonne maîtrise de JavaScript et CSS
• Sensibilité UX/UI et accessibilité
• Qualités : Créativité, sens du détail, esprit collaboratif"""
    },
    {
        "input": "Administrateur Base de Données | Infrastructure | MySQL, PostgreSQL, MongoDB, Performance",
        "output": """Gérez et optimisez nos bases de données pour garantir performance et disponibilité.

🎯 Responsabilités :
• Administrer les bases de données MySQL, PostgreSQL, MongoDB
• Optimiser les performances et les requêtes SQL
• Mettre en place les stratégies de backup et restauration
• Assurer la haute disponibilité (réplication, clustering)
• Gérer la sécurité des données et les accès
• Monitorer et résoudre les incidents de performance

💻 Technologies :
SGBD : MySQL, PostgreSQL, MongoDB, Redis
Outils : pgAdmin, MySQL Workbench, Mongo Compass
Monitoring : Prometheus, Grafana, Zabbix
Backup : Barman, pg_dump, mysqldump
Scripting : SQL, Python, Bash

👤 Profil recherché :
• Formation : Bac+3/5 en informatique
• Expérience : 4+ ans en administration BDD
• Certifications MySQL/PostgreSQL appréciées
• Expertise en tuning et optimisation SQL
• Qualités : Rigueur, réactivité, esprit d'analyse"""
    },
    {
        "input": "Développeur Mobile React Native | Informatique | React Native, JavaScript, iOS, Android",
        "output": """Développez notre application mobile cross-platform avec React Native.

🎯 Missions principales :
• Développer des fonctionnalités mobile avec React Native
• Créer des interfaces utilisateur natives et performantes
• Intégrer des APIs REST et GraphQL
• Optimiser les performances mobile
• Publier sur App Store et Play Store
• Maintenir le code et corriger les bugs

💻 Stack technique :
Framework : React Native, Expo
Langages : JavaScript, TypeScript
State Management : Redux, Context API
Navigation : React Navigation
Backend : REST API, GraphQL
Tools : Xcode, Android Studio, VS Code

👤 Profil recherché :
• Formation : Bac+3/5 en informatique
• Expérience : 2+ ans en React Native
• Applications publiées sur les stores
• Connaissance iOS et Android
• Qualités : Autonomie, passion mobile, esprit pratique"""
    },
    {
        "input": "Ingénieur Sécurité Informatique | Sécurité | Pentest, SIEM, Firewall, Audit",
        "output": """Protégez notre infrastructure contre les cybermenaces en tant qu'Ingénieur Sécurité.

🎯 Responsabilités :
• Réaliser des tests d'intrusion et audits de sécurité
• Analyser les événements de sécurité avec le SIEM
• Détecter et répondre aux incidents de sécurité
• Configurer et maintenir les firewalls et IDS/IPS
• Sensibiliser les équipes aux bonnes pratiques
• Assurer la conformité aux normes (ISO 27001, RGPD)

💻 Environnement :
SIEM : Splunk, QRadar, ELK Stack
Pentest : Metasploit, Burp Suite, Nmap, Wireshark
Sécurité : Firewall Fortinet, Palo Alto, IDS Snort
Normes : ISO 27001, OWASP, NIST, RGPD
Tools : Kali Linux, Python scripting

👤 Profil recherché :
• Formation : Bac+5 en cybersécurité
• Expérience : 3+ ans en sécurité informatique
• Certifications : CEH, OSCP, CISSP (appréciées)
• Connaissance approfondie des techniques d'attaque
• Qualités : Curiosité technique, vigilance, esprit d'analyse"""
    },
    {
        "input": "Data Scientist | Data Science | Python, Machine Learning, TensorFlow, SQL",
        "output": """Exploitez la puissance des données pour créer des modèles prédictifs et générer des insights.

🎯 Missions :
• Analyser et explorer de grandes quantités de données
• Développer des modèles de Machine Learning prédictifs
• Créer des algorithmes de recommandation et classification
• Visualiser les résultats et communiquer les insights
• Collaborer avec les équipes métiers et tech
• Mettre en production les modèles développés

💻 Stack Data Science :
Langages : Python, R, SQL
ML Frameworks : TensorFlow, PyTorch, Scikit-learn
Data Processing : Pandas, NumPy, Spark
Visualisation : Matplotlib, Plotly, Tableau
Notebooks : Jupyter, Google Colab
MLOps : MLflow, Docker

👤 Profil recherché :
• Formation : Bac+5 en mathématiques, statistiques ou IA
• Expérience : 2-3 ans en Data Science
• Maîtrise Python et des algos ML
• Excellente compréhension des statistiques
• Qualités : Curiosité scientifique, rigueur, communication"""
    },
    {
        "input": "Analyste Fonctionnel | Analyse | Business Analysis, BPMN, UML, Agile",
        "output": """Traduisez les besoins métiers en spécifications techniques en tant qu'Analyste Fonctionnel.

🎯 Responsabilités :
• Recueillir et analyser les besoins des utilisateurs
• Rédiger les spécifications fonctionnelles détaillées
• Modéliser les processus métiers avec BPMN
• Créer des user stories et critères d'acceptation
• Animer des ateliers avec les équipes métiers
• Assurer le lien entre le métier et les développeurs
• Participer aux recettes fonctionnelles

💻 Outils :
Modélisation : UML, BPMN, Merise
Gestion : Jira, Confluence, Azure DevOps
Documentation : MS Office, Visio, Lucidchart
Méthodologies : Agile Scrum, Cycle en V

👤 Profil recherché :
• Formation : Bac+5 en informatique ou gestion
• Expérience : 3+ ans en analyse fonctionnelle
• Certification Business Analyst appréciée
• Excellente communication écrite et orale
• Qualités : Écoute, analyse, médiation"""
    },
    {
        "input": "Développeur PHP Symfony | Informatique | PHP, Symfony, MySQL, JavaScript",
        "output": """Développez des applications web robustes avec le framework Symfony.

🎯 Missions principales :
• Développer des applications web avec Symfony 6
• Créer des APIs REST et services web
• Optimiser les performances et les requêtes SQL
• Intégrer les templates Twig et assets frontend
• Implémenter des tests unitaires avec PHPUnit
• Participer aux revues de code
• Maintenir et faire évoluer l'existant

💻 Stack technique :
Backend : PHP 8+, Symfony 6, Doctrine ORM
Frontend : JavaScript, jQuery, Bootstrap
Base de données : MySQL, PostgreSQL
Tools : Composer, Git, Docker
Méthodologie : Agile, TDD

👤 Profil recherché :
• Formation : Bac+3/5 en développement web
• Expérience : 3+ ans en PHP Symfony
• Maîtrise de Symfony et Doctrine
• Connaissance des design patterns
• Qualités : Rigueur, autonomie, esprit d'équipe"""
    },
    {
        "input": "Chef de Projet IT Agile | Management | Scrum, Jira, Gestion de projet",
        "output": """Pilotez des projets IT en méthodologie Agile pour livrer de la valeur en continu.

🎯 Missions :
• Gérer le portefeuille de projets IT de A à Z
• Animer les équipes en mode Agile Scrum
• Définir la roadmap produit avec les stakeholders
• Gérer les budgets, délais et ressources
• Assurer le reporting auprès de la direction
• Gérer les risques et les dépendances
• Faciliter la communication entre les équipes

💻 Outils et méthodes :
Méthodologies : Scrum, Kanban, SAFe
Outils : Jira, Confluence, MS Project
Collaboration : Teams, Slack, Zoom
Budget : Excel, SAP, Clarity

👤 Profil recherché :
• Formation : Bac+5 école d'ingénieur ou commerce
• Expérience : 5+ ans en gestion de projets IT
• Certifications : PMP, PSM, SAFe (appréciées)
• Leadership et excellente communication
• Qualités : Organisation, gestion du stress, adaptabilité"""
    }
]

def enrich_training_data():
    """Ajoute les nouveaux exemples au fichier de training"""

    print("=" * 80)
    print("📊 ENRICHISSEMENT DU DATASET")
    print("=" * 80)

    # 1. Lire les données existantes
    print("\n1️⃣ Lecture des données existantes...")
    existing_data = []
    try:
        with open('training_data.csv', 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            existing_data = list(reader)
        print(f"✓ {len(existing_data)} offres existantes")
    except Exception as e:
        print(f"❌ Erreur lecture: {e}")
        return

    # 2. Ajouter les nouvelles offres
    print(f"\n2️⃣ Ajout de {len(new_offers)} nouvelles offres...")
    all_data = existing_data + new_offers

    # 3. Supprimer les doublons (basé sur l'input)
    print("\n3️⃣ Suppression des doublons...")
    seen = set()
    unique_data = []
    duplicates = 0

    for item in all_data:
        input_key = item['input'].strip().lower()
        if input_key not in seen:
            seen.add(input_key)
            unique_data.append(item)
        else:
            duplicates += 1

    print(f"✓ {duplicates} doublons supprimés")
    print(f"✓ {len(unique_data)} offres uniques au total")

    # 4. Créer un backup
    print("\n4️⃣ Création du backup...")
    try:
        import shutil
        shutil.copy('training_data.csv', 'training_data.csv.backup2')
        print("✓ Backup créé: training_data.csv.backup2")
    except Exception as e:
        print(f"⚠️  Backup échoué: {e}")

    # 5. Sauvegarder les données enrichies
    print("\n5️⃣ Sauvegarde des données enrichies...")
    try:
        with open('training_data.csv', 'w', encoding='utf-8', newline='') as f:
            writer = csv.DictWriter(f, fieldnames=['input', 'output'])
            writer.writeheader()
            writer.writerows(unique_data)
        print("✓ Fichier training_data.csv mis à jour")
    except Exception as e:
        print(f"❌ Erreur sauvegarde: {e}")
        return

    # 6. Résumé
    print("\n" + "=" * 80)
    print("✅ ENRICHISSEMENT TERMINÉ !")
    print("=" * 80)
    print(f"\n📊 Statistiques finales:")
    print(f"   • Offres avant: {len(existing_data)}")
    print(f"   • Nouvelles offres: {len(new_offers)}")
    print(f"   • Doublons supprimés: {duplicates}")
    print(f"   • Total final: {len(unique_data)}")

    print(f"\n🚀 Prochaines étapes:")
    print(f"   1. python prepare_data.py")
    print(f"   2. python train_model.py")
    print(f"   3. python test_generation.py")

if __name__ == "__main__":
    enrich_training_data()

