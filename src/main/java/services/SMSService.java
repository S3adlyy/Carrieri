package services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SMSService {

    private static SMSService instance;
    private String accountSid;
    private String authToken;
    private String fromPhoneNumber;
    private boolean smsEnabled;
    private boolean initialized = false;

    private SMSService() {
        loadConfiguration();
    }

    public static SMSService getInstance() {
        if (instance == null) {
            instance = new SMSService();
        }
        return instance;
    }

    private void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.err.println("⚠ Fichier config.properties introuvable. SMS désactivé.");
                smsEnabled = false;
                return;
            }

            props.load(input);
            accountSid = props.getProperty("twilio.account.sid", "");
            authToken = props.getProperty("twilio.auth.token", "");
            fromPhoneNumber = props.getProperty("twilio.phone.number", "");
            smsEnabled = Boolean.parseBoolean(props.getProperty("sms.enabled", "false"));

            if (smsEnabled && !accountSid.isEmpty() && !authToken.isEmpty() && !fromPhoneNumber.isEmpty()) {
                // Vérifier que les credentials ne sont pas les valeurs par défaut
                if (accountSid.contains("YOUR_ACCOUNT_SID") || authToken.contains("YOUR_AUTH_TOKEN")) {
                    System.out.println("⚠ Configuration Twilio non complétée. SMS désactivé.");
                    smsEnabled = false;
                } else {
                    Twilio.init(accountSid, authToken);
                    initialized = true;
                    System.out.println("✓ Service SMS Twilio initialisé avec succès");
                }
            } else {
                System.out.println("ℹ Service SMS désactivé dans la configuration");
            }

        } catch (IOException e) {
            System.err.println(" Erreur lors du chargement de la configuration SMS: " + e.getMessage());
            smsEnabled = false;
        }
    }

    /**
     * Envoie un SMS de confirmation de candidature
     * @param toPhoneNumber Numéro du destinataire (format international: +33612345678)
     * @param candidatNom Nom du candidat
     * @param offreTitre Titre de l'offre
     * @return true si envoyé avec succès, false sinon
     */
    public boolean envoyerSMSConfirmationPostulation(String toPhoneNumber, String candidatNom, String offreTitre) {
        if (!smsEnabled || !initialized) {
            System.out.println("ℹ SMS non envoyé - Service désactivé ou non configuré");
            return false;
        }

        try {
            System.out.println("📤 Tentative d'envoi SMS...");
            System.out.println("   De: " + fromPhoneNumber);
            System.out.println("   À: " + toPhoneNumber);
            System.out.println("   Offre: " + offreTitre);

            String messageBody = String.format(
                    "Bonjour %s,\n\n" +
                            "Votre candidature pour l'offre '%s' a bien été envoyée ✓\n\n" +
                            "Nous reviendrons vers vous prochainement.\n" +
                            "Bonne chance!\n\n" +
                            "- Équipe Carrieri",
                    candidatNom,
                    offreTitre
            );

            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber),
                    new PhoneNumber(fromPhoneNumber),
                    messageBody
            ).create();

            System.out.println("✓ SMS envoyé avec succès - SID: " + message.getSid());
            System.out.println("   Statut: " + message.getStatus());
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi du SMS:");
            System.err.println("   Message: " + e.getMessage());
            System.err.println("   Type: " + e.getClass().getName());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Envoie un SMS personnalisé
     * @param toPhoneNumber Numéro du destinataire
     * @param messageBody Contenu du message
     * @return true si envoyé avec succès, false sinon
     */
    public boolean envoyerSMS(String toPhoneNumber, String messageBody) {
        if (!smsEnabled || !initialized) {
            System.out.println("ℹ SMS non envoyé - Service désactivé ou non configuré");
            return false;
        }

        try {
            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber),
                    new PhoneNumber(fromPhoneNumber),
                    messageBody
            ).create();

            System.out.println("✓ SMS envoyé avec succès - SID: " + message.getSid());
            return true;

        } catch (Exception e) {
            System.err.println(" Erreur lors de l'envoi du SMS: " + e.getMessage());
            return false;
        }
    }

    public boolean isEnabled() {
        return smsEnabled && initialized;
    }
}

