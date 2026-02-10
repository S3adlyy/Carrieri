package entities;

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

    public Reclamation() {
    }

    public Reclamation(int id, String objet, String description, String categorie,
                       Date dateCreation, String statut, String priorite, Integer utilisateurId) {
        this.id = id;
        this.objet = objet;
        this.description = description;
        this.categorie = categorie;
        this.dateCreation = dateCreation;
        this.statut = statut;
        this.priorite = priorite;
        this.utilisateurId = utilisateurId;
    }

    public Reclamation(String objet, String description, String categorie,
                       Date dateCreation, String statut, String priorite, Integer utilisateurId) {
        this.objet = objet;
        this.description = description;
        this.categorie = categorie;
        this.dateCreation = dateCreation;
        this.statut = statut;
        this.priorite = priorite;
        this.utilisateurId = utilisateurId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getObjet() {
        return objet;
    }

    public void setObjet(String objet) {
        this.objet = objet;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public Date getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Date dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getPriorite() {
        return priorite;
    }

    public void setPriorite(String priorite) {
        this.priorite = priorite;
    }

    public Integer getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(Integer utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

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
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Reclamation that = (Reclamation) o;
        return id == that.id &&
                Objects.equals(objet, that.objet) &&
                Objects.equals(description, that.description) &&
                Objects.equals(categorie, that.categorie) &&
                Objects.equals(dateCreation, that.dateCreation) &&
                Objects.equals(statut, that.statut) &&
                Objects.equals(priorite, that.priorite) &&
                Objects.equals(utilisateurId, that.utilisateurId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, objet, description, categorie, dateCreation, statut, priorite, utilisateurId);
    }
}