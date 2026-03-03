package com.example.guser.controllers.getude;

import com.example.guser.controllers.guser.AppNavController;
import entities.getude.*;
import entities.getude.Module;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import com.example.guser.controllers.getude.CandidatShellController;
import com.example.guser.controllers.getude.LangueTest;
import com.example.guser.controllers.getude.Main;
import services.getude.*;
import session.SessionContext;
import utils.getude.AlertUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class CoursPlayerController {
    private TraductionService traductionService = new TraductionService();
    private String langueTest = "fr";
    private Map<Integer, Lecon> leconsTraduites = new HashMap<>();
    private Map<Integer, String> modulesTraduits = new HashMap<>();
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
    @FXML
    private Button btnAssistant;
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
    int me = SessionContext.getCurrentUser().getId();
    private int candidatId = me;
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

    public void setCours(Cours cours) {
        this.coursActuel = cours;

        // Récupérer la langue
        this.langueTest = LangueTest.getInstance().getLangue();

        // Traduire le titre du cours
        String titreTraduit = traductionService.traduire(cours.getTitre(), langueTest);
        lblCoursTitre.setText(titreTraduit);

        chargerModules();
        mettreAJourProgression();

        // Ajouter un écouteur pour les changements de thème
        Platform.runLater(() -> {
            Scene scene = lblCoursTitre.getScene();
            if (scene != null) {
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

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);

            // ✅ Vérifier si le module est accessible
            boolean moduleAccessible = isModuleAccessible(module, modules);

            // Traduire le titre du module
            String moduleTitreTraduit = traductionService.traduire(
                    "Module " + module.getOrdre() + " : " + module.getTitre(),
                    langueTest
            );

            // Label du module (toujours visible)
            Label moduleLabel = new Label(moduleTitreTraduit);
            moduleLabel.getStyleClass().add("module-label");

            // Si le module n'est pas accessible, ajouter un indicateur de verrouillage
            if (!moduleAccessible && i > 0) {
                moduleLabel.setText(moduleLabel.getText() + " 🔒");
                moduleLabel.setStyle("-fx-opacity: 0.7;");
            }
            boxModules.getChildren().add(moduleLabel);

            List<Lecon> lecons = leconService.getLeconsByModule(module.getId());
            lecons.sort(Comparator.comparingInt(Lecon::getOrdre));

            for (Lecon lecon : lecons) {
                String leconTitreTraduit = traductionService.traduire(lecon.getTitre(), langueTest);

                String texteBouton = "   " + module.getOrdre() + "." + lecon.getOrdre() + " " + leconTitreTraduit;

                if (progressionLeconService.isLeconTerminee(candidatId, lecon.getId())) {
                    texteBouton += " ✓";
                }

                Button btnLecon = new Button(texteBouton);
                btnLecon.setMaxWidth(Double.MAX_VALUE);
                btnLecon.setUserData(lecon);

                // ✅ Vérifier si la leçon est accessible (module accessible ET leçon accessible)
                boolean leconAccessible = moduleAccessible && isLeconAccessible(lecon, lecons);

                if (leconAccessible) {
                    // Leçon accessible → normal
                    btnLecon.setOnAction(e -> {
                        Lecon l = (Lecon) btnLecon.getUserData();
                        afficherLecon(l);
                    });
                    btnLecon.setStyle(null);
                } else {
                    // Leçon NON accessible → désactivée
                    btnLecon.setDisable(true);
                    btnLecon.setStyle("-fx-opacity: 0.4; -fx-background-color: #e0e0e0;");

                    String tooltipText = moduleAccessible ?
                            "Vous devez d'abord terminer les leçons précédentes" :
                            "Vous devez d'abord terminer le module précédent et son quiz";
                    btnLecon.setTooltip(new Tooltip(tooltipText));
                }

                boxModules.getChildren().add(btnLecon);
            }

            // ✅ Afficher le quiz seulement si le module est accessible
            if (moduleAccessible) {
                verifierQuizModule(module, modules);
            } else if (i > 0) {
                // Message indiquant que le module est verrouillé
                Label lblLocked = new Label("   🔒 Module verrouillé - Terminez le module précédent");
                lblLocked.setStyle("-fx-text-fill: #9ca3af; -fx-font-style: italic; -fx-padding: 5 0 5 20; -fx-font-size: 12px;");
                boxModules.getChildren().add(lblLocked);
            }
        }

        verifierEtatCours();
        mettreAJourProgression();
    }

    // ============================================
    // GESTION DES QUIZ DE MODULE
    // ============================================

    private void verifierQuizModule(Module module, List<Module> tousModules) {
        boolean toutesLeconsTerminees = isAllLeconsTerminees(module.getId());
        boolean moduleAccessible = isModuleAccessible(module, tousModules); // ← DÉJÀ UTILISÉ

        if (toutesLeconsTerminees && moduleAccessible) {
            // ... reste du code

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

                // ✅ Vérifier si le certificat peut être généré
                verifierEtGenererCertificat();
            }
        } else if (!toutesLeconsTerminees) {
            // Message indiquant qu'il faut terminer toutes les leçons
            Label lblInfo = new Label("   ⏳ Terminez toutes les leçons pour débloquer le quiz");
            lblInfo.setStyle("-fx-text-fill: #9ca3af; -fx-font-style: italic; -fx-padding: 5 0 5 20; -fx-font-size: 12px;");
            boxModules.getChildren().add(lblInfo);
        } else if (!moduleAccessible) {
            // Message indiquant que le module précédent doit être terminé
            Label lblInfo = new Label("   🔒 Terminez le module précédent pour débloquer celui-ci");
            lblInfo.setStyle("-fx-text-fill: #9ca3af; -fx-font-style: italic; -fx-padding: 5 0 5 20; -fx-font-size: 12px;");
            boxModules.getChildren().add(lblInfo);
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
        AppNavController.getInstance().etudeOpenQuiz(moduleId, titreModule);
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
            } else {
                // ✅ Vérifier si le certificat peut être généré
                verifierEtGenererCertificat();
                afficherSuccesCours();
            }
        }
    }

    private void afficherSuccesCours() {
        Label lblCoursReussi = new Label("   🎓 FÉLICITATIONS ! VOUS AVEZ RÉUSSI LE COURS !");
        lblCoursReussi.getStyleClass().add("label-cours-reussi");
        boxModules.getChildren().add(lblCoursReussi);

        // Vérifier si un certificat existe déjà (AVEC TRY-CATCH)
        try {
            Certification existing = certificationService.readByCoursAndCandidat(coursActuel.getId(), candidatId);

            if (existing != null) {
                Label lblCertificat = new Label("   📄 Certificat généré le " +
                        existing.getDateObtention().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                lblCertificat.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-padding: 5 0 5 20;");
                boxModules.getChildren().add(lblCertificat);
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Impossible de vérifier le certificat: " + e.getMessage());
            // On n'affiche rien en cas d'erreur
        }
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
        AppNavController.getInstance().etudeOpenTestFinal(coursActuel.getId());
    }

    // Ajoutez un bouton retour
    @FXML
    private void retourAuCatalogue() {
        AppNavController.getInstance().onEtudeCatalogue();
    }

    /**
     * Vérifie si une leçon est accessible (si toutes les leçons précédentes sont terminées)
     */
    private boolean isLeconAccessible(Lecon lecon, List<Lecon> toutesLecons) {
        // Trier les leçons par ordre
        List<Lecon> leconsTriees = toutesLecons.stream()
                .sorted((l1, l2) -> Integer.compare(l1.getOrdre(), l2.getOrdre()))
                .collect(Collectors.toList());

        // Trouver l'index de la leçon actuelle
        int indexActuel = -1;
        for (int i = 0; i < leconsTriees.size(); i++) {
            if (leconsTriees.get(i).getId() == lecon.getId()) {
                indexActuel = i;
                break;
            }
        }

        // Si c'est la première leçon (index 0), elle est accessible
        if (indexActuel == 0) return true;

        // Vérifier que toutes les leçons précédentes sont terminées
        for (int i = 0; i < indexActuel; i++) {
            Lecon leconPrecedente = leconsTriees.get(i);
            if (!progressionLeconService.isLeconTerminee(candidatId, leconPrecedente.getId())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Vérifie si un module est accessible (si le module précédent est terminé)
     */
    private boolean isModuleAccessible(Module module, List<Module> tousModules) {
        // Trier les modules par ordre
        List<Module> modulesTries = tousModules.stream()
                .sorted(Comparator.comparingInt(Module::getOrdre))
                .collect(Collectors.toList());

        // Trouver l'index du module actuel
        int indexActuel = -1;
        for (int i = 0; i < modulesTries.size(); i++) {
            if (modulesTries.get(i).getId() == module.getId()) {
                indexActuel = i;
                break;
            }
        }

        // Si c'est le premier module (index 0), il est accessible
        if (indexActuel == 0) return true;

        // Vérifier le module précédent
        Module modulePrecedent = modulesTries.get(indexActuel - 1);

        // 1. Toutes les leçons du module précédent doivent être terminées
        List<Lecon> leconsModulePrecedent = leconService.getLeconsByModule(modulePrecedent.getId());
        for (Lecon lecon : leconsModulePrecedent) {
            if (!progressionLeconService.isLeconTerminee(candidatId, lecon.getId())) {
                System.out.println("🔒 Module " + module.getOrdre() + " verrouillé : leçon " + lecon.getTitre() + " non terminée");
                return false;
            }
        }

        // 2. Le quiz du module précédent doit être réussi (s'il existe)
        if (quizModuleService.aDesQuestions(modulePrecedent.getId())) {
            if (!quizModuleService.isModuleReussi(candidatId, modulePrecedent.getId())) {
                System.out.println("🔒 Module " + module.getOrdre() + " verrouillé : quiz du module précédent non réussi");
                return false;
            }
        }

        System.out.println("✅ Module " + module.getOrdre() + " accessible");
        return true;
    }


    // ============================================
    // GESTION DES LEÇONS - SCROLL 100% FONCTIONNEL
    // ============================================

    private void afficherLecon(Lecon lecon) {
        arreterVerificationScroll();

        Lecon leconTraduite = traduireLecon(lecon);

        this.leconCourante = leconTraduite;
        this.dejaValidee = false;

        lblLeconTitre.setText(leconTraduite.getTitre());

        if (lecon.getVideo() != null && lecon.getVideo().length > 0) {
            afficherVideoLocale(leconTraduite);
        } else {
            afficherContenuWebViewAdaptatif(leconTraduite.getContenu());
        }

        if (progressionLeconService.isLeconTerminee(candidatId, lecon.getId())) {
            lblStatus.setText("✔ Déjà terminée");
            btnTerminer.setVisible(false);
            return;
        }

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
        boolean isDarkMode =false;

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

    private Lecon traduireLecon(Lecon lecon) {
        if (leconsTraduites.containsKey(lecon.getId())) {
            return leconsTraduites.get(lecon.getId());
        }

        Lecon leconTraduite = new Lecon(
                traductionService.traduire(lecon.getTitre(), langueTest),
                traductionService.traduire(lecon.getContenu(), langueTest),
                lecon.getVideo(),
                lecon.getOrdre(),
                lecon.getModuleId()
        );
        leconTraduite.setId(lecon.getId());

        leconsTraduites.put(lecon.getId(), leconTraduite);
        return leconTraduite;
    }

    private String traduireModule(String moduleTexte, int moduleId) {
        if (modulesTraduits.containsKey(moduleId)) {
            return modulesTraduits.get(moduleId);
        }

        String traduit = traductionService.traduire(moduleTexte, langueTest);
        modulesTraduits.put(moduleId, traduit);
        return traduit;
    }
    // ✅ Méthode pour vérifier et générer le certificat automatiquement
    private void verifierEtGenererCertificat() {
        if (coursActuel == null) return;

        try {
            // 1. Vérifier que tous les modules sont réussis
            boolean tousModulesReussis = isAllModulesReussis(coursActuel.getId());

            // 2. Vérifier que le test final est réussi
            boolean testReussi = testCoursService.isCoursReussi(candidatId, coursActuel.getId());

            // 3. Vérifier qu'un certificat n'existe pas déjà (AVEC TRY-CATCH)
            Certification existing = null;
            try {
                existing = certificationService.readByCoursAndCandidat(coursActuel.getId(), candidatId);
            } catch (SQLException e) {
                System.err.println("⚠️ Erreur vérification certificat existant: " + e.getMessage());
                // Si erreur, on considère qu'il n'existe pas (on continue)
            }

            if (tousModulesReussis && testReussi && existing == null) {
                // ✅ Toutes les conditions sont remplies, on génère le certificat
                genererCertificatAutomatique();
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur vérification certificat: " + e.getMessage());
        }
    }

    // ✅ Méthode pour générer le certificat automatiquement
    private void genererCertificatAutomatique() {
        try {
            String nomCandidat = SessionContext.getCurrentUser().getFirstname();
                    // À récupérer de votre système

            // Créer l'entrée en base
            Certification certif = new Certification(
                    candidatId,
                    coursActuel.getId(),
                    java.time.LocalDateTime.now()
            );

            int certificatId = certificationService.ajouterEtRetournerId(certif);

            if (certificatId > 0) {
                // Générer le PDF
                byte[] pdfBytes = genererPDFProfessionnel(nomCandidat, coursActuel.getTitre(), certificatId);

                // Stocker en base
                String sql = "UPDATE certification SET fichier_pdf = ? WHERE id = ?";
                try (java.sql.PreparedStatement ps = utils.MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
                    ps.setBytes(1, pdfBytes);
                    ps.setInt(2, certificatId);
                    ps.executeUpdate();
                    System.out.println("✅ PDF stocké en base pour le certificat ID: " + certificatId);
                }

                // ✅ ENVOYER L'EMAIL (avec un chemin virtuel car le PDF est en base)
                envoyerEmailNotification(nomCandidat, coursActuel.getTitre(), "certificat_" + certificatId + ".pdf");

                // Notifier l'utilisateur
                Platform.runLater(() -> {
                    AlertUtils.showSuccessWithInstructions(
                            "🎉 FÉLICITATIONS !",
                            "Vous avez terminé le cours \"" + coursActuel.getTitre() + "\" avec succès.",
                            "✅ Votre certificat a été généré automatiquement.\n" +
                                    "📧 Un email de confirmation a été envoyé.\n\n" +
                                    "Vous pouvez retrouver votre certificat dans la page 'Mes certificats'."
                    );

                    rechargerModules();
                });
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur génération certificat: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================
// ENVOI EMAIL DE NOTIFICATION
// ============================================
    private void envoyerEmailNotification(String nomCandidat, String titreCours, String cheminCertificat) {
        try {
            String emailDestinataire = SessionContext.getCurrentUser().getEmail(); // Votre email

            System.out.println("📧 Tentative d'envoi d'email depuis le catalogue...");

            EmailService emailService = new EmailService();

            boolean envoye = emailService.envoyerNotificationCompletionCours(
                    emailDestinataire,
                    nomCandidat,
                    titreCours,
                    "file:///" + cheminCertificat.replace("\\", "/")
            );

            if (envoye) {
                System.out.println("✅ Email envoyé avec succès depuis le catalogue");
            } else {
                System.err.println("❌ Échec envoi email depuis le catalogue");
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ Méthode pour recharger les modules (pour mettre à jour l'affichage)
    private void rechargerModules() {
        Platform.runLater(() -> {
            boxModules.getChildren().clear();
            chargerModules();
        });
    }
    private byte[] genererPDFProfessionnel(String nomCandidat, String titreCours, int certificatId) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(baos);
        com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);

        // Format A4 portrait (une seule page)
        pdf.setDefaultPageSize(com.itextpdf.kernel.geom.PageSize.A4);

        com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf);
        document.setMargins(40, 40, 40, 40);

        // Couleurs de Carrieri
        com.itextpdf.kernel.colors.Color violetFonce = new com.itextpdf.kernel.colors.DeviceRgb(35, 25, 66);  // #231942
        com.itextpdf.kernel.colors.Color violet = new com.itextpdf.kernel.colors.DeviceRgb(94, 84, 142);      // #5E548E
        com.itextpdf.kernel.colors.Color violetClair = new com.itextpdf.kernel.colors.DeviceRgb(159, 134, 192); // #9F86C0
        com.itextpdf.kernel.colors.Color rose = new com.itextpdf.kernel.colors.DeviceRgb(224, 177, 203);      // #E0B1CB

        // --- BORDURE DÉCORATIVE EN CADRE ---
        float[] borderWidths = {1};
        com.itextpdf.layout.element.Table borderTable = new com.itextpdf.layout.element.Table(borderWidths);
        borderTable.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));

        com.itextpdf.layout.element.Cell borderCell = new com.itextpdf.layout.element.Cell();
        borderCell.setBorder(new com.itextpdf.layout.borders.SolidBorder(violet, 2));
        borderCell.setPadding(20);

        // --- LOGO IMAGE (DEPUIS LES RESSOURCES) ---
        try {
            // Charger l'image depuis les ressources
            java.io.InputStream imageStream = getClass().getResourceAsStream("/images/logo.png");
            if (imageStream != null) {
                byte[] imageBytes = imageStream.readAllBytes();
                com.itextpdf.io.image.ImageData imageData = com.itextpdf.io.image.ImageDataFactory.create(imageBytes);
                com.itextpdf.layout.element.Image logo = new com.itextpdf.layout.element.Image(imageData);

                // Redimensionner le logo (hauteur 60, largeur automatique)
                logo.setHeight(60);
                logo.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                logo.setMarginTop(10);
                logo.setMarginBottom(5);

                borderCell.add(logo);
            } else {
                // Fallback: texte si l'image n'est pas trouvée
                com.itextpdf.layout.element.Paragraph fallbackLogo = new com.itextpdf.layout.element.Paragraph("Carrieri")
                        .setFontSize(48)
                        .setFontColor(violetFonce)
                        .setBold()
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                        .setMarginTop(10)
                        .setMarginBottom(5);
                borderCell.add(fallbackLogo);
                System.out.println("⚠️ Logo non trouvé, utilisation du texte par défaut");
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback en cas d'erreur
            com.itextpdf.layout.element.Paragraph fallbackLogo = new com.itextpdf.layout.element.Paragraph("Carrieri")
                    .setFontSize(48)
                    .setFontColor(violetFonce)
                    .setBold()
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setMarginTop(10)
                    .setMarginBottom(5);
            borderCell.add(fallbackLogo);
        }

        // --- TITRE PRINCIPAL ---
        com.itextpdf.layout.element.Paragraph titreCertif = new com.itextpdf.layout.element.Paragraph("CERTIFICAT DE RÉUSSITE")
                .setFontSize(28)
                .setFontColor(violet)
                .setBold()
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(20);
        borderCell.add(titreCertif);

        // --- LIGNE DÉCORATIVE ---
        com.itextpdf.layout.element.LineSeparator ligne = new com.itextpdf.layout.element.LineSeparator(
                new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(2f));
        ligne.setWidth(150);
        ligne.setMarginBottom(25);
        ligne.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
        borderCell.add(ligne);

        // --- TEXTE "DÉCERNÉ À" ---
        com.itextpdf.layout.element.Paragraph decerne = new com.itextpdf.layout.element.Paragraph("Ce certificat est décerné à")
                .setFontSize(14)
                .setFontColor(violetClair)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(5);
        borderCell.add(decerne);

        // --- NOM DU CANDIDAT (ENCADRÉ) ---
        com.itextpdf.layout.element.Paragraph nom = new com.itextpdf.layout.element.Paragraph(nomCandidat)
                .setFontSize(30)
                .setFontColor(violetFonce)
                .setBold()
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(250, 247, 252)) // Fond très léger
                .setPadding(12)
                .setMarginBottom(15);
        borderCell.add(nom);

        // --- TEXTE "POUR AVOIR COMPLÉTÉ" ---
        com.itextpdf.layout.element.Paragraph pour = new com.itextpdf.layout.element.Paragraph("pour avoir complété avec succès le cours")
                .setFontSize(14)
                .setFontColor(violetClair)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(5);
        borderCell.add(pour);

        // --- TITRE DU COURS (STYLISÉ) ---
        com.itextpdf.layout.element.Paragraph coursTitre = new com.itextpdf.layout.element.Paragraph(titreCours)
                .setFontSize(22)
                .setFontColor(violetFonce)
                .setBold()
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(25);
        borderCell.add(coursTitre);

        // --- MENTION ---
        com.itextpdf.layout.element.Paragraph mention = new com.itextpdf.layout.element.Paragraph("avec la mention ⭐ FÉLICITATIONS ⭐")
                .setFontSize(14)
                .setFontColor(rose)
                .setBold()
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(30);
        borderCell.add(mention);

        // --- INFORMATIONS (DATE ET NUMÉRO) ---
        float[] columnWidths = {1, 1};
        com.itextpdf.layout.element.Table infoTable = new com.itextpdf.layout.element.Table(columnWidths);
        infoTable.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(80));
        infoTable.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
        infoTable.setMarginBottom(25);

        // Date d'obtention
        com.itextpdf.layout.element.Cell dateCell = new com.itextpdf.layout.element.Cell();
        String dateFormatee = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        dateCell.add(new com.itextpdf.layout.element.Paragraph("Date").setFontColor(violet).setFontSize(12).setBold());
        dateCell.add(new com.itextpdf.layout.element.Paragraph(dateFormatee).setFontColor(violetFonce).setFontSize(14));
        dateCell.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
        dateCell.setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
        dateCell.setPadding(5);

        // Numéro de certificat
        com.itextpdf.layout.element.Cell numeroCell = new com.itextpdf.layout.element.Cell();
        numeroCell.add(new com.itextpdf.layout.element.Paragraph("N° certificat").setFontColor(violet).setFontSize(12).setBold());
        numeroCell.add(new com.itextpdf.layout.element.Paragraph("CERT-" + String.format("%04d", certificatId)).setFontColor(violetFonce).setFontSize(14));
        numeroCell.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
        numeroCell.setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
        numeroCell.setPadding(5);

        infoTable.addCell(dateCell);
        infoTable.addCell(numeroCell);

        borderCell.add(infoTable);

        // --- LIGNE DE SÉPARATION ---
        com.itextpdf.layout.element.LineSeparator ligneFine = new com.itextpdf.layout.element.LineSeparator(
                new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1f));
        ligneFine.setWidth(400);
        ligneFine.setMarginBottom(20);
        ligneFine.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
        borderCell.add(ligneFine);

        // --- SIGNATURE UNIQUE (BILAL EL ETER) ---
        com.itextpdf.layout.element.Paragraph signature = new com.itextpdf.layout.element.Paragraph("Carrieri Team")
                .setFontSize(18)
                .setFontColor(violetFonce)
                .setBold()
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(5);
        borderCell.add(signature);

        com.itextpdf.layout.element.Paragraph signatureTitre = new com.itextpdf.layout.element.Paragraph("Formateur & Directeur Pédagogique")
                .setFontSize(12)
                .setFontColor(violetClair)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(5);
        borderCell.add(signatureTitre);

        // --- PIED DE PAGE ---
        com.itextpdf.layout.element.Paragraph footer = new com.itextpdf.layout.element.Paragraph("Carrieri · www.carrieri.com")
                .setFontSize(10)
                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginTop(10);
        borderCell.add(footer);

        borderTable.addCell(borderCell);
        document.add(borderTable);
        document.close();

        return baos.toByteArray();
    }

}

