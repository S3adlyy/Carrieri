package entities.goffre;

import java.time.LocalDateTime;

public class FavoriteOffre {
    private int id;
    private int candidatId;
    private int offreId;
    private LocalDateTime dateAjout;

    // Reference to the actual offer (for convenience)
    private OffreEmploi offre;

    public FavoriteOffre() {
        this.dateAjout = LocalDateTime.now();
    }

    public FavoriteOffre(int candidatId, int offreId) {
        this.candidatId = candidatId;
        this.offreId = offreId;
        this.dateAjout = LocalDateTime.now();
    }

    public FavoriteOffre(int id, int candidatId, int offreId, LocalDateTime dateAjout) {
        this.id = id;
        this.candidatId = candidatId;
        this.offreId = offreId;
        this.dateAjout = dateAjout;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCandidatId() {
        return candidatId;
    }

    public void setCandidatId(int candidatId) {
        this.candidatId = candidatId;
    }

    public int getOffreId() {
        return offreId;
    }

    public void setOffreId(int offreId) {
        this.offreId = offreId;
    }

    public LocalDateTime getDateAjout() {
        return dateAjout;
    }

    public void setDateAjout(LocalDateTime dateAjout) {
        this.dateAjout = dateAjout;
    }

    public OffreEmploi getOffre() {
        return offre;
    }

    public void setOffre(OffreEmploi offre) {
        this.offre = offre;
    }

    @Override
    public String toString() {
        return "FavoriteOffre{" +
                "id=" + id +
                ", candidatId=" + candidatId +
                ", offreId=" + offreId +
                ", dateAjout=" + dateAjout +
                '}';
    }
}

