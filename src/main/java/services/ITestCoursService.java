package services;

import entities.QuestionTest;
import entities.Reponse;
import entities.ResultatTestCours;
import java.util.List;

public interface ITestCoursService {
    void ajouterQuestion(int coursId, String questionText, int points, int ordre, List<Reponse> reponses);
    List<QuestionTest> getQuestionsByCours(int coursId);
    void sauvegarderResultat(ResultatTestCours resultat);
    boolean isCoursReussi(int candidatId, int coursId);
}

