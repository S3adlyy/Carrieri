package com.example.guser.controllers.greclam;

import entities.greclam.Feedback;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import services.greclam.FeedbackService;

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
    // @FXML private TextField renduIdField;  ← SUPPRIMÉ
    @FXML private DatePicker datePicker;

    private Feedback feedback;
    private ObservableList<Feedback> feedbackList;
    private FeedbackService feedbackService = new FeedbackService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser le slider avec un listener
        noteSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            noteLabel.setText(String.valueOf(newValue.intValue()));
        });

        // Initialiser le DatePicker avec la date du jour
        datePicker.setValue(LocalDate.now());
    }

    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;

        if (feedback != null) {
            dialogTitle.setText("Modifier le feedback #" + feedback.getId());
            commentaireArea.setText(feedback.getCommentaire());
            noteSlider.setValue(feedback.getNote());
            // renduIdField.setText(String.valueOf(feedback.getRenduId()));  ← SUPPRIMÉ

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
        if (!validateInput()) return;

        try {
            if (feedback == null) {
                // Ajout d'un nouveau feedback
                Feedback newFeedback = new Feedback();
                newFeedback.setCommentaire(commentaireArea.getText().trim());
                newFeedback.setNote((int) noteSlider.getValue());
                // newFeedback.setRenduId(0);  ← SUPPRIMÉ (ou valeur par défaut si nécessaire)

                if (datePicker != null && datePicker.getValue() != null) {
                    newFeedback.setCreatedAt(Date.from(datePicker.getValue()
                            .atStartOfDay(ZoneId.systemDefault()).toInstant()));
                } else {
                    newFeedback.setCreatedAt(new Date());
                }

                feedbackService.ajouter(newFeedback);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Feedback ajouté avec succès !");

                // ✅ RESTER SUR LE FORMULAIRE après ajout
                clearForm();

            } else {
                // Modification d'un feedback existant
                feedback.setCommentaire(commentaireArea.getText().trim());
                feedback.setNote((int) noteSlider.getValue());
                // feedback.setRenduId(Integer.parseInt(renduIdField.getText().trim()));  ← SUPPRIMÉ

                if (datePicker != null && datePicker.getValue() != null) {
                    feedback.setCreatedAt(Date.from(datePicker.getValue()
                            .atStartOfDay(ZoneId.systemDefault()).toInstant()));
                }

                feedbackService.update(feedback);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Feedback modifié avec succès !");

                // ✅ POUR LA MODIFICATION : Retour à la liste
                goBackToList();
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de sauvegarde: " + e.getMessage());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Format invalide");
        }
    }

    // ✅ Réinitialiser le formulaire pour un nouvel ajout
    private void clearForm() {
        commentaireArea.clear();
        noteSlider.setValue(50);
        // renduIdField.clear();  ← SUPPRIMÉ
        datePicker.setValue(LocalDate.now());
        feedback = null;
        dialogTitle.setText("Ajouter un nouveau feedback");
    }

    @FXML
    private void goBackToList() {
        try {
            Parent listPage = FXMLLoader.load(getClass().getResource("/com/example/guser/greclam/feedbackForm.fxml"));
            commentaireArea.getScene().setRoot(listPage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean validateInput() {
        if (commentaireArea.getText() == null || commentaireArea.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Le commentaire est requis.");
            commentaireArea.requestFocus();
            return false;
        }

        if (commentaireArea.getText().trim().length() < 5) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Le commentaire doit contenir au moins 5 caractères.");
            commentaireArea.requestFocus();
            return false;
        }

        // ✅ Validation du Rendu ID supprimée

        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}