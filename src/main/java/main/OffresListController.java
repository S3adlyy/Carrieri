package main;

import entities.OffreEmploi;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import services.OffreEmploiService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class OffresListController {

    @FXML private FlowPane flowOffers;
    @FXML private TextField txtSearch;
    @FXML private Label lblStatus;

    private final OffreEmploiService service = new OffreEmploiService();
    private final ObservableList<OffreEmploi> data = FXCollections.observableArrayList();
    private final ObservableList<OffreEmploi> allData = FXCollections.observableArrayList();

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public void initialize() {
        // live search
        txtSearch.textProperty().addListener((obs, oldV, newV) -> handleSearch());
        refreshList();
    }

    public void refreshList() {
        try {
            allData.setAll(service.read());
            data.setAll(allData);
            updateStatus();
            renderCards();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les offres :\n" + e.getMessage());
            data.clear();
            allData.clear();
            updateStatus();
            renderCards();
        }
    }

    @FXML
    private void handleSearch() {
        String search = safe(txtSearch.getText()).toLowerCase().trim();

        if (search.isEmpty()) {
            data.setAll(allData);
        } else {
            data.setAll(allData.filtered(o ->
                    safe(o.getTitre()).toLowerCase().contains(search) ||
                            safe(o.getEntreprise()).toLowerCase().contains(search) ||
                            safe(o.getLocalisation()).toLowerCase().contains(search) ||
                            safe(o.getSecteurActivite()).toLowerCase().contains(search) ||
                            safe(o.getCompetencesRequises()).toLowerCase().contains(search)
            ));
        }

        updateStatus();
        renderCards();
    }

    private void updateStatus() {
        lblStatus.setText(data.size() + " offres trouvées");
    }

    private void renderCards() {
        flowOffers.getChildren().clear();

        if (data.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(40));
            Label t = new Label("Aucune offre à afficher");
            t.getStyleClass().add("offer-title");
            Label s = new Label("Essayez de modifier la recherche ou ajoutez une nouvelle offre.");
            s.getStyleClass().add("offer-info");
            empty.getChildren().addAll(t, s);
            flowOffers.getChildren().add(empty);
            return;
        }

        for (OffreEmploi offre : data) {
            VBox card = createOfferCard(offre);
            flowOffers.getChildren().add(card);

            FadeTransition fade = new FadeTransition(Duration.millis(450), card);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();
        }
    }

    // ===================== CARD UI =====================

    private VBox createOfferCard(OffreEmploi offre) {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("offer-card", "offer-card-premium");
        card.setMaxWidth(560);

        // ===== Header (Title + badge) =====
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);

        Label title = new Label(emptyAsDash(offre.getTitre()));
        title.getStyleClass().add("offer-title-premium");

        Label subtitle = new Label(emptyAsDash(offre.getEntreprise()) + "  •  " + emptyAsDash(offre.getSecteurActivite()));
        subtitle.getStyleClass().add("offer-subtitle-premium");

        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label(emptyAsDash(offre.getTypeContrat()));
        badge.getStyleClass().add("offer-badge-premium");

        header.getChildren().addAll(titleBox, spacer, badge);

        // ===== Short description (nice emphasis) =====
        Label desc = new Label(trimTo(emptyAsDash(offre.getDescription()), 140));
        desc.getStyleClass().add("offer-desc-premium");
        desc.setWrapText(true);

        // ===== 2-column info grid =====
        GridPane grid = new GridPane();
        grid.getStyleClass().add("offer-grid");
        grid.setHgap(18);
        grid.setVgap(12);

        String pub = (offre.getDatePublication() == null) ? "—" : dateFmt.format(offre.getDatePublication());
        String exp = (offre.getDateExpiration() == null) ? "—" : dateFmt.format(offre.getDateExpiration());

        // Row 0
        grid.add(pillLine("💰", "Salaire", String.format("%.0f DT", offre.getSalaire())), 0, 0);
        grid.add(pillLine("📍", "Localisation", emptyAsDash(offre.getLocalisation())), 1, 0);

        // Row 1
        grid.add(pillLine("🕒", "Publication", pub), 0, 1);
        grid.add(pillLine("🗓", "Expiration", exp), 1, 1);

        // Row 2
        grid.add(pillLine("🎓", "Niveau", emptyAsDash(offre.getNiveauQualification())), 0, 2);
        grid.add(pillLine("⏳", "Expérience", emptyAsDash(offre.getExperienceRequise())), 1, 2);

        // Row 3 (Contact + Type contrat)
        grid.add(pillLine("✉", "Contact", emptyAsDash(offre.getContactRecruteur())), 0, 3);
        grid.add(pillLine("📄", "Contrat", emptyAsDash(offre.getTypeContrat())), 1, 3);

        // ===== Skills chips =====
        VBox skillsBox = new VBox(8);
        Label skillsTitle = new Label("Compétences");
        skillsTitle.getStyleClass().add("section-title");

        FlowPane chips = new FlowPane(8, 8);
        chips.getStyleClass().add("chips-row");
        addSkillChipsPretty(chips, emptyAsDash(offre.getCompetencesRequises()));

        skillsBox.getChildren().addAll(skillsTitle, chips);

        // ===== Separator =====
        Separator sep = new Separator();
        sep.getStyleClass().add("offer-sep-premium");

        // ===== Bottom actions =====
        HBox bottom = new HBox(10);
        bottom.setAlignment(Pos.CENTER_RIGHT);

        Button btnPostuler = new Button("Postuler");
        btnPostuler.getStyleClass().add("btn-postuler-premium");
        btnPostuler.setGraphic(new Label("✈"));
        btnPostuler.setOnAction(e ->
                OffresShellController.getInstance().showPostuler(offre.getId(), offre.getTitre())
        );

        Button btnEdit = new Button();
        btnEdit.getStyleClass().addAll("icon-btn-premium", "icon-btn-edit");
        btnEdit.setGraphic(new Label("✎"));
        btnEdit.setTooltip(new Tooltip("Modifier"));
        btnEdit.setOnAction(e -> openEditDialog(offre));

        Button btnDelete = new Button();
        btnDelete.getStyleClass().addAll("icon-btn-premium", "icon-btn-delete");
        btnDelete.setGraphic(new Label("🗑"));
        btnDelete.setTooltip(new Tooltip("Supprimer"));
        btnDelete.setOnAction(e -> handleDelete(offre));

        bottom.getChildren().addAll(btnPostuler, btnEdit, btnDelete);

        card.getChildren().addAll(header, desc, grid, skillsBox, sep, bottom);
        return card;
    }
    private void addSkillChipsPretty(FlowPane chips, String competences) {
        chips.getChildren().clear();

        if (competences == null || competences.isBlank() || "—".equals(competences.trim())) {
            Label chip = new Label("Aucune");
            chip.getStyleClass().add("chip-muted");
            chips.getChildren().add(chip);
            return;
        }

        String[] parts = competences.split(",");
        for (String p : parts) {
            String c = p.trim();
            if (c.isEmpty()) continue;

            Label chip = new Label(c);
            chip.getStyleClass().add("chip-premium");
            chips.getChildren().add(chip);
        }
    }

    private HBox attrLine(String icon, String label, String value) {
        Label ic = new Label(icon);
        ic.getStyleClass().add("attr-icon");

        Label l = new Label(label + " :");
        l.getStyleClass().add("attr-label");

        Label v = new Label(value == null || value.isBlank() ? "—" : value);
        v.getStyleClass().add("attr-value");
        v.setWrapText(true);

        HBox row = new HBox(10, ic, l, v);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox pillLine(String icon, String label, String value) {
        Label ic = new Label(icon);
        ic.getStyleClass().add("pill-icon");

        VBox text = new VBox(2);
        Label l = new Label(label);
        l.getStyleClass().add("pill-label");
        Label v = new Label(value == null || value.isBlank() ? "—" : value);
        v.getStyleClass().add("pill-value");
        v.setWrapText(true);

        text.getChildren().addAll(l, v);

        HBox pill = new HBox(10, ic, text);
        pill.getStyleClass().add("pill");
        pill.setAlignment(Pos.CENTER_LEFT);
        return pill;
    }
    


    // ===================== DELETE =====================

    private void handleDelete(OffreEmploi offre) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer cette offre ?");
        confirm.setContentText("Titre : " + safe(offre.getTitre()) + "\nEntreprise : " + safe(offre.getEntreprise()));

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        try {
            service.supprimer(offre.getId());
            refreshList();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Offre supprimée.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression impossible :\n" + e.getMessage());
        }
    }

    // ===================== EDIT DIALOG =====================

    private void openEditDialog(OffreEmploi offre) {
        Dialog<OffreEmploi> dialog = new Dialog<>();
        dialog.setTitle("Modifier l'offre");
        dialog.setHeaderText(null);

        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(18));

        TextField titre = new TextField(safe(offre.getTitre()));
        TextArea desc = new TextArea(safe(offre.getDescription()));
        desc.setPrefRowCount(4);

        TextField salaire = new TextField(String.valueOf((int) offre.getSalaire()));
        TextField localisation = new TextField(safe(offre.getLocalisation()));
        TextField entreprise = new TextField(safe(offre.getEntreprise()));
        TextField contact = new TextField(safe(offre.getContactRecruteur()));

        ComboBox<String> typeContrat = new ComboBox<>();
        typeContrat.getItems().setAll("CDI", "CDD", "Stage", "Freelance", "Alternance");
        typeContrat.setValue(safe(offre.getTypeContrat()).isEmpty() ? null : offre.getTypeContrat());

        DatePicker expDate = new DatePicker(
                offre.getDateExpiration() == null ? LocalDate.now().plusDays(7) : offre.getDateExpiration().toLocalDate()
        );

        grid.addRow(0, new Label("Titre"), titre);
        grid.addRow(1, new Label("Type contrat"), typeContrat);
        grid.addRow(2, new Label("Salaire (DT)"), salaire);
        grid.addRow(3, new Label("Localisation"), localisation);
        grid.addRow(4, new Label("Entreprise"), entreprise);
        grid.addRow(5, new Label("Contact (email)"), contact);
        grid.addRow(6, new Label("Expiration"), expDate);
        grid.addRow(7, new Label("Description"), desc);

        dialog.getDialogPane().setContent(grid);

        Node okBtn = dialog.getDialogPane().lookupButton(saveBtn);
        okBtn.setDisable(titre.getText().trim().isEmpty());
        titre.textProperty().addListener((obs, o, n) -> okBtn.setDisable(n.trim().isEmpty()));

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;

            if (titre.getText().trim().isEmpty()) { showAlert(Alert.AlertType.ERROR, "Erreur", "Le titre est obligatoire."); return null; }
            if (desc.getText().trim().isEmpty())  { showAlert(Alert.AlertType.ERROR, "Erreur", "La description est obligatoire."); return null; }
            if (typeContrat.getValue() == null || typeContrat.getValue().trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Le type de contrat est obligatoire."); return null;
            }
            if (!isValidEmail(contact.getText().trim())) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Email invalide."); return null;
            }

            double sal;
            try {
                sal = Double.parseDouble(salaire.getText().trim());
                if (sal <= 0) throw new RuntimeException();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Salaire invalide (ex: 2500).");
                return null;
            }

            LocalDateTime newExp = expDate.getValue().atTime(
                    (offre.getDateExpiration() == null ? 23 : offre.getDateExpiration().getHour()),
                    (offre.getDateExpiration() == null ? 59 : offre.getDateExpiration().getMinute())
            );

            return new OffreEmploi(
                    offre.getId(),
                    titre.getText().trim(),
                    desc.getText().trim(),
                    sal,
                    typeContrat.getValue().trim(),
                    localisation.getText().trim(),
                    offre.getDatePublication(),
                    newExp,
                    offre.getNiveauQualification(),
                    offre.getExperienceRequise(),
                    offre.getCompetencesRequises(),
                    offre.getSecteurActivite(),
                    entreprise.getText().trim(),
                    contact.getText().trim()
            );
        });

        Optional<OffreEmploi> res = dialog.showAndWait();
        if (res.isEmpty()) return;

        try {
            service.modifier(res.get());
            refreshList();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Offre modifiée.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Modification impossible :\n" + e.getMessage());
        }
    }

    // ===================== UI HELPERS =====================

    private HBox iconLine(String icon, String text) {
        Label i = new Label(icon);
        i.getStyleClass().add("mini-icon");

        Label t = new Label(text == null || text.isBlank() ? "—" : text);
        t.getStyleClass().add("mini-text");
        t.setWrapText(true);

        HBox row = new HBox(8, i, t);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Separator separator() {
        Separator s = new Separator();
        s.getStyleClass().add("offer-sep");
        return s;
    }

    private void addCompetenceChips(FlowPane chips, String competences) {
        chips.getChildren().clear();

        if (competences == null || competences.isBlank()) {
            Label chip = new Label("Aucune compétence");
            chip.getStyleClass().add("chip");
            chips.getChildren().add(chip);
            return;
        }

        String[] parts = competences.split(",");
        int shown = 0;

        for (String p : parts) {
            String c = p.trim();
            if (c.isEmpty()) continue;
            if (shown == 3) break;

            Label chip = new Label(c);
            chip.getStyleClass().add("chip");
            chips.getChildren().add(chip);
            shown++;
        }

        int extra = countNonEmpty(parts) - shown;
        if (extra > 0) {
            Label more = new Label("+" + extra);
            more.getStyleClass().add("chip-muted");
            chips.getChildren().add(more);
        }
    }

    private int countNonEmpty(String[] arr) {
        int c = 0;
        for (String s : arr) if (s != null && !s.trim().isEmpty()) c++;
        return c;
    }

    private String trimTo(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max).trim() + "...";
    }

    // ===================== VALIDATION + ALERTS =====================

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    private String emptyAsDash(String s) {
        return (s == null || s.trim().isEmpty()) ? "—" : s.trim();
    }
}
