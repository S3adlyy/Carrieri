package controllers;

import entities.Feedback;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.FeedbackService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.ResourceBundle;

public class FeedbackFormController implements Initializable {

    @FXML private Label dialogTitle;
    @FXML private TextArea commentaireArea;
    @FXML private Slider noteSlider;
    @FXML private Label noteLabel;
    @FXML private TextField renduIdField;
    @FXML private DatePicker datePicker;

    private Feedback feedback;
    private ObservableList<Feedback> feedbackList;
    private FeedbackService feedbackService = new FeedbackService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Initialisation du formulaire Feedback...");

        // Initialiser le slider avec un listener
        noteSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            noteLabel.setText(String.valueOf(newValue.intValue()));
        });

        // Initialiser le DatePicker avec la date du jour
        datePicker.setValue(LocalDate.now());

        // Valeurs par défaut
        noteSlider.setValue(50);
    }

    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;

        if (feedback != null) {
            dialogTitle.setText("Modifier le feedback #" + feedback.getId());
            commentaireArea.setText(feedback.getCommentaire());
            noteSlider.setValue(feedback.getNote());
            renduIdField.setText(String.valueOf(feedback.getRenduId()));

            if (feedback.getCreatedAt() != null) {
                datePicker.setValue(feedback.getCreatedAt().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate());
            }
        } else {
            dialogTitle.setText("Ajouter un nouveau feedback");
        }
    }

    public void setFeedbackList(ObservableList<Feedback> feedbackList) {
        this.feedbackList = feedbackList;
    }

    @FXML
    private void handleSave() {
        System.out.println("🔵 handleSave() Feedback est appelé !");
        System.out.println("Commentaire: " + commentaireArea.getText());
        System.out.println("Note: " + (int) noteSlider.getValue());

        if (!validateInput()) {
            System.out.println("❌ Validation échouée");
            return;
        }

        System.out.println("✅ Validation réussie");

        try {
            if (feedback == null) {
                // Ajouter nouveau feedback
                Feedback newFeedback = new Feedback();
                newFeedback.setCommentaire(commentaireArea.getText().trim());
                newFeedback.setNote((int) noteSlider.getValue());
                newFeedback.setRenduId(Integer.parseInt(renduIdField.getText().trim()));

                // Gestion de la date
                if (datePicker != null && datePicker.getValue() != null) {
                    newFeedback.setCreatedAt(Date.from(datePicker.getValue()
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()));
                } else {
                    newFeedback.setCreatedAt(new Date());
                }

                System.out.println("📤 Envoi à la base de données (AJOUT FEEDBACK)...");
                feedbackService.ajouter(newFeedback);
                System.out.println("✅ Ajout feedback réussi en base !");

                showAlert(Alert.AlertType.INFORMATION, "Succès", "Feedback ajouté",
                        "Le feedback a été ajouté avec succès.");

                closeDialog();

            } else {
                // Modifier feedback existant
                System.out.println("🔄 Modification du feedback #" + feedback.getId());

                feedback.setCommentaire(commentaireArea.getText().trim());
                feedback.setNote((int) noteSlider.getValue());
                feedback.setRenduId(Integer.parseInt(renduIdField.getText().trim()));

                if (datePicker != null && datePicker.getValue() != null) {
                    feedback.setCreatedAt(Date.from(datePicker.getValue()
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()));
                }

                System.out.println("📤 Envoi à la base de données (MODIFICATION FEEDBACK)...");
                feedbackService.update(feedback);
                System.out.println("✅ Modification feedback réussie en base !");

                showAlert(Alert.AlertType.INFORMATION, "Succès", "Feedback modifié",
                        "Le feedback a été modifié avec succès.");

                closeDialog();
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de sauvegarde",
                    "Détails: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("❌ Erreur format: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur", "Format invalide",
                    "Le Rendu ID doit être un nombre valide.");
        }
    }

    @FXML
    private void handleCancel() {
        System.out.println("Annulation...");
        closeDialog();
    }

    private boolean validateInput() {
        if (commentaireArea.getText() == null || commentaireArea.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Champ requis",
                    "Le champ 'Commentaire' est requis.");
            commentaireArea.requestFocus();
            return false;
        }

        if (commentaireArea.getText().trim().length() < 5) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Commentaire trop court",
                    "Le commentaire doit contenir au moins 5 caractères.");
            commentaireArea.requestFocus();
            return false;
        }

        if (renduIdField.getText() == null || renduIdField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Champ requis",
                    "Le champ 'Rendu ID' est requis.");
            renduIdField.requestFocus();
            return false;
        }

        try {
            int renduId = Integer.parseInt(renduIdField.getText().trim());
            if (renduId <= 0) {
                showAlert(Alert.AlertType.WARNING, "Validation", "ID invalide",
                        "Le Rendu ID doit être un nombre positif.");
                renduIdField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Format invalide",
                    "Le Rendu ID doit être un nombre valide.");
            renduIdField.requestFocus();
            return false;
        }

        if (datePicker.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Champ requis",
                    "Veuillez sélectionner une date.");
            datePicker.requestFocus();
            return false;
        }

        return true;
    }

    // CORRECTION - Méthode closeDialog() qui fonctionne
    private void closeDialog() {
        try {
            Stage stage = (Stage) commentaireArea.getScene().getWindow();
            stage.close();
            System.out.println("✅ Dialogue Feedback fermé");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la fermeture: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}