package services.goffre;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

/**
 * Service SMS SIMPLIFIÉ avec credentials en dur
 * À utiliser si config.properties ne fonctionne pas
 */
public class SimpleSMSService {

    // ⚠️ CREDENTIALS EN DUR (À RETIRER AVANT DE PUSHER SUR GIT)
    private static final String ACCOUNT_SID = "ACca107b73c59c0b4911e2b5595b716237";
    private static final String AUTH_TOKEN = "be552cc5d8c307c7329540484b221f67";
    private static final String FROM_PHONE = "+19853364277";

    private static SimpleSMSService instance;
    private boolean initialized = false;

    private SimpleSMSService() {
        init();
    }

    public static SimpleSMSService getInstance() {
        if (instance == null) {
            instance = new SimpleSMSService();
        }
        return instance;
    }

    private void init() {
        try {
            System.out.println("=== SIMPLE SMS SERVICE ===");
            System.out.println("Initialisation de Twilio...");
            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
            initialized = true;
            System.out.println("✓✓✓ TWILIO INITIALISÉ AVEC SUCCÈS ✓✓✓");
            System.out.println("==========================");
        } catch (Exception e) {
            System.err.println("❌ Erreur d'initialisation Twilio:");
            e.printStackTrace();
            initialized = false;
        }
    }

    public boolean envoyerSMS(String toPhone, String candidatNom, String offreTitre) {
        if (!initialized) {
            System.err.println("❌ Service SMS non initialisé!");
            return false;
        }

        try {
            System.out.println("📤 Envoi SMS...");
            System.out.println("   De: " + FROM_PHONE);
            System.out.println("   À: " + toPhone);
            System.out.println("   Offre: " + offreTitre);

            String messageBody = String.format(
                    "Bonjour %s,\n\n" +
                            "Votre candidature pour l'offre '%s' a bien été envoyée ✓  " +
                            "Nous reviendrons vers vous prochainement.  " +
                            "Bonne chance!\n\n" +
                            "- Équipe Carrieri",
                candidatNom,
                offreTitre
            );

            Message message = Message.creator(
                new PhoneNumber(toPhone),
                new PhoneNumber(FROM_PHONE),
                messageBody
            ).create();

            System.out.println("✓ SMS ENVOYÉ! SID: " + message.getSid());
            System.out.println("   Statut: " + message.getStatus());
            return true;

        } catch (Exception e) {
            System.err.println("❌ ERREUR ENVOI SMS:");
            System.err.println("   Message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean isEnabled() {
        return initialized;
    }
}

