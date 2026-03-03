package services.guser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AboutAIApiService {

    // Use a fast/cheap model; you can change later
    private static final String MODEL = "gemini-3-flash-preview"; // if this model name fails, switch to one listed in your console

    public String improveAbout(String currentBio) throws Exception {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing GEMINI_API_KEY env var.");
        }

        String prompt =
                "Improve this 'About' section for a professional profile.\n" +
                        "Rules:\n" +
                        "- Keep the same language as the input.\n" +
                        "- Keep it truthful; do not invent facts.\n" +
                        "- 2 to 4 short paragraphs.\n" +
                        "- Output ONLY the improved bio text.\n\n" +
                        "INPUT:\n" + currentBio;

        JSONObject payload = new JSONObject()
                .put("contents", new JSONArray()
                        .put(new JSONObject()
                                .put("parts", new JSONArray()
                                        .put(new JSONObject().put("text", prompt))
                                )
                        )
                );

        // Gemini generateContent endpoint (Gemini API)
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        String body = res.body() == null ? "" : res.body().trim();

        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new IllegalStateException("Gemini HTTP " + res.statusCode() + " body=" + shorten(body));
        }

        // Parse: candidates[0].content.parts[0].text
        JSONObject json = new JSONObject(body);
        JSONArray candidates = json.optJSONArray("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("No candidates in response: " + shorten(body));
        }

        JSONObject cand0 = candidates.getJSONObject(0);
        JSONObject content = cand0.optJSONObject("content");
        if (content == null) throw new IllegalStateException("Missing content: " + shorten(body));

        JSONArray parts = content.optJSONArray("parts");
        if (parts == null || parts.isEmpty()) throw new IllegalStateException("Missing parts: " + shorten(body));

        String text = parts.getJSONObject(0).optString("text", "").trim();
        if (text.isBlank()) throw new IllegalStateException("Empty AI text: " + shorten(body));

        return text;
    }

    private static String shorten(String s) {
        if (s == null) return "";
        s = s.replace("\n", " ");
        return s.length() <= 180 ? s : s.substring(0, 180) + "...";
    }
}
