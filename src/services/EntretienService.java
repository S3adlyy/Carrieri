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

    // ============ CONFIGURE THESE WITH YOUR ACTUAL GMAIL CREDENTIALS ============
    private static final String FROM_EMAIL = "saadliwassieo@gmail.com";  // Your Gmail address
    private static final String APP_PASSWORD = "vxlz iddh gcox lual";  // Your 16-character App Password
    // ============================================================================

    public EntretienService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    /**
     * Create a new entretien and send email to candidate - FIXED VERSION
     */
    public void createEntretien(Entretien entretien, String candidatEmail, String candidatName) throws Exception {
        // DON'T create a new connection - use the existing one
        // DON'T close the connection in finally block

        PreparedStatement ps = null;
        ResultSet generatedKeys = null;

        try {
            // Get postulation ID from rendu mission
            int postulationId = getPostulationIdFromRenduMission(entretien.getPostulationId());
            if (postulationId == -1) {
                throw new Exception("Impossible de trouver une postulation pour ce rendu");
            }
            entretien.setPostulationId(postulationId);

            // Insérer l'entretien
            String query = "INSERT INTO entretien (date_entretien, type, status, postulation_id) VALUES (?, ?, ?, ?)";
            ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            ps.setTimestamp(1, Timestamp.valueOf(entretien.getDateEntretien()));
            ps.setString(2, entretien.getType());
            ps.setString(3, entretien.getStatus());
            ps.setInt(4, entretien.getPostulationId());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new Exception("Échec de la création de l'entretien");
            }

            // Récupérer l'ID généré
            generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                entretien.setId(generatedKeys.getInt(1));
            }

            // Envoyer l'email - this doesn't need DB connection
            sendEntretienEmail(entretien, candidatEmail, candidatName);

        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("Erreur lors de la création de l'entretien: " + e.getMessage());
        } finally {
            // ONLY close the PreparedStatement and ResultSet, NOT the connection
            try {
                if (generatedKeys != null) generatedKeys.close();
                if (ps != null) ps.close();
                // DON'T close connection - it's reused
                // if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get candidate info from rendu_mission - FIXED VERSION
     */
    public CandidateInfo getCandidateInfoFromRendu(int renduId) throws SQLException {
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT candidat_id, mission_id FROM rendu_mission WHERE id = ?";
            pst = connection.prepareStatement(sql);
            pst.setInt(1, renduId);
            rs = pst.executeQuery();

            if (rs.next()) {
                CandidateInfo info = new CandidateInfo();
                info.candidatId = rs.getInt("candidat_id");
                info.missionId = rs.getInt("mission_id");
                info.email = "candidate" + info.candidatId + "@email.com";
                info.name = "Candidate #" + info.candidatId;
                return info;
            }
        } finally {
            // ONLY close the ResultSet and PreparedStatement, NOT the connection
            if (rs != null) rs.close();
            if (pst != null) pst.close();
        }

        return null;
    }

    /**
     * Récupère le postulation_id à partir d'un rendu_mission_id - FIXED VERSION
     */
    public int getPostulationIdFromRenduMission(int renduMissionId) {
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        PreparedStatement ps3 = null;
        ResultSet rs = null;
        ResultSet rs2 = null;
        ResultSet rs3 = null;

        try {
            // 1. Récupérer candidat_id et mission_id depuis rendu_mission
            String query = "SELECT candidat_id, mission_id FROM rendu_mission WHERE id = ?";
            ps = connection.prepareStatement(query);
            ps.setInt(1, renduMissionId);
            rs = ps.executeQuery();

            if (rs.next()) {
                int candidatId = rs.getInt("candidat_id");
                int offreId = rs.getInt("mission_id");

                System.out.println("Recherche postulation avec candidat_id: " + candidatId + " et offre_id: " + offreId);

                // 2. Trouver la postulation correspondante
                String postulationQuery = "SELECT id FROM postulation WHERE candidat_id = ? AND offre_id = ?";
                ps2 = connection.prepareStatement(postulationQuery);
                ps2.setInt(1, candidatId);
                ps2.setInt(2, offreId);
                rs2 = ps2.executeQuery();

                if (rs2.next()) {
                    int postulationId = rs2.getInt("id");
                    System.out.println("✓ Postulation trouvée avec id: " + postulationId);
                    return postulationId;
                }

                // 3. Si pas trouvé, essayer avec seulement candidat_id (dernière postulation)
                String lastPostulationQuery = "SELECT id FROM postulation WHERE candidat_id = ? ORDER BY date_postulation DESC LIMIT 1";
                ps3 = connection.prepareStatement(lastPostulationQuery);
                ps3.setInt(1, candidatId);
                rs3 = ps3.executeQuery();

                if (rs3.next()) {
                    int postulationId = rs3.getInt("id");
                    System.out.println("⚠ Dernière postulation du candidat trouvée avec id: " + postulationId);
                    return postulationId;
                }
            }

            System.out.println("❌ Aucune postulation trouvée pour rendu_mission_id: " + renduMissionId);
            return -1;

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        } finally {
            // ONLY close the ResultSets and PreparedStatements, NOT the connection
            try {
                if (rs3 != null) rs3.close();
                if (ps3 != null) ps3.close();
                if (rs2 != null) rs2.close();
                if (ps2 != null) ps2.close();
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                // DON'T close connection
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get all entretiens
     */
    public List<Entretien> getAllEntretiens() throws SQLException {
        List<Entretien> entretiens = new ArrayList<>();
        String sql = "SELECT * FROM entretien ORDER BY date_entretien DESC";
        Statement st = null;
        ResultSet rs = null;

        try {
            st = connection.createStatement();
            rs = st.executeQuery(sql);

            while (rs.next()) {
                Entretien entretien = extractEntretienFromResultSet(rs);
                entretiens.add(entretien);
            }
        } finally {
            if (rs != null) rs.close();
            if (st != null) st.close();
        }

        return entretiens;
    }

    /**
     * Get entretien by ID
     */
    public Entretien getEntretienById(int id) throws SQLException {
        String sql = "SELECT * FROM entretien WHERE id = ?";
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            pst = connection.prepareStatement(sql);
            pst.setInt(1, id);
            rs = pst.executeQuery();

            if (rs.next()) {
                return extractEntretienFromResultSet(rs);
            }
        } finally {
            if (rs != null) rs.close();
            if (pst != null) pst.close();
        }

        return null;
    }

    /**
     * Update entretien status
     */
    public void updateStatus(int id, String newStatus) throws SQLException {
        String sql = "UPDATE entretien SET status = ? WHERE id = ?";
        PreparedStatement pst = null;

        try {
            pst = connection.prepareStatement(sql);
            pst.setString(1, newStatus);
            pst.setInt(2, id);
            pst.executeUpdate();
            System.out.println("✅ Statut mis à jour: " + newStatus);
        } finally {
            if (pst != null) pst.close();
        }
    }

    /**
     * Delete entretien
     */
    public void deleteEntretien(int id) throws SQLException {
        String sql = "DELETE FROM entretien WHERE id = ?";
        PreparedStatement pst = null;

        try {
            pst = connection.prepareStatement(sql);
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("✅ Entretien supprimé");
        } finally {
            if (pst != null) pst.close();
        }
    }

    // ==================== EMAIL METHODS (No changes needed) ====================

    private void sendEntretienEmail(Entretien entretien, String candidatEmail, String candidatName) {
        final String username = FROM_EMAIL;
        final String password = APP_PASSWORD;

        System.out.println("📧 Attempting to send email...");
        System.out.println("   From: " + username);
        System.out.println("   To: " + candidatEmail);

        // METHOD 1: Using SSL (Port 465)
        Properties props1 = new Properties();
        props1.put("mail.smtp.host", "smtp.gmail.com");
        props1.put("mail.smtp.port", "465");
        props1.put("mail.smtp.auth", "true");
        props1.put("mail.smtp.ssl.enable", "true");
        props1.put("mail.smtp.socketFactory.port", "465");
        props1.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props1.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props1.put("mail.debug", "true");

        // METHOD 2: Using TLS (Port 587)
        Properties props2 = new Properties();
        props2.put("mail.smtp.host", "smtp.gmail.com");
        props2.put("mail.smtp.port", "587");
        props2.put("mail.smtp.auth", "true");
        props2.put("mail.smtp.starttls.enable", "true");
        props2.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props2.put("mail.debug", "true");

        // Try SSL first
        try {
            sendEmailWithProps(props1, username, password, candidatEmail, candidatName, entretien);
            System.out.println("✅ Email sent successfully via SSL!");
            return;
        } catch (Exception e1) {
            System.err.println("⚠️ SSL method failed: " + e1.getMessage());
            System.out.println("🔄 Trying TLS method...");

            try {
                sendEmailWithProps(props2, username, password, candidatEmail, candidatName, entretien);
                System.out.println("✅ Email sent successfully via TLS!");
                return;
            } catch (Exception e2) {
                System.err.println("❌ TLS method also failed: " + e2.getMessage());
                e2.printStackTrace();
            }
        }

        // Demo mode - show email in console
        System.out.println("\n📧 ============ EMAIL PREVIEW ============");
        System.out.println("To: " + candidatEmail);
        System.out.println(createEmailBody(entretien, candidatName));
        System.out.println("========================================\n");
    }

    private void sendEmailWithProps(Properties props, String username, String password,
                                    String toEmail, String candidatName, Entretien entretien)
            throws MessagingException {
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        session.setDebug(true);

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("🎯 Invitation - Entretien de Recrutement | Carrieri");
        message.setSentDate(new java.util.Date());

        String emailBody = createEmailBody(entretien, candidatName);
        message.setContent(emailBody, "text/html; charset=utf-8");

        Transport.send(message);
    }

    private String createEmailBody(Entretien entretien, String candidatName) {
        String formattedDate = entretien.getDateEntretien().format(formatter);
        String interviewType = entretien.getType();
        String reference = "#" + entretien.getId();

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }" +
                ".container { max-width: 600px; margin: 20px auto; background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%); border-radius: 16px; overflow: hidden; box-shadow: 0 8px 20px rgba(35,25,66,0.15); }" +
                ".header { background: linear-gradient(135deg, #231942 0%, #5E548E 100%); padding: 30px; text-align: center; }" +
                ".header h1 { color: white; margin: 0; font-size: 28px; font-weight: 900; }" +
                ".header p { color: #E0B1CB; margin: 10px 0 0 0; font-size: 16px; }" +
                ".content { padding: 40px 30px; background: white; }" +
                ".content h2 { color: #231942; margin-top: 0; font-size: 24px; }" +
                ".details-card { background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); padding: 25px; border-left: 4px solid #5E548E; margin: 25px 0; border-radius: 8px; }" +
                ".details-card h3 { margin-top: 0; color: #231942; font-size: 18px; border-bottom: 1px solid rgba(94,84,142,0.2); padding-bottom: 12px; }" +
                ".details-table { width: 100%; border-collapse: collapse; }" +
                ".details-table td { padding: 12px 0; }" +
                ".details-table td:first-child { font-weight: bold; color: #5E548E; width: 120px; }" +
                ".badge { background: #10b981; color: white; padding: 4px 12px; border-radius: 50px; font-size: 12px; font-weight: 600; display: inline-block; }" +
                ".tip-box { background: #fff3cd; border-left: 4px solid #f59e0b; padding: 20px; margin: 25px 0; border-radius: 8px; }" +
                ".tip-box p { margin: 0 0 10px 0; color: #856404; font-weight: 700; }" +
                ".tip-box ul { margin: 0; color: #856404; padding-left: 20px; }" +
                ".tip-box li { margin-bottom: 8px; }" +
                ".button { display: inline-block; background: linear-gradient(135deg, #231942 0%, #5E548E 100%); color: white; padding: 14px 40px; text-decoration: none; border-radius: 50px; font-weight: 700; font-size: 16px; margin: 20px 0; box-shadow: 0 4px 12px rgba(94,84,142,0.3); }" +
                ".footer { background: #231942; padding: 25px; text-align: center; }" +
                ".footer p { margin: 0; color: rgba(255,255,255,0.9); font-size: 13px; }" +
                ".footer small { color: rgba(255,255,255,0.7); font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +

                "<!-- Header with Gradient -->" +
                "<div class='header'>" +
                "<h1>🎯 Carrieri</h1>" +
                "<p>Plateforme de Recrutement Intelligent</p>" +
                "</div>" +

                "<!-- Body Content -->" +
                "<div class='content'>" +
                "<h2>Bonjour " + candidatName + ",</h2>" +

                "<p style='font-size: 16px; color: #555; margin-bottom: 25px;'>" +
                "Nous avons le plaisir de vous inviter à un entretien dans le cadre de votre candidature chez <strong>Carrieri</strong>." +
                "</p>" +

                "<!-- Interview Details Card -->" +
                "<div class='details-card'>" +
                "<h3>📋 Détails de l'Entretien</h3>" +

                "<table class='details-table'>" +
                "<tr>" +
                "<td>📅 Date :</td>" +
                "<td style='color: #231942; font-weight: 600;'>" + formattedDate + "</td>" +
                "</tr>" +
                "<tr>" +
                "<td>🎯 Type :</td>" +
                "<td><span style='background: rgba(94,84,142,0.1); padding: 6px 16px; border-radius: 50px; color: #231942; font-weight: 600;'>" + interviewType + "</span></td>" +
                "</tr>" +
                "<tr>" +
                "<td>✅ Statut :</td>" +
                "<td><span class='badge'>" + entretien.getStatus() + "</span></td>" +
                "</tr>" +
                "<tr>" +
                "<td>🆔 Référence :</td>" +
                "<td style='font-family: monospace; color: #5E548E; font-weight: 600;'>" + reference + "</td>" +
                "</tr>" +
                "</table>" +
                "</div>" +

                "<!-- Preparation Tips -->" +
                "<div class='tip-box'>" +
                "<p>💡 Préparation recommandée :</p>" +
                "<ul>" +
                "<li>📄 Préparez votre CV et vos diplômes</li>" +
                "<li>⏰ Connectez-vous 5 minutes avant l'heure prévue</li>" +
                "<li>❓ Préparez vos questions sur le poste et l'entreprise</li>" +
                "<li>🎥 Testez votre caméra et microphone</li>" +
                "</ul>" +
                "</div>" +

                "<!-- Confirmation Button -->" +
                "<div style='text-align: center; margin: 30px 0;'>" +
                "<a href='#' class='button'>✅ Confirmer ma présence</a>" +
                "<p style='color: #999; font-size: 12px; margin-top: 10px;'>Ce lien expirera dans 7 jours</p>" +
                "</div>" +

                "<!-- Contact Information -->" +
                "<p style='color: #777; font-size: 14px; margin-top: 30px;'>" +
                "Pour toute question, contactez-nous :<br>" +
                "📧 <a href='mailto:contact@carrieri.com' style='color: #5E548E; text-decoration: none; font-weight: 600;'>contact@carrieri.com</a><br>" +
                "📞 +33 1 23 45 67 89" +
                "</p>" +

                "<p style='color: #777; font-size: 14px; margin-top: 30px; border-top: 1px solid #e9ecef; padding-top: 25px;'>" +
                "Cordialement,<br>" +
                "<strong style='color: #231942; font-size: 16px;'>L'équipe Carrieri</strong>" +
                "</p>" +
                "</div>" +

                "<!-- Footer -->" +
                "<div class='footer'>" +
                "<p>© " + java.time.Year.now().getValue() + " Carrieri - Plateforme de Recrutement Intelligent</p>" +
                "<small>📧 contact@carrieri.com | 🌐 www.carrieri.com | 📍 Paris, France</small>" +
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

        public CandidateInfo() {}

        public CandidateInfo(int candidatId, int missionId, String email, String name) {
            this.candidatId = candidatId;
            this.missionId = missionId;
            this.email = email;
            this.name = name;
        }
    }
}