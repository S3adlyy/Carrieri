package main;

import entities.Postulation;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import services.PostulationService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class PostulationsListController {

    @FXML private FlowPane flowPostulations;
    @FXML private TextField txtSearch;
    @FXML private Label lblStatus;

    private final PostulationService service = new PostulationService();
    private final ObservableList<Postulation> data = FXCollections.observableArrayList();
    private final ObservableList<Postulation> allData = FXCollections.observableArrayList();

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ✅ filter state
    private Integer filterOffreId = null;
    private String filterOffreTitre = null;

    @FXML
    public void initialize() {
        // live search
        txtSearch.textProperty().addListener((obs, o, n) -> handleSearch());
        refreshTable();
    }

    /**
     * ✅ called by OffresShellController to show only postulations for 1 offer
     */
    public void setOffreFilter(int offreId, String offreTitre) {
        this.filterOffreId = offreId;
        this.filterOffreTitre = (offreTitre == null) ? "" : offreTitre;
        if (txtSearch != null) txtSearch.clear();
        refreshTable();
    }

    /**
     * (optional) allow "show all" mode if you want later
     */
    public void clearOffreFilter() {
        this.filterOffreId = null;
        this.filterOffreTitre = null;
        if (txtSearch != null) txtSearch.clear();
        refreshTable();
    }

    @FXML
    private void handleRefresh() {
        refreshTable();
    }

    @FXML
    private void handleSearch() {
        String search = (txtSearch.getText() == null) ? "" : txtSearch.getText().toLowerCase().trim();

        if (search.isEmpty()) {
            data.setAll(allData);
        } else {
            data.setAll(allData.filtered(p ->
                    String.valueOf(p.getOffreId()).contains(search) ||
                            String.valueOf(p.getCandidatId()).contains(search) ||
                            safe(p.getStatut()).toLowerCase().contains(search) ||
                            safe(p.getMotivationCandidature()).toLowerCase().contains(search)
            ));
        }

        updateStatus();
        renderCards();
    }

    private void refreshTable() {
        try {
            // ✅ load according to filter
            if (filterOffreId != null) {
                allData.setAll(service.afficherParOffre(filterOffreId));
            } else {
                allData.setAll(service.read());
            }

            data.setAll(allData);
            updateStatus();
            renderCards();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void updateStatus() {
        if (filterOffreId != null) {
            String titlePart = (filterOffreTitre == null || filterOffreTitre.isBlank())
                    ? ("Offre #" + filterOffreId)
                    : (filterOffreTitre + " (ID " + filterOffreId + ")");
            lblStatus.setText(data.size() + " postulations — " + titlePart);
        } else {
            lblStatus.setText(data.size() + " postulations trouvées");
        }
    }

    private void renderCards() {
        flowPostulations.getChildren().clear();

        if (data.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(Pos.CENTER);
            Label msg = new Label("Aucune postulation à afficher");
            msg.getStyleClass().add("offer-title");
            empty.getChildren().add(msg);
            flowPostulations.getChildren().add(empty);
            return;
        }

        for (Postulation p : data) {
            VBox card = createPostulationCard(p);
            flowPostulations.getChildren().add(card);

            FadeTransition fade = new FadeTransition(Duration.millis(450), card);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();
        }
    }

    private VBox createPostulationCard(Postulation p) {
        VBox card = new VBox(14);
        card.getStyleClass().add("offer-card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setMaxWidth(360);

        Label title = new Label("Postulation ID: " + p.getId());
        title.getStyleClass().add("offer-title");

        HBox offreRow = new HBox(10);
        offreRow.setAlignment(Pos.CENTER);
        Label offreIcon = new Label("📄");
        offreIcon.setStyle("-fx-font-size: 20; -fx-text-fill: #7c3aed;");
        Label offreLabel = new Label("Offre ID: " + p.getOffreId());
        offreLabel.getStyleClass().add("offer-description");
        offreRow.getChildren().addAll(offreIcon, offreLabel);

        HBox candidatRow = new HBox(10);
        candidatRow.setAlignment(Pos.CENTER);
        Label candidatIcon = new Label("👤");
        candidatIcon.setStyle("-fx-font-size: 20; -fx-text-fill: #4c1d95;");
        Label candidatLabel = new Label("Candidat ID: " + p.getCandidatId());
        candidatLabel.getStyleClass().add("offer-info");
        candidatRow.getChildren().addAll(candidatIcon, candidatLabel);

        HBox dateRow = new HBox(10);
        dateRow.setAlignment(Pos.CENTER);
        Label dateIcon = new Label("📅");
        dateIcon.setStyle("-fx-font-size: 20; -fx-text-fill: #6b7280;");
        Label dateLabel = new Label(p.getDatePostulation() == null ? "—" : dateFmt.format(p.getDatePostulation()));
        dateLabel.getStyleClass().add("offer-info");
        dateRow.getChildren().addAll(dateIcon, dateLabel);

        // Statut combo (kept)
        HBox statutRow = new HBox(12);
        statutRow.setAlignment(Pos.CENTER);

        Label statutIcon = new Label("🔖");
        statutIcon.setStyle("-fx-font-size: 20; -fx-text-fill: #7c3aed;");

        ComboBox<String> comboStatut = new ComboBox<>(FXCollections.observableArrayList(
                "En attente", "En cours", "Acceptée", "Refusée"
        ));
        comboStatut.setValue(p.getStatut());
        comboStatut.getStyleClass().add("combo-elegant");
        comboStatut.setPrefWidth(200);

        comboStatut.setOnAction(e -> {
            String newStatut = comboStatut.getValue();
            try {
                service.changerStatut(p.getId(), newStatut);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Statut mis à jour.");
                // update local object text for search consistency
                p.setStatut(newStatut);
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
                comboStatut.setValue(p.getStatut());
            }
        });

        statutRow.getChildren().addAll(statutIcon, comboStatut);

        HBox motivationRow = new HBox(10);
        motivationRow.setAlignment(Pos.CENTER);
        Label motivationIcon = new Label("📝");
        motivationIcon.setStyle("-fx-font-size: 20; -fx-text-fill: #374151;");

        String motiv = safe(p.getMotivationCandidature());
        String motivSnippet = motiv.length() > 80 ? motiv.substring(0, 80) + "..." : motiv;

        Label motivation = new Label(motivSnippet.isBlank() ? "—" : motivSnippet);
        motivation.getStyleClass().add("offer-description");
        motivation.setWrapText(true);

        motivationRow.getChildren().addAll(motivationIcon, motivation);

        HBox actions = new HBox(20);
        actions.setAlignment(Pos.CENTER);
        actions.getStyleClass().add("offer-actions");

        Button btnDelete = new Button();
        btnDelete.getStyleClass().add("btn-icon-delete");
        btnDelete.setGraphic(new Label("🗑"));
        btnDelete.setOnAction(e -> handleDelete(p));

        actions.getChildren().add(btnDelete);

        card.getChildren().addAll(title, offreRow, candidatRow, dateRow, statutRow, motivationRow, actions);

        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(220), card);
        scaleUp.setToX(1.04);
        scaleUp.setToY(1.04);

        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(220), card);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);

        card.setOnMouseEntered(e -> scaleUp.playFromStart());
        card.setOnMouseExited(e -> scaleDown.playFromStart());

        return card;
    }

    private void handleDelete(Postulation p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer suppression");
        confirm.setHeaderText("Supprimer cette postulation ?");
        confirm.setContentText("ID: " + p.getId());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                service.supprimer(p.getId());
                refreshTable();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Postulation supprimée.");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
