package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigTraduction {
    private static Properties properties = new Properties();

    static {
        try (InputStream input = ConfigTraduction.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
                System.out.println("✅ Configuration chargée");
            } else {
                System.err.println("❌ Fichier config.properties non trouvé");
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