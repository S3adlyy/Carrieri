package main;

import entities.QuestionTest;
import entities.Reponse;
import entities.ResultatTestCours;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.TestCoursService;

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
    private RadioButton[] reponsesSelectionnees;

    public void setCoursId(int coursId, int candidatId) {
        this.coursId = coursId;
        this.candidatId = candidatId;
        this.questions = testService.getQuestionsByCours(coursId);
        this.reponsesSelectionnees = new RadioButton[questions.size()];

        lblTitre.setText("TEST FINAL");

        if (!questions.isEmpty()) {
            afficherQuestion(0);
            mettreAJourProgression();
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
            rb.setUserData(r);
            rb.setWrapText(true);
            rb.setStyle("-fx-font-size: 14px; -fx-padding: 5;");

            if (reponsesSelectionnees[index] != null &&
                    reponsesSelectionnees[index].getUserData().equals(r)) {
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
            reponsesSelectionnees[indexCourant] = selected;
        }
    }

    @FXML
    private void terminerTest() {
        sauvegarderReponseCourante();

        for (int i = 0; i < reponsesSelectionnees.length; i++) {
            if (reponsesSelectionnees[i] == null) {
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

            RadioButton rb = reponsesSelectionnees[i];
            Reponse r = (Reponse) rb.getUserData();

            if (r.isEstCorrecte()) {
                score += q.getPoints();
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
        Stage stage = (Stage) lblTitre.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}