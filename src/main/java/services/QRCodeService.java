package services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Service pour générer des QR Codes pour les offres d'emploi
 */
public class QRCodeService {

    private static final int DEFAULT_WIDTH = 400;
    private static final int DEFAULT_HEIGHT = 400;

    /**
     * Génère un QR Code contenant les informations de l'offre
     * @param offreId ID de l'offre
     * @param offreTitre Titre de l'offre
     * @param entreprise Nom de l'entreprise
     * @return BufferedImage du QR Code généré
     */
    public BufferedImage generateQRCodeForOffre(int offreId, String offreTitre, String entreprise)
            throws WriterException {
        // Créer une URL ou un texte avec les infos de l'offre
        String qrContent = buildQRContent(offreId, offreTitre, entreprise);

        return generateQRCode(qrContent, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Génère un QR Code avec contenu personnalisé
     * @param content Contenu du QR Code
     * @param width Largeur
     * @param height Hauteur
     * @return BufferedImage du QR Code généré
     */
    public BufferedImage generateQRCode(String content, int width, int height)
            throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height);

        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    /**
     * Convertit BufferedImage en JavaFX Image
     * @param bufferedImage Image à convertir
     * @return JavaFX Image
     */
    public Image convertToFXImage(BufferedImage bufferedImage) {
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    /**
     * Sauvegarde le QR Code dans un fichier PNG
     * @param bufferedImage Image du QR Code
     * @param outputFile Fichier de sortie
     */
    public void saveQRCodeToFile(BufferedImage bufferedImage, File outputFile)
            throws IOException {
        ImageIO.write(bufferedImage, "PNG", outputFile);
    }

    /**
     * Construit le contenu du QR Code avec les infos de l'offre
     * Format: JSON-like string pour faciliter le parsing
     */
    private String buildQRContent(int offreId, String offreTitre, String entreprise) {
        // Option 1: URL vers l'application (si vous avez un serveur web)
        // return "https://carrieri.com/offre/" + offreId;

        // Option 2: Données structurées
        return String.format(
            "OFFRE D'EMPLOI\n" +
            "ID: %d\n" +
            "Poste: %s\n" +
            "Entreprise: %s\n" +
            "Pour postuler, contactez l'entreprise ou visitez notre plateforme Carrieri",
            offreId, offreTitre, entreprise
        );
    }

    /**
     * Génère un nom de fichier unique pour le QR Code
     * @param offreId ID de l'offre
     * @param offreTitre Titre de l'offre
     * @return Nom de fichier
     */
    public String generateFileName(int offreId, String offreTitre) {
        // Nettoyer le titre pour le nom de fichier
        String cleanTitle = offreTitre
            .replaceAll("[^a-zA-Z0-9]", "_")
            .replaceAll("_+", "_")
            .substring(0, Math.min(offreTitre.length(), 30));

        String timestamp = String.valueOf(System.currentTimeMillis());

        return String.format("QRCode_Offre_%d_%s_%s.png", offreId, cleanTitle, timestamp);
    }
}

