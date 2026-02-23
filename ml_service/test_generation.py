#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script de test du modèle entraîné
"""

import os
import time
from transformers import GPT2LMHeadModel, GPT2Tokenizer

print("=" * 80)
print("🧪 TEST DU MODÈLE ENTRAÎNÉ")
print("=" * 80)

# Vérifier que le modèle existe
MODEL_DIR = "./model_offres_final"
if not os.path.exists(MODEL_DIR):
    print(f"\n❌ Modèle non trouvé dans {MODEL_DIR}")
    print("💡 Veuillez d'abord entraîner le modèle: python train_model.py")
    exit(1)

# Charger le modèle
print("\n📥 Chargement du modèle entraîné...")
tokenizer = GPT2Tokenizer.from_pretrained(MODEL_DIR)
model = GPT2LMHeadModel.from_pretrained(MODEL_DIR)
print("✓ Modèle chargé")

def generate_description(titre, secteur, competences):
    """Génère une description d'offre"""

    # Créer le prompt - Format exact du training
    input_text = f"{titre} | {secteur} | {competences}"
    prompt = f"input,output\n\"{input_text}\",\""

    # Encoder
    input_ids = tokenizer.encode(prompt, return_tensors='pt')

    # Générer avec des paramètres améliorés
    start_time = time.time()
    output = model.generate(
        input_ids,
        max_length=800,  # Plus long pour des descriptions complètes
        min_length=200,  # Minimum pour éviter les textes trop courts
        num_return_sequences=1,
        no_repeat_ngram_size=3,  # Éviter les répétitions
        temperature=0.7,  # Moins de randomness
        top_k=50,
        top_p=0.92,
        do_sample=True,
        pad_token_id=tokenizer.eos_token_id,
        early_stopping=True
    )
    generation_time = time.time() - start_time

    # Décoder
    generated_text = tokenizer.decode(output[0], skip_special_tokens=True)

    # Nettoyer et extraire la description
    # Retirer le prompt et garder seulement la sortie
    description = generated_text.replace(prompt, "").strip()

    # Si la description commence par une guillemet, la retirer
    if description.startswith('"'):
        description = description[1:]
    if description.endswith('"'):
        description = description[:-1]

    return description, generation_time

# Mode interactif
print("\n" + "=" * 80)
print("🎯 MODE TEST INTERACTIF")
print("=" * 80)
print("\n💡 Entrez les informations de l'offre pour générer une description")
print("   (Tapez 'quit' pour quitter)\n")

# Exemples prédéfinis
exemples = [
    {
        "titre": "Développeur Python Senior",
        "secteur": "Informatique",
        "competences": "Python, Django, PostgreSQL, Docker"
    },
    {
        "titre": "Designer UI/UX",
        "secteur": "Design",
        "competences": "Figma, Adobe XD, Prototyping"
    },
    {
        "titre": "Data Scientist",
        "secteur": "Data Science",
        "competences": "Python, Machine Learning, TensorFlow"
    }
]

print("📝 Exemples disponibles:")
for i, ex in enumerate(exemples, 1):
    print(f"   {i}. {ex['titre']} | {ex['secteur']}")

print("\n" + "-" * 80 + "\n")

while True:
    # Demander l'input
    choix = input("Choisissez un exemple (1-3) ou tapez 'custom' pour un test personnalisé (ou 'quit'): ").strip()

    if choix.lower() == 'quit':
        print("\n👋 Au revoir!")
        break

    if choix in ['1', '2', '3']:
        ex = exemples[int(choix) - 1]
        titre = ex['titre']
        secteur = ex['secteur']
        competences = ex['competences']
    elif choix.lower() == 'custom':
        titre = input("\n📌 Titre du poste: ").strip()
        if not titre:
            print("❌ Titre obligatoire!")
            continue
        secteur = input("📌 Secteur: ").strip() or "Informatique"
        competences = input("📌 Compétences (séparées par des virgules): ").strip()
    else:
        print("❌ Choix invalide!")
        continue

    # Générer
    print(f"\n🔮 Génération en cours pour: {titre}...")
    print("-" * 80)

    description, gen_time = generate_description(titre, secteur, competences)

    print("\n✨ DESCRIPTION GÉNÉRÉE :\n")
    print(description)
    print("\n" + "-" * 80)
    print(f"⏱️  Temps de génération: {gen_time:.2f} secondes")
    print("-" * 80 + "\n")

    # Demander si on continue
    continuer = input("Générer une autre offre ? (o/n): ").strip().lower()
    if continuer != 'o':
        print("\n👋 Au revoir!")
        break

print("\n✅ Test terminé")
print("\n🚀 Prochaine étape: Lancer l'API")
print("   Commande: python api_service.py")

