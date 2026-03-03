package com.example.guser.controllers.goffre;

import com.example.guser.controllers.guser.AppNavController;
import entities.goffre.OffreEmploi;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import services.goffre.FavoriteOffreService;
import services.goffre.OffreEmploiService;
import utils.goffre.StyledAlert;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class OffresListController {

    @FXML private FlowPane flowOffers;
    @FXML private TextField txtSearch;
    @FXML private Label lblStatus;
    @FXML private Button btnFilter;
    @FXML private VBox filterPanel;
    @FXML private ComboBox<String> comboFilterTypeContrat;
    @FXML private ComboBox<String> comboFilterNiveau;
    @FXML private TextField txtFilterSalaireMin;
    @FXML private TextField txtFilterSalaireMax;
    @FXML private Button btnApplyFilters;
    @FXML private Button btnResetFilters;

    private final OffreEmploiService service = new OffreEmploiService();
    private final FavoriteOffreService favoriteService = new FavoriteOffreService();
    private final ObservableList<OffreEmploi> allData = FXCollections.observableArrayList();
    private final ObservableList<OffreEmploi> filtered = FXCollections.observableArrayList();

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private boolean filterPanelVisible = false;

    // TODO: Remplacer par l'ID du candidat connecté (session utilisateur)
    private final int CURRENT_CANDIDAT_ID = 1;

    // Cache des IDs favoris pour optimiser l'affichage
    private Set<Integer> favoriteOffreIds;

    @FXML
    public void initialize() {
        txtSearch.textProperty().addListener((obs, o, n) -> applyFilter());

        // Initialiser les ComboBox de filtre
        if (comboFilterTypeContrat != null) {
            comboFilterTypeContrat.getItems().addAll("Tous", "CDI", "CDD", "Stage", "Freelance", "Alternance");
            comboFilterTypeContrat.setValue("Tous");
        }

        if (comboFilterNiveau != null) {
            comboFilterNiveau.getItems().addAll("Tous", "Bac", "Bac+2", "Bac+3 (Licence)", "Bac+5 (Master/Ingenieur)", "Doctorat");
            comboFilterNiveau.setValue("Tous");
        }

        // Cacher le panneau de filtres au démarrage
        if (filterPanel != null) {
            filterPanel.setVisible(false);
            filterPanel.setManaged(false);
        }

        refreshList();
    }

    public void refreshList() {
        System.out.println("🔄 Rafraîchissement de la liste des offres...");
        try {
            allData.setAll(service.readCandidates());
            System.out.println("📋 " + allData.size() + " offres chargées");

            // Charger les IDs favoris pour optimiser l'affichage
            System.out.println("❤️ Chargement des favoris pour le candidat ID: " + CURRENT_CANDIDAT_ID);
            favoriteOffreIds = favoriteService.getFavoriteOffreIds(CURRENT_CANDIDAT_ID);
            System.out.println("✅ " + favoriteOffreIds.size() + " favoris trouvés: " + favoriteOffreIds);

            applyFilter();
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors du chargement des offres: " + e.getMessage());
            e.printStackTrace();
            allData.clear();
            filtered.clear();
            renderEmpty("Impossible de charger les offres :\n" + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Erreur inattendue: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void showFavorites() {
        AppNavController shell = AppNavController.getInstance();
        if (shell != null) {
            shell.showFavorites();
        }
    }

    private void applyFilter() {
        String q = safe(txtSearch.getText()).toLowerCase().trim();

        String typeContratFilter = (comboFilterTypeContrat != null && comboFilterTypeContrat.getValue() != null)
                ? comboFilterTypeContrat.getValue() : "Tous";
        String niveauFilter = (comboFilterNiveau != null && comboFilterNiveau.getValue() != null)
                ? comboFilterNiveau.getValue() : "Tous";

        Double salaireMin = null;
        Double salaireMax = null;

        if (txtFilterSalaireMin != null && !safe(txtFilterSalaireMin.getText()).isEmpty()) {
            try {
                salaireMin = Double.parseDouble(txtFilterSalaireMin.getText().trim());
            } catch (NumberFormatException e) {
                // Ignorer si invalide
            }
        }

        if (txtFilterSalaireMax != null && !safe(txtFilterSalaireMax.getText()).isEmpty()) {
            try {
                salaireMax = Double.parseDouble(txtFilterSalaireMax.getText().trim());
            } catch (NumberFormatException e) {
                // Ignorer si invalide
            }
        }

        final Double finalSalaireMin = salaireMin;
        final Double finalSalaireMax = salaireMax;

        if (q.isEmpty() && "Tous".equals(typeContratFilter) && "Tous".equals(niveauFilter)
                && finalSalaireMin == null && finalSalaireMax == null) {
            filtered.setAll(allData);
        } else {
            filtered.setAll(allData.filtered(o -> {
                boolean matchesSearch = q.isEmpty() || (
                        safe(o.getTitre()).toLowerCase().contains(q) ||
                                safe(o.getEntreprise()).toLowerCase().contains(q) ||
                                safe(o.getLocalisation()).toLowerCase().contains(q) ||
                                safe(o.getSecteurActivite()).toLowerCase().contains(q) ||
                                safe(o.getCompetencesRequises()).toLowerCase().contains(q) ||
                                safe(o.getDescription()).toLowerCase().contains(q) ||
                                safe(o.getTypeContrat()).toLowerCase().contains(q) ||
                                safe(o.getNiveauQualification()).toLowerCase().contains(q) ||
                                safe(o.getExperienceRequise()).toLowerCase().contains(q) ||
                                safe(o.getContactRecruteur()).toLowerCase().contains(q)
                );

                boolean matchesTypeContrat = "Tous".equals(typeContratFilter)
                        || safe(o.getTypeContrat()).equalsIgnoreCase(typeContratFilter);

                boolean matchesNiveau = "Tous".equals(niveauFilter)
                        || safe(o.getNiveauQualification()).equalsIgnoreCase(niveauFilter);

                boolean matchesSalaireMin = finalSalaireMin == null || o.getSalaire() >= finalSalaireMin;
                boolean matchesSalaireMax = finalSalaireMax == null || o.getSalaire() <= finalSalaireMax;

                return matchesSearch && matchesTypeContrat && matchesNiveau
                        && matchesSalaireMin && matchesSalaireMax;
            }));
        }

        lblStatus.setText(filtered.size() + " offres trouvées");
        renderCards();
    }

    private void renderCards() {
        flowOffers.getChildren().clear();

        if (filtered.isEmpty()) {
            renderEmpty("Aucune offre à afficher");
            return;
        }

        for (OffreEmploi offre : filtered) {
            VBox card = createOfferCard(offre);
            flowOffers.getChildren().add(card);

            FadeTransition ft = new FadeTransition(Duration.millis(350), card);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }

    private void renderEmpty(String message) {
        flowOffers.getChildren().clear();

        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));

        Label t = new Label(message);
        t.getStyleClass().add("offer-info");

        box.getChildren().add(t);
        flowOffers.getChildren().add(box);

        lblStatus.setText("0 offres trouvées");
    }

    private VBox createOfferCard(OffreEmploi offre) {
        VBox card = new VBox(10);
        card.getStyleClass().add("offer-card");
        card.setMaxWidth(560);

        // En-tête
        HBox header = new HBox(12);
        header.setAlignment(Pos.TOP_LEFT);

        Label title = new Label(emptyAsDash(offre.getTitre()));
        title.getStyleClass().add("offer-title");
        title.setMaxWidth(380);
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);

        // Badge type de contrat avec couleur
        Label badge = new Label(emptyAsDash(offre.getTypeContrat()));
        badge.getStyleClass().add("offer-contract-badge");
        String type = offre.getTypeContrat() != null ? offre.getTypeContrat() : "";
        switch (type) {
            case "CDI":
                badge.getStyleClass().add("badge-cdi");
                break;
            case "CDD":
                badge.getStyleClass().add("badge-cdd");
                break;
            case "Stage":
                badge.getStyleClass().add("badge-stage");
                break;
            case "Freelance":
                badge.getStyleClass().add("badge-freelance");
                break;
            case "Alternance":
                badge.getStyleClass().add("badge-alternance");
                break;
        }

        header.getChildren().addAll(title, badge);

        // Entreprise
        HBox companyRow = metaRowItem("🏢", emptyAsDash(offre.getEntreprise()));
        companyRow.getStyleClass().add("offer-company-row");

        // Description
        Label desc = new Label(trimTo(emptyAsDash(offre.getDescription()), 2500));
        desc.getStyleClass().add("offer-description");
        desc.setWrapText(true);

        // Métadonnées (localisation + date expiration)
        HBox meta = new HBox(18);
        meta.setAlignment(Pos.CENTER_LEFT);

        String expSmall = (offre.getDateExpiration() == null) ? "—" : dateFmt.format(offre.getDateExpiration());

        HBox locationBox = new HBox(10);
        locationBox.setAlignment(Pos.CENTER_LEFT);
        locationBox.getChildren().add(metaRowItem("📍", emptyAsDash(offre.getLocalisation())));

        if (offre.getLocalisation() != null && !offre.getLocalisation().trim().isEmpty()) {
            Button btnMap = new Button("🗺️");
            btnMap.getStyleClass().add("btn-map-mini");
            btnMap.setTooltip(new Tooltip("Voir sur la carte"));
            btnMap.setOnAction(e -> openMapForLocation(offre.getLocalisation()));
            locationBox.getChildren().add(btnMap);
        }

        meta.getChildren().add(locationBox);

        // Grille des détails
        GridPane details = new GridPane();
        details.getStyleClass().add("offer-details");
        details.setHgap(18);
        details.setVgap(10);

        String pub = (offre.getDatePublication() == null) ? "—" : dateFmt.format(offre.getDatePublication());
        String expDetail = (offre.getDateExpiration() == null) ? "—" : dateFmt.format(offre.getDateExpiration());

        details.add(detailItem("🎓", "Niveau", emptyAsDash(offre.getNiveauQualification())), 0, 0);
        details.add(detailItem("⏳", "Expérience", emptyAsDash(offre.getExperienceRequise())), 1, 0);
        details.add(detailItem("🏷️", "Secteur", emptyAsDash(offre.getSecteurActivite())), 0, 1);
        details.add(detailItem("🧩", "Compétences", emptyAsDash(offre.getCompetencesRequises())), 1, 1);
        details.add(detailItem("✉️", "Contact", emptyAsDash(offre.getContactRecruteur())), 0, 2);
        details.add(detailItem("🕒", "Expiration", expDetail), 1, 2);

        // Séparateur
        Separator sep = new Separator();
        sep.getStyleClass().add("offer-sep");

        // Pied de page : salaire + actions
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label salary = new Label(String.format("%.0f DT", offre.getSalaire()));
        salary.getStyleClass().add("offer-salary");
        salary.setMinWidth(100);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Bouton favori
        Button btnFavorite = new Button();
        boolean isFavorite = favoriteOffreIds != null && favoriteOffreIds.contains(offre.getId());
        String emojiText = isFavorite ? "💔" : "🤍";
        btnFavorite.setText(emojiText);
        btnFavorite.setStyle("-fx-font-size: 16px; -fx-font-family: 'Segoe UI Emoji', 'Apple Color Emoji', 'Noto Color Emoji';");
        btnFavorite.getStyleClass().clear();
        if (isFavorite) {
            btnFavorite.getStyleClass().addAll("btn-favorite", "btn-favorite-remove");
        } else {
            btnFavorite.getStyleClass().addAll("btn-favorite", "btn-favorite-inactive");
        }
        btnFavorite.setMinWidth(50);
        btnFavorite.setPrefWidth(50);
        btnFavorite.setPrefHeight(42);
        btnFavorite.setTooltip(new Tooltip(isFavorite ? "Retirer des favoris" : "Ajouter aux favoris"));
        btnFavorite.setOnAction(e -> toggleFavorite(offre, btnFavorite));

        // Bouton postuler
        Button btnPostuler = new Button("Postuler");
        btnPostuler.getStyleClass().add("btn-postuler");
        btnPostuler.setPrefWidth(140);
        btnPostuler.setPrefHeight(45);
        btnPostuler.setOnAction(e -> {
            AppNavController shell = AppNavController.getInstance();
            if (shell != null) {
                shell.showPostuler(offre.getId(), offre.getTitre());
            }
        });

        footer.getChildren().addAll(salary, spacer, btnFavorite, btnPostuler);

        card.getChildren().addAll(header, companyRow, desc, meta, details, sep, footer);

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

    private void toggleFavorite(OffreEmploi offre, Button btnFavorite) {
        try {
            boolean success = favoriteService.toggleFavori(CURRENT_CANDIDAT_ID, offre.getId());
            if (success) {
                boolean newState = favoriteService.isFavorite(CURRENT_CANDIDAT_ID, offre.getId());
                String newEmojiText = newState ? "💔" : "🤍";
                btnFavorite.setText(newEmojiText);
                btnFavorite.getStyleClass().clear();
                if (newState) {
                    btnFavorite.getStyleClass().addAll("btn-favorite", "btn-favorite-remove");
                } else {
                    btnFavorite.getStyleClass().addAll("btn-favorite", "btn-favorite-inactive");
                }
                btnFavorite.setTooltip(new Tooltip(newState ? "Retirer des favoris" : "Ajouter aux favoris"));

                if (newState) {
                    favoriteOffreIds.add(offre.getId());
                } else {
                    favoriteOffreIds.remove(offre.getId());
                }

                ScaleTransition pulse = new ScaleTransition(Duration.millis(150), btnFavorite);
                pulse.setToX(1.4);
                pulse.setToY(1.4);
                pulse.setAutoReverse(true);
                pulse.setCycleCount(2);
                pulse.play();

                String message = newState ? "Ajouté aux favoris ❤️" : "Retiré des favoris 💔";
                showQuickNotification(message);
            } else {
                StyledAlert.showError("Erreur", "Impossible de modifier le favori");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            StyledAlert.showError("Erreur", "Une erreur est survenue: " + ex.getMessage());
        }
    }

    private HBox metaRowItem(String icon, String text) {
        Label i = new Label(icon);
        i.getStyleClass().add("offer-info");

        Label t = new Label(text);
        t.getStyleClass().add("offer-info");

        HBox row = new HBox(8, i, t);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox detailItem(String icon, String label, String value) {
        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);

        Label i = new Label(icon);
        i.getStyleClass().add("offer-info");

        Label l = new Label(label);
        l.getStyleClass().add("offer-info");

        top.getChildren().addAll(i, l);

        Label v = new Label(value);
        v.getStyleClass().add("offer-info");
        v.setWrapText(true);

        VBox box = new VBox(4, top, v);
        return box;
    }

    // ===================== FILTER PANEL TOGGLE =====================

    @FXML
    private void toggleFilterPanel() {
        filterPanelVisible = !filterPanelVisible;
        if (filterPanel != null) {
            filterPanel.setVisible(filterPanelVisible);
            filterPanel.setManaged(filterPanelVisible);
        }
    }

    @FXML
    private void handleApplyFilters() {
        applyFilter();
    }

    @FXML
    private void handleResetFilters() {
        if (comboFilterTypeContrat != null) comboFilterTypeContrat.setValue("Tous");
        if (comboFilterNiveau != null) comboFilterNiveau.setValue("Tous");
        if (txtFilterSalaireMin != null) txtFilterSalaireMin.clear();
        if (txtFilterSalaireMax != null) txtFilterSalaireMax.clear();
        applyFilter();
    }

    // ===================== OPEN MAP =====================

    private void openMapForLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            StyledAlert.showWarning("Attention", "Aucune localisation disponible pour cette offre.");
            return;
        }
        try {
            services.goffre.MapService.getInstance().showMap(location.trim(), "Localisation - " + location);
        } catch (Exception e) {
            e.printStackTrace();
            StyledAlert.showError("Erreur", "Impossible d'ouvrir la carte interactive:\n" + e.getMessage());
        }
    }

    // ===================== HELPERS =====================

    private String trimTo(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max).trim() + "...";
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    private String emptyAsDash(String s) {
        return (s == null || s.trim().isEmpty()) ? "—" : s.trim();
    }

    private void showQuickNotification(String message) {
        System.out.println("💬 Notification: " + message);
    }
}