package services.grecru;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import com.twilio.exception.ApiException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SMSService {

    private static SMSService instance;
    private String accountSid;
    private String authToken;
    private String fromPhoneNumber;
    private String recipientPhoneNumber;
    private boolean smsEnabled;
    private boolean initialized = false;

    private SMSService() {
        System.out.println("🔧 SMSService: Initializing...");
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

        // Try multiple ways to load the properties file
        boolean loaded = false;

        // Method 1: Try loading from classpath
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                props.load(input);
                loaded = true;
                System.out.println("✅ config.properties loaded from classpath");
            }
        } catch (IOException e) {
            System.err.println("❌ Error loading from classpath: " + e.getMessage());
        }

        // Method 2: Try loading from root of resources
        if (!loaded) {
            try (InputStream input = getClass().getResourceAsStream("/config.properties")) {
                if (input != null) {
                    props.load(input);
                    loaded = true;
                    System.out.println("✅ config.properties loaded from root");
                }
            } catch (IOException e) {
                System.err.println("❌ Error loading from root: " + e.getMessage());
            }
        }

        if (!loaded) {
            System.err.println("❌ CRITICAL: config.properties file not found anywhere!");
            System.err.println("   Please ensure the file exists at: src/main/resources/config.properties");
            smsEnabled = false;
            return;
        }

        // Load properties
        accountSid = props.getProperty("twilio.account.sid", "").trim();
        authToken = props.getProperty("twilio.auth.token", "").trim();
        fromPhoneNumber = props.getProperty("twilio.phone.number", "").trim();
        recipientPhoneNumber = props.getProperty("recipient.phone.number", "+21693039271").trim();
        smsEnabled = Boolean.parseBoolean(props.getProperty("sms.enabled", "false").trim());

        // Print loaded configuration (hiding full auth token)
        System.out.println("📋 Configuration loaded:");
        System.out.println("   - Account SID: " + (accountSid.isEmpty() ? "❌ MISSING" : "✅ " + accountSid.substring(0, Math.min(10, accountSid.length())) + "..."));
        System.out.println("   - Auth Token: " + (authToken.isEmpty() ? "❌ MISSING" : "✅ " + authToken.substring(0, Math.min(4, authToken.length())) + "..." + authToken.substring(Math.max(0, authToken.length()-4))));
        System.out.println("   - From: " + (fromPhoneNumber.isEmpty() ? "❌ MISSING" : "✅ " + fromPhoneNumber));
        System.out.println("   - To: " + (recipientPhoneNumber.isEmpty() ? "❌ MISSING" : "✅ " + recipientPhoneNumber));
        System.out.println("   - Enabled: " + (smsEnabled ? "✅ true" : "❌ false"));

        // Validate configuration
        if (!smsEnabled) {
            System.out.println("⚠️ SMS is disabled in configuration (sms.enabled=false)");
            return;
        }

        if (accountSid.isEmpty() || authToken.isEmpty() || fromPhoneNumber.isEmpty()) {
            System.err.println("❌ Missing required Twilio configuration:");
            if (accountSid.isEmpty()) System.err.println("   - twilio.account.sid is missing");
            if (authToken.isEmpty()) System.err.println("   - twilio.auth.token is missing");
            if (fromPhoneNumber.isEmpty()) System.err.println("   - twilio.phone.number is missing");
            smsEnabled = false;
            return;
        }

        // Check for placeholder values
        if (accountSid.contains("YOUR_ACCOUNT_SID") || authToken.contains("YOUR_AUTH_TOKEN")) {
            System.err.println("❌ Using placeholder values! Please update with real credentials:");
            System.err.println("   - Account SID: " + accountSid);
            System.err.println("   - Auth Token: " + authToken);
            smsEnabled = false;
            return;
        }

        // Initialize Twilio
        try {
            Twilio.init(accountSid, authToken);
            initialized = true;
            System.out.println("✅ Twilio initialized successfully!");
            System.out.println("   📱 Will send from: " + fromPhoneNumber);
            System.out.println("   📱 Will send to: " + recipientPhoneNumber);
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize Twilio: " + e.getMessage());
            e.printStackTrace();
            smsEnabled = false;
            initialized = false;
        }
    }

    public boolean envoyerSMSConfirmationPostulation(String candidatNom, String missionInfo) {
        if (!smsEnabled || !initialized) {
            System.out.println("ℹ SMS not sent - Service disabled or not initialized");
            System.out.println("   smsEnabled: " + smsEnabled);
            System.out.println("   initialized: " + initialized);
            return false;
        }

        try {
            System.out.println("📤 Attempting to send SMS...");
            System.out.println("   From: " + fromPhoneNumber);
            System.out.println("   To: " + recipientPhoneNumber);
            System.out.println("   Message for: " + candidatNom + ", Mission: " + missionInfo);

            String messageBody = String.format(
                    "Bonjour %s,\n\n" +
                            "Votre candidature pour la mission %s a bien été envoyée ✓\n" +
                            "Nous reviendrons vers vous prochainement.\n\n" +
                            "Bonne chance!\n\n" +
                            "- Équipe Carrieri",
                    candidatNom,
                    missionInfo
            );

            Message message = Message.creator(
                    new PhoneNumber(recipientPhoneNumber),
                    new PhoneNumber(fromPhoneNumber),
                    messageBody
            ).create();

            System.out.println("✅ SMS sent successfully!");
            System.out.println("   SID: " + message.getSid());
            System.out.println("   Status: " + message.getStatus());
            return true;

        } catch (ApiException e) {
            System.err.println("❌ Twilio API Error:");
            System.err.println("   Code: " + e.getCode());
            System.err.println("   Message: " + e.getMessage());
            System.err.println("   More info: " + e.getMoreInfo());

            // Specific error handling
            if (e.getCode() == 21211) {
                System.err.println("   ❌ Invalid 'To' phone number: " + recipientPhoneNumber);
            } else if (e.getCode() == 21608) {
                System.err.println("   ❌ Unverified phone number. Trial accounts can only send to verified numbers.");
                System.err.println("   Please verify " + recipientPhoneNumber + " in your Twilio console.");
            } else if (e.getCode() == 21408) {
                System.err.println("   ❌ Permission to send an SMS has not been enabled for the region.");
            }
            return false;
        } catch (Exception e) {
            System.err.println("❌ Error sending SMS:");
            System.err.println("   Message: " + e.getMessage());
            System.err.println("   Type: " + e.getClass().getName());
            e.printStackTrace();
            return false;
        }
    }

    public boolean isEnabled() {
        return smsEnabled && initialized;
    }

    public String getFromPhoneNumber() {
        return fromPhoneNumber;
    }

    public String getRecipientPhoneNumber() {
        return recipientPhoneNumber;
    }

    /**
     * Envoie un SMS avec le résultat de l'évaluation (score) - pour TOUTES les soumissions
     * @param candidatNom Nom du candidat
     * @param missionId ID de la mission
     * @param score Score obtenu
     * @param accepted Si le code est accepté ou non
     * @return true si envoyé avec succès, false sinon
     */
    public boolean envoyerSMSResultat(String candidatNom, int missionId, int score, boolean accepted) {
        if (!smsEnabled || !initialized) {
            System.out.println("ℹ SMS non envoyé - Service désactivé ou non configuré");
            return false;
        }

        try {
            System.out.println("📤 Tentative d'envoi SMS avec résultat...");
            System.out.println("   De: " + fromPhoneNumber + " (Twilio Trial)");
            System.out.println("   À: " + recipientPhoneNumber);
            System.out.println("   Mission ID: " + missionId);
            System.out.println("   Score: " + score + "%");
            System.out.println("   Accepté: " + (accepted ? "Oui" : "Non"));

            String status = accepted ? "✓ ACCEPTÉE" : "✗ REJETÉE";
            String messageBody = String.format(
                    "Bonjour %s,\n\n" +
                            "Résultat de votre évaluation pour la mission #%d:\n" +
                            "━━━━━━━━━━━━━━━━━━━━━\n" +
                            "📊 Score: %d%%\n" +
                            "📌 Statut: %s\n" +
                            "━━━━━━━━━━━━━━━━━━━━━\n\n" +
                            "Merci d'avoir participé!\n\n" +
                            "- Équipe Carrieri\n" +
                            "(Sent from your Twilio trial account)",
                    candidatNom,
                    missionId,
                    score,
                    status
            );

            Message message = Message.creator(
                    new PhoneNumber(recipientPhoneNumber),
                    new PhoneNumber(fromPhoneNumber),
                    messageBody
            ).create();

            System.out.println("✓ SMS envoyé avec succès - SID: " + message.getSid());
            System.out.println("   Statut: " + message.getStatus());
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi du SMS:");
            System.err.println("   Message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}