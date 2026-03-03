package services.getude;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailService {

    // Configuration Gmail
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    // Vos identifiants Gmail
    private static final String EXPEDITEUR = "carrieritunisie@gmail.com";
    private static final String MOT_DE_PASSE = "tfph ityi ufsn brdc"; // À remplacer

    public boolean envoyerNotificationCompletionCours(String destinataire, String nomCandidat,
                                                      String titreCours, String lienCertificat) {

        // Formater le nom correctement (première lettre en majuscule)
        String nomFormate = nomCandidat.substring(0, 1).toUpperCase() +
                nomCandidat.substring(1).toLowerCase();

        String sujet = "🎓 FÉLICITATIONS " + nomFormate + " ! Vous avez réussi : " + titreCours;

        String dateFormatee = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy"));

        String numeroCertificat = "CERT-" + java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) +
                "-" + (nomCandidat.length() >= 2 ? nomCandidat.substring(0, 2).toUpperCase() : nomCandidat.toUpperCase());

        // Pour un vrai lien téléchargeable dans un email, on utilise une URL
        // Si c'est un fichier local, on met un message explicatif
        String lienMessage;
        String boutonHtml;

        if (lienCertificat.startsWith("file://")) {
            // Lien local - ne fonctionnera pas dans l'email
            lienMessage = "📁 Le certificat a été enregistré sur votre ordinateur :<br>" +
                    "<strong style='background: #f0f0f0; padding: 8px; border-radius: 5px; font-size: 12px;'>" +
                    lienCertificat.replace("file:///", "") + "</strong>";

            boutonHtml = "<div style='background: #e53e3e; color: white; padding: 15px; border-radius: 10px; margin: 20px 0;'>" +
                    "<p style='margin: 0; font-weight: bold;'>⚠️ Téléchargement non disponible dans l'email</p>" +
                    "<p style='margin: 10px 0 0 0; font-size: 14px;'>Veuillez accéder à la partie Certificats dans l'application pour pouvoir télécharger.</p>" +
                    "</div>" +
                    "<div style='background: #f7fafc; padding: 15px; border-radius: 8px; border: 1px solid #cbd5e0;'>" +
                    "<p style='margin: 0; font-family: monospace; word-break: break-all;'>" +
                    lienCertificat.replace("file:///", "") + "</p></div>";
        } else {
            // Lien HTTP - fonctionne dans l'email
            boutonHtml = "<a href='" + lienCertificat + "' style='display: inline-block; background: linear-gradient(135deg, #231942, #5E548E); color: white; text-decoration: none; padding: 18px 40px; border-radius: 60px; font-size: 20px; font-weight: 700; box-shadow: 0 10px 20px rgba(94,84,142,0.3);'>⬇️ TÉLÉCHARGER MON CERTIFICAT</a>";
        }

        String corps = "<!DOCTYPE html>" +
                "<html lang='fr'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Félicitations !</title>" +
                "    <style>" +
                "        body { font-family: 'Segoe UI', Arial, sans-serif; background: #f0f2f5; margin: 0; padding: 20px; }" +
                "        .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 20px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.1); }" +
                "        .header { background: linear-gradient(135deg, #231942, #5E548E); padding: 40px 30px; text-align: center; }" +
                "        .header h1 { color: white; font-size: 32px; margin: 0; }" +
                "        .header .subtitle { color: rgba(255,255,255,0.9); font-size: 16px; margin-top: 10px; }" +
                "        .content { padding: 40px 30px; }" +
                "        .greeting { font-size: 28px; font-weight: 700; color: #231942; text-align: center; margin-bottom: 10px; }" +
                "        .greeting span { color: #5E548E; }" +
                "        .message { color: #4a5568; line-height: 1.6; text-align: center; margin: 20px 0; }" +
                "        .course-card { background: #f8f4ff; border-radius: 15px; padding: 25px; margin: 25px 0; border-left: 5px solid #5E548E; }" +
                "        .course-card h2 { color: #231942; margin: 0 0 10px 0; }" +
                "        .course-title { font-size: 22px; font-weight: 700; color: #5E548E; margin: 10px 0; }" +
                "        .badge { display: inline-block; background: #10b981; color: white; padding: 5px 15px; border-radius: 50px; font-size: 14px; }" +
                "        .info-box { background: #f0f4f8; border-radius: 10px; padding: 20px; margin: 20px 0; }" +
                "        .info-box p { margin: 8px 0; }" +
                "        .info-box strong { color: #5E548E; }" +
                "        .button-container { text-align: center; margin: 30px 0; }" +
                "        .footer { background: #f8fafc; padding: 30px; text-align: center; color: #718096; font-size: 14px; border-top: 1px solid #e2e8f0; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h1>🎉 FÉLICITATIONS ! 🎉</h1>" +
                "            <div class='subtitle'>Vous avez réussi votre cours</div>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <div class='greeting'>Bravo <span>" + nomFormate + "</span> !</div>" +
                "            <div class='message'>Vous venez de franchir une étape importante dans votre parcours d'apprentissage.</div>" +
                "            <div class='course-card'>" +
                "                <h2>📚 Cours complété</h2>" +
                "                <div class='course-title'>" + titreCours + "</div>" +
                "                <div class='badge'>✅ 100% réussi</div>" +
                "            </div>" +
                "            <div class='info-box'>" +
                "                <p><strong>📅 Date :</strong> " + dateFormatee + "</p>" +
                "                <p><strong>🆔 N° certificat :</strong> " + numeroCertificat + "</p>" +
                "            </div>" +
                "            <div class='button-container'>" + boutonHtml + "</div>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p>© " + java.time.Year.now().getValue() + " Carrieri</p>" +
                "            <p>contact@carrieri.com</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";

        return envoyerEmail(destinataire, sujet, corps);
    }

    private boolean envoyerEmail(String destinataire, String sujet, String corpsHTML) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST);
        props.put("mail.debug", "true"); // IMPORTANT : pour voir les logs

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EXPEDITEUR, MOT_DE_PASSE);
            }
        });

        session.setDebug(true); // Affiche les logs SMTP

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EXPEDITEUR));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
            message.setSubject(sujet);
            message.setContent(corpsHTML, "text/html; charset=utf-8");

            System.out.println("📧 Tentative d'envoi à: " + destinataire);
            Transport.send(message);
            System.out.println("✅ Email envoyé avec succès !");
            return true;

        } catch (MessagingException e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}