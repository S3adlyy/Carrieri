package main;

import entities.FavoriteOffre;
import entities.OffreEmploi;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.FavoriteOffreService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class FavoritesListController {

    @FXML private FlowPane flowFavorites;
    @FXML private Label lblStatus;
    @FXML private Label lblCount;
    @FXML private Button btnBack;

    private final FavoriteOffreService favoriteService = new FavoriteOffreService();
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // TODO: Remplacer par l'ID du candidat connecté
    private final int CURRENT_CANDIDAT_ID = 1;

    @FXML
    public void initialize() {
        refreshList();
    }

    public void refreshList() {
        List<FavoriteOffre> favorites = favoriteService.getFavoritesByCandidat(CURRENT_CANDIDAT_ID);

        if (lblCount != null) {
            lblCount.setText(favorites.size() + " favori" + (favorites.size() > 1 ? "s" : ""));
        }

        if (favorites.isEmpty()) {
            renderEmpty();
        } else {
            renderCards(favorites);
        }
    }

    private void renderEmpty() {
        flowFavorites.getChildren().clear();

        VBox empty = new VBox(20);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(80));
        empty.setPrefWidth(900);

        Label icon = new Label("💔");
        icon.setStyle("-fx-font-size: 72px;");

        Label msg = new Label("Aucun favori pour le moment");
        msg.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #5c5470;");

        Label hint = new Label("Ajoutez des offres à vos favoris depuis la liste des offres");
        hint.setStyle("-fx-font-size: 14px; -fx-text-fill: #8b7fa0;");

        empty.getChildren().addAll(icon, msg, hint);
        flowFavorites.getChildren().add(empty);
    }

    private void renderCards(List<FavoriteOffre> favorites) {
        flowFavorites.getChildren().clear();

        for (FavoriteOffre favorite : favorites) {
            if (favorite.getOffre() != null) {
                VBox card = createFavoriteCard(favorite);
                flowFavorites.getChildren().add(card);
            }
        }
    }

    private VBox createFavoriteCard(FavoriteOffre favorite) {
        OffreEmploi offre = favorite.getOffre();

        VBox card = new VBox(14);
        card.setPadding(new Insets(24));
        card.setMaxWidth(450);
        card.setPrefWidth(450);
        card.getStyleClass().add("offer-card");

        // Header avec titre et badge
        HBox header = new HBox(12);
        header.setAlignment(Pos.TOP_LEFT);

        Label title = new Label(offre.getTitre() != null ? offre.getTitre() : "—");
        title.getStyleClass().add("c-title");
        title.setMaxWidth(280);
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);

        Label badge = new Label(offre.getTypeContrat() != null ? offre.getTypeContrat() : "—");
        badge.getStyleClass().add("c-badge");

        header.getChildren().addAll(title, badge);

        // Entreprise
        HBox companyRow = metaRowItem("🏢", offre.getEntreprise() != null ? offre.getEntreprise() : "—");

        // Description
        String desc = offre.getDescription() != null ? offre.getDescription() : "";
        Label description = new Label(desc.length() > 200 ? desc.substring(0, 200) + "..." : desc);
        description.getStyleClass().add("c-desc");
        description.setWrapText(true);

        // Meta info
        HBox meta = new HBox(18);
        meta.setAlignment(Pos.CENTER_LEFT);
        meta.getChildren().addAll(
            metaRowItem("📍", offre.getLocalisation() != null ? offre.getLocalisation() : "—"),
            metaRowItem("⏰", "Ajouté le " + dateFmt.format(favorite.getDateAjout()))
        );

        // Séparateur
        Separator sep = new Separator();
        sep.getStyleClass().add("c-sep");

        // Footer avec salaire et actions
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label salary = new Label(String.format("%.0f DT", offre.getSalaire()));
        salary.getStyleClass().add("c-salary");
        salary.setMinWidth(100);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Bouton retirer des favoris - TOUJOURS fond rouge avec cœur brisé
        Button btnRemove = new Button();

        // Utiliser setText directement
        btnRemove.setText("💔");
        btnRemove.setStyle("-fx-font-size: 16px; -fx-font-family: 'Segoe UI Emoji', 'Apple Color Emoji', 'Noto Color Emoji';");

        btnRemove.getStyleClass().clear();
        btnRemove.getStyleClass().addAll("btn-favorite", "btn-favorite-remove");
        btnRemove.setMinWidth(50);
        btnRemove.setPrefWidth(50);
        btnRemove.setMaxWidth(50);
        btnRemove.setPrefHeight(42);
        btnRemove.setTooltip(new Tooltip("Retirer des favoris"));

        btnRemove.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText(null);
            confirm.setContentText("Voulez-vous retirer cette offre de vos favoris ?");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                boolean success = favoriteService.retirerFavori(CURRENT_CANDIDAT_ID, offre.getId());
                if (success) {
                    refreshList();
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Offre retirée des favoris");
                }
            }
        });

        // Bouton postuler
        Button btnPostuler = new Button("Postuler");
        btnPostuler.getStyleClass().add("c-btn");
        btnPostuler.setPrefWidth(140);
        btnPostuler.setPrefHeight(45);
        btnPostuler.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        btnPostuler.setOnAction(e -> openPostulerPopup(offre));

        footer.getChildren().addAll(salary, spacer, btnRemove, btnPostuler);

        card.getChildren().addAll(header, companyRow, description, meta, sep, footer);

        // Animation hover
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

    private HBox metaRowItem(String icon, String text) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 14px;");

        Label textLabel = new Label(text);
        textLabel.getStyleClass().add("c-mini-value");

        box.getChildren().addAll(iconLabel, textLabel);
        return box;
    }

    private void openPostulerPopup(OffreEmploi offre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/postuler.fxml"));
            Parent root = loader.load();

            PostulerPopupController controller = loader.getController();
            controller.setOffreInfo(offre.getId(), offre.getTitre());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Postuler - " + offre.getTitre());
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Pas besoin de rafraîchir la liste des favoris après postulation
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire de postulation");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void goBackToOffresList() {
        // Naviguer vers la liste des offres via le shell
        OffresShellController shell = OffresShellController.getInstance();
        if (shell != null) {
            shell.showOffresList();
        }
    }
}

