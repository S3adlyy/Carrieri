package main;

import entities.OffreEmploi;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import services.OffreEmploiService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class OffresListController {

    @FXML private FlowPane flowOffers;
    @FXML private TextField txtSearch;
    @FXML private Label lblStatus;

    private final OffreEmploiService service = new OffreEmploiService();
    private final ObservableList<OffreEmploi> data = FXCollections.observableArrayList();
    private final ObservableList<OffreEmploi> allData = FXCollections.observableArrayList();

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public void initialize() {
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
            data.setAll(allData.filtered(o ->
                    o.getTitre().toLowerCase().contains(search) ||
                            o.getEntreprise().toLowerCase().contains(search) ||
                            o.getLocalisation().toLowerCase().contains(search)
            ));
        }
        lblStatus.setText(data.size() + " offres trouvées");
        renderCards();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void refreshTable() {
        try {
            allData.setAll(service.read());
            data.setAll(allData);
            lblStatus.setText(data.size() + " offres trouvées");
            renderCards();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void renderCards() {
        flowOffers.getChildren().clear();

        for (OffreEmploi offre : data) {
            VBox card = createOfferCard(offre);
            flowOffers.getChildren().add(card);

            // Fade-in on load
            FadeTransition fade = new FadeTransition(Duration.millis(700), card);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();
        }
    }

    private VBox createOfferCard(OffreEmploi offre) {
        VBox card = new VBox(14);
        card.getStyleClass().add("offer-card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setMaxWidth(360);  // fixed width for consistency

        // Title
        Label title = new Label(offre.getTitre());
        title.getStyleClass().add("offer-title");

        // Contract badge + company icon
        HBox badgeRow = new HBox(10);
        badgeRow.setAlignment(Pos.CENTER);

        Label badge = new Label(offre.getTypeContrat());
        badge.getStyleClass().add("offer-contract-badge");

        Label companyIcon = new Label("💼");  // Briefcase icon
        companyIcon.setStyle("-fx-font-size: 20; -fx-text-fill: #7c3aed;");

        badgeRow.getChildren().addAll(companyIcon, badge);

        // Tech stack icon + description
        HBox techRow = new HBox(8);
        techRow.setAlignment(Pos.CENTER_LEFT);

        Label techIcon = new Label("</>");  // Code brackets
        techIcon.setStyle("-fx-font-size: 18; -fx-text-fill: #4c1d95;");

        Label desc = new Label(offre.getDescription().length() > 80
                ? offre.getDescription().substring(0, 80) + "..."
                : offre.getDescription());
        desc.getStyleClass().add("offer-description");
        desc.setWrapText(true);

        techRow.getChildren().addAll(techIcon, desc);

        // Location + date
        HBox locRow = new HBox(8);
        locRow.setAlignment(Pos.CENTER_LEFT);

        Label locIcon = new Label("📍");
        locIcon.setStyle("-fx-font-size: 16; -fx-text-fill: #6b7280;");

        Label locDate = new Label(offre.getLocalisation() + " ○ " + dateFmt.format(offre.getDateExpiration()));
        locDate.getStyleClass().add("offer-info");

        locRow.getChildren().addAll(locIcon, locDate);

        // Salary with icon
        HBox salaryRow = new HBox(8);
        salaryRow.setAlignment(Pos.CENTER);

        Label moneyIcon = new Label("💰");
        moneyIcon.setStyle("-fx-font-size: 22; -fx-text-fill: #4c1d95;");

        Label salary = new Label(String.format("%.0f DT", offre.getSalaire()));
        salary.getStyleClass().add("offer-salary");

        salaryRow.getChildren().addAll(moneyIcon, salary);

        // Actions
        HBox actions = new HBox(20);
        actions.setAlignment(Pos.CENTER);
        actions.getStyleClass().add("offer-actions");

        Button btnPostuler = new Button("Postuler");
        btnPostuler.getStyleClass().add("btn-postuler");
        btnPostuler.setOnAction(e -> OffresShellController.getInstance().showPostuler(offre.getId(), offre.getTitre()));

        Button btnEdit = new Button();
        btnEdit.getStyleClass().add("btn-icon-edit");
        btnEdit.setGraphic(new Label("✏"));  // Pencil


        Button btnDelete = new Button();
        btnDelete.getStyleClass().add("btn-icon-delete");
        btnDelete.setGraphic(new Label("🗑"));  // Trash


        actions.getChildren().addAll(btnPostuler, btnEdit, btnDelete);

        // Assemble card
        card.getChildren().addAll(title, badgeRow, techRow, locRow, salaryRow, actions);

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

    // Your existing handleEdit / handleDelete / showAlert methods...
}