package entities;

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

    // Constructor without ID (for insert)
    public OffreEmploi(String titre, String description, double salaire,
                       String typeContrat, String localisation,
                       LocalDateTime datePublication, LocalDateTime dateExpiration,
                       String niveauQualification, String experienceRequise,
                       String competencesRequises, String secteurActivite,
                       String entreprise, String contactRecruteur) {

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
    }

    // Constructor with ID (for update)
    public OffreEmploi(int id, String titre, String description, double salaire,
                       String typeContrat, String localisation,
                       LocalDateTime datePublication, LocalDateTime dateExpiration,
                       String niveauQualification, String experienceRequise,
                       String competencesRequises, String secteurActivite,
                       String entreprise, String contactRecruteur) {

        this(titre, description, salaire, typeContrat, localisation,
                datePublication, dateExpiration, niveauQualification,
                experienceRequise, competencesRequises,
                secteurActivite, entreprise, contactRecruteur);

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
