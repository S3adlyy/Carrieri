package utils.getude;

import java.io.InputStream;
import java.util.Properties;

public class ConfigGroq {
    private static final Properties props = new Properties();
    private static String groqToken = "";
    private static String groqModel = "llama3-8b-8192";
    private static int maxTokens = 1024;
    private static double temperature = 0.7;

    static {
        // ✅ CORRECTION : Utilisation de Class.getResourceAsStream() avec / au début
        String configPath = "/com/example/guser/getude/config.properties";

        try (InputStream input = ConfigGroq.class.getResourceAsStream(configPath)) {

            if (input != null) {
                props.load(input);
                groqToken = props.getProperty("groq.api.token", "");
                groqModel = props.getProperty("groq.api.model", "llama3-8b-8192");
                maxTokens = Integer.parseInt(props.getProperty("groq.api.max.tokens", "1024"));
                temperature = Double.parseDouble(props.getProperty("groq.api.temperature", "0.7"));

                System.out.println("✅ ConfigGroq chargée depuis: " + configPath);
                if (groqToken.isEmpty()) {
                    System.out.println("⚠️ Token Groq non configuré dans config.properties");
                } else {
                    System.out.println("✅ Token Groq configuré");
                }
            } else {
                System.err.println("❌ Fichier non trouvé: " + configPath);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement config: " + e.getMessage());
        }
    }

    public static String getGroqToken() { return groqToken; }
    public static String getGroqModel() { return groqModel; }
    public static int getMaxTokens() { return maxTokens; }
    public static double getTemperature() { return temperature; }
}