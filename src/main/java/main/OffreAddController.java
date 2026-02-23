package main;

import entities.OffreEmploi;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.OffreEmploiService;
import javafx.util.StringConverter;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OffreAddController {

    private static final PseudoClass ERROR_CLASS = PseudoClass.getPseudoClass("error");
    private static final DateTimeFormatter FR_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");


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

    // Labels d'erreur
    @FXML private Label errTitre;
    @FXML private Label errDescription;
    @FXML private Label errTypeContrat;
    @FXML private Label errEntreprise;
    @FXML private Label errSalaire;
    @FXML private Label errLocalisation;
    @FXML private Label errSecteur;
    @FXML private Label errQualification;
    @FXML private Label errExperience;
    @FXML private Label errCompetences;
    @FXML private Label errContact;
    @FXML private Label errExpiration;

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

        // ===== DatePicker: format + vraie validation =====
        dpExpirationDate.setValue(LocalDate.now().plusDays(7));
        dpExpirationDate.setPromptText("jj/mm/aaaa");

// Force l’affichage/saisie en dd/MM/yyyy
        dpExpirationDate.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return (date == null) ? "" : FR_FORMAT.format(date);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null) return null;
                String t = text.trim();
                if (t.isEmpty()) return null;

                try {
                    return LocalDate.parse(t, FR_FORMAT); // vraie date (ex: 31/02 -> erreur)
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        });

// Bloquer les dates passées directement dans le calendrier
        dpExpirationDate.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) return;

                // interdit passé (et tu peux mettre <= si tu veux bloquer aujourd’hui aussi)
                if (item.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setOpacity(0.35);
                }
            }
        });

// Si l'utilisateur tape au clavier, vérifier quand il quitte le champ
        dpExpirationDate.getEditor().focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow) {
                LocalDate parsed = dpExpirationDate.getConverter().fromString(dpExpirationDate.getEditor().getText());
                if (parsed == null) {
                    // date invalide -> reset + erreur
                    dpExpirationDate.setValue(null);
                    dpExpirationDate.getEditor().clear();
                    showError(errExpiration, "Date invalide. Format attendu : jj/mm/aaaa (ex: 23/02/2026).");
                    markError(dpExpirationDate, true);
                } else {
                    dpExpirationDate.setValue(parsed);
                    hideError(errExpiration);
                    markError(dpExpirationDate, false);
                }
            }
        });

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

        // ====== validations (precise + labels rouges) ======
        boolean hasError = false;

        if (titre.isEmpty()) {
            showError(errTitre, "Le titre est obligatoire.");
            markError(txtTitre, true);
            hasError = true;
        } else if (titre.matches("^\\d.*")) {
            showError(errTitre, "Le titre ne peut pas commencer par un chiffre.");
            markError(txtTitre, true);
            hasError = true;
        } else {
            // Vérifier l'unicité du titre
            try {
                if (service.existsByTitre(titre)) {
                    showError(errTitre, "Ce titre existe déjà. Veuillez choisir un titre différent.");
                    markError(txtTitre, true);
                    hasError = true;
                }
            } catch (SQLException e) {
                showError(errTitre, "Erreur lors de la vérification du titre.");
                markError(txtTitre, true);
                hasError = true;
            }
        }

        if (description.isEmpty()) {
            showError(errDescription, "La description est obligatoire.");
            markError(txtDescription, true);
            hasError = true;
        } else if (description.matches("^\\d.*")) {
            showError(errDescription, "La description ne peut pas commencer par un chiffre.");
            markError(txtDescription, true);
            hasError = true;
        }

        if (typeContrat.isEmpty()) {
            showError(errTypeContrat, "Le type de contrat est obligatoire.");
            markError(comboTypeContrat, true);
            hasError = true;
        }

        if (entreprise.isEmpty()) {
            showError(errEntreprise, "Le nom de l'entreprise est obligatoire.");
            markError(txtEntreprise, true);
            hasError = true;
        }

        if (localisation.isEmpty()) {
            showError(errLocalisation, "La localisation est obligatoire.");
            markError(txtLocalisation, true);
            hasError = true;
        }

        if (secteur.isEmpty()) {
            showError(errSecteur, "Le secteur d'activité est obligatoire.");
            markError(txtSecteur, true);
            hasError = true;
        }

        if (niveau.isEmpty()) {
            showError(errQualification, "Le niveau de qualification est obligatoire.");
            markError(comboQualification, true);
            hasError = true;
        }

        if (experience.isEmpty()) {
            showError(errExperience, "L'expérience requise est obligatoire.");
            markError(txtExperience, true);
            hasError = true;
        } else if (!experience.matches("^\\d+(\\s*ans)?$|^\\d+\\s*-\\s*\\d+(\\s*ans)?$")) {
            showError(errExperience, "Expérience invalide (ex: 2, 2-5, 3 ans).");
            markError(txtExperience, true);
            hasError = true;
        }

        if (competences.isEmpty()) {
            showError(errCompetences, "Les compétences sont obligatoires.");
            markError(txtCompetences, true);
            hasError = true;
        } else if (!competences.contains(",")) {
            showError(errCompetences, "Séparez les compétences par des virgules (ex: Java, SQL, React).");
            markError(txtCompetences, true);
            hasError = true;
        }

        if (contact.isEmpty()) {
            showError(errContact, "Le contact recruteur est obligatoire.");
            markError(txtContact, true);
            hasError = true;
        } else if (!isValidEmail(contact)) {
            showError(errContact, "Email invalide (ex: recrutement@entreprise.tn).");
            markError(txtContact, true);
            hasError = true;
        }

        if (safe(txtSalaire.getText()).isEmpty()) {
            showError(errSalaire, "Le salaire est obligatoire.");
            markError(txtSalaire, true);
            hasError = true;
        } else {
            try {
                double salaire = Double.parseDouble(safe(txtSalaire.getText()));
                if (salaire <= 0) {
                    showError(errSalaire, "Le salaire doit être > 0 (ex: 2500).");
                    markError(txtSalaire, true);
                    hasError = true;
                }
            } catch (Exception e) {
                showError(errSalaire, "Le salaire doit être un nombre valide (ex: 2500).");
                markError(txtSalaire, true);
                hasError = true;
            }
        }

        if (datePart == null) {
            showError(errExpiration, "La date d'expiration est obligatoire.");
            markError(dpExpirationDate, true);
            hasError = true;
        } else if (datePart.isBefore(LocalDate.now())) {
            showError(errExpiration, "La date d'expiration doit être dans le futur.");
            markError(dpExpirationDate, true);
            hasError = true;
        } else if (hours == null || minutes == null) {
            showError(errExpiration, "Heure/minute d'expiration invalide.");
            hasError = true;
        } else {
            LocalDateTime expirationDateTime = LocalDateTime.of(
                    datePart.getYear(),
                    datePart.getMonthValue(),
                    datePart.getDayOfMonth(),
                    hours,
                    minutes
            );

            if (datePart.equals(LocalDate.now()) && expirationDateTime.isBefore(LocalDateTime.now())) {
                showError(errExpiration, "L'expiration doit être dans le futur (date + heure).");
                markError(dpExpirationDate, true);
                hasError = true;
            }
        }

        // Si au moins une erreur, on arrête
        if (hasError) {
            return;
        }

        // ====== create + insert ======
        double salaire = Double.parseDouble(safe(txtSalaire.getText()));
        LocalDateTime expirationDateTime = LocalDateTime.of(
                datePart.getYear(),
                datePart.getMonthValue(),
                datePart.getDayOfMonth(),
                hours,
                minutes
        );

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
            service.ajouter(offre);
            handleReset();
            showInfo("✅ Offre ajoutée avec succès !");
        } catch (SQLException e) {
            // Vérifier si c'est une erreur d'unicité du titre
            if (e.getMessage().contains("existe déjà")) {
                showError(errTitre, e.getMessage());
                markError(txtTitre, true);
                txtTitre.requestFocus();
            } else {
                showErrorPopup("Erreur base de données : " + e.getMessage());
            }
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

    private void showError(Label errorLabel, String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void hideError(Label errorLabel) {
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    private void markError(Control c, boolean on) {
        if (c != null) c.pseudoClassStateChanged(ERROR_CLASS, on);
    }

    private void clearErrors() {
        // Clear all error labels
        hideError(errTitre);
        hideError(errDescription);
        hideError(errTypeContrat);
        hideError(errEntreprise);
        hideError(errSalaire);
        hideError(errLocalisation);
        hideError(errSecteur);
        hideError(errQualification);
        hideError(errExperience);
        hideError(errCompetences);
        hideError(errContact);
        hideError(errExpiration);

        // Clear pseudo-class states
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
