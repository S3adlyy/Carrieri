#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script de vérification de l'installation
Exécutez ce script pour vérifier que tout est bien installé
"""

import sys
import struct

print("=" * 80)
print("🔍 VÉRIFICATION DE L'INSTALLATION")
print("=" * 80)

# 1. Version Python
print("\n1️⃣ Version Python")
print(f"   Python {sys.version}")
bits = struct.calcsize("P") * 8
print(f"   Architecture: {bits}-bit")

if bits == 32:
    print("   ❌ ERREUR: Python 32-bit détecté !")
    print("   ⚠️  Veuillez installer Python 64-bit")
    sys.exit(1)
else:
    print("   ✅ Python 64-bit OK")

# 2. Vérifier les modules
print("\n2️⃣ Vérification des modules...")

modules_requis = [
    ("torch", "PyTorch"),
    ("transformers", "Transformers (Hugging Face)"),
    ("pandas", "Pandas"),
    ("numpy", "NumPy"),
    ("flask", "Flask"),
    ("sklearn", "Scikit-learn"),
]

tous_ok = True

for module, nom in modules_requis:
    try:
        mod = __import__(module)
        version = getattr(mod, "__version__", "?")
        print(f"   ✅ {nom:30s} v{version}")
    except ImportError:
        print(f"   ❌ {nom:30s} NON INSTALLÉ")
        tous_ok = False

# 3. Test PyTorch
if tous_ok:
    print("\n3️⃣ Test PyTorch...")
    try:
        import torch

        # Créer un tensor
        x = torch.tensor([1.0, 2.0, 3.0])
        print(f"   ✅ Création de tensors : OK")

        # Vérifier CPU
        print(f"   ✅ Device disponible : {torch.device('cpu')}")

    except Exception as e:
        print(f"   ❌ Erreur PyTorch : {e}")
        tous_ok = False

# 4. Test Transformers
if tous_ok:
    print("\n4️⃣ Test Transformers...")
    try:
        from transformers import GPT2Tokenizer

        # Tester le tokenizer
        tokenizer = GPT2Tokenizer.from_pretrained("gpt2")
        tokens = tokenizer.encode("Test")
        print(f"   ✅ Tokenization : OK")

    except Exception as e:
        print(f"   ❌ Erreur Transformers : {e}")
        tous_ok = False

# 5. Résultat final
print("\n" + "=" * 80)
if tous_ok:
    print("✅ INSTALLATION COMPLÈTE ET FONCTIONNELLE !")
    print("=" * 80)
    print("\n🚀 Vous pouvez maintenant :")
    print("   1. Exécuter : python prepare_data.py")
    print("   2. Exécuter : python train_model.py")
    print("   3. Lancer l'entraînement du modèle")
    print("\n💡 Consultez APRES_INSTALLATION.md pour la suite")
else:
    print("❌ INSTALLATION INCOMPLÈTE")
    print("=" * 80)
    print("\n📝 Modules manquants détectés")
    print("\n🔧 Exécutez ces commandes :")
    print("   pip install torch --index-url https://download.pytorch.org/whl/cpu")
    print("   pip install -r requirements.txt")
    print("\n💡 Consultez APRES_INSTALLATION.md pour plus de détails")

print()

