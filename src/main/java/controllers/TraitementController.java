package controllers;

import entities.TraitementReclamation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.util.converter.IntegerStringConverter;
import services.TraitementReclamationService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

public class TraitementController implements Initializable {

    @FXML private TableView<TraitementReclamation> traitementTable;
    @FXML private TextField filterReclamationField;
    @FXML private Label totalLabel;

    private ObservableList<TraitementReclamation> traitementList = FXCollections.observableArrayList();
    private TraitementReclamationService traitementService = new TraitementReclamationService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeTableColumns();
        loadTraitements();
    }

    @SuppressWarnings("unchecked")
    private void initializeTableColumns() {
        try {
            // Colonne ID
            TableColumn<TraitementReclamation, Integer> idCol =
                    (TableColumn<TraitementReclamation, Integer>) traitementTable.getColumns().get(0);
            idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

            // Colonne Date Traitement
            TableColumn<TraitementReclamation, Date> dateCol =
                    (TableColumn<TraitementReclamation, Date>) traitementTable.getColumns().get(1);
            dateCol.setCellValueFactory(new PropertyValueFactory<>("dateTraitement"));
            dateCol.setCellFactory(column -> new TableCell<TraitementReclamation, Date>() {
                private final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");

                @Override
                protected void updateItem(Date date, boolean empty) {
                    super.updateItem(date, empty);
                    if (empty || date == null) {
                        setText(null);
                    } else {
                        setText(format.format(date));
                    }
                }
            });

            // Colonne Réponse Admin (éditable)
            TableColumn<TraitementReclamation, String> reponseCol =
                    (TableColumn<TraitementReclamation, String>) traitementTable.getColumns().get(2);
            reponseCol.setCellValueFactory(new PropertyValueFactory<>("reponseAdmin"));
            reponseCol.setCellFactory(TextFieldTableCell.forTableColumn());
            reponseCol.setOnEditCommit(event -> {
                TraitementReclamation traitement = event.getRowValue();
                traitement.setReponseAdmin(event.getNewValue());
                updateTraitement(traitement);
            });

            // Colonne Statut Final (éditable avec ComboBox) - VERSION SIMPLIFIÉE
            TableColumn<TraitementReclamation, String> statutCol =
                    (TableColumn<TraitementReclamation, String>) traitementTable.getColumns().get(3);
            statutCol.setCellValueFactory(new PropertyValueFactory<>("statutFinal"));

            // Créer une ComboBox pour l'édition
            ObservableList<String> statuts = FXCollections.observableArrayList(
                    "Résolue", "En cours", "Fermée", "Rejetée"
            );

            statutCol.setCellFactory(column -> new TableCell<TraitementReclamation, String>() {
                private final ComboBox<String> comboBox = new ComboBox<>(statuts);

                {
                    comboBox.setOnAction(event -> {
                        TraitementReclamation traitement = getTableView().getItems().get(getIndex());
                        traitement.setStatutFinal(comboBox.getValue());
                        updateTraitement(traitement);
                        setText(comboBox.getValue());
                        setGraphic(null);
                    });
                }

                @Override
                protected void updateItem(String statut, boolean empty) {
                    super.updateItem(statut, empty);
                    if (empty || statut == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(statut);
                        setGraphic(null);

                        // Colorer le texte selon le statut
                        switch (statut) {
                            case "Résolue":
                                setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                                break;
                            case "En cours":
                                setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                                break;
                            case "Fermée":
                                setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
                                break;
                            default:
                                setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        }
                    }
                }

                @Override
                public void startEdit() {
                    super.startEdit();
                    TraitementReclamation traitement = getTableView().getItems().get(getIndex());
                    if (traitement != null) {
                        comboBox.setValue(getItem());
                        setText(null);
                        setGraphic(comboBox);
                    }
                }

                @Override
                public void cancelEdit() {
                    super.cancelEdit();
                    setText(getItem());
                    setGraphic(null);
                }
            });

            // Colonne Réclamation ID (éditable)
            TableColumn<TraitementReclamation, Integer> recIdCol =
                    (TableColumn<TraitementReclamation, Integer>) traitementTable.getColumns().get(4);
            recIdCol.setCellValueFactory(new PropertyValueFactory<>("reclamationId"));
            recIdCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
            recIdCol.setOnEditCommit(event -> {
                TraitementReclamation traitement = event.getRowValue();
                traitement.setReclamationId(event.getNewValue());
                updateTraitement(traitement);
            });

            // Colonne Admin ID (éditable)
            TableColumn<TraitementReclamation, Integer> adminIdCol =
                    (TableColumn<TraitementReclamation, Integer>) traitementTable.getColumns().get(5);
            adminIdCol.setCellValueFactory(new PropertyValueFactory<>("adminId"));
            adminIdCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
            adminIdCol.setOnEditCommit(event -> {
                TraitementReclamation traitement = event.getRowValue();
                traitement.setAdminId(event.getNewValue());
                updateTraitement(traitement);
            });

            // Colonne Actions
            TableColumn<TraitementReclamation, Void> actionCol =
                    (TableColumn<TraitementReclamation, Void>) traitementTable.getColumns().get(6);
            actionCol.setCellFactory(param -> new TableCell<>() {
                private final Button deleteBtn = new Button("🗑️");

                {
                    deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-cursor: hand;");
                    deleteBtn.setTooltip(new Tooltip("Supprimer"));

                    deleteBtn.setOnAction(event -> {
                        TraitementReclamation traitement = getTableView().getItems().get(getIndex());
                        deleteTraitement(traitement);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(deleteBtn);
                    }
                }
            });

            // Activer l'édition
            traitementTable.setEditable(true);
            traitementTable.setItems(traitementList);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur d'initialisation", e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateTraitement(TraitementReclamation traitement) {
        try {
            traitementService.update(traitement);
            System.out.println("✅ Traitement #" + traitement.getId() + " mis à jour");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de mise à jour", e.getMessage());
            loadTraitements();
        }
    }

    @FXML
    private void loadTraitements() {
        try {
            traitementList.clear();
            traitementList.addAll(traitementService.read());
            updateTotalCount();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement", e.getMessage());
        }
    }

    @FXML
    private void showAddTraitementForm() {
        openTraitementForm(null);
    }

    @FXML
    private void filterByReclamation() {
        String filterText = filterReclamationField.getText().trim();
        if (filterText.isEmpty()) {
            loadTraitements();
            return;
        }

        try {
            int reclamationId = Integer.parseInt(filterText);
            traitementList.setAll(traitementService.getByReclamationId(reclamationId));
            updateTotalCount();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de filtrage", e.getMessage());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Format invalide",
                    "Veuillez entrer un ID valide.");
        }
    }

    private void deleteTraitement(TraitementReclamation traitement) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le traitement");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer ce traitement ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                traitementService.supprimer(traitement.getId());
                traitementList.remove(traitement);
                updateTotalCount();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Traitement supprimé",
                        "Le traitement a été supprimé avec succès.");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de suppression", e.getMessage());
            }
        }
    }

    private void openTraitementForm(TraitementReclamation traitement) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/traitementForm.fxml"));
            DialogPane dialogPane = loader.load();

            TraitementFormController controller = loader.getController();
            controller.setTraitement(traitement);
            controller.setTraitementList(traitementList);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle(traitement == null ? "Nouveau traitement" : "Modifier le traitement");

            dialog.showAndWait();
            loadTraitements();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur d'interface",
                    "Impossible d'ouvrir le formulaire: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateTotalCount() {
        totalLabel.setText("Total: " + traitementList.size());
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}