package services.getude;

import entities.getude.QuestionTest;
import entities.getude.Reponse;
import entities.getude.ResultatTestCours;

import java.util.List;

public interface ITestCoursService {
    void ajouterQuestion(int coursId, String questionText, int points, int ordre, List<Reponse> reponses);
    List<QuestionTest> getQuestionsByCours(int coursId);
    void sauvegarderResultat(ResultatTestCours resultat);
    boolean isCoursReussi(int candidatId, int coursId);
}

