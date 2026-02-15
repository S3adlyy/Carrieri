package controllers;

import entities.Reclamation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
    @FXML private ComboBox<String> statutCombo;
    @FXML private TextField utilisateurIdField;
    @FXML private DatePicker datePicker;

    private Reclamation reclamation;
    private ObservableList<Reclamation> reclamationList;
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

        statutCombo.setItems(FXCollections.observableArrayList(
                "Nouvelle", "En cours", "Résolue", "Fermée"
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
            statutCombo.setValue(reclamation.getStatut());

            if (reclamation.getUtilisateurId() != null) {
                utilisateurIdField.setText(String.valueOf(reclamation.getUtilisateurId()));
            }

            if (reclamation.getDateCreation() != null) {
                // Solution 1: Convertir java.util.Date en LocalDate
                datePicker.setValue(new java.sql.Date(reclamation.getDateCreation().getTime())
                        .toLocalDate());

                // OU Solution 2 (plus simple) :
                // datePicker.setValue(LocalDate.parse(new SimpleDateFormat("yyyy-MM-dd").format(reclamation.getDateCreation())));
            }
        } else {
            dialogTitle.setText("Ajouter une nouvelle réclamation");
            statutCombo.setValue("Nouvelle");
            prioriteCombo.setValue("Moyenne");
            categorieCombo.setValue("Technique");
        }
    }

    public void setReclamationList(ObservableList<Reclamation> reclamationList) {
        this.reclamationList = reclamationList;
    }

    @FXML
    private void handleSave() {
        System.out.println("🔵 handleSave() est appelé !");
        System.out.println("Objet: " + objetField.getText());
        System.out.println("Catégorie: " + categorieCombo.getValue());

        if (!validateInput()) {
            System.out.println("❌ Validation échouée");
            return;
        }

        System.out.println("✅ Validation réussie");

        try {
            if (reclamation == null) {
                // Ajouter nouvelle réclamation
                Reclamation newReclamation = new Reclamation();
                newReclamation.setObjet(objetField.getText().trim());
                newReclamation.setCategorie(categorieCombo.getValue());
                newReclamation.setPriorite(prioriteCombo.getValue());
                newReclamation.setDescription(descriptionArea.getText().trim());
                newReclamation.setStatut(statutCombo.getValue());

                // Gestion de la date
                if (datePicker != null && datePicker.getValue() != null) {
                    newReclamation.setDateCreation(Date.from(datePicker.getValue()
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()));
                } else {
                    newReclamation.setDateCreation(new Date());
                }

                // Gestion de l'utilisateur ID
                if (!utilisateurIdField.getText().trim().isEmpty()) {
                    newReclamation.setUtilisateurId(Integer.parseInt(utilisateurIdField.getText().trim()));
                }

                System.out.println("📤 Envoi à la base de données (AJOUT)...");
                reclamationService.ajouter(newReclamation);
                System.out.println("✅ Ajout réussi en base !");

                showAlert(Alert.AlertType.INFORMATION, "Succès", "Réclamation ajoutée",
                        "La réclamation a été ajoutée avec succès.");

                closeDialog();

            } else {
                // MODIFICATION réclamation existante (code ajouté)
                System.out.println("🔄 Modification de la réclamation #" + reclamation.getId());

                reclamation.setObjet(objetField.getText().trim());
                reclamation.setCategorie(categorieCombo.getValue());
                reclamation.setPriorite(prioriteCombo.getValue());
                reclamation.setDescription(descriptionArea.getText().trim());
                reclamation.setStatut(statutCombo.getValue());

                // Gestion de la date
                if (datePicker != null && datePicker.getValue() != null) {
                    reclamation.setDateCreation(Date.from(datePicker.getValue()
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()));
                }

                // Gestion de l'utilisateur ID
                if (!utilisateurIdField.getText().trim().isEmpty()) {
                    reclamation.setUtilisateurId(Integer.parseInt(utilisateurIdField.getText().trim()));
                } else {
                    reclamation.setUtilisateurId(null);
                }

                System.out.println("📤 Envoi à la base de données (MODIFICATION)...");
                reclamationService.update(reclamation);
                System.out.println("✅ Modification réussie en base !");

                showAlert(Alert.AlertType.INFORMATION, "Succès", "Réclamation modifiée",
                        "La réclamation a été modifiée avec succès.");

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
                    "L'ID utilisateur doit être un nombre valide.");
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private boolean validateInput() {
        if (objetField.getText() == null || objetField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Champ requis",
                    "Le champ 'Objet' est requis.");
            objetField.requestFocus();
            return false;
        }

        if (descriptionArea.getText() == null || descriptionArea.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Champ requis",
                    "Le champ 'Description' est requis.");
            descriptionArea.requestFocus();
            return false;
        }

        if (descriptionArea.getText().trim().length() < 10) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Description trop courte",
                    "La description doit contenir au moins 10 caractères.");
            descriptionArea.requestFocus();
            return false;
        }

        if (categorieCombo.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Champ requis",
                    "Veuillez sélectionner une catégorie.");
            categorieCombo.requestFocus();
            return false;
        }

        if (prioriteCombo.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Champ requis",
                    "Veuillez sélectionner une priorité.");
            prioriteCombo.requestFocus();
            return false;
        }

        if (statutCombo.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Champ requis",
                    "Veuillez sélectionner un statut.");
            statutCombo.requestFocus();
            return false;
        }

        if (datePicker.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Champ requis",
                    "Veuillez sélectionner une date.");
            datePicker.requestFocus();
            return false;
        }

        // Validation de l'ID utilisateur si présent
        if (!utilisateurIdField.getText().trim().isEmpty()) {
            try {
                int id = Integer.parseInt(utilisateurIdField.getText().trim());
                if (id <= 0) {
                    showAlert(Alert.AlertType.WARNING, "Validation", "ID invalide",
                            "L'ID utilisateur doit être un nombre positif.");
                    utilisateurIdField.requestFocus();
                    return false;
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Format invalide",
                        "L'ID utilisateur doit être un nombre valide.");
                utilisateurIdField.requestFocus();
                return false;
            }
        }

        return true;
    }

    private void closeDialog() {
        Stage stage = (Stage) objetField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}