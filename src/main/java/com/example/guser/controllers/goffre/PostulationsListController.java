package com.example.guser.controllers.goffre;

import com.example.guser.controllers.guser.AppNavController;
import entities.goffre.Postulation;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import services.goffre.PostulationService;
import utils.goffre.StyledAlert;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

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

    // État du filtre par offre
    private Integer filterOffreId = null;
    private String filterOffreTitre = null;

    @FXML
    public void initialize() {
        refreshTable();
    }

    @FXML
    private void handleBack() {
    AppNavController shellController = AppNavController.getInstance();
        if (shellController != null) {
            shellController.showOffresTable();
        }
    }

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

        mettreAJourLabelStatut();
        renderCards();
    }

    @FXML
    private void handleFilter() {
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

        if (btnFilter != null) {
            filterMenu.show(btnFilter, javafx.geometry.Side.BOTTOM, 0, 0);
        }
    }

    private void filterByStatut(String statut) {
        data.setAll(allData.filtered(p -> p.getStatut().equalsIgnoreCase(statut)));
        mettreAJourLabelStatut();
        renderCards();
    }

    private void refreshTable() {
        try {
            if (filterOffreId != null) {
                allData.setAll(service.afficherParOffre(filterOffreId));
            } else {
                allData.setAll(service.read());
            }

            data.setAll(allData);
            mettreAJourLabelStatut();
            renderCards();

        } catch (SQLException e) {
            StyledAlert.showError("Erreur", e.getMessage());
        }
    }

    private void mettreAJourLabelStatut() {
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
        card.getStyleClass().add("offer-card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setMaxWidth(360);

        // Titre : ID postulation
        Label title = new Label("Postulation ID: " + p.getId());
        title.getStyleClass().add("offer-title");

        // Offre
        HBox offreRow = createCenteredRow("📄", "Offre ID: " + p.getOffreId(), "#7c3aed", "offer-description");

        // Candidat
        HBox candidatRow = createCenteredRow("👤", "Candidat ID: " + p.getCandidatId(), "#4c1d95", "offer-info");

        // Date
        HBox dateRow = createCenteredRow("📅", dateFmt.format(p.getDatePostulation()), "#6b7280", "offer-info");

        // Statut avec ComboBox
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
                StyledAlert.showSuccess("Succès", "Statut mis à jour.");
                refreshTable();
            } catch (SQLException ex) {
                StyledAlert.showError("Erreur", ex.getMessage());
                comboStatut.setValue(p.getStatut()); // Revenir à l'ancienne valeur
            }
        });

        statutRow.getChildren().addAll(statutIcon, comboStatut);

        // Motivation
        HBox motivationRow = createMotivationRow(p.getMotivationCandidature());

        // CV
        HBox cvRow = createCvRow(p.getCvPath());

        // Actions : suppression
        HBox actions = new HBox(20);
        actions.setAlignment(Pos.CENTER);
        actions.getStyleClass().add("offer-actions");

        Button btnDelete = new Button();
        btnDelete.getStyleClass().addAll("icon-btn", "icon-btn-delete");
        btnDelete.setGraphic(new Label("🗑"));
        btnDelete.setOnAction(e -> handleDelete(p));

        actions.getChildren().add(btnDelete);

        card.getChildren().addAll(title, offreRow, candidatRow, dateRow, statutRow, motivationRow, cvRow, actions);

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

    private HBox createCenteredRow(String icon, String text, String iconColor, String styleClass) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 20; -fx-text-fill: " + iconColor + ";");

        Label textLabel = new Label(text != null ? text : "—");
        textLabel.getStyleClass().add(styleClass);
        textLabel.setWrapText(true);

        row.getChildren().addAll(iconLabel, textLabel);
        return row;
    }

    private HBox createMotivationRow(String motivation) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER);

        Label icon = new Label("📝");
        icon.setStyle("-fx-font-size: 20; -fx-text-fill: #374151;");

        String snippet = motivation.length() > 80 ? motivation.substring(0, 80) + "..." : motivation;
        Label motivLabel = new Label(snippet);
        motivLabel.getStyleClass().add("offer-description");
        motivLabel.setWrapText(true);

        row.getChildren().addAll(icon, motivLabel);
        return row;
    }

    private HBox createCvRow(String cvPath) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER);

        Label icon = new Label("📄");
        icon.setStyle("-fx-font-size: 20; -fx-text-fill: #7c3aed;");

        if (cvPath != null && !cvPath.isEmpty()) {
            Button btnOpenCV = new Button("Ouvrir CV");
            btnOpenCV.getStyleClass().addAll("button", "btn-primary");
            btnOpenCV.setPrefWidth(120);
            btnOpenCV.setOnAction(e -> handleOpenCV(cvPath));
            row.getChildren().addAll(icon, btnOpenCV);
        } else {
            Label noCv = new Label("Aucun CV");
            noCv.setStyle("-fx-text-fill: #9ca3af; -fx-font-style: italic;");
            row.getChildren().addAll(icon, noCv);
        }
        return row;
    }

    private void handleDelete(Postulation p) {
        boolean confirmed = StyledAlert.showConfirmation(
                "Confirmation de suppression",
                "Supprimer la postulation ID " + p.getId() + " ?"
        );
        if (confirmed) {
            try {
                service.supprimer(p.getId());
                refreshTable();
                StyledAlert.showSuccess("Succès", "Postulation supprimée.");
            } catch (SQLException e) {
                StyledAlert.showError("Erreur", e.getMessage());
            }
        }
    }

    private void handleOpenCV(String cvPath) {
        if (cvPath == null || cvPath.isEmpty()) {
            StyledAlert.showError("Erreur", "Aucun CV disponible pour cette postulation");
            return;
        }

        try {
            java.io.File cvFile = new java.io.File(cvPath);

            if (!cvFile.exists()) {
                StyledAlert.showError("Erreur",
                        "Fichier CV introuvable:\n" + cvPath +
                                "\n\nLe fichier a peut-être été supprimé ou déplacé.");
                return;
            }

            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
                    desktop.open(cvFile);
                    StyledAlert.showInfo("Ouverture du CV", "Ouverture de : " + cvFile.getName());
                } else {
                    StyledAlert.showError("Erreur",
                            "L'ouverture de fichiers n'est pas supportée sur ce système");
                }
            } else {
                StyledAlert.showError("Erreur",
                        "Desktop API non disponible sur ce système");
            }
        } catch (java.io.IOException e) {
            StyledAlert.showError("Erreur",
                    "Erreur lors de l'ouverture du CV:\n" + e.getMessage());
        } catch (Exception e) {
            StyledAlert.showError("Erreur",
                    "Erreur inattendue:\n" + e.getMessage());
        }
    }
}