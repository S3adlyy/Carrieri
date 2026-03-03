package entities.goffre;

import java.time.LocalDateTime;

public class OffreEmploi {

    private int id;
    private String titre;
    private String description;
    private double salaire;
    private String typeContrat;
    private String localisation;
    private LocalDateTime datePublication;
    private LocalDateTime dateExpiration;
    private String niveauQualification;
    private String experienceRequise;
    private String competencesRequises;
    private String secteurActivite;
    private String entreprise;
    private String contactRecruteur;
    private int recruiterId;

    public void setRecruiterId(int recruiterId) {
        this.recruiterId = recruiterId;
    }

    public int getRecruiterId() {
        return recruiterId;
    }

    // Constructor without ID (for insert)
    public OffreEmploi(String titre, String description, double salaire,
                       String typeContrat, String localisation,
                       LocalDateTime datePublication, LocalDateTime dateExpiration,
                       String niveauQualification, String experienceRequise,
                       String competencesRequises, String secteurActivite,
                       String entreprise, String contactRecruteur, int recruiterId) {

        this.titre = titre;
        this.description = description;
        this.salaire = salaire;
        this.typeContrat = typeContrat;
        this.localisation = localisation;
        this.datePublication = datePublication;
        this.dateExpiration = dateExpiration;
        this.niveauQualification = niveauQualification;
        this.experienceRequise = experienceRequise;
        this.competencesRequises = competencesRequises;
        this.secteurActivite = secteurActivite;
        this.entreprise = entreprise;
        this.contactRecruteur = contactRecruteur;
        this.recruiterId = recruiterId;
    }

    // Constructor with ID (for update)
    public OffreEmploi(int id, String titre, String description, double salaire,
                       String typeContrat, String localisation,
                       LocalDateTime datePublication, LocalDateTime dateExpiration,
                       String niveauQualification, String experienceRequise,
                       String competencesRequises, String secteurActivite,
                       String entreprise, String contactRecruteur, int recruiterId) {

        this(titre, description, salaire, typeContrat, localisation,
                datePublication, dateExpiration, niveauQualification,
                experienceRequise, competencesRequises,
                secteurActivite, entreprise, contactRecruteur, recruiterId);

        this.id = id;
    }

    // Getters
    public int getId() { return id; }
    public String getTitre() { return titre; }
    public String getDescription() { return description; }
    public double getSalaire() { return salaire; }
    public String getTypeContrat() { return typeContrat; }
    public String getLocalisation() { return localisation; }
    public LocalDateTime getDatePublication() { return datePublication; }
    public LocalDateTime getDateExpiration() { return dateExpiration; }
    public String getNiveauQualification() { return niveauQualification; }
    public String getExperienceRequise() { return experienceRequise; }
    public String getCompetencesRequises() { return competencesRequises; }
    public String getSecteurActivite() { return secteurActivite; }
    public String getEntreprise() { return entreprise; }
    public String getContactRecruteur() { return contactRecruteur; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setTitre(String titre) { this.titre = titre; }
    public void setDescription(String description) { this.description = description; }
    public void setSalaire(double salaire) { this.salaire = salaire; }
    public void setTypeContrat(String typeContrat) { this.typeContrat = typeContrat; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }
    public void setDatePublication(LocalDateTime datePublication) { this.datePublication = datePublication; }
    public void setDateExpiration(LocalDateTime dateExpiration) { this.dateExpiration = dateExpiration; }
    public void setNiveauQualification(String niveauQualification) { this.niveauQualification = niveauQualification; }
    public void setExperienceRequise(String experienceRequise) { this.experienceRequise = experienceRequise; }
    public void setCompetencesRequises(String competencesRequises) { this.competencesRequises = competencesRequises; }
    public void setSecteurActivite(String secteurActivite) { this.secteurActivite = secteurActivite; }
    public void setEntreprise(String entreprise) { this.entreprise = entreprise; }
    public void setContactRecruteur(String contactRecruteur) { this.contactRecruteur = contactRecruteur; }


    @Override
    public String toString() {
        return "OffreEmploi{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", salaire=" + salaire +
                ", typeContrat='" + typeContrat + '\'' +
                ", localisation='" + localisation + '\'' +
                ", datePublication=" + datePublication +
                ", dateExpiration=" + dateExpiration +
                ", niveauQualification='" + niveauQualification + '\'' +
                ", experienceRequise='" + experienceRequise + '\'' +
                ", competencesRequises='" + competencesRequises + '\'' +
                ", secteurActivite='" + secteurActivite + '\'' +
                ", entreprise='" + entreprise + '\'' +
                ", contactRecruteur='" + contactRecruteur + '\'' +
                '}';
    }

}
