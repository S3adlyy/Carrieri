package main;

import com.google.zxing.WriterException;
import entities.OffreEmploi;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.QRCodeService;
import utils.StyledAlert;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class QRCodeController {

    @FXML private ImageView qrImageView;
    @FXML private Label lblOffreTitre;
    @FXML private Label lblOffreTitreDetail;
    @FXML private Label lblEntreprise;
    @FXML private Label lblInstruction;
    @FXML private Button btnDownload;
    @FXML private Button btnBack;

    private final QRCodeService qrCodeService = new QRCodeService();
    private BufferedImage qrCodeImage;
    private OffreEmploi offre;

    public void initData(OffreEmploi offre) {
        this.offre = offre;

        lblOffreTitre.setText("QR Code - " + offre.getTitre());
        lblOffreTitreDetail.setText(offre.getTitre());
        lblEntreprise.setText(offre.getEntreprise());

        try {
            generateAndDisplayQRCode();
        } catch (WriterException e) {
            StyledAlert.showError("Erreur", "Impossible de générer le QR Code:\n" + e.getMessage());
        }
    }

    private void generateAndDisplayQRCode() throws WriterException {
        qrCodeImage = qrCodeService.generateQRCodeForOffre(
                offre.getId(),
                offre.getTitre(),
                offre.getEntreprise()
        );

        Image fxImage = qrCodeService.convertToFXImage(qrCodeImage);
        qrImageView.setImage(fxImage);
        qrImageView.setPreserveRatio(true);
        qrImageView.setSmooth(true);
    }

    @FXML
    private void handleDownload() {
        if (qrCodeImage == null) {
            StyledAlert.showError("Erreur", "Aucun QR Code à télécharger");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le QR Code");

        String defaultFileName = qrCodeService.generateFileName(offre.getId(), offre.getTitre());
        fileChooser.setInitialFileName(defaultFileName);

        FileChooser.ExtensionFilter extFilter =
                new FileChooser.ExtensionFilter("PNG Files (*.png)", "*.png");
        fileChooser.getExtensionFilters().add(extFilter);

        String userHome = System.getProperty("user.home");
        File downloadsDir = new File(userHome, "Downloads");
        if (downloadsDir.exists()) {
            fileChooser.setInitialDirectory(downloadsDir);
        }

        Stage stage = (Stage) btnDownload.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                qrCodeService.saveQRCodeToFile(qrCodeImage, file);
                StyledAlert.showSuccess("Succès",
                        "QR Code enregistré avec succès !\n\nEmplacement: " + file.getAbsolutePath());
            } catch (IOException e) {
                StyledAlert.showError("Erreur", "Impossible d'enregistrer le QR Code:\n" + e.getMessage());
            }
        }
    }

    @FXML
    private void handlePrint() {
        StyledAlert.showInfo("Info",
                "Fonctionnalité d'impression à venir.\n\n" +
                        "En attendant, téléchargez le QR Code et imprimez-le manuellement.");
    }

    @FXML
    private void handleBack() {
        OffresShellController.getInstance().showOffresTable(); // ou showOffresList() selon le contexte
    }
}