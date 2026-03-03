package services.getude;

import entities.getude.Cours;

import java.sql.SQLException;
import java.util.*;

public class SystemeRecommandationService {

    private CoursService coursService;
    private List<Cours> catalogueCours;

    public SystemeRecommandationService() {
        this.coursService = new CoursService();
        this.catalogueCours = new ArrayList<>();

        // ✅ UNIQUEMENT charger depuis la base de données
        try {
            this.catalogueCours = coursService.getAll();
            System.out.println("✅ " + catalogueCours.size() + " cours chargés depuis la BD pour recommandations");
        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement cours: " + e.getMessage());
            // Ne pas créer de cours en dur, juste un message d'erreur
            this.catalogueCours = new ArrayList<>(); // Liste vide
        }
    }

    // ✅ MÉTHODE : Extraire les compétences clés des cours suivis
    private Set<String> extraireCompetencesCles(List<Integer> idsCoursSuivis) {
        Set<String> competencesCles = new HashSet<>();

        for (Integer id : idsCoursSuivis) {
            for (Cours cours : catalogueCours) {
                if (cours.getId() == id && cours.getCompetences_visees() != null) {
                    String[] comps = cours.getCompetences_visees().split(",");
                    for (String comp : comps) {
                        competencesCles.add(comp.trim().toLowerCase());
                    }
                    break;
                }
            }
        }

        System.out.println("🔍 Compétences clés du candidat: " + competencesCles);
        return competencesCles;
    }

    // ✅ MÉTHODE : Vérifier si un cours est pertinent
    private boolean estCoursPertinent(Cours cours, Set<String> competencesCles, List<Integer> idsCoursSuivis) {
        if (idsCoursSuivis.contains(cours.getId())) return false;
        if (cours.getCompetences_visees() == null) return false;

        String[] competencesCours = cours.getCompetences_visees().toLowerCase().split(",");

        for (String compCours : competencesCours) {
            compCours = compCours.trim();
            for (String compCle : competencesCles) {
                // ✅ Vérifier si les compétences se recoupent
                if (compCours.contains(compCle) || compCle.contains(compCours)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ✅ MÉTHODE : Calculer le score de pertinence
    private int calculerScorePertinence(Cours cours, Set<String> competencesCles, String niveauCandidat) {
        int score = 0;

        if (cours.getCompetences_visees() == null) return 0;

        String[] competencesCours = cours.getCompetences_visees().toLowerCase().split(",");

        // Points par compétence correspondante
        for (String compCours : competencesCours) {
            compCours = compCours.trim();
            for (String compCle : competencesCles) {
                if (compCours.contains(compCle)) {
                    score += 10; // Correspondance exacte
                } else if (compCle.contains(compCours)) {
                    score += 5;  // Correspondance partielle
                }
            }
        }

        // Bonus si le niveau correspond
        if (cours.getNiveau() != null && cours.getNiveau().equalsIgnoreCase(niveauCandidat)) {
            score += 3;
        }

        return score;
    }

    // ✅ MÉTHODE PRINCIPALE - 100% dynamique
    public List<Cours> getRecommandationsPourCandidat(int candidatId, String niveauCandidat, List<Integer> idsCoursSuivis) {
        List<Cours> toutesRecommandations = new ArrayList<>();

        System.out.println("\n🎯 GÉNÉRATION RECOMMANDATIONS POUR CANDIDAT " + candidatId);
        System.out.println("   Cours suivis: " + idsCoursSuivis.size() + " cours");

        // Afficher les titres des cours suivis
        for (Integer id : idsCoursSuivis) {
            for (Cours c : catalogueCours) {
                if (c.getId() == id) {
                    System.out.println("   - " + c.getTitre() + " (" + c.getCompetences_visees() + ")");
                    break;
                }
            }
        }

        // Si aucun cours suivi, recommander les cours populaires/débutants
        if (idsCoursSuivis.isEmpty()) {
            System.out.println("   ℹ️ Aucun cours suivi - recommandation par défaut");
            List<Cours> defaut = new ArrayList<>();
            for (Cours c : catalogueCours) {
                if ("Débutant".equalsIgnoreCase(c.getNiveau()) && defaut.size() < 5) {
                    defaut.add(c);
                }
            }
            return defaut;
        }

        // Extraire les compétences clés des cours suivis
        Set<String> competencesCles = extraireCompetencesCles(idsCoursSuivis);

        // Si pas de compétences identifiées, retourner liste vide
        if (competencesCles.isEmpty()) {
            System.out.println("   ⚠️ Aucune compétence identifiée");
            return new ArrayList<>();
        }

        // Calculer le score pour chaque cours du catalogue
        Map<Cours, Integer> scores = new HashMap<>();
        for (Cours cours : catalogueCours) {
            if (!idsCoursSuivis.contains(cours.getId())) {
                int score = calculerScorePertinence(cours, competencesCles, niveauCandidat);
                if (score > 0) {
                    scores.put(cours, score);
                }
            }
        }

        // Trier par score décroissant
        List<Map.Entry<Cours, Integer>> listeTrie = new ArrayList<>(scores.entrySet());
        listeTrie.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        // Prendre les 8 meilleurs
        for (int i = 0; i < Math.min(8, listeTrie.size()); i++) {
            toutesRecommandations.add(listeTrie.get(i).getKey());
        }

        System.out.println("\n✅ RECOMMANDATIONS FINALES (" + toutesRecommandations.size() + "):");
        for (Cours c : toutesRecommandations) {
            System.out.println("   - " + c.getTitre() + " (score: " + scores.get(c) + ")");
        }
        System.out.println("");

        return toutesRecommandations;
    }

    public List<Cours> getCatalogueCours() {
        return catalogueCours;
    }
}