package com.example.guser.controllers.goffre;

import com.example.guser.controllers.guser.AppNavController;
import entities.goffre.FavoriteOffre;
import entities.goffre.OffreEmploi;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.util.Duration;
import services.goffre.FavoriteOffreService;
import session.SessionContext;
import utils.goffre.StyledAlert;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class FavoritesListController {

    @FXML private FlowPane flowFavorites;
    @FXML private Label lblStatus;
    @FXML private Label lblCount;
    @FXML private Button btnBack;

    private final FavoriteOffreService favoriteService = new FavoriteOffreService();
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // TODO: Remplacer par l'ID du candidat connecté
    private final int CURRENT_CANDIDAT_ID = SessionContext.getCurrentUser().getId();

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
        card.setMaxWidth(380);
        card.setPrefWidth(380);
        card.getStyleClass().add("offer-card");

        // En‑tête avec titre et badge
        HBox header = new HBox(12);
        header.setAlignment(Pos.TOP_LEFT);

        Label title = new Label(offre.getTitre() != null ? offre.getTitre() : "—");
        title.getStyleClass().add("offer-title");
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);

        Label badge = new Label(offre.getTypeContrat() != null ? offre.getTypeContrat() : "—");
        badge.getStyleClass().add("offer-contract-badge");

        header.getChildren().addAll(title, badge);

        // Entreprise
        HBox companyRow = metaRowItem("🏢", offre.getEntreprise() != null ? offre.getEntreprise() : "—");

        // Description
        String desc = offre.getDescription() != null ? offre.getDescription() : "";
        Label description = new Label(desc.length() > 150 ? desc.substring(0, 150) + "..." : desc);
        description.getStyleClass().add("offer-description");
        description.setWrapText(true);

        // Métadonnées
        HBox meta = new HBox(18);
        meta.setAlignment(Pos.CENTER_LEFT);
        meta.getChildren().addAll(
                metaRowItem("📍", offre.getLocalisation() != null ? offre.getLocalisation() : "—"),
                metaRowItem("⏰", "Ajouté le " + dateFmt.format(favorite.getDateAjout()))
        );

        // Séparateur
        Separator sep = new Separator();
        sep.getStyleClass().add("offer-sep");

        // Pied de page
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label salary = new Label(String.format("%.0f DT", offre.getSalaire()));
        salary.getStyleClass().add("offer-salary");
        salary.setMinWidth(100);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Bouton retirer (rouge)
        Button btnRemove = new Button("💔");
        btnRemove.getStyleClass().addAll("btn-favorite", "btn-favorite-remove");
        btnRemove.setMinWidth(50);
        btnRemove.setPrefWidth(50);
        btnRemove.setPrefHeight(42);
        btnRemove.setTooltip(new Tooltip("Retirer des favoris"));
        btnRemove.setOnAction(e -> handleRemoveFavorite(offre));

        // Bouton postuler
        Button btnPostuler = new Button("Postuler");
        btnPostuler.getStyleClass().add("btn-postuler");
        btnPostuler.setPrefWidth(160);
        btnPostuler.setPrefHeight(48);
        btnPostuler.setTooltip(new Tooltip("Postuler à cette offre"));
        btnPostuler.getStyleClass().addAll("button", "btn-postuler");
        btnPostuler.setOnAction(e -> openPostulerPopup(offre));

        footer.getChildren().addAll(salary, spacer, btnRemove, btnPostuler);

        card.getChildren().addAll(header, companyRow, description, meta, sep, footer);

        // Animation au survol
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
        textLabel.getStyleClass().add("offer-info");

        box.getChildren().addAll(iconLabel, textLabel);
        return box;
    }

    private void handleRemoveFavorite(OffreEmploi offre) {
        boolean confirmed = StyledAlert.showConfirmation(
                "Confirmation",
                "Voulez-vous retirer cette offre de vos favoris ?"
        );
        if (confirmed) {
            boolean success = favoriteService.retirerFavori(CURRENT_CANDIDAT_ID, offre.getId());
            if (success) {
                refreshList();
                StyledAlert.showSuccess("Succès", "Offre retirée des favoris");
            } else {
                StyledAlert.showError("Erreur", "Impossible de retirer l'offre");
            }
        }
    }

    private void openPostulerPopup(OffreEmploi offre) {
        AppNavController shell = AppNavController.getInstance();
        if (shell != null) {
            shell.showPostuler(offre.getId(), offre.getTitre());
        } else {
            StyledAlert.showError("Erreur", "Shell non disponible");
        }
    }

    @FXML
    public void goBackToOffresList() {
        AppNavController shell = AppNavController.getInstance();
        if (shell != null) {
            shell.showOffresList();
        } else {
            StyledAlert.showError("Erreur", "Shell non disponible");
        }
    }
}