package entities.grecru;

import java.sql.Date;

public class RenduMission {
    private int id;
    private String codeSolution;
    private String fichier;
    private Date dateRendu;
    private int score;
    private String resultat;
    private int missionId;
    private int candidatId;
    private String feedback;
    private String langue;

    // Constructors
    public RenduMission() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCodeSolution() { return codeSolution; }
    public void setCodeSolution(String codeSolution) { this.codeSolution = codeSolution; }

    public String getFichier() { return fichier; }
    public void setFichier(String fichier) { this.fichier = fichier; }

    public Date getDateRendu() { return dateRendu; }
    public void setDateRendu(Date dateRendu) { this.dateRendu = dateRendu; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getResultat() { return resultat; }
    public void setResultat(String resultat) { this.resultat = resultat; }

    public int getMissionId() { return missionId; }
    public void setMissionId(int missionId) { this.missionId = missionId; }

    public int getCandidatId() { return candidatId; }
    public void setCandidatId(int candidatId) { this.candidatId = candidatId; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public String getLangue() { return langue; }
    public void setLangue(String langue) { this.langue = langue; }

    public boolean isAccepted() {
        return "ACCEPTED".equalsIgnoreCase(resultat);
    }

    @Override
    public String toString() {
        return "RenduMission{" +
                "id=" + id +
                ", score=" + score +
                ", resultat='" + resultat + '\'' +
                ", accepted=" + isAccepted() +
                '}';
    }
}