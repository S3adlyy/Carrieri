package services.getude;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import utils.getude.ConfigGroq;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ChatbotGroqService {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final String apiToken;
    private final String modelName;
    private final int maxTokens;
    private final double temperature;

    private String contexteCours = "";
    private String contexteModule = "";
    private String contexteLecon = "";

    public ChatbotGroqService() {
        // Charger depuis ConfigGroq
        this.apiToken = ConfigGroq.getGroqToken();
        this.modelName = ConfigGroq.getGroqModel();
        this.maxTokens = ConfigGroq.getMaxTokens();
        this.temperature = ConfigGroq.getTemperature();

        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        this.objectMapper = new ObjectMapper();

        if (apiToken == null || apiToken.isEmpty() || apiToken.startsWith("votre-")) {
            System.err.println("⚠️ Token Groq non configuré dans config.properties!");
            System.err.println("📝 Allez sur https://console.groq.com pour obtenir un token gratuit");
        } else {
            System.out.println("✅ Chatbot Groq initialisé avec modèle: " + modelName);
            System.out.println("⚡ Service 100% GRATUIT et RAPIDE");
        }
    }

    public void setContexte(String cours, String module, String lecon) {
        this.contexteCours = cours != null ? cours : "";
        this.contexteModule = module != null ? module : "";
        this.contexteLecon = lecon != null ? lecon : "";
    }

    public String poserQuestion(String question) throws IOException {
        if (apiToken == null || apiToken.isEmpty() || apiToken.startsWith("votre-")) {
            return "❌ Token Groq non configuré!\n\n" +
                    "Pour utiliser le chatbot gratuitement:\n" +
                    "1. Allez sur https://console.groq.com\n" +
                    "2. Créez un compte\n" +
                    "3. Générez une clé API\n" +
                    "4. Mettez la clé dans config.properties\n\n" +
                    "En attendant, voici une réponse basique:\n" +
                    fallbackLocal(question);
        }

        try {
            // Construire le prompt système
            String systemPrompt = construireSystemPrompt();

            // Créer la requête au format exact attendu par Groq
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", modelName);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("top_p", 1.0);
            requestBody.put("stream", false);

            // Créer le tableau de messages
            ArrayNode messages = objectMapper.createArrayNode();

            // Message système
            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);

            // Message utilisateur
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", question);
            messages.add(userMessage);

            requestBody.set("messages", messages);

            // Afficher la requête pour debug
            String jsonRequest = objectMapper.writeValueAsString(requestBody);
            System.out.println("📤 Requête: " + jsonRequest);

            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(jsonRequest, MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    System.err.println("❌ Erreur Groq: " + response.code());
                    System.err.println("📥 Réponse: " + responseBody);
                    return "Désolé, erreur de communication. Mode hors-ligne activé:\n\n" + fallbackLocal(question);
                }

                JsonNode json = objectMapper.readTree(responseBody);

                if (json.has("choices") && json.get("choices").size() > 0) {
                    String reponse = json.get("choices").get(0).get("message").get("content").asText();
                    return reponse;
                } else {
                    return fallbackLocal(question);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Exception: " + e.getMessage());
            e.printStackTrace();
            return fallbackLocal(question);
        }
    }

    private String construireSystemPrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Tu es un assistant pédagogique expert en programmation Java et JavaFX. ");
        prompt.append("Tu réponds de façon claire, structurée et pédagogique. ");
        prompt.append("Tu utilises des exemples de code Java quand c'est pertinent. ");
        prompt.append("Tu es amical et encourageant avec les étudiants.\n\n");

        if (!contexteCours.isEmpty() || !contexteModule.isEmpty() || !contexteLecon.isEmpty()) {
            prompt.append("Contexte actuel de l'étudiant:\n");
            if (!contexteCours.isEmpty()) {
                prompt.append("- Cours: ").append(contexteCours).append("\n");
            }
            if (!contexteModule.isEmpty()) {
                prompt.append("- Module: ").append(contexteModule).append("\n");
            }
            if (!contexteLecon.isEmpty()) {
                prompt.append("- Leçon: ").append(contexteLecon).append("\n");
            }
        }

        return prompt.toString();
    }

    private String fallbackLocal(String question) {
        question = question.toLowerCase();

        if (question.contains("bonjour") || question.contains("salut") || question.contains("hello")) {
            return "Bonjour ! 👋 Je suis votre assistant pédagogique. Je peux vous aider avec Java, JavaFX, la POO, etc.\n" +
                    "Que voulez-vous apprendre aujourd'hui ?";
        }

        if (question.contains("java") && (question.contains("c'est quoi") || question.contains("qu'est-ce"))) {
            return "**Java** est un langage de programmation orienté objet créé par Sun Microsystems en 1995.\n\n" +
                    "**Caractéristiques principales :**\n" +
                    "• Orienté objet\n" +
                    "• Multiplateforme (JVM)\n" +
                    "• Gestion automatique de la mémoire\n\n" +
                    "**Exemple simple :**\n" +
                    "```java\n" +
                    "public class HelloWorld {\n" +
                    "    public static void main(String[] args) {\n" +
                    "        System.out.println(\"Hello Java!\");\n" +
                    "    }\n" +
                    "}\n" +
                    "```";
        }

        if (question.contains("classe") && question.contains("objet")) {
            return "**Classe vs Objet :**\n\n" +
                    "• Une **classe** est un modèle (un plan de construction)\n" +
                    "• Un **objet** est une instance concrète de la classe\n\n" +
                    "**Exemple :**\n" +
                    "```java\n" +
                    "// La classe (le modèle)\n" +
                    "class Voiture {\n" +
                    "    String couleur;\n" +
                    "    int vitesse;\n" +
                    "    \n" +
                    "    void accelerer() {\n" +
                    "        vitesse++;\n" +
                    "    }\n" +
                    "}\n\n" +
                    "// Création d'objets (instances)\n" +
                    "Voiture maVoiture = new Voiture();  // Objet 1\n" +
                    "maVoiture.couleur = \"rouge\";\n\n" +
                    "Voiture taVoiture = new Voiture();  // Objet 2\n" +
                    "taVoiture.couleur = \"bleue\";\n" +
                    "```";
        }

        if (question.contains("javafx")) {
            return "**JavaFX** est une bibliothèque pour créer des interfaces graphiques modernes en Java.\n\n" +
                    "**Composants principaux :**\n" +
                    "• **Stage** : La fenêtre principale\n" +
                    "• **Scene** : La scène qui contient les éléments\n" +
                    "• **Node** : Les éléments graphiques (Button, Label, etc.)\n\n" +
                    "**Exemple minimal :**\n" +
                    "```java\n" +
                    "import javafx.application.Application;\n" +
                    "import javafx.scene.Scene;\n" +
                    "import javafx.scene.control.Button;\n" +
                    "import javafx.stage.Stage;\n\n" +
                    "public class MonApp extends Application {\n" +
                    "    @Override\n" +
                    "    public void start(Stage stage) {\n" +
                    "        Button btn = new Button(\"Cliquez-moi !\");\n" +
                    "        btn.setOnAction(e -> System.out.println(\"Bonjour!\"));\n" +
                    "        \n" +
                    "        Scene scene = new Scene(btn, 300, 200);\n" +
                    "        stage.setScene(scene);\n" +
                    "        stage.setTitle(\"Ma première app JavaFX\");\n" +
                    "        stage.show();\n" +
                    "    }\n" +
                    "    \n" +
                    "    public static void main(String[] args) {\n" +
                    "        launch(args);\n" +
                    "    }\n" +
                    "}\n" +
                    "```";
        }

        if (question.contains("merci")) {
            return "Avec plaisir ! 😊 N'hésitez pas si vous avez d'autres questions sur Java ou JavaFX.";
        }

        return "Je peux vous aider sur :\n" +
                "• **Java** (classes, objets, héritage, polymorphisme)\n" +
                "• **JavaFX** (interfaces graphiques)\n" +
                "• **Concepts POO** (encapsulation, abstraction, etc.)\n\n" +
                "Posez-moi une question précise, par exemple :\n" +
                "- \"C'est quoi une classe en Java ?\"\n" +
                "- \"Comment créer un bouton avec JavaFX ?\"\n" +
                "- \"Explique-moi l'héritage\"";
    }
}