package services;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailService {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String EMAIL_EXPEDITEUR = "selimbabk28@gmail.com"; // À remplacer
    

    public static void envoyerEmail(String destinataire, String sujet, String contenu) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_EXPEDITEUR, MOT_DE_PASSE);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_EXPEDITEUR));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
            message.setSubject(sujet);
            message.setContent(contenu, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✅ Email envoyé à " + destinataire);

        } catch (MessagingException e) {
            System.err.println("❌ Erreur d'envoi d'email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void envoyerEmailUtilisateur(String email, String nom, String objetReclamation, String statut, String reponse) {
        String sujet = "Mise à jour de votre réclamation #" + objetReclamation;

        String contenu = "<html>" +
                "<body style='font-family: Arial, sans-serif; padding: 20px;'>" +
                "<h2 style='color: #2c3e50;'>Bonjour " + nom + ",</h2>" +
                "<p>Votre réclamation concernant <strong>" + objetReclamation + "</strong> a été traitée.</p>" +
                "<div style='background-color: #ecf0f1; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                "<h3 style='color: #27ae60;'>Nouveau statut : " + statut + "</h3>" +
                "<p><strong>Réponse de l'administrateur :</strong></p>" +
                "<p style='background-color: white; padding: 10px; border-left: 4px solid #3498db;'>" + reponse + "</p>" +
                "</div>" +
                "<p>Merci de votre confiance.</p>" +
                "<p>L'équipe de support</p>" +
                "<hr style='border: 1px solid #bdc3c7;'/>" +
                "<p style='color: #7f8c8d; font-size: 12px;'>Cet email est automatique, merci de ne pas y répondre.</p>" +
                "</body>" +
                "</html>";

        envoyerEmail(email, sujet, contenu);
    }
}