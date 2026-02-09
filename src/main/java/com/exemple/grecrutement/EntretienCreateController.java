package com.exemple.grecrutement;

import entities.Entretien;
import entities.RenduMission;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import services.EntretienService;
import services.EntretienService.CandidateInfo;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
                "Entretien Vidéo"
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
            CandidateInfo candidateInfo = entretienService.getCandidateInfoFromRendu(rendu.getId());

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
                showAlert(Alert.AlertType.WARNING, "Attention",
                        "Impossible de récupérer les informations du candidat. Veuillez les saisir manuellement.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur lors de la récupération des informations: " + e.getMessage());
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
                showAlert(Alert.AlertType.WARNING, "Date invalide",
                        "La date de l'entretien ne peut pas être dans le passé.");
                return;
            }

            // Create entretien object
            Entretien entretien = new Entretien();
            entretien.setDateEntretien(dateTime);
            entretien.setType(comboType.getValue());
            entretien.setStatus("SCHEDULED");
            entretien.setPostulationId(renduMissionId); // Using rendu_mission id as reference

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

            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    String.format("L'entretien a été programmé pour le %s à %02d:%02d\n\n" +
                                    "Un email de confirmation a été envoyé à: %s",
                            selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
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
            showAlert(Alert.AlertType.ERROR, "Erreur",
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
            showAlert(Alert.AlertType.WARNING, "Champ requis",
                    "Veuillez saisir l'email du candidat.");
            return false;
        }

        if (txtCandidatName.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ requis",
                    "Veuillez saisir le nom du candidat.");
            return false;
        }

        if (datePicker.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Champ requis",
                    "Veuillez sélectionner une date.");
            return false;
        }

        if (comboHour.getValue() == null || comboMinute.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Champ requis",
                    "Veuillez sélectionner l'heure de l'entretien.");
            return false;
        }

        if (comboType.getValue() == null || comboType.getValue().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ requis",
                    "Veuillez sélectionner le type d'entretien.");
            return false;
        }

        // Validate email format
        String email = txtCandidatEmail.getText().trim();
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showAlert(Alert.AlertType.WARNING, "Email invalide",
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

    /**
     * Show alert dialog
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}