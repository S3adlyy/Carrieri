package main;

import entities.OffreEmploi;
import entities.Postulation;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import services.PostulationService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class OffreStatsPopupController {

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

    private final PostulationService postulationService = new PostulationService();
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private OffreEmploi offre;
    private List<Postulation> postulations;

    public void setOffre(OffreEmploi offre) {
        this.offre = offre;
        loadStats();
    }

    private void loadStats() {
        try {
            postulations = postulationService.afficherParOffre(offre.getId());
            lblOffreTitre.setText(offre.getTitre());
            lblOffreInfo.setText(offre.getEntreprise() + " • " + offre.getTypeContrat() + " • " + offre.getLocalisation());
            calculerKPIs();
            creerPieChart();
            creerBarChart();
            afficherPostulationsRecentes();
        } catch (SQLException e) {
            lblOffreTitre.setText("Erreur lors du chargement des statistiques");
        }
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

        Map<String, String> colors = new HashMap<>();
        colors.put("En attente", "#f59e0b");
        colors.put("En cours", "#3b82f6");
        colors.put("Acceptée", "#10b981");
        colors.put("Refusée", "#ef4444");

        for (Map.Entry<String, Long> entry : countByStatut.entrySet()) {
            PieChart.Data slice = new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue());
            pieChartStatuts.getData().add(slice);
        }

        pieChartStatuts.getData().forEach(data -> {
            String statut = data.getName().split(" \\(")[0];
            String color = colors.getOrDefault(statut, "#6b7280");
            data.getNode().setStyle("-fx-pie-color: " + color + ";");
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
            case "en attente": return "#f59e0b";
            case "en cours": return "#3b82f6";
            case "acceptée": return "#10b981";
            case "refusée": return "#ef4444";
            default: return "#6b7280";
        }
    }

    @FXML
    private void handleClose() {
        lblOffreTitre.getScene().getWindow().hide();
    }
}

