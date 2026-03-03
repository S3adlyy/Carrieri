package utils.getude;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigTraduction {
    private static Properties properties = new Properties();

    static {
        // ✅ CORRECTION: utiliser Class.getResourceAsStream() avec / au début
        try (InputStream input = ConfigTraduction.class.getResourceAsStream("/com/example/guser/getude/config.properties")) {
            if (input != null) {
                properties.load(input);
                System.out.println("✅ ConfigTraduction chargée");
            } else {
                System.err.println("❌ config.properties non trouvé pour Traduction");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static String getTranslatorKey() {
        return get("translator.api.key");
    }

    public static String getTranslatorRegion() {
        return get("translator.api.region");
    }

    public static String getTranslatorEndpoint() {
        return get("translator.api.endpoint");
    }
}