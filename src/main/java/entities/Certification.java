package entities;

import java.time.LocalDateTime;

public class Certification {
    private int id;
    private LocalDateTime dateObtention;
    private int candidatId;
    private int coursId;

    public Certification() {}

    public Certification(int candidatId, int coursId, LocalDateTime dateObtention) {
        this.candidatId = candidatId;
        this.coursId = coursId;
        this.dateObtention = dateObtention;
    }

    public Certification(int id, int candidatId, int coursId, LocalDateTime dateObtention) {
        this.id = id;
        this.candidatId = candidatId;
        this.coursId = coursId;
        this.dateObtention = dateObtention;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCandidatId() { return candidatId; }
    public void setCandidatId(int candidatId) { this.candidatId = candidatId; }

    public int getCoursId() { return coursId; }
    public void setCoursId(int coursId) { this.coursId = coursId; }

    public LocalDateTime getDateObtention() { return dateObtention; }
    public void setDateObtention(LocalDateTime dateObtention) { this.dateObtention = dateObtention; }

    @Override
    public String toString() {
        return "Certification{" +
                "id=" + id +
                ", candidatId=" + candidatId +
                ", coursId=" + coursId +
                ", dateObtention=" + dateObtention +
                '}';
    }
}
