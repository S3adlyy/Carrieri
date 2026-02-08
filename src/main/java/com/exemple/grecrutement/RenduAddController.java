package com.exemple.grecrutement;

import entities.RenduMission;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import services.RenduMissionService;

import java.net.URL;
import java.util.ResourceBundle;

public class RenduAddController implements Initializable {

    @FXML private TextField txtCandidatId;
    @FXML private TextField txtMissionId;
    @FXML private TextArea txtCode;
    @FXML private ComboBox<String> comboMissionType;
    @FXML private ProgressIndicator progress;
    @FXML private Label lblResultat;

    private RenduMissionService service;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        service = new RenduMissionService();
        comboMissionType.getItems().addAll(
                "ADDITION", "FACTORIAL", "FIBONACCI", "PRIME_CHECK"
        );
        comboMissionType.setValue("ADDITION");

        // Clear any preset mission ID on initialization
        txtMissionId.clear();
    }

    // Add this method to set mission ID
    public void setMissionId(int missionId) {
        Platform.runLater(() -> {
            txtMissionId.setText(String.valueOf(missionId));
            // Optional: you can disable the field if you want
            // txtMissionId.setDisable(true);
        });
    }

    @FXML
    private void evaluer() {
        if (txtCode.getText().isEmpty()) {
            alert("Erreur", "Code vide");
            return;
        }

        int missionId, candidatId;
        try {
            missionId = Integer.parseInt(txtMissionId.getText());
            candidatId = Integer.parseInt(txtCandidatId.getText());
        } catch (Exception e) {
            alert("Erreur", "IDs invalides");
            return;
        }

        progress.setVisible(true);

        new Thread(() -> {
            RenduMission r = service.evaluerCodePython(
                    txtCode.getText(), missionId, candidatId);

            Platform.runLater(() -> {
                progress.setVisible(false);
                lblResultat.setText(
                        "Score: " + r.getScore() + "% - " + r.getResultat()
                );
                lblResultat.setStyle(
                        r.isAccepted()
                                ? "-fx-text-fill: green; -fx-font-weight: bold;"
                                : "-fx-text-fill: red; -fx-font-weight: bold;"
                );
            });
        }).start();
    }

    private void alert(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, m);
        a.setTitle(t);
        a.show();
    }
}