#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Génère un large dataset de 500+ offres d'emploi pour l'entraînement
"""

import csv
import random

# ============================================================
# TEMPLATES D'OFFRES PAR SECTEUR
# ============================================================

SECTEURS = {
    "Informatique": {
        "postes": [
            "Développeur Backend", "Développeur Frontend", "Développeur Full Stack",
            "Ingénieur DevOps", "Architecte Solutions", "Data Engineer",
            "Développeur Mobile", "Ingénieur Cloud", "Administrateur Système",
            "Ingénieur Réseau", "Chef de Projet IT", "Testeur QA",
            "Développeur Python", "Développeur Java", "Développeur JavaScript",
            "Développeur PHP", "Développeur .NET", "Développeur Go",
            "Ingénieur Big Data", "Ingénieur Machine Learning"
        ],
        "technologies": [
            ["Python", "Django", "PostgreSQL", "Docker"],
            ["Java", "Spring Boot", "MySQL", "Kubernetes"],
            ["JavaScript", "React", "Node.js", "MongoDB"],
            ["PHP", "Symfony", "MySQL", "Redis"],
            ["C#", ".NET", "SQL Server", "Azure"],
            ["Go", "Microservices", "Docker", "Kubernetes"],
            ["TypeScript", "Angular", "NestJS", "PostgreSQL"],
            ["Ruby", "Rails", "PostgreSQL", "Heroku"],
            ["Scala", "Spark", "Kafka", "Cassandra"],
            ["Kotlin", "Android", "Firebase", "MVVM"],
            ["Python", "Flask", "MongoDB", "Redis"],
            ["Java", "Hibernate", "Oracle", "Maven"],
            ["JavaScript", "Vue.js", "Express", "PostgreSQL"],
            ["PHP", "Laravel", "PostgreSQL", "Nginx"],
            ["C#", "ASP.NET", "Azure", "CosmosDB"],
            ["Go", "Gin", "PostgreSQL", "gRPC"],
            ["TypeScript", "React", "GraphQL", "Apollo"],
            ["Python", "FastAPI", "MySQL", "Docker"],
            ["Java", "Quarkus", "PostgreSQL", "OpenShift"],
            ["JavaScript", "Next.js", "Prisma", "Vercel"],
            ["Rust", "Actix", "PostgreSQL", "Docker"],
            ["Swift", "SwiftUI", "CoreData", "CloudKit"],
            ["Kotlin", "Ktor", "PostgreSQL", "Exposed"],
            ["Python", "Tornado", "Cassandra", "Kafka"],
            ["Java", "Micronaut", "MySQL", "GraalVM"]
        ],
        "template": """Rejoignez notre équipe technique pour développer des solutions innovantes et performantes.

🎯 Missions principales :
• Concevoir et développer des applications {type_app}
• Participer aux choix architecturaux et techniques
• Collaborer avec les équipes {collab_teams}
• Optimiser les performances et la scalabilité
• Maintenir la qualité du code avec des tests automatisés
• Participer aux code reviews et au mentorat
• Assurer une veille technologique active

💻 Environnement technique :
Technologies : {tech_stack}
Méthodologie : Agile/Scrum, CI/CD
Outils : Git, Docker, Jenkins
Architecture : {architecture}

👤 Profil recherché :
• Formation : {niveau}
• Expérience : {experience}
• Compétences techniques : Maîtrise de {competences_principales}
• Connaissance des design patterns et bonnes pratiques
• Qualités : Autonomie, esprit d'équipe, curiosité technique, rigueur"""
    },

    "Data Science": {
        "postes": [
            "Data Scientist", "Data Analyst", "Data Engineer",
            "Machine Learning Engineer", "AI Engineer", "Business Intelligence Analyst",
            "Data Architect", "MLOps Engineer", "Research Scientist",
            "Analytics Manager"
        ],
        "technologies": [
            ["Python", "TensorFlow", "PyTorch", "Pandas"],
            ["R", "Scikit-learn", "SQL", "Tableau"],
            ["Python", "Spark", "Kafka", "Airflow"],
            ["Python", "MLflow", "Kubeflow", "Docker"],
            ["SQL", "Power BI", "Excel", "Python"],
            ["Scala", "Spark", "Hadoop", "Hive"],
            ["Python", "Deep Learning", "Computer Vision", "NLP"],
            ["Python", "Statistics", "Jupyter", "Git"],
            ["Python", "XGBoost", "LightGBM", "CatBoost"],
            ["R", "ggplot2", "dplyr", "Shiny"],
            ["Python", "Keras", "OpenCV", "NLTK"],
            ["Python", "SpaCy", "Hugging Face", "Transformers"],
            ["SQL", "Tableau", "Looker", "Metabase"],
            ["Python", "Dask", "Vaex", "Modin"],
            ["Python", "Ray", "Horovod", "DeepSpeed"],
            ["Julia", "Flux", "DataFrames", "Plots"],
            ["Python", "Prophet", "ARIMA", "LSTM"],
            ["Python", "Plotly", "Seaborn", "Bokeh"],
            ["Scala", "MLlib", "GraphX", "SparkSQL"],
            ["Python", "Optuna", "Hyperopt", "Ray Tune"]
        ],
        "template": """Exploitez la puissance des données pour créer de la valeur et générer des insights actionnables.

🎯 Missions principales :
• Analyser et explorer de grandes quantités de données
• Développer des modèles {type_modele}
• Créer des visualisations et dashboards interactifs
• Collaborer avec les équipes métiers pour identifier les use cases
• Mettre en production les modèles développés
• Documenter les analyses et communiquer les résultats
• Assurer la qualité et la fiabilité des données

💻 Stack Data Science :
Langages : {tech_stack}
Outils : Jupyter, Git, Docker
Cloud : {cloud_platform}
Visualisation : Tableau, Power BI, Matplotlib

👤 Profil recherché :
• Formation : {niveau} en mathématiques, statistiques ou data science
• Expérience : {experience}
• Compétences : Maîtrise de {competences_principales}
• Excellente compréhension des statistiques et algos ML
• Qualités : Esprit analytique, curiosité, rigueur scientifique, communication"""
    },

    "Design": {
        "postes": [
            "Designer UX/UI", "Product Designer", "UI Designer",
            "UX Designer", "Motion Designer", "Graphiste",
            "Designer d'Interaction", "Design System Manager", "Creative Director",
            "Brand Designer"
        ],
        "technologies": [
            ["Figma", "Adobe XD", "Sketch", "Prototyping"],
            ["Figma", "Illustrator", "Photoshop", "InVision"],
            ["After Effects", "Cinema 4D", "Premiere Pro", "Lottie"],
            ["Figma", "Principle", "Framer", "Design Thinking"],
            ["Illustrator", "Photoshop", "InDesign", "Procreate"],
            ["Figma", "Zeplin", "Abstract", "Maze"],
            ["Adobe XD", "InVision Studio", "Marvel", "Proto.io"],
            ["Sketch", "Craft", "Anima", "Avocode"],
            ["Figma", "FigJam", "Miro", "Whimsical"],
            ["After Effects", "Blender", "DaVinci Resolve", "Final Cut"],
            ["Illustrator", "CorelDRAW", "Affinity Designer", "Inkscape"],
            ["Photoshop", "GIMP", "Affinity Photo", "Lightroom"],
            ["Figma", "ProtoPie", "Flinto", "Origami Studio"],
            ["Adobe XD", "Axure RP", "Balsamiq", "Justinmind"],
            ["Sketch", "Lunacy", "Gravit Designer", "Vectr"]
        ],
        "template": """Créez des expériences utilisateur exceptionnelles et des interfaces visuellement attractives.

🎯 Missions principales :
• Concevoir des interfaces {type_interface}
• Conduire des recherches utilisateurs (interviews, tests, analytics)
• Créer des wireframes, maquettes et prototypes interactifs
• Maintenir et faire évoluer le design system
• Collaborer avec les développeurs pour l'implémentation
• Animer des ateliers de co-création
• Assurer la cohérence de l'expérience sur tous les supports

💻 Outils créatifs :
Design : {tech_stack}
Prototypage : {proto_tools}
Collaboration : Miro, FigJam, Notion
Research : Optimal Workshop, Hotjar

👤 Profil recherché :
• Formation : {niveau} en design, UX/UI
• Expérience : {experience}
• Portfolio obligatoire démontrant votre processus de design
• Maîtrise de {competences_principales}
• Qualités : Créativité, empathie, esprit d'analyse, sens du détail"""
    },

    "Marketing": {
        "postes": [
            "Responsable Marketing Digital", "Chef de Projet Marketing",
            "Traffic Manager", "SEO Manager", "Content Manager",
            "Social Media Manager", "Growth Hacker", "Product Marketing Manager",
            "Marketing Automation Specialist", "CRM Manager"
        ],
        "technologies": [
            ["Google Ads", "Facebook Ads", "Analytics", "SEMrush"],
            ["HubSpot", "Mailchimp", "Salesforce", "Google Analytics"],
            ["SEO", "SEMrush", "Ahrefs", "Google Search Console"],
            ["Hootsuite", "Buffer", "Canva", "Meta Business Suite"],
            ["WordPress", "Webflow", "Contentful", "Yoast"],
            ["Google Ads", "LinkedIn Ads", "Twitter Ads", "TikTok Ads"],
            ["HubSpot", "Pardot", "Marketo", "ActiveCampaign"],
            ["Ahrefs", "Moz", "Screaming Frog", "Ubersuggest"],
            ["Sprout Social", "Later", "Planoly", "ContentCal"],
            ["Shopify", "WooCommerce", "Magento", "PrestaShop"],
            ["Google Tag Manager", "Hotjar", "Crazy Egg", "Mixpanel"],
            ["Zapier", "Make", "IFTTT", "Integromat"],
            ["Canva Pro", "Adobe Creative Suite", "Visme", "Piktochart"],
            ["SEMrush", "SpyFu", "Similarweb", "BuzzSumo"],
            ["Mailchimp", "SendGrid", "Sendinblue", "ConvertKit"]
        ],
        "template": """Développez notre présence digitale et générez des leads qualifiés grâce à des stratégies marketing innovantes.

🎯 Missions principales :
• Définir et piloter la stratégie {type_strategie}
• Gérer les campagnes {type_campagnes}
• Analyser les performances et optimiser le ROI
• Créer du contenu engageant pour différents canaux
• Gérer le budget marketing et reporting
• Collaborer avec les équipes sales et produit
• Assurer une veille concurrentielle active

💻 Outils marketing :
Acquisition : {tech_stack}
Analytics : Google Analytics, Data Studio
Automation : {automation_tools}
Social Media : LinkedIn, Facebook, Instagram, Twitter

👤 Profil recherché :
• Formation : {niveau} en marketing digital
• Expérience : {experience}
• Maîtrise de {competences_principales}
• ROI-driven avec excellente compréhension des KPIs
• Qualités : Créativité, esprit analytique, leadership, adaptabilité"""
    },

    "Finance": {
        "postes": [
            "Comptable", "Contrôleur de Gestion", "Analyste Financier",
            "Auditeur", "Responsable Comptable", "Trésorier",
            "Contrôleur Interne", "Directeur Financier", "Credit Manager",
            "Consolideur"
        ],
        "technologies": [
            ["SAP", "Excel", "Cegid", "QuickBooks"],
            ["SAP CO", "Hyperion", "Excel VBA", "Power BI"],
            ["Bloomberg", "Excel", "SAP", "Tableau"],
            ["ACL", "IDEA", "Excel", "SAP"],
            ["Oracle", "SAP FI", "Excel", "SQL"],
            ["Sage", "EBP", "Excel", "Access"],
            ["SAP S/4HANA", "Anaplan", "Tableau", "Alteryx"],
            ["Oracle Financials", "PeopleSoft", "Excel", "Power BI"],
            ["Microsoft Dynamics", "NetSuite", "Excel", "SQL"],
            ["Workday Financials", "Adaptive Insights", "Excel", "Tableau"],
            ["BlackLine", "FloQast", "Excel", "SAP"],
            ["Xero", "FreshBooks", "Wave", "Excel"],
            ["Tagetik", "OneStream", "Board", "Jedox"],
            ["SAP BPC", "IBM Cognos", "Oracle PBCS", "Excel"],
            ["Kyriba", "Reval", "FIS", "Bloomberg"]
        ],
        "template": """Pilotez la performance financière et assurez la conformité des opérations comptables.

🎯 Missions principales :
• Gérer {type_gestion}
• Établir les reportings financiers {periodicite}
• Analyser la performance et identifier les écarts
• Participer aux clôtures comptables
• Assurer la conformité aux normes {normes}
• Collaborer avec les équipes opérationnelles
• Optimiser les processus financiers

💻 Environnement :
ERP : {tech_stack}
Outils : Excel avancé, Power BI
Normes : {normes_comptables}
Reporting : {reporting_tools}

👤 Profil recherché :
• Formation : {niveau} en finance, comptabilité ou gestion
• Expérience : {experience}
• Maîtrise de {competences_principales}
• Bonne connaissance de la réglementation financière
• Qualités : Rigueur, organisation, esprit d'analyse, discrétion"""
    },

    "Ressources Humaines": {
        "postes": [
            "Chargé de Recrutement", "Responsable RH", "Gestionnaire Paie",
            "Responsable Formation", "Business Partner RH", "Talent Acquisition Manager",
            "Chargé SIRH", "Responsable Développement RH", "Assistant RH",
            "Responsable Relations Sociales"
        ],
        "technologies": [
            ["ATS", "LinkedIn Recruiter", "Indeed", "Excel"],
            ["SAP HR", "Workday", "BambooHR", "Excel"],
            ["Silae", "PayFit", "Excel", "DADS"],
            ["Cornerstone", "360Learning", "Moodle", "PowerPoint"],
            ["SAP SuccessFactors", "Workday", "Oracle HCM", "Power BI"],
            ["Greenhouse", "Lever", "JazzHR", "Excel"],
            ["ADP", "Ceridian", "Paylocity", "Excel"],
            ["Talentsoft", "Lucca", "PeopleSoft", "Excel"],
            ["UltiPro", "iCIMS", "SmartRecruiters", "LinkedIn"],
            ["Zoho People", "Gusto", "Rippling", "Excel"],
            ["Jobvite", "Taleo", "Bullhorn", "Indeed"],
            ["Sage HRMS", "Paychex", "Zenefits", "Excel"],
            ["SuccessFactors", "Oracle Taleo", "Kronos", "Excel"],
            ["Bob", "HiBob", "Charlie HR", "Breathe HR"],
            ["TriNet", "Namely", "Bamboo HR", "Excel"]
        ],
        "template": """Accompagnez les collaborateurs et contribuez au développement du capital humain de l'entreprise.

🎯 Missions principales :
• Gérer {type_processus_rh}
• Accompagner les managers dans leurs besoins RH
• Assurer le respect de la législation sociale
• Piloter les projets {type_projets_rh}
• Analyser les indicateurs RH et proposer des actions
• Animer la communication interne
• Garantir une expérience collaborateur positive

💻 Outils RH :
SIRH : {tech_stack}
Recrutement : {recrutement_tools}
Communication : Teams, Slack, Intranet
Reporting : Excel, Power BI

👤 Profil recherché :
• Formation : {niveau} en RH, psychologie ou droit social
• Expérience : {experience}
• Compétences : {competences_principales}
• Connaissance du droit du travail et des processus RH
• Qualités : Écoute, diplomatie, confidentialité, organisation"""
    },

    "Commercial": {
        "postes": [
            "Commercial B2B", "Business Developer", "Account Manager",
            "Sales Manager", "Key Account Manager", "Ingénieur Commercial",
            "Commercial Terrain", "Inside Sales", "Sales Engineer",
            "Responsable Développement Commercial"
        ],
        "technologies": [
            ["Salesforce", "LinkedIn Sales Navigator", "HubSpot", "Excel"],
            ["CRM", "Pipedrive", "Slack", "Google Workspace"],
            ["Salesforce", "Tableau", "Excel", "PowerPoint"],
            ["SAP CRM", "Microsoft Dynamics", "Power BI", "Teams"],
            ["Zoho CRM", "Freshsales", "Nimble", "Excel"],
            ["SugarCRM", "Insightly", "Close", "Google Sheets"],
            ["Monday.com", "Copper", "Nutshell", "Excel"],
            ["Agile CRM", "Capsule", "Less Annoying CRM", "Excel"],
            ["Streak", "Base CRM", "Apptivo", "Excel"],
            ["Oracle Sales Cloud", "SAP C4C", "NetSuite CRM", "Excel"],
            ["Salesflare", "Pipeliner", "Keap", "Excel"],
            ["ActiveCampaign", "Ontraport", "Drip", "Excel"],
            ["Sendinblue", "GetResponse", "Mailchimp", "Excel"],
            ["ConvertKit", "AWeber", "Constant Contact", "Excel"],
            ["Pardot", "Marketo", "Eloqua", "Excel"]
        ],
        "template": """Développez le chiffre d'affaires et fidélisez les clients grâce à votre expertise commerciale.

🎯 Missions principales :
• Prospecter et identifier de nouveaux clients {type_cible}
• Conduire les négociations commerciales
• Gérer et développer un portefeuille de clients
• Atteindre et dépasser les objectifs de vente
• Assurer le suivi et la satisfaction client
• Collaborer avec les équipes marketing et produit
• Réaliser les reportings d'activité

💻 Outils commerciaux :
CRM : {tech_stack}
Prospection : {prospection_tools}
Communication : Teams, Zoom, LinkedIn
Reporting : Excel, Power BI

👤 Profil recherché :
• Formation : {niveau} en commerce, marketing ou gestion
• Expérience : {experience}
• Compétences : {competences_principales}
• Excellente capacité de négociation et closing
• Qualités : Persévérance, écoute active, orientation résultats, autonomie"""
    },

    "Juridique": {
        "postes": [
            "Juriste d'Entreprise", "Juriste Contrats", "Juriste Droit Social",
            "Juriste Propriété Intellectuelle", "Compliance Officer", "Paralegal",
            "Juriste Contentieux", "Legal Counsel", "Responsable Juridique",
            "Data Protection Officer"
        ],
        "technologies": [
            ["LegalTech", "Doctrine", "Lexis Nexis", "Excel"],
            ["Contract Management", "DocuSign", "Salesforce", "SharePoint"],
            ["Dalloz", "Lexbase", "Excel", "Word"],
            ["Legifrance", "EUR-Lex", "Trello", "Teams"],
            ["PandaDoc", "ContractWorks", "Concord", "Excel"],
            ["Ironclad", "Agiloft", "Conga", "SharePoint"],
            ["HelloSign", "SignNow", "Adobe Sign", "Excel"],
            ["Westlaw", "LexisNexis", "Bloomberg Law", "Excel"],
            ["Clio", "MyCase", "PracticePanther", "Excel"],
            ["Thomson Reuters", "Fastcase", "Justia", "Excel"],
            ["LawGeex", "Kira Systems", "eBrevia", "Excel"],
            ["Legal Files", "Filevine", "CASEpeer", "Excel"],
            ["ROSS Intelligence", "Lex Machina", "Ravel Law", "Excel"],
            ["Wolters Kluwer", "CCH", "ProLaw", "Excel"],
            ["AbacusLaw", "Smokeball", "Rocket Matter", "Excel"]
        ],
        "template": """Assurez la sécurité juridique de l'entreprise et conseillez les opérationnels sur les questions légales.

🎯 Missions principales :
• Rédiger et négocier les contrats {type_contrats}
• Conseiller les équipes sur les problématiques juridiques
• Assurer une veille juridique et réglementaire
• Gérer les contentieux et relations avec les avocats
• Garantir la conformité {type_conformite}
• Former les collaborateurs aux enjeux juridiques
• Participer aux projets stratégiques

💻 Outils juridiques :
Bases légales : {tech_stack}
Documentation : {doc_tools}
Collaboration : SharePoint, Teams
Gestion : {gestion_tools}

👤 Profil recherché :
• Formation : {niveau} en droit (Master 2 ou équivalent)
• Expérience : {experience}
• Spécialisation : {competences_principales}
• Excellente maîtrise de la rédaction juridique
• Qualités : Rigueur, analyse, pédagogie, confidentialité"""
    },

    "Support Client": {
        "postes": [
            "Chargé de Support Client", "Customer Success Manager", "Support Technique",
            "Responsable Service Client", "Agent Support", "Technical Support Engineer",
            "Customer Care Specialist", "Support Team Lead", "Client Onboarding Specialist",
            "Customer Experience Manager"
        ],
        "technologies": [
            ["Zendesk", "Intercom", "Freshdesk", "Slack"],
            ["Salesforce Service Cloud", "Jira", "Confluence", "Zoom"],
            ["HubSpot", "LiveChat", "Aircall", "Notion"],
            ["Zoho Desk", "Monday.com", "Teams", "Excel"],
            ["Help Scout", "Kayako", "Front", "Slack"],
            ["Freshservice", "ServiceNow", "Jira Service Desk", "Teams"],
            ["Drift", "Olark", "Tidio", "Slack"],
            ["Gorgias", "Re:amaze", "Richpanel", "Shopify"],
            ["Kustomer", "Gladly", "Dixa", "Zoom"],
            ["Groove", "Helpjuice", "Document360", "Teams"],
            ["LiveAgent", "TeamSupport", "Vision Helpdesk", "Slack"],
            ["ProProfs", "Helpshift", "UserVoice", "Intercom"],
            ["Acquire", "Comm100", "Pure Chat", "Zoom"],
            ["Chaport", "Podium", "JivoChat", "Teams"],
            ["Crisp", "Tawk.to", "Smartsupp", "Slack"]
        ],
        "template": """Assurez la satisfaction et la fidélisation des clients en leur apportant un support de qualité.

🎯 Missions principales :
• Répondre aux demandes clients {canaux_support}
• Résoudre les problèmes techniques et fonctionnels
• Assurer le suivi et l'escalade des tickets complexes
• Former les clients à l'utilisation du produit
• Collecter les feedbacks et proposer des améliorations
• Maintenir la documentation support à jour
• Garantir le respect des SLA et la satisfaction client

💻 Outils support :
Ticketing : {tech_stack}
Communication : {communication_tools}
Documentation : Confluence, Notion
Monitoring : {monitoring_tools}

👤 Profil recherché :
• Formation : {niveau}
• Expérience : {experience}
• Compétences : {competences_principales}
• Excellente communication écrite et orale
• Qualités : Patience, empathie, réactivité, résolution de problèmes"""
    },

    "Logistique": {
        "postes": [
            "Responsable Logistique", "Approvisionneur", "Supply Chain Manager",
            "Gestionnaire de Stock", "Préparateur de Commandes", "Chef d'Entrepôt",
            "Transport Manager", "Planificateur Logistique", "Coordinateur Supply Chain",
            "Responsable Expéditions"
        ],
        "technologies": [
            ["SAP MM", "WMS", "TMS", "Excel"],
            ["Oracle SCM", "Manhattan", "Excel", "Power BI"],
            ["SAP", "MRP", "Excel", "ERP"],
            ["WMS", "RF", "Excel", "Barcode Scanner"],
            ["JDA", "Blue Yonder", "Kinaxis", "Excel"],
            ["Infor", "HighJump", "Körber", "Excel"],
            ["Oracle WMS", "NetSuite", "Fishbowl", "Excel"],
            ["SAP EWM", "SAP TM", "SAP IBP", "Excel"],
            ["Manhattan SCALE", "3PL", "CargoWise", "Excel"],
            ["RedPrairie", "Logfire", "Descartes", "Excel"],
            ["Epicor", "Acumatica", "SYSPRO", "Excel"],
            ["IFS", "Sage X3", "Microsoft Dynamics", "Excel"],
            ["Warehouse Insight", "SkuVault", "Ordoro", "Excel"],
            ["ShipStation", "ShipBob", "ShipHero", "Excel"],
            ["Logiwa", "Fishbowl Inventory", "Cin7", "Excel"]
        ],
        "template": """Optimisez les flux logistiques et garantissez la disponibilité des produits.

🎯 Missions principales :
• Gérer {type_gestion_logistique}
• Optimiser les coûts et les délais de livraison
• Coordonner avec les fournisseurs et transporteurs
• Suivre les indicateurs de performance logistique
• Assurer le respect des procédures qualité et sécurité
• Gérer les stocks et anticiper les besoins
• Améliorer continuellement les processus

💻 Outils logistiques :
ERP/WMS : {tech_stack}
Planification : {planning_tools}
Suivi : Excel, Power BI
Communication : {communication_tools}

👤 Profil recherché :
• Formation : {niveau} en logistique, supply chain
• Expérience : {experience}
• Compétences : {competences_principales}
• Connaissance des réglementations transport et douanes
• Qualités : Organisation, réactivité, gestion du stress, leadership"""
    }
}

# Niveaux d'études
NIVEAUX_ETUDES = ["Bac+2", "Bac+3", "Bac+4", "Bac+5", "Doctorat", "Master 2", "Licence", "BTS", "DUT"]

# Expériences
EXPERIENCES = [
    "1-2 ans", "2-3 ans", "3-5 ans", "5+ ans", "7+ ans", "10+ ans",
    "Junior (0-2 ans)", "Confirmé (3-5 ans)", "Senior (5+ ans)", "Expert (7+ ans)"
]

# ============================================================
# FONCTIONS DE GÉNÉRATION
# ============================================================

def generate_offer(secteur, poste, technologies):
    """Génère une offre d'emploi complète"""

    config = SECTEURS[secteur]
    template = config["template"]

    # Sélectionner aléatoirement les paramètres
    niveau = random.choice(NIVEAUX_ETUDES)
    experience = random.choice(EXPERIENCES)
    tech_stack = ", ".join(technologies)
    competences_principales = ", ".join(technologies[:2])

    # Paramètres spécifiques par secteur
    if secteur == "Informatique":
        params = {
            "type_app": random.choice(["web modernes", "backend scalables", "microservices", "mobile natives", "cloud"]),
            "collab_teams": random.choice(["frontend et DevOps", "product et design", "data et backend", "QA et ops"]),
            "tech_stack": tech_stack,
            "architecture": random.choice(["Microservices", "Clean Architecture", "Hexagonale", "MVC", "Event-Driven"]),
            "niveau": niveau,
            "experience": experience,
            "competences_principales": competences_principales
        }

    elif secteur == "Data Science":
        params = {
            "type_modele": random.choice(["prédictifs", "de classification", "de recommandation", "de détection d'anomalies", "de NLP"]),
            "tech_stack": tech_stack,
            "cloud_platform": random.choice(["AWS", "GCP", "Azure"]),
            "niveau": niveau,
            "experience": experience,
            "competences_principales": competences_principales
        }

    elif secteur == "Design":
        params = {
            "type_interface": random.choice(["web et mobile", "SaaS", "e-commerce", "dashboard", "applications mobiles"]),
            "tech_stack": tech_stack,
            "proto_tools": random.choice(["Figma, Principle", "Adobe XD, InVision", "Framer, ProtoPie"]),
            "niveau": niveau,
            "experience": experience,
            "competences_principales": competences_principales
        }

    elif secteur == "Marketing":
        params = {
            "type_strategie": random.choice(["marketing digital", "acquisition", "growth", "content marketing", "branding"]),
            "type_campagnes": random.choice(["SEA (Google Ads, LinkedIn)", "SEO", "Social Media", "Display", "Email Marketing"]),
            "tech_stack": tech_stack,
            "automation_tools": random.choice(["HubSpot, Marketo", "Pardot, ActiveCampaign", "Mailchimp, Sendinblue"]),
            "niveau": niveau,
            "experience": experience,
            "competences_principales": competences_principales
        }

    elif secteur == "Finance":
        params = {
            "type_gestion": random.choice(["la comptabilité générale", "le contrôle de gestion", "la trésorerie", "les audits", "la consolidation"]),
            "periodicite": random.choice(["mensuels et annuels", "trimestriels", "hebdomadaires", "quotidiens"]),
            "normes": random.choice(["IFRS, PCG", "Sarbanes-Oxley", "GAAP", "Bâle III"]),
            "tech_stack": tech_stack,
            "normes_comptables": random.choice(["IFRS", "PCG français", "US GAAP"]),
            "reporting_tools": random.choice(["SAP BW", "Hyperion", "Excel VBA"]),
            "niveau": niveau,
            "experience": experience,
            "competences_principales": competences_principales
        }

    elif secteur == "Ressources Humaines":
        params = {
            "type_processus_rh": random.choice(["le recrutement", "la paie", "la formation", "les relations sociales", "la GPEC"]),
            "type_projets_rh": random.choice(["de formation", "de mobilité interne", "de transformation RH", "SIRH", "d'onboarding"]),
            "tech_stack": tech_stack,
            "recrutement_tools": random.choice(["LinkedIn, Indeed", "Welcome to the Jungle", "JobTeaser, Apec"]),
            "niveau": niveau,
            "experience": experience,
            "competences_principales": competences_principales
        }

    elif secteur == "Commercial":
        params = {
            "type_cible": random.choice(["B2B", "B2C", "grands comptes", "PME", "startups"]),
            "tech_stack": tech_stack,
            "prospection_tools": random.choice(["LinkedIn Sales Navigator", "Hunter.io, Lusha", "Pharow, Kaspr"]),
            "niveau": niveau,
            "experience": experience,
            "competences_principales": competences_principales
        }

    elif secteur == "Juridique":
        params = {
            "type_contrats": random.choice(["commerciaux", "de travail", "de confidentialité", "de prestation", "internationaux"]),
            "type_conformite": random.choice(["RGPD", "réglementaire", "contractuelle", "éthique"]),
            "tech_stack": tech_stack,
            "doc_tools": random.choice(["SharePoint", "Confluence", "Google Drive"]),
            "gestion_tools": random.choice(["Jira", "Trello", "Asana"]),
            "niveau": niveau,
            "experience": experience,
            "competences_principales": competences_principales
        }

    elif secteur == "Support Client":
        params = {
            "canaux_support": random.choice(["(email, chat, téléphone)", "(ticket, chat live)", "(email, visio)", "(téléphone, ticket)"]),
            "tech_stack": tech_stack,
            "communication_tools": random.choice(["Slack, Teams", "Zoom, Google Meet", "Phone, Email"]),
            "monitoring_tools": random.choice(["Datadog", "New Relic", "Grafana"]),
            "niveau": niveau,
            "experience": experience,
            "competences_principales": competences_principales
        }

    elif secteur == "Logistique":
        params = {
            "type_gestion_logistique": random.choice(["les approvisionnements", "les stocks", "les expéditions", "les transports", "la supply chain"]),
            "tech_stack": tech_stack,
            "planning_tools": random.choice(["SAP APO", "Excel", "Kinaxis"]),
            "communication_tools": random.choice(["Teams, Email", "Slack, Phone", "Walkie-Talkie, RF"]),
            "niveau": niveau,
            "experience": experience,
            "competences_principales": competences_principales
        }

    else:
        params = {
            "tech_stack": tech_stack,
            "niveau": niveau,
            "experience": experience,
            "competences_principales": competences_principales
        }

    # Générer la description
    description = template.format(**params)

    return {
        "input": f"{poste} | {secteur} | {tech_stack}",
        "output": description
    }

def generate_dataset(target_size=500):
    """Génère un dataset de taille target_size"""

    print("=" * 80)
    print(f"🚀 GÉNÉRATION D'UN DATASET DE {target_size}+ OFFRES D'EMPLOI")
    print("=" * 80)

    dataset = []

    for secteur, config in SECTEURS.items():
        print(f"\n📂 Génération pour le secteur : {secteur}")

        postes = config["postes"]
        technologies_list = config["technologies"]

        # Calculer combien d'offres par secteur
        offers_per_sector = target_size // len(SECTEURS)

        count = 0
        while count < offers_per_sector:
            # Sélectionner un poste et des technologies
            poste = random.choice(postes)
            technologies = random.choice(technologies_list)

            # Générer l'offre
            offer = generate_offer(secteur, poste, technologies)
            dataset.append(offer)
            count += 1

            if count % 10 == 0:
                print(f"   ✓ {count} offres générées...")

        print(f"   ✅ {count} offres générées pour {secteur}")

    print(f"\n{'=' * 80}")
    print(f"✅ GÉNÉRATION TERMINÉE : {len(dataset)} offres créées")
    print(f"{'=' * 80}")

    return dataset

def save_dataset(dataset, filename="training_data.csv"):
    """Sauvegarde le dataset dans un fichier CSV"""

    print(f"\n💾 Sauvegarde dans {filename}...")

    # Créer un backup
    import os
    if os.path.exists(filename):
        import shutil
        backup_name = filename + ".backup_large"
        shutil.copy(filename, backup_name)
        print(f"✓ Backup créé : {backup_name}")

    # Sauvegarder
    with open(filename, 'w', encoding='utf-8', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=['input', 'output'])
        writer.writeheader()
        writer.writerows(dataset)

    print(f"✅ Dataset sauvegardé : {len(dataset)} offres")

    # Statistiques
    print(f"\n📊 Statistiques :")
    secteurs_count = {}
    for item in dataset:
        secteur = item['input'].split('|')[1].strip()
        secteurs_count[secteur] = secteurs_count.get(secteur, 0) + 1

    for secteur, count in sorted(secteurs_count.items()):
        print(f"   • {secteur}: {count} offres")

    print(f"\n🚀 Prochaines étapes :")
    print(f"   1. python prepare_data.py")
    print(f"   2. python train_model.py")
    print(f"   3. python test_generation.py")

# ============================================================
# MAIN
# ============================================================

if __name__ == "__main__":
    # Générer 1000+ offres
    dataset = generate_dataset(target_size=1000)

    # Sauvegarder
    save_dataset(dataset, "training_data.csv")

    print(f"\n{'=' * 80}")
    print("🎉 TERMINÉ ! Votre dataset de 1000+ offres est prêt !")
    print("=" * 80)

