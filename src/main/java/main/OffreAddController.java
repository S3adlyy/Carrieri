package main;

import entities.OffreEmploi;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.OffreEmploiService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OffreAddController {

    private static final PseudoClass ERROR_CLASS = PseudoClass.getPseudoClass("error");

    // === FXML ids (MUST match offre-add.fxml) ===
    @FXML private TextField txtTitre;
    @FXML private TextArea txtDescription;
    @FXML private Label lblCharCount;

    @FXML private TextField txtSalaire;
    @FXML private TextField txtLocalisation;
    @FXML private TextField txtSecteur;

    @FXML private ComboBox<String> comboTypeContrat;
    @FXML private ComboBox<String> comboQualification;

    @FXML private TextField txtExperience;
    @FXML private TextField txtCompetences;

    @FXML private TextField txtEntreprise;
    @FXML private TextField txtContact;

    @FXML private DatePicker dpExpirationDate;
    @FXML private Spinner<Integer> spExpirationTime;      // 0-23
    @FXML private Spinner<Integer> spExpirationMinute;    // 0-59

    private final OffreEmploiService service = new OffreEmploiService();

    @FXML
    public void initialize() {

        // ====== combos ======
        comboTypeContrat.getItems().setAll("CDI", "CDD", "Stage", "Freelance", "Alternance");
        comboQualification.getItems().setAll("Bac", "Bac+2", "Bac+3", "Bac+5", "Doctorat");

        // ====== spinners ======
        spExpirationTime.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 23));
        spExpirationMinute.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 59, 5));
        spExpirationTime.setEditable(true);
        spExpirationMinute.setEditable(true);

        // ====== default date ======
        dpExpirationDate.setValue(LocalDate.now().plusDays(7));

        // ====== salary: allow only digits + optional .xx ======
        txtSalaire.setTextFormatter(new TextFormatter<>(change -> {
            String next = change.getControlNewText().trim();
            if (next.isEmpty()) return change;
            return next.matches("\\d{0,9}(\\.\\d{0,2})?") ? change : null;
        }));

        // ====== description counter + hard limit 2000 ======
        txtDescription.textProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) newV = "";
            if (newV.length() > 2000) {
                txtDescription.setText(newV.substring(0, 2000));
                return;
            }
            if (lblCharCount != null) {
                lblCharCount.setText(newV.length() + "/2000");
            }
        });
        if (lblCharCount != null) {
            lblCharCount.setText((txtDescription.getText() == null ? 0 : txtDescription.getText().length()) + "/2000");
        }
    }

    @FXML
    private void handleSave() {
        clearErrors();

        // ====== read values ======
        String titre = safe(txtTitre.getText());
        String description = safe(txtDescription.getText());
        String typeContrat = comboTypeContrat.getValue() == null ? "" : comboTypeContrat.getValue().trim();
        String entreprise = safe(txtEntreprise.getText());

        String localisation = safe(txtLocalisation.getText());
        String secteur = safe(txtSecteur.getText());
        String niveau = comboQualification.getValue() == null ? "" : comboQualification.getValue().trim();

        String experience = safe(txtExperience.getText());
        String competences = safe(txtCompetences.getText());
        String contact = safe(txtContact.getText());

        LocalDate datePart = dpExpirationDate.getValue();
        Integer hours = spExpirationTime.getValue();
        Integer minutes = spExpirationMinute.getValue();

        // ====== validations (precise + popups + highlight field) ======
        if (titre.isEmpty())          { fail(txtTitre, "Le titre est obligatoire."); return; }
        if (description.isEmpty())    { fail(txtDescription, "La description est obligatoire."); return; }
        if (typeContrat.isEmpty())    { fail(comboTypeContrat, "Le type de contrat est obligatoire."); return; }
        if (entreprise.isEmpty())     { fail(txtEntreprise, "Le nom de l'entreprise est obligatoire."); return; }

        if (localisation.isEmpty())   { fail(txtLocalisation, "La localisation est obligatoire."); return; }
        if (secteur.isEmpty())        { fail(txtSecteur, "Le secteur d'activité est obligatoire."); return; }
        if (niveau.isEmpty())         { fail(comboQualification, "Le niveau de qualification est obligatoire."); return; }

        if (experience.isEmpty())     { fail(txtExperience, "L'expérience requise est obligatoire."); return; }
        if (!experience.matches("^\\d+(\\s*ans)?$|^\\d+\\s*-\\s*\\d+(\\s*ans)?$")) {
            fail(txtExperience, "Expérience invalide (ex: 2, 2-5, 3 ans).");
            return;
        }

        if (competences.isEmpty())    { fail(txtCompetences, "Les compétences sont obligatoires."); return; }
        if (!competences.contains(",")) {
            fail(txtCompetences, "Sépare les compétences par des virgules (ex: Java, SQL, React).");
            return;
        }

        if (contact.isEmpty())        { fail(txtContact, "Le contact recruteur est obligatoire."); return; }
        if (!isValidEmail(contact))   { fail(txtContact, "Email invalide (ex: recrutement@entreprise.tn)."); return; }

        if (safe(txtSalaire.getText()).isEmpty()) { fail(txtSalaire, "Le salaire est obligatoire."); return; }

        double salaire;
        try {
            salaire = Double.parseDouble(safe(txtSalaire.getText()));
            if (salaire <= 0) {
                fail(txtSalaire, "Le salaire doit être > 0 (ex: 2500).");
                return;
            }
        } catch (Exception e) {
            fail(txtSalaire, "Le salaire doit être un nombre valide (ex: 2500).");
            return;
        }

        if (datePart == null) { fail(dpExpirationDate, "La date d'expiration est obligatoire."); return; }
        if (datePart.isBefore(LocalDate.now())) {
            fail(dpExpirationDate, "La date d'expiration doit être dans le futur.");
            return;
        }
        if (hours == null || minutes == null) {
            showErrorPopup("Heure/minute d'expiration invalide.");
            return;
        }

        LocalDateTime expirationDateTime = LocalDateTime.of(
                datePart.getYear(),
                datePart.getMonthValue(),
                datePart.getDayOfMonth(),
                hours,
                minutes
        );

        // (Optionnel mais clean) : si même jour, empêcher une heure déjà passée
        if (datePart.equals(LocalDate.now()) && expirationDateTime.isBefore(LocalDateTime.now())) {
            fail(dpExpirationDate, "L'expiration doit être dans le futur (date + heure).");
            return;
        }

        // ====== create + insert ======
        OffreEmploi offre = new OffreEmploi(
                titre,
                description,
                salaire,
                typeContrat,
                localisation,
                LocalDateTime.now(),
                expirationDateTime,
                niveau,
                experience,
                competences,
                secteur,
                entreprise,
                contact
        );

        try {
            service.ajouter(offre); // insert SQL ok :contentReference[oaicite:1]{index=1}
            handleReset();
            showInfo("✅ Offre ajoutée avec succès !");
        } catch (SQLException e) {
            showErrorPopup("Erreur base de données : " + e.getMessage());
        }
    }

    @FXML
    private void handleReset() {
        clearErrors();

        txtTitre.clear();
        txtDescription.clear();
        txtSalaire.clear();
        txtLocalisation.clear();
        txtSecteur.clear();

        comboTypeContrat.getSelectionModel().clearSelection();
        comboQualification.getSelectionModel().clearSelection();

        txtExperience.clear();
        txtCompetences.clear();

        txtEntreprise.clear();
        txtContact.clear();

        dpExpirationDate.setValue(LocalDate.now().plusDays(7));
        spExpirationTime.getValueFactory().setValue(23);
        spExpirationMinute.getValueFactory().setValue(59);

        if (lblCharCount != null) lblCharCount.setText("0/2000");
    }

    // ==================== helpers ====================

    private void fail(Control field, String message) {
        markError(field, true);
        showErrorPopup(message);
        field.requestFocus();
    }

    private void markError(Control c, boolean on) {
        if (c != null) c.pseudoClassStateChanged(ERROR_CLASS, on);
    }

    private void clearErrors() {
        List<Control> all = new ArrayList<>();
        all.add(txtTitre);
        all.add(txtDescription);
        all.add(txtSalaire);
        all.add(txtLocalisation);
        all.add(txtSecteur);
        all.add(comboTypeContrat);
        all.add(comboQualification);
        all.add(txtExperience);
        all.add(txtCompetences);
        all.add(txtEntreprise);
        all.add(txtContact);
        all.add(dpExpirationDate);

        for (Control c : all) markError(c, false);
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    private void showErrorPopup(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur de saisie");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
