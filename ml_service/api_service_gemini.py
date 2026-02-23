#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Service API Flask pour générer des offres d'emploi avec Google Gemini
GRATUIT - Pas d'entraînement nécessaire - Génération instantanée
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import os
import time
import requests
import json

# ============================
# 🔧 CONFIGURATION GEMINI
# ============================

# Clé API Gemini (GRATUIT) - À obtenir sur : https://makersuite.google.com/app/apikey
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")

# URL de l'API Gemini REST (v1beta avec Gemini 2.5 Flash)
GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

if GEMINI_API_KEY:
    print("✅ Gemini configuré avec succès")
else:
    print("⚠️  Gemini non configuré")

def improve_title(titre_actuel, secteur, type_contrat, salaire):
    """Améliore un titre d'offre d'emploi pour le rendre plus attractif"""

    if not GEMINI_API_KEY:
        return {
            "success": False,
            "error": "Gemini API non configurée. Veuillez définir GEMINI_API_KEY"
        }

    # Créer le prompt optimisé pour améliorer le titre
    prompt = f"""Tu es un expert en recrutement et en rédaction d'annonces attractives.

Améliore ce titre d'offre d'emploi pour le rendre plus accrocheur et professionnel :

**Titre actuel :** {titre_actuel}
**Secteur :** {secteur}
**Type de contrat :** {type_contrat}
**Salaire :** {salaire} DT (si indiqué)

**Instructions :**
1. Le titre doit être accrocheur, professionnel et optimisé pour attirer les candidats
2. Longueur maximale : 80 caractères
3. Inclure des éléments clés si pertinents : niveau d'expérience, technologie principale, type de contrat
4. Éviter les superlatifs excessifs ("meilleur", "extraordinaire")
5. Rester factuel et professionnel
6. Ne PAS inclure le salaire dans le titre (c'est redondant)

**Format attendu :**
Génère UNIQUEMENT le nouveau titre amélioré sur une seule ligne, sans explication, sans guillemets, sans introduction.

Exemples de transformations réussies :
- "Développeur Java" → "Développeur Java Confirmé - Microservices & Cloud"
- "Designer" → "Designer UI/UX - Produits Digitaux Innovants"
- "Commercial" → "Commercial B2B - Solutions Technologiques"

Génère maintenant le titre amélioré :"""

    try:
        # Préparer la requête pour l'API REST Gemini
        headers = {
            "Content-Type": "application/json"
        }

        data = {
            "contents": [{
                "parts": [{
                    "text": prompt
                }]
            }]
        }

        # Ajouter la clé API dans l'URL
        url = f"{GEMINI_API_URL}?key={GEMINI_API_KEY}"

        # Faire la requête
        response = requests.post(url, headers=headers, json=data, timeout=30)

        if response.status_code == 200:
            result = response.json()

            # Extraire le texte généré
            if "candidates" in result and len(result["candidates"]) > 0:
                candidate = result["candidates"][0]
                if "content" in candidate and "parts" in candidate["content"]:
                    text = candidate["content"]["parts"][0].get("text", "")
                    if text:
                        # Nettoyer le titre (enlever guillemets, espaces, etc.)
                        titre_ameliore = text.strip().strip('"').strip("'").strip()
                        return {
                            "success": True,
                            "description": titre_ameliore
                        }

            return {
                "success": False,
                "error": "Aucune réponse valide de Gemini"
            }
        else:
            error_msg = f"Erreur HTTP {response.status_code}"
            try:
                error_detail = response.json()
                error_msg = f"{error_msg}: {error_detail}"
            except:
                pass

            return {
                "success": False,
                "error": error_msg
            }

    except requests.exceptions.Timeout:
        return {
            "success": False,
            "error": "Timeout : La requête a pris trop de temps"
        }
    except Exception as e:
        return {
            "success": False,
            "error": f"Erreur : {str(e)}"
        }

# ============================
# 🌐 FLASK ENDPOINTS
# ============================

app = Flask(__name__)
CORS(app)

def generate_job_description_gemini(titre, secteur, competences, niveau_etudes, experience):
    """Génère une description d'offre avec Gemini via API REST"""

    if not GEMINI_API_KEY:
        return {
            "success": False,
            "error": "Gemini API non configurée. Veuillez définir GEMINI_API_KEY"
        }

    # Créer le prompt optimisé
    prompt = f"""Tu es un expert en recrutement RH. Génère une description d'offre d'emploi professionnelle et attractive au format suivant :

**Informations de l'offre :**
- Titre du poste : {titre}
- Secteur : {secteur}
- Compétences requises : {competences}
- Niveau d'études : {niveau_etudes}
- Expérience requise : {experience}

**Format attendu (IMPORTANT - Respecte exactement ce format) :**

[Paragraphe d'introduction engageant de 2-3 phrases]

🎯 Missions principales :
• [Mission 1]
• [Mission 2]
• [Mission 3]
• [Mission 4]
• [Mission 5]

💻 Environnement technique :
[Détails sur les technologies, outils et méthodologies utilisés]

👤 Profil recherché :
• Formation : {niveau_etudes}
• Expérience : {experience}
• Compétences techniques : [Compétences détaillées]
• Qualités personnelles : [Soft skills]

**Instructions importantes :**
1. Utilise les émojis indiqués (🎯 💻 👤)
2. Sois professionnel mais engageant
3. Adapte le ton au secteur ({secteur})
4. Mentionne toutes les compétences : {competences}
5. Évite le jargon excessif
6. Reste concis et clair (400-600 mots)

Génère uniquement la description, sans titre ni commentaire supplémentaire."""

    try:
        # Préparer la requête pour l'API REST Gemini
        headers = {
            "Content-Type": "application/json"
        }

        data = {
            "contents": [{
                "parts": [{
                    "text": prompt
                }]
            }]
        }

        # Ajouter la clé API dans l'URL
        url = f"{GEMINI_API_URL}?key={GEMINI_API_KEY}"

        # Faire la requête
        response = requests.post(url, headers=headers, json=data, timeout=30)

        if response.status_code == 200:
            result = response.json()

            # Extraire le texte généré
            if "candidates" in result and len(result["candidates"]) > 0:
                candidate = result["candidates"][0]
                if "content" in candidate and "parts" in candidate["content"]:
                    text = candidate["content"]["parts"][0].get("text", "")
                    if text:
                        return {
                            "success": True,
                            "description": text.strip()
                        }

            return {
                "success": False,
                "error": "Aucune réponse valide de Gemini"
            }
        else:
            error_msg = f"Erreur HTTP {response.status_code}"
            try:
                error_detail = response.json()
                error_msg = f"{error_msg}: {error_detail}"
            except:
                pass

            return {
                "success": False,
                "error": error_msg
            }

    except requests.exceptions.Timeout:
        return {
            "success": False,
            "error": "Timeout : La requête a pris trop de temps"
        }
    except Exception as e:
        return {
            "success": False,
            "error": f"Erreur : {str(e)}"
        }

@app.route('/health', methods=['GET'])
def health():
    """Endpoint de santé"""
    return jsonify({
        "status": "running",
        "service": "Générateur d'Offres IA",
        "model": "Google Gemini 2.5 Flash",
        "gemini_configured": bool(GEMINI_API_KEY)
    })

@app.route('/generate', methods=['POST'])
def generate():
    """Génère une description d'offre d'emploi ou améliore un titre"""

    try:
        # Récupérer les données JSON
        data = request.get_json()

        # Vérifier le mode (description ou amélioration de titre)
        mode = data.get('mode', 'description')

        if mode == 'improve_title':
            # Mode amélioration de titre
            titre_actuel = data.get('titre', '')
            secteur = data.get('secteur', 'Non spécifié')
            type_contrat = data.get('typeContrat', 'Non spécifié')
            salaire = data.get('salaire', 'Non spécifié')

            if not titre_actuel:
                return jsonify({
                    "success": False,
                    "error": "Le titre actuel est requis"
                }), 400

            print(f"\n🔧 Amélioration du titre : {titre_actuel}")
            print(f"   Secteur : {secteur}, Contrat : {type_contrat}, Salaire : {salaire}")

            # Générer le titre amélioré
            result = improve_title(titre_actuel, secteur, type_contrat, salaire)

            if result["success"]:
                print(f"✅ Titre amélioré : {result['description']}")
                return jsonify({
                    "success": True,
                    "description": result["description"],
                    "model": "gemini-2.5-flash"
                })
            else:
                print(f"❌ Erreur : {result['error']}")
                return jsonify(result), 500

        else:
            # Mode génération de description (existant)
            # Validation des champs requis
            required_fields = ['titre', 'secteur', 'competences']
            for field in required_fields:
                if not data.get(field):
                    return jsonify({
                        "success": False,
                        "error": f"Le champ '{field}' est requis"
                    }), 400

            titre = data['titre']
            secteur = data['secteur']
            competences = data['competences']
            niveau_etudes = data.get('niveauEtudes', 'Bac+3/5')
            experience = data.get('experience', '2-3 ans')

            print(f"\n📝 Génération pour : {titre} | {secteur}")
            print(f"   Compétences : {competences}")
            print(f"   Niveau : {niveau_etudes}, Expérience : {experience}")

            # Générer avec Gemini
            result = generate_job_description_gemini(
                titre, secteur, competences, niveau_etudes, experience
            )

            if result["success"]:
                print("✅ Génération réussie")
                return jsonify({
                    "success": True,
                    "description": result["description"],
                    "model": "gemini-2.5-flash"
                })
            else:
                print(f"❌ Erreur : {result['error']}")
                return jsonify(result), 500

    except Exception as e:
        print(f"❌ Erreur serveur : {str(e)}")
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500

@app.route('/test', methods=['GET'])
def test():
    """Endpoint de test"""

    # Test avec un exemple
    result = generate_job_description_gemini(
        titre="Développeur Java",
        secteur="Informatique",
        competences="Java, Spring Boot, MySQL",
        niveau_etudes="Bac+5",
        experience="3+ ans"
    )

    return jsonify(result)

if __name__ == '__main__':
    print("\n" + "=" * 80)
    print("🚀 SERVICE IA - GÉNÉRATEUR D'OFFRES AVEC GEMINI")
    print("=" * 80)

    if not GEMINI_API_KEY:
        print("\n⚠️  ATTENTION : GEMINI_API_KEY non définie")
        print("\n📝 Pour obtenir votre clé API GRATUITE :")
        print("   1. Aller sur : https://makersuite.google.com/app/apikey")
        print("   2. Se connecter avec un compte Google")
        print("   3. Cliquer sur 'Create API Key'")
        print("   4. Copier la clé (format : AIza...)")
        print("\n🔧 Configuration :")
        print("   Windows PowerShell:")
        print("      $env:GEMINI_API_KEY = 'votre_cle_ici'")
        print("\n")
        exit(1)

    print("\n✅ Configuration OK")
    print(f"🔑 API Key : {GEMINI_API_KEY[:10]}...{GEMINI_API_KEY[-4:]}")
    print(f"🤖 Modèle : Gemini 2.5 Flash (Dernier modèle stable)")
    print(f"🌐 URL : http://localhost:5000")
    print("\n📋 Endpoints disponibles :")
    print("   GET  /health    - Vérifier l'état du service")
    print("   POST /generate  - Générer une description d'offre")
    print("   GET  /test      - Tester avec un exemple")
    print("\n" + "=" * 80)
    print("🔥 Service prêt ! Appuyez sur Ctrl+C pour arrêter")
    print("=" * 80 + "\n")

    app.run(host='0.0.0.0', port=5000, debug=False)


