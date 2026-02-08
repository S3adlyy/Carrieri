package com.exemple.grecrutement;

import entities.Mission;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class MissionShellController {

    private static MissionShellController instance;

    @FXML
    private StackPane contentPane;

    public MissionShellController() {
        instance = this;
    }

    public static MissionShellController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        showMissionList();
    }

    // =========================
    // MISSION NAVIGATION
    // =========================

    @FXML
    public void showAddMission() {
        loadView("mission-add.fxml");
    }

    @FXML
    public void showMissionList() {
        loadView("mission-list.fxml");
    }

    public void showEditMission(Mission mission) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("mission-edit.fxml")
            );
            Node view = loader.load();

            MissionEditController controller = loader.getController();
            controller.setMission(mission);

            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // RENDU NAVIGATION
    // =========================

    @FXML
    public void showRenduAdd() {
        loadView("rendu-add.fxml");
    }

    @FXML
    public void showRenduList() {
        loadView("rendu-list.fxml");
    }

    // =========================
    // CORE LOADER
    // =========================

    private void loadView(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Node view = loader.load();
            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadViewWithNode(Node view) {
        contentPane.getChildren().setAll(view);
    }
}
