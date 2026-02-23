package main;

import com.google.zxing.WriterException;
import entities.OffreEmploi;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.QRCodeService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Contrôleur pour le popup d'affichage du QR Code d'une offre
 */
public class QRCodePopupController {

    @FXML private ImageView qrImageView;
    @FXML private Label lblOffreTitre;
    @FXML private Label lblEntreprise;
    @FXML private Label lblInstruction;
    @FXML private Button btnDownload;
    @FXML private Button btnClose;

    private final QRCodeService qrCodeService = new QRCodeService();
    private BufferedImage qrCodeImage;
    private OffreEmploi offre;

    /**
     * Initialise le popup avec les données de l'offre
     */
    public void initData(OffreEmploi offre) {
        this.offre = offre;

        // Afficher les infos de l'offre
        lblOffreTitre.setText(offre.getTitre());
        lblEntreprise.setText(offre.getEntreprise());
        lblInstruction.setText("Scannez ce QR Code pour accéder à l'offre d'emploi");

        // Générer et afficher le QR Code
        try {
            generateAndDisplayQRCode();
        } catch (WriterException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                "Impossible de générer le QR Code:\n" + e.getMessage());
        }
    }

    /**
     * Génère le QR Code et l'affiche dans l'ImageView
     */
    private void generateAndDisplayQRCode() throws WriterException {
        // Générer le QR Code
        qrCodeImage = qrCodeService.generateQRCodeForOffre(
            offre.getId(),
            offre.getTitre(),
            offre.getEntreprise()
        );

        // Convertir en JavaFX Image et afficher
        Image fxImage = qrCodeService.convertToFXImage(qrCodeImage);
        qrImageView.setImage(fxImage);

        // Configurer l'ImageView
        qrImageView.setPreserveRatio(true);
        qrImageView.setSmooth(true);
    }

    /**
     * Télécharger le QR Code en PNG
     */
    @FXML
    private void handleDownload() {
        if (qrCodeImage == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun QR Code à télécharger");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le QR Code");

        // Nom de fichier par défaut
        String defaultFileName = qrCodeService.generateFileName(offre.getId(), offre.getTitre());
        fileChooser.setInitialFileName(defaultFileName);

        // Extension
        FileChooser.ExtensionFilter extFilter =
            new FileChooser.ExtensionFilter("PNG Files (*.png)", "*.png");
        fileChooser.getExtensionFilters().add(extFilter);

        // Dossier par défaut (Downloads)
        String userHome = System.getProperty("user.home");
        File downloadsDir = new File(userHome, "Downloads");
        if (downloadsDir.exists()) {
            fileChooser.setInitialDirectory(downloadsDir);
        }

        // Dialogue de sauvegarde
        Stage stage = (Stage) btnDownload.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                qrCodeService.saveQRCodeToFile(qrCodeImage, file);
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "QR Code enregistré avec succès !\n\n" +
                    "Emplacement: " + file.getAbsolutePath());
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'enregistrer le QR Code:\n" + e.getMessage());
            }
        }
    }

    /**
     * Imprimer le QR Code (optionnel - peut être implémenté plus tard)
     */
    @FXML
    private void handlePrint() {
        // TODO: Implémenter l'impression si nécessaire
        showAlert(Alert.AlertType.INFORMATION, "Info",
            "Fonctionnalité d'impression à venir.\n\n" +
            "En attendant, vous pouvez télécharger le QR Code et l'imprimer manuellement.");
    }

    /**
     * Fermer le popup
     */
    @FXML
    private void handleClose() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }

    /**
     * Afficher une alerte
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

