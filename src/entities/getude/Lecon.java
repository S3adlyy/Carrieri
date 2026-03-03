package entities.getude;

public class Lecon {
    private int id;
    private String titre;
    private String contenu;
    private byte[] video;        // ✅ LONGBLOB - pas de changement !
    private int ordre;
    private int moduleId;

    // Constructeur sans ID (pour ajout)
    public Lecon(String titre, String contenu, byte[] video, int ordre, int moduleId) {
        this.titre = titre;
        this.contenu = contenu;
        this.video = video;
        this.ordre = ordre;
        this.moduleId = moduleId;
    }

    // Constructeur avec ID (pour lecture)
    public Lecon(int id, String titre, String contenu, byte[] video, int ordre, int moduleId) {
        this.id = id;
        this.titre = titre;
        this.contenu = contenu;
        this.video = video;
        this.ordre = ordre;
        this.moduleId = moduleId;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public byte[] getVideo() { return video; }
    public void setVideo(byte[] video) { this.video = video; }

    public int getOrdre() { return ordre; }
    public void setOrdre(int ordre) { this.ordre = ordre; }

    public int getModuleId() { return moduleId; }
    public void setModuleId(int moduleId) { this.moduleId = moduleId; }
}