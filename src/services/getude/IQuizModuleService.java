package services.getude;

import entities.getude.QuestionQuiz;
import entities.getude.Reponse;
import entities.getude.ResultatQuizModule;

import java.util.List;

public interface IQuizModuleService {
    void ajouterQuestion(int moduleId, String questionText, int points, int ordre, List<Reponse> reponses);
    List<QuestionQuiz> getQuestionsByModule(int moduleId);
    boolean isModuleReussi(int candidatId, int moduleId);
    void sauvegarderResultat(ResultatQuizModule resultat);
}

