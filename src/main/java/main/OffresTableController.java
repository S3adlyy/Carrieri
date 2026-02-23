package main;

import entities.OffreEmploi;
import entities.Postulation;
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
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import services.OffreEmploiService;
import services.PostulationService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class OffresTableController {

    @FXML private TableView<OffreEmploi> tableOffres;

    @FXML private TableColumn<OffreEmploi, String> colTitre;
    @FXML private TableColumn<OffreEmploi, String> colDescription;
    @FXML private TableColumn<OffreEmploi, Double> colSalaire;
    @FXML private TableColumn<OffreEmploi, String> colType;
    @FXML private TableColumn<OffreEmploi, String> colLocalisation;
    @FXML private TableColumn<OffreEmploi, LocalDateTime> colPublication;
    @FXML private TableColumn<OffreEmploi, LocalDateTime> colExpiration;
    @FXML private TableColumn<OffreEmploi, String> colNiveau;
    @FXML private TableColumn<OffreEmploi, String> colExperience;
    @FXML private TableColumn<OffreEmploi, String> colCompetences;
    @FXML private TableColumn<OffreEmploi, String> colSecteur;
    @FXML private TableColumn<OffreEmploi, String> colEntreprise;
    @FXML private TableColumn<OffreEmploi, String> colContact;

    @FXML private TableColumn<OffreEmploi, Void> colActions;

    @FXML private TextField txtSearch;
    @FXML private Label lblStatus;

    private final OffreEmploiService offreService = new OffreEmploiService();
    private final PostulationService postService = new PostulationService();

    private final ObservableList<OffreEmploi> allData = FXCollections.observableArrayList();
    private final ObservableList<OffreEmploi> filtered = FXCollections.observableArrayList();

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {

        //  INLINE EDIT ENABLED
        tableOffres.setEditable(true);


        // ===== Bind columns =====
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colSalaire.setCellValueFactory(new PropertyValueFactory<>("salaire"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeContrat"));
        colLocalisation.setCellValueFactory(new PropertyValueFactory<>("localisation"));
        colPublication.setCellValueFactory(new PropertyValueFactory<>("datePublication"));
        colExpiration.setCellValueFactory(new PropertyValueFactory<>("dateExpiration"));
        colNiveau.setCellValueFactory(new PropertyValueFactory<>("niveauQualification"));
        colExperience.setCellValueFactory(new PropertyValueFactory<>("experienceRequise"));
        colCompetences.setCellValueFactory(new PropertyValueFactory<>("competencesRequises"));
        colSecteur.setCellValueFactory(new PropertyValueFactory<>("secteurActivite"));
        colEntreprise.setCellValueFactory(new PropertyValueFactory<>("entreprise"));
        colContact.setCellValueFactory(new PropertyValueFactory<>("contactRecruteur"));

        // Configure description column to show full text without wrapping
        colDescription.setCellFactory(col -> new TableCell<OffreEmploi, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    setWrapText(false);
                    setTooltip(new Tooltip(item));
                }
            }
        });

        // ===== Inline editing (cell factories + commit => DB update) =====
        enableInlineEditing();

        // ===== Actions (5 icons) =====
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnPosts = iconBtn("👥", "Voir postulations");
            private final Button btnStats = iconBtn("📊", "Statistiques");
            private final Button btnQR    = iconBtn("📱", "QR Code");
            private final Button btnEdit  = iconBtn("✏", "Modifier (dialog)");
            private final Button btnDel   = iconBtn("🗑", "Supprimer");

            private final HBox box = new HBox(12, btnPosts, btnStats, btnQR, btnEdit, btnDel);

            {
                box.setAlignment(Pos.CENTER);
                box.setPadding(new Insets(8, 16, 8, 16));

                btnPosts.getStyleClass().addAll("icon-btn", "icon-btn-neutral");
                btnStats.getStyleClass().addAll("icon-btn", "icon-btn-stats");
                btnQR.getStyleClass().addAll("icon-btn", "icon-btn-primary");
                btnEdit.getStyleClass().addAll("icon-btn", "icon-btn-edit");
                btnDel.getStyleClass().addAll("icon-btn", "icon-btn-delete");

                btnPosts.setOnAction(e -> {
                    OffreEmploi o = getTableView().getItems().get(getIndex());
                    goToPostulations(o);
                });

                btnStats.setOnAction(e -> {
                    OffreEmploi o = getTableView().getItems().get(getIndex());
                    showStatsPopup(o);
                });

                btnQR.setOnAction(e -> {
                    OffreEmploi o = getTableView().getItems().get(getIndex());
                    showQRCodePopup(o);
                });

                btnEdit.setOnAction(e -> {
                    OffreEmploi o = getTableView().getItems().get(getIndex());
                    openEditDialog(o);
                });

                btnDel.setOnAction(e -> {
                    OffreEmploi o = getTableView().getItems().get(getIndex());
                    handleDelete(o);
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        // Search
        txtSearch.textProperty().addListener((obs, o, n) -> applyFilter());

        loadData();
    }

    // ===================== INLINE EDITING (NO SETTERS) =====================

    private void enableInlineEditing() {

        // TITRE
        colTitre.setCellFactory(TextFieldTableCell.forTableColumn());
        colTitre.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());

            // Validation: titre non vide
            if (nv.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Titre obligatoire.");
                refreshRow(old);
                return;
            }

            // Validation: ne commence pas par un chiffre
            if (nv.matches("^\\d.*")) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Le titre ne peut pas commencer par un chiffre.");
                refreshRow(old);
                return;
            }

            // Validation: unicité du titre (si différent de l'ancien)
            if (!nv.equals(old.getTitre())) {
                try {
                    if (offreService.existsByTitre(nv)) {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Ce titre existe déjà. Veuillez choisir un titre différent.");
                        refreshRow(old);
                        return;
                    }
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la vérification du titre:\n" + e.getMessage());
                    refreshRow(old);
                    return;
                }
            }

            OffreEmploi updated = copy(old, nv, null, null, null, null, null, null, null, null, null, null, null, null);
            saveInline(old, updated, "Titre");
        });

        // DESCRIPTION
        colDescription.setCellFactory(TextFieldTableCell.forTableColumn());
        colDescription.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());

            // Validation: description non vide
            if (nv.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Description obligatoire.");
                refreshRow(old);
                return;
            }

            // Validation: ne commence pas par un chiffre
            if (nv.matches("^\\d.*")) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "La description ne peut pas commencer par un chiffre.");
                refreshRow(old);
                return;
            }

            OffreEmploi updated = copy(old, null, nv, null, null, null, null, null, null, null, null, null, null, null);
            saveInline(old, updated, "Description");
        });

        // SALAIRE
        colSalaire.setCellFactory(tc -> new TextFieldTableCell<>(new StringConverter<>() {
            @Override public String toString(Double object) {
                return object == null ? "" : String.format("%.0f", object);
            }
            @Override public Double fromString(String string) {
                try {
                    String s = safe(string);
                    if (s.isEmpty()) return null;
                    return Double.parseDouble(s);
                } catch (Exception e) {
                    return null;
                }
            }
        }));
        colSalaire.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            Double nv = ev.getNewValue();
            if (nv == null || nv <= 0) { showAlert(Alert.AlertType.ERROR, "Erreur", "Salaire invalide."); refreshRow(old); return; }
            OffreEmploi updated = copy(old, null, null, nv, null, null, null, null, null, null, null, null, null, null);
            saveInline(old, updated, "Salaire");
        });

        // TYPE CONTRAT (dropdown)
        colType.setCellFactory(ComboBoxTableCell.forTableColumn("CDI", "CDD", "Stage", "Freelance", "Alternance"));
        colType.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            if (nv.isEmpty()) { showAlert(Alert.AlertType.ERROR, "Erreur", "Type contrat obligatoire."); refreshRow(old); return; }
            OffreEmploi updated = copy(old, null, null, null, nv, null, null, null, null, null, null, null, null, null);
            saveInline(old, updated, "Type contrat");
        });

        // LOCALISATION
        colLocalisation.setCellFactory(TextFieldTableCell.forTableColumn());
        colLocalisation.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            if (nv.isEmpty()) { showAlert(Alert.AlertType.ERROR, "Erreur", "Localisation obligatoire."); refreshRow(old); return; }
            OffreEmploi updated = copy(old, null, null, null, null, nv, null, null, null, null, null, null, null, null);
            saveInline(old, updated, "Localisation");
        });

        // PUBLICATION (dd/MM/yyyy)
        colPublication.setCellFactory(tc -> new TextFieldTableCell<>(new StringConverter<>() {
            @Override public String toString(LocalDateTime object) {
                return object == null ? "" : dateFmt.format(object);
            }
            @Override public LocalDateTime fromString(String string) {
                try {
                    LocalDate d = LocalDate.parse(safe(string), dateFmt);
                    return d.atTime(0, 0);
                } catch (Exception e) {
                    return null;
                }
            }
        }));
        colPublication.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            LocalDateTime nv = ev.getNewValue();
            if (nv == null) { showAlert(Alert.AlertType.ERROR, "Erreur", "Format publication invalide (dd/MM/yyyy)."); refreshRow(old); return; }

            // publication <= expiration
            LocalDateTime exp = old.getDateExpiration();
            if (exp != null && nv.isAfter(exp)) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Publication doit être avant expiration.");
                refreshRow(old);
                return;
            }

            OffreEmploi updated = copy(old, null, null, null, null, null, nv, null, null, null, null, null, null, null);
            saveInline(old, updated, "Publication");
        });

        // EXPIRATION (dd/MM/yyyy)
        colExpiration.setCellFactory(tc -> new TextFieldTableCell<>(new StringConverter<>() {
            @Override public String toString(LocalDateTime object) {
                return object == null ? "" : dateFmt.format(object);
            }
            @Override public LocalDateTime fromString(String string) {
                try {
                    LocalDate d = LocalDate.parse(safe(string), dateFmt);
                    return d.atTime(23, 59);
                } catch (Exception e) {
                    return null;
                }
            }
        }));
        colExpiration.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            LocalDateTime nv = ev.getNewValue();
            if (nv == null) { showAlert(Alert.AlertType.ERROR, "Erreur", "Format expiration invalide (dd/MM/yyyy)."); refreshRow(old); return; }

            LocalDateTime pub = old.getDatePublication();
            if (pub != null && pub.isAfter(nv)) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Expiration doit être après publication.");
                refreshRow(old);
                return;
            }

            OffreEmploi updated = copy(old, null, null, null, null, null, null, nv, null, null, null, null, null, null);
            saveInline(old, updated, "Expiration");
        });

        // NIVEAU
        colNiveau.setCellFactory(TextFieldTableCell.forTableColumn());
        colNiveau.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            OffreEmploi updated = copy(old, null, null, null, null, null, null, null, nv, null, null, null, null, null);
            saveInline(old, updated, "Niveau");
        });

        // EXPERIENCE
        colExperience.setCellFactory(TextFieldTableCell.forTableColumn());
        colExperience.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            OffreEmploi updated = copy(old, null, null, null, null, null, null, null, null, nv, null, null, null, null);
            saveInline(old, updated, "Expérience");
        });

        // COMPETENCES
        colCompetences.setCellFactory(TextFieldTableCell.forTableColumn());
        colCompetences.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            OffreEmploi updated = copy(old, null, null, null, null, null, null, null, null, null, nv, null, null, null);
            saveInline(old, updated, "Compétences");
        });

        // SECTEUR
        colSecteur.setCellFactory(TextFieldTableCell.forTableColumn());
        colSecteur.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            OffreEmploi updated = copy(old, null, null, null, null, null, null, null, null, null, null, nv, null, null);
            saveInline(old, updated, "Secteur");
        });

        // ENTREPRISE
        colEntreprise.setCellFactory(TextFieldTableCell.forTableColumn());
        colEntreprise.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            if (nv.isEmpty()) { showAlert(Alert.AlertType.ERROR, "Erreur", "Entreprise obligatoire."); refreshRow(old); return; }
            OffreEmploi updated = copy(old, null, null, null, null, null, null, null, null, null, null, null, nv, null);
            saveInline(old, updated, "Entreprise");
        });

        // CONTACT
        colContact.setCellFactory(TextFieldTableCell.forTableColumn());
        colContact.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String email = safe(ev.getNewValue());
            if (!isValidEmail(email)) { showAlert(Alert.AlertType.ERROR, "Erreur", "Email invalide."); refreshRow(old); return; }
            OffreEmploi updated = copy(old, null, null, null, null, null, null, null, null, null, null, null, null, email);
            saveInline(old, updated, "Contact");
        });
    }

    private void saveInline(OffreEmploi oldOffre, OffreEmploi updated, String fieldName) {
        try {
            offreService.modifier(updated);

            // replace in lists so TableView reflects new object (no setters)
            replaceById(allData, updated);
            replaceById(filtered, updated);
            tableOffres.refresh();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Modification impossible (" + fieldName + ") :\n" + e.getMessage());
            loadData(); // rollback from DB
        }
    }

    private void refreshRow(OffreEmploi offre) {
        // revert UI to existing item values
        tableOffres.refresh();
    }

    private void replaceById(ObservableList<OffreEmploi> list, OffreEmploi updated) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() == updated.getId()) {
                list.set(i, updated);
                return;
            }
        }
    }

    /**
     * copy helper: any null parameter means keep old value
     */
    private OffreEmploi copy(
            OffreEmploi o,
            String titre,
            String description,
            Double salaire,
            String typeContrat,
            String localisation,
            LocalDateTime datePublication,
            LocalDateTime dateExpiration,
            String niveauQualification,
            String experienceRequise,
            String competencesRequises,
            String secteurActivite,
            String entreprise,
            String contactRecruteur
    ) {
        return new OffreEmploi(
                o.getId(),
                (titre != null) ? titre : o.getTitre(),
                (description != null) ? description : o.getDescription(),
                (salaire != null) ? salaire : o.getSalaire(),
                (typeContrat != null) ? typeContrat : o.getTypeContrat(),
                (localisation != null) ? localisation : o.getLocalisation(),
                (datePublication != null) ? datePublication : o.getDatePublication(),
                (dateExpiration != null) ? dateExpiration : o.getDateExpiration(),
                (niveauQualification != null) ? niveauQualification : o.getNiveauQualification(),
                (experienceRequise != null) ? experienceRequise : o.getExperienceRequise(),
                (competencesRequises != null) ? competencesRequises : o.getCompetencesRequises(),
                (secteurActivite != null) ? secteurActivite : o.getSecteurActivite(),
                (entreprise != null) ? entreprise : o.getEntreprise(),
                (contactRecruteur != null) ? contactRecruteur : o.getContactRecruteur()
        );
    }

    // ===================== DATA / FILTER =====================

    @FXML
    private void handleGoToAddOffre() {
        OffresShellController shell = OffresShellController.getInstance();
        if (shell != null) {
            shell.showOffreAdd();
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Shell non disponible.");
        }
    }

    private void loadData() {
        try {
            allData.setAll(offreService.read());
            applyFilter();
        } catch (SQLException e) {
            allData.clear();
            filtered.clear();
            tableOffres.setItems(filtered);
            lblStatus.setText("0 offres trouvées");
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement :\n" + e.getMessage());
        }
    }

    private void applyFilter() {
        String q = safe(txtSearch.getText()).toLowerCase().trim();

        if (q.isEmpty()) {
            filtered.setAll(allData);
        } else {
            filtered.setAll(allData.filtered(o ->
                    safe(o.getTitre()).toLowerCase().contains(q) ||
                            safe(o.getEntreprise()).toLowerCase().contains(q) ||
                            safe(o.getLocalisation()).toLowerCase().contains(q) ||
                            safe(o.getSecteurActivite()).toLowerCase().contains(q) ||
                            safe(o.getCompetencesRequises()).toLowerCase().contains(q) ||
                            safe(o.getNiveauQualification()).toLowerCase().contains(q) ||
                            safe(o.getExperienceRequise()).toLowerCase().contains(q) ||
                            safe(o.getContactRecruteur()).toLowerCase().contains(q)
            ));
        }

        tableOffres.setItems(filtered);
        lblStatus.setText(filtered.size() + " offres trouvées");
    }

    // ===================== ACTION: POSTULATIONS (FILTERED) =====================

    private void goToPostulations(OffreEmploi offre) {
        OffresShellController shell = OffresShellController.getInstance();
        if (shell != null) {
            shell.showPostulationsForOffre(offre.getId(), offre.getTitre());
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Shell non disponible.");
        }
    }

    // ===================== DELETE (FK SAFE) =====================

    private void handleDelete(OffreEmploi offre) {
        try {
            List<Postulation> posts = postService.afficherParOffre(offre.getId());

            if (!posts.isEmpty()) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirmation de suppression");
                confirm.setHeaderText(null);

                // Custom content with styled layout
                VBox content = new VBox(16);
                content.setPadding(new Insets(20));
                content.setStyle("-fx-background-color: white;");

                Label icon = new Label("⚠️");
                icon.setStyle("-fx-font-size: 48px;");

                Label title = new Label("Attention : Postulations existantes");
                title.setStyle(
                    "-fx-font-size: 18px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: #dc2626;"
                );

                Label message = new Label(
                    "Cette offre a déjà " + posts.size() + " postulation(s).\n\n" +
                    "Voulez-vous supprimer les postulations et l'offre ?"
                );
                message.setStyle(
                    "-fx-font-size: 14px;" +
                    "-fx-text-fill: #374151;" +
                    "-fx-wrap-text: true;"
                );
                message.setWrapText(true);

                content.getChildren().addAll(icon, title, message);
                content.setAlignment(Pos.CENTER);

                confirm.getDialogPane().setContent(content);

                // Style dialog pane
                confirm.getDialogPane().setStyle(
                    "-fx-background-color: white;" +
                    "-fx-background-radius: 20;" +
                    "-fx-border-color: #fecaca;" +
                    "-fx-border-radius: 20;" +
                    "-fx-border-width: 2;" +
                    "-fx-effect: dropshadow(gaussian, rgba(220,38,38,0.2), 20, 0, 0, 8);"
                );

                ButtonType btnOui = new ButtonType("Oui, supprimer tout", ButtonBar.ButtonData.OK_DONE);
                ButtonType btnNon = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
                confirm.getButtonTypes().setAll(btnOui, btnNon);

                // Style buttons
                confirm.getDialogPane().lookupButton(btnOui).setStyle(
                    "-fx-background-color: #dc2626;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 10 24;" +
                    "-fx-background-radius: 10;" +
                    "-fx-cursor: hand;" +
                    "-fx-font-size: 14px;"
                );

                confirm.getDialogPane().lookupButton(btnNon).setStyle(
                    "-fx-background-color: #f3f4f6;" +
                    "-fx-text-fill: #6b7280;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 10 24;" +
                    "-fx-background-radius: 10;" +
                    "-fx-cursor: hand;" +
                    "-fx-font-size: 14px;"
                );

                Optional<ButtonType> res = confirm.showAndWait();
                if (res.isEmpty() || res.get() != btnOui) return;

                for (Postulation p : posts) {
                    postService.supprimer(p.getId());
                }
            } else {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirmation de suppression");
                confirm.setHeaderText(null);

                // Custom content with styled layout
                VBox content = new VBox(16);
                content.setPadding(new Insets(20));
                content.setStyle("-fx-background-color: white;");

                Label icon = new Label("🗑️");
                icon.setStyle("-fx-font-size: 48px;");

                Label title = new Label("Supprimer cette offre ?");
                title.setStyle(
                    "-fx-font-size: 18px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: #1e1133;"
                );

                Label message = new Label(
                    "Titre : " + safe(offre.getTitre()) + "\n" +
                    "Entreprise : " + safe(offre.getEntreprise())
                );
                message.setStyle(
                    "-fx-font-size: 14px;" +
                    "-fx-text-fill: #6b7280;" +
                    "-fx-wrap-text: true;"
                );
                message.setWrapText(true);

                content.getChildren().addAll(icon, title, message);
                content.setAlignment(Pos.CENTER);

                confirm.getDialogPane().setContent(content);

                // Style dialog pane
                confirm.getDialogPane().setStyle(
                    "-fx-background-color: white;" +
                    "-fx-background-radius: 20;" +
                    "-fx-border-color: #e0d4f5;" +
                    "-fx-border-radius: 20;" +
                    "-fx-border-width: 2;" +
                    "-fx-effect: dropshadow(gaussian, rgba(124,58,237,0.2), 20, 0, 0, 8);"
                );

                // Style default buttons
                confirm.getDialogPane().lookupButton(ButtonType.OK).setStyle(
                    "-fx-background-color: #7c3aed;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 10 24;" +
                    "-fx-background-radius: 10;" +
                    "-fx-cursor: hand;" +
                    "-fx-font-size: 14px;"
                );

                confirm.getDialogPane().lookupButton(ButtonType.CANCEL).setStyle(
                    "-fx-background-color: #f3f4f6;" +
                    "-fx-text-fill: #6b7280;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 10 24;" +
                    "-fx-background-radius: 10;" +
                    "-fx-cursor: hand;" +
                    "-fx-font-size: 14px;"
                );

                Optional<ButtonType> res = confirm.showAndWait();
                if (res.isEmpty() || res.get() != ButtonType.OK) return;
            }

            offreService.supprimer(offre.getId());
            loadData();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Offre supprimée.");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression impossible :\n" + e.getMessage());
        }
    }

    // ===================== EDIT DIALOG (button only) =====================

    private void openEditDialog(OffreEmploi offre) {
        Dialog<OffreEmploi> dialog = new Dialog<>();
        dialog.setTitle("Modifier l'offre");
        dialog.setHeaderText(null);

        // Make dialog resizable
        dialog.setResizable(true);

        // Apply custom styling to dialog
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: #e0d4f5;" +
            "-fx-border-radius: 20;" +
            "-fx-border-width: 2;" +
            "-fx-effect: dropshadow(gaussian, rgba(124,58,237,0.2), 20, 0, 0, 8);"
        );

        // Set preferred and min dimensions for responsiveness
        dialogPane.setPrefSize(700, 650);
        dialogPane.setMinSize(600, 500);

        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        // Style buttons
        dialogPane.lookupButton(saveBtn).setStyle(
            "-fx-background-color: #7c3aed;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 10 24;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;" +
            "-fx-font-size: 14px;"
        );

        dialogPane.lookupButton(ButtonType.CANCEL).setStyle(
            "-fx-background-color: #f3f4f6;" +
            "-fx-text-fill: #6b7280;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 10 24;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;" +
            "-fx-font-size: 14px;"
        );

        // Create GridPane inside ScrollPane for scrollable content
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);
        grid.setPadding(new Insets(24));
        grid.setStyle("-fx-background-color: white;");

        // Wrap grid in ScrollPane
        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
            "-fx-background: white;" +
            "-fx-background-color: white;" +
            "-fx-border-color: transparent;"
        );
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // Style for labels
        final String labelStyle = "-fx-font-weight: bold; -fx-text-fill: #1a1225; -fx-font-size: 13px;";

        // Style for input fields
        final String inputStyle =
            "-fx-background-color: white;" +
            "-fx-border-color: #e0d4f5;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 10 14;" +
            "-fx-font-size: 13px;";

        // Style for input fields with error
        final String inputErrorStyle =
            "-fx-background-color: white;" +
            "-fx-border-color: #dc2626;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 10 14;" +
            "-fx-font-size: 13px;" +
            "-fx-border-width: 2;";

        // Style for error labels
        final String errorLabelStyle =
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #dc2626;" +
            "-fx-font-weight: 600;" +
            "-fx-padding: 4 0 0 0;";

        // Create error labels
        Label errTitre = new Label();
        errTitre.setStyle(errorLabelStyle);
        errTitre.setVisible(false);
        errTitre.setManaged(false);

        Label errDescription = new Label();
        errDescription.setStyle(errorLabelStyle);
        errDescription.setVisible(false);
        errDescription.setManaged(false);

        Label errSalaire = new Label();
        errSalaire.setStyle(errorLabelStyle);
        errSalaire.setVisible(false);
        errSalaire.setManaged(false);

        Label errTypeContrat = new Label();
        errTypeContrat.setStyle(errorLabelStyle);
        errTypeContrat.setVisible(false);
        errTypeContrat.setManaged(false);

        Label errLocalisation = new Label();
        errLocalisation.setStyle(errorLabelStyle);
        errLocalisation.setVisible(false);
        errLocalisation.setManaged(false);

        Label errExpiration = new Label();
        errExpiration.setStyle(errorLabelStyle);
        errExpiration.setVisible(false);
        errExpiration.setManaged(false);

        Label errContact = new Label();
        errContact.setStyle(errorLabelStyle);
        errContact.setVisible(false);
        errContact.setManaged(false);

        Label errEntreprise = new Label();
        errEntreprise.setStyle(errorLabelStyle);
        errEntreprise.setVisible(false);
        errEntreprise.setManaged(false);

        Label errSecteur = new Label();
        errSecteur.setStyle(errorLabelStyle);
        errSecteur.setVisible(false);
        errSecteur.setManaged(false);

        Label errNiveau = new Label();
        errNiveau.setStyle(errorLabelStyle);
        errNiveau.setVisible(false);
        errNiveau.setManaged(false);

        Label errExperience = new Label();
        errExperience.setStyle(errorLabelStyle);
        errExperience.setVisible(false);
        errExperience.setManaged(false);

        Label errCompetences = new Label();
        errCompetences.setStyle(errorLabelStyle);
        errCompetences.setVisible(false);
        errCompetences.setManaged(false);

        TextField titre = new TextField(safe(offre.getTitre()));
        titre.setStyle(inputStyle);
        titre.setPromptText("Ex: Développeur Java Senior");
        titre.setPrefWidth(400);

        TextArea desc = new TextArea(safe(offre.getDescription()));
        desc.setPrefRowCount(3);
        desc.setWrapText(true);
        desc.setStyle(inputStyle);
        desc.setPromptText("Description détaillée du poste...");
        desc.setPrefWidth(400);

        TextField salaire = new TextField(offre.getSalaire() == 0 ? "" : String.valueOf((int) offre.getSalaire()));
        salaire.setStyle(inputStyle);
        salaire.setPromptText("Ex: 2500");
        salaire.setPrefWidth(400);

        TextField localisation = new TextField(safe(offre.getLocalisation()));
        localisation.setStyle(inputStyle);
        localisation.setPromptText("Ex: Tunis, Ariana");
        localisation.setPrefWidth(400);

        TextField entreprise = new TextField(safe(offre.getEntreprise()));
        entreprise.setStyle(inputStyle);
        entreprise.setPromptText("Nom de l'entreprise");
        entreprise.setPrefWidth(400);

        TextField contact = new TextField(safe(offre.getContactRecruteur()));
        contact.setStyle(inputStyle);
        contact.setPromptText("email@entreprise.tn");
        contact.setPrefWidth(400);

        ComboBox<String> typeContrat = new ComboBox<>();
        typeContrat.getItems().setAll("CDI", "CDD", "Stage", "Freelance", "Alternance");
        typeContrat.setValue(safe(offre.getTypeContrat()).isEmpty() ? null : offre.getTypeContrat());
        typeContrat.setStyle(inputStyle);
        typeContrat.setPromptText("Sélectionnez...");
        typeContrat.setPrefWidth(400);
        typeContrat.setMaxWidth(Double.MAX_VALUE);

        TextField niveau = new TextField(safe(offre.getNiveauQualification()));
        niveau.setStyle(inputStyle);
        niveau.setPromptText("Ex: Bac+5");
        niveau.setPrefWidth(400);

        TextField experience = new TextField(safe(offre.getExperienceRequise()));
        experience.setStyle(inputStyle);
        experience.setPromptText("Ex: 2-4 ans");
        experience.setPrefWidth(400);

        TextField competences = new TextField(safe(offre.getCompetencesRequises()));
        competences.setStyle(inputStyle);
        competences.setPromptText("Java, Spring, SQL...");
        competences.setPrefWidth(400);

        TextField secteur = new TextField(safe(offre.getSecteurActivite()));
        secteur.setStyle(inputStyle);
        secteur.setPromptText("Ex: Informatique");
        secteur.setPrefWidth(400);

        DatePicker pubDate = new DatePicker(
                offre.getDatePublication() == null ? LocalDate.now() : offre.getDatePublication().toLocalDate()
        );
        pubDate.setStyle(inputStyle);
        pubDate.setPrefWidth(400);

        DatePicker expDate = new DatePicker(
                offre.getDateExpiration() == null ? LocalDate.now().plusDays(7) : offre.getDateExpiration().toLocalDate()
        );
        expDate.setStyle(inputStyle);
        expDate.setPrefWidth(400);

        // Add header section
        Label headerLabel = new Label("✏️ Modifier l'offre d'emploi");
        headerLabel.setStyle(
            "-fx-font-size: 20px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #1e1133;" +
            "-fx-padding: 0 0 12 0;"
        );
        grid.add(headerLabel, 0, 0, 2, 1);

        int row = 1;

        // Add fields with styled labels and error labels
        Label lblTitre = new Label("Titre");
        lblTitre.setStyle(labelStyle);
        VBox vboxTitre = new VBox(6, titre, errTitre);
        grid.add(lblTitre, 0, row);
        grid.add(vboxTitre, 1, row++);

        Label lblDesc = new Label("Description");
        lblDesc.setStyle(labelStyle);
        VBox vboxDesc = new VBox(6, desc, errDescription);
        grid.add(lblDesc, 0, row);
        grid.add(vboxDesc, 1, row++);

        Label lblSalaire = new Label("Salaire (DT)");
        lblSalaire.setStyle(labelStyle);
        VBox vboxSalaire = new VBox(6, salaire, errSalaire);
        grid.add(lblSalaire, 0, row);
        grid.add(vboxSalaire, 1, row++);

        Label lblType = new Label("Type contrat");
        lblType.setStyle(labelStyle);
        VBox vboxType = new VBox(6, typeContrat, errTypeContrat);
        grid.add(lblType, 0, row);
        grid.add(vboxType, 1, row++);

        Label lblLoc = new Label("Localisation");
        lblLoc.setStyle(labelStyle);
        VBox vboxLoc = new VBox(6, localisation, errLocalisation);
        grid.add(lblLoc, 0, row);
        grid.add(vboxLoc, 1, row++);

        Label lblPub = new Label("Publication");
        lblPub.setStyle(labelStyle);
        grid.add(lblPub, 0, row);
        grid.add(pubDate, 1, row++);

        Label lblExp = new Label("Expiration");
        lblExp.setStyle(labelStyle);
        VBox vboxExp = new VBox(6, expDate, errExpiration);
        grid.add(lblExp, 0, row);
        grid.add(vboxExp, 1, row++);

        Label lblNiveau = new Label("Niveau");
        lblNiveau.setStyle(labelStyle);
        VBox vboxNiveau = new VBox(6, niveau, errNiveau);
        grid.add(lblNiveau, 0, row);
        grid.add(vboxNiveau, 1, row++);

        Label lblExperience = new Label("Expérience");
        lblExperience.setStyle(labelStyle);
        VBox vboxExperience = new VBox(6, experience, errExperience);
        grid.add(lblExperience, 0, row);
        grid.add(vboxExperience, 1, row++);

        Label lblComp = new Label("Compétences");
        lblComp.setStyle(labelStyle);
        VBox vboxComp = new VBox(6, competences, errCompetences);
        grid.add(lblComp, 0, row);
        grid.add(vboxComp, 1, row++);

        Label lblSecteur = new Label("Secteur");
        lblSecteur.setStyle(labelStyle);
        VBox vboxSecteur = new VBox(6, secteur, errSecteur);
        grid.add(lblSecteur, 0, row);
        grid.add(vboxSecteur, 1, row++);

        Label lblEntreprise = new Label("Entreprise");
        lblEntreprise.setStyle(labelStyle);
        VBox vboxEntreprise = new VBox(6, entreprise, errEntreprise);
        grid.add(lblEntreprise, 0, row);
        grid.add(vboxEntreprise, 1, row++);

        Label lblContact = new Label("Contact (email)");
        lblContact.setStyle(labelStyle);
        VBox vboxContact = new VBox(6, contact, errContact);
        grid.add(lblContact, 0, row);
        grid.add(vboxContact, 1, row++);

        // Set scrollPane as content
        dialog.getDialogPane().setContent(scrollPane);

        Node okBtn = dialog.getDialogPane().lookupButton(saveBtn);
        okBtn.setDisable(titre.getText().trim().isEmpty());
        titre.textProperty().addListener((obs, o, n) -> okBtn.setDisable(n.trim().isEmpty()));

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;

            // Clear all errors first
            errTitre.setVisible(false);
            errTitre.setManaged(false);
            errDescription.setVisible(false);
            errDescription.setManaged(false);
            errSalaire.setVisible(false);
            errSalaire.setManaged(false);
            errTypeContrat.setVisible(false);
            errTypeContrat.setManaged(false);
            errLocalisation.setVisible(false);
            errLocalisation.setManaged(false);
            errExpiration.setVisible(false);
            errExpiration.setManaged(false);
            errContact.setVisible(false);
            errContact.setManaged(false);
            errEntreprise.setVisible(false);
            errEntreprise.setManaged(false);
            errSecteur.setVisible(false);
            errSecteur.setManaged(false);
            errNiveau.setVisible(false);
            errNiveau.setManaged(false);
            errExperience.setVisible(false);
            errExperience.setManaged(false);
            errCompetences.setVisible(false);
            errCompetences.setManaged(false);

            boolean hasError = false;

            // Validate titre
            if (safe(titre.getText()).isEmpty()) {
                errTitre.setText("Le titre est obligatoire.");
                errTitre.setVisible(true);
                errTitre.setManaged(true);
                titre.setStyle(inputErrorStyle);
                hasError = true;
            } else {
                titre.setStyle(inputStyle);
            }

            // Validate description
            if (safe(desc.getText()).isEmpty()) {
                errDescription.setText("La description est obligatoire.");
                errDescription.setVisible(true);
                errDescription.setManaged(true);
                desc.setStyle(inputErrorStyle);
                hasError = true;
            } else {
                desc.setStyle(inputStyle);
            }

            // Validate type contrat
            if (typeContrat.getValue() == null || safe(typeContrat.getValue()).isEmpty()) {
                errTypeContrat.setText("Le type de contrat est obligatoire.");
                errTypeContrat.setVisible(true);
                errTypeContrat.setManaged(true);
                typeContrat.setStyle(inputErrorStyle);
                hasError = true;
            } else {
                typeContrat.setStyle(inputStyle);
            }

            // Validate localisation
            if (safe(localisation.getText()).isEmpty()) {
                errLocalisation.setText("La localisation est obligatoire.");
                errLocalisation.setVisible(true);
                errLocalisation.setManaged(true);
                localisation.setStyle(inputErrorStyle);
                hasError = true;
            } else {
                localisation.setStyle(inputStyle);
            }

            // Validate contact email
            if (safe(contact.getText()).isEmpty()) {
                errContact.setText("Le contact est obligatoire.");
                errContact.setVisible(true);
                errContact.setManaged(true);
                contact.setStyle(inputErrorStyle);
                hasError = true;
            } else if (!isValidEmail(safe(contact.getText()))) {
                errContact.setText("Email invalide (ex: recrutement@entreprise.tn).");
                errContact.setVisible(true);
                errContact.setManaged(true);
                contact.setStyle(inputErrorStyle);
                hasError = true;
            } else {
                contact.setStyle(inputStyle);
            }

            // Validate salaire
            double sal = 0;
            if (safe(salaire.getText()).isEmpty()) {
                errSalaire.setText("Le salaire est obligatoire.");
                errSalaire.setVisible(true);
                errSalaire.setManaged(true);
                salaire.setStyle(inputErrorStyle);
                hasError = true;
            } else {
                try {
                    sal = Double.parseDouble(safe(salaire.getText()));
                    if (sal <= 0) {
                        errSalaire.setText("Le salaire doit être > 0 (ex: 2500).");
                        errSalaire.setVisible(true);
                        errSalaire.setManaged(true);
                        salaire.setStyle(inputErrorStyle);
                        hasError = true;
                    } else {
                        salaire.setStyle(inputStyle);
                    }
                } catch (Exception ex) {
                    errSalaire.setText("Le salaire doit être un nombre valide (ex: 2500).");
                    errSalaire.setVisible(true);
                    errSalaire.setManaged(true);
                    salaire.setStyle(inputErrorStyle);
                    hasError = true;
                }
            }

            // Validate entreprise
            if (safe(entreprise.getText()).isEmpty()) {
                errEntreprise.setText("Le nom de l'entreprise est obligatoire.");
                errEntreprise.setVisible(true);
                errEntreprise.setManaged(true);
                entreprise.setStyle(inputErrorStyle);
                hasError = true;
            } else {
                entreprise.setStyle(inputStyle);
            }

            // Validate secteur
            if (safe(secteur.getText()).isEmpty()) {
                errSecteur.setText("Le secteur d'activité est obligatoire.");
                errSecteur.setVisible(true);
                errSecteur.setManaged(true);
                secteur.setStyle(inputErrorStyle);
                hasError = true;
            } else {
                secteur.setStyle(inputStyle);
            }

            // Validate niveau
            if (safe(niveau.getText()).isEmpty()) {
                errNiveau.setText("Le niveau de qualification est obligatoire.");
                errNiveau.setVisible(true);
                errNiveau.setManaged(true);
                niveau.setStyle(inputErrorStyle);
                hasError = true;
            } else {
                niveau.setStyle(inputStyle);
            }

            // Validate experience
            if (safe(experience.getText()).isEmpty()) {
                errExperience.setText("L'expérience requise est obligatoire.");
                errExperience.setVisible(true);
                errExperience.setManaged(true);
                experience.setStyle(inputErrorStyle);
                hasError = true;
            } else if (!experience.getText().matches("^\\d+(\\s*ans)?$|^\\d+\\s*-\\s*\\d+(\\s*ans)?$")) {
                errExperience.setText("Expérience invalide (ex: 2, 2-5, 3 ans).");
                errExperience.setVisible(true);
                errExperience.setManaged(true);
                experience.setStyle(inputErrorStyle);
                hasError = true;
            } else {
                experience.setStyle(inputStyle);
            }

            // Validate competences
            if (safe(competences.getText()).isEmpty()) {
                errCompetences.setText("Les compétences sont obligatoires.");
                errCompetences.setVisible(true);
                errCompetences.setManaged(true);
                competences.setStyle(inputErrorStyle);
                hasError = true;
            } else if (!competences.getText().contains(",")) {
                errCompetences.setText("Séparez les compétences par des virgules (ex: Java, SQL, React).");
                errCompetences.setVisible(true);
                errCompetences.setManaged(true);
                competences.setStyle(inputErrorStyle);
                hasError = true;
            } else {
                competences.setStyle(inputStyle);
            }

            // Validate dates
            if (pubDate.getValue() == null || expDate.getValue() == null) {
                errExpiration.setText("Les dates de publication et expiration sont obligatoires.");
                errExpiration.setVisible(true);
                errExpiration.setManaged(true);
                if (expDate.getValue() == null) {
                    expDate.setStyle(inputErrorStyle);
                }
                hasError = true;
            } else if (expDate.getValue().isBefore(pubDate.getValue())) {
                errExpiration.setText("L'expiration doit être après la publication.");
                errExpiration.setVisible(true);
                errExpiration.setManaged(true);
                expDate.setStyle(inputErrorStyle);
                hasError = true;
            } else {
                expDate.setStyle(inputStyle);
            }

            if (hasError) {
                return null;
            }

            LocalDateTime pub = pubDate.getValue().atTime(0, 0);
            LocalDateTime exp = expDate.getValue().atTime(23, 59);

            return new OffreEmploi(
                    offre.getId(),
                    safe(titre.getText()),
                    safe(desc.getText()),
                    sal,
                    safe(typeContrat.getValue()),
                    safe(localisation.getText()),
                    pub,
                    exp,
                    safe(niveau.getText()),
                    safe(experience.getText()),
                    safe(competences.getText()),
                    safe(secteur.getText()),
                    safe(entreprise.getText()),
                    safe(contact.getText())
            );
        });

        Optional<OffreEmploi> res = dialog.showAndWait();
        if (res.isEmpty()) return;

        try {
            offreService.modifier(res.get());
            loadData();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Offre modifiée.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Modification impossible :\n" + e.getMessage());
        }
    }

    // ===================== HELPERS =====================

    private Button iconBtn(String icon, String tip) {
        Button b = new Button(icon);
        b.setTooltip(new Tooltip(tip));
        b.setMinWidth(36);
        b.setPrefWidth(36);
        b.setMinHeight(32);
        b.setPrefHeight(32);
        return b;
    }

    // ===================== SHOW STATS - Navigation complète =====================

    private void showStatsPopup(OffreEmploi offre) {
        // Naviguer vers l'interface stats via le shell
        OffresShellController shell = OffresShellController.getInstance();
        if (shell != null) {
            shell.showOffreStats(offre);
        }
    }

    // ===================== SHOW QR CODE POPUP =====================

    /**
     * Affiche le popup du QR Code pour partager l'offre
     */
    private void showQRCodePopup(OffreEmploi offre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/qrcode-popup.fxml"));
            Parent root = loader.load();

            // Passer les données de l'offre au contrôleur
            QRCodePopupController controller = loader.getController();
            controller.initData(offre);

            // Créer le stage du popup
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("QR Code - " + offre.getTitre());
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            // Centrer sur l'écran
            stage.centerOnScreen();

            // Afficher
            stage.showAndWait();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                "Impossible d'ouvrir le popup QR Code:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===================== ALERTS =====================

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);

        // Custom content with styled layout
        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");

        // Icon based on alert type
        String icon = switch (type) {
            case ERROR -> "❌";
            case WARNING -> "⚠️";
            case INFORMATION -> "✅";
            case CONFIRMATION -> "❓";
            default -> "ℹ️";
        };

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 48px;");

        Label titleLabel = new Label(title);
        String titleColor = switch (type) {
            case ERROR -> "#dc2626";
            case WARNING -> "#f59e0b";
            case INFORMATION -> "#10b981";
            case CONFIRMATION -> "#7c3aed";
            default -> "#6b7280";
        };
        titleLabel.setStyle(
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + titleColor + ";"
        );

        Label message = new Label(msg);
        message.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-text-fill: #374151;" +
            "-fx-wrap-text: true;"
        );
        message.setWrapText(true);
        message.setMaxWidth(400);

        content.getChildren().addAll(iconLabel, titleLabel, message);
        content.setAlignment(Pos.CENTER);

        a.getDialogPane().setContent(content);

        // Style dialog pane with border color based on type
        String borderColor = switch (type) {
            case ERROR -> "#fecaca";
            case WARNING -> "#fef3c7";
            case INFORMATION -> "#d1fae5";
            case CONFIRMATION -> "#e0d4f5";
            default -> "#e5e7eb";
        };

        a.getDialogPane().setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: " + borderColor + ";" +
            "-fx-border-radius: 20;" +
            "-fx-border-width: 2;" +
            "-fx-effect: dropshadow(gaussian, rgba(124,58,237,0.2), 20, 0, 0, 8);"
        );

        // Style OK button
        Node okButton = a.getDialogPane().lookupButton(ButtonType.OK);
        if (okButton != null) {
            String buttonColor = switch (type) {
                case ERROR -> "#dc2626";
                case WARNING -> "#f59e0b";
                case INFORMATION -> "#10b981";
                default -> "#7c3aed";
            };
            okButton.setStyle(
                "-fx-background-color: " + buttonColor + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 24;" +
                "-fx-background-radius: 10;" +
                "-fx-cursor: hand;" +
                "-fx-font-size: 14px;"
            );
        }

        a.showAndWait();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }
}
