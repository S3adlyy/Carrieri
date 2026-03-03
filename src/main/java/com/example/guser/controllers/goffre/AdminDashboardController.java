package com.example.guser.controllers.goffre;

import com.example.guser.controllers.guser.AppNavController;
import entities.goffre.OffreEmploi;
import entities.goffre.Postulation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import services.goffre.OffreEmploiService;
import services.goffre.PostulationService;
import utils.goffre.StyledAlert;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AdminDashboardController {

    // ===== OFFRES TAB =====
    @FXML private TextField txtSearchOffre;
    @FXML private Label lblOffresStatus;
    @FXML private TableView<OffreEmploi> tableOffres;
    @FXML private TableColumn<OffreEmploi, Integer> colOffreId;
    @FXML private TableColumn<OffreEmploi, String> colOffreTitre;
    @FXML private TableColumn<OffreEmploi, String> colOffreDescription;
    @FXML private TableColumn<OffreEmploi, String> colOffreEntreprise;
    @FXML private TableColumn<OffreEmploi, String> colOffreTypeContrat;
    @FXML private TableColumn<OffreEmploi, String> colOffreLocalisation;
    @FXML private TableColumn<OffreEmploi, Double> colOffreSalaire;
    @FXML private TableColumn<OffreEmploi, String> colOffreNiveauQualif;
    @FXML private TableColumn<OffreEmploi, String> colOffreExperience;
    @FXML private TableColumn<OffreEmploi, String> colOffreCompetences;
    @FXML private TableColumn<OffreEmploi, String> colOffreSecteur;
    @FXML private TableColumn<OffreEmploi, String> colOffreContact;
    @FXML private TableColumn<OffreEmploi, String> colOffreDatePub;
    @FXML private TableColumn<OffreEmploi, String> colOffreDateExp;
    @FXML private TableColumn<OffreEmploi, Integer> colOffreRecruteurId;
    @FXML private TableColumn<OffreEmploi, Void> colOffreActions;

    // ===== POSTULATIONS TAB =====
    @FXML private TextField txtSearchPostulation;
    @FXML private Label lblPostulationsStatus;
    @FXML private Button btnFilterPostulation;
    @FXML private TableView<Postulation> tablePostulations;
    @FXML private TableColumn<Postulation, Integer> colPostulationId;
    @FXML private TableColumn<Postulation, Integer> colPostulationOffreId;
    @FXML private TableColumn<Postulation, String> colPostulationOffreTitre;
    @FXML private TableColumn<Postulation, Integer> colPostulationCandidatId;
    @FXML private TableColumn<Postulation, String> colPostulationDate;
    @FXML private TableColumn<Postulation, String> colPostulationStatut;
    @FXML private TableColumn<Postulation, String> colPostulationMotivation;
    @FXML private TableColumn<Postulation, String> colPostulationCvPath;
    @FXML private TableColumn<Postulation, Void> colPostulationActions;

    private final OffreEmploiService offreService = new OffreEmploiService();
    private final PostulationService postulationService = new PostulationService();

    private final ObservableList<OffreEmploi> offresData = FXCollections.observableArrayList();
    private final ObservableList<OffreEmploi> allOffresData = FXCollections.observableArrayList();

    private final ObservableList<Postulation> postulationsData = FXCollections.observableArrayList();
    private final ObservableList<Postulation> allPostulationsData = FXCollections.observableArrayList();

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Cache offreId -> titre offre pour la recherche (évite requêtes DB à chaque frappe)
    private final Map<Integer, String> offreTitreCache = new HashMap<>();

    @FXML
    public void initialize() {
        setupOffresTable();
        setupPostulationsTable();
        loadOffres();
        loadPostulations();
    }

    // ==================== OFFRES METHODS ====================

    private void setupOffresTable() {
        // Bind all columns to properties
        colOffreId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOffreTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colOffreDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colOffreEntreprise.setCellValueFactory(new PropertyValueFactory<>("entreprise"));
        colOffreTypeContrat.setCellValueFactory(new PropertyValueFactory<>("typeContrat"));
        colOffreLocalisation.setCellValueFactory(new PropertyValueFactory<>("localisation"));
        colOffreSalaire.setCellValueFactory(new PropertyValueFactory<>("salaire"));
        colOffreNiveauQualif.setCellValueFactory(new PropertyValueFactory<>("niveauQualification"));
        colOffreExperience.setCellValueFactory(new PropertyValueFactory<>("experienceRequise"));
        colOffreCompetences.setCellValueFactory(new PropertyValueFactory<>("competencesRequises"));
        colOffreSecteur.setCellValueFactory(new PropertyValueFactory<>("secteurActivite"));
        colOffreContact.setCellValueFactory(new PropertyValueFactory<>("contactRecruteur"));

        // Dates - format them
        colOffreDatePub.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDatePublication() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        dateFmt.format(cellData.getValue().getDatePublication())
                );
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });

        colOffreDateExp.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDateExpiration() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        dateFmt.format(cellData.getValue().getDateExpiration())
                );
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });

        // Recruteur ID column (empty for now)
        colOffreRecruteurId.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(null)
        );
        colOffreRecruteurId.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText("-");
                }
            }
        });

        // Make ALL columns editable with validation
        setupEditableTextColumn(colOffreTitre, "titre", true, 3, 100);
        setupEditableTextColumn(colOffreDescription, "description", true, 10, 1000);
        setupEditableTextColumn(colOffreEntreprise, "entreprise", true, 2, 100);
        setupEditableTextColumn(colOffreLocalisation, "localisation", true, 2, 100);
        setupEditableTextColumn(colOffreNiveauQualif, "niveauQualification", true, 2, 100);
        setupEditableTextColumn(colOffreExperience, "experienceRequise", true, 2, 200);
        setupEditableTextColumn(colOffreCompetences, "competencesRequises", true, 3, 500);
        setupEditableTextColumn(colOffreSecteur, "secteurActivite", true, 2, 100);
        setupEditableEmailColumn(colOffreContact, "contactRecruteur");
        setupEditableTextColumn(colOffreTypeContrat, "typeContrat", true, 2, 50);

        // Salaire editable with number validation
        setupEditableDoubleColumn(colOffreSalaire);

        // Actions column
        colOffreActions.setCellFactory(createOffreActionsColumn());

        tableOffres.setItems(offresData);
        tableOffres.setEditable(true);
    }

    private void setupEditableTextColumn(TableColumn<OffreEmploi, String> column, String property,
                                         boolean required, int minLength, int maxLength) {
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setOnEditCommit(event -> {
            OffreEmploi offre = event.getRowValue();
            String newValue = event.getNewValue() != null ? event.getNewValue().trim() : "";

            // Validation: Required
            if (required && newValue.isEmpty()) {
                StyledAlert.showError("❌ Champ obligatoire", "Le champ " + column.getText() + " est obligatoire.");
                tableOffres.refresh();
                return;
            }

            // Validation: Minimum length
            if (required && newValue.length() < minLength) {
                StyledAlert.showError("❌ Longueur insuffisante",
                        "Le champ " + column.getText() + " doit contenir au moins " + minLength + " caractères.\nActuellement: " + newValue.length() + " caractères.");
                tableOffres.refresh();
                return;
            }

            // Validation: Maximum length
            if (!newValue.isEmpty() && newValue.length() > maxLength) {
                StyledAlert.showError("❌ Longueur excessive",
                        "Le champ " + column.getText() + " ne peut pas dépasser " + maxLength + " caractères.\nActuellement: " + newValue.length() + " caractères.");
                tableOffres.refresh();
                return;
            }

            // Validation: Ne peut pas commencer par un chiffre (pour titre et description)
            if ((property.equals("titre") || property.equals("description")) && newValue.matches("^\\d.*")) {
                StyledAlert.showError("❌ Format invalide", "Le champ " + column.getText() + " ne peut pas commencer par un chiffre.");
                tableOffres.refresh();
                return;
            }

            // Update the property
            try {
                switch (property) {
                    case "titre":
                        // Check uniqueness
                        if (!newValue.equals(offre.getTitre()) && offreService.existsByTitre(newValue)) {
                            StyledAlert.showError("❌ Titre existant", "Une offre avec le titre \"" + newValue + "\" existe déjà.");
                            tableOffres.refresh();
                            return;
                        }
                        offre.setTitre(newValue);
                        break;
                    case "description": offre.setDescription(newValue); break;
                    case "entreprise": offre.setEntreprise(newValue); break;
                    case "localisation": offre.setLocalisation(newValue); break;
                    case "niveauQualification": offre.setNiveauQualification(newValue); break;
                    case "experienceRequise": offre.setExperienceRequise(newValue); break;
                    case "competencesRequises": offre.setCompetencesRequises(newValue); break;
                    case "secteurActivite": offre.setSecteurActivite(newValue); break;
                    case "typeContrat": offre.setTypeContrat(newValue); break;
                }

                offreService.modifier(offre);
                StyledAlert.showSuccess("✅ Succès", "Modification enregistrée avec succès.");
                tableOffres.refresh();
            } catch (SQLException e) {
                StyledAlert.showError("❌ Erreur", "Erreur lors de la modification :\n" + e.getMessage());
                tableOffres.refresh();
            }
        });
    }

    private void setupEditableEmailColumn(TableColumn<OffreEmploi, String> column, String property) {
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setOnEditCommit(event -> {
            OffreEmploi offre = event.getRowValue();
            String newValue = event.getNewValue() != null ? event.getNewValue().trim() : "";

            // Validation: Required
            if (newValue.isEmpty()) {
                StyledAlert.showError("❌ Champ obligatoire", "L'adresse email est obligatoire.");
                tableOffres.refresh();
                return;
            }

            // Validation: Email format
            if (!isValidEmail(newValue)) {
                StyledAlert.showError("❌ Format invalide", "Format d'email invalide.\nExemple valide: exemple@domaine.com");
                tableOffres.refresh();
                return;
            }

            // Validation: Length
            if (newValue.length() < 5 || newValue.length() > 100) {
                StyledAlert.showError("❌ Longueur invalide", "L'email doit contenir entre 5 et 100 caractères.");
                tableOffres.refresh();
                return;
            }

            // Update
            try {
                offre.setContactRecruteur(newValue);
                offreService.modifier(offre);
                StyledAlert.showSuccess("✅ Succès", "Email modifié avec succès.");
                tableOffres.refresh();
            } catch (SQLException e) {
                StyledAlert.showError("❌ Erreur", "Erreur lors de la modification :\n" + e.getMessage());
                tableOffres.refresh();
            }
        });
    }

    private void setupEditableDoubleColumn(TableColumn<OffreEmploi, Double> column) {
        column.setCellFactory(col -> new TableCell<OffreEmploi, Double>() {
            private TextField textField;

            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (isEditing()) {
                        if (textField != null) {
                            textField.setText(getString());
                        }
                        setText(null);
                        setGraphic(textField);
                    } else {
                        setText(getString());
                        setGraphic(null);
                    }
                }
            }

            @Override
            public void startEdit() {
                if (!isEmpty()) {
                    super.startEdit();
                    createTextField();
                    setText(null);
                    setGraphic(textField);
                    textField.selectAll();
                    textField.requestFocus();
                }
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getString());
                setGraphic(null);
            }

            private void createTextField() {
                textField = new TextField(getString());
                textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
                textField.setOnAction(evt -> {
                    String value = textField.getText();

                    // Validation: Not empty
                    if (value == null || value.trim().isEmpty()) {
                        StyledAlert.showError("❌ Champ obligatoire", "Le salaire est obligatoire.");
                        cancelEdit();
                        return;
                    }

                    // Validation: Numeric
                    try {
                        double salaire = Double.parseDouble(value.trim());

                        // Validation: Positive
                        if (salaire <= 0) {
                            StyledAlert.showError("❌ Valeur invalide", "Le salaire doit être supérieur à 0.");
                            cancelEdit();
                            return;
                        }

                        // Validation: Reasonable range
                        if (salaire > 1000000) {
                            StyledAlert.showError("❌ Valeur trop élevée", "Le salaire ne peut pas dépasser 1 000 000.");
                            cancelEdit();
                            return;
                        }

                        // Update
                        OffreEmploi offre = getTableView().getItems().get(getIndex());
                        offre.setSalaire(salaire);

                        try {
                            offreService.modifier(offre);
                            commitEdit(salaire);
                            StyledAlert.showSuccess("✅ Succès", "Salaire modifié avec succès.");
                        } catch (SQLException e) {
                            StyledAlert.showError("❌ Erreur", "Erreur lors de la modification :\n" + e.getMessage());
                            cancelEdit();
                        }

                    } catch (NumberFormatException e) {
                        StyledAlert.showError("❌ Format invalide", "Le salaire doit être un nombre valide.\nExemple: 2500.50");
                        cancelEdit();
                    }
                });

                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        cancelEdit();
                    }
                });
            }

            private String getString() {
                return getItem() == null ? "" : String.format("%.2f", getItem());
            }
        });
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    private Callback<TableColumn<OffreEmploi, Void>, TableCell<OffreEmploi, Void>> createOffreActionsColumn() {
        return param -> new TableCell<>() {
            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();

            {
                btnEdit.getStyleClass().add("btn-icon-edit");
                btnEdit.setGraphic(new Label("✏️"));
                btnEdit.setOnAction(e -> {
                    OffreEmploi offre = getTableView().getItems().get(getIndex());
                    handleEditOffre(offre);
                });

                btnDelete.getStyleClass().add("btn-icon-delete");
                btnDelete.setGraphic(new Label("🗑"));
                btnDelete.setOnAction(e -> {
                    OffreEmploi offre = getTableView().getItems().get(getIndex());
                    handleDeleteOffre(offre);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(10, btnEdit, btnDelete);
                    buttons.setAlignment(Pos.CENTER);
                    setGraphic(buttons);
                }
            }
        };
    }

    private void loadOffres() {
        try {
            allOffresData.setAll(offreService.readCandidates());
            offresData.setAll(allOffresData);
            lblOffresStatus.setText(offresData.size() + " offres trouvées");
        } catch (SQLException e) {
            StyledAlert.showError("Erreur de chargement", "Impossible de charger les offres :\n" + e.getMessage());
        }
    }

    @FXML
    private void handleSearchOffre() {
        String search = txtSearchOffre.getText().toLowerCase().trim();

        if (search.isEmpty()) {
            offresData.setAll(allOffresData);
        } else {
            offresData.setAll(allOffresData.filtered(o ->
                    o.getTitre().toLowerCase().contains(search) ||
                            o.getEntreprise().toLowerCase().contains(search) ||
                            o.getLocalisation().toLowerCase().contains(search) ||
                            o.getTypeContrat().toLowerCase().contains(search) ||
                            String.valueOf(o.getSalaire()).contains(search)
            ));
        }

        lblOffresStatus.setText(offresData.size() + " offres trouvées");
    }

    @FXML
    private void handleAddOffre() {
        AppNavController shell = AppNavController.getInstance();
        if (shell != null) {
            shell.showOffreAdd();
        } else {
            StyledAlert.showError("Erreur", "Shell non disponible.");
        }
    }

    private void handleEditOffre(OffreEmploi offre) {
        StyledAlert.showInfo("Édition", "Double-cliquez sur une cellule pour la modifier directement dans le tableau.");
    }

    private void handleDeleteOffre(OffreEmploi offre) {
        try {
            java.util.List<Postulation> postulations = postulationService.afficherParOffre(offre.getId());

            if (!postulations.isEmpty()) {
                StyledAlert.showWarning("Suppression impossible",
                        "Cette offre ne peut pas être supprimée car elle a " + postulations.size() +
                                " postulation(s) associée(s).\n\nSupprimez d'abord les postulations.");
                return;
            }
        } catch (SQLException e) {
            StyledAlert.showError("Erreur", "Erreur lors de la vérification :\n" + e.getMessage());
            return;
        }

        boolean confirmed = StyledAlert.showConfirmation("Confirmer la suppression",
                "Supprimer l'offre : " + offre.getTitre() + " ?");
        if (confirmed) {
            try {
                offreService.supprimer(offre.getRecruiterId());
                loadOffres();
                StyledAlert.showSuccess("Succès", "Offre supprimée avec succès.");
            } catch (SQLException e) {
                StyledAlert.showError("Erreur", "Erreur lors de la suppression :\n" + e.getMessage());
            }
        }
    }

    // ==================== POSTULATIONS METHODS ====================

    private void setupPostulationsTable() {
        colPostulationId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPostulationOffreId.setCellValueFactory(new PropertyValueFactory<>("offreId"));
        colPostulationCandidatId.setCellValueFactory(new PropertyValueFactory<>("candidatId"));

        colPostulationOffreTitre.setCellValueFactory(cellData -> {
            try {
                OffreEmploi offre = offreService.findById(cellData.getValue().getOffreId());
                return new javafx.beans.property.SimpleStringProperty(
                        offre != null ? offre.getTitre() : "N/A"
                );
            } catch (SQLException e) {
                return new javafx.beans.property.SimpleStringProperty("Erreur");
            }
        });

        colPostulationDate.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        dateFmt.format(cellData.getValue().getDatePostulation())
                )
        );

        colPostulationStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colPostulationMotivation.setCellValueFactory(new PropertyValueFactory<>("motivationCandidature"));

        colPostulationCvPath.setCellValueFactory(new PropertyValueFactory<>("cvPath"));
        colPostulationCvPath.setCellFactory(col -> new TableCell<>() {
            private final Button btnOpenCV = new Button();

            {
                btnOpenCV.getStyleClass().add("btn-icon-view");
                btnOpenCV.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; " +
                        "-fx-border-radius: 8; -fx-background-radius: 8; " +
                        "-fx-padding: 5 10; -fx-cursor: hand;");
                btnOpenCV.setOnAction(e -> {
                    Postulation postulation = getTableView().getItems().get(getIndex());
                    handleOpenCV(postulation.getCvPath());
                });
            }

            @Override
            protected void updateItem(String cvPath, boolean empty) {
                super.updateItem(cvPath, empty);
                if (empty || cvPath == null || cvPath.isEmpty()) {
                    setText("Aucun CV");
                    setGraphic(null);
                    setStyle("-fx-text-fill: #9ca3af; -fx-font-style: italic;");
                } else {
                    setText(null);
                    btnOpenCV.setText("📄 Ouvrir CV");
                    setGraphic(btnOpenCV);
                    setStyle("");
                }
            }
        });

        colPostulationMotivation.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String shortened = item.length() > 50 ? item.substring(0, 50) + "..." : item;
                    setText(shortened);
                }
            }
        });

        colPostulationActions.setCellFactory(createPostulationActionsColumn());

        tablePostulations.setItems(postulationsData);
    }

    private Callback<TableColumn<Postulation, Void>, TableCell<Postulation, Void>> createPostulationActionsColumn() {
        return param -> new TableCell<>() {
            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();

            {
                btnEdit.getStyleClass().add("btn-icon-edit");
                btnEdit.setGraphic(new Label("✏️"));
                btnEdit.setOnAction(e -> {
                    Postulation postulation = getTableView().getItems().get(getIndex());
                    handleEditPostulation(postulation);
                });

                btnDelete.getStyleClass().add("btn-icon-delete");
                btnDelete.setGraphic(new Label("🗑"));
                btnDelete.setOnAction(e -> {
                    Postulation postulation = getTableView().getItems().get(getIndex());
                    handleDeletePostulation(postulation);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(10, btnEdit, btnDelete);
                    buttons.setAlignment(Pos.CENTER);
                    setGraphic(buttons);
                }
            }
        };
    }

    private void handleEditPostulation(Postulation postulation) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Modifier le statut");
        dialog.setHeaderText("Postulation ID: " + postulation.getId() +
                "\nCandidat: " + postulation.getCandidatId());

        dialog.getDialogPane().getStylesheets().addAll(
                getClass().getResource("/com/example/guser/goffre/css/theme-unified.css").toExternalForm(),
                getClass().getResource("/com/example/guser/goffre/css/theme-dark.css").toExternalForm()
        );
        dialog.getDialogPane().getStyleClass().add("glass-card");

        ButtonType confirmButtonType = new ButtonType("Modifier", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        // Styliser les boutons manuellement
        Node okButton = dialog.getDialogPane().lookupButton(confirmButtonType);
        if (okButton != null) {
            okButton.getStyleClass().addAll("button", "btn-primary");
        }
        Node cancelButton = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.getStyleClass().addAll("button", "btn-secondary");
        }

        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20;");

        Label label = new Label("Nouveau statut :");
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        ComboBox<String> statutCombo = new ComboBox<>();
        statutCombo.getItems().addAll("En attente", "En cours", "Acceptée", "Refusée");
        statutCombo.setValue(postulation.getStatut());
        statutCombo.setStyle("-fx-min-width: 200; -fx-pref-width: 200;");

        content.getChildren().addAll(label, statutCombo);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return statutCombo.getValue();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newStatut -> {
            if (!newStatut.equals(postulation.getStatut())) {
                try {
                    postulationService.changerStatut(postulation.getId(), newStatut);
                    loadPostulations();
                    StyledAlert.showSuccess("✅ Succès", "Statut modifié avec succès.");
                } catch (SQLException e) {
                    StyledAlert.showError("❌ Erreur", "Erreur lors de la modification :\n" + e.getMessage());
                }
            }
        });
    }

    private void loadPostulations() {
        try {
            allPostulationsData.setAll(postulationService.readCandidates());
            postulationsData.setAll(allPostulationsData);

            rebuildOffreTitreCache(); // <-- IMPORTANT

            lblPostulationsStatus.setText(postulationsData.size() + " postulations trouvées");
        } catch (SQLException e) {
            StyledAlert.showError("Erreur de chargement", "Impossible de charger les postulations :\n" + e.getMessage());
        }
    }
    private void rebuildOffreTitreCache() {
        offreTitreCache.clear();

        for (Postulation p : allPostulationsData) {
            int offreId = p.getOffreId();
            if (offreTitreCache.containsKey(offreId)) continue;

            try {
                OffreEmploi offre = offreService.findById(offreId);
                String titre = (offre != null && offre.getTitre() != null) ? offre.getTitre() : "";
                offreTitreCache.put(offreId, titre);
            } catch (SQLException e) {
                offreTitreCache.put(offreId, "");
            }
        }
    }
    @FXML
    private void handleSearchPostulation() {
        String search = txtSearchPostulation.getText() == null
                ? ""
                : txtSearchPostulation.getText().toLowerCase().trim();

        if (search.isEmpty()) {
            postulationsData.setAll(allPostulationsData);
        } else {
            postulationsData.setAll(allPostulationsData.filtered(p -> {
                String titreOffre = offreTitreCache.getOrDefault(p.getOffreId(), "");
                String motivation = p.getMotivationCandidature() == null ? "" : p.getMotivationCandidature();
                String statut = p.getStatut() == null ? "" : p.getStatut();
                String dateStr = p.getDatePostulation() == null ? "" : p.getDatePostulation().toString();

                return String.valueOf(p.getId()).contains(search)
                        || String.valueOf(p.getOffreId()).contains(search)
                        || String.valueOf(p.getCandidatId()).contains(search)
                        || statut.toLowerCase().contains(search)
                        || motivation.toLowerCase().contains(search)
                        || dateStr.contains(search)
                        || titreOffre.toLowerCase().contains(search); // ✅ Titre Offre maintenant
            }));
        }

        lblPostulationsStatus.setText(postulationsData.size() + " postulations trouvées");
    }

    @FXML
    private void handleFilterPostulation() {
        ContextMenu filterMenu = new ContextMenu();

        MenuItem allItem = new MenuItem("Toutes les postulations");
        allItem.setOnAction(e -> {
            txtSearchPostulation.clear();
            loadPostulations();
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

        if (btnFilterPostulation != null) {
            filterMenu.show(btnFilterPostulation, javafx.geometry.Side.BOTTOM, 0, 0);
        }
    }

    private void filterByStatut(String statut) {
        postulationsData.setAll(allPostulationsData.filtered(p ->
                p.getStatut().equalsIgnoreCase(statut)
        ));
        lblPostulationsStatus.setText(postulationsData.size() + " postulations (" + statut + ")");
    }

    private void handleDeletePostulation(Postulation postulation) {
        boolean confirmed = StyledAlert.showConfirmation("Confirmer la suppression",
                "Supprimer la postulation ID " + postulation.getId() + " ?");
        if (confirmed) {
            try {
                postulationService.supprimer(postulation.getId());
                loadPostulations();
                StyledAlert.showSuccess("Succès", "Postulation supprimée avec succès.");
            } catch (SQLException e) {
                StyledAlert.showError("Erreur", "Erreur lors de la suppression :\n" + e.getMessage());
            }
        }
    }

    private void handleOpenCV(String cvPath) {
        if (cvPath == null || cvPath.isEmpty()) {
            StyledAlert.showError("❌ CV absent", "Aucun CV disponible pour cette postulation.");
            return;
        }

        try {
            java.io.File cvFile = new java.io.File(cvPath);

            if (!cvFile.exists()) {
                StyledAlert.showError("❌ Fichier introuvable",
                        "Le fichier CV n'existe pas :\n" + cvPath +
                                "\n\nIl a peut-être été supprimé ou déplacé.");
                return;
            }

            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
                    desktop.open(cvFile);
                    StyledAlert.showInfo("Ouverture du CV", "Ouverture de : " + cvFile.getName());
                } else {
                    StyledAlert.showError("❌ Action non supportée", "L'ouverture de fichiers n'est pas supportée sur ce système.");
                }
            } else {
                StyledAlert.showError("❌ Desktop API non disponible", "L'ouverture de fichiers n'est pas supportée.");
            }
        } catch (java.io.IOException e) {
            StyledAlert.showError("❌ Erreur d'ouverture", "Erreur lors de l'ouverture du CV :\n" + e.getMessage());
        } catch (Exception e) {
            StyledAlert.showError("❌ Erreur inattendue", e.getMessage());
        }
    }
}