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
        VBox card = new VBox(0);
        card.getStyleClass().add("postulation-card");
        card.setMaxWidth(380);
        card.setMinWidth(380);

        // === HEADER SECTION avec dégradé ===
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
            "-fx-background-color: linear-gradient(to right, #7c3aed, #a78bfa);" +
            "-fx-background-radius: 18 18 0 0;" +
            "-fx-padding: 20 24;"
        );

        // Icon postulation
        StackPane iconCircle = new StackPane();
        iconCircle.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.25);" +
            "-fx-background-radius: 50%;" +
            "-fx-min-width: 50; -fx-min-height: 50;" +
            "-fx-max-width: 50; -fx-max-height: 50;"
        );
        Label icon = new Label("📋");
        icon.setStyle("-fx-font-size: 28;");
        iconCircle.getChildren().add(icon);

        // Titre et ID
        VBox headerText = new VBox(4);
        HBox.setHgrow(headerText, Priority.ALWAYS);

        Label idLabel = new Label("Postulation #" + p.getId());
        idLabel.setStyle("-fx-font-size: 18; -fx-font-weight: 800; -fx-text-fill: white;");

        Label dateLabel = new Label("📅 " + dateFmt.format(p.getDatePostulation()));
        dateLabel.setStyle("-fx-font-size: 13; -fx-text-fill: rgba(255,255,255,0.9);");

        headerText.getChildren().addAll(idLabel, dateLabel);
        header.getChildren().addAll(iconCircle, headerText);

        // === BODY SECTION ===
        VBox body = new VBox(16);
        body.setStyle("-fx-padding: 24; -fx-background-color: white; -fx-background-radius: 0 0 18 18;");

        // Offre info avec style amélioré
        HBox offreBox = new HBox(12);
        offreBox.setAlignment(Pos.CENTER_LEFT);
        offreBox.setStyle(
            "-fx-background-color: #f9f5ff;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 14 16;" +
            "-fx-border-color: #e0d4f5;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1;"
        );

        Label offreIcon = new Label("💼");
        offreIcon.setStyle("-fx-font-size: 22;");

        VBox offreInfo = new VBox(3);
        HBox.setHgrow(offreInfo, Priority.ALWAYS);
        Label offreTitleLabel = new Label("Offre d'Emploi");
        offreTitleLabel.setStyle("-fx-font-size: 11; -fx-font-weight: 700; -fx-text-fill: #9ca3af;");
        Label offreIdLabel = new Label("ID: " + p.getOffreId());
        offreIdLabel.setStyle("-fx-font-size: 15; -fx-font-weight: 700; -fx-text-fill: #1e1133;");
        offreInfo.getChildren().addAll(offreTitleLabel, offreIdLabel);

        offreBox.getChildren().addAll(offreIcon, offreInfo);

        // Candidat info
        HBox candidatBox = new HBox(12);
        candidatBox.setAlignment(Pos.CENTER_LEFT);
        candidatBox.setStyle(
            "-fx-background-color: #faf5ff;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 14 16;" +
            "-fx-border-color: #e9d5ff;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1;"
        );

        Label candidatIcon = new Label("👤");
        candidatIcon.setStyle("-fx-font-size: 22;");

        VBox candidatInfo = new VBox(3);
        HBox.setHgrow(candidatInfo, Priority.ALWAYS);
        Label candidatTitleLabel = new Label("Candidat");
        candidatTitleLabel.setStyle("-fx-font-size: 11; -fx-font-weight: 700; -fx-text-fill: #9ca3af;");
        Label candidatIdLabel = new Label("ID: " + p.getCandidatId());
        candidatIdLabel.setStyle("-fx-font-size: 15; -fx-font-weight: 700; -fx-text-fill: #1e1133;");
        candidatInfo.getChildren().addAll(candidatTitleLabel, candidatIdLabel);

        candidatBox.getChildren().addAll(candidatIcon, candidatInfo);

        // Statut avec ComboBox stylé
        VBox statutBox = new VBox(8);
        Label statutTitle = new Label("Statut de la postulation");
        statutTitle.setStyle("-fx-font-size: 12; -fx-font-weight: 700; -fx-text-fill: #6b7280;");

        ComboBox<String> comboStatut = new ComboBox<>(FXCollections.observableArrayList(
            "En attente", "En cours", "Acceptée", "Refusée"
        ));
        comboStatut.setValue(p.getStatut());
        comboStatut.setMaxWidth(Double.MAX_VALUE);
        comboStatut.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: #d1c4e9;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-border-width: 2;" +
            "-fx-padding: 10 16;" +
            "-fx-font-size: 14;" +
            "-fx-font-weight: 600;"
        );

        comboStatut.setOnAction(e -> {
            String newStatut = comboStatut.getValue();
            try {
                service.changerStatut(p.getId(), newStatut);
                showAlert(Alert.AlertType.INFORMATION, "✅ Succès", "Statut mis à jour avec succès !");
                refreshTable();
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "❌ Erreur", ex.getMessage());
                comboStatut.setValue(p.getStatut());
            }
        });

        statutBox.getChildren().addAll(statutTitle, comboStatut);

        // Motivation avec style amélioré
        VBox motivationBox = new VBox(8);
        Label motivationTitle = new Label("💬 Lettre de motivation");
        motivationTitle.setStyle("-fx-font-size: 12; -fx-font-weight: 700; -fx-text-fill: #6b7280;");

        String motivSnippet = p.getMotivationCandidature().length() > 120
                ? p.getMotivationCandidature().substring(0, 120) + "..."
                : p.getMotivationCandidature();

        Label motivation = new Label(motivSnippet);
        motivation.setWrapText(true);
        motivation.setStyle(
            "-fx-font-size: 13;" +
            "-fx-text-fill: #374151;" +
            "-fx-line-spacing: 4;" +
            "-fx-padding: 12;" +
            "-fx-background-color: #fafafa;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #e5e7eb;" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1;"
        );

        motivationBox.getChildren().addAll(motivationTitle, motivation);

        // Separator
        Separator sep = new Separator();
        sep.setStyle("-fx-opacity: 0.3;");

        // Actions avec bouton supprimer stylé
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);
        actions.setStyle("-fx-padding: 8 0 0 0;");

        Button btnDelete = new Button("Supprimer");
        btnDelete.setGraphic(new Label("🗑"));
        btnDelete.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDelete, Priority.ALWAYS);
        btnDelete.setStyle(
            "-fx-background-color: #fef2f2;" +
            "-fx-text-fill: #dc2626;" +
            "-fx-font-size: 14;" +
            "-fx-font-weight: 700;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #fecaca;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1.5;" +
            "-fx-padding: 10 20;" +
            "-fx-cursor: hand;"
        );

        btnDelete.setOnMouseEntered(e -> btnDelete.setStyle(
            "-fx-background-color: #fee2e2;" +
            "-fx-text-fill: #dc2626;" +
            "-fx-font-size: 14;" +
            "-fx-font-weight: 700;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #fca5a5;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 2;" +
            "-fx-padding: 10 20;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(220,38,38,0.25), 8, 0, 0, 2);"
        ));

        btnDelete.setOnMouseExited(e -> btnDelete.setStyle(
            "-fx-background-color: #fef2f2;" +
            "-fx-text-fill: #dc2626;" +
            "-fx-font-size: 14;" +
            "-fx-font-weight: 700;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #fecaca;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1.5;" +
            "-fx-padding: 10 20;" +
            "-fx-cursor: hand;"
        ));

        btnDelete.setOnAction(e -> handleDelete(p));

        actions.getChildren().add(btnDelete);

        // Assembler le body
        body.getChildren().addAll(offreBox, candidatBox, statutBox, motivationBox, sep, actions);

        // Assembler la card
        card.getChildren().addAll(header, body);

        // Animation hover
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(200), card);
        scaleUp.setToX(1.03);
        scaleUp.setToY(1.03);

        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(200), card);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);

        card.setOnMouseEntered(e -> {
            scaleUp.playFromStart();
            card.setStyle(
                "-fx-effect: dropshadow(gaussian, rgba(124,58,237,0.3), 25, 0, 0, 10);"
            );
        });

        card.setOnMouseExited(e -> {
            scaleDown.playFromStart();
            card.setStyle("");
        });

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
