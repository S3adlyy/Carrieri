package com.exemple.grecrutement;

import entities.Mission;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;  // Change from TextField to TextArea
import javafx.scene.control.TextField;
import services.MissionService;


public class MissionAddController {



    @FXML
    private TextArea descriptionField;  // Change from TextField to TextArea

    @FXML
    private TextField scoreField;

    @FXML
    private TextField createdByIdField;

    private final MissionService missionService = new MissionService();

    @FXML
    private void ajouterMission() {
        try {
            String description = descriptionField.getText();
            int scoreMin = Integer.parseInt(scoreField.getText());
            int creatorId = Integer.parseInt(createdByIdField.getText());

            if (description.isEmpty() || scoreMin < 0 || scoreMin > 100) {
                showAlert(Alert.AlertType.ERROR, "Invalid input", "Please check all fields.");
                return;
            }

            Mission mission = new Mission(description, scoreMin, creatorId);
            missionService.ajouter(mission);

            showAlert(Alert.AlertType.INFORMATION, "Success", "Mission added successfully!");
            clearFields();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid input", "Please enter valid numbers for score and creator ID.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    private void clearFields() {
        descriptionField.clear();
        scoreField.clear();
        createdByIdField.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}