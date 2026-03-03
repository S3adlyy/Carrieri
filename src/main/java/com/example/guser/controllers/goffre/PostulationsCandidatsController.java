package com.example.guser.controllers.goffre;

import entities.goffre.OffreEmploi;
import entities.goffre.Postulation;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import services.goffre.OffreEmploiService;
import services.goffre.PostulationService;
import session.SessionContext;
import utils.goffre.StyledAlert;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PostulationsCandidatsController {

    @FXML private FlowPane flowPostulations;
    @FXML private TextField txtSearch;
    @FXML private Label lblStatus;
    @FXML private Button btnFilter;

    private final PostulationService postulationService = new PostulationService();
    private final OffreEmploiService offreService = new OffreEmploiService();

    private final ObservableList<Postulation> allData = FXCollections.observableArrayList();
    private final Map<Integer, List<Postulation>> postulationsByCandidat = new HashMap<>();

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        refreshData();
    }

    @FXML
    private void handleSearch() {
        String search = txtSearch.getText().toLowerCase().trim();

        if (search.isEmpty()) {
            renderAllCards();
        } else {
            Map<Integer, List<Postulation>> filtered = new HashMap<>();

            for (Map.Entry<Integer, List<Postulation>> entry : postulationsByCandidat.entrySet()) {
                List<Postulation> matchingPostulations = entry.getValue().stream()
                        .filter(p -> {
                            try {
                                OffreEmploi offre = offreService.findById(p.getOffreId());
                                if (offre == null) return false;

                                return String.valueOf(p.getCandidatId()).contains(search) ||
                                        String.valueOf(p.getId()).contains(search) ||
                                        offre.getTitre().toLowerCase().contains(search) ||
                                        offre.getEntreprise().toLowerCase().contains(search) ||
                                        p.getStatut().toLowerCase().contains(search) ||
                                        p.getMotivationCandidature().toLowerCase().contains(search) ||
                                        p.getDatePostulation().toString().contains(search);
                            } catch (SQLException e) {
                                return false;
                            }
                        })
                        .collect(Collectors.toList());

                if (!matchingPostulations.isEmpty()) {
                    filtered.put(entry.getKey(), matchingPostulations);
                }
            }

            renderCards(filtered);

            int totalPostulations = filtered.values().stream().mapToInt(List::size).sum();
            lblStatus.setText(totalPostulations + " postulations trouvées ");
        }
    }

    @FXML
    private void handleFilter() {
        ContextMenu filterMenu = new ContextMenu();

        MenuItem allItem = new MenuItem("Toutes les Postulations");
        allItem.setOnAction(e -> {
            txtSearch.clear();
            refreshData();
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
        Map<Integer, List<Postulation>> filtered = new HashMap<>();

        for (Map.Entry<Integer, List<Postulation>> entry : postulationsByCandidat.entrySet()) {
            List<Postulation> matchingPostulations = entry.getValue().stream()
                    .filter(p -> p.getStatut().equalsIgnoreCase(statut))
                    .collect(Collectors.toList());

            if (!matchingPostulations.isEmpty()) {
                filtered.put(entry.getKey(), matchingPostulations);
            }
        }

        renderCards(filtered);

        int totalPostulations = filtered.values().stream().mapToInt(List::size).sum();
        lblStatus.setText(totalPostulations + " postulations (" + statut + ") ");
    }

    private void refreshData() {
        try {
            allData.setAll(postulationService.read());

            postulationsByCandidat.clear();
            for (Postulation p : allData) {
                postulationsByCandidat
                        .computeIfAbsent(p.getCandidatId(), k -> new ArrayList<>())
                        .add(p);
            }

            renderAllCards();

            int totalCandidats = postulationsByCandidat.size();
            int totalPostulations = allData.size();
            lblStatus.setText(totalPostulations + " postulations pour  " + SessionContext.getCurrentUser().getFirstname());

        } catch (SQLException e) {
            StyledAlert.showError("Erreur", e.getMessage());
        }
    }

    private void renderAllCards() {
        renderCards(postulationsByCandidat);
    }

    private void renderCards(Map<Integer, List<Postulation>> data) {
        flowPostulations.getChildren().clear();

        for (Map.Entry<Integer, List<Postulation>> entry : data.entrySet()) {
            int candidatId = entry.getKey();
            List<Postulation> postulations = entry.getValue();

            for (Postulation p : postulations) {
                VBox card = createPostulationCard(p, candidatId, postulations.size());
                flowPostulations.getChildren().add(card);

                FadeTransition fade = new FadeTransition(Duration.millis(700), card);
                fade.setFromValue(0.0);
                fade.setToValue(1.0);
                fade.play();
            }
        }
    }

    private VBox createPostulationCard(Postulation p, int candidatId, int totalForCandidat) {
        VBox card = new VBox(14);
        card.getStyleClass().add("offer-card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setMaxWidth(360);

        Label title = new Label("Postulation ID: " + p.getId());
        title.getStyleClass().add("offer-title");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: 700;");

        try {
            OffreEmploi offre = offreService.findById(p.getOffreId());

            if (offre != null) {
                // Titre de l'offre
                HBox offreRow = createCenteredRow("📄", offre.getTitre(), "#7c3aed", "offer-description");
                offreRow.getChildren().get(1).setStyle("-fx-font-weight: 700; -fx-font-size: 15;");

                // Entreprise
                HBox entrepriseRow = createCenteredRow("🏢", offre.getEntreprise(), "#4c1d95", "offer-info");

                // Date
                HBox dateRow = createCenteredRow("📅", dateFmt.format(p.getDatePostulation()), "#6b7280", "offer-info");

                // Statut
                HBox statutRow = createStatutRow(p.getStatut());

                // Motivation
                HBox motivationRow = createMotivationRow(p.getMotivationCandidature());

                // CV
                HBox cvRow = createCvRow(p.getCvPath());

                // Actions
                HBox actions = new HBox(20);
                actions.setAlignment(Pos.CENTER);
                actions.getStyleClass().add("offer-actions");

                Button btnDetails = new Button();
                btnDetails.getStyleClass().addAll("icon-btn", "icon-btn-edit");
                btnDetails.setGraphic(new Label("ℹ️"));
                btnDetails.setOnAction(e -> showOffreDetails(offre));

                Button btnDelete = new Button();
                btnDelete.getStyleClass().addAll("icon-btn", "icon-btn-delete");
                btnDelete.setGraphic(new Label("🗑"));
                btnDelete.setOnAction(e -> handleDelete(p));

                actions.getChildren().addAll(btnDetails, btnDelete);

                card.getChildren().addAll(
                        title,
                        offreRow,
                        entrepriseRow,
                        dateRow,
                        statutRow,
                        motivationRow,
                        cvRow,
                        actions
                );

            } else {
                Label errorLabel = new Label("⚠ Offre introuvable (ID: " + p.getOffreId() + ")");
                errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 600;");
                card.getChildren().addAll(title, errorLabel);
            }

        } catch (SQLException e) {
            Label errorLabel = new Label("⚠ Erreur lors du chargement de l'offre");
            errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 600;");
            card.getChildren().addAll(title, errorLabel);
        }

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

    private HBox createStatutRow(String statut) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER);

        Label icon = new Label("🔖");
        icon.setStyle("-fx-font-size: 20; -fx-text-fill: #7c3aed;");

        Label statutLabel = new Label(statut);
        statutLabel.setStyle(
                "-fx-background-color: " + getStatutColor(statut) + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 13; " +
                        "-fx-font-weight: 700; " +
                        "-fx-padding: 8 16; " +
                        "-fx-background-radius: 12;"
        );

        row.getChildren().addAll(icon, statutLabel);
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

    private String getStatutColor(String statut) {
        switch (statut.toLowerCase()) {
            case "en attente": return "#f59e0b";
            case "en cours": return "#3b82f6";
            case "acceptée": return "#10b981";
            case "refusée": return "#ef4444";
            default: return "#6b7280";
        }
    }

    private void showOffreDetails(OffreEmploi offre) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails de l'Offre");
        dialog.setHeaderText(null);

        DialogPane dialogPane = dialog.getDialogPane();
        // Appliquer les feuilles de style du thème
        dialogPane.getStylesheets().addAll(
                getClass().getResource("/com/example/guser/goffre/css/theme-unified.css").toExternalForm(),
                getClass().getResource("/com/example/guser/goffre/css/theme-dark.css").toExternalForm()
        );
        dialogPane.getStyleClass().add("glass-card");
        dialogPane.setPrefWidth(700);
        dialogPane.setPadding(new Insets(20));

        VBox content = new VBox(20);
        content.setStyle("-fx-background-color: transparent;");

        // Titre
        Label titleLabel = new Label(offre.getTitre());
        titleLabel.getStyleClass().add("header-title");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-padding: 0 0 10 0;");
        content.getChildren().add(titleLabel);

        // Badge type de contrat
        Label typeBadge = new Label(offre.getTypeContrat());
        typeBadge.getStyleClass().add("offer-contract-badge");
        HBox badgeBox = new HBox(typeBadge);
        badgeBox.setAlignment(Pos.CENTER_LEFT);
        content.getChildren().add(badgeBox);

        // Informations principales en cartes
        HBox mainInfo = new HBox(20);
        mainInfo.setAlignment(Pos.CENTER_LEFT);

        VBox entrepriseCard = createInfoCard("🏢 Entreprise", offre.getEntreprise());
        VBox salaireCard = createInfoCard("💰 Salaire", offre.getSalaire() + " TND");
        VBox localisationCard = createInfoCard("📍 Localisation", offre.getLocalisation());

        mainInfo.getChildren().addAll(entrepriseCard, salaireCard, localisationCard);
        content.getChildren().add(mainInfo);

        content.getChildren().add(new Separator());

        // Description
        VBox descCard = createTextCard("📄 Description", offre.getDescription());
        content.getChildren().add(descCard);

        content.getChildren().add(new Separator());

        // Dates
        HBox datesInfo = new HBox(20);
        datesInfo.setAlignment(Pos.CENTER_LEFT);

        String pubDate = offre.getDatePublication() != null ? dateFmt.format(offre.getDatePublication()) : "N/A";
        String expDate = offre.getDateExpiration() != null ? dateFmt.format(offre.getDateExpiration()) : "N/A";

        VBox pubCard = createInfoCard("📅 Date de publication", pubDate);
        VBox expCard = createInfoCard("⏰ Date d'expiration", expDate);

        datesInfo.getChildren().addAll(pubCard, expCard);
        content.getChildren().add(datesInfo);

        content.getChildren().add(new Separator());

        // Niveau et Expérience
        HBox niveauExpInfo = new HBox(20);
        niveauExpInfo.setAlignment(Pos.CENTER_LEFT);

        VBox niveauCard = createInfoCard("🎓 Niveau", offre.getNiveauQualification());
        VBox expCard2 = createInfoCard("💼 Expérience", offre.getExperienceRequise());

        niveauExpInfo.getChildren().addAll(niveauCard, expCard2);
        content.getChildren().add(niveauExpInfo);

        content.getChildren().add(new Separator());

        // Compétences
        VBox competencesCard = createTextCard("⚡ Compétences", offre.getCompetencesRequises());
        content.getChildren().add(competencesCard);

        content.getChildren().add(new Separator());

        // Secteur et Contact
        HBox secteurContactInfo = new HBox(20);
        secteurContactInfo.setAlignment(Pos.CENTER_LEFT);

        VBox secteurCard = createInfoCard("🏭 Secteur", offre.getSecteurActivite());
        VBox contactCard = createInfoCard("📧 Contact", offre.getContactRecruteur());

        secteurContactInfo.getChildren().addAll(secteurCard, contactCard);
        content.getChildren().add(secteurContactInfo);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setPrefHeight(600);

        dialogPane.setContent(scrollPane);

        ButtonType closeButton = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().add(closeButton);

        Node closeBtn = dialogPane.lookupButton(closeButton);
        if (closeBtn != null) {
            closeBtn.getStyleClass().addAll("button", "btn-secondary");
        }

        dialog.showAndWait();
    }

    /**
     * Crée une petite carte d'information avec un titre et une valeur.
     */
    private VBox createInfoCard(String title, String value) {
        VBox card = new VBox(8);
        card.getStyleClass().add("kpi-card-secondary");
        card.setPadding(new Insets(12));
        card.setPrefWidth(200);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #7c3aed;");

        Label valueLabel = new Label(value != null && !value.isEmpty() ? value : "—");
        valueLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #1f2937;");
        valueLabel.setWrapText(true);

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    /**
     * Crée une carte pour les textes longs (description, compétences).
     */
    private VBox createTextCard(String title, String text) {
        VBox card = new VBox(8);
        card.getStyleClass().add("kpi-card-secondary");
        card.setPadding(new Insets(12));

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #7c3aed;");

        Label textLabel = new Label(text != null && !text.isEmpty() ? text : "—");
        textLabel.setWrapText(true);
        textLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 400; -fx-text-fill: #374151;");

        card.getChildren().addAll(titleLabel, textLabel);
        return card;
    }


    private HBox createDetailRow(String label, String value) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-size: 14; -fx-font-weight: 700; -fx-text-fill: #4c1d95; -fx-min-width: 160;");

        Label valueNode = new Label(value != null ? value : "—");
        valueNode.setStyle("-fx-font-size: 14; -fx-text-fill: #374151; -fx-wrap-text: true;");
        valueNode.setWrapText(true);

        row.getChildren().addAll(labelNode, valueNode);
        return row;
    }

    private void handleDelete(Postulation p) {
        boolean confirmed = StyledAlert.showConfirmation(
                "Confirmation de suppression",
                "Supprimer la postulation ID " + p.getId() + " pour le candidat " + p.getCandidatId() + " ?"
        );
        if (confirmed) {
            try {
                postulationService.supprimer(p.getId());
                refreshData();
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