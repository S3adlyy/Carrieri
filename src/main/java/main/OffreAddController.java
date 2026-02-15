package main;

import entities.OffreEmploi;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.OffreEmploiService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class OffreAddController {

    @FXML private TextField titreField;
    @FXML private TextArea descriptionField;
    @FXML private TextField salaireField;
    @FXML private TextField typeContratField;
    @FXML private TextField localisationField;
    @FXML private DatePicker expirationPicker;
    @FXML private TextField niveauField;
    @FXML private TextField experienceField;
    @FXML private TextField competencesField;
    @FXML private TextField secteurField;
    @FXML private TextField entrepriseField;
    @FXML private TextField contactField;

    @FXML private Label errorLabel;

    private final OffreEmploiService service = new OffreEmploiService();

    @FXML
    private void save() {
        hideError();

        String titre = safe(titreField.getText());
        String desc = safe(descriptionField.getText());

        if (titre.isEmpty() || desc.isEmpty()) {
            showError("Titre et description sont obligatoires.");
            return;
        }

        LocalDate exp = expirationPicker.getValue();
        if (exp == null) {
            showError("Date expiration est obligatoire.");
            return;
        }

        double salaire;
        try {
            salaire = Double.parseDouble(safe(salaireField.getText()));
        } catch (Exception e) {
            showError("Salaire invalide (ex: 2500).");
            return;
        }

        LocalDateTime datePub = LocalDateTime.now();
        LocalDateTime dateExp = exp.atStartOfDay();

        OffreEmploi o = new OffreEmploi(
                0,
                titre,
                desc,
                salaire,
                safe(typeContratField.getText()),
                safe(localisationField.getText()),
                datePub,
                dateExp,
                safe(niveauField.getText()),
                safe(experienceField.getText()),
                safe(competencesField.getText()),
                safe(secteurField.getText()),
                safe(entrepriseField.getText()),
                safe(contactField.getText())
        );

        try {
            service.ajouter(o);
            reset();
            showInfo("✅ Offre ajoutée !");
        } catch (SQLException e) {
            showError("Erreur DB: " + e.getMessage());
        }
    }

    @FXML
    private void reset() {
        titreField.clear();
        descriptionField.clear();
        salaireField.clear();
        typeContratField.clear();
        localisationField.clear();
        expirationPicker.setValue(null);
        niveauField.clear();
        experienceField.clear();
        competencesField.clear();
        secteurField.clear();
        entrepriseField.clear();
        contactField.clear();
        hideError();
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

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
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
