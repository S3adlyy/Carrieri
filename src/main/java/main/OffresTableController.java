package main;

import entities.OffreEmploi;
import entities.Postulation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
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
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import services.OffreEmploiService;
import services.PostulationService;
import utils.StyledAlert;

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
        tableOffres.setEditable(true);

        // Liaison des colonnes
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

        // Tooltip pour description longue
        colDescription.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    setTooltip(new Tooltip(item));
                }
            }
        });

        // Édition inline
        enableInlineEditing();

        // Colonne actions (boutons)
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

        // Recherche
        txtSearch.textProperty().addListener((obs, o, n) -> applyFilter());

        loadData();
    }

    // ===================== INLINE EDITING =====================

    private void enableInlineEditing() {
        // Titre
        colTitre.setCellFactory(TextFieldTableCell.forTableColumn());
        colTitre.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            if (nv.isEmpty()) {
                StyledAlert.showError("Erreur", "Le titre est obligatoire.");
                refreshRow(old);
                return;
            }
            if (nv.matches("^\\d.*")) {
                StyledAlert.showError("Erreur", "Le titre ne peut pas commencer par un chiffre.");
                refreshRow(old);
                return;
            }
            if (!nv.equals(old.getTitre())) {
                try {
                    if (offreService.existsByTitre(nv)) {
                        StyledAlert.showError("Erreur", "Ce titre existe déjà.");
                        refreshRow(old);
                        return;
                    }
                } catch (SQLException e) {
                    StyledAlert.showError("Erreur", "Erreur lors de la vérification du titre.");
                    refreshRow(old);
                    return;
                }
            }
            OffreEmploi updated = copy(old, nv, null, null, null, null, null, null, null, null, null, null, null, null);
            saveInline(old, updated, "Titre");
        });

        // Description
        colDescription.setCellFactory(TextFieldTableCell.forTableColumn());
        colDescription.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            if (nv.isEmpty()) {
                StyledAlert.showError("Erreur", "La description est obligatoire.");
                refreshRow(old);
                return;
            }
            if (nv.matches("^\\d.*")) {
                StyledAlert.showError("Erreur", "La description ne peut pas commencer par un chiffre.");
                refreshRow(old);
                return;
            }
            OffreEmploi updated = copy(old, null, nv, null, null, null, null, null, null, null, null, null, null, null);
            saveInline(old, updated, "Description");
        });

        // Salaire
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
            if (nv == null || nv <= 0) {
                StyledAlert.showError("Erreur", "Salaire invalide.");
                refreshRow(old);
                return;
            }
            OffreEmploi updated = copy(old, null, null, nv, null, null, null, null, null, null, null, null, null, null);
            saveInline(old, updated, "Salaire");
        });

        // Type contrat
        colType.setCellFactory(ComboBoxTableCell.forTableColumn("CDI", "CDD", "Stage", "Freelance", "Alternance"));
        colType.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            if (nv.isEmpty()) {
                StyledAlert.showError("Erreur", "Le type de contrat est obligatoire.");
                refreshRow(old);
                return;
            }
            OffreEmploi updated = copy(old, null, null, null, nv, null, null, null, null, null, null, null, null, null);
            saveInline(old, updated, "Type contrat");
        });

        // Localisation
        colLocalisation.setCellFactory(TextFieldTableCell.forTableColumn());
        colLocalisation.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            if (nv.isEmpty()) {
                StyledAlert.showError("Erreur", "La localisation est obligatoire.");
                refreshRow(old);
                return;
            }
            OffreEmploi updated = copy(old, null, null, null, null, nv, null, null, null, null, null, null, null, null);
            saveInline(old, updated, "Localisation");
        });

        // Publication
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
            if (nv == null) {
                StyledAlert.showError("Erreur", "Format publication invalide (dd/MM/yyyy).");
                refreshRow(old);
                return;
            }
            LocalDateTime exp = old.getDateExpiration();
            if (exp != null && nv.isAfter(exp)) {
                StyledAlert.showError("Erreur", "La publication doit être avant l'expiration.");
                refreshRow(old);
                return;
            }
            OffreEmploi updated = copy(old, null, null, null, null, null, nv, null, null, null, null, null, null, null);
            saveInline(old, updated, "Publication");
        });

        // Expiration
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
            if (nv == null) {
                StyledAlert.showError("Erreur", "Format expiration invalide (dd/MM/yyyy).");
                refreshRow(old);
                return;
            }
            LocalDateTime pub = old.getDatePublication();
            if (pub != null && pub.isAfter(nv)) {
                StyledAlert.showError("Erreur", "L'expiration doit être après la publication.");
                refreshRow(old);
                return;
            }
            OffreEmploi updated = copy(old, null, null, null, null, null, null, nv, null, null, null, null, null, null);
            saveInline(old, updated, "Expiration");
        });

        // Niveau
        colNiveau.setCellFactory(TextFieldTableCell.forTableColumn());
        colNiveau.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            OffreEmploi updated = copy(old, null, null, null, null, null, null, null, nv, null, null, null, null, null);
            saveInline(old, updated, "Niveau");
        });

        // Expérience
        colExperience.setCellFactory(TextFieldTableCell.forTableColumn());
        colExperience.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            OffreEmploi updated = copy(old, null, null, null, null, null, null, null, null, nv, null, null, null, null);
            saveInline(old, updated, "Expérience");
        });

        // Compétences
        colCompetences.setCellFactory(TextFieldTableCell.forTableColumn());
        colCompetences.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            OffreEmploi updated = copy(old, null, null, null, null, null, null, null, null, null, nv, null, null, null);
            saveInline(old, updated, "Compétences");
        });

        // Secteur
        colSecteur.setCellFactory(TextFieldTableCell.forTableColumn());
        colSecteur.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            OffreEmploi updated = copy(old, null, null, null, null, null, null, null, null, null, null, nv, null, null);
            saveInline(old, updated, "Secteur");
        });

        // Entreprise
        colEntreprise.setCellFactory(TextFieldTableCell.forTableColumn());
        colEntreprise.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            if (nv.isEmpty()) {
                StyledAlert.showError("Erreur", "Le nom de l'entreprise est obligatoire.");
                refreshRow(old);
                return;
            }
            OffreEmploi updated = copy(old, null, null, null, null, null, null, null, null, null, null, null, nv, null);
            saveInline(old, updated, "Entreprise");
        });

        // Contact
        colContact.setCellFactory(TextFieldTableCell.forTableColumn());
        colContact.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String email = safe(ev.getNewValue());
            if (!isValidEmail(email)) {
                StyledAlert.showError("Erreur", "Email invalide.");
                refreshRow(old);
                return;
            }
            OffreEmploi updated = copy(old, null, null, null, null, null, null, null, null, null, null, null, null, email);
            saveInline(old, updated, "Contact");
        });
    }

    private void saveInline(OffreEmploi oldOffre, OffreEmploi updated, String fieldName) {
        try {
            offreService.modifier(updated);
            replaceById(allData, updated);
            replaceById(filtered, updated);
            tableOffres.refresh();
        } catch (SQLException e) {
            StyledAlert.showError("Erreur", "Modification impossible (" + fieldName + ") :\n" + e.getMessage());
            loadData(); // rollback
        }
    }

    private void refreshRow(OffreEmploi offre) {
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

    private OffreEmploi copy(OffreEmploi o,
                             String titre, String description, Double salaire,
                             String typeContrat, String localisation,
                             LocalDateTime datePublication, LocalDateTime dateExpiration,
                             String niveauQualification, String experienceRequise,
                             String competencesRequises, String secteurActivite,
                             String entreprise, String contactRecruteur) {
        return new OffreEmploi(
                o.getId(),
                titre != null ? titre : o.getTitre(),
                description != null ? description : o.getDescription(),
                salaire != null ? salaire : o.getSalaire(),
                typeContrat != null ? typeContrat : o.getTypeContrat(),
                localisation != null ? localisation : o.getLocalisation(),
                datePublication != null ? datePublication : o.getDatePublication(),
                dateExpiration != null ? dateExpiration : o.getDateExpiration(),
                niveauQualification != null ? niveauQualification : o.getNiveauQualification(),
                experienceRequise != null ? experienceRequise : o.getExperienceRequise(),
                competencesRequises != null ? competencesRequises : o.getCompetencesRequises(),
                secteurActivite != null ? secteurActivite : o.getSecteurActivite(),
                entreprise != null ? entreprise : o.getEntreprise(),
                contactRecruteur != null ? contactRecruteur : o.getContactRecruteur()
        );
    }

    // ===================== DATA / FILTER =====================

    @FXML
    private void handleGoToAddOffre() {
        OffresShellController shell = OffresShellController.getInstance();
        if (shell != null) {
            shell.showOffreAdd();
        } else {
            StyledAlert.showError("Erreur", "Shell non disponible.");
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
            StyledAlert.showError("Erreur", "Erreur de chargement :\n" + e.getMessage());
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

    // ===================== ACTIONS =====================

    private void goToPostulations(OffreEmploi offre) {
        OffresShellController shell = OffresShellController.getInstance();
        if (shell != null) {
            shell.showPostulationsForOffre(offre.getId(), offre.getTitre());
        } else {
            StyledAlert.showError("Erreur", "Shell non disponible.");
        }
    }

    private void handleDelete(OffreEmploi offre) {
        try {
            List<Postulation> posts = postService.afficherParOffre(offre.getId());
            if (!posts.isEmpty()) {
                Alert confirm = StyledAlert.deleteConfirmation(offre.getTitre() + " (" + posts.size() + " postulation(s))");
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get().getText().equals("🗑️ Supprimer")) {
                    for (Postulation p : posts) {
                        postService.supprimer(p.getId());
                    }
                    offreService.supprimer(offre.getId());
                    loadData();
                    StyledAlert.showSuccess("Succès", "Offre et postulations supprimées.");
                }
            } else {
                boolean confirmed = StyledAlert.showConfirmation(
                        "Confirmation de suppression",
                        "Supprimer l'offre : " + offre.getTitre() + " ?"
                );
                if (confirmed) {
                    offreService.supprimer(offre.getId());
                    loadData();
                    StyledAlert.showSuccess("Succès", "Offre supprimée.");
                }
            }
        } catch (SQLException e) {
            StyledAlert.showError("Erreur", "Suppression impossible :\n" + e.getMessage());
        }
    }

    private void showStatsPopup(OffreEmploi offre) {
        OffresShellController shell = OffresShellController.getInstance();
        if (shell != null) {
            shell.showOffreStats(offre);
        }
    }

    private void showQRCodePopup(OffreEmploi offre) {
        OffresShellController shell = OffresShellController.getInstance();
        if (shell != null) {
            shell.showQRCode(offre);
        } else {
            StyledAlert.showError("Erreur", "Shell non disponible.");
        }
    }

    // ===================== DIALOG D'ÉDITION =====================

    private void openEditDialog(OffreEmploi offre) {
        Dialog<OffreEmploi> dialog = new Dialog<>();
        dialog.setTitle("Modifier l'offre");
        dialog.setHeaderText(null);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().addAll(
                getClass().getResource("/css/theme-unified.css").toExternalForm(),
                getClass().getResource("/css/theme-dark.css").toExternalForm()
        );
        dialogPane.getStyleClass().add("glass-card");
        dialogPane.setPrefSize(700, 650);
        dialogPane.setMinSize(600, 500);

        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        // Styliser les boutons via le CSS
        Node okButton = dialogPane.lookupButton(saveBtn);
        okButton.getStyleClass().addAll("button", "btn-primary");
        Node cancelButton = dialogPane.lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().addAll("button", "btn-secondary");

        // Création du formulaire (identique à avant, mais sans styles inline)
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);
        grid.setPadding(new Insets(24));
        grid.setStyle("-fx-background-color: transparent;");

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // Champs de saisie (avec classes CSS)
        TextField titre = new TextField(safe(offre.getTitre()));
        titre.getStyleClass().add("elegant-field");
        titre.setPromptText("Ex: Développeur Java Senior");

        TextArea desc = new TextArea(safe(offre.getDescription()));
        desc.getStyleClass().add("elegant-field");
        desc.setPromptText("Description détaillée du poste...");
        desc.setPrefRowCount(3);
        desc.setWrapText(true);

        TextField salaire = new TextField(offre.getSalaire() == 0 ? "" : String.valueOf((int) offre.getSalaire()));
        salaire.getStyleClass().add("elegant-field");
        salaire.setPromptText("Ex: 2500");

        TextField localisation = new TextField(safe(offre.getLocalisation()));
        localisation.getStyleClass().add("elegant-field");
        localisation.setPromptText("Ex: Tunis, Ariana");

        TextField entreprise = new TextField(safe(offre.getEntreprise()));
        entreprise.getStyleClass().add("elegant-field");
        entreprise.setPromptText("Nom de l'entreprise");

        TextField contact = new TextField(safe(offre.getContactRecruteur()));
        contact.getStyleClass().add("elegant-field");
        contact.setPromptText("email@entreprise.tn");

        ComboBox<String> typeContrat = new ComboBox<>();
        typeContrat.getItems().setAll("CDI", "CDD", "Stage", "Freelance", "Alternance");
        typeContrat.setValue(safe(offre.getTypeContrat()).isEmpty() ? null : offre.getTypeContrat());
        typeContrat.setPromptText("Sélectionnez...");
        typeContrat.getStyleClass().add("combo-box");

        TextField niveau = new TextField(safe(offre.getNiveauQualification()));
        niveau.getStyleClass().add("elegant-field");
        niveau.setPromptText("Ex: Bac+5");

        TextField experience = new TextField(safe(offre.getExperienceRequise()));
        experience.getStyleClass().add("elegant-field");
        experience.setPromptText("Ex: 2-4 ans");

        TextField competences = new TextField(safe(offre.getCompetencesRequises()));
        competences.getStyleClass().add("elegant-field");
        competences.setPromptText("Java, Spring, SQL...");

        TextField secteur = new TextField(safe(offre.getSecteurActivite()));
        secteur.getStyleClass().add("elegant-field");
        secteur.setPromptText("Ex: Informatique");

        DatePicker pubDate = new DatePicker(
                offre.getDatePublication() == null ? LocalDate.now() : offre.getDatePublication().toLocalDate()
        );
        pubDate.getStyleClass().add("date-picker-elegant");

        DatePicker expDate = new DatePicker(
                offre.getDateExpiration() == null ? LocalDate.now().plusDays(7) : offre.getDateExpiration().toLocalDate()
        );
        expDate.getStyleClass().add("date-picker-elegant");

        // Labels d'erreur (optionnels, mais on les ajoute pour la validation)
        Label errTitre = new Label();
        errTitre.getStyleClass().add("error-label");
        errTitre.setVisible(false);
        errTitre.setManaged(false);

        Label errDescription = new Label();
        errDescription.getStyleClass().add("error-label");
        errDescription.setVisible(false);
        errDescription.setManaged(false);

        Label errSalaire = new Label();
        errSalaire.getStyleClass().add("error-label");
        errSalaire.setVisible(false);
        errSalaire.setManaged(false);

        Label errTypeContrat = new Label();
        errTypeContrat.getStyleClass().add("error-label");
        errTypeContrat.setVisible(false);
        errTypeContrat.setManaged(false);

        Label errLocalisation = new Label();
        errLocalisation.getStyleClass().add("error-label");
        errLocalisation.setVisible(false);
        errLocalisation.setManaged(false);

        Label errExpiration = new Label();
        errExpiration.getStyleClass().add("error-label");
        errExpiration.setVisible(false);
        errExpiration.setManaged(false);

        Label errContact = new Label();
        errContact.getStyleClass().add("error-label");
        errContact.setVisible(false);
        errContact.setManaged(false);

        Label errEntreprise = new Label();
        errEntreprise.getStyleClass().add("error-label");
        errEntreprise.setVisible(false);
        errEntreprise.setManaged(false);

        Label errSecteur = new Label();
        errSecteur.getStyleClass().add("error-label");
        errSecteur.setVisible(false);
        errSecteur.setManaged(false);

        Label errNiveau = new Label();
        errNiveau.getStyleClass().add("error-label");
        errNiveau.setVisible(false);
        errNiveau.setManaged(false);

        Label errExperience = new Label();
        errExperience.getStyleClass().add("error-label");
        errExperience.setVisible(false);
        errExperience.setManaged(false);

        Label errCompetences = new Label();
        errCompetences.getStyleClass().add("error-label");
        errCompetences.setVisible(false);
        errCompetences.setManaged(false);

        // Construction du GridPane
        final int[] row = {0};
        Label headerLabel = new Label("✏️ Modifier l'offre d'emploi");
        headerLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e1133; -fx-padding: 0 0 12 0;");
        grid.add(headerLabel, 0, row[0]++, 2, 1);

        // Fonction utilitaire pour ajouter une ligne
        class FieldRow {
            void add(String labelText, Control field, Label errorLabel) {
                Label lbl = new Label(labelText);
                lbl.getStyleClass().add("field-label");
                VBox vbox = new VBox(6, field, errorLabel);
                grid.add(lbl, 0, row[0]);
                grid.add(vbox, 1, row[0]++);
            }
        }
        FieldRow fieldRow = new FieldRow();

        fieldRow.add("Titre", titre, errTitre);
        fieldRow.add("Description", desc, errDescription);
        fieldRow.add("Salaire (DT)", salaire, errSalaire);
        fieldRow.add("Type contrat", typeContrat, errTypeContrat);
        fieldRow.add("Localisation", localisation, errLocalisation);
        fieldRow.add("Date de publication", pubDate, new Label());
        fieldRow.add("Date d'expiration", expDate, errExpiration);
        fieldRow.add("Niveau", niveau, errNiveau);
        fieldRow.add("Expérience", experience, errExperience);
        fieldRow.add("Compétences", competences, errCompetences);
        fieldRow.add("Secteur", secteur, errSecteur);
        fieldRow.add("Entreprise", entreprise, errEntreprise);
        fieldRow.add("Contact (email)", contact, errContact);

        dialog.getDialogPane().setContent(scrollPane);

        // Validation
        Node okBtn = dialog.getDialogPane().lookupButton(saveBtn);
        okBtn.setDisable(titre.getText().trim().isEmpty());
        titre.textProperty().addListener((obs, o, n) -> okBtn.setDisable(n.trim().isEmpty()));

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;

            // Réinitialiser les erreurs
            List.of(errTitre, errDescription, errSalaire, errTypeContrat, errLocalisation,
                    errExpiration, errContact, errEntreprise, errSecteur, errNiveau,
                    errExperience, errCompetences).forEach(l -> {
                l.setVisible(false);
                l.setManaged(false);
            });
            List.of(titre, desc, salaire, typeContrat, localisation, expDate, contact,
                    entreprise, secteur, niveau, experience, competences).forEach(c -> c.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), false));

            boolean hasError = false;

            // Validations
            if (safe(titre.getText()).isEmpty()) {
                errTitre.setText("Le titre est obligatoire.");
                errTitre.setVisible(true);
                errTitre.setManaged(true);
                titre.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), true);
                hasError = true;
            }
            if (safe(desc.getText()).isEmpty()) {
                errDescription.setText("La description est obligatoire.");
                errDescription.setVisible(true);
                errDescription.setManaged(true);
                desc.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), true);
                hasError = true;
            }
            if (typeContrat.getValue() == null || safe(typeContrat.getValue()).isEmpty()) {
                errTypeContrat.setText("Le type de contrat est obligatoire.");
                errTypeContrat.setVisible(true);
                errTypeContrat.setManaged(true);
                typeContrat.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), true);
                hasError = true;
            }
            if (safe(localisation.getText()).isEmpty()) {
                errLocalisation.setText("La localisation est obligatoire.");
                errLocalisation.setVisible(true);
                errLocalisation.setManaged(true);
                localisation.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), true);
                hasError = true;
            }
            if (safe(entreprise.getText()).isEmpty()) {
                errEntreprise.setText("Le nom de l'entreprise est obligatoire.");
                errEntreprise.setVisible(true);
                errEntreprise.setManaged(true);
                entreprise.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), true);
                hasError = true;
            }
            if (safe(contact.getText()).isEmpty()) {
                errContact.setText("Le contact est obligatoire.");
                errContact.setVisible(true);
                errContact.setManaged(true);
                contact.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), true);
                hasError = true;
            } else if (!isValidEmail(safe(contact.getText()))) {
                errContact.setText("Email invalide.");
                errContact.setVisible(true);
                errContact.setManaged(true);
                contact.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), true);
                hasError = true;
            }

            double sal = 0;
            if (safe(salaire.getText()).isEmpty()) {
                errSalaire.setText("Le salaire est obligatoire.");
                errSalaire.setVisible(true);
                errSalaire.setManaged(true);
                salaire.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), true);
                hasError = true;
            } else {
                try {
                    sal = Double.parseDouble(safe(salaire.getText()));
                    if (sal <= 0) {
                        errSalaire.setText("Le salaire doit être > 0.");
                        errSalaire.setVisible(true);
                        errSalaire.setManaged(true);
                        salaire.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), true);
                        hasError = true;
                    }
                } catch (NumberFormatException e) {
                    errSalaire.setText("Salaire invalide.");
                    errSalaire.setVisible(true);
                    errSalaire.setManaged(true);
                    salaire.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), true);
                    hasError = true;
                }
            }

            if (pubDate.getValue() == null || expDate.getValue() == null) {
                errExpiration.setText("Les dates de publication et expiration sont obligatoires.");
                errExpiration.setVisible(true);
                errExpiration.setManaged(true);
                if (expDate.getValue() == null) expDate.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), true);
                hasError = true;
            } else if (expDate.getValue().isBefore(pubDate.getValue())) {
                errExpiration.setText("L'expiration doit être après la publication.");
                errExpiration.setVisible(true);
                errExpiration.setManaged(true);
                expDate.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), true);
                hasError = true;
            }

            if (safe(secteur.getText()).isEmpty()) {
                errSecteur.setText("Le secteur est obligatoire.");
                errSecteur.setVisible(true);
                errSecteur.setManaged(true);
                secteur.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), true);
                hasError = true;
            }
            if (safe(niveau.getText()).isEmpty()) {
                errNiveau.setText("Le niveau est obligatoire.");
                errNiveau.setVisible(true);
                errNiveau.setManaged(true);
                niveau.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), true);
                hasError = true;
            }
            if (safe(experience.getText()).isEmpty()) {
                errExperience.setText("L'expérience est obligatoire.");
                errExperience.setVisible(true);
                errExperience.setManaged(true);
                experience.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), true);
                hasError = true;
            } else if (!experience.getText().matches("^\\d+(\\s*ans)?$|^\\d+\\s*-\\s*\\d+(\\s*ans)?$")) {
                errExperience.setText("Format expérience invalide (ex: 2, 2-5 ans).");
                errExperience.setVisible(true);
                errExperience.setManaged(true);
                experience.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), true);
                hasError = true;
            }
            if (safe(competences.getText()).isEmpty()) {
                errCompetences.setText("Les compétences sont obligatoires.");
                errCompetences.setVisible(true);
                errCompetences.setManaged(true);
                competences.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), true);
                hasError = true;
            } else if (!competences.getText().contains(",")) {
                errCompetences.setText("Séparez les compétences par des virgules.");
                errCompetences.setVisible(true);
                errCompetences.setManaged(true);
                competences.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), true);
                hasError = true;
            }

            if (hasError) return null;

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
        res.ifPresent(updated -> {
            try {
                offreService.modifier(updated);
                loadData();
                StyledAlert.showSuccess("Succès", "Offre modifiée.");
            } catch (SQLException e) {
                StyledAlert.showError("Erreur", "Modification impossible :\n" + e.getMessage());
            }
        });
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

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }
}