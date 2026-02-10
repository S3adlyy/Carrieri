package main;

import entities.Cours;
import entities.Lecon;
import entities.Module;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import services.LeconService;
import services.ModuleService;
import services.ProgressionCoursService;
import services.ProgressionLeconService;

import java.util.List;

public class CoursPlayerController {

    @FXML private Label lblCoursTitre;
    @FXML private VBox boxModules;
    @FXML private Label lblLeconTitre;
    @FXML private ScrollPane scrollPaneContenu;
    @FXML private TextArea txtContenu;
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

    @FXML
    private void initialize() {
        System.out.println("DEBUG: CoursPlayerController initialisé");
        btnTerminer.setOnAction(e -> terminerLecon());

        // Configurer le TextArea
        txtContenu.setWrapText(true);
        txtContenu.setEditable(false);

        // Solution: Détecter le scroll du TextArea LUI-MÊME
        setupTextAreaScrollDetection();
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
        this.leconCourante = lecon;
        this.dejaValidee = false;

        System.out.println("DEBUG: Affichage leçon ID=" + lecon.getId() + ", Titre=" + lecon.getTitre());

        lblLeconTitre.setText(lecon.getTitre());
        txtContenu.setText(lecon.getContenu());
        lblStatus.setText("");

        // Positionner le scroll en haut
        Platform.runLater(() -> {
            txtContenu.positionCaret(0);
            txtContenu.deselect();
        });

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
        }
    }

    // =============================
    // SOLUTION: Détection de scroll sur TextArea
    // =============================
    private void setupTextAreaScrollDetection() {
        // 1. Écouter la position du curseur (caret) - quand l'utilisateur scroll, le curseur bouge
        txtContenu.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
            checkIfReachedBottom();
        });

        // 2. Écouter les événements de molette de souris sur le TextArea
        txtContenu.setOnScroll(event -> {
            System.out.println("SCROLL EVENT on TextArea");
            checkIfReachedBottom();
        });

        // 3. Vérifier périodiquement (toutes les 500ms) via un thread
        Thread scrollCheckThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);
                    Platform.runLater(this::checkIfReachedBottom);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        scrollCheckThread.setDaemon(true);
        scrollCheckThread.start();
    }

    private void checkIfReachedBottom() {
        if (dejaValidee || leconCourante == null || txtContenu.getText().isEmpty()) {
            return;
        }

        try {
            // Méthode 1: Basée sur la position du curseur
            String text = txtContenu.getText();
            int caretPosition = txtContenu.getCaretPosition();
            int textLength = text.length();

            // Calculer le pourcentage
            double percentage = (double) caretPosition / textLength;

            // Si le texte est court, valider immédiatement
            if (textLength < 500) {
                if (!dejaValidee) {
                    dejaValidee = true;
                    Platform.runLater(this::marquerCommeTerminee);
                }
                return;
            }

            // Log pour déboguer (toutes les 10%)
            int percent = (int)(percentage * 100);
            if (percent % 10 == 0 && percent > 0) {
                System.out.println("SCROLL DETECTION: " + percent + "% lu (caret=" + caretPosition + "/" + textLength + ")");
            }

            // Si on a atteint 95% du texte
            if (percentage >= 0.95 && !dejaValidee) {
                dejaValidee = true;
                System.out.println("SCROLL: Atteint " + percent + "% - validation");
                Platform.runLater(this::marquerCommeTerminee);
            }

        } catch (Exception e) {
            System.out.println("Erreur lors de la détection du scroll: " + e.getMessage());
        }
    }

    // =============================
    // Solution ALTERNATIVE: Détection par longueur de texte visible
    // =============================
    private void setupAlternativeScrollDetection() {
        // Cette méthode vérifie si l'utilisateur a scrollé en bas
        // en comparant la hauteur du contenu avec la position du scroll

        // Attendre que le TextArea soit rendu
        Platform.runLater(() -> {
            // Vérifier périodiquement
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(300);
                        Platform.runLater(() -> {
                            if (!dejaValidee && leconCourante != null) {
                                checkVisibleTextAreaContent();
                            }
                        });
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }).start();
        });
    }

    private void checkVisibleTextAreaContent() {
        try {
            // Obtenir la position du curseur
            int caretPos = txtContenu.getCaretPosition();
            String text = txtContenu.getText();

            if (text == null || text.isEmpty()) return;

            // Vérifier si le curseur est proche de la fin
            double percentage = (double) caretPos / text.length();

            // Log occasionnel
            if (Math.random() < 0.1) { // 10% du temps
                System.out.println("ALT SCROLL CHECK: " + (int)(percentage * 100) + "%");
            }

            if (percentage >= 0.90 && !dejaValidee) { // 90%
                dejaValidee = true;
                System.out.println("ALT SCROLL: Validation à " + (int)(percentage * 100) + "%");
                marquerCommeTerminee();
            }
        } catch (Exception e) {
            // Ignorer les erreurs
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