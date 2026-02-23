#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script d'entraînement du modèle GPT-2 pour la génération d'offres
"""

import os
import json
import time
from pathlib import Path
from transformers import (
    GPT2LMHeadModel,
    GPT2Tokenizer,
    TextDataset,
    DataCollatorForLanguageModeling,
    Trainer,
    TrainingArguments
)

print("=" * 80)
print("🚀 ENTRAÎNEMENT DU MODÈLE - GÉNÉRATEUR D'OFFRES IA")
print("=" * 80)

# Configuration
MODEL_NAME = "gpt2"  # Modèle de base
OUTPUT_DIR = "./model_offres_final"
EPOCHS = 5  # Augmenté à 5 pour mieux apprendre avec le dataset enrichi
BATCH_SIZE = 4
LEARNING_RATE = 5e-5

# 1. Vérifier les données
print("\n1️⃣ Vérification des données d'entraînement...")
if not os.path.exists('train_data.txt'):
    print("❌ Fichier train_data.txt non trouvé!")
    print("💡 Veuillez d'abord exécuter: python prepare_data.py")
    exit(1)

with open('train_data.txt', 'r', encoding='utf-8') as f:
    train_lines = f.readlines()
print(f"✓ {len(train_lines)} exemples d'entraînement trouvés")

# 2. Charger le tokenizer et le modèle
print("\n2️⃣ Chargement du modèle GPT-2...")
print("   (Première fois : téléchargement ~500MB, cela peut prendre 5-10 min)")

start_time = time.time()
tokenizer = GPT2Tokenizer.from_pretrained(MODEL_NAME)
model = GPT2LMHeadModel.from_pretrained(MODEL_NAME)

# Ajouter un token de padding si nécessaire
if tokenizer.pad_token is None:
    tokenizer.pad_token = tokenizer.eos_token
    model.config.pad_token_id = model.config.eos_token_id

load_time = time.time() - start_time
print(f"✓ Modèle chargé en {load_time:.1f}s ({model.num_parameters():,} paramètres)")

# 3. Préparer les datasets
print("\n3️⃣ Préparation des datasets...")

def load_dataset(file_path, tokenizer):
    return TextDataset(
        tokenizer=tokenizer,
        file_path=file_path,
        block_size=512  # Taille max des séquences
    )

train_dataset = load_dataset('train_data.txt', tokenizer)
val_dataset = load_dataset('val_data.txt', tokenizer) if os.path.exists('val_data.txt') else None

print(f"✓ Dataset d'entraînement: {len(train_dataset)} blocs")
if val_dataset:
    print(f"✓ Dataset de validation: {len(val_dataset)} blocs")

# Data collator pour le masquage des tokens
data_collator = DataCollatorForLanguageModeling(
    tokenizer=tokenizer,
    mlm=False  # GPT-2 est un modèle causal (pas de masked LM)
)

# 4. Configuration de l'entraînement
print("\n4️⃣ Configuration de l'entraînement...")
training_args = TrainingArguments(
    output_dir=OUTPUT_DIR,
    overwrite_output_dir=True,
    num_train_epochs=EPOCHS,
    per_device_train_batch_size=BATCH_SIZE,
    per_device_eval_batch_size=BATCH_SIZE,
    evaluation_strategy="epoch" if val_dataset else "no",
    save_strategy="epoch",
    save_total_limit=2,
    learning_rate=LEARNING_RATE,
    weight_decay=0.01,
    logging_dir='./logs',
    logging_steps=10,
    warmup_steps=100,
    load_best_model_at_end=True if val_dataset else False,
    report_to="none"  # Désactiver wandb
)

print(f"   • Epochs: {EPOCHS}")
print(f"   • Batch size: {BATCH_SIZE}")
print(f"   • Learning rate: {LEARNING_RATE}")
print(f"   • Sauvegarde: {OUTPUT_DIR}")

# 5. Créer le Trainer
print("\n5️⃣ Initialisation du Trainer...")
trainer = Trainer(
    model=model,
    args=training_args,
    data_collator=data_collator,
    train_dataset=train_dataset,
    eval_dataset=val_dataset
)

print("✓ Trainer initialisé")

# 6. Lancer l'entraînement
print("\n" + "=" * 80)
print("🔥 DÉBUT DE L'ENTRAÎNEMENT")
print("=" * 80)
print("\n⏰ Temps estimé: 30 minutes - 2 heures selon votre PC")
print("💡 La loss devrait diminuer progressivement (3.0 → 1.0 ≈ bon)")
print("\n" + "-" * 80 + "\n")

training_start = time.time()

try:
    train_result = trainer.train()

    training_time = time.time() - training_start

    print("\n" + "-" * 80)
    print("\n✅ ENTRAÎNEMENT TERMINÉ !")
    print(f"⏱️  Temps total: {training_time/60:.1f} minutes")

    # Afficher les métriques
    print("\n📊 Métriques finales:")
    print(f"   • Loss finale: {train_result.training_loss:.4f}")

    if val_dataset:
        eval_results = trainer.evaluate()
        print(f"   • Loss validation: {eval_results['eval_loss']:.4f}")
        print(f"   • Perplexity: {eval_results.get('eval_perplexity', 'N/A')}")

except KeyboardInterrupt:
    print("\n\n⚠️  Entraînement interrompu par l'utilisateur")
    print("💾 Sauvegarde du modèle actuel...")

# 7. Sauvegarder le modèle final
print("\n6️⃣ Sauvegarde du modèle entraîné...")
model.save_pretrained(OUTPUT_DIR)
tokenizer.save_pretrained(OUTPUT_DIR)
print(f"✓ Modèle sauvegardé dans {OUTPUT_DIR}/")

# 8. Sauvegarder les métriques d'entraînement
metrics = {
    'training_time_minutes': round(training_time / 60, 2),
    'final_loss': float(train_result.training_loss),
    'epochs': EPOCHS,
    'batch_size': BATCH_SIZE,
    'learning_rate': LEARNING_RATE,
    'model_parameters': model.num_parameters()
}

with open(f'{OUTPUT_DIR}/training_metrics.json', 'w') as f:
    json.dump(metrics, f, indent=2)
print("✓ Métriques sauvegardées")

# 9. Résumé final
print("\n" + "=" * 80)
print("🎉 ENTRAÎNEMENT COMPLÉTÉ AVEC SUCCÈS !")
print("=" * 80)
print(f"\n📁 Modèle entraîné disponible dans: {OUTPUT_DIR}/")
print(f"📊 Loss finale: {train_result.training_loss:.4f}")
print(f"⏱️  Temps total: {training_time/60:.1f} minutes")

print("\n🚀 Prochaines étapes:")
print("   1. Tester le modèle: python test_generation.py")
print("   2. Lancer l'API: python api_service.py")
print("   3. Intégrer dans votre application Java")

print("\n💡 Note: Plus votre loss est basse, meilleur est le modèle!")
print("   • Loss < 1.5 = Excellent")
print("   • Loss 1.5-2.5 = Très bon")
print("   • Loss > 2.5 = Acceptable (besoin de plus de données)")

