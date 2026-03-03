package services.getude;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import entities.getude.QuestionQuiz;
import entities.getude.QuestionTest;
import entities.getude.Reponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import utils.MyDatabase;
import utils.getude.ConfigTraduction;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TraductionService {

    private static final String API_KEY = ConfigTraduction.getTranslatorKey();
    private static final String REGION = ConfigTraduction.getTranslatorRegion();
    private static final String ENDPOINT = ConfigTraduction.getTranslatorEndpoint();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Connection connection;

    // Cache mémoire (plus rapide)
    private final Map<String, String> cacheMemoire = new ConcurrentHashMap<>();

    // Statistiques
    private int compteurCaracteres = 0;
    private static final int LIMITE_CARACTERES = 500_000; // Niveau gratuit

    public TraductionService() {
        this.connection = MyDatabase.getInstance().getConnection();
        verifierBaseDeDonnees();
        System.out.println("🌐 Service de traduction initialisé avec Microsoft Translator");
        System.out.println("📊 Quota mensuel: " + LIMITE_CARACTERES + " caractères");
    }

    /**
     * Vérifier que la table de cache existe
     */
    private void verifierBaseDeDonnees() {
        try {
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet rs = meta.getTables(null, null, "traductions", null);
            if (!rs.next()) {
                System.err.println("⚠️ Table 'traductions' manquante. Créez-la avec le script SQL.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Traduire un texte
     */
    public String traduire(String texte, String langueCible) {
        if (texte == null || texte.isEmpty()) {
            return texte;
        }

        // Ne pas traduire si c'est trop court
        if (texte.length() < 10) {
            return texte;
        }

        // Vérifier le cache mémoire
        String cleCache = texte.hashCode() + "_" + langueCible;
        if (cacheMemoire.containsKey(cleCache)) {
            System.out.println("✅ Cache mémoire: " + texte.substring(0, Math.min(20, texte.length())) + "...");
            return cacheMemoire.get(cleCache);
        }

        // Vérifier le cache base de données
        try {
            String cacheBD = getFromCacheBD(texte, langueCible);
            if (cacheBD != null) {
                cacheMemoire.put(cleCache, cacheBD);
                System.out.println("✅ Cache BD: " + texte.substring(0, Math.min(20, texte.length())) + "...");
                return cacheBD;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Vérifier le quota
        if (compteurCaracteres + texte.length() > LIMITE_CARACTERES) {
            System.err.println("⚠️ Quota mensuel atteint (" + compteurCaracteres + "/" + LIMITE_CARACTERES + ")");
            return texte;
        }

        // Appeler l'API
        try {
            System.out.println("🔄 Appel API Microsoft Translator: " + texte.length() + " caractères");
            String traduction = appelerApiMicrosoft(texte, langueCible);

            // Mettre en cache
            cacheMemoire.put(cleCache, traduction);
            sauvegarderEnBase(texte, langueCible, traduction);

            // Mettre à jour le compteur
            compteurCaracteres += texte.length();
            System.out.println("📊 Caractères utilisés: " + compteurCaracteres + "/" + LIMITE_CARACTERES);

            return traduction;

        } catch (Exception e) {
            System.err.println("❌ Erreur API: " + e.getMessage());
            e.printStackTrace();
            return texte;
        }
    }

    /**
     * Appeler l'API Microsoft Translator
     */
    private String appelerApiMicrosoft(String texte, String langueCible) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {

            // Construire l'URL
            String url = ENDPOINT + "&to=" + langueCible + "&from=fr";

            HttpPost request = new HttpPost(url);
            request.setHeader("Ocp-Apim-Subscription-Key", API_KEY);
            request.setHeader("Ocp-Apim-Subscription-Region", REGION);
            request.setHeader("Content-Type", "application/json; charset=UTF-8");

            // Corps de la requête
            String body = "[{\"Text\":\"" + escapeJson(texte) + "\"}]";
            request.setEntity(new StringEntity(body, "UTF-8"));

            // Exécuter la requête
            long debut = System.currentTimeMillis();
            var response = client.execute(request);
            String json = EntityUtils.toString(response.getEntity());
            long fin = System.currentTimeMillis();

            System.out.println("⏱️ Temps de réponse: " + (fin - debut) + "ms");

            // Parser la réponse
            JsonNode node = objectMapper.readTree(json);
            String traduction = node.get(0).get("translations").get(0).get("text").asText();

            return traduction;
        }
    }

    /**
     * Échapper les caractères spéciaux pour JSON
     */
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Vérifier si une traduction existe en cache BD
     */
    private String getFromCacheBD(String texte, String langueCible) throws SQLException {
        // Utiliser une clé de hash pour éviter les problèmes de longueur
        String hash = Integer.toHexString(texte.hashCode());

        String sql = "SELECT traduction FROM traductions WHERE hash_original = ? AND langue_cible = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setString(2, langueCible);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("traduction");
            }
        }
        return null;
    }

    /**
     * Sauvegarder une traduction en base
     */
    private void sauvegarderEnBase(String texte, String langueCible, String traduction) {
        try {
            String hash = Integer.toHexString(texte.hashCode());

            // Limiter la taille pour éviter les débordements
            String texteTronque = texte.length() > 500 ? texte.substring(0, 500) : texte;
            String traductionTronque = traduction.length() > 500 ? traduction.substring(0, 500) : traduction;

            String sql = "INSERT INTO traductions (hash_original, texte_original, langue_cible, traduction) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, hash);
                ps.setString(2, texteTronque);
                ps.setString(3, langueCible);
                ps.setString(4, traductionTronque);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Traduire un lot de textes (optimisé)
     */
    public List<String> traduireLot(List<String> textes, String langueCible) {
        List<String> resultats = new ArrayList<>();

        for (String texte : textes) {
            resultats.add(traduire(texte, langueCible));
        }

        return resultats;
    }

    /**
     * Traduire un cours complet
     */
    public void traduireCoursComplet(entities.getude.Cours cours, String langueCible) {
        System.out.println("📚 Traduction du cours: " + cours.getTitre());

        cours.setTitre(traduire(cours.getTitre(), langueCible));
        cours.setDescription(traduire(cours.getDescription(), langueCible));
        cours.setCompetences_visees(traduire(cours.getCompetences_visees(), langueCible));
    }

    /**
     * Obtenir les statistiques d'utilisation
     */
    public String getStatistiques() {
        int pourcentage = (compteurCaracteres * 100) / LIMITE_CARACTERES;
        return String.format(
                "📈 API Microsoft Translator: %d / %d caractères (%d%%)",
                compteurCaracteres, LIMITE_CARACTERES, pourcentage
        );
    }

    /**
     * Obtenir la liste des langues supportées
     */
    public Map<String, String> getLanguesSupportees() {
        Map<String, String> langues = new LinkedHashMap<>();
        langues.put("fr", "Français");
        langues.put("en", "Anglais");
        langues.put("es", "Espagnol");
        langues.put("de", "Allemand");
        langues.put("it", "Italien");
        langues.put("pt", "Portugais");
        langues.put("nl", "Néerlandais");
        langues.put("ru", "Russe");
        langues.put("zh-Hans", "Chinois simplifié");
        langues.put("ja", "Japonais");
        langues.put("ko", "Coréen");
        langues.put("ar", "Arabe");
        langues.put("hi", "Hindi");
        langues.put("tr", "Turc");
        langues.put("pl", "Polonais");
        langues.put("sv", "Suédois");
        return langues;
    }

    /**
     * Obtenir le nom d'une langue
     */
    public String getNomLangue(String code) {
        return getLanguesSupportees().getOrDefault(code, code);
    }
    /**
     * Traduire une question de quiz
     */
    public QuestionQuiz traduireQuestionQuiz(QuestionQuiz question, String langueCible) {
        if (question == null) return null;

        // Traduire le texte de la question
        String texteTraduit = traduire(question.getQuestionText(), langueCible);

        // Créer une nouvelle question avec le texte traduit
        QuestionQuiz questionTraduite = new QuestionQuiz(
                question.getModuleId(),
                texteTraduit,
                question.getPoints(),
                question.getOrdre()
        );
        questionTraduite.setId(question.getId());

        // Traduire les réponses
        if (question.getReponses() != null) {
            List<Reponse> reponsesTraduites = new ArrayList<>();
            for (Reponse r : question.getReponses()) {
                Reponse reponseTraduite = new Reponse(
                        r.getQuestionId(),
                        r.getQuestionType(),
                        traduire(r.getReponseText(), langueCible),
                        r.isEstCorrecte(),
                        r.getOrdre()
                );
                reponseTraduite.setId(r.getId());
                reponsesTraduites.add(reponseTraduite);
            }
            questionTraduite.setReponses(reponsesTraduites);
        }

        return questionTraduite;
    }

    /**
     * Traduire toutes les questions d'un module
     */
    public List<QuestionQuiz> traduireQuestionsModule(List<QuestionQuiz> questions, String langueCible) {
        List<QuestionQuiz> questionsTraduites = new ArrayList<>();
        for (QuestionQuiz q : questions) {
            questionsTraduites.add(traduireQuestionQuiz(q, langueCible));
        }
        return questionsTraduites;
    }
    /**
     * Traduire une question de test final
     */
    public QuestionTest traduireQuestionTest(QuestionTest question, String langueCible) {
        if (question == null) return null;

        String texteTraduit = traduire(question.getQuestionText(), langueCible);

        QuestionTest questionTraduite = new QuestionTest(
                question.getCoursId(),
                texteTraduit,
                question.getPoints(),
                question.getOrdre()
        );
        questionTraduite.setId(question.getId());

        if (question.getReponses() != null) {
            List<Reponse> reponsesTraduites = new ArrayList<>();
            for (Reponse r : question.getReponses()) {
                Reponse reponseTraduite = new Reponse(
                        r.getQuestionId(),
                        r.getQuestionType(),
                        traduire(r.getReponseText(), langueCible),
                        r.isEstCorrecte(),
                        r.getOrdre()
                );
                reponseTraduite.setId(r.getId());
                reponsesTraduites.add(reponseTraduite);
            }
            questionTraduite.setReponses(reponsesTraduites);
        }

        return questionTraduite;
    }
}