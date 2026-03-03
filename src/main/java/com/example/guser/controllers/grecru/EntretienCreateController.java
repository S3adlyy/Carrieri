package com.example.guser.controllers.grecru;

import com.example.guser.controllers.guser.AppNavController;
import entities.grecru.Entretien;
import entities.grecru.RenduMission;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import services.grecru.EntretienService;
import session.SessionContext;
import utils.grecru.AlertUtils;

import javax.mail.Session;
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

    private static RenduMission currentRendu = null;

    public static void setCurrentRendu(RenduMission rendu) {
        currentRendu = rendu;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("Current rendu in init entretien: " + currentRendu);
        if(currentRendu != null) {
            txtCandidatId.setText(String.valueOf(currentRendu.getCandidatId()));
        }
        else{
            txtCandidatId.setText(String.valueOf(""));

        }

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
                "Entretien Code"
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
        System.out.println("rendu passed in controller: " + rendu);

        System.out.println("Current rendu set in controller: " + this.selectedRendu);

        try {
            // Get candidate information from the rendu
            EntretienService.CandidateInfo candidateInfo = entretienService.getCandidateInfoFromRendu(rendu.getId());

            if (candidateInfo != null) {
                txtCandidatId.setText(String.valueOf(candidateInfo.candidatId));
                txtCandidatEmail.setText(SessionContext.getCurrentUser().getEmail());
                txtCandidatName.setText(SessionContext.getCurrentUser().getFirstname());

                // Display rendu information with mission ID
                lblCandidatInfo.setText(String.format(
                        "📊 Rendu #%d | Score: %d%% | Résultat: %s | 🎯 Mission #%d",
                        rendu.getId(),
                        rendu.getScore(),
                        rendu.getResultat(),
                        candidateInfo.missionId
                ));

                lblCandidatInfo.setStyle(rendu.isAccepted()
                        ? "-fx-text-fill: #10b981; -fx-font-weight: bold;"
                        : "-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
            } else {
                // FALLBACK: Use data from the RenduMission object itself
                txtCandidatId.setText(String.valueOf(rendu.getCandidatId()));
                txtCandidatEmail.setText(SessionContext.getCurrentUser().getEmail());
                txtCandidatName.setText(SessionContext.getCurrentUser().getFirstname());

                lblCandidatInfo.setText(String.format(
                        "📊 Rendu #%d | Score: %d%% | Résultat: %s (Informations générées automatiquement)",
                        rendu.getId(),
                        rendu.getScore(),
                        rendu.getResultat()
                ));

                AlertUtils.showWarning("Attention",
                        "Informations du candidat générées automatiquement. Veuillez vérifier et modifier si nécessaire.");
            }

        } catch (Exception e) {
            e.printStackTrace();

            // FALLBACK: Use data from the RenduMission object
            txtCandidatId.setText(String.valueOf(rendu.getCandidatId()));
            txtCandidatEmail.setText(SessionContext.getCurrentUser().getEmail());
            txtCandidatName.setText(SessionContext.getCurrentUser().getFirstname());

            lblCandidatInfo.setText(String.format(
                    "📊 Rendu #%d | Score: %d%% | Résultat: %s",
                    rendu.getId(),
                    rendu.getScore(),
                    rendu.getResultat()
            ));

            AlertUtils.showError("Erreur",
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
     * Schedule the interview with Jitsi Meet link
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
                AlertUtils.showWarning("Date invalide",
                        "La date de l'entretien ne peut pas être dans le passé.");
                return;
            }

            // Create entretien object
            Entretien entretien = new Entretien();
            entretien.setDateEntretien(dateTime);
            entretien.setType(comboType.getValue());
            entretien.setStatus("SCHEDULED");
            entretien.setPostulationId(renduMissionId);

            // Get candidate info
            String candidatEmail = SessionContext.getCurrentUser().getEmail();
            String candidatName = SessionContext.getCurrentUser().getFirstname() + " " + SessionContext.getCurrentUser().getLastname();
            int candidatId = Integer.parseInt(txtCandidatId.getText());

            // Get mission ID from selected rendu or from candidateInfo
            int missionId = 0;
            if (selectedRendu != null) {
                missionId = selectedRendu.getMissionId();
            }

            // Disable button during processing
            btnSchedule.setDisable(true);
            lblStatus.setText("⏳ Création de l'entretien et génération du lien Jitsi...");
            lblStatus.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");

            // Create entretien and send email with Jitsi link
            entretienService.createEntretienWithJitsi(entretien, candidatEmail, candidatName, candidatId, missionId);

            // Show success message
            lblStatus.setText("✅ Entretien programmé avec succès! Lien Jitsi envoyé par email.");
            lblStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");

            AlertUtils.showSuccess("Succès avec Jitsi Meet",
                    String.format("L'entretien a été programmé pour le %s à %02d:%02d\n\n" +
                                    "✅ Un lien Jitsi Meet a été généré et envoyé à:\n%s\n\n" +
                                    "Le candidat pourra rejoindre la visio directement depuis son navigateur.\n\n" +
                                    "🎥 Lien: https://meet.jit.si/Carrieri-CANDIDAT%d-MISSION%d-ENTRETIEN%d",
                            selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            hour, minute,
                            candidatEmail,
                            candidatId, missionId, entretien.getId()));

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
            AlertUtils.showError("Erreur",
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
            AlertUtils.showWarning("Champ requis",
                    "Veuillez saisir l'email du candidat.");
            return false;
        }

        if (txtCandidatName.getText().trim().isEmpty()) {
            AlertUtils.showWarning("Champ requis",
                    "Veuillez saisir le nom du candidat.");
            return false;
        }

        if (datePicker.getValue() == null) {
            AlertUtils.showWarning("Champ requis",
                    "Veuillez sélectionner une date.");
            return false;
        }

        if (comboHour.getValue() == null || comboMinute.getValue() == null) {
            AlertUtils.showWarning("Champ requis",
                    "Veuillez sélectionner l'heure de l'entretien.");
            return false;
        }

        if (comboType.getValue() == null || comboType.getValue().isEmpty()) {
            AlertUtils.showWarning("Champ requis",
                    "Veuillez sélectionner le type d'entretien.");
            return false;
        }

        // Validate email format
        String email = txtCandidatEmail.getText().trim();
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            AlertUtils.showWarning("Email invalide",
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
        AppNavController.getInstance().onRenduShow(null);
    }
}

