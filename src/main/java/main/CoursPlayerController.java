package main;

import entities.*;
import entities.Module;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import services.*;
import utils.AlertUtils;
import services.EmailService;
import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.*;

public class CoursPlayerController {

    @FXML
    private Label lblCoursTitre;
    @FXML
    private VBox boxModules;
    @FXML
    private Label lblLeconTitre;
    @FXML
    private WebView webViewContenu;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label lblProgression;
    @FXML
    private Button btnTerminer;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblProgressionValue;  // Pour l'affichage en bas

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

        // ✅ Initialiser la progression à 0
        if (progressBar != null) {
            progressBar.setProgress(0);
        }
        if (lblProgressionValue != null) {
            lblProgressionValue.setText("0%");
        }
    }


    // ✅ NOUVELLE MÉTHODE
    public void setCandidatId(int candidatId) {
        this.candidatId = candidatId;
        System.out.println("✅ Candidat ID défini: " + candidatId);
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
    private void rechargerContenuAvecTheme() {
        if (leconCourante != null) {
            System.out.println("🎨 Changement de thème détecté, rechargement du contenu...");
            afficherLecon(leconCourante);
        }
    }

    // Modifiez la méthode setCours existante
    public void setCours(Cours cours) {
        this.coursActuel = cours;
        lblCoursTitre.setText(cours.getTitre());
        chargerModules();
        mettreAJourProgression();

        // Ajouter un écouteur pour les changements de thème
        Platform.runLater(() -> {
            Scene scene = lblCoursTitre.getScene();
            if (scene != null) {
                // Utiliser ListChangeListener au lieu de ChangeListener
                scene.getStylesheets().addListener((ListChangeListener<String>) change -> {
                    rechargerContenuAvecTheme();
                });
            }
        });
    }

    // ============================================
    // CHARGEMENT DES MODULES ET LEÇONS
    // ============================================

    private void chargerModules() {
        boxModules.getChildren().clear();
        List<Module> modules = moduleService.getModulesByCours(coursActuel.getId());

        for (Module module : modules) {
            Label moduleLabel = new Label("Module " + module.getOrdre() + " : " + module.getTitre());
            moduleLabel.getStyleClass().add("module-label");
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
        mettreAJourProgression();
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
                btnQuiz.getStyleClass().add("btn-quiz-module");
                btnQuiz.setUserData(module);
                btnQuiz.setOnAction(e -> {
                    Module m = (Module) btnQuiz.getUserData();
                    lancerQuizModule(m.getId(), m.getTitre());
                });
                boxModules.getChildren().add(btnQuiz);
            } else {
                Label lblQuizReussi = new Label("   ✅ Quiz du module réussi");
                lblQuizReussi.getStyleClass().add("label-quiz-reussi");
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
        CandidatShellController.getInstance().openQuiz(moduleId, titreModule);
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
                btnTestFinal.getStyleClass().add("btn-test-final");
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
        lblCoursReussi.getStyleClass().add("label-cours-reussi");
        boxModules.getChildren().add(lblCoursReussi);
        Button btnCertificat = new Button("   📄 GÉNÉRER MON CERTIFICAT");
        btnCertificat.setMaxWidth(Double.MAX_VALUE);
        btnCertificat.getStyleClass().add("btn-certificat");
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
        CandidatShellController.getInstance().openTestFinal(coursActuel.getId());
    }

    // Ajoutez un bouton retour
    @FXML
    private void retourAuCatalogue() {
        CandidatShellController.getInstance().showCatalogue();
    }
    // ============================================
    // GESTION DU CERTIFICAT
    // ============================================

    // ============================================
// GESTION DU CERTIFICAT - VERSION INVIOLABLE
// ============================================

    private void genererCertificat() {
        System.out.println("\n🟡=== GÉNÉRATION CERTIFICAT - VERSION INVIOLABLE ===🟡");

        double progression = progressionLeconService.getProgressionCours(candidatId, coursActuel.getId());
        if (progression < 99.9) {
            AlertUtils.showWarning("⛔ PROGRESSION INCOMPLÈTE",
                    "Votre progression actuelle est de " + String.format("%.0f%%", progression) + ".\n\n" +
                            "Vous devez terminer toutes les leçons (100%) pour obtenir votre certificat.\n\n" +
                            "Continuez votre apprentissage !");
            return;
        }

        List<Module> modules = moduleService.getModulesByCours(coursActuel.getId());
        List<String> modulesNonReussis = new ArrayList<>();

        System.out.println("\n📝 VÉRIFICATION MODULES:");

        for (Module m : modules) {
            boolean reussi = quizModuleService.isModuleReussi(candidatId, m.getId());
            System.out.println("   Module " + m.getId() + " - " + m.getTitre() +
                    " | Réussi: " + (reussi ? "✅" : "❌"));

            if (!reussi) {
                modulesNonReussis.add("Module " + m.getOrdre() + " - " + m.getTitre());
            }
        }

        if (!modulesNonReussis.isEmpty()) {
            String liste = String.join("\n• ", modulesNonReussis);
            AlertUtils.showWarning("⛔ MODULES NON RÉUSSIS",
                    "Vous devez réussir les quiz des modules suivants :\n\n• " + liste + "\n\n" +
                            "Revenez après avoir obtenu au moins 70% à chaque quiz.");
            return;
        }
        System.out.println("✅ Tous les modules sont réussis !");

        boolean testReussi = testCoursService.isCoursReussi(candidatId, coursActuel.getId());
        System.out.println("   Test final: " + (testReussi ? "✅" : "❌"));

        if (!testReussi) {
            AlertUtils.showWarning("⛔ TEST FINAL NON RÉUSSI",
                    "Vous devez réussir le test final du cours (note minimum : 70%).\n\n" +
                            "Le test final est disponible dans la section ci-dessus.");
            return;
        }

        try {
            Certification existing = certificationService.readByCoursAndCandidat(coursActuel.getId(), candidatId);
            if (existing != null) {
                String date = existing.getDateObtention()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"));
                AlertUtils.showInfo("📄 Certificat déjà généré",
                        "Vous avez déjà généré un certificat pour ce cours le " + date + ".\n\n" +
                                "Félicitations pour votre réussite ! 🎉");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // ✅ Confirmation avant génération
        boolean confirmed = AlertUtils.showConfirmation(
                "🎓 Félicitations !",
                "Vous avez rempli toutes les conditions pour obtenir le certificat du cours \"" + coursActuel.getTitre() + "\" :\n\n" +
                        "✓ Toutes les leçons terminées (100%)\n" +
                        "✓ Tous les quiz de modules réussis\n" +
                        "✓ Test final réussi\n\n" +
                        "Voulez-vous générer votre certificat maintenant ?",
                "Oui, générer mon certificat",
                "Non, plus tard"
        );

        if (confirmed) {
            try {
                // Remplacez cette partie dans genererCertificat() :

                String nomCandidat = "Bilal El Eter";
                String date = java.time.LocalDate.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

// ✅ Créer le dossier s'il n'existe pas
                String dossierPath = "C:/Users/MSI/Desktop/certificats/"; // J'ai mis "certificats" au lieu de "certif"
                File dossier = new File(dossierPath);
                if (!dossier.exists()) {
                    dossier.mkdirs(); // Crée le dossier
                    System.out.println("✅ Dossier créé: " + dossierPath);
                }

                String chemin = dossierPath +
                        coursActuel.getTitre().replace(" ", "_") + "_" + date + ".pdf";

                certificationService.genererEtEnregistrer(
                        nomCandidat, coursActuel.getTitre(), chemin, candidatId, coursActuel.getId());

                // ✅ NOUVEAU : Envoyer l'email de notification
                envoyerEmailNotification(nomCandidat, coursActuel.getTitre(), chemin);

                AlertUtils.showSuccessWithInstructions(
                        "🎉 FÉLICITATIONS !!!",
                        "Votre certificat pour le cours \"" + coursActuel.getTitre() + "\" a été généré avec succès.",
                        "📁 Emplacement : " + chemin + "\n\n" +
                                "Un email de confirmation a été envoyé.\n\n" +
                                "Continuez votre parcours d'apprentissage ! 🚀"
                );

            } catch (Exception e) {
                e.printStackTrace();
                AlertUtils.showError("❌ Erreur", "Erreur lors de la génération du certificat:\n\n" + e.getMessage());
            }
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

    // ============================================
// AFFICHAGE VIDÉO - AVEC MARQUEUR DE FIN ET SUPPORT DU THÈME SOMBRE
// ============================================

    private void afficherVideoLocale(Lecon lecon) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("video_" + lecon.getId() + "_", ".mp4");
            tempFile.deleteOnExit();
            Files.write(tempFile.toPath(), lecon.getVideo());

            String videoPath = tempFile.toURI().toString();
            String contenu = lecon.getContenu();

            // ✅ Détecter si le mode sombre est actif
            boolean isDarkMode = Main.isDarkMode();

            String html;
            if (isDarkMode) {
                html = genererHtmlVideoSombre(lecon.getTitre(), videoPath, contenu);
            } else {
                html = genererHtmlVideoClair(lecon.getTitre(), videoPath, contenu);
            }

            webEngine.loadContent(html);

        } catch (IOException e) {
            e.printStackTrace();
            afficherContenuWebViewAdaptatif(lecon.getContenu());
        }
    }

// ============================================
// AFFICHAGE TEXTE - AVEC MARQUEUR DE FIN ET SUPPORT DU THÈME SOMBRE
// ============================================

    private void afficherContenuWebViewAdaptatif(String contenu) {
        // ✅ Détecter si le mode sombre est actif
        boolean isDarkMode = Main.isDarkMode();

        String htmlContent;
        if (isDarkMode) {
            htmlContent = genererHtmlTexteSombre(lblLeconTitre.getText(), contenu);
        } else {
            htmlContent = genererHtmlTexteClair(lblLeconTitre.getText(), contenu);
        }

        webEngine.loadContent(htmlContent);
    }

// ============================================
// GÉNÉRATION HTML - MODE CLAIR
// ============================================

    private String genererHtmlVideoClair(String titre, String videoPath, String contenu) {
        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: 'Segoe UI', Arial, sans-serif; padding: 20px; background: #f5f3f7; margin: 0; }" +
                ".video-container { background: #1a1a2e; border-radius: 16px; overflow: hidden; margin-bottom: 30px; box-shadow: 0 10px 25px rgba(94,84,142,0.15); border: 1px solid rgba(94,84,142,0.2); }" +
                "video { width: 100%; max-height: 400px; display: block; }" +
                ".content { background: white; padding: 30px; border-radius: 16px; box-shadow: 0 10px 25px rgba(94,84,142,0.1); border: 1px solid rgba(94,84,142,0.15); }" +
                "h2 { color: #231942; border-bottom: 3px solid #5E548E; padding-bottom: 15px; font-weight: 700; margin-top: 0; font-size: 24px; }" +
                "p { color: #2d2d44; line-height: 1.8; font-size: 16px; margin-bottom: 20px; text-align: justify; }" +
                ".chapter { background: #f8f4ff; padding: 15px; border-radius: 12px; margin: 20px 0; border-left: 5px solid #9F86C0; }" +
                ".chapter-title { color: #5E548E; font-weight: bold; font-size: 18px; margin-bottom: 10px; }" +
                ".end-marker { " +
                "    text-align: center; " +
                "    color: #6b7280; " +
                "    margin-top: 50px; " +
                "    padding-top: 25px; " +
                "    border-top: 3px dashed #9F86C0; " +
                "    font-weight: bold;" +
                "    font-size: 18px;" +
                "    letter-spacing: 2px;" +
                "}" +
                "ul, ol { color: #2d2d44; line-height: 1.8; }" +
                "li { margin-bottom: 8px; }" +
                "code { background: #f0f0f0; padding: 2px 6px; border-radius: 4px; font-family: monospace; }" +
                "pre { background: #f0f0f0; padding: 15px; border-radius: 8px; overflow-x: auto; }" +
                "</style></head><body>" +
                "<div class='video-container'><video controls preload='auto'>" +
                "<source src='" + videoPath + "' type='video/mp4'></video></div>" +
                "<div class='content'><h2>" + titre + "</h2>" +
                formatContenuPourHTMLAdaptatif(contenu) +
                "<div class='end-marker'>★ FIN DE LA LEÇON ★</div></div>" +
                "</body></html>";
    }

    private String genererHtmlTexteClair(String titre, String contenu) {
        int nombreLignes = contenu.split("\n").length;
        int hauteurEstimee = Math.min(2000, Math.max(400, nombreLignes * 25 + 100));

        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: 'Segoe UI', Arial, sans-serif; padding: 20px; background: #f5f3f7; margin: 0; }" +
                ".container { max-width: 900px; margin: 0 auto; background: white; padding: 40px; border-radius: 16px; box-shadow: 0 10px 25px rgba(94,84,142,0.1); border: 1px solid rgba(94,84,142,0.15); }" +
                "h1 { color: #231942; border-bottom: 3px solid #5E548E; padding-bottom: 15px; font-weight: 800; margin-top: 0; font-size: 28px; }" +
                "p { color: #2d2d44; line-height: 1.8; font-size: 16px; margin-bottom: 20px; text-align: justify; }" +
                ".chapter { background: #f8f4ff; padding: 15px; border-radius: 12px; margin: 20px 0; border-left: 5px solid #9F86C0; }" +
                ".chapter-title { color: #5E548E; font-weight: bold; font-size: 18px; margin-bottom: 10px; }" +
                ".end-marker { " +
                "    text-align: center; " +
                "    color: #6b7280; " +
                "    margin-top: 50px; " +
                "    padding-top: 25px; " +
                "    border-top: 3px dashed #9F86C0; " +
                "    font-weight: bold;" +
                "    font-size: 18px;" +
                "    letter-spacing: 2px;" +
                "}" +
                "ul, ol { color: #2d2d44; line-height: 1.8; }" +
                "li { margin-bottom: 8px; }" +
                "code { background: #f0f0f0; padding: 2px 6px; border-radius: 4px; font-family: monospace; }" +
                "pre { background: #f0f0f0; padding: 15px; border-radius: 8px; overflow-x: auto; }" +
                "</style></head><body>" +
                "<div class='container'>" +
                "<h1>" + titre + "</h1>" +
                formatContenuPourHTMLAdaptatif(contenu) +
                "<div class='end-marker'>★ FIN DE LA LEÇON ★</div>" +
                "</div></body></html>";
    }

// ============================================
// GÉNÉRATION HTML - MODE SOMBRE
// ============================================

    private String genererHtmlVideoSombre(String titre, String videoPath, String contenu) {
        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: 'Segoe UI', Arial, sans-serif; padding: 20px; background: #1a1a2e; margin: 0; }" +
                ".video-container { background: #0f0f1f; border-radius: 16px; overflow: hidden; margin-bottom: 30px; box-shadow: 0 10px 25px rgba(0,0,0,0.5); border: 1px solid #5E548E; }" +
                "video { width: 100%; max-height: 400px; display: block; }" +
                ".content { background: #2d2d44; padding: 30px; border-radius: 16px; box-shadow: 0 10px 25px rgba(0,0,0,0.5); border: 1px solid #5E548E; }" +
                "h2 { color: #E0B1CB; border-bottom: 3px solid #9F86C0; padding-bottom: 15px; font-weight: 700; margin-top: 0; font-size: 24px; }" +
                "p { color: #e0e0e0; line-height: 1.8; font-size: 16px; margin-bottom: 20px; text-align: justify; }" +
                ".chapter { background: #35354f; padding: 15px; border-radius: 12px; margin: 20px 0; border-left: 5px solid #E0B1CB; }" +
                ".chapter-title { color: #E0B1CB; font-weight: bold; font-size: 18px; margin-bottom: 10px; }" +
                ".end-marker { " +
                "    text-align: center; " +
                "    color: #9F86C0; " +
                "    margin-top: 50px; " +
                "    padding-top: 25px; " +
                "    border-top: 3px dashed #5E548E; " +
                "    font-weight: bold;" +
                "    font-size: 18px;" +
                "    letter-spacing: 2px;" +
                "}" +
                "ul, ol { color: #e0e0e0; line-height: 1.8; }" +
                "li { margin-bottom: 8px; }" +
                "code { background: #40405c; color: #E0B1CB; padding: 2px 6px; border-radius: 4px; font-family: monospace; }" +
                "pre { background: #40405c; color: #e0e0e0; padding: 15px; border-radius: 8px; overflow-x: auto; border: 1px solid #5E548E; }" +
                "a { color: #E0B1CB; }" +
                "a:hover { color: #f5b0d5; }" +
                "</style></head><body>" +
                "<div class='video-container'><video controls preload='auto'>" +
                "<source src='" + videoPath + "' type='video/mp4'></video></div>" +
                "<div class='content'><h2>" + titre + "</h2>" +
                formatContenuPourHTMLAdaptatif(contenu) +
                "<div class='end-marker'>★ FIN DE LA LEÇON ★</div></div>" +
                "</body></html>";
    }

    private String genererHtmlTexteSombre(String titre, String contenu) {
        int nombreLignes = contenu.split("\n").length;
        int hauteurEstimee = Math.min(2000, Math.max(400, nombreLignes * 25 + 100));

        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: 'Segoe UI', Arial, sans-serif; padding: 20px; background: #1a1a2e; margin: 0; }" +
                ".container { max-width: 900px; margin: 0 auto; background: #2d2d44; padding: 40px; border-radius: 16px; box-shadow: 0 10px 25px rgba(0,0,0,0.5); border: 1px solid #5E548E; }" +
                "h1 { color: #E0B1CB; border-bottom: 3px solid #9F86C0; padding-bottom: 15px; font-weight: 800; margin-top: 0; font-size: 28px; }" +
                "p { color: #e0e0e0; line-height: 1.8; font-size: 16px; margin-bottom: 20px; text-align: justify; }" +
                ".chapter { background: #35354f; padding: 15px; border-radius: 12px; margin: 20px 0; border-left: 5px solid #E0B1CB; }" +
                ".chapter-title { color: #E0B1CB; font-weight: bold; font-size: 18px; margin-bottom: 10px; }" +
                ".end-marker { " +
                "    text-align: center; " +
                "    color: #9F86C0; " +
                "    margin-top: 50px; " +
                "    padding-top: 25px; " +
                "    border-top: 3px dashed #5E548E; " +
                "    font-weight: bold;" +
                "    font-size: 18px;" +
                "    letter-spacing: 2px;" +
                "}" +
                "ul, ol { color: #e0e0e0; line-height: 1.8; }" +
                "li { margin-bottom: 8px; }" +
                "code { background: #40405c; color: #E0B1CB; padding: 2px 6px; border-radius: 4px; font-family: monospace; }" +
                "pre { background: #40405c; color: #e0e0e0; padding: 15px; border-radius: 8px; overflow-x: auto; border: 1px solid #5E548E; }" +
                "a { color: #E0B1CB; }" +
                "a:hover { color: #f5b0d5; }" +
                "</style></head><body>" +
                "<div class='container'>" +
                "<h1>" + titre + "</h1>" +
                formatContenuPourHTMLAdaptatif(contenu) +
                "<div class='end-marker'>★ FIN DE LA LEÇON ★</div>" +
                "</div></body></html>";
    }

// ============================================
// FORMATAGE DU CONTENU (inchangé)
// ============================================

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
        // ✅ Calculer la progression via le service
        double prog = progressionLeconService.getProgressionCours(candidatId, coursActuel.getId());

        // ✅ Arrondir pour l'affichage
        int progArrondie = (int) Math.round(prog);

        System.out.println("📊 Progression calculée: " + prog + "% -> Arrondie: " + progArrondie + "%");

        // ✅ Mettre à jour la barre de progression
        if (progressBar != null) {
            progressBar.setProgress(prog / 100);
        }

        // ✅ Mettre à jour le label en bas
        if (lblProgressionValue != null) {
            lblProgressionValue.setText(progArrondie + "%");
        }

        // ✅ Mettre à jour le label en haut si vous l'avez
        if (lblProgression != null) {
            lblProgression.setText(progArrondie + "%");
        }

        // ✅ Enregistrer en base
        try {
            progressionCoursService.ajouterOuUpdate(candidatId, coursActuel.getId(), progArrondie);
            System.out.println("✅ Progression enregistrée en base: " + progArrondie + "%");
        } catch (Exception e) {
            System.err.println("❌ Erreur enregistrement progression: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // ============================================
// ENVOI EMAIL DE NOTIFICATION
// ============================================
    // ============================================
// ENVOI EMAIL DE NOTIFICATION - VERSION CORRIGÉE
// ============================================
    private void envoyerEmailNotification(String nomCandidat, String titreCours, String cheminCertificat) {
        try {
            String emailDestinataire = "bilaleter05@gmail.com"; // Peut être n'importe quel email pour le test

            EmailService emailService = new EmailService();

            boolean envoye = emailService.envoyerNotificationCompletionCours(
                    emailDestinataire,
                    nomCandidat,
                    titreCours,
                    "file:///" + cheminCertificat.replace("\\", "/")
            );

            if (envoye) {
                System.out.println("✅ Notification email traitée");
                AlertUtils.showInfo("📧 Email simulé",
                        "L'email a été envoyé à Mailtrap.\n\n" +
                                "Connectez-vous sur mailtrap.io pour voir le message.");
            } else {
                AlertUtils.showWarning("⚠️ Erreur email",
                        "L'email n'a pas pu être envoyé. Vérifiez la console.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ============================================
    // UTILITAIRES
    // ============================================


}

