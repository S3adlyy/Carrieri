package services.getude;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import entities.getude.Certification;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CertificationService implements ICertificationService {

    private Connection connection;

    public CertificationService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    // ============================================
    // PARTIE 1: GESTION BASE DE DONNÉES
    // ============================================

    @Override
    public void ajouter(Certification c) throws SQLException {
        validateCertification(c);
        String sql = "INSERT INTO certification (candidat_id, cours_id, date_obtention) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, c.getCandidatId());
            ps.setInt(2, c.getCoursId());
            ps.setTimestamp(3, Timestamp.valueOf(c.getDateObtention()));
            ps.executeUpdate();
            System.out.println("✅ Certification enregistrée en base");
        }
    }

    @Override
    public void modifier(Certification c) throws SQLException {
        validateCertification(c);
        String sql = "UPDATE certification SET candidat_id = ?, cours_id = ?, date_obtention = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, c.getCandidatId());
            ps.setInt(2, c.getCoursId());
            ps.setTimestamp(3, Timestamp.valueOf(c.getDateObtention()));
            ps.setInt(4, c.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM certification WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ Certification supprimée: " + id);
        }
    }

    @Override
    public List<Certification> read() throws SQLException {
        List<Certification> list = new ArrayList<>();
        String sql = "SELECT * FROM certification ORDER BY date_obtention DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Certification c = new Certification(
                        rs.getInt("id"),
                        rs.getInt("candidat_id"),
                        rs.getInt("cours_id"),
                        rs.getTimestamp("date_obtention").toLocalDateTime()
                );
                list.add(c);
            }
            System.out.println("📄 Certifications chargées: " + list.size());
        }
        return list;
    }

    @Override
    public Certification readByCoursAndCandidat(int coursId, int candidatId) throws SQLException {
        String sql = "SELECT * FROM certification WHERE cours_id = ? AND candidat_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, coursId);
            ps.setInt(2, candidatId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Certification(
                        rs.getInt("id"),
                        rs.getInt("candidat_id"),
                        rs.getInt("cours_id"),
                        rs.getTimestamp("date_obtention").toLocalDateTime()
                );
            }
        }
        return null;
    }

    @Override
    public boolean aDejaCertificat(int coursId, int candidatId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM certification WHERE cours_id = ? AND candidat_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, coursId);
            ps.setInt(2, candidatId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    // ============================================
    // PARTIE 2: GÉNÉRATION PDF
    // ============================================

    @Override
    public void genererCertification(String nomCandidat, String titreCours, String cheminFichier) {
        genererCertification(nomCandidat, titreCours, cheminFichier, LocalDateTime.now());
    }

    @Override
    public void genererCertification(String nomCandidat, String titreCours, String cheminFichier, LocalDateTime dateObtention) {
        try {
            PdfWriter writer = new PdfWriter(cheminFichier);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            Paragraph titre = new Paragraph("CERTIFICATION")
                    .setFontSize(36)
                    .setBold()
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
            document.add(titre);

            Paragraph texte = new Paragraph()
                    .add(new Text("Ce certificat est décerné à ").setFontSize(14))
                    .add(new Text(nomCandidat).setBold().setFontSize(16))
                    .add(new Text(" pour avoir complété le cours ").setFontSize(14))
                    .add(new Text("\"" + titreCours + "\"").setBold().setFontSize(16))
                    .add(new Text(".").setFontSize(14))
                    .setMarginTop(50)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
            document.add(texte);

            String date = dateObtention.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
            Paragraph dateParagraphe = new Paragraph("Date : " + date)
                    .setFontSize(12)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)
                    .setMarginTop(100);
            document.add(dateParagraphe);

            String numero = "CERT-" + dateObtention.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Paragraph numParagraphe = new Paragraph("N° " + numero)
                    .setFontSize(10)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)
                    .setMarginTop(20);
            document.add(numParagraphe);

            document.close();
            System.out.println("✅ PDF généré : " + cheminFichier);

        } catch (Exception e) {
            System.err.println("❌ Erreur génération PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================
    // PARTIE 3: MÉTHODES UTILITAIRES
    // ============================================

    @Override
    public void genererEtEnregistrer(String nomCandidat, String titreCours, String cheminFichier,
                                     int candidatId, int coursId) throws SQLException {
        genererCertification(nomCandidat, titreCours, cheminFichier);
        Certification certif = new Certification(candidatId, coursId, LocalDateTime.now());
        ajouter(certif);
        System.out.println("✅ Certification complète: " + titreCours + " pour " + nomCandidat);
    }

    @Override
    public String genererNomFichier(String nomCandidat, String titreCours) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String nom = nomCandidat.replace(" ", "_");
        String cours = titreCours.replace(" ", "_");
        return "certificats/certificat_" + nom + "_" + cours + "_" + date + ".pdf";
    }

    @Override
    public List<Certification> getAll() throws SQLException {
        return read();
    }

    // ============================================
    // VALIDATION
    // ============================================
    private void validateCertification(Certification certification) {
        // Validation du candidat ID
        if (certification.getCandidatId() <= 0) {
            throw new IllegalArgumentException("ID candidat invalide");
        }

        // Validation du cours ID
        if (certification.getCoursId() <= 0) {
            throw new IllegalArgumentException("ID cours invalide");
        }

        // Validation de la date d'obtention
        if (certification.getDateObtention() == null) {
            throw new IllegalArgumentException("La date d'obtention est obligatoire");
        }

        // Vérifier que la date n'est pas dans le futur
        if (certification.getDateObtention().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("La date d'obtention ne peut pas être dans le futur");
        }
    }
    // Ajoutez cette méthode dans CertificationService.java
    public List<Certification> readByCandidat(int candidatId) throws SQLException {
        List<Certification> list = new ArrayList<>();
        String sql = "SELECT * FROM certification WHERE candidat_id = ? ORDER BY date_obtention DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, candidatId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Certification c = new Certification(
                        rs.getInt("id"),
                        rs.getInt("candidat_id"),
                        rs.getInt("cours_id"),
                        rs.getTimestamp("date_obtention").toLocalDateTime()
                );
                list.add(c);
            }
        }
        return list;
    }
    // ============================================
// PARTIE 4: NOUVELLES MÉTHODES POUR LA GESTION DES CHEMINS
// ============================================

    /**
     * Ajoute un certificat et retourne son ID généré
     */
    public int ajouterEtRetournerId(Certification c) throws SQLException {
        validateCertification(c);
        String sql = "INSERT INTO certification (candidat_id, cours_id, date_obtention) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getCandidatId());
            ps.setInt(2, c.getCoursId());
            ps.setTimestamp(3, Timestamp.valueOf(c.getDateObtention()));
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                c.setId(id);
                System.out.println("✅ Certification enregistrée avec ID: " + id);
                return id;
            }
        }
        return -1;
    }

    /**
     * Met à jour le chemin du fichier certificat
     */
    public void updateCheminFichier(int certificatId, String cheminFichier) throws SQLException {
        // D'abord, vérifier si la colonne existe, sinon l'ajouter
        String checkColumnSql = "SHOW COLUMNS FROM certification LIKE 'chemin_fichier'";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(checkColumnSql)) {
            if (!rs.next()) {
                // La colonne n'existe pas, on l'ajoute
                String alterSql = "ALTER TABLE certification ADD COLUMN chemin_fichier VARCHAR(500)";
                stmt.executeUpdate(alterSql);
                System.out.println("✅ Colonne 'chemin_fichier' ajoutée à la table certification");
            }
        }

        // Maintenant on peut faire l'UPDATE
        String sql = "UPDATE certification SET chemin_fichier = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, cheminFichier);
            ps.setInt(2, certificatId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Chemin du certificat mis à jour pour l'ID: " + certificatId);
            }
        }
    }

    /**
     * Récupère le chemin d'un certificat
     */
    public String getCheminFichier(int certificatId) throws SQLException {
        String sql = "SELECT chemin_fichier FROM certification WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, certificatId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("chemin_fichier");
            }
        }
        return null;
    }
    public boolean aPDFEnBase(int certificatId) throws SQLException {
        String sql = "SELECT fichier_pdf IS NOT NULL as a_pdf FROM certification WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, certificatId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("a_pdf");
            }
        }
        return false;
    }

    /**
     * Récupère le PDF depuis la base de données
     */
    public byte[] getPDFBytes(int certificatId) throws SQLException {
        String sql = "SELECT fichier_pdf FROM certification WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, certificatId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBytes("fichier_pdf");
            }
        }
        return null;
    }

    /**
     * Stocke les bytes d'un PDF dans la base
     */
    public void stockerPDF(int certificatId, byte[] pdfBytes) throws SQLException {
        String sql = "UPDATE certification SET fichier_pdf = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, pdfBytes);
            ps.setInt(2, certificatId);
            ps.executeUpdate();
            System.out.println("✅ PDF stocké en base pour le certificat ID: " + certificatId);
        }
    }
}

