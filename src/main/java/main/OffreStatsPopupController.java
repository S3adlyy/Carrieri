package main;

import entities.OffreEmploi;
import entities.Postulation;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import services.PostulationService;
import services.OffreAnalyticsService;
import services.OffreAnalyticsService.OffreStatistics;
import services.OffreAnalyticsService.Recommendation;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class OffreStatsPopupController {

    // Éléments existants
    @FXML private Label lblOffreTitre;
    @FXML private Label lblOffreInfo;
    @FXML private Label lblTotalPostulations;
    @FXML private Label lblEnAttente;
    @FXML private Label lblAcceptees;
    @FXML private Label lblTauxAcceptation;
    @FXML private PieChart pieChartStatuts;
    @FXML private BarChart<String, Number> barChartEvolution;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private VBox vboxRecentPostulations;

    // Nouveaux éléments pour analytics avancées
    @FXML private Label lblTotalVues;
    @FXML private Label lblTauxConversion;
    @FXML private Label lblScoreQualite;
    @FXML private Label lblTendance;
    @FXML private LineChart<String, Number> lineChartVues;
    @FXML private BarChart<String, Number> barChartHeures;
    @FXML private BarChart<String, Number> barChartLocalisation;
    @FXML private VBox vboxRecommandations;
    @FXML private Label lblSalaireComparaison;
    @FXML private ScrollPane scrollPane;

    private final PostulationService postulationService = new PostulationService();
    private final OffreAnalyticsService analyticsService = new OffreAnalyticsService();
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private OffreEmploi offre;
    private List<Postulation> postulations;
    private OffreStatistics statistics;

    public void setOffre(OffreEmploi offre) {
        this.offre = offre;
        loadStats();
    }

    private void loadStats() {
        try {
            // Charger les données
            postulations = postulationService.afficherParOffre(offre.getId());
            statistics = analyticsService.getStatistics(offre);

            // Afficher les informations de base
            lblOffreTitre.setText(offre.getTitre());
            lblOffreInfo.setText(offre.getEntreprise() + " • " + offre.getTypeContrat() + " • " + offre.getLocalisation());

            // Calculer les KPIs de base
            calculerKPIs();

            // Nouveaux KPIs avancés
            afficherKPIsAvances();

            // Graphiques existants
            creerPieChart();
            creerBarChart();

            // Nouveaux graphiques avancés
            creerLineChartVues();
            creerBarChartHeures();
            // Répartition géographique supprimée pour simplifier l'interface

            // Recommandations
            afficherRecommandations();

            // Postulations récentes
            afficherPostulationsRecentes();

        } catch (SQLException e) {
            lblOffreTitre.setText("Erreur lors du chargement des statistiques");
            e.printStackTrace();
        }
    }

    private void afficherKPIsAvances() {
        // Total vues
        if (lblTotalVues != null) {
            lblTotalVues.setText(String.valueOf(statistics.getTotalVues()));
        }

        // Taux de conversion
        if (lblTauxConversion != null) {
            lblTauxConversion.setText(String.format("%.1f%%", statistics.getTauxConversion()));
        }

        // Score de qualité
        if (lblScoreQualite != null) {
            int score = statistics.getScoreQualite();
            lblScoreQualite.setText(score + "/100");
        }

        // Tendance
        if (lblTendance != null) {
            double variation = statistics.getVariationPourcentage();
            String signe = variation > 0 ? "+" : "";
            lblTendance.setText(signe + String.format("%.1f%%", variation));
        }

        // Comparaison salaire
        if (lblSalaireComparaison != null) {
            if (statistics.isSalaireCompetitif()) {
                lblSalaireComparaison.setText("✅ Salaire compétitif (" +
                    String.format("%.0f DT vs %.0f DT moyenne)", offre.getSalaire(), statistics.getSalaireMoyenSecteur()));
            } else {
                lblSalaireComparaison.setText("⚠️ En dessous du marché (" +
                    String.format("%.0f DT vs %.0f DT moyenne)", offre.getSalaire(), statistics.getSalaireMoyenSecteur()));
            }
        }
    }

    private void creerLineChartVues() {
        if (lineChartVues == null) return;

        lineChartVues.getData().clear();
        lineChartVues.setTitle("Évolution des Vues");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Vues quotidiennes");

        Map<LocalDate, Integer> vuesParJour = statistics.getVuesParJour();

        // Prendre les 30 derniers jours
        List<LocalDate> dates = new ArrayList<>(vuesParJour.keySet());
        Collections.sort(dates);

        int start = Math.max(0, dates.size() - 30);
        for (int i = start; i < dates.size(); i++) {
            LocalDate date = dates.get(i);
            Integer vues = vuesParJour.get(date);
            series.getData().add(new XYChart.Data<>(
                date.format(DateTimeFormatter.ofPattern("dd/MM")),
                vues
            ));
        }

        lineChartVues.getData().add(series);
    }

    private void creerBarChartHeures() {
        if (barChartHeures == null) return;

        barChartHeures.getData().clear();
        barChartHeures.setTitle("Vues par Heure");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Distribution horaire");

        Map<Integer, Integer> vuesParHeure = statistics.getVuesParHeure();

        for (int heure = 0; heure < 24; heure++) {
            Integer vues = vuesParHeure.getOrDefault(heure, 0);
            series.getData().add(new XYChart.Data<>(heure + "h", vues));
        }

        barChartHeures.getData().add(series);
    }

    private void creerBarChartLocalisation() {
        if (barChartLocalisation == null) return;

        barChartLocalisation.getData().clear();
        barChartLocalisation.setTitle("Candidatures par Localisation");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Répartition géographique");

        Map<String, Integer> parLocalisation = statistics.getCandidaturesParLocalisation();

        // Trier par nombre décroissant
        parLocalisation.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .forEach(entry -> {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            });

        barChartLocalisation.getData().add(series);
    }

    private void afficherRecommandations() {
        if (vboxRecommandations == null) return;

        vboxRecommandations.getChildren().clear();

        List<Recommendation> recommendations = statistics.getRecommandations();

        if (recommendations.isEmpty()) {
            Label noReco = new Label("✅ Aucune recommandation - Votre offre est optimale !");
            noReco.setStyle("-fx-text-fill: #10b981; -fx-font-size: 14; -fx-font-weight: 600;");
            vboxRecommandations.getChildren().add(noReco);
            return;
        }

        for (Recommendation reco : recommendations) {
            vboxRecommandations.getChildren().add(creerItemRecommandation(reco));
        }
    }

    private VBox creerItemRecommandation(Recommendation reco) {
        VBox item = new VBox(8);
        item.setPadding(new Insets(16));
        item.setStyle("-fx-background-color: #fef3c7; -fx-background-radius: 12; -fx-border-color: #fbbf24; -fx-border-radius: 12; -fx-border-width: 2;");

        // Si priorité haute, mettre en rouge
        if ("haute".equals(reco.getPriorite())) {
            item.setStyle("-fx-background-color: #fee2e2; -fx-background-radius: 12; -fx-border-color: #ef4444; -fx-border-radius: 12; -fx-border-width: 2;");
        } else if ("basse".equals(reco.getPriorite())) {
            item.setStyle("-fx-background-color: #d1fae5; -fx-background-radius: 12; -fx-border-color: #10b981; -fx-border-radius: 12; -fx-border-width: 2;");
        }

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label(reco.getIcon());
        icon.setStyle("-fx-font-size: 24;");

        Label titre = new Label(reco.getTitre());
        titre.setStyle("-fx-font-weight: 700; -fx-font-size: 15; -fx-text-fill: #1f2937;");

        header.getChildren().addAll(icon, titre);

        Label description = new Label(reco.getDescription());
        description.setWrapText(true);
        description.setStyle("-fx-font-size: 13; -fx-text-fill: #374151;");

        item.getChildren().addAll(header, description);

        return item;
    }

    private void calculerKPIs() {
        int total = postulations.size();
        Map<String, Long> countByStatut = postulations.stream()
            .collect(Collectors.groupingBy(Postulation::getStatut, Collectors.counting()));

        int enAttente = countByStatut.getOrDefault("En attente", 0L).intValue();
        int acceptees = countByStatut.getOrDefault("Acceptée", 0L).intValue();
        double tauxAcceptation = total > 0 ? (acceptees * 100.0 / total) : 0;

        lblTotalPostulations.setText(String.valueOf(total));
        lblEnAttente.setText(String.valueOf(enAttente));
        lblAcceptees.setText(String.valueOf(acceptees));
        lblTauxAcceptation.setText(String.format("%.1f%%", tauxAcceptation));
    }

    private void creerPieChart() {
        Map<String, Long> countByStatut = postulations.stream()
            .collect(Collectors.groupingBy(Postulation::getStatut, Collectors.counting()));

        pieChartStatuts.getData().clear();

        // Palette de couleurs violettes claires et contrastées
        Map<String, String> colors = new HashMap<>();
        colors.put("En attente", "#fbbf24");   // Jaune-orangé doux
        colors.put("En cours", "#a78bfa");     // Violet clair
        colors.put("Acceptée", "#10b981");     // Vert émeraude
        colors.put("Refusée", "#f87171");      // Rouge corail doux

        for (Map.Entry<String, Long> entry : countByStatut.entrySet()) {
            PieChart.Data slice = new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue());
            pieChartStatuts.getData().add(slice);
        }

        // Appliquer les couleurs avec une meilleure lisibilité
        pieChartStatuts.getData().forEach(data -> {
            String statut = data.getName().split(" \\(")[0];
            String color = colors.getOrDefault(statut, "#9ca3af");
            data.getNode().setStyle(
                "-fx-pie-color: " + color + ";" +
                "-fx-border-color: white;" +
                "-fx-border-width: 2px;"
            );
        });
    }

    private void creerBarChart() {
        barChartEvolution.getData().clear();

        Map<String, Long> countByMonth = postulations.stream()
            .collect(Collectors.groupingBy(
                p -> p.getDatePostulation().format(DateTimeFormatter.ofPattern("MM/yyyy")),
                Collectors.counting()
            ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Postulations");

        countByMonth.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue())));

        barChartEvolution.getData().add(series);
        xAxis.setLabel("Mois");
        yAxis.setLabel("Nombre");
    }

    private void afficherPostulationsRecentes() {
        vboxRecentPostulations.getChildren().clear();

        List<Postulation> recentes = postulations.stream()
            .sorted((p1, p2) -> p2.getDatePostulation().compareTo(p1.getDatePostulation()))
            .limit(5)
            .collect(Collectors.toList());

        if (recentes.isEmpty()) {
            Label noData = new Label("Aucune postulation pour le moment");
            noData.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14;");
            vboxRecentPostulations.getChildren().add(noData);
            return;
        }

        for (Postulation p : recentes) {
            vboxRecentPostulations.getChildren().add(creerItemPostulation(p));
        }
    }

    private HBox creerItemPostulation(Postulation p) {
        HBox item = new HBox(16);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 12; -fx-padding: 16; -fx-border-color: #e5e7eb; -fx-border-radius: 12; -fx-border-width: 1;");

        Label icon = new Label("👤");
        icon.setStyle("-fx-font-size: 24;");

        VBox info = new VBox(4);
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

        Label candidatLabel = new Label("Candidat ID: " + p.getCandidatId());
        candidatLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 14; -fx-text-fill: #1f2937;");

        Label dateLabel = new Label(dateFmt.format(p.getDatePostulation()));
        dateLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #6b7280;");

        info.getChildren().addAll(candidatLabel, dateLabel);

        Label statutBadge = new Label(p.getStatut());
        statutBadge.setStyle("-fx-background-color: " + getStatutColor(p.getStatut()) + "; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: 700; -fx-padding: 6 12; -fx-background-radius: 999;");

        item.getChildren().addAll(icon, info, statutBadge);
        return item;
    }

    private String getStatutColor(String statut) {
        switch (statut.toLowerCase()) {
            case "en attente": return "#fbbf24";   // Jaune-orangé doux
            case "en cours": return "#a78bfa";     // Violet clair
            case "acceptée": return "#10b981";     // Vert émeraude
            case "refusée": return "#f87171";      // Rouge corail doux
            default: return "#9ca3af";             // Gris par défaut
        }
    }

    @FXML
    private void handleClose() {
        lblOffreTitre.getScene().getWindow().hide();
    }
}

