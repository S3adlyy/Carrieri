package main;

import entities.OffreEmploi;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.OffreEmploiService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class OffreAddController {

    // Form fields
    @FXML private TextField titreField;
    @FXML private TextArea descriptionField;
    @FXML private TextField salaireField;
    @FXML private TextField typeContratField;
    @FXML private TextField localisationField;
    @FXML private DatePicker dpExpirationDate;
    @FXML private Spinner<Integer> spExpirationTime;      // Hours (0-23)
    @FXML private Spinner<Integer> spExpirationMinute;    // Minutes (0-59)
    @FXML private TextField niveauField;
    @FXML private TextField experienceField;
    @FXML private TextField competencesField;
    @FXML private TextField secteurField;
    @FXML private TextField entrepriseField;
    @FXML private TextField contactField;

    @FXML private Label errorLabel;

    private final OffreEmploiService service = new OffreEmploiService();

    @FXML
    public void initialize() {
        // Set default expiration time to end of day (23:59)
        spExpirationTime.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 23));
        spExpirationMinute.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 59, 5));

        // Optional: make spinners editable and add arrow buttons styling if needed
        spExpirationTime.setEditable(true);
        spExpirationMinute.setEditable(true);
    }

    @FXML
    private void save() {
        hideError();

        // ===================== INPUT VALIDATION =====================
        String titre = safe(titreField.getText());
        String description = safe(descriptionField.getText());
        String typeContrat = safe(typeContratField.getText());
        String localisation = safe(localisationField.getText());
        String niveau = safe(niveauField.getText());
        String experience = safe(experienceField.getText());
        String competences = safe(competencesField.getText());
        String secteur = safe(secteurField.getText());
        String entreprise = safe(entrepriseField.getText());
        String contact = safe(contactField.getText());

        // Required text fields
        if (titre.isEmpty())          { showError("Le titre est obligatoire."); return; }
        if (description.isEmpty())    { showError("La description est obligatoire."); return; }
        if (typeContrat.isEmpty())    { showError("Le type de contrat est obligatoire."); return; }
        if (localisation.isEmpty())   { showError("La localisation est obligatoire."); return; }
        if (niveau.isEmpty())         { showError("Le niveau de qualification est obligatoire."); return; }
        if (experience.isEmpty())     { showError("L'expérience requise est obligatoire."); return; }
        if (competences.isEmpty())    { showError("Les compétences sont obligatoires."); return; }
        if (secteur.isEmpty())        { showError("Le secteur d'activité est obligatoire."); return; }
        if (entreprise.isEmpty())     { showError("Le nom de l'entreprise est obligatoire."); return; }
        if (contact.isEmpty())        { showError("Le contact recruteur est obligatoire."); return; }

        // Email validation
        if (!isValidEmail(contact)) {
            showError("Format d'email invalide (ex: recrutement@entreprise.tn).");
            return;
        }

        // Salary validation
        double salaire;
        try {
            salaire = Double.parseDouble(safe(salaireField.getText()));
            if (salaire <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            showError("Le salaire doit être un nombre positif (ex: 2500).");
            return;
        }

        // Expiration date & time
        LocalDate datePart = dpExpirationDate.getValue();
        if (datePart == null) {
            showError("La date d'expiration est obligatoire.");
            return;
        }

        int hours = spExpirationTime.getValue();
        int minutes = spExpirationMinute.getValue();

        LocalDateTime expirationDateTime = LocalDateTime.of(
                datePart.getYear(),
                datePart.getMonthValue(),
                datePart.getDayOfMonth(),
                hours,
                minutes
        );

        LocalDateTime datePublication = LocalDateTime.now();

        // ===================== CREATE OFFRE OBJECT =====================
        OffreEmploi offre = new OffreEmploi(
                0,                      // id = 0 for new record
                titre,
                description,
                salaire,
                typeContrat,
                localisation,
                datePublication,
                expirationDateTime,
                niveau,
                experience,
                competences,
                secteur,
                entreprise,
                contact
        );

        try {
            service.ajouter(offre);
            handleReset();
            showInfo("✅ Offre ajoutée avec succès !");
        } catch (SQLException e) {
            showError("Erreur base de données : " + e.getMessage());
        }
    }

    @FXML
    private void handleReset() {
        titreField.clear();
        descriptionField.clear();
        salaireField.clear();
        typeContratField.clear();
        localisationField.clear();
        dpExpirationDate.setValue(null);
        spExpirationTime.getValueFactory().setValue(23);
        spExpirationMinute.getValueFactory().setValue(59);
        niveauField.clear();
        experienceField.clear();
        competencesField.clear();
        secteurField.clear();
        entrepriseField.clear();
        contactField.clear();
        hideError();
    }

    // ==================== HELPERS ====================

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private boolean isValidEmail(String email) {
        // Simple but effective email regex
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}