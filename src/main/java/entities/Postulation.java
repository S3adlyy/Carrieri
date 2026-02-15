package entities;

import java.time.LocalDateTime;

public class Postulation {

    private int id;
    private int candidatId;
    private int offreId;
    private LocalDateTime datePostulation;
    private String statut;
    private String motivationCandidature;

    // Insert (sans id)
    public Postulation(int candidatId, int offreId, LocalDateTime datePostulation, String statut, String motivationCandidature) {
        this.candidatId = candidatId;
        this.offreId = offreId;
        this.datePostulation = datePostulation;
        this.statut = statut;
        this.motivationCandidature = motivationCandidature;
    }

    // Read/Update (avec id)
    public Postulation(int id, int candidatId, int offreId, LocalDateTime datePostulation, String statut, String motivationCandidature) {
        this(candidatId, offreId, datePostulation, statut, motivationCandidature);
        this.id = id;
    }

    public int getId() { return id; }
    public int getCandidatId() { return candidatId; }
    public int getOffreId() { return offreId; }
    public LocalDateTime getDatePostulation() { return datePostulation; }
    public String getStatut() { return statut; }
    public String getMotivationCandidature() { return motivationCandidature; }

    public void setId(int id) { this.id = id; }
    public void setStatut(String statut) { this.statut = statut; }

    @Override
    public String toString() {
        return "Postulation{" +
                "id=" + id +
                ", candidatId=" + candidatId +
                ", offreId=" + offreId +
                ", datePostulation=" + datePostulation +
                ", statut='" + statut + '\'' +
                ", motivationCandidature='" + motivationCandidature + '\'' +
                '}';
    }
}
