package services;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import entities.Cours;
import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import utils.ConfigStripe;
import utils.MyDatabase;
import utils.AlertUtils;

import java.io.*;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PaiementService {

    private Connection connection;

    public PaiementService() {
        Stripe.apiKey = ConfigStripe.getSecretKey();
        this.connection = MyDatabase.getInstance().getConnection();
    }

    public String creerPaymentIntent(int montant, String titre, int coursId, int candidatId) throws Exception {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) (montant * 100))
                .setCurrency("eur")
                .putMetadata("cours_id", String.valueOf(coursId))
                .putMetadata("candidat_id", String.valueOf(candidatId))
                .putMetadata("titre", titre)
                .build();

        PaymentIntent intent = PaymentIntent.create(params);
        return intent.getClientSecret();
    }

    public void chargerPagePaiement(WebView webView, int montant, String titre,
                                    int coursId, int candidatId, Runnable onSuccess) {
        try {
            String clientSecret = creerPaymentIntent(montant, titre, coursId, candidatId);

            String htmlContent;
            try (InputStream inputStream = getClass().getResourceAsStream("/html/PaiementStripe.html")) {
                if (inputStream == null) {
                    throw new IOException("Fichier HTML non trouvé");
                }
                // ✅ FORCER L'ENCODAGE UTF-8
                htmlContent = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));
            }

            htmlContent = htmlContent.replace("{{PUBLISHABLE_KEY}}", ConfigStripe.getPublishableKey());
            htmlContent = htmlContent.replace("{{CLIENT_SECRET}}", clientSecret);

            WebEngine engine = webView.getEngine();

            // ✅ AJOUTER UNE INTERFACE JAVA POUR LE JAVASCRIPT
            JSObject window = (JSObject) engine.executeScript("window");
            window.setMember("java", new Object() {
                public void paiementReussi() {
                    System.out.println("✅ Callback JavaScript reçu !");
                    Platform.runLater(() -> {
                        try {
                            String paymentIntentId = clientSecret.split("_secret")[0];
                            confirmerPaiement(candidatId, coursId, paymentIntentId);
                            AlertUtils.showSuccess("✅ Paiement réussi",
                                    "Votre achat a été confirmé. Vous pouvez maintenant accéder au cours.");
                            onSuccess.run();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            });

            // ✅ GÉRER LE CALLBACK VIA URL (méthode de secours)
            engine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
                System.out.println("📍 Navigation: " + newUrl);
                if (newUrl != null && newUrl.contains("paiement-success")) {
                    Platform.runLater(() -> {
                        try {
                            String paymentIntentId = clientSecret.split("_secret")[0];
                            confirmerPaiement(candidatId, coursId, paymentIntentId);
                            AlertUtils.showSuccess("✅ Paiement réussi",
                                    "Votre achat a été confirmé. Vous pouvez maintenant accéder au cours.");
                            onSuccess.run();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            });

            engine.loadContent(htmlContent);

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() ->
                    AlertUtils.showError("❌ Erreur", "Impossible de charger la page de paiement.")
            );
        }
    }

    public void confirmerPaiement(int candidatId, int coursId, String paymentIntentId) {
        try {
            // Récupérer le montant
            CoursService coursService = new CoursService();
            Cours cours = coursService.getById(coursId);

            String sql = "INSERT INTO achat_cours (candidat_id, cours_id, montant, stripe_payment_intent_id, statut) VALUES (?, ?, ?, ?, 'PAYE')";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, candidatId);
            ps.setInt(2, coursId);
            ps.setDouble(3, cours.getPrix());
            ps.setString(4, paymentIntentId);
            ps.executeUpdate();

            System.out.println("✅ Paiement confirmé pour le cours " + coursId);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Integer> getCoursAchetes(int candidatId) throws SQLException {
        List<Integer> coursAchetes = new ArrayList<>();
        String sql = "SELECT cours_id FROM achat_cours WHERE candidat_id = ? AND statut = 'PAYE'";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, candidatId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    coursAchetes.add(rs.getInt("cours_id"));
                }
            }
        }
        return coursAchetes;
    }
}