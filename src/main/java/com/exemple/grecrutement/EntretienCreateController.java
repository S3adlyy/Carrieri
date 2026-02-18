package com.exemple.grecrutement;

import entities.Entretien;
import entities.RenduMission;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import services.EntretienService;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class EntretienCreateController implements Initializable {

    @FXML private Label lblCandidatInfo;
    @FXML private TextField txtCandidatId;
    @FXML private TextField txtCandidatEmail;
    @FXML private TextField txtCandidatName;

    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> comboHour;
    @FXML private ComboBox<String> comboMinute;
    @FXML private ComboBox<String> comboType;
    @FXML private TextArea txtNotes;

    @FXML private Button btnSchedule;
    @FXML private Label lblStatus;

    private EntretienService entretienService;
    private int renduMissionId;
    private RenduMission selectedRendu;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        entretienService = new EntretienService();

        // Initialize time combos
        setupTimeComboBoxes();

        // Initialize interview types
        comboType.getItems().addAll(
                "Entretien Technique",
                "Entretien RH",
                "Entretien avec le Manager",
                "Entretien Final",
                "Entretien Téléphonique",
                "Entretien Vidéo",
                "Entretirn Code"
        );
        comboType.setValue("Entretien Technique");

        // Set minimum date to today
        datePicker.setValue(LocalDate.now());

        // Clear status
        lblStatus.setText("");
    }

    /**
     * Set the rendu mission that triggered this interview creation
     */
    public void setRenduMission(RenduMission rendu) {
        this.selectedRendu = rendu;
        this.renduMissionId = rendu.getId();

        try {
            // Get candidate information from the rendu
            EntretienService.CandidateInfo candidateInfo = entretienService.getCandidateInfoFromRendu(rendu.getId());

            if (candidateInfo != null) {
                txtCandidatId.setText(String.valueOf(candidateInfo.candidatId));
                txtCandidatEmail.setText(candidateInfo.email);
                txtCandidatName.setText(candidateInfo.name);

                // Display rendu information
                lblCandidatInfo.setText(String.format(
                        "📊 Rendu #%d | Score: %d%% | Résultat: %s",
                        rendu.getId(),
                        rendu.getScore(),
                        rendu.getResultat()
                ));

                lblCandidatInfo.setStyle(rendu.isAccepted()
                        ? "-fx-text-fill: #10b981; -fx-font-weight: bold;"
                        : "-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
            } else {
                // FALLBACK: Use data from the RenduMission object itself
                txtCandidatId.setText(String.valueOf(rendu.getCandidatId()));
                txtCandidatEmail.setText("candidate" + rendu.getCandidatId() + "@email.com");
                txtCandidatName.setText("Candidate #" + rendu.getCandidatId());

                lblCandidatInfo.setText(String.format(
                        "📊 Rendu #%d | Score: %d%% | Résultat: %s (Informations générées automatiquement)",
                        rendu.getId(),
                        rendu.getScore(),
                        rendu.getResultat()
                ));

                showWarningAlert("Attention",
                        "Informations du candidat générées automatiquement. Veuillez vérifier et modifier si nécessaire.");
            }

        } catch (Exception e) {
            e.printStackTrace();

            // FALLBACK: Use data from the RenduMission object
            txtCandidatId.setText(String.valueOf(rendu.getCandidatId()));
            txtCandidatEmail.setText("candidate" + rendu.getCandidatId() + "@email.com");
            txtCandidatName.setText("Candidate #" + rendu.getCandidatId());

            lblCandidatInfo.setText(String.format(
                    "📊 Rendu #%d | Score: %d%% | Résultat: %s",
                    rendu.getId(),
                    rendu.getScore(),
                    rendu.getResultat()
            ));

            showErrorAlert("Erreur",
                    "Erreur lors de la récupération des informations: " + e.getMessage() +
                            "\n\nDes informations par défaut ont été générées.");
        }
    }

    /**
     * Setup time combo boxes with hours and minutes
     */
    private void setupTimeComboBoxes() {
        // Hours: 08:00 to 18:00
        for (int hour = 8; hour <= 18; hour++) {
            comboHour.getItems().add(String.format("%02d", hour));
        }
        comboHour.setValue("09");

        // Minutes: 00, 15, 30, 45
        comboMinute.getItems().addAll("00", "15", "30", "45");
        comboMinute.setValue("00");
    }

    /**
     * Schedule the interview
     */
    @FXML
    private void scheduleInterview() {
        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        try {
            // Get selected date and time
            LocalDate selectedDate = datePicker.getValue();
            int hour = Integer.parseInt(comboHour.getValue());
            int minute = Integer.parseInt(comboMinute.getValue());
            LocalDateTime dateTime = LocalDateTime.of(selectedDate, LocalTime.of(hour, minute));

            // Check if date is in the past
            if (dateTime.isBefore(LocalDateTime.now())) {
                showWarningAlert("Date invalide",
                        "La date de l'entretien ne peut pas être dans le passé.");
                return;
            }

            // Create entretien object
            Entretien entretien = new Entretien();
            entretien.setDateEntretien(dateTime);
            entretien.setType(comboType.getValue());
            entretien.setStatus("SCHEDULED");
            entretien.setPostulationId(renduMissionId); // This will be converted in createEntretien

            // Get candidate info
            String candidatEmail = txtCandidatEmail.getText().trim();
            String candidatName = txtCandidatName.getText().trim();

            // Disable button during processing
            btnSchedule.setDisable(true);
            lblStatus.setText("⏳ Création de l'entretien en cours...");
            lblStatus.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");

            // Create entretien and send email
            entretienService.createEntretien(entretien, candidatEmail, candidatName);

            // Show success message
            lblStatus.setText("✅ Entretien programmé avec succès! Email envoyé.");
            lblStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");

            showSuccessAlert("Succès",
                    String.format("L'entretien a été programmé pour le %s à %02d:%02d\n\n" +
                                    "Un email de confirmation a été envoyé à: %s",
                            selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            hour, minute, candidatEmail));

            // Clear form after short delay
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(this::clearForm);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("❌ Erreur lors de la création");
            lblStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            showErrorAlert("Erreur",
                    "Erreur lors de la création de l'entretien: " + e.getMessage());
        } finally {
            btnSchedule.setDisable(false);
        }
    }

    /**
     * Validate all inputs
     */
    private boolean validateInputs() {
        if (txtCandidatEmail.getText().trim().isEmpty()) {
            showWarningAlert("Champ requis",
                    "Veuillez saisir l'email du candidat.");
            return false;
        }

        if (txtCandidatName.getText().trim().isEmpty()) {
            showWarningAlert("Champ requis",
                    "Veuillez saisir le nom du candidat.");
            return false;
        }

        if (datePicker.getValue() == null) {
            showWarningAlert("Champ requis",
                    "Veuillez sélectionner une date.");
            return false;
        }

        if (comboHour.getValue() == null || comboMinute.getValue() == null) {
            showWarningAlert("Champ requis",
                    "Veuillez sélectionner l'heure de l'entretien.");
            return false;
        }

        if (comboType.getValue() == null || comboType.getValue().isEmpty()) {
            showWarningAlert("Champ requis",
                    "Veuillez sélectionner le type d'entretien.");
            return false;
        }

        // Validate email format
        String email = txtCandidatEmail.getText().trim();
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showWarningAlert("Email invalide",
                    "Veuillez saisir un email valide.");
            return false;
        }

        return true;
    }

    /**
     * Clear the form
     */
    @FXML
    private void clearForm() {
        datePicker.setValue(LocalDate.now());
        comboHour.setValue("09");
        comboMinute.setValue("00");
        comboType.setValue("Entretien Technique");
        txtNotes.clear();
        lblStatus.setText("");
    }

    /**
     * Cancel and go back
     */
    @FXML
    private void cancel() {
        MissionShellController.getInstance().showRenduList();
    }

    // ============ STYLED ALERT METHODS ============

    /**
     * Show styled success alert
     */
    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("✅ " + title);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/Alert.css").toExternalForm());
        dialogPane.getStyleClass().add("information");

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setStyle(
                    "-fx-background-color: #10b981;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 8;" +
                            "-fx-padding: 10 25;"
            );
        }

        alert.showAndWait();
    }

    /**
     * Show styled warning alert
     */
    private void showWarningAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText("⚠️ " + title);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/Alert.css").toExternalForm());
        dialogPane.getStyleClass().add("warning");

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setStyle(
                    "-fx-background-color: #f59e0b;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 8;" +
                            "-fx-padding: 10 25;"
            );
        }

        alert.showAndWait();
    }

    /**
     * Show styled error alert
     */
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("❌ " + title);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/Alert.css").toExternalForm());
        dialogPane.getStyleClass().add("error");

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setStyle(
                    "-fx-background-color: #ef4444;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 8;" +
                            "-fx-padding: 10 25;"
            );
        }

        alert.showAndWait();
    }

    /**
     * Show styled info alert
     */
    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("ℹ️ " + title);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/Alert.css").toExternalForm());
        dialogPane.getStyleClass().add("information");

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setStyle(
                    "-fx-background-color: #3b82f6;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 8;" +
                            "-fx-padding: 10 25;"
            );
        }

        alert.showAndWait();
    }

    /**
     * Legacy showAlert method - updated to use styled alerts
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        switch (type) {
            case ERROR:
                showErrorAlert(title, message);
                break;
            case WARNING:
                showWarningAlert(title, message);
                break;
            case INFORMATION:
                showInfoAlert(title, message);
                break;
            case CONFIRMATION:
                // For confirmation, we'll use info style with a different button
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle(title);
                confirm.setHeaderText("❓ " + title);
                confirm.setContentText(message);

                DialogPane dialogPane = confirm.getDialogPane();
                dialogPane.getStylesheets().add(getClass().getResource("/Alert.css").toExternalForm());
                dialogPane.getStyleClass().add("information");

                ButtonBar buttonBar = (ButtonBar) dialogPane.lookup(".button-bar");
                if (buttonBar != null) {
                    buttonBar.getButtons().forEach(button -> {
                        if (button instanceof Button) {
                            Button btn = (Button) button;
                            if (btn.getText().equals("OK") || btn.getText().equals("Yes")) {
                                btn.setStyle(
                                        "-fx-background-color: #3b82f6;" +
                                                "-fx-text-fill: white;" +
                                                "-fx-font-weight: bold;" +
                                                "-fx-background-radius: 8;" +
                                                "-fx-padding: 10 25;"
                                );
                            } else {
                                btn.setStyle(
                                        "-fx-background-color: transparent;" +
                                                "-fx-text-fill: #6b7280;" +
                                                "-fx-border-color: #e2e8f0;" +
                                                "-fx-border-width: 1;" +
                                                "-fx-border-radius: 8;" +
                                                "-fx-background-radius: 8;" +
                                                "-fx-padding: 10 25;"
                                );
                            }
                        }
                    });
                }

                confirm.showAndWait();
                break;
            default:
                showInfoAlert(title, message);
                break;
        }
    }
}