package main;

import entities.QuestionQuiz;
import entities.Reponse;
import entities.ResultatQuizModule;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.QuizModuleService;
import services.TraductionService;

import java.util.ArrayList;
import java.util.List;

public class QuizModulePlayerController {

    @FXML private Label lblTitre;
    @FXML private Label lblQuestion;
    @FXML private Label lblPoints;
    @FXML private VBox boxReponses;
    @FXML private Button btnPrecedent;
    @FXML private Button btnSuivant;
    @FXML private Button btnTerminer;
    @FXML private Label lblProgression;
    @FXML private ProgressBar progressBar;
    @FXML private VBox boxResultat;
    @FXML private Label lblScore;
    @FXML private Label lblMessage;

    private QuizModuleService quizService = new QuizModuleService();
    private List<QuestionQuiz> questions;
    private int indexCourant = 0;
    private int candidatId;
    private int moduleId;
    private ToggleGroup group;
    private int[] reponsesSelectionnees; // Stocke les IDs des réponses sélectionnées
    private TraductionService traductionService = new TraductionService();
    private String langueTest = "fr";

    public void setModuleId(int moduleId, int candidatId) {
        this.moduleId = moduleId;
        this.candidatId = candidatId;

        // ✅ Récupérer la langue depuis LangueTest
        this.langueTest = LangueTest.getInstance().getLangue();

        // Charger les questions originales
        this.questions = quizService.getQuestionsByModule(moduleId);

        // ✅ Traduire les questions selon la langue
        traduireQuestions();

        // Initialiser le tableau des réponses
        this.reponsesSelectionnees = new int[questions.size()];
        for (int i = 0; i < reponsesSelectionnees.length; i++) {
            reponsesSelectionnees[i] = -1;
        }

        lblTitre.setText("Quiz du module");

        if (!questions.isEmpty()) {
            afficherQuestion(0);
            mettreAJourProgression();
        } else {
            lblQuestion.setText("Aucune question disponible pour ce module.");
            boxReponses.getChildren().clear();
            btnPrecedent.setDisable(true);
            btnSuivant.setDisable(true);
            btnTerminer.setVisible(false);
        }
    }
    private void traduireQuestions() {
        if (langueTest.equals("fr")) {
            // Pas besoin de traduction pour le français
            return;
        }

        // Traduire toutes les questions
        List<QuestionQuiz> questionsTraduites = new ArrayList<>();
        for (QuestionQuiz q : questions) {
            questionsTraduites.add(traductionService.traduireQuestionQuiz(q, langueTest));
        }
        this.questions = questionsTraduites;

        System.out.println("✅ Questions traduites en " + langueTest);
    }

    private void afficherQuestion(int index) {
        QuestionQuiz q = questions.get(index);

        // ✅ Afficher la question
        lblQuestion.setText((index + 1) + ". " + q.getQuestionText());
        lblPoints.setText("Points: " + q.getPoints());

        // ✅ Vider et reconstruire les réponses
        boxReponses.getChildren().clear();
        group = new ToggleGroup();

        // ✅ Vérifier qu'il y a des réponses
        List<Reponse> reponses = q.getReponses();
        if (reponses == null || reponses.isEmpty()) {
            Label lblErreur = new Label("Aucune réponse disponible pour cette question.");
            lblErreur.setStyle("-fx-text-fill: red;");
            boxReponses.getChildren().add(lblErreur);
            return;
        }

        // ✅ Créer les RadioButtons pour chaque réponse
        for (Reponse r : reponses) {
            RadioButton rb = new RadioButton(r.getReponseText());
            rb.setToggleGroup(group);
            rb.setUserData(r); // Stocker l'objet Reponse complet
            rb.setWrapText(true);
            rb.setStyle("-fx-font-size: 14px; -fx-padding: 5 0;");

            // ✅ Restaurer la réponse précédente si elle existe
            if (reponsesSelectionnees[index] != -1 && reponsesSelectionnees[index] == r.getId()) {
                rb.setSelected(true);
            }

            boxReponses.getChildren().add(rb);
        }

        // ✅ Gérer les boutons de navigation
        btnPrecedent.setDisable(index == 0);
        btnSuivant.setDisable(index == questions.size() - 1);
        btnTerminer.setVisible(index == questions.size() - 1);
    }

    @FXML
    private void questionPrecedente() {
        sauvegarderReponseCourante();
        if (indexCourant > 0) {
            indexCourant--;
            afficherQuestion(indexCourant);
            mettreAJourProgression();
        }
    }

    @FXML
    private void questionSuivante() {
        sauvegarderReponseCourante();
        if (indexCourant < questions.size() - 1) {
            indexCourant++;
            afficherQuestion(indexCourant);
            mettreAJourProgression();
        }
    }

    private void sauvegarderReponseCourante() {
        RadioButton selected = (RadioButton) group.getSelectedToggle();
        if (selected != null) {
            Reponse r = (Reponse) selected.getUserData();
            reponsesSelectionnees[indexCourant] = r.getId();
            System.out.println("✅ Réponse sauvegardée pour question " + (indexCourant + 1) +
                    ": " + r.getReponseText() + " (ID: " + r.getId() + ")");
        }
    }

    @FXML
    private void terminerQuiz() {
        // ✅ Sauvegarder la dernière réponse
        sauvegarderReponseCourante();

        // ✅ Vérifier que toutes les questions ont une réponse
        for (int i = 0; i < reponsesSelectionnees.length; i++) {
            if (reponsesSelectionnees[i] == -1) {
                showAlert("⚠️ Attention", "Veuillez répondre à toutes les questions avant de terminer.");
                indexCourant = i;
                afficherQuestion(indexCourant);
                return;
            }
        }

        // ✅ Calculer le score
        int score = 0;
        int totalPoints = 0;

        for (int i = 0; i < questions.size(); i++) {
            QuestionQuiz q = questions.get(i);
            totalPoints += q.getPoints();

            // ✅ Trouver la réponse sélectionnée
            for (Reponse r : q.getReponses()) {
                if (r.getId() == reponsesSelectionnees[i]) {
                    if (r.isEstCorrecte()) {
                        score += q.getPoints();
                        System.out.println("✅ Question " + (i+1) + ": Bonne réponse (+" + q.getPoints() + " pts)");
                    } else {
                        System.out.println("❌ Question " + (i+1) + ": Mauvaise réponse");
                    }
                    break;
                }
            }
        }

        double pourcentage = (double) score / totalPoints * 100;
        boolean reussite = pourcentage >= 70; // 70% requis

        System.out.println("📊 Score: " + score + "/" + totalPoints + " (" + String.format("%.0f", pourcentage) + "%)");
        System.out.println("🎯 Résultat: " + (reussite ? "RÉUSSI" : "ÉCHEC"));

        // ✅ Sauvegarder le résultat
        ResultatQuizModule resultat = new ResultatQuizModule(candidatId, moduleId, score, totalPoints, reussite);
        quizService.sauvegarderResultat(resultat);

        // ✅ Afficher le résultat
        afficherResultat(score, totalPoints, pourcentage, reussite);
    }

    private void afficherResultat(int score, int total, double pourcentage, boolean reussite) {
        // ✅ Cacher le quiz
        lblQuestion.setVisible(false);
        boxReponses.setVisible(false);
        btnPrecedent.setVisible(false);
        btnSuivant.setVisible(false);
        btnTerminer.setVisible(false);
        lblProgression.setVisible(false);
        progressBar.setVisible(false);

        // ✅ Afficher le résultat
        boxResultat.setVisible(true);
        lblScore.setText(String.format("Score: %d/%d (%.0f%%)", score, total, pourcentage));

        if (reussite) {
            lblMessage.setText("✅ FÉLICITATIONS ! Quiz réussi !");
            lblMessage.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 18px;");
        } else {
            lblMessage.setText("❌ Quiz non réussi. Score minimum requis: 70%");
            lblMessage.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 18px;");
        }
        lblMessage.getStyleClass().removeAll("message-success", "message-error");

        if (reussite) {
            lblMessage.getStyleClass().add("message-success");
        } else {
            lblMessage.getStyleClass().add("message-error");
        }
    }

    private void mettreAJourProgression() {
        lblProgression.setText(String.format("Question %d/%d", indexCourant + 1, questions.size()));
        progressBar.setProgress((double) (indexCourant + 1) / questions.size());
    }

    @FXML
    private void fermer() {
        CandidatShellController.getInstance().retourAuCours();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

}