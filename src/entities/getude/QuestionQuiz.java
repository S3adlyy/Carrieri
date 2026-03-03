package entities.getude;

import java.util.List;

public class QuestionQuiz {
    private int id;
    private int moduleId;
    private String questionText;
    private int points;
    private int ordre;
    private List<Reponse> reponses;

    public QuestionQuiz() {}

    public QuestionQuiz(int moduleId, String questionText, int points, int ordre) {
        this.moduleId = moduleId;
        this.questionText = questionText;
        this.points = points;
        this.ordre = ordre;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getModuleId() { return moduleId; }
    public void setModuleId(int moduleId) { this.moduleId = moduleId; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public int getOrdre() { return ordre; }
    public void setOrdre(int ordre) { this.ordre = ordre; }
    public List<Reponse> getReponses() { return reponses; }
    public void setReponses(List<Reponse> reponses) { this.reponses = reponses; }
}