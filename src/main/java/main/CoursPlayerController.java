package main;

import entities.*;
import entities.Module;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import services.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class CoursPlayerController {

    @FXML private Label lblCoursTitre;
    @FXML private VBox boxModules;
    @FXML private Label lblLeconTitre;
    @FXML private WebView webViewContenu;
    @FXML private ProgressBar progressBar;
    @FXML private Label lblProgression;
    @FXML private Button btnTerminer;
    @FXML private Label lblStatus;

    // SERVICES
    private ModuleService moduleService = new ModuleService();
    private LeconService leconService = new LeconService();
    private ProgressionLeconService progressionLeconService = new ProgressionLeconService();
    private ProgressionCoursService progressionCoursService = new ProgressionCoursService();
    private QuizModuleService quizModuleService = new QuizModuleService();
    private TestCoursService testCoursService = new TestCoursService();
    private CertificationService certificationService = new CertificationService();

    private Cours coursActuel;
    private Lecon leconCourante;
    private int candidatId = 1;
    private boolean dejaValidee = false;
    private Timer scrollCheckTimer;
    private WebEngine webEngine;

    // ============================================
    // INITIALISATION
    // ============================================

    @FXML
    private void initialize() {
        System.out.println("DEBUG: CoursPlayerController initialisé");
        btnTerminer.setOnAction(e -> terminerLecon());
        configurerWebView();
    }

    private void configurerWebView() {
        webEngine = webViewContenu.getEngine();
        webEngine.setJavaScriptEnabled(true);
        setupDynamicWebViewHeight();
    }

    private void setupDynamicWebViewHeight() {
        webEngine.getLoadWorker().stateProperty().addListener(
                (ChangeListener<Worker.State>) (observable, oldValue, newValue) -> {
                    if (newValue == Worker.State.SUCCEEDED) {
                        ajusterHauteurWebView();
                        injecterDetectionScroll();
                    }
                }
        );
    }

    private void ajusterHauteurWebView() {
        Platform.runLater(() -> {
            try {
                Object result = webEngine.executeScript(
                        "(function() {" +
                                "    var body = document.body;" +
                                "    var html = document.documentElement;" +
                                "    return Math.max(" +
                                "        body.scrollHeight, body.offsetHeight," +
                                "        html.clientHeight, html.scrollHeight, html.offsetHeight" +
                                "    );" +
                                "})()"
                );

                if (result instanceof Number) {
                    int hauteur = ((Number) result).intValue();
                    int hauteurFinale = Math.min(1500, Math.max(400, hauteur + 50));
                    webViewContenu.setPrefHeight(hauteurFinale);
                }
            } catch (Exception e) {
                System.out.println("Erreur ajustement hauteur: " + e.getMessage());
            }
        });
    }

    public void setCours(Cours cours) {
        this.coursActuel = cours;
        lblCoursTitre.setText(cours.getTitre());
        chargerModules();
        mettreAJourProgression();
    }

    // ============================================
    // CHARGEMENT DES MODULES ET LEÇONS
    // ============================================

    private void chargerModules() {
        boxModules.getChildren().clear();
        List<Module> modules = moduleService.getModulesByCours(coursActuel.getId());

        for (Module module : modules) {
            Label moduleLabel = new Label("Module " + module.getOrdre() + " : " + module.getTitre());
            moduleLabel.setStyle("-fx-font-weight:bold; -fx-padding:5;");
            boxModules.getChildren().add(moduleLabel);

            List<Lecon> lecons = leconService.getLeconsByModule(module.getId());
            for (Lecon lecon : lecons) {
                String texteBouton = "   " + module.getOrdre() + "." + lecon.getOrdre() + " " + lecon.getTitre();
                if (progressionLeconService.isLeconTerminee(candidatId, lecon.getId())) {
                    texteBouton += " ✓";
                }

                Button btnLecon = new Button(texteBouton);
                btnLecon.setMaxWidth(Double.MAX_VALUE);
                btnLecon.setUserData(lecon);
                btnLecon.setOnAction(e -> {
                    Lecon l = (Lecon) btnLecon.getUserData();
                    afficherLecon(l);
                });
                boxModules.getChildren().add(btnLecon);
            }

            verifierQuizModule(module);
        }

        verifierEtatCours();
    }

    // ============================================
    // GESTION DES QUIZ DE MODULE
    // ============================================

    private void verifierQuizModule(Module module) {
        boolean toutesLeconsTerminees = isAllLeconsTerminees(module.getId());

        if (toutesLeconsTerminees) {
            boolean quizReussi = quizModuleService.isModuleReussi(candidatId, module.getId());

            if (!quizReussi) {
                Button btnQuiz = new Button("   📝 PASSER LE QUIZ DU MODULE");
                btnQuiz.setMaxWidth(Double.MAX_VALUE);
                btnQuiz.setStyle("-fx-background-color: #9F86C0; -fx-text-fill: white; -fx-font-weight: bold;");
                btnQuiz.setUserData(module);
                btnQuiz.setOnAction(e -> {
                    Module m = (Module) btnQuiz.getUserData();
                    lancerQuizModule(m.getId(), m.getTitre());
                });
                boxModules.getChildren().add(btnQuiz);
            } else {
                Label lblQuizReussi = new Label("   ✅ Quiz du module réussi");
                lblQuizReussi.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                boxModules.getChildren().add(lblQuizReussi);
            }
        }
    }

    private boolean isAllLeconsTerminees(int moduleId) {
        List<Lecon> lecons = leconService.getLeconsByModule(moduleId);
        for (Lecon l : lecons) {
            if (!progressionLeconService.isLeconTerminee(candidatId, l.getId())) {
                return false;
            }
        }
        return true;
    }

    private void lancerQuizModule(int moduleId, String titreModule) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/QuizModulePlayer.fxml"));
            Parent root = loader.load();
            QuizModulePlayerController controller = loader.getController();
            controller.setModuleId(moduleId, candidatId);
            Stage stage = new Stage();
            stage.setTitle("Quiz - " + titreModule);
            stage.setScene(new Scene(root));
            stage.setOnHiding(e -> {
                chargerModules();
                mettreAJourProgression();
            });
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("❌ Erreur", "Impossible de lancer le quiz");
        }
    }

    // ============================================
    // GESTION DU TEST FINAL
    // ============================================

    private void verifierEtatCours() {
        boolean tousModulesReussis = isAllModulesReussis(coursActuel.getId());

        if (tousModulesReussis) {
            boolean testReussi = testCoursService.isCoursReussi(candidatId, coursActuel.getId());

            if (!testReussi) {
                // Afficher bouton test final
                Button btnTestFinal = new Button("   🎯 PASSER LE TEST FINAL DU COURS");
                btnTestFinal.setMaxWidth(Double.MAX_VALUE);
                btnTestFinal.setStyle("-fx-background-color: #E0B1CB; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
                btnTestFinal.setOnAction(e -> lancerTestFinal());
                boxModules.getChildren().add(btnTestFinal);

                // ✅ Mettre à jour la progression pour montrer que le test manque
                mettreAJourProgression();

            } else {
                afficherSuccesCours();
            }
        }
    }

    private void afficherSuccesCours() {
        Label lblCoursReussi = new Label("   🎓 FÉLICITATIONS ! VOUS AVEZ RÉUSSI LE COURS !");
        lblCoursReussi.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 14px;");
        boxModules.getChildren().add(lblCoursReussi);
        Button btnCertificat = new Button("   📄 GÉNÉRER MON CERTIFICAT");
        btnCertificat.setMaxWidth(Double.MAX_VALUE);
        btnCertificat.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnCertificat.setOnAction(e -> genererCertificat());
        boxModules.getChildren().add(btnCertificat);
    }

    private boolean isAllModulesReussis(int coursId) {
        List<Module> modules = moduleService.getModulesByCours(coursId);

        for (Module m : modules) {
            // ✅ Vérifier si le module a des questions
            if (aDesQuestionsQuiz(m.getId())) {
                if (!quizModuleService.isModuleReussi(candidatId, m.getId())) {
                    return false;
                }
            }
            // ✅ Si pas de questions, le module est automatiquement réussi
        }
        return true;
    }

    // ✅ Vérifier si un module a des questions de quiz
    private boolean aDesQuestionsQuiz(int moduleId) {
        List<QuestionQuiz> questions = quizModuleService.getQuestionsByModule(moduleId);
        return questions != null && !questions.isEmpty();
    }

    private void lancerTestFinal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TestFinalPlayer.fxml"));
            Parent root = loader.load();
            TestFinalPlayerController controller = loader.getController();
            controller.setCoursId(coursActuel.getId(), candidatId);
            Stage stage = new Stage();
            stage.setTitle("Test final - " + coursActuel.getTitre());
            stage.setScene(new Scene(root));
            stage.setOnHiding(e -> {
                chargerModules();
                mettreAJourProgression();
                if (testCoursService.isCoursReussi(candidatId, coursActuel.getId())) {
                    try {
                        progressionCoursService.ajouterOuUpdate(candidatId, coursActuel.getId(), 100);
                        mettreAJourProgression();
                        showAlert("🎓 FÉLICITATIONS !",
                                "Vous avez réussi le test final du cours !\n\n" +
                                        "Vous pouvez maintenant générer votre certificat.");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("❌ Erreur", "Impossible de lancer le test final");
        }
    }

    // ============================================
    // GESTION DU CERTIFICAT
    // ============================================

    // ============================================
// GESTION DU CERTIFICAT - VERSION INVIOLABLE
// ============================================

    private void genererCertificat() {
        System.out.println("\n🟡=== GÉNÉRATION CERTIFICAT - VERSION INVIOLABLE ===🟡");

        // ✅ 1. Vérifier la progression (100%)
        double progression = progressionLeconService.getProgressionCours(candidatId, coursActuel.getId());
        if (progression < 99.9) {
            showAlert("⛔ PROGRESSION INCOMPLÈTE",
                    "Progression: " + String.format("%.0f%%", progression) + "\nTerminez toutes les leçons.");
            return;
        }

        // ✅ 2. Vérifier CHAQUE module - MÊME SANS QUESTIONS !
        List<Module> modules = moduleService.getModulesByCours(coursActuel.getId());
        List<String> modulesNonReussis = new ArrayList<>();

        System.out.println("\n📝 VÉRIFICATION MODULES:");

        for (Module m : modules) {
            // ✅ Vérification INDÉPENDANTE - toujours vérifier isModuleReussi()
            boolean reussi = quizModuleService.isModuleReussi(candidatId, m.getId());
            System.out.println("   Module " + m.getId() + " - " + m.getTitre() +
                    " | Réussi: " + (reussi ? "✅" : "❌"));

            if (!reussi) {
                modulesNonReussis.add("Module " + m.getOrdre() + " - " + m.getTitre());
            }
        }

        // ✅ 3. BLOQUER si des modules ne sont pas réussis
        if (!modulesNonReussis.isEmpty()) {
            String liste = String.join("\n• ", modulesNonReussis);
            showAlert("⛔ MODULES NON RÉUSSIS",
                    "Vous devez réussir CES modules :\n\n• " + liste);
            return;
        }
        System.out.println("✅ Tous les modules sont réussis !");

        // ✅ 4. Vérifier le test final - TOUJOURS vérifier !
        boolean testReussi = testCoursService.isCoursReussi(candidatId, coursActuel.getId());
        System.out.println("   Test final: " + (testReussi ? "✅" : "❌"));

        if (!testReussi) {
            showAlert("⛔ TEST FINAL NON RÉUSSI",
                    "Vous devez réussir le test final (70%).");
            return;
        }

        // ✅ 5. Vérifier le certificat existant
        try {
            Certification existing = certificationService.readByCoursAndCandidat(coursActuel.getId(), candidatId);
            if (existing != null) {
                String date = existing.getDateObtention()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                showAlert("ℹ️ CERTIFICAT EXISTANT", "Certificat déjà généré le " + date);
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // ✅ 6. TOUT EST BON - Générer le certificat
        try {
            String nomCandidat = "Bilal Eter";
            String date = java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            String chemin = "C:/Users/MSI/Desktop/certificat_" +
                    coursActuel.getTitre().replace(" ", "_") + "_" + date + ".pdf";

            certificationService.genererEtEnregistrer(
                    nomCandidat, coursActuel.getTitre(), chemin, candidatId, coursActuel.getId());

            showAlert("🎓 FÉLICITATIONS !",
                    "Certificat généré avec succès !\n" + chemin +
                            "\n\nFélicitations pour votre réussite ! 🎉");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("❌ ERREUR", e.getMessage());
        }
    }

    // ============================================
    // GESTION DES LEÇONS - SCROLL 100% FONCTIONNEL
    // ============================================

    private void afficherLecon(Lecon lecon) {
        arreterVerificationScroll();
        this.leconCourante = lecon;
        this.dejaValidee = false;

        lblLeconTitre.setText(lecon.getTitre());

        // ✅ Afficher le contenu (vidéo ou texte)
        if (lecon.getVideo() != null && lecon.getVideo().length > 0) {
            afficherVideoLocale(lecon);
        } else {
            afficherContenuWebViewAdaptatif(lecon.getContenu());
        }

        // ✅ Vérifier si déjà terminée
        if (progressionLeconService.isLeconTerminee(candidatId, lecon.getId())) {
            lblStatus.setText("✔ Déjà terminée");
            btnTerminer.setVisible(false);
            return;
        }

        // Toujours traiter comme une leçon classique
        btnTerminer.setVisible(false);
        lblStatus.setText("📖 Scrollez jusqu'en bas pour valider la leçon");
        demarrerVerificationScroll();
        System.out.println("✅ Détection de scroll activée pour: " + lecon.getTitre());
    }

    @FXML
    private void terminerLecon() {
        if (leconCourante == null) return;

        // Sans type, on valide directement la leçon
        validerLecon();
    }

    // ✅ Garder cette méthode pour la compatibilité
    private void marquerCommeTerminee() {
        validerLecon();
    }

    private void validerLecon() {
        if (leconCourante == null) {
            System.out.println("❌ Erreur: leconCourante est null");
            return;
        }

        // ✅ NE PAS vérifier dejaValidee ici - on vérifie directement la BD
        // ✅ On enlève le if (dejaValidee) car il bloque l'appel à la BD

        // ✅ Vérifier si déjà terminée en base
        if (progressionLeconService.isLeconTerminee(candidatId, leconCourante.getId())) {
            System.out.println("⚠️ Déjà terminée en base: " + leconCourante.getTitre());
            dejaValidee = true;
            mettreAJourBoutonLecon(leconCourante);
            return;
        }

        System.out.println("🎯 VALIDATION - Leçon terminée: " + leconCourante.getTitre());

        try {
            // ✅ 1. Marquer comme terminée dans la base de données
            progressionLeconService.marquerTerminee(candidatId, leconCourante.getId());
            System.out.println("✅ Base de données mise à jour");

            // ✅ 2. Vérifier que c'est bien enregistré
            boolean estTerminee = progressionLeconService.isLeconTerminee(candidatId, leconCourante.getId());
            System.out.println("✅ Vérification: leçon terminée = " + estTerminee);

            if (estTerminee) {
                // ✅ 3. Mettre à jour l'interface SEULEMENT si la BD a fonctionné
                dejaValidee = true;
                lblStatus.setText("✔ Terminée - Félicitations !");
                btnTerminer.setVisible(false);

                // ✅ 4. Mettre à jour la progression du cours
                mettreAJourProgression();

                // ✅ 5. Mettre à jour le bouton de la leçon (✓)
                mettreAJourBoutonLecon(leconCourante);

                // ✅ 6. Recharger les modules pour afficher les quiz
                Platform.runLater(() -> {
                    chargerModules();
                });

                System.out.println("✅ Leçon validée avec succès: " + leconCourante.getTitre());
            } else {
                System.err.println("❌ Échec de l'enregistrement en base");
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la validation: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // ============================================
    // DÉTECTION DE SCROLL - CORRIGÉE
    // ============================================

    private void demarrerVerificationScroll() {
        arreterVerificationScroll();
        scrollCheckTimer = new Timer(true);
        scrollCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> verifierPositionScroll());
            }
        }, 500, 200);
    }

    private void arreterVerificationScroll() {
        if (scrollCheckTimer != null) {
            scrollCheckTimer.cancel();
            scrollCheckTimer = null;
        }
    }

    private void verifierPositionScroll() {
        // ✅ Ne pas vérifier si pas de leçon
        if (leconCourante == null) {
            return;
        }

        // ✅ Vérifier directement en base si déjà terminée
        if (progressionLeconService.isLeconTerminee(candidatId, leconCourante.getId())) {
            // ✅ Mettre à jour l'interface pour refléter l'état réel
            Platform.runLater(() -> {
                lblStatus.setText("✔ Déjà terminée");
                btnTerminer.setVisible(false);
                mettreAJourBoutonLecon(leconCourante);
            });
            dejaValidee = true;
            arreterVerificationScroll();
            return;
        }

        try {
            // ✅ Vérification du scroll
            Object result = webEngine.executeScript(
                    "(function() {" +
                            "    var scrollTop = window.pageYOffset || document.documentElement.scrollTop;" +
                            "    var windowHeight = window.innerHeight || document.documentElement.clientHeight;" +
                            "    var documentHeight = Math.max(" +
                            "        document.body.scrollHeight, document.body.offsetHeight," +
                            "        document.documentElement.clientHeight, document.documentElement.scrollHeight," +
                            "        document.documentElement.offsetHeight);" +
                            "    return (scrollTop + windowHeight) >= documentHeight - 30;" +
                            "})()"
            );

            if (result instanceof Boolean && (Boolean) result) {
                System.out.println("📜 SCROLL DÉTECTÉ - Bas de page atteint pour: " + leconCourante.getTitre());
                arreterVerificationScroll();
                validerLecon(); // ✅ Appelle la méthode corrigée
            }
        } catch (Exception e) {
            // Ignorer les erreurs JavaScript
        }
    }

    private void injecterDetectionScroll() {
        // ✅ Marqueur de fin visible
        webEngine.executeScript(
                "console.log('✅ Détection de scroll injectée');"
        );
    }

    // ============================================
    // AFFICHAGE VIDÉO - AVEC MARQUEUR DE FIN
    // ============================================

    private void afficherVideoLocale(Lecon lecon) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("video_" + lecon.getId() + "_", ".mp4");
            tempFile.deleteOnExit();
            Files.write(tempFile.toPath(), lecon.getVideo());

            String videoPath = tempFile.toURI().toString();
            String contenu = lecon.getContenu();

            String html = "<!DOCTYPE html>" +
                    "<html><head><meta charset='UTF-8'>" +
                    "<style>" +
                    "body { font-family: Arial; padding: 20px; background: #f9f9f9; }" +
                    ".video-container { background: black; border-radius: 10px; overflow: hidden; margin-bottom: 30px; }" +
                    "video { width: 100%; max-height: 400px; }" +
                    ".content { background: white; padding: 30px; border-radius: 10px; }" +
                    "h2 { color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 10px; }" +
                    ".end-marker { " +
                    "    text-align: center; " +
                    "    color: #7f8c8d; " +
                    "    margin-top: 40px; " +
                    "    padding-top: 20px; " +
                    "    border-top: 2px dashed #bdc3c7; " +
                    "    font-weight: bold;" +
                    "}" +
                    "</style></head><body>" +
                    "<div class='video-container'><video controls preload='auto'>" +
                    "<source src='" + videoPath + "' type='video/mp4'></video></div>" +
                    "<div class='content'><h2>" + lecon.getTitre() + "</h2>" +
                    formatContenuPourHTMLAdaptatif(contenu) +
                    "<div class='end-marker'>★ FIN DE LA LEÇON ★</div></div>" +
                    "</body></html>";

            webEngine.loadContent(html);

        } catch (IOException e) {
            e.printStackTrace();
            afficherContenuWebViewAdaptatif(lecon.getContenu());
        }
    }

    // ============================================
    // AFFICHAGE TEXTE - AVEC MARQUEUR DE FIN
    // ============================================

    private void afficherContenuWebViewAdaptatif(String contenu) {
        int nombreLignes = contenu.split("\n").length;
        int hauteurEstimee = Math.min(2000, Math.max(400, nombreLignes * 25 + 100));

        String htmlContent = "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: 'Segoe UI'; font-size: 16px; line-height: 1.8; padding: 30px; background: #f9f9f9; }" +
                ".container { max-width: 900px; margin: 0 auto; background: white; padding: 40px; border-radius: 10px; }" +
                "h1 { color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 15px; }" +
                "p { margin-bottom: 20px; text-align: justify; }" +
                ".end-marker { " +
                "    text-align: center; " +
                "    color: #7f8c8d; " +
                "    margin-top: 50px; " +
                "    padding-top: 20px; " +
                "    border-top: 3px solid #3498db; " +
                "    font-size: 18px;" +
                "    font-weight: bold;" +
                "}" +
                "</style></head><body>" +
                "<div class='container'>" +
                "<h1>" + lblLeconTitre.getText() + "</h1>" +
                formatContenuPourHTMLAdaptatif(contenu) +
                "<div class='end-marker'>★ FIN DE LA LEÇON ★</div>" +
                "</div></body></html>";

        webEngine.loadContent(htmlContent);
    }

    private String formatContenuPourHTMLAdaptatif(String contenu) {
        if (contenu == null || contenu.isEmpty()) {
            return "<p>Aucun contenu disponible.</p>";
        }

        StringBuilder html = new StringBuilder();
        String[] paragraphes = contenu.split("\n\n");

        for (String p : paragraphes) {
            if (!p.trim().isEmpty()) {
                if (p.startsWith("Chapitre") || p.startsWith("CHAPITRE") || p.startsWith("Partie")) {
                    html.append("<div class='chapter'><div class='chapter-title'>")
                            .append(p.replace("\n", "<br>"))
                            .append("</div></div>");
                } else {
                    html.append("<p>").append(p.replace("\n", "<br>")).append("</p>");
                }
            }
        }
        return html.toString();
    }

    // ============================================
    // PROGRESSION
    // ============================================

    private void mettreAJourBoutonLecon(Lecon lecon) {
        for (javafx.scene.Node node : boxModules.getChildren()) {
            if (node instanceof Button) {
                Button btn = (Button) node;
                if (btn.getUserData() == lecon && !btn.getText().contains("✓")) {
                    btn.setText(btn.getText() + " ✓");
                    break;
                }
            }
        }
    }

    private void mettreAJourProgression() {
        // ✅ Utiliser la nouvelle méthode qui inclut quiz et test
        double prog = progressionLeconService.getProgressionCours(candidatId, coursActuel.getId());

        progressBar.setProgress(prog / 100);
        lblProgression.setText(String.format("%.0f%%", prog));

        try {
            progressionCoursService.ajouterOuUpdate(candidatId, coursActuel.getId(), (int) prog);
            System.out.println("📊 Progression enregistrée: " + prog + "%");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ============================================
    // UTILITAIRES
    // ============================================

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

