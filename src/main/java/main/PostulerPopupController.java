package main;

import entities.OffreEmploi;
import entities.Postulation;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import services.OffreEmploiService;
import services.PostulationService;
import services.SimpleSMSService;
import utils.StyledAlert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PostulerPopupController {

    @FXML private Label lblOfferTitle;
    @FXML private TextArea txtMotivation;
    @FXML private Label lblCharCount;
    @FXML private Label lblMotivationError;
    @FXML private Button btnSelectCV;
    @FXML private Label lblCVFileName;
    @FXML private Label lblCVError;
    @FXML private TextField txtPhone;
    @FXML private Label lblPhoneError;
    @FXML private Button btnBack;
    @FXML private Button btnCancel;
    @FXML private Button btnSubmit;

    private int offreId;
    private String offreTitre; // Pour le SMS
    private int candidatId = 1; // TODO: replace with real connected user id
    private String candidatNom = "Candidat"; // TODO: replace with real connected user name

    private File selectedCVFile = null; // Fichier CV sélectionné

    private final PostulationService service = new PostulationService();
    private final OffreEmploiService offreService = new OffreEmploiService();
    private final SimpleSMSService smsService = SimpleSMSService.getInstance();

    private static final int MIN_WORDS = 5;
    private static final int MAX_CHARS = 1500;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final String CV_STORAGE_DIR = "uploads/cv/"; // Dossier de stockage des CV

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

    private boolean validateCV(boolean showError) {
        if (selectedCVFile == null) {
            if (showError && lblCVError != null) {
                lblCVError.setText("⚠ Le CV est obligatoire. Veuillez sélectionner un fichier.");
                lblCVError.setManaged(true);
                lblCVError.setVisible(true);
            }
            return false;
        }

        // Validation réussie
        if (lblCVError != null) {
            lblCVError.setText("");
            lblCVError.setManaged(false);
            lblCVError.setVisible(false);
        }
        return true;
    }

    @FXML
    private void handleSelectCV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner votre CV");

        // Extensions acceptées
        FileChooser.ExtensionFilter pdfFilter = new FileChooser.ExtensionFilter("PDF Files", "*.pdf");
        FileChooser.ExtensionFilter docFilter = new FileChooser.ExtensionFilter("Word Files", "*.doc", "*.docx");
        FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter("All Supported Files", "*.pdf", "*.doc", "*.docx");
        fileChooser.getExtensionFilters().addAll(allFilter, pdfFilter, docFilter);
        fileChooser.setSelectedExtensionFilter(allFilter);

        // Ouvrir le dialogue de sélection
        File file = fileChooser.showOpenDialog(btnSelectCV.getScene().getWindow());

        if (file != null) {
            // Vérifier la taille du fichier
            if (file.length() > MAX_FILE_SIZE) {
                if (lblCVError != null) {
                    lblCVError.setText("⚠ Le fichier est trop volumineux. Taille maximale : 5 MB");
                    lblCVError.setManaged(true);
                    lblCVError.setVisible(true);
                }
                return;
            }

            // Vérifier l'extension
            String fileName = file.getName().toLowerCase();
            if (!fileName.endsWith(".pdf") && !fileName.endsWith(".doc") && !fileName.endsWith(".docx")) {
                if (lblCVError != null) {
                    lblCVError.setText("⚠ Format non supporté. Utilisez PDF, DOC ou DOCX");
                    lblCVError.setManaged(true);
                    lblCVError.setVisible(true);
                }
                return;
            }

            // Fichier valide
            selectedCVFile = file;
            if (lblCVFileName != null) {
                lblCVFileName.setText("✓ " + file.getName() + " (" + formatFileSize(file.length()) + ")");
                lblCVFileName.setStyle("-fx-text-fill: #10b981; -fx-font-size: 13; -fx-font-weight: 600;");
            }
            if (lblCVError != null) {
                lblCVError.setText("");
                lblCVError.setManaged(false);
                lblCVError.setVisible(false);
            }
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
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
            StyledAlert.showError("Erreur", "Offre invalide. Veuillez réouvrir la fenêtre.");
            return;
        }

        // Valider les champs dans l'ordre avec affichage d'erreur
        boolean motivationValid = validateMotivation(true);
        boolean cvValid = validateCV(true);
        boolean phoneValid = validatePhone(true);

        // Si une validation échoue, focus sur le premier champ invalide
        if (!motivationValid) {
            txtMotivation.requestFocus();
            return;
        }

        if (!cvValid) {
            btnSelectCV.requestFocus();
            return;
        }

        if (!phoneValid) {
            txtPhone.requestFocus();
            return;
        }

        String motivation = txtMotivation.getText().trim();
        String phone = (txtPhone.getText() == null) ? "" : txtPhone.getText().trim();
        String statut = "En attente"; // default status

        // Copier le fichier CV dans le dossier uploads
        String cvPath = null;
        try {
            cvPath = saveCVFile(selectedCVFile);
            System.out.println("✅ CV sauvegardé: " + cvPath);
        } catch (IOException e) {
            StyledAlert.showError("Erreur", "Impossible de sauvegarder le CV:\n" + e.getMessage());
            return;
        }

        Postulation postulation = new Postulation(
                candidatId,
                offreId,
                LocalDateTime.now(),
                statut,
                motivation,
                cvPath
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
                    StyledAlert.showSuccess("Succès",
                            "Votre candidature a été envoyée !\n\n" +
                                    "✓ Un SMS de confirmation a été envoyé au " + phoneClean + "\n" +
                                    "✓ Votre CV a été joint : " + selectedCVFile.getName() + "\n" +
                                    "✓ CV sauvegardé : " + cvPath);
                } else {
                    StyledAlert.showWarning("Attention",
                            "Votre candidature a été envoyée !\n\n" +
                                    "✓ Votre CV a été joint : " + selectedCVFile.getName() + "\n" +
                                    "✓ CV sauvegardé : " + cvPath + "\n\n" +
                                    "⚠ Le SMS de confirmation n'a pas pu être envoyé.\n\n" +
                                    "Raisons possibles:\n" +
                                    "- Numéro non vérifié dans Twilio (mode Trial)\n" +
                                    "- Problème de connexion réseau\n" +
                                    "- Crédit Twilio épuisé\n\n" +
                                    "Consultez la console pour plus de détails.");
                }
            } else {
                String message = "Votre candidature a été envoyée !\n\n" +
                        "✓ Votre CV a été joint : " + selectedCVFile.getName() + "\n" +
                        "✓ CV sauvegardé : " + cvPath;
                if (phone.isEmpty()) {
                    message += "\n\nℹ Aucun numéro fourni, pas de SMS envoyé.";
                } else if (!smsService.isEnabled()) {
                    message += "\n\nℹ Service SMS non configuré.";
                }
                StyledAlert.showSuccess("Succès", message);
            }

            handleRetour(); // fermer la vue
        } catch (SQLException e) {
            StyledAlert.showError("Erreur", "Erreur base de données : " + e.getMessage());
        }
    }

    /**
     * Sauvegarde le fichier CV dans le dossier uploads/cv/
     * Retourne le chemin relatif du fichier sauvegardé
     */
    private String saveCVFile(File sourceFile) throws IOException {
        // Créer le dossier uploads/cv s'il n'existe pas
        Path uploadDir = Paths.get(CV_STORAGE_DIR);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // Générer un nom de fichier unique avec timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String extension = getFileExtension(sourceFile.getName());
        String fileName = "CV_Candidat" + candidatId + "_Offre" + offreId + "_" + timestamp + extension;

        // Chemin de destination
        Path destinationPath = uploadDir.resolve(fileName);

        // Copier le fichier
        Files.copy(sourceFile.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

        // Retourner le chemin relatif
        return CV_STORAGE_DIR + fileName;
    }

    /**
     * Récupère l'extension d'un fichier
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot);
        }
        return "";
    }

    /**
     * Retour à la liste des offres
     */
    @FXML
    public void handleBack() {
        OffresShellController shell = OffresShellController.getInstance();
        if (shell != null) {
            shell.showOffresList();
        }
    }

    @FXML
    private void handleRetour() {
        OffresShellController.getInstance().showOffresList();
    }
}