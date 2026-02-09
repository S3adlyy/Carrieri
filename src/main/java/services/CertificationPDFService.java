package services;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CertificationPDFService {

    public void genererCertification(String nomCandidat, String titreCours, String cheminFichier) {
        try {
            // Créer le fichier PDF
            PdfWriter writer = new PdfWriter(cheminFichier);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Ajouter titre
            Paragraph titre = new Paragraph("CERTIFICATION")
                    .setFontSize(36)
                    .setBold()
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
            document.add(titre);

            // Ajouter texte du certificat
            Paragraph texte = new Paragraph()
                    .add(new Text("Ce certificat est décerné à ").setFontSize(14))
                    .add(new Text(nomCandidat).setBold().setFontSize(16))
                    .add(new Text(" pour avoir complété le cours ").setFontSize(14))
                    .add(new Text("\"" + titreCours + "\"").setBold().setFontSize(16))
                    .add(new Text(".").setFontSize(14))
                    .setMarginTop(50)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
            document.add(texte);

            // Ajouter date
            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
            Paragraph dateParagraphe = new Paragraph("Date : " + date)
                    .setFontSize(12)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)
                    .setMarginTop(100);
            document.add(dateParagraphe);

            // Fermer le document
            document.close();
            System.out.println("Certification générée : " + cheminFichier);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

