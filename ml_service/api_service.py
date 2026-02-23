#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
API Flask pour le service de génération d'offres
Permet à l'application Java de communiquer avec le modèle ML
"""

import os
import time
import json
from flask import Flask, request, jsonify
from flask_cors import CORS
from transformers import GPT2LMHeadModel, GPT2Tokenizer

app = Flask(__name__)
CORS(app)  # Autoriser les requêtes depuis Java

# Configuration
MODEL_DIR = "./model_offres_final"
model = None
tokenizer = None

print("=" * 80)
print("🚀 API FLASK - SERVICE DE GÉNÉRATION D'OFFRES IA")
print("=" * 80)

# Charger le modèle au démarrage
print("\n📥 Chargement du modèle entraîné...")
try:
    if not os.path.exists(MODEL_DIR):
        print(f"❌ Modèle non trouvé dans {MODEL_DIR}")
        print("💡 Veuillez d'abord entraîner le modèle: python train_model.py")
        exit(1)

    tokenizer = GPT2Tokenizer.from_pretrained(MODEL_DIR)
    model = GPT2LMHeadModel.from_pretrained(MODEL_DIR)
    print("✓ Modèle chargé avec succès")
    print(f"   Paramètres: {model.num_parameters():,}")

except Exception as e:
    print(f"❌ Erreur lors du chargement: {e}")
    exit(1)

def generate_description(titre, secteur, competences):
    """Génère une description d'offre"""
    try:
        # Créer le prompt
        input_text = f"Input: {titre} | {secteur} | {competences}\nDescription:"

        # Encoder
        input_ids = tokenizer.encode(input_text, return_tensors='pt')

        # Générer
        start_time = time.time()
        output = model.generate(
            input_ids,
            max_length=500,
            num_return_sequences=1,
            no_repeat_ngram_size=2,
            temperature=0.8,
            top_k=50,
            top_p=0.95,
            do_sample=True,
            pad_token_id=tokenizer.eos_token_id
        )
        generation_time = time.time() - start_time

        # Décoder
        generated_text = tokenizer.decode(output[0], skip_special_tokens=True)

        # Extraire la description
        if "Description:" in generated_text:
            description = generated_text.split("Description:", 1)[1].strip()
        else:
            description = generated_text

        return description, generation_time

    except Exception as e:
        print(f"❌ Erreur génération: {e}")
        return None, 0

@app.route('/health', methods=['GET'])
def health():
    """Endpoint de santé pour vérifier que l'API fonctionne"""
    return jsonify({
        'status': 'ok',
        'model_loaded': model is not None,
        'service': 'Générateur d\'offres IA',
        'version': '1.0.0'
    })

@app.route('/generate', methods=['POST'])
def generate():
    """Endpoint principal pour générer une description"""
    try:
        # Récupérer les données JSON
        data = request.get_json()

        if not data:
            return jsonify({
                'success': False,
                'error': 'Aucune donnée fournie'
            }), 400

        # Extraire les paramètres
        titre = data.get('titre', '')
        secteur = data.get('secteur', 'Informatique')
        competences = data.get('competences', '')

        if not titre:
            return jsonify({
                'success': False,
                'error': 'Le titre est obligatoire'
            }), 400

        print(f"\n🔮 Génération pour: {titre} | {secteur}")

        # Générer la description
        description, gen_time = generate_description(titre, secteur, competences)

        if description is None:
            return jsonify({
                'success': False,
                'error': 'Erreur lors de la génération'
            }), 500

        print(f"✓ Généré en {gen_time:.2f}s")

        # Retourner la réponse
        return jsonify({
            'success': True,
            'description': description,
            'generation_time': round(gen_time, 2),
            'input': {
                'titre': titre,
                'secteur': secteur,
                'competences': competences
            }
        })

    except Exception as e:
        print(f"❌ Erreur: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@app.route('/batch_generate', methods=['POST'])
def batch_generate():
    """Générer plusieurs descriptions à la fois"""
    try:
        data = request.get_json()
        offres = data.get('offres', [])

        if not offres:
            return jsonify({
                'success': False,
                'error': 'Liste d\'offres vide'
            }), 400

        results = []
        for offre in offres:
            titre = offre.get('titre', '')
            secteur = offre.get('secteur', 'Informatique')
            competences = offre.get('competences', '')

            if titre:
                description, gen_time = generate_description(titre, secteur, competences)
                results.append({
                    'titre': titre,
                    'description': description,
                    'generation_time': round(gen_time, 2)
                })

        return jsonify({
            'success': True,
            'count': len(results),
            'results': results
        })

    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

# Route pour tester depuis le navigateur
@app.route('/', methods=['GET'])
def index():
    """Page d'accueil de l'API"""
    return """
    <html>
        <head>
            <title>API Générateur d'Offres IA</title>
            <style>
                body { font-family: Arial; padding: 40px; background: #f5f5f5; }
                .container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; }
                h1 { color: #7c3aed; }
                .endpoint { background: #f9f9f9; padding: 15px; margin: 10px 0; border-radius: 5px; }
                code { background: #e0e0e0; padding: 2px 6px; border-radius: 3px; }
            </style>
        </head>
        <body>
            <div class="container">
                <h1>🤖 API Générateur d'Offres IA</h1>
                <p>Service de génération automatique de descriptions d'offres d'emploi par Intelligence Artificielle</p>

                <h2>📡 Endpoints disponibles</h2>

                <div class="endpoint">
                    <h3>GET /health</h3>
                    <p>Vérifier le statut de l'API</p>
                </div>

                <div class="endpoint">
                    <h3>POST /generate</h3>
                    <p>Générer une description d'offre</p>
                    <p><strong>Body JSON:</strong></p>
                    <pre><code>{
  "titre": "Développeur Java Senior",
  "secteur": "Informatique",
  "competences": "Java, Spring Boot, MySQL"
}</code></pre>
                </div>

                <div class="endpoint">
                    <h3>POST /batch_generate</h3>
                    <p>Générer plusieurs descriptions à la fois</p>
                </div>

                <h2>✅ Statut</h2>
                <p>✓ API opérationnelle</p>
                <p>✓ Modèle chargé</p>
            </div>
        </body>
    </html>
    """

if __name__ == '__main__':
    print("\n" + "=" * 80)
    print("🌐 API DÉMARRÉE")
    print("=" * 80)
    print(f"\n📡 Écoute sur: http://localhost:5000")
    print("\n📋 Routes disponibles:")
    print("   • GET  /              → Page d'accueil")
    print("   • GET  /health        → Vérification du statut")
    print("   • POST /generate      → Génération simple")
    print("   • POST /batch_generate → Génération multiple")
    print("\n💡 Pour arrêter: Ctrl+C")
    print("\n✅ Prêt à recevoir des requêtes depuis Java!")
    print("=" * 80 + "\n")

    # Lancer le serveur
    app.run(host='0.0.0.0', port=5000, debug=False)

