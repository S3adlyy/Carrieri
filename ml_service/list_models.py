#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script pour lister les modèles Gemini disponibles avec ta clé API
"""

import requests
import os
import json

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")

if not GEMINI_API_KEY:
    print("❌ GEMINI_API_KEY non définie")
    exit(1)

print("🔍 Récupération des modèles disponibles...\n")

# Appel à l'API ListModels
url = f"https://generativelanguage.googleapis.com/v1beta/models?key={GEMINI_API_KEY}"

try:
    response = requests.get(url, timeout=10)

    if response.status_code == 200:
        data = response.json()

        if "models" in data:
            print(f"✅ {len(data['models'])} modèles trouvés :\n")
            print("=" * 80)

            for model in data["models"]:
                name = model.get("name", "")
                display_name = model.get("displayName", "")
                description = model.get("description", "")
                methods = model.get("supportedGenerationMethods", [])

                # Vérifier si le modèle supporte generateContent
                if "generateContent" in methods:
                    print(f"✅ {name}")
                    print(f"   Nom affiché: {display_name}")
                    print(f"   Méthodes: {', '.join(methods)}")
                    if description:
                        print(f"   Description: {description[:100]}...")
                    print()

            print("=" * 80)
            print("\n💡 Utilise un des modèles ci-dessus dans api_service_gemini.py")
            print("   Format: models/nom-du-modele")

        else:
            print("❌ Aucun modèle trouvé")
            print(json.dumps(data, indent=2))
    else:
        print(f"❌ Erreur HTTP {response.status_code}")
        print(response.text)

except Exception as e:
    print(f"❌ Erreur: {e}")

