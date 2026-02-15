package controllers;

import entities.TraitementReclamation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.TraitementReclamationService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.ResourceBundle;

public class TraitementFormController implements Initializable {

    @FXML private Label dialogTitle;
    @FXML private TextField reclamationIdField;
    @FXML private TextField adminIdField;
    @FXML private ComboBox<String> statutFinalCombo;
    @FXML private DatePicker datePicker;
    @FXML private TextArea reponseArea;

    private TraitementReclamation traitement;
    private ObservableList<TraitementReclamation> traitementList;
    private TraitementReclamationService traitementService = new TraitementReclamationService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser la combo box des statuts
        statutFinalCombo.setItems(FXCollections.observableArrayList(
                "Résolue", "En cours", "Fermée", "Rejetée"
        ));

        // Date par défaut = aujourd'hui
        datePicker.setValue(LocalDate.now());
    }

    public void setTraitement(TraitementReclamation traitement) {
        this.traitement = traitement;

        if (traitement != null) {
            dialogTitle.setText("Modifier le traitement #" + traitement.getId());

            if (traitement.getReclamationId() != null) {
                reclamationIdField.setText(String.valueOf(traitement.getReclamationId()));
            }

            if (traitement.getAdminId() != null) {
                adminIdField.setText(String.valueOf(traitement.getAdminId()));
            }

            statutFinalCombo.setValue(traitement.getStatutFinal());
            reponseArea.setText(traitement.getReponseAdmin());

            if (traitement.getDateTraitement() != null) {
                datePicker.setValue(traitement.getDateTraitement().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate());
            }
        } else {
            dialogTitle.setText("Nouveau traitement");
            statutFinalCombo.setValue("En cours");
        }
    }

    public void setTraitementList(ObservableList<TraitementReclamation> traitementList) {
        this.traitementList = traitementList;
    }

    @FXML
    private void handleSave() {
        System.out.println("🔵 handleSave() est appelé !");

        if (!validateInput()) {
            return;
        }

        try {
            if (traitement == null) {
                // Ajouter nouveau traitement
                TraitementReclamation newTraitement = new TraitementReclamation();

                // Gestion de la date
                if (datePicker != null && datePicker.getValue() != null) {
                    newTraitement.setDateTraitement(Date.from(datePicker.getValue()
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()));
                } else {
                    newTraitement.setDateTraitement(new Date());
                }

                newTraitement.setReponseAdmin(reponseArea.getText().trim());
                newTraitement.setStatutFinal(statutFinalCombo.getValue());

                if (!reclamationIdField.getText().trim().isEmpty()) {
                    newTraitement.setReclamationId(Integer.parseInt(reclamationIdField.getText().trim()));
                }

                if (!adminIdField.getText().trim().isEmpty()) {
                    newTraitement.setAdminId(Integer.parseInt(adminIdField.getText().trim()));
                }

                traitementService.ajouter(newTraitement);

                // Si c'est un traitement, mettre à jour le statut de la réclamation
                if (newTraitement.getReclamationId() != null && newTraitement.getAdminId() != null) {
                    traitementService.traiterReclamation(
                            newTraitement.getReclamationId(),
                            newTraitement.getReponseAdmin(),
                            newTraitement.getStatutFinal(),
                            newTraitement.getAdminId()
                    );
                }

                showAlert(Alert.AlertType.INFORMATION, "Succès", "Traitement ajouté",
                        "Le traitement a été ajouté avec succès.");

                // Fermer le dialogue
                closeDialog();

            } else {
                // Modifier traitement existant
                if (datePicker != null && datePicker.getValue() != null) {
                    traitement.setDateTraitement(Date.from(datePicker.getValue()
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()));
                }

                traitement.setReponseAdmin(reponseArea.getText().trim());
                traitement.setStatutFinal(statutFinalCombo.getValue());

                if (!reclamationIdField.getText().trim().isEmpty()) {
                    traitement.setReclamationId(Integer.parseInt(reclamationIdField.getText().trim()));
                } else {
                    traitement.setReclamationId(null);
                }

                if (!adminIdField.getText().trim().isEmpty()) {
                    traitement.setAdminId(Integer.parseInt(adminIdField.getText().trim()));
                } else {
                    traitement.setAdminId(null);
                }

                traitementService.update(traitement);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Traitement modifié",
                        "Le traitement a été modifié avec succès.");

                // Fermer le dialogue
                closeDialog();
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de sauvegarde",
                    "Détails: " + e.getMessage());
            e.printStackTrace();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Format invalide",
                    "Les IDs doivent être des nombres valides.");
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private boolean validateInput() {
        if (reponseArea.getText() == null || reponseArea.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Champ requis",
                    "La réponse admin est requise.");
            reponseArea.requestFocus();
            return false;
        }

        if (statutFinalCombo.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Champ requis",
                    "Veuillez sélectionner un statut final.");
            statutFinalCombo.requestFocus();
            return false;
        }

        if (datePicker.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Champ requis",
                    "Veuillez sélectionner une date.");
            datePicker.requestFocus();
            return false;
        }

        // Validation des IDs si présents
        if (!reclamationIdField.getText().trim().isEmpty()) {
            try {
                int id = Integer.parseInt(reclamationIdField.getText().trim());
                if (id <= 0) {
                    showAlert(Alert.AlertType.WARNING, "Validation", "ID invalide",
                            "L'ID de réclamation doit être positif.");
                    reclamationIdField.requestFocus();
                    return false;
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Format invalide",
                        "L'ID de réclamation doit être un nombre.");
                reclamationIdField.requestFocus();
                return false;
            }
        }

        if (!adminIdField.getText().trim().isEmpty()) {
            try {
                int id = Integer.parseInt(adminIdField.getText().trim());
                if (id <= 0) {
                    showAlert(Alert.AlertType.WARNING, "Validation", "ID invalide",
                            "L'ID admin doit être positif.");
                    adminIdField.requestFocus();
                    return false;
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Format invalide",
                        "L'ID admin doit être un nombre.");
                adminIdField.requestFocus();
                return false;
            }
        }

        return true;
    }

    // CORRECTION ICI - Nouvelle méthode closeDialog()
    private void closeDialog() {
        try {
            Stage stage = (Stage) reponseArea.getScene().getWindow();
            stage.close();
            System.out.println("✅ Dialogue fermé");
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