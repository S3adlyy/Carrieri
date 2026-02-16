package main;

import entities.Postulation;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.PostulationService;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class PostulerPopupController {

    @FXML private Label lblOfferTitle;
    @FXML private TextArea txtMotivation;
    @FXML private Label lblCharCount;

    private int offreId;
    private int candidatId = 1; // TODO: replace with real connected user id

    private final PostulationService service = new PostulationService();

    @FXML
    public void initialize() {
        // Live char count + hard limit
        txtMotivation.textProperty().addListener((obs, old, newVal) -> {
            String v = (newVal == null) ? "" : newVal;
            if (v.length() > 1500) {
                txtMotivation.setText(v.substring(0, 1500));
                txtMotivation.positionCaret(1500);
                v = txtMotivation.getText();
            }
            if (lblCharCount != null) lblCharCount.setText(v.length() + "/1500");
        });

        if (lblCharCount != null) lblCharCount.setText("0/1500");
    }

    // Called from OffresListController when opening the popup
    public void setOffreInfo(int id, String titre) {
        this.offreId = id;
        if (lblOfferTitle != null) {
            lblOfferTitle.setText("Offre: " + (titre == null ? "" : titre));
        }
    }

    @FXML
    private void handleSubmit() {
        // Ensure offerId is set
        if (offreId <= 0) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Offre invalide. Veuillez réouvrir la fenêtre.");
            return;
        }

        String motivation = (txtMotivation.getText() == null) ? "" : txtMotivation.getText().trim();
        if (motivation.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "La lettre de motivation est requise.");
            txtMotivation.requestFocus();
            return;
        }

        String statut = "En attente"; // default status

        Postulation postulation = new Postulation(
                candidatId,
                offreId,
                LocalDateTime.now(),
                statut,
                motivation
        );

        try {
            service.postuler(postulation);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Votre candidature a été envoyée !");
            handleCancel(); // close popup
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur base de données : " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        txtMotivation.getScene().getWindow().hide();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
