package com.example.guser.controllers.greclam;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import services.greclam.StatsService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private Label totalReclamationsLabel;
    @FXML private Label totalFeedbacksLabel;
    @FXML private Label totalTraitementsLabel;
    @FXML private Label moyenneNotesLabel;
    @FXML private Label tauxResolutionLabel;
    @FXML private Label tempsMoyenLabel;

    @FXML private PieChart statutChart;
    @FXML private PieChart prioriteChart;
    @FXML private PieChart feedbackChart;
    @FXML private BarChart<String, Number> evolutionChart;
    @FXML private CategoryAxis moisAxis;
    @FXML private NumberAxis countAxis;

    @FXML private ListView<String> urgentesListView;
    @FXML private ComboBox<Integer> anneeCombo;
    @FXML private Button refreshButton;
    @FXML private VBox loadingIndicator;

    private StatsService statsService = new StatsService();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupAnnees();
        chargerStatistiques();
        setupRefreshButton();
    }

    private void setupAnnees() {
        int anneeCourante = LocalDate.now().getYear();
        for (int i = anneeCourante - 2; i <= anneeCourante; i++) {
            anneeCombo.getItems().add(i);
        }
        anneeCombo.setValue(anneeCourante);
        anneeCombo.setOnAction(e -> chargerStatistiques());
    }

    private void setupRefreshButton() {
        refreshButton.setOnAction(e -> chargerStatistiques());
    }

    private void chargerStatistiques() {
        showLoading(true);

        try {
            // Statistiques générales
            totalReclamationsLabel.setText(String.valueOf(statsService.getTotalReclamations()));
            totalFeedbacksLabel.setText(String.valueOf(statsService.getTotalFeedbacks()));
            totalTraitementsLabel.setText(String.valueOf(statsService.getTotalTraitements()));

            double moyenne = statsService.getMoyenneNotes();
            moyenneNotesLabel.setText(String.format("%.1f/100", moyenne));

            double taux = statsService.getTauxResolution();
            tauxResolutionLabel.setText(String.format("%.1f%%", taux));

            double temps = statsService.getTempsMoyenTraitement();
            if (temps > 0) {
                tempsMoyenLabel.setText(String.format("%.1f heures", temps));
            } else {
                tempsMoyenLabel.setText("N/A");
            }

            // Graphiques
            chargerStatuts();
            chargerPriorites();
            chargerFeedbacks();
            chargerEvolution();
            chargerReclamationsUrgentes();

        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les statistiques: " + e.getMessage());
            e.printStackTrace();
        } finally {
            showLoading(false);
        }
    }

    private void chargerStatuts() throws SQLException {
        Map<String, Integer> stats = statsService.getReclamationsParStatut();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        String[] couleurs = {"#3498db", "#f39c12", "#27ae60", "#7f8c8d"};
        int i = 0;

        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            if (entry.getValue() > 0) {
                PieChart.Data data = new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue());
                pieData.add(data);
            }
        }

        statutChart.setData(pieData);
        statutChart.setTitle("Réclamations par statut");
    }

    private void chargerPriorites() throws SQLException {
        Map<String, Integer> stats = statsService.getReclamationsParPriorite();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            if (entry.getValue() > 0) {
                pieData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
            }
        }

        prioriteChart.setData(pieData);
        prioriteChart.setTitle("Réclamations par priorité");
    }

    private void chargerFeedbacks() throws SQLException {
        Map<String, Integer> stats = statsService.getFeedbacksParNote();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            if (entry.getValue() > 0) {
                pieData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
            }
        }

        feedbackChart.setData(pieData);
        feedbackChart.setTitle("Feedbacks par note");
    }

    private void chargerEvolution() throws SQLException {
        int annee = anneeCombo.getValue();
        Map<String, Integer> reclamations = statsService.getReclamationsParMois(annee);
        Map<String, Integer> traitements = statsService.getTraitementsParMois(annee);

        XYChart.Series<String, Number> recSeries = new XYChart.Series<>();
        recSeries.setName("Réclamations");

        XYChart.Series<String, Number> traitSeries = new XYChart.Series<>();
        traitSeries.setName("Traitements");

        for (Map.Entry<String, Integer> entry : reclamations.entrySet()) {
            recSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        for (Map.Entry<String, Integer> entry : traitements.entrySet()) {
            traitSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        evolutionChart.getData().clear();
        evolutionChart.getData().addAll(recSeries, traitSeries);
        evolutionChart.setTitle("Évolution " + annee);
    }

    private void chargerReclamationsUrgentes() throws SQLException {
        var urgentes = statsService.getReclamationsUrgentes();
        ObservableList<String> items = FXCollections.observableArrayList();

        for (var r : urgentes) {
            String date = formatter.format(r.getDateCreation().toInstant()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate());
            items.add(String.format("#%d - %s (%s) - %s",
                    r.getId(), r.getObjet(), r.getPriorite(), date));
        }

        urgentesListView.setItems(items);

        if (items.isEmpty()) {
            urgentesListView.setItems(FXCollections.observableArrayList(
                    "✅ Aucune réclamation urgente"));
        }
    }

    private void showLoading(boolean show) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(show);
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}