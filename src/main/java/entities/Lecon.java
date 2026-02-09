package entities;

public class Lecon {
    private int id;
    private String titre;
    private String contenu;
    private String videoUrl;
    private int ordre;
    private int moduleId;

    public Lecon(String titre, String contenu, String videoUrl, int ordre, int moduleId) {
        this.titre = titre;
        this.contenu = contenu;
        this.videoUrl = videoUrl;
        this.ordre = ordre;
        this.moduleId = moduleId;
    }

    public Lecon(int id, String titre, String contenu, String videoUrl, int ordre, int moduleId) {
        this.id = id;
        this.titre = titre;
        this.contenu = contenu;
        this.videoUrl = videoUrl;
        this.ordre = ordre;
        this.moduleId = moduleId;
    }

    public int getId() { return id; }
    public String getTitre() { return titre; }
    public String getContenu() { return contenu; }
    public String getVideoUrl() { return videoUrl; }
    public int getOrdre() { return ordre; }
    public int getModuleId() { return moduleId; }
}

