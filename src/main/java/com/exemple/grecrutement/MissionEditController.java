package com.exemple.grecrutement;

import entities.Mission;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import services.MissionService;

public class MissionEditController {

    @FXML
    private TextField descriptionField;

    @FXML
    private TextField scoreField;

    @FXML
    private TextField creatorField;

    private Mission mission;
    private final MissionService missionService = new MissionService();

    public void setMission(Mission mission) {
        this.mission = mission;

        descriptionField.setText(mission.getDescription());
        scoreField.setText(String.valueOf(mission.getScore_min()));
        creatorField.setText(String.valueOf(mission.getCreated_by_id()));
    }

    @FXML
    private void saveMission() {
        try {
            mission.setDescription(descriptionField.getText());
            mission.setScore_min(Integer.parseInt(scoreField.getText()));
            mission.setCreated_by_id(Integer.parseInt(creatorField.getText()));

            missionService.update(mission);

            MissionShellController.getInstance().showMissionList();

        } catch (Exception e) {
            showAlert("Error", e.getMessage());
        }
    }

    @FXML
    private void cancel() {
        MissionShellController.getInstance().showMissionList();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }
}
