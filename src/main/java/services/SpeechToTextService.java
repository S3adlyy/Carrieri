package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;

public class SpeechToTextService {

    private static final String API_KEY = ;
    private static final String API_URL = "https://api.assemblyai.com/v2";
    private final HttpClient client;
    private final ObjectMapper mapper;

    public SpeechToTextService() {
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
        System.out.println("✅ Service de transcription initialisé avec la clé: " + API_KEY.substring(0, 5) + "...");
    }

    /**
     * Transcrit un fichier audio avec détection automatique de la langue
     */
    public String transcribeAudio(File audioFile) {
        try {
            // Vérifier que le fichier existe
            if (!audioFile.exists()) {
                return "❌ Fichier audio introuvable";
            }

            long fileSize = audioFile.length();
            System.out.println("📤 Taille du fichier: " + fileSize + " octets (" + (fileSize/1024) + " Ko)");

            if (fileSize == 0) {
                return "❌ Fichier audio vide";
            }

            // Étape 1: Upload du fichier
            System.out.println("📤 Étape 1: Upload du fichier...");
            String uploadUrl = uploadFile(audioFile);
            System.out.println("✅ URL d'upload reçue: " + uploadUrl);

            // Étape 2: Demander la transcription avec détection automatique de la langue
            System.out.println("📝 Étape 2: Demande de transcription (détection auto de la langue)...");
            String transcriptId = requestTranscript(uploadUrl);
            System.out.println("✅ ID de transcription reçu: '" + transcriptId + "'");

            if (transcriptId == null || transcriptId.isEmpty() || transcriptId.length() < 5) {
                return "❌ ID de transcription invalide: " + transcriptId;
            }

            // Étape 3: Attendre le résultat
            System.out.println("⏳ Étape 3: Attente du résultat...");
            String result = waitForTranscript(transcriptId);

            return result;

        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
            return "Erreur de transcription: " + e.getMessage();
        }
    }

    private String uploadFile(File audioFile) throws IOException, InterruptedException {
        byte[] fileBytes = Files.readAllBytes(audioFile.toPath());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL + "/upload"))
            .header("authorization", API_KEY)
            .header("Content-Type", "application/octet-stream")
            .POST(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
            .build();

        System.out.println("📤 Envoi de la requête d'upload...");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("📥 Statut HTTP upload: " + response.statusCode());

        if (response.statusCode() != 200) {
            System.err.println("❌ Corps de la réponse: " + response.body());
            throw new IOException("Erreur upload: " + response.statusCode() + " - " + response.body());
        }

        JsonNode json = mapper.readTree(response.body());
        if (!json.has("upload_url")) {
            System.err.println("❌ Réponse inattendue: " + response.body());
            throw new IOException("Réponse upload invalide");
        }

        String uploadUrl = json.get("upload_url").asText();
        return uploadUrl;
    }

    private String requestTranscript(String audioUrl) throws IOException, InterruptedException {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("audio_url", audioUrl);

        // NE PAS spécifier language_code pour activer la détection automatique
        // payload.put("language_code", "fr"); // ← Commenté pour détection auto

        // Options pour améliorer la qualité
        payload.put("punctuate", true);
        payload.put("format_text", true);
        payload.put("language_detection", true); // Active explicitement la détection de langue

        // Ajouter le modèle requis
        ArrayNode speechModels = mapper.createArrayNode();
        speechModels.add("universal-2"); // Modèle universel supporte plusieurs langues
        payload.set("speech_models", speechModels);

        String jsonBody = mapper.writeValueAsString(payload);
        System.out.println("📝 Corps de la requête: " + jsonBody);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL + "/transcript"))
            .header("authorization", API_KEY)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        System.out.println("📝 Envoi de la demande de transcription...");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("📥 Statut HTTP transcription: " + response.statusCode());
        System.out.println("📥 Réponse brute: " + response.body());

        if (response.statusCode() != 200) {
            throw new IOException("Erreur transcription: " + response.statusCode() + " - " + response.body());
        }

        JsonNode json = mapper.readTree(response.body());

        // Vérifier s'il y a une erreur
        if (json.has("error")) {
            String error = json.get("error").asText();
            throw new IOException("Erreur API: " + error);
        }

        if (!json.has("id")) {
            System.err.println("❌ Réponse inattendue: " + response.body());
            throw new IOException("Réponse transcription invalide - pas d'ID");
        }

        String transcriptId = json.get("id").asText();
        return transcriptId;
    }

    private String waitForTranscript(String transcriptId) throws IOException, InterruptedException {
        String url = API_URL + "/transcript/" + transcriptId;
        int attempts = 0;
        int maxAttempts = 60; // 60 secondes max

        System.out.println("⏳ Début de l'attente pour l'ID: " + transcriptId);

        while (attempts < maxAttempts) {
            attempts++;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("authorization", API_KEY)
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("⚠️ Erreur HTTP " + response.statusCode() + " à la tentative " + attempts);
                System.err.println("📥 Corps: " + response.body());
                Thread.sleep(2000);
                continue;
            }

            JsonNode json = mapper.readTree(response.body());

            if (!json.has("status")) {
                System.err.println("❌ Pas de statut dans la réponse");
                Thread.sleep(2000);
                continue;
            }

            String status = json.get("status").asText();
            System.out.println("⏳ Statut: " + status + " (tentative " + attempts + "/" + maxAttempts + ")");

            switch (status) {
                case "completed":
                    if (!json.has("text")) {
                        return "✅ Transcription terminée mais texte vide";
                    }
                    String text = json.get("text").asText();

                    // Afficher la langue détectée si disponible
                    if (json.has("language_code")) {
                        String detectedLang = json.get("language_code").asText();
                        String langName = getLanguageName(detectedLang);
                        System.out.println("🌐 Langue détectée: " + langName + " (" + detectedLang + ")");
                    }

                    System.out.println("✅ Texte transcrit: \"" + text + "\"");
                    return text;

                case "error":
                    String error = json.has("error") ? json.get("error").asText() : "Erreur inconnue";
                    System.err.println("❌ Erreur de transcription: " + error);
                    return "❌ " + error;

                case "queued":
                    System.out.println("⏳ En attente dans la file...");
                    break;

                case "processing":
                    System.out.println("⚙️ Transcription en cours de traitement...");
                    break;

                default:
                    System.out.println("⚠️ Statut inconnu: " + status);
            }

            // Attendre 2 secondes avant de réessayer
            Thread.sleep(2000);
        }

        return "⏰ Délai d'attente dépassé (" + maxAttempts + " secondes)";
    }

    /**
     * Convertit un code de langue en nom lisible
     */
    private String getLanguageName(String code) {
        switch (code) {
            case "fr": return "Français";
            case "en": return "Anglais";
            case "ar": return "Arabe";
            case "es": return "Espagnol";
            case "de": return "Allemand";
            case "it": return "Italien";
            case "pt": return "Portugais";
            case "ru": return "Russe";
            case "zh": return "Chinois";
            case "ja": return "Japonais";
            case "nl": return "Néerlandais";
            case "pl": return "Polonais";
            default: return code;
        }
    }
}
