package entities;

public class Reponse {
    private int id;
    private int questionId;
    private String questionType; // "QUIZ" ou "TEST"
    private String reponseText;
    private boolean estCorrecte;
    private int ordre;

    public Reponse() {}

    public Reponse(int questionId, String questionType, String reponseText, boolean estCorrecte, int ordre) {
        this.questionId = questionId;
        this.questionType = questionType;
        this.reponseText = reponseText;
        this.estCorrecte = estCorrecte;
        this.ordre = ordre;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public String getReponseText() { return reponseText; }
    public void setReponseText(String reponseText) { this.reponseText = reponseText; }
    public boolean isEstCorrecte() { return estCorrecte; }
    public void setEstCorrecte(boolean estCorrecte) { this.estCorrecte = estCorrecte; }
    public int getOrdre() { return ordre; }
    public void setOrdre(int ordre) { this.ordre = ordre; }
}