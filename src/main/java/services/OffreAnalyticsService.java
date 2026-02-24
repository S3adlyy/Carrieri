package services;

import entities.OffreEmploi;
import entities.Postulation;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service d'Analytics Avancées pour les Offres d'Emploi
 * Fournit des métriques détaillées, graphiques et recommandations
 */
public class OffreAnalyticsService {

    private final OffreEmploiService offreService;
    private final PostulationService postulationService;

    public OffreAnalyticsService() {
        this.offreService = new OffreEmploiService();
        this.postulationService = new PostulationService();
    }

    /**
     * Classe contenant toutes les statistiques avancées d'une offre
     */
    public static class OffreStatistics {
        // KPIs de base
        private int totalVues = 0;
        private int totalCandidatures = 0;
        private double tauxConversion = 0.0;
        private int scoreQualite = 0;

        // Statistiques par statut
        private int enAttente = 0;
        private int acceptees = 0;
        private int refusees = 0;

        // Évolution temporelle
        private Map<LocalDate, Integer> vuesParJour = new HashMap<>();
        private Map<Integer, Integer> vuesParHeure = new HashMap<>();
        private Map<String, Integer> candidaturesParSemaine = new HashMap<>();

        // Répartition géographique
        private Map<String, Integer> candidaturesParLocalisation = new HashMap<>();

        // Comparaison marché
        private double salaireMoyenSecteur = 0.0;
        private boolean salaireCompetitif = false;

        // Recommandations
        private List<Recommendation> recommandations = new ArrayList<>();

        // Performance
        private double tempsReponseRecrut = 0.0; // en jours

        // Tendances
        private String tendance = "stable"; // "hausse", "baisse", "stable"
        private double variationPourcentage = 0.0;

        // Getters et Setters
        public int getTotalVues() { return totalVues; }
        public void setTotalVues(int totalVues) { this.totalVues = totalVues; }

        public int getTotalCandidatures() { return totalCandidatures; }
        public void setTotalCandidatures(int totalCandidatures) { this.totalCandidatures = totalCandidatures; }

        public double getTauxConversion() { return tauxConversion; }
        public void setTauxConversion(double tauxConversion) { this.tauxConversion = tauxConversion; }

        public int getScoreQualite() { return scoreQualite; }
        public void setScoreQualite(int scoreQualite) { this.scoreQualite = scoreQualite; }

        public int getEnAttente() { return enAttente; }
        public void setEnAttente(int enAttente) { this.enAttente = enAttente; }

        public int getAcceptees() { return acceptees; }
        public void setAcceptees(int acceptees) { this.acceptees = acceptees; }

        public int getRefusees() { return refusees; }
        public void setRefusees(int refusees) { this.refusees = refusees; }

        public Map<LocalDate, Integer> getVuesParJour() { return vuesParJour; }
        public void setVuesParJour(Map<LocalDate, Integer> vuesParJour) { this.vuesParJour = vuesParJour; }

        public Map<Integer, Integer> getVuesParHeure() { return vuesParHeure; }
        public void setVuesParHeure(Map<Integer, Integer> vuesParHeure) { this.vuesParHeure = vuesParHeure; }

        public Map<String, Integer> getCandidaturesParSemaine() { return candidaturesParSemaine; }
        public void setCandidaturesParSemaine(Map<String, Integer> candidaturesParSemaine) {
            this.candidaturesParSemaine = candidaturesParSemaine;
        }

        public Map<String, Integer> getCandidaturesParLocalisation() { return candidaturesParLocalisation; }
        public void setCandidaturesParLocalisation(Map<String, Integer> candidaturesParLocalisation) {
            this.candidaturesParLocalisation = candidaturesParLocalisation;
        }

        public double getSalaireMoyenSecteur() { return salaireMoyenSecteur; }
        public void setSalaireMoyenSecteur(double salaireMoyenSecteur) {
            this.salaireMoyenSecteur = salaireMoyenSecteur;
        }

        public boolean isSalaireCompetitif() { return salaireCompetitif; }
        public void setSalaireCompetitif(boolean salaireCompetitif) {
            this.salaireCompetitif = salaireCompetitif;
        }

        public List<Recommendation> getRecommandations() { return recommandations; }
        public void setRecommandations(List<Recommendation> recommandations) {
            this.recommandations = recommandations;
        }

        public double getTempsReponseRecrut() { return tempsReponseRecrut; }
        public void setTempsReponseRecrut(double tempsReponseRecrut) {
            this.tempsReponseRecrut = tempsReponseRecrut;
        }

        public String getTendance() { return tendance; }
        public void setTendance(String tendance) { this.tendance = tendance; }

        public double getVariationPourcentage() { return variationPourcentage; }
        public void setVariationPourcentage(double variationPourcentage) {
            this.variationPourcentage = variationPourcentage;
        }
    }

    /**
     * Classe pour les recommandations
     */
    public static class Recommendation {
        private String icon;
        private String titre;
        private String description;
        private String priorite; // "haute", "moyenne", "basse"

        public Recommendation(String icon, String titre, String description) {
            this.icon = icon;
            this.titre = titre;
            this.description = description;
            this.priorite = "moyenne";
        }

        public Recommendation(String icon, String titre, String description, String priorite) {
            this.icon = icon;
            this.titre = titre;
            this.description = description;
            this.priorite = priorite;
        }

        public String getIcon() { return icon; }
        public String getTitre() { return titre; }
        public String getDescription() { return description; }
        public String getPriorite() { return priorite; }
    }

    /**
     * Calcule toutes les statistiques avancées pour une offre
     */
    public OffreStatistics getStatistics(OffreEmploi offre) throws SQLException {
        OffreStatistics stats = new OffreStatistics();

        // Récupérer les postulations
        List<Postulation> postulations = postulationService.afficherParOffre(offre.getId());

        // KPIs de base
        stats.setTotalCandidatures(postulations.size());
        stats.setTotalVues(generateFakeViews(offre)); // TODO: implémenter tracking réel
        stats.setTauxConversion(calculateConversionRate(stats.getTotalVues(), stats.getTotalCandidatures()));
        stats.setScoreQualite(calculateQualityScore(offre));

        // Statistiques par statut
        Map<String, Long> statutCounts = postulations.stream()
            .collect(Collectors.groupingBy(Postulation::getStatut, Collectors.counting()));

        stats.setEnAttente(statutCounts.getOrDefault("En attente", 0L).intValue());
        stats.setAcceptees(statutCounts.getOrDefault("Acceptée", 0L).intValue());
        stats.setRefusees(statutCounts.getOrDefault("Refusée", 0L).intValue());

        // Évolution temporelle
        stats.setVuesParJour(generateVuesParJour(offre));
        stats.setVuesParHeure(generateVuesParHeure());
        stats.setCandidaturesParSemaine(getCandidaturesParSemaine(postulations));

        // Répartition géographique (basée sur localisation des candidats)
        stats.setCandidaturesParLocalisation(getCandidaturesParLocalisation(postulations));

        // Comparaison marché
        double salaireMoyen = getMoyenneSalaireSecteur(offre.getSecteurActivite());
        stats.setSalaireMoyenSecteur(salaireMoyen);
        stats.setSalaireCompetitif(offre.getSalaire() >= salaireMoyen * 0.95);

        // Temps de réponse
        stats.setTempsReponseRecrut(calculateTempsReponse(postulations));

        // Tendance
        calculateTendance(stats, postulations);

        // Recommandations
        stats.setRecommandations(generateRecommendations(offre, stats));

        return stats;
    }

    private double calculateConversionRate(int vues, int candidatures) {
        return vues > 0 ? (candidatures * 100.0 / vues) : 0.0;
    }

    /**
     * Calcule le score de qualité de l'offre (0-100)
     */
    private int calculateQualityScore(OffreEmploi offre) {
        int score = 0;

        // Titre (20 points)
        String titre = offre.getTitre();
        if (titre != null && titre.length() >= 10 && titre.length() <= 70) {
            score += 20;
        } else if (titre != null && titre.length() >= 5) {
            score += 10;
        }

        // Description (30 points)
        String desc = offre.getDescription();
        if (desc != null) {
            int descLength = desc.length();
            if (descLength >= 300 && descLength <= 1000) {
                score += 30;
            } else if (descLength >= 200) {
                score += 20;
            } else if (descLength >= 100) {
                score += 10;
            }
        }

        // Compétences requises (15 points)
        if (offre.getCompetencesRequises() != null && !offre.getCompetencesRequises().trim().isEmpty()) {
            score += 15;
        }

        // Salaire (15 points)
        if (offre.getSalaire() > 0) {
            score += 15;
        }

        // Localisation (10 points)
        if (offre.getLocalisation() != null && !offre.getLocalisation().trim().isEmpty()) {
            score += 10;
        }

        // Contact (10 points)
        if (offre.getContactRecruteur() != null && offre.getContactRecruteur().contains("@")) {
            score += 10;
        }

        return score;
    }

    /**
     * Génère des vues simulées par jour (TODO: remplacer par vrai tracking)
     */
    private Map<LocalDate, Integer> generateVuesParJour(OffreEmploi offre) {
        Map<LocalDate, Integer> vues = new LinkedHashMap<>();

        LocalDate debut = offre.getDatePublication() != null ?
            offre.getDatePublication().toLocalDate() : LocalDate.now().minusDays(30);
        LocalDate fin = LocalDate.now();

        Random random = new Random(offre.getId());

        for (LocalDate date = debut; !date.isAfter(fin); date = date.plusDays(1)) {
            // Plus de vues en début de semaine
            int baseViews = date.getDayOfWeek() == DayOfWeek.MONDAY ? 30 : 20;
            int dailyViews = baseViews + random.nextInt(15);
            vues.put(date, dailyViews);
        }

        return vues;
    }

    /**
     * Distribution des vues par heure de la journée
     */
    private Map<Integer, Integer> generateVuesParHeure() {
        Map<Integer, Integer> vues = new LinkedHashMap<>();

        // Simulation d'un pattern réaliste
        int[] pattern = {2, 1, 1, 0, 0, 1, 3, 8, 15, 12, 10, 9, 8, 7, 9, 11, 10, 8, 6, 5, 4, 3, 3, 2};

        for (int hour = 0; hour < 24; hour++) {
            vues.put(hour, pattern[hour]);
        }

        return vues;
    }

    /**
     * Génère un nombre simulé de vues totales
     */
    private int generateFakeViews(OffreEmploi offre) {
        // Base sur nombre de candidatures * facteur
        try {
            int candidatures = postulationService.afficherParOffre(offre.getId()).size();
            return candidatures * 15 + new Random(offre.getId()).nextInt(50);
        } catch (SQLException e) {
            return 100;
        }
    }

    /**
     * Répartit les candidatures par semaine
     */
    private Map<String, Integer> getCandidaturesParSemaine(List<Postulation> postulations) {
        Map<String, Integer> parSemaine = new LinkedHashMap<>();

        Map<String, Long> grouped = postulations.stream()
            .collect(Collectors.groupingBy(
                p -> "Sem " + p.getDatePostulation().format(
                    java.time.format.DateTimeFormatter.ofPattern("ww/yyyy")
                ),
                Collectors.counting()
            ));

        grouped.forEach((k, v) -> parSemaine.put(k, v.intValue()));

        return parSemaine;
    }

    /**
     * Répartition géographique simulée des candidatures
     */
    private Map<String, Integer> getCandidaturesParLocalisation(List<Postulation> postulations) {
        Map<String, Integer> parLocalisation = new LinkedHashMap<>();

        // Simulation basée sur les villes principales de Tunisie
        String[] villes = {"Tunis", "Sfax", "Sousse", "Nabeul", "Autres"};
        double[] distribution = {0.45, 0.22, 0.18, 0.10, 0.05};

        int total = postulations.size();
        for (int i = 0; i < villes.length; i++) {
            parLocalisation.put(villes[i], (int)(total * distribution[i]));
        }

        return parLocalisation;
    }

    /**
     * Calcule la moyenne des salaires du secteur
     */
    private double getMoyenneSalaireSecteur(String secteur) {
        // Moyennes par secteur (exemple)
        Map<String, Double> moyennes = new HashMap<>();
        moyennes.put("Informatique", 2850.0);
        moyennes.put("Finance", 3200.0);
        moyennes.put("Marketing", 2500.0);
        moyennes.put("Santé", 3500.0);
        moyennes.put("Éducation", 2000.0);
        moyennes.put("Ingénierie", 3000.0);

        return moyennes.getOrDefault(secteur, 2500.0);
    }

    /**
     * Calcule le temps moyen de réponse du recruteur
     */
    private double calculateTempsReponse(List<Postulation> postulations) {
        // TODO: Implémenter avec vraies dates de réponse
        return 3.5; // jours
    }

    /**
     * Détermine la tendance (hausse, baisse, stable)
     */
    private void calculateTendance(OffreStatistics stats, List<Postulation> postulations) {
        if (postulations.size() < 2) {
            stats.setTendance("stable");
            stats.setVariationPourcentage(0.0);
            return;
        }

        // Comparer les 7 derniers jours vs les 7 précédents
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime septJoursAvant = now.minusDays(7);
        LocalDateTime quatorzeJoursAvant = now.minusDays(14);

        long derniersSeptJours = postulations.stream()
            .filter(p -> p.getDatePostulation().isAfter(septJoursAvant))
            .count();

        long septJoursPrecedents = postulations.stream()
            .filter(p -> p.getDatePostulation().isAfter(quatorzeJoursAvant) &&
                        p.getDatePostulation().isBefore(septJoursAvant))
            .count();

        if (septJoursPrecedents == 0) {
            stats.setTendance("hausse");
            stats.setVariationPourcentage(100.0);
        } else {
            double variation = ((derniersSeptJours - septJoursPrecedents) * 100.0) / septJoursPrecedents;
            stats.setVariationPourcentage(variation);

            if (variation > 10) {
                stats.setTendance("hausse");
            } else if (variation < -10) {
                stats.setTendance("baisse");
            } else {
                stats.setTendance("stable");
            }
        }
    }

    /**
     * Génère des recommandations d'optimisation
     */
    private List<Recommendation> generateRecommendations(OffreEmploi offre, OffreStatistics stats) {
        List<Recommendation> recommendations = new ArrayList<>();

        // Vérifier la longueur du titre
        if (offre.getTitre() != null && offre.getTitre().length() > 70) {
            recommendations.add(new Recommendation(
                "⚠️",
                "Titre trop long",
                "Réduisez le titre à moins de 60 caractères pour améliorer la visibilité",
                "moyenne"
            ));
        }

        // Vérifier la description
        if (offre.getDescription() != null) {
            int descLength = offre.getDescription().length();
            if (descLength < 50) {
                recommendations.add(new Recommendation(
                    "⚠️",
                    "Description trop courte",
                    "Ajoutez plus de détails (recommandé : 400-800 charactères)",
                    "haute"
                ));
            } else if (descLength > 1000) {
                recommendations.add(new Recommendation(
                    "⚠️",
                    "Description longue",
                    "Une description de 500-800 charactères est plus efficace",
                    "moyenne"
                ));
            }
        }

        // Vérifier l'expiration
        if (offre.getDateExpiration() != null) {
            long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), offre.getDateExpiration().toLocalDate());
            if (daysRemaining < 7) {
                recommendations.add(new Recommendation(
                    "🔴",
                    "Expiration proche",
                    String.format("L'offre expire dans %d jours. Prolongez-la pour recevoir plus de candidatures", daysRemaining),
                    "haute"
                ));
            }
        }

        // Vérifier le salaire
        if (!stats.isSalaireCompetitif()) {
            recommendations.add(new Recommendation(
                "💰",
                "Salaire en dessous du marché",
                String.format("Le salaire moyen pour ce secteur est %.0f DT", stats.getSalaireMoyenSecteur()),
                "haute"
            ));
        }

        // Vérifier les compétences
        if (offre.getCompetencesRequises() == null || offre.getCompetencesRequises().trim().isEmpty()) {
            recommendations.add(new Recommendation(
                "🔧",
                "Compétences manquantes",
                "Ajoutez les compétences requises pour attirer les bons candidats",
                "haute"
            ));
        }

        // Vérifier le taux de conversion
        if (stats.getTauxConversion() < 3.0 && stats.getTotalVues() > 50) {
            recommendations.add(new Recommendation(
                "📉",
                "Faible taux de conversion",
                "Votre taux de conversion est bas. Améliorez le titre et la description",
                "haute"
            ));
        }

        // Recommandations positives
        if (stats.getScoreQualite() >= 80) {
            recommendations.add(new Recommendation(
                "✅",
                "Excellente qualité d'offre",
                "Votre offre est bien rédigée et complète !",
                "basse"
            ));
        }

        if (stats.isSalaireCompetitif()) {
            recommendations.add(new Recommendation(
                "💚",
                "Salaire compétitif",
                "Votre offre salariale est attractive pour le marché",
                "basse"
            ));
        }

        return recommendations;
    }
}

