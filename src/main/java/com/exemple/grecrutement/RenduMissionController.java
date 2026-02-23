package com.exemple.grecrutement;

import entities.RenduMission;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import services.RenduMissionService;
import utils.AlertUtils;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class RenduMissionController implements Initializable {

    @FXML private TableView<RenduMission> tableRendu;
    @FXML private TableColumn<RenduMission, Integer> colId;
    @FXML private TableColumn<RenduMission, Integer> colScore;
    @FXML private TableColumn<RenduMission, String> colResultat;
    @FXML private TableColumn<RenduMission, Integer> colMissionId;
    @FXML private TableColumn<RenduMission, Integer> colCandidatId;
    @FXML private TableColumn<RenduMission, java.sql.Date> colDate;
    @FXML private TableColumn<RenduMission, String> colCode;

    @FXML private TextArea txtCodeSolution;
    @FXML private TextField txtMissionId;
    @FXML private TextField txtCandidatId;
    @FXML private ComboBox<String> comboMissionType;
    @FXML private Button btnEvaluer;
    @FXML private Button btnAfficher;
    @FXML private Button btnSupprimer;
    @FXML private Label lblResultat;
    @FXML private ProgressIndicator progressIndicator;

    private RenduMissionService renduMissionService;
    private ObservableList<RenduMission> renduList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        renduMissionService = new RenduMissionService();
        renduList = FXCollections.observableArrayList();

        setupTable();
        loadRenduMissions();
        setupComboBox();

        btnEvaluer.setOnAction(e -> evaluerCode());
        btnAfficher.setOnAction(e -> loadRenduMissions());
        btnSupprimer.setOnAction(e -> supprimerSelectionne());
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));
        colResultat.setCellValueFactory(new PropertyValueFactory<>("resultat"));
        colMissionId.setCellValueFactory(new PropertyValueFactory<>("missionId"));
        colCandidatId.setCellValueFactory(new PropertyValueFactory<>("candidatId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateRendu"));
        colCode.setCellValueFactory(new PropertyValueFactory<>("codeSolution"));

        // Add code preview functionality
        setupCodePreviewColumn();

        tableRendu.setItems(renduList);
    }

    private void setupCodePreviewColumn() {
        colCode.setCellFactory(column -> new TableCell<RenduMission, String>() {
            @Override
            protected void updateItem(String code, boolean empty) {
                super.updateItem(code, empty);

                if (empty || code == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    // Show first 50 characters
                    String preview = code.length() > 50 ? code.substring(0, 50) + "..." : code;
                    setText(preview);

                    // Tooltip with full code
                    Tooltip tooltip = new Tooltip(code);
                    tooltip.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 11px;");
                    setTooltip(tooltip);
                }
            }
        });
    }

    private void loadRenduMissions() {
        List<RenduMission> rendus = renduMissionService.afficherRenduMissions();
        renduList.setAll(rendus);
    }

    private void setupComboBox() {
        comboMissionType.getItems().addAll(
                "ADDITION",
                "FACTORIAL",
                "FIBONACCI",
                "PRIME_CHECK"
        );
        comboMissionType.setValue("ADDITION");
    }

    @FXML
    private void evaluerCode() {
        String code = txtCodeSolution.getText();

        if (code.isEmpty()) {
            AlertUtils.showError("Erreur", "Veuillez entrer le code à évaluer.");
            return;
        }

        int missionId, candidatId;

        try {
            missionId = Integer.parseInt(txtMissionId.getText());
            candidatId = Integer.parseInt(txtCandidatId.getText());
        } catch (NumberFormatException e) {
            AlertUtils.showError("Erreur", "ID Mission et ID Candidat doivent être des nombres valides.");
            return;
        }

        // Show progress
        progressIndicator.setVisible(true);
        btnEvaluer.setDisable(true);

        // Run evaluation in background thread
        new Thread(() -> {
            try {
                RenduMission result = renduMissionService.evaluerCodePython(code, missionId, candidatId);

                // Update UI on JavaFX Application Thread
                javafx.application.Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    btnEvaluer.setDisable(false);

                    lblResultat.setText(String.format("Score: %d%% - %s",
                            result.getScore(), result.getResultat()));

                    if (result.isAccepted()) {
                        lblResultat.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                        AlertUtils.showSuccess("Succès", "Code accepté! Le candidat est ajouté à la liste des acceptés.");
                    } else {
                        lblResultat.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        AlertUtils.showWarning("Échec", "Code rejeté. Score insuffisant.");
                    }

                    // Refresh table
                    loadRenduMissions();
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    btnEvaluer.setDisable(false);
                    lblResultat.setText("Erreur: " + e.getMessage());
                    lblResultat.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                });
            }
        }).start();
    }

    @FXML
    private void supprimerSelectionne() {
        RenduMission selected = tableRendu.getSelectionModel().getSelectedItem();

        if (selected == null) {
            AlertUtils.showWarning("Erreur", "Veuillez sélectionner un rendu à supprimer.");
            return;
        }

        if (AlertUtils.showConfirmation("Confirmation", "Voulez-vous vraiment supprimer ce rendu?")) {
            renduMissionService.supprimerRenduMission(selected.getId());
            loadRenduMissions();
            AlertUtils.showSuccess("Succès", "Rendu supprimé avec succès.");
        }
    }

    @FXML
    private void filtrerParCandidat() {
        try {
            int candidatId = Integer.parseInt(txtCandidatId.getText());
            List<RenduMission> rendus = renduMissionService.getRenduMissionsByCandidat(candidatId);
            renduList.setAll(rendus);
        } catch (NumberFormatException e) {
            loadRenduMissions();
        }
    }

    @FXML
    private void filtrerParMission() {
        try {
            int missionId = Integer.parseInt(txtMissionId.getText());
            List<RenduMission> rendus = renduMissionService.getRenduMissionsByMission(missionId);
            renduList.setAll(rendus);
        } catch (NumberFormatException e) {
            loadRenduMissions();
        }
    }

    // ==================== BACK NAVIGATION METHOD ====================
    @FXML
    private void retourMissions() {
        try {
            // Load the mission interface
            FXMLLoader loader = new FXMLLoader(getClass().getResource("mission-simple.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Gestion des Missions - Carrieri");
            stage.setScene(new Scene(root, 900, 700));
            stage.show();

            // Close current AI evaluation window
            Stage currentStage = (Stage) tableRendu.getScene().getWindow();
            currentStage.close();

        } catch (IOException e) {
            e.printStackTrace();
            AlertUtils.showError("Erreur", "Impossible de retourner aux missions: " + e.getMessage());
        }
    }

    private void runDiagnosticTest() {
        System.out.println("🩺 DIAGNOSTIC TEST");

        // Test 1: Perfect code
        String perfectCode = "a = int(input())\nb = int(input())\nprint(a + b)";
        System.out.println("\nTest 1 - Perfect addition code:");
        System.out.println(perfectCode);

        // Test 2: Wrong code
        String wrongCode = "a = int(input())\nb = int(input())\nprint(a - b)";
        System.out.println("\nTest 2 - Wrong code (subtracts):");
        System.out.println(wrongCode);

        // Test 3: Empty code
        String emptyCode = "";
        System.out.println("\nTest 3 - Empty code:");
        System.out.println("(empty)");

        // Run tests
        RenduMissionService service = new RenduMissionService();

        try {
            System.out.println("\n📊 Running tests...");

            // Test perfect code
            RenduMission result1 = service.evaluerCodePython(perfectCode, 1, 999);
            System.out.println("Test 1 score: " + result1.getScore() + "%");

            // Test wrong code
            RenduMission result2 = service.evaluerCodePython(wrongCode, 1, 998);
            System.out.println("Test 2 score: " + result2.getScore() + "%");

            // Test empty code
            RenduMission result3 = service.evaluerCodePython(emptyCode, 1, 997);
            System.out.println("Test 3 score: " + result3.getScore() + "%");

            // Analysis
            System.out.println("\n📈 ANALYSIS:");
            if (result1.getScore() > 70 && result2.getScore() < 40) {
                System.out.println("✅ AI working correctly - scores differ as expected");
            } else if (Math.abs(result1.getScore() - result2.getScore()) < 10) {
                System.out.println("❌ PROBLEM: AI giving similar scores for correct/wrong code");
                System.out.println("   Check Python server test execution");
            } else {
                System.out.println("⚠️  WARNING: Unexpected score pattern");
            }

        } catch (Exception e) {
            System.err.println("Diagnostic failed: " + e.getMessage());
        }
    }
}