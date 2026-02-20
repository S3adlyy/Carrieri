package main;

import entities.QuestionTest;
import entities.Reponse;
import entities.ResultatTestCours;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.TestCoursService;
import services.TraductionService;

import java.util.ArrayList;
import java.util.List;

public class TestFinalPlayerController {

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

    private TestCoursService testService = new TestCoursService();
    private List<QuestionTest> questions;
    private int indexCourant = 0;
    private int candidatId;
    private int coursId;
    private ToggleGroup group = new ToggleGroup();
    private int[] reponsesSelectionnees; // ✅ Changé de RadioButton[] à int[]
    private TraductionService traductionService = new TraductionService();
    private String langueTest = "fr";

    public void setCoursId(int coursId, int candidatId) {
        this.coursId = coursId;
        this.candidatId = candidatId;

        // ✅ Récupérer la langue depuis LangueTest
        this.langueTest = LangueTest.getInstance().getLangue();

        // Charger les questions originales
        this.questions = testService.getQuestionsByCours(coursId);

        // ✅ Traduire les questions
        traduireQuestions();

        // ✅ Initialiser le tableau d'entiers (pas de RadioButton)
        this.reponsesSelectionnees = new int[questions.size()];
        for (int i = 0; i < reponsesSelectionnees.length; i++) {
            reponsesSelectionnees[i] = -1; // -1 = pas de réponse
        }

        if (!questions.isEmpty()) {
            afficherQuestion(0);
            mettreAJourProgression();
        } else {
            lblQuestion.setText("Aucune question disponible pour ce test.");
            boxReponses.getChildren().clear();
            btnPrecedent.setDisable(true);
            btnSuivant.setDisable(true);
            btnTerminer.setVisible(false);
        }
    }

    private void traduireQuestions() {
        if (langueTest.equals("fr")) return;

        for (QuestionTest q : questions) {
            // Traduire le texte de la question
            q.setQuestionText(traductionService.traduire(q.getQuestionText(), langueTest));

            // Traduire les réponses
            if (q.getReponses() != null) {
                for (Reponse r : q.getReponses()) {
                    r.setReponseText(traductionService.traduire(r.getReponseText(), langueTest));
                }
            }
        }
    }

    private void afficherQuestion(int index) {
        QuestionTest q = questions.get(index);

        lblQuestion.setText((index + 1) + ". " + q.getQuestionText());
        lblPoints.setText("Points: " + q.getPoints());

        boxReponses.getChildren().clear();
        group = new ToggleGroup();

        for (Reponse r : q.getReponses()) {
            RadioButton rb = new RadioButton(r.getReponseText());
            rb.setToggleGroup(group);
            rb.setUserData(r); // Stocker l'objet Reponse
            rb.setWrapText(true);
            rb.setStyle("-fx-font-size: 14px; -fx-padding: 5;");

            // ✅ Restaurer la réponse précédente (comparer les IDs)
            if (reponsesSelectionnees[index] != -1 && reponsesSelectionnees[index] == r.getId()) {
                rb.setSelected(true);
            }

            boxReponses.getChildren().add(rb);
        }

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
            reponsesSelectionnees[indexCourant] = r.getId(); // ✅ Stocker l'ID, pas le RadioButton
        }
    }

    @FXML
    private void terminerTest() {
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

        int score = 0;
        int totalPoints = 0;

        for (int i = 0; i < questions.size(); i++) {
            QuestionTest q = questions.get(i);
            totalPoints += q.getPoints();

            // ✅ Chercher la réponse correspondante
            for (Reponse r : q.getReponses()) {
                if (r.getId() == reponsesSelectionnees[i] && r.isEstCorrecte()) {
                    score += q.getPoints();
                    break;
                }
            }
        }

        double pourcentage = (double) score / totalPoints * 100;
        boolean reussite = pourcentage >= 70;

        ResultatTestCours resultat = new ResultatTestCours(candidatId, coursId, score, totalPoints, reussite);
        testService.sauvegarderResultat(resultat);

        afficherResultat(score, totalPoints, pourcentage, reussite);
    }

    private void afficherResultat(int score, int total, double pourcentage, boolean reussite) {
        lblQuestion.setVisible(false);
        boxReponses.setVisible(false);
        btnPrecedent.setVisible(false);
        btnSuivant.setVisible(false);
        btnTerminer.setVisible(false);
        lblProgression.setVisible(false);
        progressBar.setVisible(false);

        boxResultat.setVisible(true);
        lblScore.setText(String.format("Score final: %d/%d (%.0f%%)", score, total, pourcentage));

        if (reussite) {
            lblMessage.setText("🎓 FÉLICITATIONS ! Vous avez réussi le cours !");
            lblMessage.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 16px;");
        } else {
            lblMessage.setText("❌ Test non réussi. Minimum 70% requis.");
            lblMessage.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
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