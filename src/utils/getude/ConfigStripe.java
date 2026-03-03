package utils.getude;

import java.io.InputStream;
import java.util.Properties;

public class ConfigStripe {
    private static Properties props = new Properties();
    private static String publishableKey;
    private static String secretKey;
    private static String currency;

    static {
        // ✅ CORRECTION: utiliser Class.getResourceAsStream() avec / au début
        try (InputStream input = ConfigStripe.class.getResourceAsStream("/com/example/guser/getude/config.properties")) {
            if (input != null) {
                props.load(input);
                publishableKey = props.getProperty("stripe.api.key.pk", "");
                secretKey = props.getProperty("stripe.api.key.sk", "");
                currency = props.getProperty("stripe.currency", "eur");
                System.out.println("✅ ConfigStripe chargée");
            } else {
                System.err.println("❌ config.properties non trouvé pour Stripe");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getPublishableKey() { return publishableKey; }
    public static String getSecretKey() { return secretKey; }
    public static String getCurrency() { return currency; }
}