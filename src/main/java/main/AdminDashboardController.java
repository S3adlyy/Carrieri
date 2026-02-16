package main;

import entities.OffreEmploi;
import entities.Postulation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import services.OffreEmploiService;
import services.PostulationService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
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
    @FXML private TableColumn<Postulation, Void> colPostulationActions;

    private final OffreEmploiService offreService = new OffreEmploiService();
    private final PostulationService postulationService = new PostulationService();

    private final ObservableList<OffreEmploi> offresData = FXCollections.observableArrayList();
    private final ObservableList<OffreEmploi> allOffresData = FXCollections.observableArrayList();

    private final ObservableList<Postulation> postulationsData = FXCollections.observableArrayList();
    private final ObservableList<Postulation> allPostulationsData = FXCollections.observableArrayList();

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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
                showError("❌ Le champ " + column.getText() + " est obligatoire");
                tableOffres.refresh();
                return;
            }

            // Validation: Minimum length
            if (required && newValue.length() < minLength) {
                showError("❌ Le champ " + column.getText() + " doit contenir au moins " + minLength + " caractères\nActuellement: " + newValue.length() + " caractères");
                tableOffres.refresh();
                return;
            }

            // Validation: Maximum length
            if (!newValue.isEmpty() && newValue.length() > maxLength) {
                showError("❌ Le champ " + column.getText() + " ne peut pas dépasser " + maxLength + " caractères\nActuellement: " + newValue.length() + " caractères");
                tableOffres.refresh();
                return;
            }

            // Update the property
            try {
                switch (property) {
                    case "titre":
                        // Check uniqueness
                        if (!newValue.equals(offre.getTitre()) && offreService.existsByTitre(newValue)) {
                            showError("❌ Une offre avec le titre \"" + newValue + "\" existe déjà");
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
                showInfo("✅ Modification enregistrée avec succès");
                tableOffres.refresh();
            } catch (SQLException e) {
                showError("❌ Erreur: " + e.getMessage());
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
                showError("❌ L'adresse email est obligatoire");
                tableOffres.refresh();
                return;
            }

            // Validation: Email format
            if (!isValidEmail(newValue)) {
                showError("❌ Format d'email invalide\nExemple valide: exemple@domaine.com");
                tableOffres.refresh();
                return;
            }

            // Validation: Length
            if (newValue.length() < 5 || newValue.length() > 100) {
                showError("❌ L'email doit contenir entre 5 et 100 caractères");
                tableOffres.refresh();
                return;
            }

            // Update
            try {
                offre.setContactRecruteur(newValue);
                offreService.modifier(offre);
                showInfo("✅ Email modifié avec succès");
                tableOffres.refresh();
            } catch (SQLException e) {
                showError("❌ Erreur: " + e.getMessage());
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
                        showError("❌ Le salaire est obligatoire");
                        cancelEdit();
                        return;
                    }

                    // Validation: Numeric
                    try {
                        double salaire = Double.parseDouble(value.trim());

                        // Validation: Positive
                        if (salaire <= 0) {
                            showError("❌ Le salaire doit être supérieur à 0");
                            cancelEdit();
                            return;
                        }

                        // Validation: Reasonable range
                        if (salaire > 1000000) {
                            showError("❌ Le salaire ne peut pas dépasser 1 000 000");
                            cancelEdit();
                            return;
                        }

                        // Update
                        OffreEmploi offre = getTableView().getItems().get(getIndex());
                        offre.setSalaire(salaire);

                        try {
                            offreService.modifier(offre);
                            commitEdit(salaire);
                            showInfo("✅ Salaire modifié avec succès");
                        } catch (SQLException e) {
                            showError("❌ Erreur: " + e.getMessage());
                            cancelEdit();
                        }

                    } catch (NumberFormatException e) {
                        showError("❌ Le salaire doit être un nombre valide\nExemple: 2500.50");
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
            allOffresData.setAll(offreService.read());
            offresData.setAll(allOffresData);
            lblOffresStatus.setText(offresData.size() + " offres trouvées");
        } catch (SQLException e) {
            showError("Erreur lors du chargement des offres: " + e.getMessage());
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
        // Redirect to add offre view
        OffresShellController shell = OffresShellController.getInstance();
        if (shell != null) {
            shell.showOffreAdd();
        }
    }

    private void handleEditOffre(OffreEmploi offre) {
        // Inline editing is already enabled via setEditable(true)
        showInfo("Double-cliquez sur une cellule pour la modifier");
    }

    private void handleDeleteOffre(OffreEmploi offre) {
        // Vérifier s'il y a des postulations liées
        try {
            java.util.List<Postulation> postulations = postulationService.afficherParOffre(offre.getId());

            if (!postulations.isEmpty()) {
                Alert warningAlert = new Alert(Alert.AlertType.WARNING);
                warningAlert.setTitle("Impossible de supprimer");
                warningAlert.setHeaderText("Cette offre ne peut pas être supprimée");
                warningAlert.setContentText("Il existe " + postulations.size() +
                    " postulation(s) liée(s) à cette offre.\n\n" +
                    "Vous devez d'abord supprimer toutes les postulations associées.");
                warningAlert.showAndWait();
                return;
            }
        } catch (SQLException e) {
            showError("Erreur lors de la vérification: " + e.getMessage());
            return;
        }

        // Si pas de postulations, procéder à la suppression
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer suppression");
        confirm.setHeaderText("Supprimer cette offre ?");
        confirm.setContentText("Titre: " + offre.getTitre() + "\nEntreprise: " + offre.getEntreprise());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                offreService.supprimer(offre.getId());
                loadOffres();
                showInfo("✅ Offre supprimée avec succès");
            } catch (SQLException e) {
                showError("❌ Erreur lors de la suppression: " + e.getMessage());
            }
        }
    }


    // ==================== POSTULATIONS METHODS ====================

    private void setupPostulationsTable() {
        colPostulationId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPostulationOffreId.setCellValueFactory(new PropertyValueFactory<>("offreId"));
        colPostulationCandidatId.setCellValueFactory(new PropertyValueFactory<>("candidatId"));

        // Offre titre (fetch from service)
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

        // Make motivation column wrap text
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

        // Actions column
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
        // Create a dialog to edit the status
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Modifier le statut");
        dialog.setHeaderText("Postulation ID: " + postulation.getId() +
                           "\nCandidat: " + postulation.getCandidatId());

        // Set the button types
        ButtonType confirmButtonType = new ButtonType("Modifier", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        // Create the status choice
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

        // Convert the result
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
                    showInfo("✅ Statut modifié avec succès");
                } catch (SQLException e) {
                    showError("❌ Erreur lors de la modification: " + e.getMessage());
                }
            }
        });
    }

    private void loadPostulations() {
        try {
            allPostulationsData.setAll(postulationService.read());
            postulationsData.setAll(allPostulationsData);
            lblPostulationsStatus.setText(postulationsData.size() + " postulations trouvées");
        } catch (SQLException e) {
            showError("Erreur lors du chargement des postulations: " + e.getMessage());
        }
    }

    @FXML
    private void handleSearchPostulation() {
        String search = txtSearchPostulation.getText().toLowerCase().trim();

        if (search.isEmpty()) {
            postulationsData.setAll(allPostulationsData);
        } else {
            postulationsData.setAll(allPostulationsData.filtered(p ->
                String.valueOf(p.getId()).contains(search) ||
                String.valueOf(p.getOffreId()).contains(search) ||
                String.valueOf(p.getCandidatId()).contains(search) ||
                p.getStatut().toLowerCase().contains(search) ||
                p.getMotivationCandidature().toLowerCase().contains(search) ||
                p.getDatePostulation().toString().contains(search)
            ));
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
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer suppression");
        confirm.setHeaderText("Supprimer cette postulation ?");
        confirm.setContentText("ID: " + postulation.getId() + " - Candidat: " + postulation.getCandidatId());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                postulationService.supprimer(postulation.getId());
                loadPostulations();
                showInfo("Postulation supprimée avec succès");
            } catch (SQLException e) {
                showError("Erreur lors de la suppression: " + e.getMessage());
            }
        }
    }

    // ==================== UTILITY METHODS ====================

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

