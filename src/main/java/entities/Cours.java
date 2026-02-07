package entities;

public class Cours {
    private int id;
    private String titre;
    private String description;
    private int duree;
    private String niveau;
    private String competences_visees;
    private boolean est_obligatoire;

    private int createdBy;

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public Cours(){}
    public Cours(String titre, String description, int duree, String niveau, String competences_visees, boolean est_obligatoire,int createdBy) {
        this.titre = titre;
        this.description = description;
        this.duree = duree;
        this.niveau = niveau;
        this.competences_visees = competences_visees;
        this.est_obligatoire = est_obligatoire;
        this.createdBy = createdBy;
    }

    public Cours(int id, String titre, String description, int duree, String niveau, String competences_visees, boolean est_obligatoire,int createdBy) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.duree = duree;
        this.niveau = niveau;
        this.competences_visees = competences_visees;
        this.est_obligatoire = est_obligatoire;
        this.createdBy = createdBy;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDuree() {
        return duree;
    }

    public void setDuree(int duree) {
        this.duree = duree;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public String getCompetences_visees() {
        return competences_visees;
    }

    public void setCompetences_visees(String competences_visees) {
        this.competences_visees = competences_visees;
    }

    public boolean isEst_obligatoire() {
        return est_obligatoire;
    }

    public void setEst_obligatoire(boolean est_obligatoire) {
        this.est_obligatoire = est_obligatoire;
    }

    @Override
    public String toString() {
        return "Cours{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", duree=" + duree +
                ", niveau='" + niveau + '\'' +
                ", competences_visees='" + competences_visees + '\'' +
                ", est_obligatoire=" + est_obligatoire +
                '}';
    }


}