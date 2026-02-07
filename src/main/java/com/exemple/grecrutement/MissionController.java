package com.exemple.grecrutement;

import entities.Mission;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import services.MissionService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class MissionController implements Initializable {

    @FXML
    private TableView<Mission> missionTable;

    @FXML
    private TableColumn<Mission, Integer> idColumn;

    @FXML
    private TableColumn<Mission, String> descriptionColumn;

    @FXML
    private TableColumn<Mission, Integer> scoreColumn;

    @FXML
    private TableColumn<Mission, String> dateColumn;

    @FXML
    private TableColumn<Mission, Integer> createdByColumn;

    @FXML
    private TextField idField;

    @FXML
    private TextField descriptionField;

    @FXML
    private TextField scoreField;

    @FXML
    private TextField createdByIdField;

    private MissionService missionService;
    private ObservableList<Mission> missionList;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            missionService = new MissionService();
            missionList = FXCollections.observableArrayList();

            // Initialize table columns if they exist
            if (idColumn != null) {
                idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
            }
            if (descriptionColumn != null) {
                descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
            }
            if (scoreColumn != null) {
                scoreColumn.setCellValueFactory(new PropertyValueFactory<>("score_min"));
            }
            if (dateColumn != null) {
                // Custom cell value factory for date
                dateColumn.setCellValueFactory(cellData -> {
                    Mission mission = cellData.getValue();
                    LocalDateTime date = mission.getCreated_at();
                    if (date != null) {
                        return new javafx.beans.property.SimpleStringProperty(date.format(dateFormatter));
                    } else {
                        return new javafx.beans.property.SimpleStringProperty("");
                    }
                });
            }
            if (createdByColumn != null) {
                createdByColumn.setCellValueFactory(new PropertyValueFactory<>("created_by_id"));
            }

            // Load missions
            chargerMissions();

            // Add listener for table selection
            if (missionTable != null) {
                missionTable.getSelectionModel().selectedItemProperty().addListener(
                        (observable, oldValue, newValue) -> selectionnerMission(newValue)
                );
            }

        } catch (Exception e) {
            showAlert("Erreur", "Erreur d'initialisation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void chargerMissions() {
        try {
            missionList.clear();
            missionList.addAll(missionService.read());
            missionTable.setItems(missionList);
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement des missions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void selectionnerMission(Mission mission) {
        if (mission != null) {
            idField.setText(String.valueOf(mission.getId()));
            descriptionField.setText(mission.getDescription());
            scoreField.setText(String.valueOf(mission.getScore_min()));
            createdByIdField.setText(mission.getCreated_by_id() != null ?
                    mission.getCreated_by_id().toString() : "");
        }
    }

    @FXML
    private void ajouterMission() {
        try {
            String description = descriptionField.getText().trim();
            String scoreText = scoreField.getText().trim();
            String createdByIdText = createdByIdField.getText().trim();

            // Validation
            if (description.isEmpty()) {
                showAlert("Validation", "La description est requise!");
                return;
            }

            if (scoreText.isEmpty()) {
                showAlert("Validation", "Le score minimum est requis!");
                return;
            }

            int score;
            try {
                score = Integer.parseInt(scoreText);
                if (score < 0) {
                    showAlert("Validation", "Le score doit être positif!");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Erreur", "Le score doit être un nombre valide!");
                return;
            }

            Integer createdById = null;
            if (!createdByIdText.isEmpty()) {
                try {
                    createdById = Integer.parseInt(createdByIdText);
                } catch (NumberFormatException e) {
                    showAlert("Erreur", "L'ID créateur doit être un nombre valide!");
                    return;
                }
            }

            Mission mission = new Mission(description, score, createdById);
            missionService.ajouter(mission);

            showAlert("Succès", "Mission ajoutée avec succès!");
            viderChamps();
            chargerMissions();

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de l'ajout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void modifierMission() {
        try {
            String idText = idField.getText().trim();
            if (idText.isEmpty()) {
                showAlert("Validation", "Veuillez sélectionner une mission à modifier!");
                return;
            }

            int id = Integer.parseInt(idText);
            String description = descriptionField.getText().trim();
            String scoreText = scoreField.getText().trim();
            String createdByIdText = createdByIdField.getText().trim();

            if (description.isEmpty()) {
                showAlert("Validation", "La description est requise!");
                return;
            }

            if (scoreText.isEmpty()) {
                showAlert("Validation", "Le score minimum est requis!");
                return;
            }

            int score = Integer.parseInt(scoreText);
            Integer createdById = createdByIdText.isEmpty() ? null : Integer.parseInt(createdByIdText);

            // Get existing mission to preserve creation date
            Mission existingMission = missionService.getById(id);
            if (existingMission == null) {
                showAlert("Erreur", "Mission introuvable!");
                return;
            }

            Mission mission = new Mission(id, description, score, existingMission.getCreated_at(), createdById);
            missionService.update(mission);

            showAlert("Succès", "Mission modifiée avec succès!");
            viderChamps();
            chargerMissions();

        } catch (NumberFormatException e) {
            showAlert("Erreur", "Veuillez entrer des nombres valides!");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de la modification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void supprimerMission() {
        try {
            String idText = idField.getText().trim();
            if (idText.isEmpty()) {
                showAlert("Validation", "Veuillez sélectionner une mission à supprimer!");
                return;
            }

            int id = Integer.parseInt(idText);

            // Confirmation
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirmation");
            confirmation.setHeaderText("Supprimer la mission");
            confirmation.setContentText("Êtes-vous sûr de vouloir supprimer cette mission?");

            if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                missionService.supprimer(id);
                showAlert("Succès", "Mission supprimée avec succès!");
                viderChamps();
                chargerMissions();
            }

        } catch (NumberFormatException e) {
            showAlert("Erreur", "ID invalide!");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de la suppression: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void viderChamps() {
        idField.clear();
        descriptionField.clear();
        scoreField.clear();
        createdByIdField.clear();
        if (missionTable != null) {
            missionTable.getSelectionModel().clearSelection();
        }
    }

    @FXML
    private void actualiserTableau() {
        chargerMissions();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void ouvrirEvaluationAI() {
        try {
            // Load the AI evaluation interface
            FXMLLoader loader = new FXMLLoader(getClass().getResource("rendu-view.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Évaluation AI - Gestion Recrutement");
            stage.setScene(new Scene(root, 1200, 700));
            stage.show();

            // Optional: Close current mission window if you want
            // ((Stage) missionTable.getScene().getWindow()).close();

        } catch (IOException e) {
            e.printStackTrace();
            afficherAlert("Erreur", "Impossible d'ouvrir l'interface d'évaluation AI: " + e.getMessage());
        }
    }

    // Helper method to show alerts
    private void afficherAlert(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}