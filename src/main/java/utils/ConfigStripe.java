package utils;

import java.io.InputStream;
import java.util.Properties;

public class ConfigStripe {
    private static Properties props = new Properties();
    private static String publishableKey;
    private static String secretKey;
    private static String currency;

    static {
        try (InputStream input = ConfigStripe.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                props.load(input);
                publishableKey = props.getProperty("stripe.api.key.pk", "");
                secretKey = props.getProperty("stripe.api.key.sk", "");
                currency = props.getProperty("stripe.currency", "eur");
            } else {
                System.err.println("⚠️ config.properties non trouvé");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getPublishableKey() { return publishableKey; }
    public static String getSecretKey() { return secretKey; }
    public static String getCurrency() { return currency; }
}