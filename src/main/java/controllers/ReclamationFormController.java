package controllers;

import entities.Reclamation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.ReclamationService;

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
    @FXML private ComboBox<String> prioriteCombo;
    @FXML private TextArea descriptionArea;
    @FXML private TextField utilisateurIdField;
    @FXML private DatePicker datePicker;
    @FXML private TextField emailField;

    private Reclamation reclamation;
    private boolean saved = false;
    private ReclamationService reclamationService = new ReclamationService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeComboBoxes();
        datePicker.setValue(LocalDate.now());
    }

    private void initializeComboBoxes() {
        categorieCombo.setItems(FXCollections.observableArrayList(
                "Technique", "Facturation", "Service", "Autre"
        ));
        prioriteCombo.setItems(FXCollections.observableArrayList(
                "Haute", "Moyenne", "Basse"
        ));
    }

    public void setReclamation(Reclamation reclamation) {
        this.reclamation = reclamation;

        if (reclamation != null) {
            dialogTitle.setText("Modifier la réclamation #" + reclamation.getId());
            objetField.setText(reclamation.getObjet());
            categorieCombo.setValue(reclamation.getCategorie());
            prioriteCombo.setValue(reclamation.getPriorite());
            descriptionArea.setText(reclamation.getDescription());

            if (reclamation.getUtilisateurId() != null) {
                utilisateurIdField.setText(String.valueOf(reclamation.getUtilisateurId()));
            }

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
            prioriteCombo.setValue("Moyenne");
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
                Reclamation newReclamation = new Reclamation();
                newReclamation.setObjet(objetField.getText().trim());
                newReclamation.setCategorie(categorieCombo.getValue());
                newReclamation.setPriorite(prioriteCombo.getValue());
                newReclamation.setDescription(descriptionArea.getText().trim());
                newReclamation.setStatut("Nouvelle");
                newReclamation.setDateCreation(Date.from(datePicker.getValue()
                        .atStartOfDay(ZoneId.systemDefault()).toInstant()));

                if (!utilisateurIdField.getText().trim().isEmpty()) {
                    newReclamation.setUtilisateurId(Integer.parseInt(utilisateurIdField.getText().trim()));
                }

                if (emailField.getText() != null && !emailField.getText().trim().isEmpty()) {
                    newReclamation.setEmail(emailField.getText().trim());
                }

                reclamationService.ajouter(newReclamation);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Réclamation ajoutée avec succès !");
                saved = true;
                goBackToList(); // ← Retour à la liste après ajout

            } else {
                reclamation.setObjet(objetField.getText().trim());
                reclamation.setCategorie(categorieCombo.getValue());
                reclamation.setPriorite(prioriteCombo.getValue());
                reclamation.setDescription(descriptionArea.getText().trim());

                if (datePicker.getValue() != null) {
                    reclamation.setDateCreation(Date.from(datePicker.getValue()
                            .atStartOfDay(ZoneId.systemDefault()).toInstant()));
                }

                if (!utilisateurIdField.getText().trim().isEmpty()) {
                    reclamation.setUtilisateurId(Integer.parseInt(utilisateurIdField.getText().trim()));
                } else {
                    reclamation.setUtilisateurId(null);
                }

                if (emailField.getText() != null && !emailField.getText().trim().isEmpty()) {
                    reclamation.setEmail(emailField.getText().trim());
                } else {
                    reclamation.setEmail(null);
                }

                reclamationService.update(reclamation);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Réclamation modifiée avec succès !");
                saved = true;
                goBackToList(); // ← Retour à la liste après modification
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de sauvegarde: " + e.getMessage());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "L'ID utilisateur doit être un nombre valide.");
        }
    }

    @FXML
    private void handleCancel() {
        goBackToList(); // ← Annuler retourne aussi à la liste
    }

    // ✅ MÉTHODE AJOUTÉE - Retour à la liste des réclamations
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
        if (prioriteCombo.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "La priorité est requise.");
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