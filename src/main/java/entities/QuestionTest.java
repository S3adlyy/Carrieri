package entities;

import java.util.List;

public class QuestionTest {
    private int id;
    private int coursId;
    private String questionText;
    private int points;
    private int ordre;
    private List<Reponse> reponses;

    public QuestionTest() {}

    public QuestionTest(int coursId, String questionText, int points, int ordre) {
        this.coursId = coursId;
        this.questionText = questionText;
        this.points = points;
        this.ordre = ordre;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCoursId() { return coursId; }
    public void setCoursId(int coursId) { this.coursId = coursId; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public int getOrdre() { return ordre; }
    public void setOrdre(int ordre) { this.ordre = ordre; }
    public List<Reponse> getReponses() { return reponses; }
    public void setReponses(List<Reponse> reponses) { this.reponses = reponses; }
}