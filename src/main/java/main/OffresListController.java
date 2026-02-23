package main;

import entities.OffreEmploi;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import services.OffreEmploiService;
import services.FavoriteOffreService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
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
            allData.setAll(service.read());
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
        // Naviguer vers l'interface des favoris via le shell
        OffresShellController shell = OffresShellController.getInstance();
        if (shell != null) {
            shell.showFavorites();
        }
    }

    private void applyFilter() {
        String q = safe(txtSearch.getText()).toLowerCase().trim();

        // Filtres avancés
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
                // Filtre de recherche textuelle
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

                // Filtre type de contrat
                boolean matchesTypeContrat = "Tous".equals(typeContratFilter)
                        || safe(o.getTypeContrat()).equalsIgnoreCase(typeContratFilter);

                // Filtre niveau
                boolean matchesNiveau = "Tous".equals(niveauFilter)
                        || safe(o.getNiveauQualification()).equalsIgnoreCase(niveauFilter);

                // Filtre salaire minimum
                boolean matchesSalaireMin = finalSalaireMin == null || o.getSalaire() >= finalSalaireMin;

                // Filtre salaire maximum
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

    // ===================== CARD UI (screenshot style + attributes) =====================

    private VBox createOfferCard(OffreEmploi offre) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("offer-card", "card-like-screenshot");
        card.setMaxWidth(560);

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.TOP_LEFT);

        Label title = new Label(emptyAsDash(offre.getTitre()));
        title.getStyleClass().add("c-title");
        title.setMaxWidth(380); // Limite la largeur pour laisser place au badge
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);

        Label badge = new Label(emptyAsDash(offre.getTypeContrat()));
        badge.getStyleClass().add("c-badge");

        header.getChildren().addAll(title, badge);

        // Company
        HBox companyRow = metaRowItem("🏢", emptyAsDash(offre.getEntreprise()));
        companyRow.getStyleClass().add("c-company-row");

        // Description
        Label desc = new Label(trimTo(emptyAsDash(offre.getDescription()), 2500));
        desc.getStyleClass().add("c-desc");
        desc.setWrapText(true);

        // Meta: location + map button
        HBox meta = new HBox(18);
        meta.setAlignment(Pos.CENTER_LEFT);

        String expSmall = (offre.getDateExpiration() == null) ? "—" : dateFmt.format(offre.getDateExpiration());

        // Location with map button
        HBox locationBox = new HBox(10);
        locationBox.setAlignment(Pos.CENTER_LEFT);
        locationBox.getChildren().add(metaRowItem("📍", emptyAsDash(offre.getLocalisation())));

        // Add map button if location exists
        if (offre.getLocalisation() != null && !offre.getLocalisation().trim().isEmpty()) {
            Button btnMap = new Button("🗺️");
            btnMap.getStyleClass().add("btn-map-mini");
            btnMap.setTooltip(new Tooltip("Voir sur la carte"));
            btnMap.setOnAction(e -> openMapForLocation(offre.getLocalisation()));

            // Add hover animation
            btnMap.setOnMouseEntered(e -> {
                btnMap.setStyle("-fx-cursor: hand;");
            });

            locationBox.getChildren().add(btnMap);
        }

        meta.getChildren().add(locationBox);

        // Details grid (Expiration next to Contact)
        GridPane details = new GridPane();
        details.getStyleClass().add("c-details");
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



        // Divider
        Separator sep = new Separator();
        sep.getStyleClass().add("c-sep");

        // Footer: Salary + actions
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label salary = new Label(String.format("%.0f DT", offre.getSalaire()));
        salary.getStyleClass().add("c-salary");
        salary.setMinWidth(100);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Bouton Favori - Cœur blanc si non favori, cœur brisé rouge si favori
        Button btnFavorite = new Button();
        boolean isFavorite = favoriteOffreIds != null && favoriteOffreIds.contains(offre.getId());

        // Utiliser setText directement (plus simple et plus fiable)
        String emojiText = isFavorite ? "💔" : "🤍";
        btnFavorite.setText(emojiText);

        // Style inline pour forcer la taille de la police - réduite pour affichage complet
        btnFavorite.setStyle("-fx-font-size: 16px; -fx-font-family: 'Segoe UI Emoji', 'Apple Color Emoji', 'Noto Color Emoji';");

        // Classes CSS selon l'état
        btnFavorite.getStyleClass().clear();
        if (isFavorite) {
            btnFavorite.getStyleClass().addAll("btn-favorite", "btn-favorite-remove"); // Fond rouge
        } else {
            btnFavorite.getStyleClass().addAll("btn-favorite", "btn-favorite-inactive"); // Fond blanc, bordure mauve
        }

        btnFavorite.setMinWidth(50);
        btnFavorite.setPrefWidth(50);
        btnFavorite.setMaxWidth(50);
        btnFavorite.setPrefHeight(42);
        btnFavorite.setTooltip(new Tooltip(isFavorite ? "Retirer des favoris" : "Ajouter aux favoris"));

        btnFavorite.setOnAction(e -> {
            System.out.println("🔘 Clic sur bouton favori - Offre ID: " + offre.getId() + " - Candidat ID: " + CURRENT_CANDIDAT_ID);

            try {
                boolean success = favoriteService.toggleFavori(CURRENT_CANDIDAT_ID, offre.getId());
                System.out.println("✅ Toggle favori result: " + success);

                if (success) {
                    boolean newState = favoriteService.isFavorite(CURRENT_CANDIDAT_ID, offre.getId());
                    System.out.println("📊 Nouvel état favori: " + newState);

                    // Mettre à jour le texte selon le nouvel état
                    String newEmojiText = newState ? "💔" : "🤍";
                    btnFavorite.setText(newEmojiText);

                    // Mettre à jour les classes CSS selon le nouvel état
                    btnFavorite.getStyleClass().clear();
                    if (newState) {
                        btnFavorite.getStyleClass().addAll("btn-favorite", "btn-favorite-remove"); // Fond rouge
                    } else {
                        btnFavorite.getStyleClass().addAll("btn-favorite", "btn-favorite-inactive"); // Fond blanc
                    }
                    btnFavorite.setTooltip(new Tooltip(newState ? "Retirer des favoris" : "Ajouter aux favoris"));

                    // Mettre à jour le cache
                    if (newState) {
                        favoriteOffreIds.add(offre.getId());
                        System.out.println("➕ Ajouté au cache des favoris");
                    } else {
                        favoriteOffreIds.remove(offre.getId());
                        System.out.println("➖ Retiré du cache des favoris");
                    }

                    // Animation de feedback plus visible
                    ScaleTransition pulse = new ScaleTransition(Duration.millis(150), btnFavorite);
                    pulse.setToX(1.4);
                    pulse.setToY(1.4);
                    pulse.setAutoReverse(true);
                    pulse.setCycleCount(2);
                    pulse.play();

                    // Afficher un message de succès
                    String message = newState ? "Ajouté aux favoris ❤️" : "Retiré des favoris 💔";
                    showQuickNotification(message);
                } else {
                    System.err.println("❌ Échec du toggle favori");
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de modifier le favori");
                }
            } catch (Exception ex) {
                System.err.println("❌ Exception lors du toggle favori: " + ex.getMessage());
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Une erreur est survenue: " + ex.getMessage());
            }
        });

        Button btnPostuler = new Button("Postuler");
        btnPostuler.getStyleClass().add("c-btn");
        btnPostuler.setPrefWidth(140);
        btnPostuler.setPrefHeight(45);
        btnPostuler.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        btnPostuler.setOnAction(e -> {
            // Naviguer vers l'interface postuler via le shell
            OffresShellController shell = OffresShellController.getInstance();
            if (shell != null) {
                shell.showPostuler(offre.getId(), offre.getTitre());
            }
        });

        footer.getChildren().addAll(salary, spacer, btnFavorite, btnPostuler);

        card.getChildren().addAll(header, companyRow, desc, meta, details, sep, footer);

        // ===== HOVER ANIMATION =====
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
        Label i = new Label(icon);
        i.getStyleClass().add("c-meta-icon");

        Label t = new Label(text == null || text.isBlank() ? "—" : text);
        t.getStyleClass().add("c-meta");

        HBox row = new HBox(8, i, t);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox detailItem(String icon, String label, String value) {
        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);

        Label i = new Label(icon);
        i.getStyleClass().add("c-mini-icon");

        Label l = new Label(label);
        l.getStyleClass().add("c-mini-label");

        top.getChildren().addAll(i, l);

        Label v = new Label(value == null || value.isBlank() ? "—" : value);
        v.getStyleClass().add("c-mini-value");
        v.setWrapText(true);

        VBox box = new VBox(4, top, v);
        box.getStyleClass().add("c-detail-box");
        return box;
    }


    // ===================== DELETE =====================

    private void handleDelete(OffreEmploi offre) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer cette offre ?");
        confirm.setContentText("Titre : " + safe(offre.getTitre()) + "\nEntreprise : " + safe(offre.getEntreprise()));

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        try {
            service.supprimer(offre.getId());
            refreshList();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Offre supprimée.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression impossible :\n" + e.getMessage());
        }
    }

    // ===================== EDIT =====================

    private void openEditDialog(OffreEmploi offre) {
        Dialog<OffreEmploi> dialog = new Dialog<>();
        dialog.setTitle("Modifier l'offre");
        dialog.setHeaderText(null);

        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(18));

        TextField titre = new TextField(safe(offre.getTitre()));
        TextArea desc = new TextArea(safe(offre.getDescription()));
        desc.setPrefRowCount(4);

        TextField salaire = new TextField(String.valueOf((int) offre.getSalaire()));
        TextField localisation = new TextField(safe(offre.getLocalisation()));
        TextField entreprise = new TextField(safe(offre.getEntreprise()));
        TextField contact = new TextField(safe(offre.getContactRecruteur()));

        ComboBox<String> typeContrat = new ComboBox<>();
        typeContrat.getItems().setAll("CDI", "CDD", "Stage", "Freelance", "Alternance");
        typeContrat.setValue(safe(offre.getTypeContrat()).isEmpty() ? null : offre.getTypeContrat());

        DatePicker expDate = new DatePicker(
                offre.getDateExpiration() == null ? LocalDate.now().plusDays(7) : offre.getDateExpiration().toLocalDate()
        );

        grid.addRow(0, new Label("Titre"), titre);
        grid.addRow(1, new Label("Type contrat"), typeContrat);
        grid.addRow(2, new Label("Salaire (DT)"), salaire);
        grid.addRow(3, new Label("Localisation"), localisation);
        grid.addRow(4, new Label("Entreprise"), entreprise);
        grid.addRow(5, new Label("Contact (email)"), contact);
        grid.addRow(6, new Label("Expiration"), expDate);
        grid.addRow(7, new Label("Description"), desc);

        dialog.getDialogPane().setContent(grid);

        Node okBtn = dialog.getDialogPane().lookupButton(saveBtn);
        okBtn.setDisable(titre.getText().trim().isEmpty());
        titre.textProperty().addListener((obs, o, n) -> okBtn.setDisable(n.trim().isEmpty()));

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;

            if (titre.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Le titre est obligatoire.");
                return null;
            }
            if (desc.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "La description est obligatoire.");
                return null;
            }
            if (typeContrat.getValue() == null || typeContrat.getValue().trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Le type de contrat est obligatoire.");
                return null;
            }
            if (!isValidEmail(contact.getText().trim())) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Email invalide.");
                return null;
            }

            double sal;
            try {
                sal = Double.parseDouble(salaire.getText().trim());
                if (sal <= 0) throw new RuntimeException();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Salaire invalide (ex: 2500).");
                return null;
            }

            LocalDateTime newExp = expDate.getValue().atTime(
                    (offre.getDateExpiration() == null ? 23 : offre.getDateExpiration().getHour()),
                    (offre.getDateExpiration() == null ? 59 : offre.getDateExpiration().getMinute())
            );

            return new OffreEmploi(
                    offre.getId(),
                    titre.getText().trim(),
                    desc.getText().trim(),
                    sal,
                    typeContrat.getValue().trim(),
                    localisation.getText().trim(),
                    offre.getDatePublication(),
                    newExp,
                    offre.getNiveauQualification(),
                    offre.getExperienceRequise(),
                    offre.getCompetencesRequises(),
                    offre.getSecteurActivite(),
                    entreprise.getText().trim(),
                    contact.getText().trim()
            );
        });

        Optional<OffreEmploi> res = dialog.showAndWait();
        if (res.isEmpty()) return;

        try {
            service.modifier(res.get());
            refreshList();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Offre modifiée.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Modification impossible :\n" + e.getMessage());
        }
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
        if (comboFilterTypeContrat != null) {
            comboFilterTypeContrat.setValue("Tous");
        }
        if (comboFilterNiveau != null) {
            comboFilterNiveau.setValue("Tous");
        }
        if (txtFilterSalaireMin != null) {
            txtFilterSalaireMin.clear();
        }
        if (txtFilterSalaireMax != null) {
            txtFilterSalaireMax.clear();
        }
        applyFilter();
    }

    // ===================== HELPERS =====================

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    // ===================== OPEN MAP =====================

    /**
     * Ouvre une carte interactive avec OpenStreetMap + Leaflet.js
     * Utilise Nominatim API pour la géolocalisation (gratuit, pas de clé API)
     * La carte s'affiche dans une WebView JavaFX
     */
    private void openMapForLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Aucune localisation disponible pour cette offre.");
            return;
        }

        try {
            // Utiliser le MapService pour afficher la carte interactive
            services.MapService mapService = services.MapService.getInstance();
            mapService.showMap(location.trim(), "Localisation - " + location);

            System.out.println("✓ Carte interactive ouverte pour : " + location);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'ouverture de la carte: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                "Impossible d'ouvrir la carte interactive:\n" + e.getMessage());
        }
    }

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

    /**
     * Affiche une notification rapide à l'utilisateur
     */
    private void showQuickNotification(String message) {
        // Pour l'instant, on utilise juste un print, mais on pourrait améliorer avec un toast
        System.out.println("💬 Notification: " + message);
    }
}
