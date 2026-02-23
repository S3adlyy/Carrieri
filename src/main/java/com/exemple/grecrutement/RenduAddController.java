package com.exemple.grecrutement;

import services.SMSService;
import entities.RenduMission;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import services.RenduMissionService;
import services.MissionService;
import services.PythoniumService;
import entities.Mission;
import utils.AlertUtils;

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

    // Console components
    @FXML private TextArea consoleOutput;
    @FXML private Label lblKernelStatus;
    @FXML private ListView<String> variableListView;
    @FXML private Button btnExecute;
    @FXML private Button btnProfile;
    @FXML private Button btnRestartKernel;
    @FXML private Button btnClearConsole;
    @FXML private Button btnInspectVariable;

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
    private SMSService smsService;
    private StringBuilder currentOutput = new StringBuilder();

    // Timer variables
    private Timeline timerTimeline;
    private int remainingSeconds;
    private static final int DEFAULT_TIMER_MINUTES = 20;
    private static final int WARNING_THRESHOLD_SECONDS = 300;
    private static final int CRITICAL_THRESHOLD_SECONDS = 60;
    private boolean isAutoSubmitEnabled = true;
    private boolean isTimerFinished = false;
    private boolean isFullScreenMode = false;

    // Pythonium Variables
    private PythoniumService pythoniumService;
    private int executionCount = 0;
    private static final int MAX_EXECUTIONS = 3;
    private Label lblExecutionCounter;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            // Initialize services
            pythoniumService = new PythoniumService();
            service = new RenduMissionService();
            missionService = new MissionService();

            // Initialize SMS Service
            smsService = SMSService.getInstance();
            if (smsService != null && smsService.isEnabled()) {
                System.out.println("✅ SMS Service initialized successfully");
                System.out.println("   From: " + smsService.getFromPhoneNumber() + " (Twilio Trial)");
                System.out.println("   To: " + smsService.getRecipientPhoneNumber() + " (Your phone)");
            } else {
                System.out.println("⚠️ SMS Service is disabled or not properly configured");
            }

            System.out.println("✅ Services initialized successfully");

            // Setup mission types
            comboMissionType.getItems().addAll(
                    "ADDITION", "FACTORIAL", "FIBONACCI", "PRIME_CHECK"
            );
            comboMissionType.setValue("ADDITION");

            // Clear any preset mission ID on initialization
            txtMissionId.clear();
            lblMissionDescription.setText("Select a mission to see its description");
            lblMissionDescription.setEditable(false);
            lblMissionDescription.setWrapText(true);

            // Setup input validation
            setupInputValidation();

            // Setup code editor security features
            setupCodeEditorSecurity();

            // Setup keyboard shortcuts
            setupKeyboardShortcuts();

            // Update UI for Pythonium
            updateUIPythoniumMode();

            // Create execution counter
            createExecutionCounter();

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

            // Initialize validation labels
            initValidationLabels();

            System.out.println("✅ RenduAddController initialized successfully");

        } catch (Exception e) {
            System.err.println("❌ Error in initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initValidationLabels() {
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

    private void updateUIPythoniumMode() {
        if (lblKernelStatus != null) {
            lblKernelStatus.setText("✅ Pythonium Ready");
            lblKernelStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
        }

        if (consoleOutput != null) {
            consoleOutput.setText("🐍 Pythonium API initialized. You have " + MAX_EXECUTIONS + " execution attempts.\n");
        }

        if (variableListView != null) {
            variableListView.setVisible(false);
        }

        if (btnExecute != null) {
            btnExecute.setDisable(false);
            btnExecute.setText("▶ Execute (Pythonium)");
            btnExecute.setOnAction(e -> {
                if (executionCount < MAX_EXECUTIONS) {
                    executeWithPythonium();
                } else {
                    showExecutionLimitReached();
                }
            });
        }

        if (btnProfile != null) {
            btnProfile.setDisable(true);
            btnProfile.setVisible(false);
        }

        if (btnRestartKernel != null) {
            btnRestartKernel.setDisable(true);
            btnRestartKernel.setVisible(false);
        }

        if (btnInspectVariable != null) {
            btnInspectVariable.setDisable(true);
            btnInspectVariable.setVisible(false);
        }
    }

    // ============================
    // EXECUTION COUNTER METHODS
    // ============================

    private void createExecutionCounter() {
        lblExecutionCounter = new Label("⚡ Executions: 0/" + MAX_EXECUTIONS);
        lblExecutionCounter.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-padding: 0 15 0 0;");

        Platform.runLater(() -> {
            if (consoleOutput != null && consoleOutput.getParent() != null) {
                VBox consoleSection = (VBox) consoleOutput.getParent();
                for (javafx.scene.Node node : consoleSection.getChildren()) {
                    if (node instanceof HBox) {
                        HBox headerBox = (HBox) node;
                        int regionIndex = -1;
                        for (int i = 0; i < headerBox.getChildren().size(); i++) {
                            if (headerBox.getChildren().get(i) instanceof Region) {
                                regionIndex = i;
                                break;
                            }
                        }
                        if (regionIndex != -1) {
                            headerBox.getChildren().add(regionIndex, lblExecutionCounter);
                        } else {
                            headerBox.getChildren().add(0, lblExecutionCounter);
                        }
                        break;
                    }
                }
            }
        });
    }

    private void updateExecutionCounter() {
        Platform.runLater(() -> {
            if (lblExecutionCounter != null) {
                lblExecutionCounter.setText("⚡ Executions: " + executionCount + "/" + MAX_EXECUTIONS);

                if (executionCount >= MAX_EXECUTIONS) {
                    lblExecutionCounter.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-padding: 0 15 0 0;");
                } else if (executionCount >= MAX_EXECUTIONS - 1) {
                    lblExecutionCounter.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-padding: 0 15 0 0;");
                } else {
                    lblExecutionCounter.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-padding: 0 15 0 0;");
                }
            }
        });
    }

    private void showExecutionLimitReached() {
        Platform.runLater(() -> {
            appendToConsole("", "");
            appendToConsole("═".repeat(60), "#ef4444");
            appendToConsole("⚠️ EXECUTION LIMIT REACHED", "#ef4444");
            appendToConsole("You have used all " + MAX_EXECUTIONS + " allowed executions.", "#f59e0b");
            appendToConsole("Please submit your code using the 'Submit' button to proceed.", "#f59e0b");
            appendToConsole("═".repeat(60), "#ef4444");

            if (btnExecute != null) {
                btnExecute.setDisable(true);
            }

            if (lblKernelStatus != null) {
                lblKernelStatus.setText("⛔ Limit reached");
                lblKernelStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            }

            showSecurityWarning("Maximum executions (" + MAX_EXECUTIONS + ") reached. Please submit your solution.");
        });
    }

    // ============================
    // PYTHONIUM EXECUTION
    // ============================

    @FXML
    private void executeWithPythonium() {
        if (executionCount >= MAX_EXECUTIONS) {
            showExecutionLimitReached();
            return;
        }

        String code = txtCode.getText();
        String missionType = comboMissionType.getValue();

        if (code == null || code.trim().isEmpty()) {
            appendToConsole("⚠️ No code to execute", "#f59e0b");
            return;
        }

        executionCount++;
        updateExecutionCounter();

        if (consoleOutput != null) {
            consoleOutput.clear();
            currentOutput.setLength(0);
        }

        appendToConsole("═".repeat(60), "#5b21b6");
        appendToConsole("🚀 Executing with Pythonium API (Attempt " + executionCount + "/" + MAX_EXECUTIONS + ")", "#8b5cf6");
        appendToConsole("═".repeat(60), "#5b21b6");

        if (btnExecute != null) {
            btnExecute.setDisable(true);
        }

        if (lblKernelStatus != null) {
            lblKernelStatus.setText("🔄 Executing...");
            lblKernelStatus.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
        }

        new Thread(() -> {
            try {
                PythoniumService.PythoniumResponse response = pythoniumService.executeCode(code, missionType);

                Platform.runLater(() -> {
                    if (response.success) {
                        appendToConsole("\n✅ Execution Successful!", "#10b981");
                        appendToConsole("⏱️ Execution Time: " + String.format("%.2f", response.executionTime) + "ms", "#8b5cf6");

                        if (response.output != null && !response.output.isEmpty()) {
                            appendToConsole("\n📤 OUTPUT:", "#f59e0b");
                            appendToConsole(response.output, "#e0e0e0");
                        }

                        if (response.result != null) {
                            appendToConsole("\n📊 Result: " + response.result, "#10b981");
                        }

                        if (lblKernelStatus != null) {
                            lblKernelStatus.setText("✅ Ready");
                            lblKernelStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                        }
                    } else {
                        appendToConsole("\n❌ Execution Failed!", "#ef4444");
                        if (response.error != null && !response.error.isEmpty()) {
                            appendToConsole("Error: " + response.error, "#ef4444");
                        }
                        if (lblKernelStatus != null) {
                            lblKernelStatus.setText("❌ Failed");
                            lblKernelStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                        }
                    }

                    if (executionCount < MAX_EXECUTIONS) {
                        if (btnExecute != null) {
                            btnExecute.setDisable(false);
                        }
                    } else {
                        showExecutionLimitReached();
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    appendToConsole("❌ Pythonium API Error: " + e.getMessage(), "#ef4444");
                    e.printStackTrace();
                    if (lblKernelStatus != null) {
                        lblKernelStatus.setText("❌ API Error");
                        lblKernelStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    }

                    if (executionCount < MAX_EXECUTIONS) {
                        if (btnExecute != null) {
                            btnExecute.setDisable(false);
                        }
                    }
                });
            }
        }).start();
    }

    // ============================
    // CONSOLE METHODS
    // ============================

    private void appendToConsole(String text, String color) {
        if (consoleOutput != null) {
            if (!text.isEmpty()) {
                currentOutput.append(text).append("\n");
                consoleOutput.appendText(text + "\n");
            } else {
                consoleOutput.appendText("\n");
            }
            consoleOutput.setScrollTop(Double.MAX_VALUE);
        }
    }

    @FXML
    private void clearConsole() {
        if (consoleOutput != null) {
            consoleOutput.clear();
            currentOutput.setLength(0);
            appendToConsole("🧹 Console cleared", "#6b7280");
            appendToConsole("You have " + (MAX_EXECUTIONS - executionCount) + " executions remaining.", "#8b5cf6");
        }
    }

    // ============================
    // INPUT VALIDATION METHODS
    // ============================

    private void setupInputValidation() {
        txtCandidatId.textProperty().addListener((observable, oldValue, newValue) -> validateCandidatId());
        txtCandidatId.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) validateCandidatId();
        });

        txtMissionId.textProperty().addListener((observable, oldValue, newValue) -> validateMissionId());
        txtMissionId.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) validateMissionId();
        });

        txtCode.textProperty().addListener((observable, oldValue, newValue) -> {
            validateCode();
            updateCodeCharacterCounter();
        });
        txtCode.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) validateCode();
        });

        comboMissionType.valueProperty().addListener((observable, oldValue, newValue) -> validateMissionType());

        txtCandidatId.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.matches("\\d*")) {
                txtCandidatId.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        txtMissionId.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.matches("\\d*")) {
                txtMissionId.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

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
        return validateCandidatId() && validateMissionId() && validateCode() && validateMissionType();
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
            txtCode.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.F11) {
                    event.consume();
                    toggleFullScreen();
                }
                if (event.getCode() == KeyCode.ESCAPE && isFullScreenMode) {
                    event.consume();
                    exitFullScreen();
                }
                if (event.isControlDown() && event.getCode() == KeyCode.ENTER) {
                    event.consume();
                    if (executionCount < MAX_EXECUTIONS) {
                        executeWithPythonium();
                    } else {
                        showExecutionLimitReached();
                    }
                }
            });
        }

        if (fullScreenCodeEditor != null) {
            fullScreenCodeEditor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    event.consume();
                    exitFullScreen();
                }
                if (event.isControlDown() && event.getCode() == KeyCode.ENTER) {
                    event.consume();
                    if (executionCount < MAX_EXECUTIONS) {
                        evaluateFromFullScreen();
                    } else {
                        showExecutionLimitReached();
                    }
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

        fullScreenCodeEditor.setText(txtCode.getText());
        fullScreenCodeEditor.setPromptText(txtCode.getPromptText());
        fullScreenCodeEditor.setEditable(txtCode.isEditable());
        fullScreenCodeEditor.setDisable(txtCode.isDisabled());

        if (fullScreenTimer != null && lblTimer != null) {
            fullScreenTimer.setText(lblTimer.getText());
            fullScreenTimer.setStyle(lblTimer.getStyle());
        }

        fullScreenEditorOverlay.setVisible(true);
        fullScreenEditorOverlay.setManaged(true);
        fullScreenEditorOverlay.toFront();

        Platform.runLater(() -> {
            fullScreenCodeEditor.requestFocus();
            fullScreenCodeEditor.positionCaret(fullScreenCodeEditor.getText().length());
        });

        isFullScreenMode = true;
        setupFullScreenSecurity();
    }

    @FXML
    private void exitFullScreen() {
        if (fullScreenEditorOverlay == null || txtCode == null) return;

        txtCode.setText(fullScreenCodeEditor.getText());
        fullScreenEditorOverlay.setVisible(false);
        fullScreenEditorOverlay.setManaged(false);
        isFullScreenMode = false;
    }

    private void setupFullScreenSecurity() {
        if (fullScreenCodeEditor == null) return;

        fullScreenCodeEditor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isShortcutDown()) {
                if (event.getCode() == KeyCode.C || event.getCode() == KeyCode.V ||
                        event.getCode() == KeyCode.X || event.getCode() == KeyCode.A) {
                    event.consume();
                    showSecurityWarning("Copy, paste, and cut are disabled during the mission!");
                }
            }
        });

        fullScreenCodeEditor.setContextMenu(null);
        fullScreenCodeEditor.setOnDragOver(event -> {
            event.consume();
            showSecurityWarning("Drag and drop is disabled!");
        });
        fullScreenCodeEditor.setOnDragDropped(event -> event.consume());
    }

    @FXML
    private void evaluateFromFullScreen() {
        if (fullScreenCodeEditor != null && txtCode != null) {
            txtCode.setText(fullScreenCodeEditor.getText());
            exitFullScreen();
            if (executionCount < MAX_EXECUTIONS) {
                executeWithPythonium();
            } else {
                showExecutionLimitReached();
            }
        }
    }

    // ============================
    // MISSION SETUP
    // ============================

    public void setMissionId(int missionId) {
        executionCount = 0;
        updateExecutionCounter();

        if (btnExecute != null) {
            btnExecute.setDisable(false);
        }

        if (lblKernelStatus != null) {
            lblKernelStatus.setText("✅ Ready");
            lblKernelStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
        }

        Platform.runLater(() -> {
            txtMissionId.setText(String.valueOf(missionId));
            validateMissionId();

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
                    startTimer(DEFAULT_TIMER_MINUTES * 60);
                } else {
                    lblMissionDescription.setText("❌ Mission not found (ID: " + missionId + ")");
                }
            } catch (Exception e) {
                lblMissionDescription.setText("⚠️ Error loading mission details: " + e.getMessage());
                e.printStackTrace();
            }

            if (consoleOutput != null) {
                consoleOutput.clear();
                currentOutput.setLength(0);
            }
            appendToConsole("🆕 New mission loaded. You have " + MAX_EXECUTIONS + " execution attempts.", "#8b5cf6");
        });
    }

    // ============================
    // TIMER FUNCTIONALITY
    // ============================

    private void startTimer(int seconds) {
        if (timerTimeline != null) {
            timerTimeline.stop();
        }

        this.remainingSeconds = seconds;

        if (timerContainer != null) {
            timerContainer.setVisible(true);
            timerContainer.setManaged(true);
        }

        isTimerFinished = false;
        updateTimerDisplay();

        if (timerProgressBar != null) {
            timerProgressBar.setProgress(1.0);
            timerProgressBar.setStyle("-fx-accent: #10b981;");
        }

        if (lblTimerProgress != null) {
            lblTimerProgress.setText("100%");
        }

        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), this::updateTimer));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();
    }

    private void updateTimer(ActionEvent event) {
        if (isTimerFinished) return;

        remainingSeconds--;

        if (remainingSeconds <= 0) {
            timerFinished();
        } else {
            updateTimerDisplay();
            updateTimerProgress();
        }

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

        if (remainingSeconds <= CRITICAL_THRESHOLD_SECONDS) {
            lblTimer.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 32px;");
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

        if (txtCode != null) {
            txtCode.setEditable(false);
            txtCode.setDisable(true);
        }

        if (fullScreenCodeEditor != null) {
            fullScreenCodeEditor.setEditable(false);
            fullScreenCodeEditor.setDisable(true);
        }

        if (lblTimerWarning != null) {
            lblTimerWarning.setText("⏰ TIME'S UP! Auto-submitting your solution...");
            lblTimerWarning.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 14px;");
            lblTimerWarning.setVisible(true);
        }

        if (isAutoSubmitEnabled && txtCode != null && !txtCode.getText().trim().isEmpty()) {
            evaluer();
        } else if (txtCode != null && txtCode.getText().trim().isEmpty()) {
            if (lblResultat != null) {
                lblResultat.setText("❌ Time's up! No code submitted.");
                lblResultat.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            }
            AlertUtils.showWarning("Time's Up", "You didn't submit any code before the timer ended.");
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

        txtCode.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isShortcutDown()) {
                if (event.getCode() == KeyCode.C || event.getCode() == KeyCode.V ||
                        event.getCode() == KeyCode.X || event.getCode() == KeyCode.A) {
                    event.consume();
                    showSecurityWarning("Copy, paste, and cut are disabled during the mission!");
                }
            }
        });

        txtCode.setContextMenu(null);
        txtCode.setOnDragOver(event -> {
            event.consume();
            showSecurityWarning("Drag and drop is disabled!");
        });
        txtCode.setOnDragDropped(event -> event.consume());
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

        Timeline hideTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            securityWarningContainer.setVisible(false);
            securityWarningContainer.setManaged(false);
        }));
        hideTimeline.setCycleCount(1);
        hideTimeline.play();
    }

    // ============================
    // EVALUATION METHOD WITH SMS FOR ALL SUBMISSIONS
    // ============================

    @FXML
    private void evaluer() {
        if (!validateAllFields()) {
            StringBuilder errors = new StringBuilder();
            if (!validateCandidatId()) errors.append("• Invalid Candidate ID\n");
            if (!validateMissionId()) errors.append("• Invalid Mission ID\n");
            if (!validateCode()) errors.append("• Invalid Python code\n");
            if (!validateMissionType()) errors.append("• Mission type required\n");
            AlertUtils.showWarning("Validation Error", errors.toString());
            return;
        }

        if (timerTimeline != null && !isTimerFinished) {
            timerTimeline.stop();
            isTimerFinished = true;
        }

        int missionId, candidatId;

        try {
            missionId = Integer.parseInt(txtMissionId.getText());
            candidatId = Integer.parseInt(txtCandidatId.getText());

        } catch (Exception e) {
            AlertUtils.showError("Invalid IDs", "Please enter valid numbers for Mission ID and Candidate ID");
            if (!isTimerFinished) {
                startTimer(remainingSeconds > 0 ? remainingSeconds : DEFAULT_TIMER_MINUTES * 60);
            }
            return;
        }

        progress.setVisible(true);
        if (lblProcessing != null) {
            lblProcessing.setVisible(true);
            lblProcessing.setText("Evaluating code and preparing SMS notification...");
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
                // Evaluate the code
                RenduMission r = service.evaluerCodePython(txtCode.getText(), missionId, candidatId);

                // Get mission details for SMS
                Mission mission = missionService.getById(missionId);
                int missionIdForSms = (mission != null) ? mission.getId() : missionId;

                // Get candidate name (you might need to fetch this from a service)
                String candidateName = "Candidate";

                // Send SMS notification for ALL submissions (both accepted and rejected)
                boolean smsSent = false;
                if (smsService != null && smsService.isEnabled()) {
                    // Use the new method that sends score and status
                    smsSent = smsService.envoyerSMSResultat(
                            candidateName,
                            missionIdForSms,
                            r.getScore(),
                            r.isAccepted()
                    );

                    if (smsSent) {
                        System.out.println("📱 SMS sent successfully to " + smsService.getRecipientPhoneNumber());
                    } else {
                        System.out.println("📱 SMS failed to send");
                    }
                }

                final boolean finalSmsSent = smsSent;
                final String fromNumber = smsService != null ? smsService.getFromPhoneNumber() : "+18122864465";
                final String toNumber = smsService != null ? smsService.getRecipientPhoneNumber() : "+21693039271";
                final int finalScore = r.getScore();
                final boolean accepted = r.isAccepted();

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

                    String resultText = "🎯 AI Score: " + finalScore + "% - " + r.getResultat() + "\n";

                    if (accepted) {
                        resultText += "✅ ACCEPTED! (Score meets minimum requirement)\n";
                        lblResultat.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 14px;");
                    } else {
                        resultText += "❌ REJECTED (Score below minimum requirement)\n";
                        lblResultat.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 14px;");
                    }

                    // Add SMS status to result
                    if (finalSmsSent) {
                        resultText += "📱 SMS notification sent to 93039271 with score " + finalScore + "% ✓";

                        // Show detailed alert with SMS info
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Submission Complete");
                        alert.setHeaderText(accepted ? "✅ Code Accepted & SMS Sent" : "❌ Code Rejected & SMS Sent");
                        alert.setContentText(String.format(
                                "Your code has been evaluated with a score of %d%%!\n\n" +
                                        "📱 An SMS notification has been sent from:\n%s (Twilio Trial)\n\n" +
                                        "To:\n%s\n\n" +
                                        "The recipient has been notified of your result for Mission #%d.",
                                finalScore,
                                fromNumber,
                                toNumber,
                                missionIdForSms
                        ));
                        alert.showAndWait();
                    } else {
                        resultText += "📱 SMS notification could not be sent (service unavailable)";
                        if (accepted) {
                            AlertUtils.showSuccess("Code Accepted",
                                    "Your code has been accepted with a score of " + finalScore + "%!\n" +
                                            "(SMS notification was not sent - check Twilio configuration)");
                        } else {
                            AlertUtils.showWarning("Code Rejected",
                                    "Your code scored " + finalScore + "%, which is below the minimum requirement.\n" +
                                            "(SMS notification was not sent - check Twilio configuration)");
                        }
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
                    AlertUtils.showError("Evaluation Error", e.getMessage());
                });
            }
        }).start();
    }

    private Button getEvaluateButton() {
        if (txtMissionId != null && txtMissionId.getScene() != null) {
            return (Button) txtMissionId.getScene().lookup("#btnEvaluer");
        }
        return null;
    }

    public void setAutoSubmitEnabled(boolean enabled) {
        this.isAutoSubmitEnabled = enabled;
    }
}