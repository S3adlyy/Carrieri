package entities.getude;

import java.time.LocalDateTime;

public class ResultatQuizModule {
    private int id;
    private int candidatId;
    private int moduleId;
    private int score;
    private int totalPoints;
    private LocalDateTime dateCompletion;
    private boolean reussite;

    public ResultatQuizModule() {}

    public ResultatQuizModule(int candidatId, int moduleId, int score, int totalPoints, boolean reussite) {
        this.candidatId = candidatId;
        this.moduleId = moduleId;
        this.score = score;
        this.totalPoints = totalPoints;
        this.reussite = reussite;
        this.dateCompletion = LocalDateTime.now();
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCandidatId() { return candidatId; }
    public void setCandidatId(int candidatId) { this.candidatId = candidatId; }
    public int getModuleId() { return moduleId; }
    public void setModuleId(int moduleId) { this.moduleId = moduleId; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }
    public LocalDateTime getDateCompletion() { return dateCompletion; }
    public void setDateCompletion(LocalDateTime dateCompletion) { this.dateCompletion = dateCompletion; }
    public boolean isReussite() { return reussite; }
    public void setReussite(boolean reussite) { this.reussite = reussite; }
}