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
    @FXML private Button btnBack;
    @FXML private Button btnFilter;

    private final PostulationService service = new PostulationService();
    private final ObservableList<Postulation> data = FXCollections.observableArrayList();
    private final ObservableList<Postulation> allData = FXCollections.observableArrayList();

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    //  ADDED: filter state
    private Integer filterOffreId = null;
    private String filterOffreTitre = null;

    @FXML
    public void initialize() {
        refreshTable();
    }

    @FXML
    private void handleBack() {
        // Retour au tableau des offres pour recruteur
        OffresShellController shellController = OffresShellController.getInstance();
        if (shellController != null) {
            shellController.showOffresTable();
        }
    }

    //  ADDED: called by Shell when user clicks 👥 on an offer
    public void setOffreFilter(int offreId, String offreTitre) {
        this.filterOffreId = offreId;
        this.filterOffreTitre = (offreTitre == null) ? "" : offreTitre;
        if (txtSearch != null) txtSearch.clear();
        refreshTable();
    }


    @FXML
    private void handleRefresh() {
        refreshTable();
    }

    @FXML
    private void handleSearch() {
        String search = txtSearch.getText().toLowerCase().trim();
        if (search.isEmpty()) {
            data.setAll(allData);
        } else {
            data.setAll(allData.filtered(p ->
                    String.valueOf(p.getId()).contains(search) ||
                    String.valueOf(p.getOffreId()).contains(search) ||
                    String.valueOf(p.getCandidatId()).contains(search) ||
                    p.getStatut().toLowerCase().contains(search) ||
                    p.getMotivationCandidature().toLowerCase().contains(search) ||
                    p.getDatePostulation().toString().contains(search)
            ));
        }

        //  keep label consistent with filter
        if (filterOffreId != null) {
            String titlePart = (filterOffreTitre == null || filterOffreTitre.isBlank())
                    ? ("Offre #" + filterOffreId)
                    : (filterOffreTitre + " (ID " + filterOffreId + ")");
            lblStatus.setText(data.size() + " postulations — " + titlePart);
        } else {
            lblStatus.setText(data.size() + " postulations trouvées");
        }

        renderCards();
    }

    @FXML
    private void handleFilter() {
        // Créer un menu contextuel pour filtrer par statut
        ContextMenu filterMenu = new ContextMenu();

        MenuItem allItem = new MenuItem("Toutes les postulations");
        allItem.setOnAction(e -> {
            filterOffreId = null;
            filterOffreTitre = null;
            txtSearch.clear();
            refreshTable();
        });

        MenuItem pendingItem = new MenuItem("En attente");
        pendingItem.setOnAction(e -> filterByStatut("En attente"));

        MenuItem progressItem = new MenuItem("En cours");
        progressItem.setOnAction(e -> filterByStatut("En cours"));

        MenuItem acceptedItem = new MenuItem("Acceptée");
        acceptedItem.setOnAction(e -> filterByStatut("Acceptée"));

        MenuItem refusedItem = new MenuItem("Refusée");
        refusedItem.setOnAction(e -> filterByStatut("Refusée"));

        filterMenu.getItems().addAll(allItem, new SeparatorMenuItem(),
                                     pendingItem, progressItem, acceptedItem, refusedItem);

        // Afficher le menu sous le bouton filter
        if (btnFilter != null) {
            filterMenu.show(btnFilter, javafx.geometry.Side.BOTTOM, 0, 0);
        }
    }

    private void filterByStatut(String statut) {
        data.setAll(allData.filtered(p -> p.getStatut().equalsIgnoreCase(statut)));
        lblStatus.setText(data.size() + " postulations (" + statut + ")");
        renderCards();
    }

    private void refreshTable() {
        try {
            //  CHANGED: load filtered if filter is set
            if (filterOffreId != null) {
                allData.setAll(service.afficherParOffre(filterOffreId));
            } else {
                allData.setAll(service.read());
            }

            data.setAll(allData);

            if (filterOffreId != null) {
                String titlePart = (filterOffreTitre == null || filterOffreTitre.isBlank())
                        ? ("Offre #" + filterOffreId)
                        : (filterOffreTitre + " (ID " + filterOffreId + ")");
                lblStatus.setText(data.size() + " postulations — " + titlePart);
            } else {
                lblStatus.setText(data.size() + " postulations trouvées");
            }

            renderCards();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void renderCards() {
        flowPostulations.getChildren().clear();

        for (Postulation p : data) {
            VBox card = createPostulationCard(p);
            flowPostulations.getChildren().add(card);

            FadeTransition fade = new FadeTransition(Duration.millis(700), card);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();
        }
    }

    private VBox createPostulationCard(Postulation p) {
        VBox card = new VBox(14);
        card.getStyleClass().add("offer-card");  // Reuse offer-card style for consistency
        card.setAlignment(Pos.TOP_CENTER);
        card.setMaxWidth(360);

        // Title: "Postulation ID: X"
        Label title = new Label("Postulation ID: " + p.getId());
        title.getStyleClass().add("offer-title");

        // Offre + icon
        HBox offreRow = new HBox(10);
        offreRow.setAlignment(Pos.CENTER);

        Label offreIcon = new Label("📄");
        offreIcon.setStyle("-fx-font-size: 20; -fx-text-fill: #7c3aed;");

        Label offreLabel = new Label("Offre ID: " + p.getOffreId());  // Or fetch titre if needed
        offreLabel.getStyleClass().add("offer-description");

        offreRow.getChildren().addAll(offreIcon, offreLabel);

        // Candidat + icon
        HBox candidatRow = new HBox(10);
        candidatRow.setAlignment(Pos.CENTER);

        Label candidatIcon = new Label("👤");
        candidatIcon.setStyle("-fx-font-size: 20; -fx-text-fill: #4c1d95;");

        Label candidatLabel = new Label("Candidat ID: " + p.getCandidatId());
        candidatLabel.getStyleClass().add("offer-info");

        candidatRow.getChildren().addAll(candidatIcon, candidatLabel);

        // Date + icon
        HBox dateRow = new HBox(10);
        dateRow.setAlignment(Pos.CENTER);

        Label dateIcon = new Label("📅");
        dateIcon.setStyle("-fx-font-size: 20; -fx-text-fill: #6b7280;");

        Label dateLabel = new Label(dateFmt.format(p.getDatePostulation()));
        dateLabel.getStyleClass().add("offer-info");

        dateRow.getChildren().addAll(dateIcon, dateLabel);

        // Statut with badge + modifier combo
        HBox statutRow = new HBox(12);
        statutRow.setAlignment(Pos.CENTER);

        Label statutIcon = new Label("🔖");
        statutIcon.setStyle("-fx-font-size: 20; -fx-text-fill: #7c3aed;");

        ComboBox<String> comboStatut = new ComboBox<>(FXCollections.observableArrayList("En attente", "En cours", "Acceptée", "Refusée"));
        comboStatut.setValue(p.getStatut());
        comboStatut.getStyleClass().add("combo-elegant");
        comboStatut.setPrefWidth(200);

        comboStatut.setOnAction(e -> {
            String newStatut = comboStatut.getValue();
            try {
                service.changerStatut(p.getId(), newStatut);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Statut mis à jour.");
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
                comboStatut.setValue(p.getStatut());  // Revert on error
            }
        });

        statutRow.getChildren().addAll(statutIcon, comboStatut);

        // Motivation snippet + icon
        HBox motivationRow = new HBox(10);
        motivationRow.setAlignment(Pos.CENTER);

        Label motivationIcon = new Label("📝");
        motivationIcon.setStyle("-fx-font-size: 20; -fx-text-fill: #374151;");

        String motivSnippet = p.getMotivationCandidature().length() > 80
                ? p.getMotivationCandidature().substring(0, 80) + "..."
                : p.getMotivationCandidature();
        Label motivation = new Label(motivSnippet);
        motivation.getStyleClass().add("offer-description");
        motivation.setWrapText(true);

        motivationRow.getChildren().addAll(motivationIcon, motivation);

        // Actions: only delete (as it's a postulation)
        HBox actions = new HBox(20);
        actions.setAlignment(Pos.CENTER);
        actions.getStyleClass().add("offer-actions");

        Button btnDelete = new Button();
        btnDelete.getStyleClass().add("btn-icon-delete");
        btnDelete.setGraphic(new Label("🗑"));
        btnDelete.setOnAction(e -> handleDelete(p));

        actions.getChildren().add(btnDelete);

        // Assemble card
        card.getChildren().addAll(title, offreRow, candidatRow, dateRow, statutRow, motivationRow, actions);

        // Hover animation
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

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
