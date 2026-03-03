package services.goffre;

import org.json.JSONObject;
import org.json.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Service pour générer des descriptions d'offres d'emploi avec l'IA Gemini
 * Appelle l'API Python Flask qui utilise Google Gemini
 */
public class AIGeneratorService {

    private static final String API_URL = "http://127.0.0.1:5001/generate";
    private static final String HEALTH_URL = "http://127.0.0.1:5001/health";

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Vérifie si le service IA est disponible
     * @return true si le service répond, false sinon
     */
    public static boolean isServiceAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HEALTH_URL))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            return response.statusCode() == 200;

        } catch (Exception e) {
            System.err.println("⚠️ Service IA non disponible : " + e.getMessage());
            return false;
        }
    }

    /**
     * Génère une description d'offre avec l'IA Gemini
     *
     * @param titre Titre du poste
     * @param secteur Secteur d'activité
     * @param competences Compétences requises (séparées par des virgules)
     * @param niveauEtudes Niveau d'études requis
     * @param experience Expérience requise
     * @return Description générée ou message d'erreur
     */
    public static String generateDescription(
            String titre,
            String secteur,
            String competences,
            String niveauEtudes,
            String experience
    ) {
        try {
            System.out.println("🤖 Génération IA pour : " + titre);

            // Créer le JSON de requête
            JSONObject requestJson = new JSONObject();
            requestJson.put("titre", titre);
            requestJson.put("secteur", secteur);
            requestJson.put("competences", competences);
            requestJson.put("niveauEtudes", niveauEtudes);
            requestJson.put("experience", experience);

            // Créer la requête HTTP
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson.toString()))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            // Envoyer la requête
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            // Parser la réponse
            JSONObject responseJson = new JSONObject(response.body());

            if (responseJson.getBoolean("success")) {
                String description = responseJson.getString("description");
                System.out.println("✅ Description générée avec succès");
                return description;
            } else {
                String error = responseJson.optString("error", "Erreur inconnue");
                System.err.println("❌ Erreur API : " + error);
                return "Erreur lors de la génération : " + error;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Erreur de connexion à l'API IA\n\n" +
                   "Le service de génération IA n'est pas disponible.\n" +
                   "Veuillez vérifier que le service Python est démarré :\n\n" +
                   "Commande : python api_service_gemini.py\n\n" +
                   "Détails : " + e.getMessage();
        }
    }

    /**
     * Génère une description avec valeurs par défaut pour niveau et expérience
     *
     * @param titre Titre du poste
     * @param secteur Secteur d'activité
     * @param competences Compétences requises
     * @return Description générée
     */
    public static String generateDescription(
            String titre,
            String secteur,
            String competences
    ) {
        return generateDescription(titre, secteur, competences, "Bac+3/5", "2-3 ans");
    }

    /**
     * Améliore un titre d'offre d'emploi pour le rendre plus attractif
     *
     * @param titreActuel Titre actuel à améliorer
     * @param secteur Secteur d'activité
     * @param typeContrat Type de contrat (CDI, CDD, etc.)
     * @param salaire Salaire proposé
     * @return Titre amélioré
     */
    public static String generateTitleImprovement(
            String titreActuel,
            String secteur,
            String typeContrat,
            String salaire
    ) {
        try {
            System.out.println("🔧 Amélioration du titre : " + titreActuel);

            JSONObject requestJson = new JSONObject();
            requestJson.put("titre", titreActuel);
            requestJson.put("secteur", secteur);
            requestJson.put("typeContrat", typeContrat);
            requestJson.put("salaire", salaire);
            requestJson.put("mode", "improve_title");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson.toString()))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            String body = response.body() == null ? "" : response.body().trim();

            // Log utile (tu peux le garder)
            System.out.println("HTTP " + response.statusCode());
            System.out.println("Content-Type: " + response.headers().firstValue("content-type").orElse("n/a"));
            System.out.println("Body (200 chars): " + body.substring(0, Math.min(200, body.length())));

            // IMPORTANT: si ce n'est pas 2xx, on ne parse pas en JSON
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "❌ Erreur HTTP " + response.statusCode()
                        + "\nURL: " + API_URL
                        + "\nContent-Type: " + response.headers().firstValue("content-type").orElse("n/a")
                        + "\n\nRéponse:\n" + body;
            }

            // Ici seulement on parse le JSON
            JSONObject responseJson = new JSONObject(body);

            if (responseJson.optBoolean("success", false)) {
                // Si ton API renvoie "title" au lieu de "description", remplace la ligne suivante
                String titreAmeliore = responseJson.optString("description", "").trim();
                if (titreAmeliore.isEmpty()) {
                    titreAmeliore = responseJson.optString("title", "").trim();
                }
                if (titreAmeliore.isEmpty()) {
                    return "❌ Réponse JSON OK mais champ 'description/title' manquant.\nBody:\n" + body;
                }

                System.out.println("✅ Titre amélioré : " + titreAmeliore);
                return titreAmeliore;
            } else {
                String error = responseJson.optString("error", "Erreur inconnue");
                System.err.println("❌ Erreur API : " + error);
                return "Erreur lors de l'amélioration : " + error;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Erreur de connexion à l'API IA\n\n"
                    + "Le service de génération IA n'est pas disponible.\n"
                    + "Veuillez vérifier que le service Python est démarré.\n\n"
                    + "Détails : " + e.getMessage();
        }
    }


    /**
     * Teste la génération avec un exemple
     * Utile pour vérifier que l'intégration fonctionne
     */
    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("🧪 TEST DU SERVICE IA GEMINI");
        System.out.println("=".repeat(80));

        // Vérifier la disponibilité
        System.out.println("\n1️⃣ Vérification du service...");
        if (!isServiceAvailable()) {
            System.err.println("❌ Le service IA n'est pas disponible !");
            System.err.println("💡 Lancez d'abord : python api_service_gemini.py");
            System.err.println("📝 Et configurez GEMINI_API_KEY");
            return;
        }
        System.out.println("✅ Service disponible");

        // Test de génération
        System.out.println("\n2️⃣ Test de génération...");
        String description = generateDescription(
                "Développeur Java",
                "Informatique",
                "Java, Spring Boot, MySQL",
                "Bac+5",
                "3 ans"
        );

        System.out.println("\n3️⃣ Résultat :");
        System.out.println("-".repeat(80));
        System.out.println(description);
        System.out.println("-".repeat(80));

        System.out.println("\n✅ Test terminé !");
    }
}

