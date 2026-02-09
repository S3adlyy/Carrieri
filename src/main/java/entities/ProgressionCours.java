package entities;

public class ProgressionCours {

    private int id;
    private int candidatId;
    private int coursId;
    private int progression;

    public ProgressionCours() {}

    public ProgressionCours(int candidatId, int coursId, int progression) {
        this.candidatId = candidatId;
        this.coursId = coursId;
        this.progression = progression;
    }

    public int getId() {
        return id;
    }

    public int getCandidatId() {
        return candidatId;
    }

    public int getCoursId() {
        return coursId;
    }

    public int getProgression() {
        return progression;
    }

    public void setCandidatId(int candidatId) {
        this.candidatId = candidatId;
    }

    public void setCoursId(int coursId) {
        this.coursId = coursId;
    }

    public void setProgression(int progression) {
        this.progression = progression;
    }
}
