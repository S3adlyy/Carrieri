package com.example.guser.controllers.greclam;

import entities.greclam.Reclamation;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import services.greclam.PrioriteService;
import services.greclam.ReclamationService;
import session.SessionContext;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.ResourceBundle;

public class ReclamationFormController implements Initializable {

    @FXML private Label dialogTitle;
    @FXML private TextField objetField;
    @FXML private ComboBox<String> categorieCombo;
    @FXML private ComboBox<String> prioriteCombo; // À SUPPRIMER DU FXML AUSSI
    @FXML private TextArea descriptionArea;
    @FXML private DatePicker datePicker;
    @FXML private TextField emailField;
    private PrioriteService prioriteService = new PrioriteService();
    // @FXML private ComboBox<String> prioriteCombo;  ← À SUPPRIMER

    private Reclamation reclamation;
    private boolean saved = false;
    private ReclamationService reclamationService = new ReclamationService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeComboBoxes();
        datePicker.setValue(LocalDate.now());
        emailField.setText(SessionContext.getCurrentUser().getEmail());
    }

    private void initializeComboBoxes() {
        categorieCombo.setItems(FXCollections.observableArrayList(
                "Technique", "Facturation", "Service", "Autre"
        ));
        // prioriteCombo.setItems(...);  ← À SUPPRIMER
    }

    public void setReclamation(Reclamation reclamation) {
        this.reclamation = reclamation;

        if (reclamation != null) {
            dialogTitle.setText("Modifier la réclamation #" + reclamation.getId());
            objetField.setText(reclamation.getObjet());
            categorieCombo.setValue(reclamation.getCategorie());
            // prioriteCombo.setValue(...);  ← À SUPPRIMER
            descriptionArea.setText(reclamation.getDescription());

            if (reclamation.getEmail() != null) {
                emailField.setText(reclamation.getEmail());
            }

            if (reclamation.getDateCreation() != null) {
                datePicker.setValue(reclamation.getDateCreation().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate());
            }
        } else {
            dialogTitle.setText("Ajouter une nouvelle réclamation");
            // prioriteCombo.setValue("Moyenne");  ← À SUPPRIMER
            categorieCombo.setValue("Technique");
        }
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) return;

        try {
            if (reclamation == null) {
                // AJOUT
                Reclamation newReclamation = new Reclamation();
                newReclamation.setObjet(objetField.getText().trim());
                newReclamation.setCategorie(categorieCombo.getValue());
                newReclamation.setDescription(descriptionArea.getText().trim());
                newReclamation.setStatut("Nouvelle");
                newReclamation.setDateCreation(Date.from(datePicker.getValue()
                        .atStartOfDay(ZoneId.systemDefault()).toInstant()));

                if (emailField.getText() != null && !emailField.getText().trim().isEmpty()) {
                    newReclamation.setEmail(emailField.getText().trim());
                }

                // La priorité sera calculée automatiquement par PrioriteService
                // Pas besoin de la setter ici

                String nouvellePriorite = prioriteService.calculerPriorite(newReclamation);
                newReclamation.setPriorite(nouvellePriorite);

                newReclamation.setUtilisateurId(SessionContext.getCurrentUser().getId());

                reclamationService.ajouter(newReclamation);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Réclamation ajoutée avec succès !");
                saved = true;

                // RESTER SUR LE FORMULAIRE
                clearForm();

            } else {
                // MODIFICATION
                reclamation.setObjet(objetField.getText().trim());
                reclamation.setCategorie(categorieCombo.getValue());
                reclamation.setDescription(descriptionArea.getText().trim());

                if (datePicker.getValue() != null) {
                    reclamation.setDateCreation(Date.from(datePicker.getValue()
                            .atStartOfDay(ZoneId.systemDefault()).toInstant()));
                }

                if (emailField.getText() != null && !emailField.getText().trim().isEmpty()) {
                    reclamation.setEmail(emailField.getText().trim());
                } else {
                    reclamation.setEmail(null);
                }

                // La priorité sera recalculée automatiquement par le système
                // Pas besoin de la modifier ici

                reclamationService.update(reclamation);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Réclamation modifiée avec succès !");
                saved = true;

                // RETOUR À LA LISTE
                goBackToList();
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de sauvegarde: " + e.getMessage());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Format invalide");
        }
    }

    @FXML
    private void handleCancel() {
        goBackToList();
    }

    @FXML
    private void goBackToList() {
        try {
            Parent listPage = FXMLLoader.load(getClass().getResource("/reclamationList.fxml"));
            objetField.getScene().setRoot(listPage);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retourner à la liste");
        }
    }

    private void clearForm() {
        objetField.clear();
        categorieCombo.setValue("Technique");
        descriptionArea.clear();
        emailField.clear();
        datePicker.setValue(LocalDate.now());
        reclamation = null;
        dialogTitle.setText("Ajouter une nouvelle réclamation");
    }



    private boolean validateInput() {
        if (objetField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "L'objet est requis.");
            return false;
        }
        if (descriptionArea.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "La description est requise.");
            return false;
        }
        if (categorieCombo.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "La catégorie est requise.");
            return false;
        }

        if (emailField.getText() != null && !emailField.getText().trim().isEmpty()) {
            String email = emailField.getText().trim();
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                showAlert(Alert.AlertType.WARNING, "Validation", "L'email n'est pas valide.");
                return false;
            }
        }

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