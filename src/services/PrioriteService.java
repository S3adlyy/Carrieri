package services;

import entities.Reclamation;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrioriteService {

    // Seuils de priorité
    private static final int SEUIL_URGENT = 30;
    private static final int SEUIL_HAUTE = 20;
    private static final int SEUIL_MOYENNE = 10;

    // Coefficients par catégorie
    private static final Map<String, Integer> COEFF_CATEGORIE = new HashMap<>();
    static {
        COEFF_CATEGORIE.put("Technique", 5);
        COEFF_CATEGORIE.put("Facturation", 8);
        COEFF_CATEGORIE.put("Service", 3);
        COEFF_CATEGORIE.put("Autre", 1);
    }

    // Mots-clés d'urgence
    private static final Map<String, Integer> MOTS_CLES = new HashMap<>();
    static {
        MOTS_CLES.put("urgence", 15);
        MOTS_CLES.put("critique", 15);
        MOTS_CLES.put("paiement", 10);
        MOTS_CLES.put("compte bloqué", 10);
        MOTS_CLES.put("bug", 5);
        MOTS_CLES.put("erreur", 5);
        MOTS_CLES.put("problème", 5);
        MOTS_CLES.put("panne", 10);
        MOTS_CLES.put("hack", 20);
        MOTS_CLES.put("piratage", 20);
    }

    /**
     * Calcule la priorité d'une réclamation
     * @param r La réclamation à analyser
     * @return String avec emoji et niveau de priorité
     */
    public String calculerPriorite(Reclamation r) {
        int score = 0;

        // 1. Priorité par email (type d'utilisateur)
        score += calculerScoreParEmail(r.getEmail());

        // 2. Mots-clés dans la description
        score += calculerScoreParMotsCles(r.getDescription());

        // 3. Âge de la réclamation (CORRIGÉ)
        score += calculerScoreParAge(r.getDateCreation());

        // 4. Catégorie
        score += COEFF_CATEGORIE.getOrDefault(r.getCategorie(), 0);

        // 5. Priorité actuelle (si déjà définie)
        score += calculerScoreParPrioriteActuelle(r.getPriorite());

        // Déterminer le niveau final
        return formaterPriorite(score);
    }

    /**
     * Calcule le score basé sur l'email
     */
    private int calculerScoreParEmail(String email) {
        if (email == null || email.isEmpty()) return 0;

        email = email.toLowerCase();
        if (email.contains("@vip") || email.contains("@directeur")) {
            return 20; // VIP
        }
        if (email.contains("@entreprise") || email.contains("@company")) {
            return 10; // Entreprise
        }
        if (email.contains("@gmail") || email.contains("@yahoo")) {
            return 2; // Particulier
        }
        return 5; // Autre
    }

    /**
     * Calcule le score basé sur les mots-clés dans la description
     */
    private int calculerScoreParMotsCles(String description) {
        if (description == null || description.isEmpty()) return 0;

        String desc = description.toLowerCase();
        int score = 0;

        for (Map.Entry<String, Integer> entry : MOTS_CLES.entrySet()) {
            if (desc.contains(entry.getKey())) {
                score += entry.getValue();
                System.out.println("🔍 Mot-clé trouvé: " + entry.getKey() + " (+" + entry.getValue() + ")");
            }
        }

        return score;
    }

    /**
     * Calcule le score basé sur l'âge de la réclamation (CORRIGÉ)
     */
    private int calculerScoreParAge(Date dateCreation) {
        if (dateCreation == null) return 0;

        try {
            // Convertir java.util.Date en LocalDate correctement
            LocalDate creation = dateCreation.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            long jours = ChronoUnit.DAYS.between(creation, LocalDate.now());

            // +2 points par jour, plafonné à 20
            return (int) Math.min(jours * 2, 20);

        } catch (Exception e) {
            // En cas d'erreur, essayer une autre méthode
            try {
                // Fallback: utiliser getTime()
                long diff = System.currentTimeMillis() - dateCreation.getTime();
                long jours = diff / (1000 * 60 * 60 * 24);
                return (int) Math.min(jours * 2, 20);
            } catch (Exception ex) {
                System.err.println("⚠️ Erreur calcul âge: " + ex.getMessage());
                return 0;
            }
        }
    }

    /**
     * Calcule le score basé sur la priorité actuelle
     */
    private int calculerScoreParPrioriteActuelle(String priorite) {
        if (priorite == null) return 0;

        if (priorite.contains("URGENT")) return 10;
        if (priorite.contains("HAUTE")) return 5;
        if (priorite.contains("MOYENNE")) return 2;
        if (priorite.contains("BASSE")) return 0;

        // Ancien format
        switch(priorite) {
            case "Haute": return 5;
            case "Moyenne": return 2;
            case "Basse": return 0;
            default: return 0;
        }
    }

    /**
     * Formate le score en libellé de priorité avec emoji
     */
    private String formaterPriorite(int score) {
        if (score >= SEUIL_URGENT) {
            return "🔴 URGENT (" + score + ")";
        } else if (score >= SEUIL_HAUTE) {
            return "🟠 HAUTE (" + score + ")";
        } else if (score >= SEUIL_MOYENNE) {
            return "🟡 MOYENNE (" + score + ")";
        } else {
            return "🟢 BASSE (" + score + ")";
        }
    }

    /**
     * Récupère les statistiques des priorités
     */
    public Map<String, Integer> getStatsPriorites(List<Reclamation> reclamations) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("URGENT", 0);
        stats.put("HAUTE", 0);
        stats.put("MOYENNE", 0);
        stats.put("BASSE", 0);

        for (Reclamation r : reclamations) {
            String priorite = calculerPriorite(r);
            if (priorite.contains("URGENT")) {
                stats.put("URGENT", stats.get("URGENT") + 1);
            } else if (priorite.contains("HAUTE")) {
                stats.put("HAUTE", stats.get("HAUTE") + 1);
            } else if (priorite.contains("MOYENNE")) {
                stats.put("MOYENNE", stats.get("MOYENNE") + 1);
            } else {
                stats.put("BASSE", stats.get("BASSE") + 1);
            }
        }

        return stats;
    }
}