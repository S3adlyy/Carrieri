#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script de préparation des données pour l'entraînement du modèle
"""

import pandas as pd
import json
from pathlib import Path

print("=" * 60)
print("📊 PRÉPARATION DES DONNÉES POUR L'ENTRAÎNEMENT")
print("=" * 60)

# 1. Charger les données
print("\n1️⃣ Chargement des données...")
try:
    df = pd.read_csv('training_data.csv')
    print(f"✓ {len(df)} offres chargées avec succès")
except FileNotFoundError:
    print("❌ Fichier training_data.csv non trouvé!")
    print("💡 Veuillez créer ce fichier avec vos données d'offres")
    exit(1)

# 2. Nettoyer les données
print("\n2️⃣ Nettoyage des données...")
initial_count = len(df)

# Supprimer les doublons
df = df.drop_duplicates(subset=['input'])
print(f"   - Doublons supprimés: {initial_count - len(df)}")

# Supprimer les lignes avec valeurs manquantes
df = df.dropna()
print(f"   - Lignes valides: {len(df)}")

# Filtrer les descriptions trop courtes (< 100 caractères)
df = df[df['output'].str.len() >= 100]
print(f"   - Après filtrage longueur: {len(df)}")

# 3. Créer le format d'entraînement
print("\n3️⃣ Formatage pour GPT-2...")
training_texts = []
for idx, row in df.iterrows():
    # Format: "Input: [input]\nDescription: [output]<|endoftext|>"
    text = f"Input: {row['input']}\nDescription: {row['output']}<|endoftext|>"
    training_texts.append(text)

print(f"✓ {len(training_texts)} textes formatés")

# 4. Séparer en train/validation
print("\n4️⃣ Séparation train/validation (80/20)...")
from sklearn.model_selection import train_test_split

train_texts, val_texts = train_test_split(
    training_texts,
    test_size=0.2,
    random_state=42
)

print(f"   - Entraînement: {len(train_texts)} exemples")
print(f"   - Validation: {len(val_texts)} exemples")

# 5. Sauvegarder
print("\n5️⃣ Sauvegarde des données préparées...")

# Sauvegarder en format texte
with open('train_data.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(train_texts))
print("✓ train_data.txt créé")

with open('val_data.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(val_texts))
print("✓ val_data.txt créé")

# Sauvegarder les statistiques
stats = {
    'total_offres': len(df),
    'train_size': len(train_texts),
    'val_size': len(val_texts),
    'avg_input_length': df['input'].str.len().mean(),
    'avg_output_length': df['output'].str.len().mean()
}

with open('data_stats.json', 'w', encoding='utf-8') as f:
    json.dump(stats, f, indent=2, ensure_ascii=False)
print("✓ data_stats.json créé")

# 6. Résumé
print("\n" + "=" * 60)
print("✅ PRÉPARATION TERMINÉE !")
print("=" * 60)
print(f"\n📊 Statistiques:")
print(f"   • Total d'offres: {stats['total_offres']}")
print(f"   • Données d'entraînement: {stats['train_size']}")
print(f"   • Données de validation: {stats['val_size']}")
print(f"   • Longueur moyenne input: {stats['avg_input_length']:.0f} caractères")
print(f"   • Longueur moyenne output: {stats['avg_output_length']:.0f} caractères")

print("\n🚀 Prochaine étape: Exécuter train_model.py")
print("   Commande: python train_model.py")

