package controllers;

import entities.Reclamation;
import entities.TraitementReclamation;
import entities.User; // Vous devez créer cette entité
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import services.ReclamationService;
import services.TraitementReclamationService;
import services.UserService; // Service pour récupérer l'utilisateur
import services.EmailService;

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
    @FXML private Label reclamationObjetLabel;
    @FXML private Label reclamationDescriptionLabel;

    private TraitementReclamation traitement;
    private Reclamation reclamation;
    private String mode = "AJOUT";
    private ObservableList<TraitementReclamation> traitementList;
    private TraitementReclamationService traitementService = new TraitementReclamationService();
    private ReclamationService reclamationService = new ReclamationService();
    private UserService userService = new UserService(); // À créer

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statutFinalCombo.setItems(FXCollections.observableArrayList(
                "Résolue", "En cours", "Fermée", "Rejetée"
        ));
        datePicker.setValue(LocalDate.now());
    }

    public void setReclamation(Reclamation reclamation) {
        this.reclamation = reclamation;
        this.mode = "TRAITEMENT";

        if (reclamation != null) {
            dialogTitle.setText("Traiter la réclamation #" + reclamation.getId());
            reclamationIdField.setText(String.valueOf(reclamation.getId()));
            reclamationIdField.setEditable(false);

            if (reclamationObjetLabel != null) {
                reclamationObjetLabel.setText("Objet: " + reclamation.getObjet());
            }
            if (reclamationDescriptionLabel != null) {
                reclamationDescriptionLabel.setText("Description: " + reclamation.getDescription());
            }

            statutFinalCombo.setValue("En cours");
            adminIdField.setText("1");
        }
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setTraitement(TraitementReclamation traitement) {
        this.traitement = traitement;
        this.mode = "MODIFICATION";

        if (traitement != null) {
            dialogTitle.setText("Modifier le traitement #" + traitement.getId());

            if (traitement.getReclamationId() != null) {
                reclamationIdField.setText(String.valueOf(traitement.getReclamationId()));
                try {
                    Reclamation rec = reclamationService.getById(traitement.getReclamationId());
                    if (rec != null) {
                        if (reclamationObjetLabel != null) {
                            reclamationObjetLabel.setText("Objet: " + rec.getObjet());
                        }
                        if (reclamationDescriptionLabel != null) {
                            reclamationDescriptionLabel.setText("Description: " + rec.getDescription());
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
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
        }
    }

    public void setTraitementList(ObservableList<TraitementReclamation> traitementList) {
        this.traitementList = traitementList;
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) return;

        try {
            int recId;
            String ancienStatut = "";
            String nouveauStatut = statutFinalCombo.getValue();

            if (mode.equals("TRAITEMENT") && reclamation != null) {
                recId = reclamation.getId();
                ancienStatut = reclamation.getStatut();
            } else {
                recId = Integer.parseInt(reclamationIdField.getText().trim());
                // Récupérer l'ancien statut
                Reclamation temp = reclamationService.getById(recId);
                if (temp != null) {
                    ancienStatut = temp.getStatut();
                }
            }

            if (traitement == null) {
                // Ajout d'un nouveau traitement
                TraitementReclamation newTraitement = new TraitementReclamation();
                newTraitement.setDateTraitement(Date.from(datePicker.getValue()
                        .atStartOfDay(ZoneId.systemDefault()).toInstant()));
                newTraitement.setReponseAdmin(reponseArea.getText().trim());
                newTraitement.setStatutFinal(nouveauStatut);
                newTraitement.setReclamationId(recId);

                if (!adminIdField.getText().trim().isEmpty()) {
                    newTraitement.setAdminId(Integer.parseInt(adminIdField.getText().trim()));
                }

                traitementService.ajouter(newTraitement);

                // ✅ Mise à jour du statut de la réclamation
                Reclamation rec = (reclamation != null) ? reclamation :
                        reclamationService.getById(recId);
                if (rec != null) {
                    rec.setStatut(nouveauStatut);
                    reclamationService.update(rec);

                    // ✅ Envoi d'email si le statut est "Résolue" ou "Rejetée"
                    if (nouveauStatut.equals("Résolue") || nouveauStatut.equals("Rejetée")) {
                        envoyerEmailNotification(rec, nouveauStatut, reponseArea.getText().trim());
                    }
                }

                showAlert(Alert.AlertType.INFORMATION, "Succès", "Traitement ajouté avec succès !");
                goToTraitementList();

            } else {
                // Modification d'un traitement existant
                if (datePicker.getValue() != null) {
                    traitement.setDateTraitement(Date.from(datePicker.getValue()
                            .atStartOfDay(ZoneId.systemDefault()).toInstant()));
                }
                traitement.setReponseAdmin(reponseArea.getText().trim());
                traitement.setStatutFinal(nouveauStatut);

                if (!reclamationIdField.getText().trim().isEmpty()) {
                    traitement.setReclamationId(Integer.parseInt(reclamationIdField.getText().trim()));
                }

                if (!adminIdField.getText().trim().isEmpty()) {
                    traitement.setAdminId(Integer.parseInt(adminIdField.getText().trim()));
                }

                traitementService.update(traitement);

                // ✅ Mise à jour du statut de la réclamation
                Reclamation rec = reclamationService.getById(recId);
                if (rec != null) {
                    rec.setStatut(nouveauStatut);
                    reclamationService.update(rec);

                    // ✅ Envoi d'email si le statut change vers "Résolue" ou "Rejetée"
                    if ((nouveauStatut.equals("Résolue") || nouveauStatut.equals("Rejetée"))
                            && !nouveauStatut.equals(ancienStatut)) {
                        envoyerEmailNotification(rec, nouveauStatut, reponseArea.getText().trim());
                    }
                }

                showAlert(Alert.AlertType.INFORMATION, "Succès", "Traitement modifié avec succès !");
                goToTraitementList();
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de sauvegarde: " + e.getMessage());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Les IDs doivent être des nombres valides.");
        }
    }

    // ✅ Nouvelle méthode pour envoyer l'email
    private void envoyerEmailNotification(Reclamation reclamation, String nouveauStatut, String reponse) {
        try {
            // Vérifier si un email est fourni dans la réclamation
            if (reclamation.getEmail() != null && !reclamation.getEmail().isEmpty()) {
                // Récupérer le nom de l'utilisateur (si disponible)
                String nom = "Utilisateur";
                if (reclamation.getUtilisateurId() != null) {
                    try {
                        User user = userService.getById(reclamation.getUtilisateurId());
                        if (user != null) {
                            nom = user.getFirstName() + " " + user.getLastName();
                        }
                    } catch (Exception e) {
                        // Ignorer, on garde "Utilisateur"
                    }
                }

                // Envoyer l'email à l'adresse fournie
                EmailService.envoyerEmailUtilisateur(
                        reclamation.getEmail(),
                        nom,
                        reclamation.getObjet(),
                        nouveauStatut,
                        reponse
                );
            } else {
                System.out.println("⚠️ Aucun email fourni pour la réclamation #" + reclamation.getId());
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email: " + e.getMessage());
            // Ne pas bloquer le processus
        }
    }

    private void goToTraitementList() {
        try {
            MainController mainController = MainController.getInstance();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/traitementList.fxml"));
            Parent listPage = loader.load();

            if (mainController != null) {
                mainController.setContent(listPage);
            } else {
                reponseArea.getScene().setRoot(listPage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goBackToList() {
        try {
            Parent listPage = FXMLLoader.load(getClass().getResource("/traitementList.fxml"));
            reponseArea.getScene().setRoot(listPage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean validateInput() {
        if (reponseArea.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "La réponse admin est requise.");
            reponseArea.requestFocus();
            return false;
        }
        if (statutFinalCombo.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Le statut final est requis.");
            statutFinalCombo.requestFocus();
            return false;
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