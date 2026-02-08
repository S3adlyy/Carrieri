package com.exemple.grecrutement;

import entities.Mission;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import services.MissionService;

import java.net.URL;
import java.util.ResourceBundle;

public class MissionListController implements Initializable {

    @FXML
    private TableView<Mission> missionTable;

    @FXML
    private TableColumn<Mission, Integer> idColumn;

    @FXML
    private TableColumn<Mission, String> descriptionColumn;

    @FXML
    private TableColumn<Mission, Integer> scoreColumn;

    @FXML
    private TableColumn<Mission, Integer> creatorColumn;

    private final MissionService missionService = new MissionService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configureColumns();
        loadMissions();
    }

    private void configureColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("score_min"));
        creatorColumn.setCellValueFactory(new PropertyValueFactory<>("created_by_id"));
    }

    private void loadMissions() {
        try {
            missionTable.setItems(
                    FXCollections.observableArrayList(missionService.read())
            );
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Load Error", e.getMessage());
        }
    }

    @FXML
    private void deleteMission() {
        Mission selected = missionTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a mission.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Mission ID " + selected.getId());
        confirm.setContentText("This action cannot be undone.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                missionService.supprimer(selected.getId());
                loadMissions();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Delete Error", e.getMessage());
            }
        }
    }

    @FXML
    private void editMission() {
        Mission selected = missionTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a mission.");
            return;
        }

        MissionShellController.getInstance().showEditMission(selected);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
