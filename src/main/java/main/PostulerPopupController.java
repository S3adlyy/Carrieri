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
    @FXML private Label lblMotivationError;

    private int offreId;
    private int candidatId = 1; // TODO: replace with real connected user id

    private final PostulationService service = new PostulationService();
    private static final int MIN_WORDS = 25;
    private static final int MAX_CHARS = 1500;

    @FXML
    public void initialize() {
        // Live char count + word count + hard limit
        txtMotivation.textProperty().addListener((obs, old, newVal) -> {
            String v = (newVal == null) ? "" : newVal;
            if (v.length() > MAX_CHARS) {
                txtMotivation.setText(v.substring(0, MAX_CHARS));
                txtMotivation.positionCaret(MAX_CHARS);
                v = txtMotivation.getText();
            }

            int wordCount = countWords(v);
            if (lblCharCount != null) {
                lblCharCount.setText(v.length() + "/" + MAX_CHARS + " | " + wordCount + " mots");
            }

            // Validation en temps réel
            validateMotivation(false);
        });

        if (lblCharCount != null) lblCharCount.setText("0/" + MAX_CHARS + " | 0 mots");
    }

    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        String[] words = text.trim().split("\\s+");
        return words.length;
    }

    private boolean validateMotivation(boolean showError) {
        String motivation = (txtMotivation.getText() == null) ? "" : txtMotivation.getText().trim();

        if (motivation.isEmpty()) {
            if (showError && lblMotivationError != null) {
                lblMotivationError.setText("⚠ La lettre de motivation est obligatoire");
                lblMotivationError.setManaged(true);
                lblMotivationError.setVisible(true);
                txtMotivation.getStyleClass().add("error");
            }
            return false;
        }

        int wordCount = countWords(motivation);
        if (wordCount < MIN_WORDS) {
            if (showError && lblMotivationError != null) {
                lblMotivationError.setText("⚠ La lettre de motivation doit contenir au moins " + MIN_WORDS + " mots (actuellement : " + wordCount + " mots)");
                lblMotivationError.setManaged(true);
                lblMotivationError.setVisible(true);
                txtMotivation.getStyleClass().add("error");
            }
            return false;
        }

        // Validation réussie
        if (lblMotivationError != null) {
            lblMotivationError.setText("");
            lblMotivationError.setManaged(false);
            lblMotivationError.setVisible(false);
            txtMotivation.getStyleClass().remove("error");
        }
        return true;
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

        // Valider le champ avec affichage d'erreur
        if (!validateMotivation(true)) {
            txtMotivation.requestFocus();
            return;
        }

        String motivation = txtMotivation.getText().trim();
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
