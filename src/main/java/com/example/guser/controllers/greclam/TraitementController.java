package com.example.guser.controllers.greclam;

import entities.greclam.TraitementReclamation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.util.converter.IntegerStringConverter;
import services.greclam.TraitementReclamationService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

public class TraitementController implements Initializable {

    @FXML
    private TableView<TraitementReclamation> traitementTable;
    @FXML
    private TextField filterReclamationField;
    @FXML
    private Label totalLabel;

    private ObservableList<TraitementReclamation> traitementList = FXCollections.observableArrayList();
    private TraitementReclamationService traitementService = new TraitementReclamationService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeTableColumns();
        loadTraitements();

        // Activer l'édition
        traitementTable.setEditable(true);
    }

    @SuppressWarnings("unchecked")
    private void initializeTableColumns() {
        try {
            // Colonne ID (non éditable)
            TableColumn<TraitementReclamation, Integer> idCol =
                    (TableColumn<TraitementReclamation, Integer>) traitementTable.getColumns().get(0);
            idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

            // Colonne Date Traitement (non éditable)
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

            // Colonne Statut Final (éditable avec ComboBox)
            TableColumn<TraitementReclamation, String> statutCol =
                    (TableColumn<TraitementReclamation, String>) traitementTable.getColumns().get(3);
            statutCol.setCellValueFactory(new PropertyValueFactory<>("statutFinal"));
            statutCol.setCellFactory(ComboBoxTableCell.forTableColumn("Résolue", "En cours", "Fermée", "Rejetée"));
            statutCol.setOnEditCommit(event -> {
                TraitementReclamation traitement = event.getRowValue();
                traitement.setStatutFinal(event.getNewValue());
                updateTraitement(traitement);
            });

            // Colorer le statut
            statutCol.setCellFactory(column -> new TableCell<TraitementReclamation, String>() {
                @Override
                protected void updateItem(String statut, boolean empty) {
                    super.updateItem(statut, empty);
                    if (empty || statut == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(statut);
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

            // Colonne Actions (boutons)
            TableColumn<TraitementReclamation, Void> actionCol =
                    (TableColumn<TraitementReclamation, Void>) traitementTable.getColumns().get(6);
            actionCol.setCellFactory(param -> new TableCell<>() {
                private final Button editBtn = new Button("✏️");
                private final Button deleteBtn = new Button("🗑️");
                private final Button detailsBtn = new Button("📋");

                {
                    editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3498db; -fx-cursor: hand;");
                    deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-cursor: hand;");
                    detailsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9b59b6; -fx-cursor: hand;");

                    editBtn.setTooltip(new Tooltip("Modifier"));
                    deleteBtn.setTooltip(new Tooltip("Supprimer"));
                    detailsBtn.setTooltip(new Tooltip("Détails"));

                    editBtn.setOnAction(event -> {
                        TraitementReclamation traitement = getTableView().getItems().get(getIndex());
                        openTraitementForm(traitement);
                    });

                    deleteBtn.setOnAction(event -> {
                        TraitementReclamation traitement = getTableView().getItems().get(getIndex());
                        deleteTraitement(traitement);
                    });

                    detailsBtn.setOnAction(event -> {
                        TraitementReclamation traitement = getTableView().getItems().get(getIndex());
                        showTraitementDetails(traitement);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        HBox buttons = new HBox(5, editBtn, deleteBtn, detailsBtn);
                        buttons.setStyle("-fx-alignment: center;");
                        setGraphic(buttons);
                    }
                }
            });

            traitementTable.setItems(traitementList);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur d'initialisation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateTraitement(TraitementReclamation traitement) {
        try {
            traitementService.update(traitement);
            System.out.println("✅ Traitement #" + traitement.getId() + " mis à jour");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de mise à jour: " + e.getMessage());
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
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement: " + e.getMessage());
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
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de filtrage: " + e.getMessage());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez entrer un ID valide.");
        }
    }

    private void openTraitementForm(TraitementReclamation traitement) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/traitementForm.fxml"));
            Parent root = loader.load();

            TraitementFormController controller = loader.getController();
            controller.setTraitement(traitement);
            controller.setTraitementList(traitementList);

            // Use MainController to set content
            MainController mainController = MainController.getInstance();
            if (mainController != null) {
                mainController.setContent(root);
            } else {
                traitementTable.getScene().setRoot(root);
            }

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showTraitementDetails(TraitementReclamation traitement) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails du traitement");
        alert.setHeaderText("Traitement #" + traitement.getId());

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String dateStr = traitement.getDateTraitement() != null ?
                dateFormat.format(traitement.getDateTraitement()) : "Non définie";

        String content = "═══════════════════════════════════════\n" +
                "📅 Date: " + dateStr + "\n\n" +
                "📝 RÉPONSE ADMIN:\n" + traitement.getReponseAdmin() + "\n\n" +
                "═══════════════════════════════════════\n" +
                "🏷️ Statut Final: " + traitement.getStatutFinal() + "\n" +
                "🆔 Réclamation ID: " + traitement.getReclamationId() + "\n" +
                "👤 Admin ID: " + traitement.getAdminId() + "\n" +
                "═══════════════════════════════════════";

        alert.setContentText(content);
        alert.showAndWait();
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
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Traitement supprimé avec succès.");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de suppression: " + e.getMessage());
            }
        }
    }

    private void updateTotalCount() {
        totalLabel.setText("Total: " + traitementList.size());
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void goBackToReclamations() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/reclamationList.fxml"));
            Parent reclamationList = loader.load();

            MainController mainController = MainController.getInstance();
            if (mainController != null) {
                mainController.setContent(reclamationList);
            } else {
                traitementTable.getScene().setRoot(reclamationList);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retourner à la liste des réclamations");
        }
    }
}