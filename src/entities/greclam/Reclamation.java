package entities.greclam;

import java.util.Date;
import java.util.Objects;

public class Reclamation {
    private int id;
    private String objet;
    private String description;
    private String categorie;
    private Date dateCreation;
    private String statut;
    private String priorite;
    private Integer utilisateurId;
    private String email; // ← NOUVEAU CHAMP

    // Constructeurs
    public Reclamation() {}

    public Reclamation(int id, String objet, String description, String categorie,
                       Date dateCreation, String statut, String priorite,
                       Integer utilisateurId, String email) {
        this.id = id;
        this.objet = objet;
        this.description = description;
        this.categorie = categorie;
        this.dateCreation = dateCreation;
        this.statut = statut;
        this.priorite = priorite;
        this.utilisateurId = utilisateurId;
        this.email = email;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getObjet() { return objet; }
    public void setObjet(String objet) { this.objet = objet; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public Date getDateCreation() { return dateCreation; }
    public void setDateCreation(Date dateCreation) { this.dateCreation = dateCreation; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getPriorite() { return priorite; }
    public void setPriorite(String priorite) { this.priorite = priorite; }

    public Integer getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(Integer utilisateurId) { this.utilisateurId = utilisateurId; }

    public String getEmail() { return email; } // ← NOUVEAU
    public void setEmail(String email) { this.email = email; } // ← NOUVEAU

    @Override
    public String toString() {
        return "Reclamation{" +
                "id=" + id +
                ", objet='" + objet + '\'' +
                ", description='" + description + '\'' +
                ", categorie='" + categorie + '\'' +
                ", dateCreation=" + dateCreation +
                ", statut='" + statut + '\'' +
                ", priorite='" + priorite + '\'' +
                ", utilisateurId=" + utilisateurId +
                ", email='" + email + '\'' +
                '}';
    }
}