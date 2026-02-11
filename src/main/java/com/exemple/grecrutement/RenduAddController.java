package com.exemple.grecrutement;

import entities.RenduMission;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.event.ActionEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import services.RenduMissionService;
import services.MissionService;
import entities.Mission;

import java.net.URL;
import java.util.ResourceBundle;

public class RenduAddController implements Initializable {

    @FXML private TextField txtCandidatId;
    @FXML private TextField txtMissionId;
    @FXML private TextArea txtCode;
    @FXML private ComboBox<String> comboMissionType;
    @FXML private ProgressIndicator progress;
    @FXML private Label lblResultat;
    @FXML private TextArea lblMissionDescription;

    // Timer components
    @FXML private VBox timerContainer;
    @FXML private Label lblTimer;
    @FXML private Label lblTimerProgress;
    @FXML private ProgressBar timerProgressBar;
    @FXML private Label lblTimerWarning;

    // Security components
    @FXML private VBox securityWarningContainer;
    @FXML private Label lblSecurityWarning;

    private RenduMissionService service;
    private MissionService missionService;

    // Timer variables
    private Timeline timerTimeline;
    private int remainingSeconds;
    private static final int DEFAULT_TIMER_MINUTES = 20;
    private static final int WARNING_THRESHOLD_SECONDS = 300; // 5 minutes
    private static final int CRITICAL_THRESHOLD_SECONDS = 60; // 1 minute
    private boolean isAutoSubmitEnabled = true;
    private boolean isTimerFinished = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        service = new RenduMissionService();
        missionService = new MissionService();

        comboMissionType.getItems().addAll(
                "ADDITION", "FACTORIAL", "FIBONACCI", "PRIME_CHECK"
        );
        comboMissionType.setValue("ADDITION");

        // Clear any preset mission ID on initialization
        txtMissionId.clear();
        lblMissionDescription.setText("Select a mission to see its description");

        // Make the mission description area non-editable
        lblMissionDescription.setEditable(false);
        lblMissionDescription.setWrapText(true);

        // Setup code editor security features
        setupCodeEditorSecurity();

        // Timer is not started until mission is selected
        if (timerContainer != null) {
            timerContainer.setVisible(false);
            timerContainer.setManaged(false);
        }

        if (securityWarningContainer != null) {
            securityWarningContainer.setVisible(false);
            securityWarningContainer.setManaged(false);
        }
    }

    // Update this method to also set mission description and start timer
    public void setMissionId(int missionId) {
        Platform.runLater(() -> {
            txtMissionId.setText(String.valueOf(missionId));

            // Load and display mission description
            try {
                Mission mission = missionService.getById(missionId);
                if (mission != null) {
                    String description = mission.getDescription();
                    int minScore = mission.getScore_min();

                    StringBuilder missionInfo = new StringBuilder();
                    missionInfo.append("📋 Mission Details:\n");
                    missionInfo.append("────────────────────\n");
                    missionInfo.append("ID: ").append(missionId).append("\n");
                    missionInfo.append("Description:\n").append(description != null ? description : "No description").append("\n\n");
                    missionInfo.append("📊 Minimum Score: ").append(minScore).append("%");

                    lblMissionDescription.setText(missionInfo.toString());

                    // Start the timer when mission is loaded
                    startTimer(DEFAULT_TIMER_MINUTES * 60);

                } else {
                    lblMissionDescription.setText("❌ Mission not found (ID: " + missionId + ")");
                }
            } catch (Exception e) {
                lblMissionDescription.setText("⚠️ Error loading mission details: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // ============================
    // TIMER FUNCTIONALITY
    // ============================

    private void startTimer(int seconds) {
        // Stop any existing timer
        if (timerTimeline != null) {
            timerTimeline.stop();
        }

        this.remainingSeconds = seconds;

        if (timerContainer != null) {
            timerContainer.setVisible(true);
            timerContainer.setManaged(true);
        }

        isTimerFinished = false;

        // Initialize timer display
        updateTimerDisplay();

        if (timerProgressBar != null) {
            timerProgressBar.setProgress(1.0);
            timerProgressBar.setStyle("-fx-accent: #10b981;");
        }

        if (lblTimerProgress != null) {
            lblTimerProgress.setText("100%");
        }

        // Create timeline that updates every second
        timerTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), this::updateTimer)
        );
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();
    }

    private void updateTimer(ActionEvent event) {
        if (isTimerFinished) return;

        remainingSeconds--;

        if (remainingSeconds <= 0) {
            // Time's up - auto submit
            timerFinished();
        } else {
            updateTimerDisplay();
            updateTimerProgress();
            checkTimerWarning();
        }
    }

    private void updateTimerDisplay() {
        if (lblTimer == null) return;

        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        String timeString = String.format("%02d:%02d", minutes, seconds);

        lblTimer.setText(timeString);

        // Change color based on time remaining
        if (remainingSeconds <= CRITICAL_THRESHOLD_SECONDS) {
            lblTimer.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 32px;");
            lblTimer.getStyleClass().add("timer-critical");
            if (timerProgressBar != null) {
                timerProgressBar.setStyle("-fx-accent: #ef4444;");
            }
            if (lblTimerWarning != null) {
                lblTimerWarning.setText("⚠️ CRITICAL: Less than 1 minute remaining!");
                lblTimerWarning.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                lblTimerWarning.setVisible(true);
            }
        } else if (remainingSeconds <= WARNING_THRESHOLD_SECONDS) {
            lblTimer.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 32px;");
            lblTimer.getStyleClass().remove("timer-critical");
            if (timerProgressBar != null) {
                timerProgressBar.setStyle("-fx-accent: #f59e0b;");
            }
            if (lblTimerWarning != null) {
                lblTimerWarning.setText("⚠️ Warning: " + (remainingSeconds / 60) + " minutes remaining");
                lblTimerWarning.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                lblTimerWarning.setVisible(true);
            }
        } else {
            lblTimer.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 32px;");
            lblTimer.getStyleClass().remove("timer-critical");
            if (timerProgressBar != null) {
                timerProgressBar.setStyle("-fx-accent: #10b981;");
            }
            if (lblTimerWarning != null) {
                lblTimerWarning.setVisible(false);
            }
        }
    }

    private void updateTimerProgress() {
        if (timerProgressBar == null || lblTimerProgress == null) return;

        double progress = (double) remainingSeconds / (DEFAULT_TIMER_MINUTES * 60);
        timerProgressBar.setProgress(progress);

        int percentRemaining = (int) (progress * 100);
        lblTimerProgress.setText(percentRemaining + "%");
    }

    private void checkTimerWarning() {
        // Warning logic is handled in updateTimerDisplay
    }

    private void timerFinished() {
        if (isTimerFinished) return;

        isTimerFinished = true;

        if (timerTimeline != null) {
            timerTimeline.stop();
        }

        if (lblTimer != null) {
            lblTimer.setText("00:00");
            lblTimer.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 32px;");
        }

        if (timerProgressBar != null) {
            timerProgressBar.setProgress(0);
            timerProgressBar.setStyle("-fx-accent: #ef4444;");
        }

        if (lblTimerProgress != null) {
            lblTimerProgress.setText("0%");
        }

        // Disable code editor
        if (txtCode != null) {
            txtCode.setEditable(false);
            txtCode.setDisable(true);
        }

        // Show timeout message
        if (lblTimerWarning != null) {
            lblTimerWarning.setText("⏰ TIME'S UP! Auto-submitting your solution...");
            lblTimerWarning.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 14px;");
            lblTimerWarning.setVisible(true);
        }

        // Auto-submit the code
        if (isAutoSubmitEnabled && txtCode != null && !txtCode.getText().trim().isEmpty()) {
            evaluer();
        } else if (txtCode != null && txtCode.getText().trim().isEmpty()) {
            if (lblResultat != null) {
                lblResultat.setText("❌ Time's up! No code submitted.");
                lblResultat.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            }

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Time's Up");
            alert.setHeaderText("Submission Time Expired");
            alert.setContentText("You didn't submit any code before the timer ended.");
            alert.showAndWait();
        }
    }

    // ============================
    // SECURITY FEATURES
    // ============================

    private void setupCodeEditorSecurity() {
        if (txtCode == null) return;

        // Disable copy/paste shortcuts
        txtCode.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isShortcutDown()) {
                if (event.getCode() == KeyCode.C ||
                        event.getCode() == KeyCode.V ||
                        event.getCode() == KeyCode.X ||
                        event.getCode() == KeyCode.A) {
                    event.consume();
                    showSecurityWarning("Copy, paste, and cut are disabled during the mission!");
                }
            }
        });

        // Disable context menu (right-click)
        txtCode.setContextMenu(null);

        // Disable drag and drop
        txtCode.setOnDragOver(event -> {
            event.consume();
            showSecurityWarning("Drag and drop is disabled!");
        });

        txtCode.setOnDragDropped(event -> {
            event.consume();
        });
    }

    @FXML
    private void hideSecurityWarning() {
        if (securityWarningContainer != null) {
            securityWarningContainer.setVisible(false);
            securityWarningContainer.setManaged(false);
        }
    }

    private void showSecurityWarning(String message) {
        if (securityWarningContainer == null || lblSecurityWarning == null) return;

        securityWarningContainer.setVisible(true);
        securityWarningContainer.setManaged(true);
        lblSecurityWarning.setText("⚠️ " + message);

        // Auto-hide after 3 seconds using Timeline (JavaFX way)
        Timeline hideTimeline = new Timeline(
                new KeyFrame(Duration.seconds(3), e -> {
                    securityWarningContainer.setVisible(false);
                    securityWarningContainer.setManaged(false);
                })
        );
        hideTimeline.setCycleCount(1);
        hideTimeline.play();
    }

    // ============================
    // EXTENDED EVALUATION METHOD
    // ============================

    @FXML
    private void evaluer() {
        // Stop the timer if it's still running
        if (timerTimeline != null && !isTimerFinished) {
            timerTimeline.stop();
            isTimerFinished = true;
        }

        if (txtCode.getText().isEmpty()) {
            alert("Error", "Please enter your Python code");
            // Restart timer if code is empty
            if (!isTimerFinished) {
                startTimer(remainingSeconds > 0 ? remainingSeconds : DEFAULT_TIMER_MINUTES * 60);
            }
            return;
        }

        int missionId, candidatId;
        try {
            missionId = Integer.parseInt(txtMissionId.getText());
            candidatId = Integer.parseInt(txtCandidatId.getText());
        } catch (Exception e) {
            alert("Error", "Invalid IDs. Please enter valid numbers for Mission ID and Candidate ID");
            // Restart timer
            if (!isTimerFinished) {
                startTimer(remainingSeconds > 0 ? remainingSeconds : DEFAULT_TIMER_MINUTES * 60);
            }
            return;
        }

        progress.setVisible(true);
        lblResultat.setText("Evaluating code...");

        // Disable evaluate button and code editor during evaluation
        Button evalButton = getEvaluateButton();
        if (evalButton != null) {
            evalButton.setDisable(true);
        }
        txtCode.setEditable(false);

        new Thread(() -> {
            try {
                RenduMission r = service.evaluerCodePython(
                        txtCode.getText(), missionId, candidatId);

                Platform.runLater(() -> {
                    progress.setVisible(false);

                    // Re-enable button and editor
                    if (evalButton != null) {
                        evalButton.setDisable(false);
                    }
                    txtCode.setEditable(true);

                    String resultText = "🎯 Score: " + r.getScore() + "% - " + r.getResultat();

                    if (r.isAccepted()) {
                        resultText += "\n✅ Code Accepted!";
                        lblResultat.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 14px;");
                    } else {
                        resultText += "\n❌ Code Rejected";
                        lblResultat.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 14px;");
                    }

                    lblResultat.setText(resultText);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    progress.setVisible(false);

                    // Re-enable button and editor
                    if (evalButton != null) {
                        evalButton.setDisable(false);
                    }
                    txtCode.setEditable(true);

                    // Restart timer if evaluation failed
                    if (!isTimerFinished) {
                        startTimer(remainingSeconds > 0 ? remainingSeconds : DEFAULT_TIMER_MINUTES * 60);
                    }

                    lblResultat.setText("❌ Error: " + e.getMessage());
                    lblResultat.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                });
            }
        }).start();
    }

    private Button getEvaluateButton() {
        // Find the evaluate button in the scene
        if (txtMissionId != null && txtMissionId.getScene() != null) {
            return (Button) txtMissionId.getScene().lookup("#btnEvaluer");
        }
        return null;
    }

    // ============================
    // UTILITY METHODS
    // ============================

    public void setAutoSubmitEnabled(boolean enabled) {
        this.isAutoSubmitEnabled = enabled;
    }

    @FXML
    private void resetTimer() {
        if (timerTimeline != null) {
            timerTimeline.stop();
        }
        startTimer(DEFAULT_TIMER_MINUTES * 60);
        if (txtCode != null) {
            txtCode.setEditable(true);
            txtCode.setDisable(false);
        }
        isTimerFinished = false;
    }

    private void alert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}