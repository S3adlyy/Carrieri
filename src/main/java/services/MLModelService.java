package services;

import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Service Java pour communiquer avec l'API Python ML
 * Permet de générer des descriptions d'offres avec le modèle entraîné
 */
public class MLModelService {

    private static final String API_URL = "http://localhost:5000";
    private static final int TIMEOUT = 30000; // 30 secondes

    // Cache des résultats pour éviter de régénérer la même chose
    private Map<String, String> cache;
    private boolean apiAvailable;

    public MLModelService() {
        this.cache = new HashMap<>();
        this.apiAvailable = checkApiHealth();
    }

    /**
     * Vérifie si l'API Python est disponible
     */
    public boolean checkApiHealth() {
        try {
            URL url = new URL(API_URL + "/health");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            conn.disconnect();

            if (responseCode == 200) {
                System.out.println("✓ API ML disponible");
                return true;
            }
        } catch (Exception e) {
            System.out.println("⚠️ API ML non disponible: " + e.getMessage());
        }
        return false;
    }

    /**
     * Génère une description d'offre avec le modèle ML
     *
     * @param titre Titre du poste
     * @param secteur Secteur d'activité
     * @param competences Compétences requises
     * @return Description générée ou null si erreur
     */
    public String generateDescription(String titre, String secteur, String competences) {
        // Vérifier le cache
        String cacheKey = titre + "|" + secteur + "|" + competences;
        if (cache.containsKey(cacheKey)) {
            System.out.println("📦 Résultat depuis le cache");
            return cache.get(cacheKey);
        }

        // Vérifier si l'API est disponible
        if (!apiAvailable) {
            System.out.println("⚠️ API ML non disponible, utilisation du fallback");
            return generateDescriptionFallback(titre, secteur, competences);
        }

        try {
            System.out.println("🤖 Génération avec le modèle ML...");

            // Préparer la requête JSON
            JSONObject requestBody = new JSONObject();
            requestBody.put("titre", titre);
            requestBody.put("secteur", secteur != null ? secteur : "Informatique");
            requestBody.put("competences", competences != null ? competences : "");

            // Envoyer la requête POST
            URL url = new URL(API_URL + "/generate");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);
            conn.setDoOutput(true);

            // Écrire le body
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Lire la réponse
            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line.trim());
                    }
                }

                // Parser la réponse JSON
                JSONObject jsonResponse = new JSONObject(response.toString());

                if (jsonResponse.getBoolean("success")) {
                    String description = jsonResponse.getString("description");
                    double generationTime = jsonResponse.getDouble("generation_time");

                    System.out.println("✓ Description générée en " + generationTime + "s");

                    // Mettre en cache
                    cache.put(cacheKey, description);

                    return description;
                } else {
                    String error = jsonResponse.getString("error");
                    System.err.println("❌ Erreur API: " + error);
                    return generateDescriptionFallback(titre, secteur, competences);
                }
            } else {
                System.err.println("❌ Erreur HTTP: " + responseCode);
                return generateDescriptionFallback(titre, secteur, competences);
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'appel à l'API ML: " + e.getMessage());
            e.printStackTrace();
            return generateDescriptionFallback(titre, secteur, competences);
        }
    }

    /**
     * Génération de fallback en cas d'erreur de l'API
     * Utilise des templates locaux simples
     */
    private String generateDescriptionFallback(String titre, String secteur, String competences) {
        System.out.println("📝 Utilisation du template local (fallback)");

        StringBuilder desc = new StringBuilder();

        desc.append("Nous recherchons un(e) ").append(titre);
        desc.append(" pour rejoindre notre équipe");

        if (secteur != null && !secteur.isEmpty()) {
            desc.append(" ").append(secteur);
        }

        desc.append(".\n\n");

        desc.append("🎯 Missions principales :\n");
        desc.append("• Participer activement aux projets de l'entreprise\n");
        desc.append("• Collaborer avec les équipes techniques et fonctionnelles\n");
        desc.append("• Contribuer à l'amélioration continue des processus\n");
        desc.append("• Assurer la qualité et le respect des délais\n\n");

        if (competences != null && !competences.isEmpty()) {
            desc.append("💻 Compétences requises :\n");
            desc.append("• ").append(competences).append("\n\n");
        }

        desc.append("👤 Profil recherché :\n");
        desc.append("• Formation supérieure en lien avec le poste\n");
        desc.append("• Expérience pertinente dans le domaine\n");
        desc.append("• Excellentes capacités de communication\n");
        desc.append("• Autonomie, esprit d'équipe, rigueur\n");

        return desc.toString();
    }

    /**
     * Vider le cache
     */
    public void clearCache() {
        cache.clear();
        System.out.println("🗑️ Cache vidé");
    }

    /**
     * Obtenir la taille du cache
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * Tester la connexion à l'API
     */
    public boolean isApiAvailable() {
        return apiAvailable;
    }

    /**
     * Rafraîchir le statut de l'API
     */
    public void refreshApiStatus() {
        this.apiAvailable = checkApiHealth();
    }
}

