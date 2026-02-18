package com.exemple.grecrutement;

import entities.RenduMission;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.event.ActionEvent;
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
    @FXML private Label lblProcessing;

    // Validation labels
    @FXML private Label lblCandidatIdError;
    @FXML private Label lblMissionIdError;
    @FXML private Label lblCodeError;
    @FXML private Label lblMissionTypeError;

    // Timer components
    @FXML private VBox timerContainer;
    @FXML private Label lblTimer;
    @FXML private Label lblTimerProgress;
    @FXML private ProgressBar timerProgressBar;
    @FXML private Label lblTimerWarning;

    // Security components
    @FXML private VBox securityWarningContainer;
    @FXML private Label lblSecurityWarning;

    // Full Screen components
    @FXML private VBox fullScreenEditorOverlay;
    @FXML private TextArea fullScreenCodeEditor;
    @FXML private Label fullScreenTimer;
    @FXML private Button btnFullScreen;
    @FXML private VBox codeEditorContainer;
    @FXML private ScrollPane mainScrollPane;

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
    private boolean isFullScreenMode = false;

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

        // Setup input validation
        setupInputValidation();

        // Setup code editor security features
        setupCodeEditorSecurity();

        // Setup keyboard shortcuts for full screen
        setupKeyboardShortcuts();

        // Timer is not started until mission is selected
        if (timerContainer != null) {
            timerContainer.setVisible(false);
            timerContainer.setManaged(false);
        }

        if (securityWarningContainer != null) {
            securityWarningContainer.setVisible(false);
            securityWarningContainer.setManaged(false);
        }

        // Initialize full screen overlay
        if (fullScreenEditorOverlay != null) {
            fullScreenEditorOverlay.setVisible(false);
            fullScreenEditorOverlay.setManaged(false);
        }

        // Initialize validation labels as invisible
        if (lblCandidatIdError != null) {
            lblCandidatIdError.setVisible(false);
            lblCandidatIdError.setManaged(false);
        }
        if (lblMissionIdError != null) {
            lblMissionIdError.setVisible(false);
            lblMissionIdError.setManaged(false);
        }
        if (lblCodeError != null) {
            lblCodeError.setVisible(false);
            lblCodeError.setManaged(false);
        }
        if (lblMissionTypeError != null) {
            lblMissionTypeError.setVisible(false);
            lblMissionTypeError.setManaged(false);
        }
    }

    // ============================
    // INPUT VALIDATION METHODS
    // ============================

    private void setupInputValidation() {
        // Candidate ID validation (must be a positive integer)
        txtCandidatId.textProperty().addListener((observable, oldValue, newValue) -> {
            validateCandidatId();
        });

        txtCandidatId.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // Lost focus
                validateCandidatId();
            }
        });

        // Mission ID validation (must be a positive integer)
        txtMissionId.textProperty().addListener((observable, oldValue, newValue) -> {
            validateMissionId();
        });

        txtMissionId.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // Lost focus
                validateMissionId();
            }
        });

        // Code validation (cannot be empty)
        txtCode.textProperty().addListener((observable, oldValue, newValue) -> {
            validateCode();
        });

        txtCode.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // Lost focus
                validateCode();
            }
        });

        // Mission Type validation
        comboMissionType.valueProperty().addListener((observable, oldValue, newValue) -> {
            validateMissionType();
        });

        // Add real-time character counter for code
        txtCode.textProperty().addListener((observable, oldValue, newValue) -> {
            updateCodeCharacterCounter();
        });

        // Restrict Candidate ID to numbers only
        txtCandidatId.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.matches("\\d*")) {
                txtCandidatId.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        // Restrict Mission ID to numbers only
        txtMissionId.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.matches("\\d*")) {
                txtMissionId.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        // Add CSS classes for validation styling
        addValidationStyleClasses();
    }

    private void addValidationStyleClasses() {
        txtCandidatId.getStyleClass().add("validation-field");
        txtMissionId.getStyleClass().add("validation-field");
        txtCode.getStyleClass().add("validation-field");
        comboMissionType.getStyleClass().add("validation-field");
    }

    private boolean validateCandidatId() {
        String text = txtCandidatId.getText();

        if (text == null || text.trim().isEmpty()) {
            showFieldError(txtCandidatId, lblCandidatIdError, "Candidate ID is required");
            return false;
        }

        try {
            int id = Integer.parseInt(text);
            if (id <= 0) {
                showFieldError(txtCandidatId, lblCandidatIdError, "Candidate ID must be positive");
                return false;
            }
            clearFieldError(txtCandidatId, lblCandidatIdError);
            return true;
        } catch (NumberFormatException e) {
            showFieldError(txtCandidatId, lblCandidatIdError, "Candidate ID must be a number");
            return false;
        }
    }

    private boolean validateMissionId() {
        String text = txtMissionId.getText();

        if (text == null || text.trim().isEmpty()) {
            showFieldError(txtMissionId, lblMissionIdError, "Mission ID is required");
            return false;
        }

        try {
            int id = Integer.parseInt(text);
            if (id <= 0) {
                showFieldError(txtMissionId, lblMissionIdError, "Mission ID must be positive");
                return false;
            }
            clearFieldError(txtMissionId, lblMissionIdError);
            return true;
        } catch (NumberFormatException e) {
            showFieldError(txtMissionId, lblMissionIdError, "Mission ID must be a number");
            return false;
        }
    }

    private boolean validateCode() {
        String text = txtCode.getText();

        if (text == null || text.trim().isEmpty()) {
            showFieldError(txtCode, lblCodeError, "Python code is required");
            return false;
        }

        if (text.trim().length() < 10) {
            showFieldError(txtCode, lblCodeError, "Code must be at least 10 characters");
            return false;
        }

        clearFieldError(txtCode, lblCodeError);
        return true;
    }

    private boolean validateMissionType() {
        String type = comboMissionType.getValue();

        if (type == null || type.trim().isEmpty()) {
            showFieldError(comboMissionType, lblMissionTypeError, "Mission type is required");
            return false;
        }

        clearFieldError(comboMissionType, lblMissionTypeError);
        return true;
    }

    private boolean validateAllFields() {
        boolean isValid = true;

        isValid &= validateCandidatId();
        isValid &= validateMissionId();
        isValid &= validateCode();
        isValid &= validateMissionType();

        return isValid;
    }

    private void showFieldError(Control field, Label errorLabel, String message) {
        field.getStyleClass().remove("validation-field-valid");
        field.getStyleClass().add("validation-field-error");

        if (errorLabel != null) {
            errorLabel.setText("⚠ " + message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            errorLabel.getStyleClass().remove("success-label");
            errorLabel.getStyleClass().add("error-label");
        }
    }

    private void clearFieldError(Control field, Label errorLabel) {
        field.getStyleClass().remove("validation-field-error");
        field.getStyleClass().add("validation-field-valid");

        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    private void updateCodeCharacterCounter() {
        if (lblCodeError != null && txtCode.getText() != null) {
            int length = txtCode.getText().length();
            if (length > 0 && !validateCode()) {
                lblCodeError.setText("⚠ " + length + " characters (minimum 10)");
                lblCodeError.setVisible(true);
                lblCodeError.setManaged(true);
                lblCodeError.getStyleClass().remove("success-label");
                lblCodeError.getStyleClass().add("error-label");
            } else if (length > 0) {
                lblCodeError.setText("✅ " + length + " characters");
                lblCodeError.setVisible(true);
                lblCodeError.setManaged(true);
                lblCodeError.getStyleClass().remove("error-label");
                lblCodeError.getStyleClass().add("success-label");

                // Schedule to hide after 2 seconds
                PauseTransition pause = new PauseTransition(Duration.seconds(2));
                pause.setOnFinished(e -> {
                    if (validateCode()) {
                        lblCodeError.setVisible(false);
                        lblCodeError.setManaged(false);
                    }
                });
                pause.play();
            }
        }
    }

    // ============================
    // KEYBOARD SHORTCUTS
    // ============================

    private void setupKeyboardShortcuts() {
        if (txtCode != null) {
            // F11 to toggle full screen
            txtCode.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.F11) {
                    event.consume();
                    toggleFullScreen();
                }

                // ESC to exit full screen
                if (event.getCode() == KeyCode.ESCAPE && isFullScreenMode) {
                    event.consume();
                    exitFullScreen();
                }

                // Ctrl+Enter to submit
                if (event.isControlDown() && event.getCode() == KeyCode.ENTER) {
                    event.consume();
                    evaluer();
                }
            });
        }

        // Also add to the full screen editor
        if (fullScreenCodeEditor != null) {
            fullScreenCodeEditor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    event.consume();
                    exitFullScreen();
                }
                if (event.isControlDown() && event.getCode() == KeyCode.ENTER) {
                    event.consume();
                    evaluateFromFullScreen();
                }
            });
        }
    }

    // ============================
    // FULL SCREEN METHODS
    // ============================

    @FXML
    private void toggleFullScreen() {
        if (isFullScreenMode) {
            exitFullScreen();
        } else {
            enterFullScreen();
        }
    }

    private void enterFullScreen() {
        if (fullScreenEditorOverlay == null || fullScreenCodeEditor == null || txtCode == null) return;

        // Copy the current code to full screen editor
        fullScreenCodeEditor.setText(txtCode.getText());
        fullScreenCodeEditor.setPromptText(txtCode.getPromptText());

        // Copy security settings
        fullScreenCodeEditor.setEditable(txtCode.isEditable());
        fullScreenCodeEditor.setDisable(txtCode.isDisabled());

        // Update timer in full screen
        if (fullScreenTimer != null && lblTimer != null) {
            fullScreenTimer.setText(lblTimer.getText());
            fullScreenTimer.setStyle(lblTimer.getStyle());
        }

        // Show full screen overlay
        fullScreenEditorOverlay.setVisible(true);
        fullScreenEditorOverlay.setManaged(true);
        fullScreenEditorOverlay.toFront();

        // Focus on full screen editor
        Platform.runLater(() -> {
            fullScreenCodeEditor.requestFocus();
            fullScreenCodeEditor.positionCaret(fullScreenCodeEditor.getText().length());
        });

        isFullScreenMode = true;

        // Setup security for full screen editor
        setupFullScreenSecurity();
    }

    @FXML
    private void exitFullScreen() {
        if (fullScreenEditorOverlay == null || txtCode == null) return;

        // Copy code back from full screen editor
        txtCode.setText(fullScreenCodeEditor.getText());

        // Hide full screen overlay
        fullScreenEditorOverlay.setVisible(false);
        fullScreenEditorOverlay.setManaged(false);

        isFullScreenMode = false;
    }

    private void setupFullScreenSecurity() {
        if (fullScreenCodeEditor == null) return;

        // Disable copy/paste shortcuts
        fullScreenCodeEditor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
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

        // Disable context menu
        fullScreenCodeEditor.setContextMenu(null);

        // Disable drag and drop
        fullScreenCodeEditor.setOnDragOver(event -> {
            event.consume();
            showSecurityWarning("Drag and drop is disabled!");
        });

        fullScreenCodeEditor.setOnDragDropped(event -> {
            event.consume();
        });
    }

    @FXML
    private void evaluateFromFullScreen() {
        if (fullScreenCodeEditor != null && txtCode != null) {
            // Copy code from full screen editor to main editor
            txtCode.setText(fullScreenCodeEditor.getText());

            // Exit full screen
            exitFullScreen();

            // Evaluate
            evaluer();
        }
    }

    // ============================
    // MISSION SETUP
    // ============================

    public void setMissionId(int missionId) {
        Platform.runLater(() -> {
            txtMissionId.setText(String.valueOf(missionId));
            validateMissionId();

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

        // Update full screen timer if visible
        if (isFullScreenMode && fullScreenTimer != null && lblTimer != null) {
            fullScreenTimer.setText(lblTimer.getText());
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

        if (fullScreenTimer != null) {
            fullScreenTimer.setText("00:00");
            fullScreenTimer.setStyle("-fx-text-fill: #ef4444;");
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

        if (fullScreenCodeEditor != null) {
            fullScreenCodeEditor.setEditable(false);
            fullScreenCodeEditor.setDisable(true);
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
            alert.setHeaderText("⏰ Submission Time Expired");
            alert.setContentText("You didn't submit any code before the timer ended.");
            styleAlert(alert, "warning");
            alert.showAndWait();
        }
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
        if (fullScreenCodeEditor != null) {
            fullScreenCodeEditor.setEditable(true);
            fullScreenCodeEditor.setDisable(false);
        }
        isTimerFinished = false;
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

        // Auto-hide after 3 seconds
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
    // HELPER METHOD FOR STYLING ALERTS
    // ============================

    private void styleAlert(Alert alert, String alertType) {
        DialogPane dialogPane = alert.getDialogPane();

        // Add Alert.css from resources folder
        dialogPane.getStylesheets().add(getClass().getResource("/Alert.css").toExternalForm());

        // Add style class based on alert type
        dialogPane.getStyleClass().add(alertType);

        // Style the OK button
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("ok-button");
        }

        // Style Cancel button if present
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.getStyleClass().add("cancel-button");
        }
    }

    // ============================
    // EVALUATION METHOD
    // ============================

    @FXML
    private void evaluer() {
        // Validate all fields before proceeding
        if (!validateAllFields()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setHeaderText("⚠️ Please fix the following errors:");

            StringBuilder errors = new StringBuilder();
            if (!validateCandidatId()) errors.append("• Invalid Candidate ID\n");
            if (!validateMissionId()) errors.append("• Invalid Mission ID\n");
            if (!validateCode()) errors.append("• Invalid Python code\n");
            if (!validateMissionType()) errors.append("• Mission type required\n");

            alert.setContentText(errors.toString());
            styleAlert(alert, "warning");
            alert.showAndWait();
            return;
        }

        // Stop the timer if it's still running
        if (timerTimeline != null && !isTimerFinished) {
            timerTimeline.stop();
            isTimerFinished = true;
        }

        int missionId, candidatId;
        try {
            missionId = Integer.parseInt(txtMissionId.getText());
            candidatId = Integer.parseInt(txtCandidatId.getText());
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Input");
            alert.setHeaderText("❌ Invalid IDs");
            alert.setContentText("Please enter valid numbers for Mission ID and Candidate ID");
            styleAlert(alert, "error");
            alert.showAndWait();

            if (!isTimerFinished) {
                startTimer(remainingSeconds > 0 ? remainingSeconds : DEFAULT_TIMER_MINUTES * 60);
            }
            return;
        }

        progress.setVisible(true);
        if (lblProcessing != null) {
            lblProcessing.setVisible(true);
        }
        lblResultat.setText("Evaluating code with AI system...");

        Button evalButton = getEvaluateButton();
        if (evalButton != null) {
            evalButton.setDisable(true);
        }
        txtCode.setEditable(false);

        if (fullScreenCodeEditor != null) {
            fullScreenCodeEditor.setEditable(false);
        }

        new Thread(() -> {
            try {
                RenduMission r = service.evaluerCodePython(txtCode.getText(), missionId, candidatId);

                Platform.runLater(() -> {
                    progress.setVisible(false);
                    if (lblProcessing != null) {
                        lblProcessing.setVisible(false);
                    }

                    if (evalButton != null) {
                        evalButton.setDisable(false);
                    }
                    txtCode.setEditable(true);
                    if (fullScreenCodeEditor != null) {
                        fullScreenCodeEditor.setEditable(true);
                    }

                    String resultText = "🎯 AI Score: " + r.getScore() + "% - " + r.getResultat() + "\n";

                    if (r.isAccepted()) {
                        resultText += "✅ ACCEPTED! (Score meets minimum requirement)";
                        lblResultat.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 14px;");

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Success");
                        alert.setHeaderText("✅ Congratulations!");
                        alert.setContentText("Your code has been accepted with a score of " + r.getScore() + "%!");
                        styleAlert(alert, "information");
                        alert.showAndWait();

                    } else {
                        resultText += "❌ REJECTED (Score below minimum requirement)";
                        lblResultat.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 14px;");

                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Code Rejected");
                        alert.setHeaderText("❌ Code Rejected");
                        alert.setContentText("Your code scored " + r.getScore() + "%, which is below the minimum requirement.");
                        styleAlert(alert, "warning");
                        alert.showAndWait();
                    }

                    lblResultat.setText(resultText);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    progress.setVisible(false);
                    if (lblProcessing != null) {
                        lblProcessing.setVisible(false);
                    }

                    if (evalButton != null) {
                        evalButton.setDisable(false);
                    }
                    txtCode.setEditable(true);
                    if (fullScreenCodeEditor != null) {
                        fullScreenCodeEditor.setEditable(true);
                    }

                    if (!isTimerFinished) {
                        startTimer(remainingSeconds > 0 ? remainingSeconds : DEFAULT_TIMER_MINUTES * 60);
                    }

                    lblResultat.setText("❌ Error: " + e.getMessage());
                    lblResultat.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");

                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Evaluation Error");
                    alert.setHeaderText("❌ Failed to evaluate code");
                    alert.setContentText(e.getMessage());
                    styleAlert(alert, "error");
                    alert.showAndWait();
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

    private void alert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText("⚠️ " + title);
        alert.setContentText(message);
        styleAlert(alert, "warning");
        alert.showAndWait();
    }
}