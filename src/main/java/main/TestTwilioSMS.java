package main;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class TestTwilioSMS {

    // Vos credentials Twilio
    public static final String ACCOUNT_SID = "AC_REDACTED";
    public static final String AUTH_TOKEN = "TOKEN_REDACTED";
    public static final String FROM_PHONE = "+19853364277";
    public static final String TO_PHONE = "+21655616110";

    public static void main(String[] args) {
        System.out.println("=== TEST DIRECT TWILIO API ===");
        System.out.println("Account SID: " + ACCOUNT_SID);
        System.out.println("From: " + FROM_PHONE);
        System.out.println("To: " + TO_PHONE);
        System.out.println();

        try {
            // Initialiser Twilio
            System.out.println("1. Initialisation de Twilio...");
            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
            System.out.println("   ✓ Twilio initialisé");
            System.out.println();

            // Créer le message
            System.out.println("2. Création du message SMS...");
            String messageBody = "Test SMS depuis Goffres - Si vous recevez ce message, l'intégration fonctionne !";

            Message message = Message.creator(
                new PhoneNumber(TO_PHONE),
                new PhoneNumber(FROM_PHONE),
                messageBody
            ).create();

            System.out.println("   ✓ Message créé avec succès !");
            System.out.println();

            // Afficher les détails
            System.out.println("3. Détails du SMS:");
            System.out.println("   - SID: " + message.getSid());
            System.out.println("   - Status: " + message.getStatus());
            System.out.println("   - From: " + message.getFrom());
            System.out.println("   - To: " + message.getTo());
            System.out.println("   - Date Created: " + message.getDateCreated());
            System.out.println("   - Price: " + message.getPrice());
            System.out.println("   - Error Code: " + message.getErrorCode());
            System.out.println("   - Error Message: " + message.getErrorMessage());
            System.out.println();

            System.out.println("=== TEST TERMINÉ AVEC SUCCÈS ===");
            System.out.println("Vérifiez votre téléphone dans 30 secondes !");
            System.out.println("Dashboard Twilio: https://console.twilio.com/us1/monitor/logs/sms");

        } catch (Exception e) {
            System.err.println();
            System.err.println("=== ERREUR DÉTECTÉE ===");
            System.err.println("Type: " + e.getClass().getName());
            System.err.println("Message: " + e.getMessage());
            System.err.println();
            System.err.println("Stack trace complète:");
            e.printStackTrace();
            System.err.println();

            // Interpréter l'erreur
            String errorMsg = e.getMessage();
            if (errorMsg != null) {
                System.err.println("=== DIAGNOSTIC ===");

                if (errorMsg.contains("21608")) {
                    System.err.println("❌ ERREUR 21608: Numéro non vérifié");
                    System.err.println("   Le numéro " + TO_PHONE + " n'est pas vérifié dans votre compte Twilio Trial.");
                    System.err.println("   Solution: https://console.twilio.com/us1/develop/phone-numbers/manage/verified");

                } else if (errorMsg.contains("21606")) {
                    System.err.println("❌ ERREUR 21606: Numéro From invalide");
                    System.err.println("   Le numéro " + FROM_PHONE + " n'existe pas dans votre compte Twilio.");
                    System.err.println("   Solution: Vérifiez vos numéros actifs: https://console.twilio.com/us1/develop/phone-numbers/manage/incoming");

                } else if (errorMsg.contains("21614")) {
                    System.err.println("❌ ERREUR 21614: Numéro To invalide");
                    System.err.println("   Le numéro " + TO_PHONE + " n'est pas un numéro de téléphone valide.");
                    System.err.println("   Vérifiez le format (doit commencer par +)");

                } else if (errorMsg.contains("20003")) {
                    System.err.println("❌ ERREUR 20003: Authentification échouée");
                    System.err.println("   Le Account SID ou Auth Token est incorrect.");
                    System.err.println("   Vérifiez: https://console.twilio.com/");

                } else if (errorMsg.contains("21610")) {
                    System.err.println("❌ ERREUR 21610: Message bloqué");
                    System.err.println("   Twilio a bloqué ce message (spam, contenu inapproprié, etc.)");

                } else {
                    System.err.println("❌ ERREUR INCONNUE");
                    System.err.println("   Consultez la documentation Twilio pour cette erreur.");
                }
            }
        }
    }
}

