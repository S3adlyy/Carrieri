package com.exemple.grecrutement;

import entities.Mission;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import services.MissionService;
import utils.AlertUtils;

public class MissionEditController {

    @FXML private TextField descriptionField;
    @FXML private TextField scoreField;
    @FXML private TextField creatorField;

    private Mission mission;
    private final MissionService missionService = new MissionService();

    public void setMission(Mission mission) {
        this.mission = mission;

        descriptionField.setText(mission.getDescription());
        scoreField.setText(String.valueOf(mission.getScore_min()));
        creatorField.setText(String.valueOf(mission.getCreated_by_id()));

        // Add input validation for score field
        scoreField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) {
                if (!newText.isEmpty()) {
                    int value = Integer.parseInt(newText);
                    if (value >= 0 && value <= 100) {
                        return change;
                    }
                } else {
                    return change;
                }
            }
            return null;
        }));
    }

    @FXML
    private void saveMission() {
        try {
            // Validation
            if (descriptionField.getText().trim().isEmpty()) {
                AlertUtils.showWarning("Validation Error", "Description cannot be empty!");
                return;
            }

            int score;
            try {
                score = Integer.parseInt(scoreField.getText());
                if (score < 0 || score > 100) {
                    AlertUtils.showWarning("Validation Error", "Score must be between 0 and 100!");
                    return;
                }
            } catch (NumberFormatException e) {
                AlertUtils.showWarning("Validation Error", "Score must be a valid number!");
                return;
            }

            int creatorId;
            try {
                creatorId = Integer.parseInt(creatorField.getText());
                if (creatorId <= 0) {
                    AlertUtils.showWarning("Validation Error", "Creator ID must be positive!");
                    return;
                }
            } catch (NumberFormatException e) {
                AlertUtils.showWarning("Validation Error", "Creator ID must be a valid number!");
                return;
            }

            // Update mission
            mission.setDescription(descriptionField.getText());
            mission.setScore_min(score);
            mission.setCreated_by_id(creatorId);

            missionService.update(mission);

            // Show success message
            AlertUtils.showSuccess("Success", "Mission #" + mission.getId() + " has been successfully updated!");

            MissionShellController.getInstance().showMissionList();

        } catch (Exception e) {
            AlertUtils.showError("Error", e.getMessage());
        }
    }

    @FXML
    private void cancel() {
        MissionShellController.getInstance().showMissionList();
    }
}