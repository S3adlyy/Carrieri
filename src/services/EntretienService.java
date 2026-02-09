package services;

import entities.Entretien;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EntretienService {
    private Connection connection;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public EntretienService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    /**
     * Create a new entretien and send email to candidate
     */
    public void createEntretien(Entretien entretien, String candidatEmail, String candidatName) throws SQLException {
        String sql = "INSERT INTO entretien (date_entretien, type, status, postulation_id) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pst = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setTimestamp(1, Timestamp.valueOf(entretien.getDateEntretien()));
            pst.setString(2, entretien.getType());
            pst.setString(3, entretien.getStatus());
            pst.setObject(4, entretien.getPostulationId(), Types.INTEGER);

            int affectedRows = pst.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entretien.setId(generatedKeys.getInt(1));
                    }
                }

                System.out.println("✅ Entretien créé avec succès - ID: " + entretien.getId());

                // Send email notification
                try {
                    sendEntretienEmail(entretien, candidatEmail, candidatName);
                    System.out.println("✅ Email envoyé à: " + candidatEmail);
                } catch (Exception e) {
                    System.err.println("⚠️ Erreur lors de l'envoi de l'email: " + e.getMessage());
                    // Don't throw exception, entretien is already created
                }
            }
        }
    }

    /**
     * Get all entretiens
     */
    public List<Entretien> getAllEntretiens() throws SQLException {
        List<Entretien> entretiens = new ArrayList<>();
        String sql = "SELECT * FROM entretien ORDER BY date_entretien DESC";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Entretien entretien = extractEntretienFromResultSet(rs);
                entretiens.add(entretien);
            }
        }

        return entretiens;
    }

    /**
     * Get entretien by ID
     */
    public Entretien getEntretienById(int id) throws SQLException {
        String sql = "SELECT * FROM entretien WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return extractEntretienFromResultSet(rs);
            }
        }

        return null;
    }

    /**
     * Update entretien status
     */
    public void updateStatus(int id, String newStatus) throws SQLException {
        String sql = "UPDATE entretien SET status = ? WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, newStatus);
            pst.setInt(2, id);
            pst.executeUpdate();
            System.out.println("✅ Statut mis à jour: " + newStatus);
        }
    }

    /**
     * Delete entretien
     */
    public void deleteEntretien(int id) throws SQLException {
        String sql = "DELETE FROM entretien WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("✅ Entretien supprimé");
        }
    }

    /**
     * Get candidate info from rendu_mission
     */
    public CandidateInfo getCandidateInfoFromRendu(int renduId) throws SQLException {
        String sql = "SELECT rm.candidat_id, rm.mission_id " +
                "FROM rendu_mission rm " +
                "WHERE rm.id = ?";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, renduId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                CandidateInfo info = new CandidateInfo();
                info.candidatId = rs.getInt("candidat_id");
                info.missionId = rs.getInt("mission_id");

                // Generate mock email and name based on ID
                // In production, you would fetch this from a 'candidat' or 'user' table
                info.email = "candidate" + info.candidatId + "@email.com";
                info.name = "Candidate #" + info.candidatId;

                return info;
            }
        }

        return null;
    }

    /**
     * Send email notification to candidate
     */
    private void sendEntretienEmail(Entretien entretien, String candidatEmail, String candidatName) throws MessagingException {
        // Email configuration
        String host = "smtp.gmail.com"; // Change to your SMTP server
        String from = "recruitment@carrieri.com"; // Change to your email
        String password = "your-app-password"; // Use app-specific password

        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        // Create session - FIXED: Use javax.mail.Authenticator
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        try {
            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(candidatEmail));
            message.setSubject("🎯 Invitation - Entretien de Recrutement | Carrieri");

            // Email body
            String emailBody = createEmailBody(entretien, candidatName);
            message.setContent(emailBody, "text/html; charset=utf-8");

            // Send message
            Transport.send(message);

        } catch (MessagingException e) {
            System.err.println("❌ Erreur d'envoi d'email: " + e.getMessage());
            // For development: just log the email content instead of sending
            System.out.println("\n📧 EMAIL PREVIEW:");
            System.out.println("To: " + candidatEmail);
            System.out.println("Subject: Invitation - Entretien de Recrutement");
            System.out.println(createEmailBody(entretien, candidatName));
            System.out.println("\n");
        }
    }

    /**
     * Create HTML email body
     */
    private String createEmailBody(Entretien entretien, String candidatName) {
        String formattedDate = entretien.getDateEntretien().format(formatter);

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f8f9fa;'>" +

                "<!-- Header -->" +
                "<div style='background: linear-gradient(135deg, #231942 0%, #5E548E 100%); padding: 30px; text-align: center; border-radius: 10px 10px 0 0;'>" +
                "<h1 style='color: white; margin: 0; font-size: 28px;'>🎯 Carrieri</h1>" +
                "<p style='color: #E0B1CB; margin: 10px 0 0 0;'>Plateforme de Recrutement</p>" +
                "</div>" +

                "<!-- Body -->" +
                "<div style='background: white; padding: 40px 30px; border-radius: 0 0 10px 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                "<h2 style='color: #231942; margin-top: 0;'>Bonjour " + candidatName + ",</h2>" +

                "<p style='font-size: 16px; color: #555;'>" +
                "Nous avons le plaisir de vous inviter à un entretien de recrutement." +
                "</p>" +

                "<!-- Meeting Details Box -->" +
                "<div style='background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); padding: 25px; border-left: 4px solid #5E548E; margin: 25px 0; border-radius: 5px;'>" +
                "<h3 style='margin-top: 0; color: #231942;'>📋 Détails de l'Entretien</h3>" +

                "<table style='width: 100%; border-collapse: collapse;'>" +
                "<tr><td style='padding: 10px 0; font-weight: bold; color: #5E548E;'>📅 Date et Heure:</td><td style='padding: 10px 0;'>" + formattedDate + "</td></tr>" +
                "<tr><td style='padding: 10px 0; font-weight: bold; color: #5E548E;'>🎯 Type:</td><td style='padding: 10px 0;'>" + entretien.getType() + "</td></tr>" +
                "<tr><td style='padding: 10px 0; font-weight: bold; color: #5E548E;'>✅ Statut:</td><td style='padding: 10px 0;'><span style='background: #10b981; color: white; padding: 4px 12px; border-radius: 20px; font-size: 12px;'>" + entretien.getStatus() + "</span></td></tr>" +
                "<tr><td style='padding: 10px 0; font-weight: bold; color: #5E548E;'>🆔 Référence:</td><td style='padding: 10px 0;'>#" + entretien.getId() + "</td></tr>" +
                "</table>" +
                "</div>" +

                "<!-- Instructions -->" +
                "<div style='background: #fff3cd; border-left: 4px solid #f59e0b; padding: 15px; margin: 20px 0; border-radius: 5px;'>" +
                "<p style='margin: 0; color: #856404;'><strong>💡 Conseils:</strong></p>" +
                "<ul style='margin: 10px 0 0 0; color: #856404;'>" +
                "<li>Préparez vos documents (CV, diplômes)</li>" +
                "<li>Soyez ponctuel(le)</li>" +
                "<li>Préparez des questions sur l'entreprise</li>" +
                "</ul>" +
                "</div>" +

                "<!-- Action Button -->" +
                "<div style='text-align: center; margin: 30px 0;'>" +
                "<a href='#' style='background: linear-gradient(135deg, #231942 0%, #5E548E 100%); color: white; padding: 15px 40px; text-decoration: none; border-radius: 50px; font-weight: bold; display: inline-block; box-shadow: 0 4px 6px rgba(94,84,142,0.3);'>" +
                "✅ Confirmer ma Présence" +
                "</a>" +
                "</div>" +

                "<p style='color: #777; font-size: 14px; margin-top: 30px;'>" +
                "Si vous avez des questions, n'hésitez pas à nous contacter à <a href='mailto:contact@carrieri.com' style='color: #5E548E;'>contact@carrieri.com</a>" +
                "</p>" +

                "<p style='color: #777; font-size: 14px;'>Cordialement,<br><strong style='color: #231942;'>L'équipe Carrieri</strong></p>" +
                "</div>" +

                "<!-- Footer -->" +
                "<div style='text-align: center; padding: 20px; color: #999; font-size: 12px;'>" +
                "<p>© 2024 Carrieri - Plateforme de Recrutement Intelligent</p>" +
                "<p style='margin: 5px 0;'>📧 contact@carrieri.com | 🌐 www.carrieri.com</p>" +
                "</div>" +

                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Helper method to extract Entretien from ResultSet
     */
    private Entretien extractEntretienFromResultSet(ResultSet rs) throws SQLException {
        Entretien entretien = new Entretien();
        entretien.setId(rs.getInt("id"));
        entretien.setDateEntretien(rs.getTimestamp("date_entretien").toLocalDateTime());
        entretien.setType(rs.getString("type"));
        entretien.setStatus(rs.getString("status"));
        entretien.setPostulationId(rs.getObject("postulation_id", Integer.class));
        return entretien;
    }

    /**
     * Inner class to hold candidate information
     */
    public static class CandidateInfo {
        public int candidatId;
        public int missionId;
        public String email;
        public String name;
    }
}