package entities.getude;

import java.util.List;

public class Module {
    private int id;
    private String titre;
    private String description;
    private int ordre;
    private int coursId;
    private List<Lecon> lecons;

    public List<Lecon> getLecons() {
        return lecons;
    }

    public void setLecons(List<Lecon> lecons) {
        this.lecons = lecons;
    }

    public Module(int id, String titre, String description, int ordre, int coursId) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.ordre = ordre;
        this.coursId = coursId;
    }

    public Module(String titre, String description, int ordre, int coursId) {
        this.titre = titre;
        this.description = description;
        this.ordre = ordre;
        this.coursId = coursId;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }

    public void setCoursId(int coursId) {
        this.coursId = coursId;
    }

    public int getId() { return id; }
    public String getTitre() { return titre; }
    public String getDescription() { return description; }
    public int getOrdre() { return ordre; }
    public int getCoursId() { return coursId; }
}

