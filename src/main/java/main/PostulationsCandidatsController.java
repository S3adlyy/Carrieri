package main;

import entities.OffreEmploi;
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
import services.OffreEmploiService;
import services.PostulationService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
            // Filtrer les postulations
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
        lblStatus.setText(totalPostulations + " postulations (" + statut + ") "  );
    }

    private void refreshData() {
        try {
            allData.setAll(postulationService.read());

            // Grouper les postulations par candidat
            postulationsByCandidat.clear();
            for (Postulation p : allData) {
                postulationsByCandidat
                    .computeIfAbsent(p.getCandidatId(), k -> new ArrayList<>())
                    .add(p);
            }

            renderAllCards();

            int totalCandidats = postulationsByCandidat.size();
            int totalPostulations = allData.size();
            lblStatus.setText(totalPostulations + " postulations pour candidat:" + totalCandidats);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
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

        // Title: Postulation ID uniquement
        Label title = new Label("Postulation ID: " + p.getId());
        title.getStyleClass().add("offer-title");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: 700;");


        // Récupérer l'offre pour afficher titre et entreprise
        try {
            OffreEmploi offre = offreService.findById(p.getOffreId());

            if (offre != null) {
                // Titre de l'offre + icon
                HBox offreRow = new HBox(10);
                offreRow.setAlignment(Pos.CENTER);

                Label offreIcon = new Label("📄");
                offreIcon.setStyle("-fx-font-size: 20; -fx-text-fill: #7c3aed;");

                Label offreLabel = new Label(offre.getTitre());
                offreLabel.getStyleClass().add("offer-description");
                offreLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 15;");

                offreRow.getChildren().addAll(offreIcon, offreLabel);

                // Entreprise + icon
                HBox entrepriseRow = new HBox(10);
                entrepriseRow.setAlignment(Pos.CENTER);

                Label entrepriseIcon = new Label("🏢");
                entrepriseIcon.setStyle("-fx-font-size: 20; -fx-text-fill: #4c1d95;");

                Label entrepriseLabel = new Label(offre.getEntreprise());
                entrepriseLabel.getStyleClass().add("offer-info");
                entrepriseLabel.setStyle("-fx-font-weight: 600;");

                entrepriseRow.getChildren().addAll(entrepriseIcon, entrepriseLabel);

                // Date + icon
                HBox dateRow = new HBox(10);
                dateRow.setAlignment(Pos.CENTER);

                Label dateIcon = new Label("📅");
                dateIcon.setStyle("-fx-font-size: 20; -fx-text-fill: #6b7280;");

                Label dateLabel = new Label(dateFmt.format(p.getDatePostulation()));
                dateLabel.getStyleClass().add("offer-info");

                dateRow.getChildren().addAll(dateIcon, dateLabel);

                // Statut (NON MODIFIABLE - juste un Label)
                HBox statutRow = new HBox(12);
                statutRow.setAlignment(Pos.CENTER);

                Label statutIcon = new Label("🔖");
                statutIcon.setStyle("-fx-font-size: 20; -fx-text-fill: #7c3aed;");

                Label statutLabel = new Label(p.getStatut());
                statutLabel.setStyle(
                    "-fx-background-color: " + getStatutColor(p.getStatut()) + "; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 13; " +
                    "-fx-font-weight: 700; " +
                    "-fx-padding: 8 16; " +
                    "-fx-background-radius: 12;"
                );

                statutRow.getChildren().addAll(statutIcon, statutLabel);

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

                // Actions: delete button + details button
                HBox actions = new HBox(20);
                actions.setAlignment(Pos.CENTER);
                actions.getStyleClass().add("offer-actions");

                Button btnDetails = new Button();
                btnDetails.getStyleClass().add("btn-icon-edit");
                btnDetails.setGraphic(new Label("ℹ️"));
                btnDetails.setOnAction(e -> showOffreDetails(offre));

                Button btnDelete = new Button();
                btnDelete.getStyleClass().add("btn-icon-delete");
                btnDelete.setGraphic(new Label("🗑"));
                btnDelete.setOnAction(e -> handleDelete(p));

                actions.getChildren().addAll(btnDetails, btnDelete);

                // Assemble card
                card.getChildren().addAll(
                    title,
                    offreRow,
                    entrepriseRow,
                    dateRow,
                    statutRow,
                    motivationRow,
                    actions
                );

            } else {
                // Si l'offre n'existe plus
                Label errorLabel = new Label("⚠ Offre introuvable (ID: " + p.getOffreId() + ")");
                errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 600;");
                card.getChildren().addAll(title, errorLabel);
            }

        } catch (SQLException e) {
            Label errorLabel = new Label("⚠ Erreur lors du chargement de l'offre");
            errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 600;");
            card.getChildren().addAll(title, errorLabel);
        }

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
        // Créer un dialog personnalisé
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails de l'Offre");
        dialog.setHeaderText(null);

        // Créer le contenu du dialog
        VBox content = new VBox(16);
        content.setPadding(new javafx.geometry.Insets(20));
        content.setStyle("-fx-background-color: #faf7ff;");
        content.setMaxWidth(600);

        // Titre de l'offre
        Label titleLabel = new Label(offre.getTitre());
        titleLabel.setStyle(
            "-fx-font-size: 24; " +
            "-fx-font-weight: 800; " +
            "-fx-text-fill: #1e1133;"
        );

        // Entreprise
        HBox entrepriseBox = createDetailRow("🏢 Entreprise", offre.getEntreprise());

        // Type de contrat badge
        Label typeContratLabel = new Label(offre.getTypeContrat());
        typeContratLabel.setStyle(
            "-fx-background-color: #f3e8ff; " +
            "-fx-text-fill: #7c3aed; " +
            "-fx-font-size: 14; " +
            "-fx-font-weight: 700; " +
            "-fx-padding: 8 16; " +
            "-fx-background-radius: 12;"
        );

        // Salaire
        HBox salaireBox = createDetailRow("💰 Salaire", offre.getSalaire() + " TND");

        // Localisation
        HBox localisationBox = createDetailRow("📍 Localisation", offre.getLocalisation());

        // Description
        VBox descBox = new VBox(8);
        Label descTitle = new Label("📄 Description");
        descTitle.setStyle("-fx-font-size: 15; -fx-font-weight: 700; -fx-text-fill: #4c1d95;");

        Label descContent = new Label(offre.getDescription());
        descContent.setWrapText(true);
        descContent.setStyle("-fx-font-size: 14; -fx-text-fill: #374151; -fx-line-spacing: 4;");

        descBox.getChildren().addAll(descTitle, descContent);

        // Dates
        HBox datePublicationBox = createDetailRow("📅 Date de publication",
            offre.getDatePublication() != null ? dateFmt.format(offre.getDatePublication()) : "N/A");
        HBox dateExpirationBox = createDetailRow("⏰ Date d'expiration",
            offre.getDateExpiration() != null ? dateFmt.format(offre.getDateExpiration()) : "N/A");

        // Qualification
        HBox qualificationBox = createDetailRow("🎓 Niveau de qualification", offre.getNiveauQualification());

        // Expérience
        HBox experienceBox = createDetailRow("💼 Expérience requise", offre.getExperienceRequise());

        // Compétences
        VBox competencesBox = new VBox(8);
        Label compTitle = new Label("⚡ Compétences requises");
        compTitle.setStyle("-fx-font-size: 15; -fx-font-weight: 700; -fx-text-fill: #4c1d95;");

        Label compContent = new Label(offre.getCompetencesRequises());
        compContent.setWrapText(true);
        compContent.setStyle("-fx-font-size: 14; -fx-text-fill: #374151;");

        competencesBox.getChildren().addAll(compTitle, compContent);

        // Secteur d'activité
        HBox secteurBox = createDetailRow("🏭 Secteur d'activité", offre.getSecteurActivite());

        // Contact recruteur
        HBox contactBox = createDetailRow("📧 Contact recruteur", offre.getContactRecruteur());

        // Séparateur
        javafx.scene.control.Separator separator1 = new javafx.scene.control.Separator();
        separator1.setStyle("-fx-opacity: 0.3;");

        javafx.scene.control.Separator separator2 = new javafx.scene.control.Separator();
        separator2.setStyle("-fx-opacity: 0.3;");

        javafx.scene.control.Separator separator3 = new javafx.scene.control.Separator();
        separator3.setStyle("-fx-opacity: 0.3;");

        // Assembler tout le contenu
        content.getChildren().addAll(
            titleLabel,
            typeContratLabel,
            entrepriseBox,
            salaireBox,
            localisationBox,
            separator1,
            descBox,
            separator2,
            datePublicationBox,
            dateExpirationBox,
            qualificationBox,
            experienceBox,
            separator3,
            competencesBox,
            secteurBox,
            contactBox
        );

        // ScrollPane pour le contenu
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setPrefHeight(500);

        dialog.getDialogPane().setContent(scrollPane);

        // Bouton fermer
        ButtonType closeButton = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);

        // Styliser le dialog
        dialog.getDialogPane().setStyle(
            "-fx-background-color: #faf7ff; " +
            "-fx-border-color: #e0d4f5; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 20; " +
            "-fx-background-radius: 20;"
        );

        dialog.showAndWait();
    }

    private HBox createDetailRow(String label, String value) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label labelNode = new Label(label);
        labelNode.setStyle(
            "-fx-font-size: 14; " +
            "-fx-font-weight: 700; " +
            "-fx-text-fill: #4c1d95; " +
            "-fx-min-width: 200;"
        );

        Label valueNode = new Label(value);
        valueNode.setStyle(
            "-fx-font-size: 14; " +
            "-fx-text-fill: #374151; " +
            "-fx-wrap-text: true;"
        );
        valueNode.setWrapText(true);

        row.getChildren().addAll(labelNode, valueNode);
        return row;
    }

    private void handleDelete(Postulation p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer suppression");
        confirm.setHeaderText("Supprimer cette postulation ?");
        confirm.setContentText("ID: " + p.getId() + " pour le candidat " + p.getCandidatId());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                postulationService.supprimer(p.getId());
                refreshData();
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

