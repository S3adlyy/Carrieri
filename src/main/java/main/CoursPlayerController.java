package main;

import entities.Cours;
import entities.Lecon;
import entities.Module;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import services.LeconService;
import services.ModuleService;
import services.ProgressionCoursService;
import services.ProgressionLeconService;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class CoursPlayerController {

    @FXML private Label lblCoursTitre;
    @FXML private VBox boxModules;
    @FXML private Label lblLeconTitre;
    @FXML private WebView webViewContenu;
    @FXML private ProgressBar progressBar;
    @FXML private Label lblProgression;
    @FXML private Button btnTerminer;
    @FXML private Label lblStatus;

    private ModuleService moduleService = new ModuleService();
    private LeconService leconService = new LeconService();
    private ProgressionLeconService progressionLeconService = new ProgressionLeconService();
    private ProgressionCoursService progressionCoursService = new ProgressionCoursService();

    private Cours coursActuel;
    private Lecon leconCourante;
    private int candidatId = 1;
    private boolean dejaValidee = false;
    private Timer scrollCheckTimer;
    private WebEngine webEngine;

    @FXML
    private void initialize() {
        System.out.println("DEBUG: CoursPlayerController initialisé");
        btnTerminer.setOnAction(e -> terminerLecon());

        // Configurer le WebView
        configurerWebView();
    }

    private void configurerWebView() {
        webEngine = webViewContenu.getEngine();

        // Activer JavaScript
        webEngine.setJavaScriptEnabled(true);

        // Redimensionner dynamiquement le WebView
        setupDynamicWebViewHeight();

        // Démarrer la vérification du scroll
        demarrerVerificationScroll();
    }

    private void setupDynamicWebViewHeight() {
        // Quand le contenu est chargé, ajuster la hauteur
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
                // Obtenir la hauteur réelle du contenu
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
                    System.out.println("Hauteur contenu: " + hauteur + "px");

                    // Ajuster la hauteur du WebView (limiter à 1500px max)
                    int hauteurFinale = Math.min(1500, Math.max(400, hauteur + 50));
                    webViewContenu.setPrefHeight(hauteurFinale);
                    System.out.println("Hauteur WebView ajustée: " + hauteurFinale + "px");
                }
            } catch (Exception e) {
                System.out.println("Erreur ajustement hauteur: " + e.getMessage());
            }
        });
    }

    public void setCours(Cours cours) {
        this.coursActuel = cours;
        System.out.println("DEBUG: Cours défini - ID=" + cours.getId() + ", Titre=" + cours.getTitre());
        lblCoursTitre.setText(cours.getTitre());
        chargerModules();
        mettreAJourProgression();
    }

    private void chargerModules() {
        boxModules.getChildren().clear();
        List<Module> modules = moduleService.getModulesByCours(coursActuel.getId());
        System.out.println("DEBUG: " + modules.size() + " modules chargés");

        for (Module module : modules) {
            Label moduleLabel = new Label("Module " + module.getOrdre() + " : " + module.getTitre());
            moduleLabel.setStyle("-fx-font-weight:bold; -fx-padding:5;");
            boxModules.getChildren().add(moduleLabel);

            List<Lecon> lecons = leconService.getLeconsByModule(module.getId());
            System.out.println("DEBUG: Module " + module.getId() + " a " + lecons.size() + " leçons");

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
        }
    }

    private void afficherLecon(Lecon lecon) {
        // Arrêter le timer précédent
        arreterVerificationScroll();

        this.leconCourante = lecon;
        this.dejaValidee = false;

        System.out.println("DEBUG: Affichage leçon ID=" + lecon.getId() + ", Titre=" + lecon.getTitre());

        lblLeconTitre.setText(lecon.getTitre());

        // Afficher le contenu dans le WebView avec hauteur adaptative
        afficherContenuWebViewAdaptatif(lecon.getContenu());
        lblStatus.setText("");

        // Vérifier si déjà terminée
        if (progressionLeconService.isLeconTerminee(candidatId, lecon.getId())) {
            lblStatus.setText("✔ Déjà terminée");
            btnTerminer.setVisible(false);
            return;
        }

        String type = lecon.getType();
        System.out.println("DEBUG: Type de leçon = " + type);

        if (type != null && (type.equalsIgnoreCase("QUIZ") || type.equalsIgnoreCase("EXAM"))) {
            btnTerminer.setVisible(true);
            btnTerminer.setText(type.equalsIgnoreCase("QUIZ") ? "Passer le Quiz" : "Passer l'Examen");
        } else {
            btnTerminer.setVisible(false);
            // Redémarrer la vérification pour cette leçon
            demarrerVerificationScroll();
        }
    }

    private void afficherContenuWebViewAdaptatif(String contenu) {
        // Analyser la longueur du contenu
        int nombreMots = contenu.split("\\s+").length;
        int nombreLignes = contenu.split("\n").length;

        // Calculer une hauteur estimée (20px par ligne, min 400px, max 2000px)
        int hauteurEstimee = Math.min(2000, Math.max(400, nombreLignes * 25 + 100));

        System.out.println("Contenu: " + nombreMots + " mots, " + nombreLignes + " lignes → " + hauteurEstimee + "px");

        // Formater le contenu avec une hauteur flexible
        String htmlContent = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<style>" +
                "* {" +
                "    margin: 0;" +
                "    padding: 0;" +
                "    box-sizing: border-box;" +
                "}" +
                "body {" +
                "    font-family: 'Segoe UI', Arial, sans-serif;" +
                "    font-size: 16px;" +
                "    line-height: 1.8;" +
                "    color: #333;" +
                "    background-color: #f9f9f9;" +
                "    min-height: " + hauteurEstimee + "px;" +
                "    padding: 30px;" +
                "}" +
                ".content-container {" +
                "    max-width: 900px;" +
                "    margin: 0 auto;" +
                "    background-color: white;" +
                "    padding: 40px;" +
                "    border-radius: 10px;" +
                "    box-shadow: 0 2px 10px rgba(0,0,0,0.1);" +
                "    min-height: " + (hauteurEstimee - 100) + "px;" +
                "}" +
                "h1 {" +
                "    color: #2c3e50;" +
                "    margin-bottom: 30px;" +
                "    padding-bottom: 15px;" +
                "    border-bottom: 2px solid #3498db;" +
                "}" +
                "p {" +
                "    margin-bottom: 20px;" +
                "    text-align: justify;" +
                "}" +
                ".chapter {" +
                "    margin-top: 40px;" +
                "    padding-top: 20px;" +
                "    border-top: 1px dashed #ddd;" +
                "}" +
                ".chapter-title {" +
                "    color: #3498db;" +
                "    font-size: 1.3em;" +
                "    margin-bottom: 15px;" +
                "}" +
                ".end-marker {" +
                "    text-align: center;" +
                "    color: #7f8c8d;" +
                "    font-style: italic;" +
                "    margin-top: 50px;" +
                "    padding-top: 20px;" +
                "    border-top: 3px solid #3498db;" +
                "}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"content-container\">" +
                "<h1>" + lblLeconTitre.getText() + "</h1>" +
                formatContenuPourHTMLAdaptatif(contenu) +
                "<div class=\"end-marker\">" +
                "★ Fin du contenu ★<br>" +
                "<small>Vous avez atteint la fin de cette leçon</small>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        webEngine.loadContent(htmlContent);
    }

    private String formatContenuPourHTMLAdaptatif(String contenu) {
        if (contenu == null || contenu.isEmpty()) {
            return "<p>Aucun contenu disponible pour cette leçon.</p>";
        }

        StringBuilder html = new StringBuilder();
        String[] paragraphes = contenu.split("\n\n");

        int chapitreNum = 1;
        for (String paragraphe : paragraphes) {
            if (!paragraphe.trim().isEmpty()) {
                // Si le paragraphe commence par un titre de chapitre
                if (paragraphe.startsWith("Chapitre") || paragraphe.startsWith("CHAPITRE") ||
                        paragraphe.startsWith("Partie") || paragraphe.startsWith("SECTION")) {
                    html.append("<div class=\"chapter\">")
                            .append("<div class=\"chapter-title\">")
                            .append(paragraphe.replace("\n", "<br>"))
                            .append("</div>")
                            .append("</div>");
                } else {
                    html.append("<p>")
                            .append(paragraphe.replace("\n", "<br>"))
                            .append("</p>");
                }
            }
        }

        return html.toString();
    }

    // =============================
    // DÉTECTION DE SCROLL PRÉCISE
    // =============================
    private void injecterDetectionScroll() {
        webEngine.executeScript(
                "// Marqueur de fin de contenu\n" +
                        "var endMarker = document.querySelector('.end-marker');\n" +
                        "if (endMarker) {\n" +
                        "    endMarker.id = 'end-of-content';\n" +
                        "}\n" +
                        "\n" +
                        "// Fonction pour vérifier si la fin est visible\n" +
                        "function isEndVisible() {\n" +
                        "    var endElement = document.getElementById('end-of-content') || document.body;\n" +
                        "    var rect = endElement.getBoundingClientRect();\n" +
                        "    var windowHeight = window.innerHeight || document.documentElement.clientHeight;\n" +
                        "    \n" +
                        "    // L'élément est visible si sa partie supérieure est dans la fenêtre\n" +
                        "    return rect.top >= 0 && rect.top <= windowHeight;\n" +
                        "}\n" +
                        "\n" +
                        "// Détecter le scroll\n" +
                        "var scrollTimeout;\n" +
                        "window.addEventListener('scroll', function() {\n" +
                        "    clearTimeout(scrollTimeout);\n" +
                        "    scrollTimeout = setTimeout(function() {\n" +
                        "        if (isEndVisible()) {\n" +
                        "            document.title = 'END_VISIBLE';\n" +
                        "        }\n" +
                        "    }, 300); // Délai pour éviter les déclenchements multiples\n" +
                        "});\n" +
                        "\n" +
                        "console.log('Scroll detection injected');"
        );

        // Écouter les changements de titre
        webEngine.titleProperty().addListener((obs, oldTitle, newTitle) -> {
            if ("END_VISIBLE".equals(newTitle) && !dejaValidee && leconCourante != null) {
                dejaValidee = true;
                System.out.println("WEBVIEW: Fin du contenu visible - validation");
                arreterVerificationScroll();
                marquerCommeTerminee();
            }
        });
    }

    private void demarrerVerificationScroll() {
        scrollCheckTimer = new Timer(true);
        scrollCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    verifierPositionScrollPrecise();
                });
            }
        }, 2000, 1000); // Commencer après 2s, vérifier chaque seconde
    }

    private void arreterVerificationScroll() {
        if (scrollCheckTimer != null) {
            scrollCheckTimer.cancel();
            scrollCheckTimer = null;
        }
    }

    private void verifierPositionScrollPrecise() {
        if (dejaValidee || leconCourante == null) {
            return;
        }

        try {
            // Vérifier si la fin du contenu est visible
            Object result = webEngine.executeScript(
                    "(function() {\n" +
                            "    var body = document.body;\n" +
                            "    var html = document.documentElement;\n" +
                            "    \n" +
                            "    var windowHeight = window.innerHeight || html.clientHeight;\n" +
                            "    var scrollTop = window.pageYOffset || html.scrollTop;\n" +
                            "    var scrollHeight = Math.max(body.scrollHeight, html.scrollHeight);\n" +
                            "    \n" +
                            "    // Distance du bas\n" +
                            "    var distanceFromBottom = scrollHeight - (scrollTop + windowHeight);\n" +
                            "    \n" +
                            "    // Si on est à moins de 50px du bas\n" +
                            "    return distanceFromBottom <= 50;\n" +
                            "})()"
            );

            if (result instanceof Boolean) {
                boolean atBottom = (Boolean) result;

                if (atBottom && !dejaValidee) {
                    dejaValidee = true;
                    System.out.println("VERIFICATION: Bas du contenu atteint");
                    arreterVerificationScroll();
                    marquerCommeTerminee();
                }
            }
        } catch (Exception e) {
            // JavaScript pas encore prêt
        }
    }

    private void marquerCommeTerminee() {
        if (leconCourante == null) {
            System.out.println("ERROR: leconCourante est null!");
            return;
        }

        System.out.println("=== MARQUER COMME TERMINÉE ===");
        System.out.println("Candidat ID: " + candidatId);
        System.out.println("Leçon ID: " + leconCourante.getId());
        System.out.println("Leçon Titre: " + leconCourante.getTitre());

        try {
            progressionLeconService.marquerTerminee(candidatId, leconCourante.getId());

            boolean estTerminee = progressionLeconService.isLeconTerminee(candidatId, leconCourante.getId());
            System.out.println("Vérification BD: leçon terminée = " + estTerminee);

            lblStatus.setText("✔ Terminée");
            btnTerminer.setVisible(false);

            mettreAJourProgression();
            mettreAJourBoutonLecon(leconCourante);

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mettreAJourBoutonLecon(Lecon lecon) {
        for (javafx.scene.Node node : boxModules.getChildren()) {
            if (node instanceof Button) {
                Button btn = (Button) node;
                if (btn.getUserData() == lecon) {
                    if (!btn.getText().contains("✓")) {
                        btn.setText(btn.getText() + " ✓");
                        System.out.println("DEBUG: Bouton mis à jour avec ✓");
                    }
                    break;
                }
            }
        }
    }

    @FXML
    private void terminerLecon() {
        System.out.println("DEBUG: Bouton Terminer cliqué");
        marquerCommeTerminee();
    }

    private void mettreAJourProgression() {
        System.out.println("=== MISE À JOUR PROGRESSION ===");

        double prog = progressionLeconService.getProgressionCours(candidatId, coursActuel.getId());
        System.out.println("DEBUG: Progression calculée = " + prog + "%");

        progressBar.setProgress(prog / 100);
        lblProgression.setText(String.format("%.0f%%", prog));

        try {
            progressionCoursService.ajouterOuUpdate(candidatId, coursActuel.getId(), (int) prog);
            System.out.println("DEBUG: Progression enregistrée en BD");
        } catch (Exception e) {
            System.out.println("ERROR lors de l'enregistrement: " + e.getMessage());
            e.printStackTrace();
        }
    }
}