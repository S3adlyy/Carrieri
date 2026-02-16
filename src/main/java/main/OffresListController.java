package main;

import entities.OffreEmploi;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
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
    private final ObservableList<OffreEmploi> allData = FXCollections.observableArrayList();
    private final ObservableList<OffreEmploi> filtered = FXCollections.observableArrayList();

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public void initialize() {
        txtSearch.textProperty().addListener((obs, o, n) -> applyFilter());
        refreshList();
    }

    public void refreshList() {
        try {
            allData.setAll(service.read());
            applyFilter();
        } catch (SQLException e) {
            allData.clear();
            filtered.clear();
            renderEmpty("Impossible de charger les offres :\n" + e.getMessage());
        }
    }

    private void applyFilter() {
        String q = safe(txtSearch.getText()).toLowerCase().trim();

        if (q.isEmpty()) {
            filtered.setAll(allData);
        } else {
            filtered.setAll(allData.filtered(o ->
                    safe(o.getTitre()).toLowerCase().contains(q) ||
                            safe(o.getEntreprise()).toLowerCase().contains(q) ||
                            safe(o.getLocalisation()).toLowerCase().contains(q) ||
                            safe(o.getSecteurActivite()).toLowerCase().contains(q) ||
                            safe(o.getCompetencesRequises()).toLowerCase().contains(q) ||
                            safe(o.getDescription()).toLowerCase().contains(q) ||
                            safe(o.getTypeContrat()).toLowerCase().contains(q) ||
                            safe(o.getNiveauQualification()).toLowerCase().contains(q) ||
                            safe(o.getExperienceRequise()).toLowerCase().contains(q) ||
                            safe(o.getContactRecruteur()).toLowerCase().contains(q)
            ));
        }

        lblStatus.setText(filtered.size() + " offres trouvées");
        renderCards();
    }

    private void renderCards() {
        flowOffers.getChildren().clear();

        if (filtered.isEmpty()) {
            renderEmpty("Aucune offre à afficher");
            return;
        }

        for (OffreEmploi offre : filtered) {
            VBox card = createOfferCard(offre);
            flowOffers.getChildren().add(card);

            FadeTransition ft = new FadeTransition(Duration.millis(350), card);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }

    private void renderEmpty(String message) {
        flowOffers.getChildren().clear();

        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));

        Label t = new Label(message);
        t.getStyleClass().add("offer-info");

        box.getChildren().add(t);
        flowOffers.getChildren().add(box);

        lblStatus.setText("0 offres trouvées");
    }

    // ===================== CARD UI (screenshot style + attributes) =====================

    private VBox createOfferCard(OffreEmploi offre) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("offer-card", "card-like-screenshot");
        card.setMaxWidth(560);

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.TOP_LEFT);

        Label title = new Label(emptyAsDash(offre.getTitre()));
        title.getStyleClass().add("c-title");
        title.setMaxWidth(380); // Limite la largeur pour laisser place au badge
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);

        Label badge = new Label(emptyAsDash(offre.getTypeContrat()));
        badge.getStyleClass().add("c-badge");

        header.getChildren().addAll(title, badge);

        // Company
        HBox companyRow = metaRowItem("🏢", emptyAsDash(offre.getEntreprise()));
        companyRow.getStyleClass().add("c-company-row");

        // Description
        Label desc = new Label(trimTo(emptyAsDash(offre.getDescription()), 2500));
        desc.getStyleClass().add("c-desc");
        desc.setWrapText(true);

        // Meta: location + expiration (small row)
        HBox meta = new HBox(18);
        meta.setAlignment(Pos.CENTER_LEFT);

        String expSmall = (offre.getDateExpiration() == null) ? "—" : dateFmt.format(offre.getDateExpiration());
        meta.getChildren().add(metaRowItem("📍", emptyAsDash(offre.getLocalisation())));

        // Details grid (Expiration next to Contact)
        GridPane details = new GridPane();
        details.getStyleClass().add("c-details");
        details.setHgap(18);
        details.setVgap(10);

        String pub = (offre.getDatePublication() == null) ? "—" : dateFmt.format(offre.getDatePublication());
        String expDetail = (offre.getDateExpiration() == null) ? "—" : dateFmt.format(offre.getDateExpiration());

        details.add(detailItem("🎓", "Niveau", emptyAsDash(offre.getNiveauQualification())), 0, 0);
        details.add(detailItem("⏳", "Expérience", emptyAsDash(offre.getExperienceRequise())), 1, 0);

        details.add(detailItem("🏷️", "Secteur", emptyAsDash(offre.getSecteurActivite())), 0, 1);
        details.add(detailItem("🧩", "Compétences", emptyAsDash(offre.getCompetencesRequises())), 1, 1);

        details.add(detailItem("✉️", "Contact", emptyAsDash(offre.getContactRecruteur())), 0, 2);
        details.add(detailItem("🕒", "Expiration", expDetail), 1, 2);



        // Divider
        Separator sep = new Separator();
        sep.getStyleClass().add("c-sep");

        // Footer: Salary + actions
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label salary = new Label(String.format("%.0f DT", offre.getSalaire()));
        salary.getStyleClass().add("c-salary");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnPostuler = new Button("Postuler");
        btnPostuler.getStyleClass().add("c-btn");
        btnPostuler.setPrefWidth(140);
        btnPostuler.setPrefHeight(45);
        btnPostuler.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        btnPostuler.setOnAction(e -> openPostulerPopup(offre)); // ✅ popup + insert postulation

        footer.getChildren().addAll(salary, spacer, btnPostuler);

        card.getChildren().addAll(header, companyRow, desc, meta, details, sep, footer);
        return card;
    }

    private HBox metaRowItem(String icon, String text) {
        Label i = new Label(icon);
        i.getStyleClass().add("c-meta-icon");

        Label t = new Label(text == null || text.isBlank() ? "—" : text);
        t.getStyleClass().add("c-meta");

        HBox row = new HBox(8, i, t);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox detailItem(String icon, String label, String value) {
        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);

        Label i = new Label(icon);
        i.getStyleClass().add("c-mini-icon");

        Label l = new Label(label);
        l.getStyleClass().add("c-mini-label");

        top.getChildren().addAll(i, l);

        Label v = new Label(value == null || value.isBlank() ? "—" : value);
        v.getStyleClass().add("c-mini-value");
        v.setWrapText(true);

        VBox box = new VBox(4, top, v);
        box.getStyleClass().add("c-detail-box");
        return box;
    }

    // ===================== ✅ POSTULER POPUP (Motivation only) =====================

    private void openPostulerPopup(OffreEmploi offre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/postuler.fxml"));
            Parent root = loader.load();

            // Your controller class: PostulerPopupController
            PostulerPopupController ctrl = loader.getController();
            ctrl.setOffreInfo(offre.getId(), offre.getTitre());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);

            Window owner = (flowOffers.getScene() != null) ? flowOffers.getScene().getWindow() : null;
            if (owner != null) stage.initOwner(owner);

            stage.setTitle("Postuler • " + safe(offre.getTitre()));

            Scene scene = new Scene(root);
            try {
                scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());
            } catch (Exception ignored) {}

            stage.setScene(scene);
            stage.setResizable(false);
            stage.showAndWait();

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir la fenêtre Postuler.\n" + ex.getMessage());
        }
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

    // ===================== EDIT =====================

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

            if (titre.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Le titre est obligatoire.");
                return null;
            }
            if (desc.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "La description est obligatoire.");
                return null;
            }
            if (typeContrat.getValue() == null || typeContrat.getValue().trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Le type de contrat est obligatoire.");
                return null;
            }
            if (!isValidEmail(contact.getText().trim())) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Email invalide.");
                return null;
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

    // ===================== HELPERS =====================

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    private String trimTo(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max).trim() + "...";
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    private String emptyAsDash(String s) {
        return (s == null || s.trim().isEmpty()) ? "—" : s.trim();
    }
}
