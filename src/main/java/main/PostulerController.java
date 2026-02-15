package main;

import entities.Postulation;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.PostulationService;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class PostulerController {

    @FXML private TextField txtCandidatId;
    @FXML private TextField txtOffreId;
    @FXML private ComboBox<String> comboStatut;
    @FXML private TextArea txtMotivation;
    @FXML private Label lblMotivCharCount;
    @FXML private Label lblOffreInfo;

    private final PostulationService service = new PostulationService();

    @FXML
    public void initialize() {
        // Default status
        comboStatut.getSelectionModel().selectFirst();

        // Live char counter for motivation
        txtMotivation.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = (newVal == null) ? 0 : newVal.length();
            lblMotivCharCount.setText(len + "/1500");
        });
    }

    /**
     * Called by OffresShellController to pre-fill offre info
     * when user clicks "Postuler" from the list view.
     */
    public void setOffreInfo(int offreId, String offreTitre) {
        txtOffreId.setText(String.valueOf(offreId));
        txtOffreId.setEditable(false);
        lblOffreInfo.setText("Offre: " + offreTitre + " (ID: " + offreId + ")");
    }

    @FXML
    public void handleSubmit() {
        // Validation
        if (txtCandidatId.getText() == null || txtCandidatId.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Champ requis", "L'identifiant candidat est obligatoire.");
            return;
        }
        if (txtOffreId.getText() == null || txtOffreId.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Champ requis", "L'ID de l'offre est obligatoire.");
            return;
        }
        if (txtMotivation.getText() == null || txtMotivation.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Champ requis", "La lettre de motivation est obligatoire.");
            return;
        }

        int candidatId, offreId;
        try {
            candidatId = Integer.parseInt(txtCandidatId.getText().trim());
            offreId = Integer.parseInt(txtOffreId.getText().trim());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Les identifiants doivent etre des nombres.");
            return;
        }

        String statut = comboStatut.getValue() != null ? comboStatut.getValue() : "En attente";

        Postulation postulation = new Postulation(
                candidatId,
                offreId,
                LocalDateTime.now(),
                statut,
                txtMotivation.getText().trim()
        );

        try {
            service.postuler(postulation);
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Votre candidature a ete envoyee avec succes !");
            handleCancel();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur base de donnees", e.getMessage());
        }
    }

    @FXML
    public void handleCancel() {
        // Go back to list
        OffresShellController.getInstance().showOffresList();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
