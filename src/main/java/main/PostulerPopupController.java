package main;

import entities.OffreEmploi;
import entities.Postulation;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.OffreEmploiService;
import services.PostulationService;
import services.SimpleSMSService;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class PostulerPopupController {

    @FXML private Label lblOfferTitle;
    @FXML private TextField txtPhone;
    @FXML private TextArea txtMotivation;
    @FXML private Label lblCharCount;
    @FXML private Label lblPhoneError;
    @FXML private Label lblMotivationError;

    private int offreId;
    private String offreTitre; // Pour le SMS
    private int candidatId = 1; // TODO: replace with real connected user id
    private String candidatNom = "Candidat"; // TODO: replace with real connected user name

    private final PostulationService service = new PostulationService();
    private final OffreEmploiService offreService = new OffreEmploiService();
    private final SimpleSMSService smsService = SimpleSMSService.getInstance();

    private static final int MIN_WORDS = 5;
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

        // Validation téléphone en temps réel
        if (txtPhone != null) {
            txtPhone.textProperty().addListener((obs, old, newVal) -> {
                validatePhone(false);
            });
        }

        if (lblCharCount != null) lblCharCount.setText("0/" + MAX_CHARS + " | 0 mots");
    }

    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        String[] words = text.trim().split("\\s+");
        return words.length;
    }

    private boolean validatePhone(boolean showError) {
        String phone = (txtPhone.getText() == null) ? "" : txtPhone.getText().trim();

        // Le téléphone est optionnel
        if (phone.isEmpty()) {
            if (lblPhoneError != null) {
                lblPhoneError.setText("");
                lblPhoneError.setManaged(false);
                lblPhoneError.setVisible(false);
                txtPhone.getStyleClass().remove("error");
            }
            return true;
        }

        // Validation basique du format
        String phoneClean = phone.replaceAll("[\\s\\-\\.]", "");
        if (!phoneClean.matches("^\\+?[0-9]{10,15}$")) {
            if (showError && lblPhoneError != null) {
                lblPhoneError.setText("⚠ Format invalide. Utilisez le format international (ex: +33612345678)");
                lblPhoneError.setManaged(true);
                lblPhoneError.setVisible(true);
                txtPhone.getStyleClass().add("error");
            }
            return false;
        }

        // Validation réussie
        if (lblPhoneError != null) {
            lblPhoneError.setText("");
            lblPhoneError.setManaged(false);
            lblPhoneError.setVisible(false);
            txtPhone.getStyleClass().remove("error");
        }
        return true;
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
        this.offreTitre = titre;
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

        // Valider les champs avec affichage d'erreur
        boolean motivationValid = validateMotivation(true);
        boolean phoneValid = validatePhone(true);

        if (!motivationValid) {
            txtMotivation.requestFocus();
            return;
        }

        if (!phoneValid) {
            txtPhone.requestFocus();
            return;
        }

        String motivation = txtMotivation.getText().trim();
        String phone = (txtPhone.getText() == null) ? "" : txtPhone.getText().trim();
        String statut = "En attente"; // default status

        Postulation postulation = new Postulation(
                candidatId,
                offreId,
                LocalDateTime.now(),
                statut,
                motivation
        );

        try {
            // Sauvegarder la postulation
            service.postuler(postulation);

            // Récupérer le titre de l'offre si non disponible
            if (offreTitre == null || offreTitre.isEmpty()) {
                try {
                    OffreEmploi offre = offreService.findById(offreId);
                    if (offre != null) {
                        offreTitre = offre.getTitre();
                    }
                } catch (SQLException e) {
                    System.err.println("Erreur lors de la récupération du titre de l'offre: " + e.getMessage());
                    offreTitre = "l'offre sélectionnée";
                }
            }

            // Envoyer SMS de confirmation si numéro fourni
            if (!phone.isEmpty() && smsService.isEnabled()) {
                String phoneClean = phone.replaceAll("[\\s\\-\\.]", "");
                if (!phoneClean.startsWith("+")) {
                    // Ajouter +33 par défaut si pas de code pays
                    if (phoneClean.startsWith("0")) {
                        phoneClean = "+33" + phoneClean.substring(1);
                    } else {
                        phoneClean = "+" + phoneClean;
                    }
                }

                System.out.println("🔍 DEBUG - Tentative d'envoi SMS:");
                System.out.println("   Numéro brut: " + phone);
                System.out.println("   Numéro nettoyé: " + phoneClean);
                System.out.println("   Service SMS activé: " + smsService.isEnabled());

                boolean smsSent = smsService.envoyerSMS(
                    phoneClean,
                    candidatNom,
                    offreTitre
                );

                System.out.println("   Résultat envoi: " + (smsSent ? "SUCCÈS ✓" : "ÉCHEC ❌"));

                if (smsSent) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Votre candidature a été envoyée !\n\n" +
                        "✓ Un SMS de confirmation a été envoyé au " + phoneClean);
                } else {
                    showAlert(Alert.AlertType.WARNING, "Attention",
                        "Votre candidature a été envoyée !\n\n" +
                        "⚠ Le SMS de confirmation n'a pas pu être envoyé.\n\n" +
                        "Raisons possibles:\n" +
                        "- Numéro non vérifié dans Twilio (mode Trial)\n" +
                        "- Problème de connexion réseau\n" +
                        "- Crédit Twilio épuisé\n\n" +
                        "Consultez la console pour plus de détails.");
                }
            } else {
                String message = "Votre candidature a été envoyée !";
                if (phone.isEmpty()) {
                    message += "\n\nℹ Aucun numéro fourni, pas de SMS envoyé.";
                } else if (!smsService.isEnabled()) {
                    message += "\n\nℹ Service SMS non configuré.";
                }
                showAlert(Alert.AlertType.INFORMATION, "Succès", message);
            }

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
