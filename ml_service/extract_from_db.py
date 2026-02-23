#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script pour extraire les offres de la base de données MySQL
et les ajouter au fichier training_data.csv (en gardant les exemples existants)
"""

import csv
import sys
import os

# Ajouter le chemin pour importer MyDatabase
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'java'))

print("=" * 80)
print("📊 EXTRACTION DES OFFRES DEPUIS LA BASE DE DONNÉES")
print("=" * 80)

# Configuration de la connexion MySQL
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '',  # Mettez votre mot de passe MySQL ici si nécessaire
    'database': 'carrieri'  # Nom de votre base de données
}

print("\n1️⃣ Connexion à MySQL...")

try:
    import mysql.connector

    # Se connecter à MySQL
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor()

    print("✓ Connexion réussie à MySQL")

except ImportError:
    print("❌ Module mysql-connector-python non trouvé")
    print("💡 Installation en cours...")
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "mysql-connector-python"])
    import mysql.connector
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor()
    print("✓ Connexion réussie à MySQL")

except mysql.connector.Error as e:
    print(f"❌ Erreur de connexion MySQL : {e}")
    print("\n💡 Vérifiez que :")
    print("   1. MySQL est démarré")
    print("   2. Le nom de la base de données est correct (carrieri)")
    print("   3. Le mot de passe est correct (si nécessaire)")
    sys.exit(1)

# 2. Extraire les offres
print("\n2️⃣ Extraction des offres...")

query = """
SELECT
    titre,
    description,
    competences_requises,
    secteur_activite,
    niveau_qualification,
    experience_requise
FROM offre_emploi
WHERE description IS NOT NULL
    AND titre IS NOT NULL
    AND TRIM(description) != ''
ORDER BY id DESC
"""

cursor.execute(query)
offres = cursor.fetchall()

print(f"✓ {len(offres)} offres extraites de la base de données")

if len(offres) == 0:
    print("\n⚠️ Aucune offre trouvée dans la base de données")
    print("💡 Assurez-vous que la table 'offres_emploi' contient des données")
    cursor.close()
    conn.close()
    sys.exit(1)

# 3. Lire les exemples existants
print("\n3️⃣ Lecture des exemples existants...")

exemples_existants = []
try:
    with open('training_data.csv', 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            exemples_existants.append(row)
    print(f"✓ {len(exemples_existants)} exemples existants conservés")
except FileNotFoundError:
    print("⚠️ Fichier training_data.csv non trouvé, création d'un nouveau fichier")

# 4. Formater les offres
print("\n4️⃣ Formatage des données...")

nouvelles_offres = []
for offre in offres:
    titre, description, competences, secteur, niveau, experience = offre

    # Créer l'input (format: "Titre | Secteur | Compétences")
    input_parts = [titre]
    if secteur:
        input_parts.append(secteur)
    if competences:
        input_parts.append(competences)

    input_text = " | ".join(input_parts)

    # Enrichir la description si elle est trop courte
    output_text = description
    if len(description) < 150:
        # Ajouter des informations structurées
        enriched = description + "\n\n"

        if niveau or experience:
            enriched += "👤 Profil recherché :\n"
            if niveau:
                enriched += f"• Niveau : {niveau}\n"
            if experience:
                enriched += f"• Expérience : {experience}\n"
            enriched += "\n"

        if competences:
            enriched += "💻 Compétences requises :\n"
            comp_list = [c.strip() for c in competences.split(',')]
            for comp in comp_list[:5]:  # Limiter à 5 compétences
                enriched += f"• {comp}\n"
            enriched += "\n"

        if secteur:
            enriched += f"🏢 Secteur : {secteur}\n"

        output_text = enriched

    nouvelles_offres.append({
        'input': input_text,
        'output': output_text
    })

print(f"✓ {len(nouvelles_offres)} offres formatées et enrichies")

# 5. Combiner avec les exemples existants
print("\n5️⃣ Combinaison des données...")

toutes_les_offres = exemples_existants + nouvelles_offres

# Supprimer les doublons (basé sur l'input)
offres_uniques = {}
for offre in toutes_les_offres:
    input_key = offre['input']
    if input_key not in offres_uniques:
        offres_uniques[input_key] = offre

offres_finales = list(offres_uniques.values())

print(f"✓ Total final : {len(offres_finales)} offres uniques")
print(f"   • Exemples initiaux : {len(exemples_existants)}")
print(f"   • Nouvelles offres : {len(nouvelles_offres)}")
print(f"   • Doublons supprimés : {len(toutes_les_offres) - len(offres_finales)}")

# 6. Sauvegarder dans training_data.csv
print("\n6️⃣ Sauvegarde dans training_data.csv...")

# Backup de l'ancien fichier
if os.path.exists('training_data.csv'):
    import shutil
    shutil.copy('training_data.csv', 'training_data.csv.backup')
    print("✓ Backup créé : training_data.csv.backup")

with open('training_data.csv', 'w', encoding='utf-8', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=['input', 'output'])
    writer.writeheader()
    writer.writerows(offres_finales)

print(f"✓ Fichier training_data.csv mis à jour")

# 7. Fermer la connexion
cursor.close()
conn.close()

# 8. Résumé
print("\n" + "=" * 80)
print("✅ EXTRACTION TERMINÉE AVEC SUCCÈS !")
print("=" * 80)

print(f"\n📊 Statistiques finales :")
print(f"   • Total d'offres dans training_data.csv : {len(offres_finales)}")
print(f"   • Exemples initiaux conservés : {len(exemples_existants)}")
print(f"   • Nouvelles offres ajoutées : {len(nouvelles_offres)}")
print(f"   • Fichier de backup : training_data.csv.backup")

print("\n🚀 Prochaines étapes :")
print("   1. Vérifier training_data.csv (ouvrir avec Excel ou un éditeur)")
print("   2. Exécuter : python prepare_data.py")
print("   3. Exécuter : python train_model.py")

print("\n💡 Avec " + str(len(offres_finales)) + " offres, votre modèle sera excellent !")

