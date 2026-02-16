package main;

import entities.OffreEmploi;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import services.OffreEmploiService;
import services.PostulationService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class OffresTableController {

    @FXML private TableView<OffreEmploi> tableOffres;

    @FXML private TableColumn<OffreEmploi, String> colTitre;
    @FXML private TableColumn<OffreEmploi, String> colDescription;
    @FXML private TableColumn<OffreEmploi, Double> colSalaire;
    @FXML private TableColumn<OffreEmploi, String> colType;
    @FXML private TableColumn<OffreEmploi, String> colLocalisation;
    @FXML private TableColumn<OffreEmploi, LocalDateTime> colPublication;
    @FXML private TableColumn<OffreEmploi, LocalDateTime> colExpiration;
    @FXML private TableColumn<OffreEmploi, String> colNiveau;
    @FXML private TableColumn<OffreEmploi, String> colExperience;
    @FXML private TableColumn<OffreEmploi, String> colCompetences;
    @FXML private TableColumn<OffreEmploi, String> colSecteur;
    @FXML private TableColumn<OffreEmploi, String> colEntreprise;
    @FXML private TableColumn<OffreEmploi, String> colContact;

    @FXML private TableColumn<OffreEmploi, Void> colActions;

    @FXML private TextField txtSearch;
    @FXML private Label lblStatus;

    private final OffreEmploiService offreService = new OffreEmploiService();
    private final PostulationService postService = new PostulationService();

    private final ObservableList<OffreEmploi> allData = FXCollections.observableArrayList();
    private final ObservableList<OffreEmploi> filtered = FXCollections.observableArrayList();

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {

        // ===== Bind columns =====
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colSalaire.setCellValueFactory(new PropertyValueFactory<>("salaire"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeContrat"));
        colLocalisation.setCellValueFactory(new PropertyValueFactory<>("localisation"));
        colPublication.setCellValueFactory(new PropertyValueFactory<>("datePublication"));
        colExpiration.setCellValueFactory(new PropertyValueFactory<>("dateExpiration"));
        colNiveau.setCellValueFactory(new PropertyValueFactory<>("niveauQualification"));
        colExperience.setCellValueFactory(new PropertyValueFactory<>("experienceRequise"));
        colCompetences.setCellValueFactory(new PropertyValueFactory<>("competencesRequises"));
        colSecteur.setCellValueFactory(new PropertyValueFactory<>("secteurActivite"));
        colEntreprise.setCellValueFactory(new PropertyValueFactory<>("entreprise"));
        colContact.setCellValueFactory(new PropertyValueFactory<>("contactRecruteur"));

        // ===== Pretty formatting =====
        colSalaire.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format("%.0f DT", item));
            }
        });

        colPublication.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : dateFmt.format(item));
            }
        });

        colExpiration.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : dateFmt.format(item));
            }
        });

        colDescription.setCellFactory(col -> makeTrimCell(70));
        colCompetences.setCellFactory(col -> makeTrimCell(70));
        colContact.setCellFactory(col -> makeTrimCell(40));

        // ===== Actions (👥 ✏ 🗑) =====
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnViewPosts = new Button("👥");
            private final Button btnEdit = new Button("✏");
            private final Button btnDelete = new Button("🗑");

            {
                btnEdit.getStyleClass().addAll("btn-icon", "btn-icon-edit");
                btnDelete.getStyleClass().addAll("btn-icon", "btn-icon-delete");
                btnViewPosts.getStyleClass().addAll("btn-icon", "btn-icon-edit");

                btnViewPosts.setTooltip(new Tooltip("Voir les postulations"));
                btnEdit.setTooltip(new Tooltip("Modifier (double-clic fonctionne aussi)"));
                btnDelete.setTooltip(new Tooltip("Supprimer"));

                btnViewPosts.setOnAction(e -> {
                    OffreEmploi o = getRowItem();
                    if (o != null) OffresShellController.getInstance().showPostulationsForOffre(o.getId(), o.getTitre());
                });

                btnEdit.setOnAction(e -> {
                    OffreEmploi o = getRowItem();
                    if (o != null) openEditDialog(o);
                });

                btnDelete.setOnAction(e -> {
                    OffreEmploi o = getRowItem();
                    if (o != null) handleDelete(o);
                });
            }

            private OffreEmploi getRowItem() {
                return (getTableRow() == null) ? null : getTableRow().getItem();
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    HBox box = new HBox(10, btnViewPosts, btnEdit, btnDelete);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });

        // ✅ Double click to edit (anywhere on row)
        tableOffres.setRowFactory(tv -> {
            TableRow<OffreEmploi> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !row.isEmpty()) {
                    openEditDialog(row.getItem());
                }
            });
            return row;
        });

        // ===== Search =====
        txtSearch.textProperty().addListener((obs, o, n) -> applyFilter());

        loadData();
    }

    private TableCell<OffreEmploi, String> makeTrimCell(int max) {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                    setTooltip(null);
                    return;
                }
                String txt = item.trim();
                if (txt.length() > max) {
                    setText(txt.substring(0, max).trim() + "…");
                    setTooltip(new Tooltip(txt));
                } else {
                    setText(txt);
                    setTooltip(null);
                }
            }
        };
    }

    private void loadData() {
        try {
            allData.setAll(offreService.read());
            applyFilter();
        } catch (SQLException e) {
            allData.clear();
            filtered.clear();
            tableOffres.setItems(filtered);
            lblStatus.setText("0 offres trouvées");
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement :\n" + e.getMessage());
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
                            safe(o.getNiveauQualification()).toLowerCase().contains(q) ||
                            safe(o.getExperienceRequise()).toLowerCase().contains(q) ||
                            safe(o.getContactRecruteur()).toLowerCase().contains(q)
            ));
        }

        tableOffres.setItems(filtered);
        lblStatus.setText(filtered.size() + " offres trouvées");
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    // ===================== ✅ DELETE FIX (handles FK postulation) =====================

    private void handleDelete(OffreEmploi offre) {
        try {
            int nbPosts = postService.afficherParOffre(offre.getId()).size();

            if (nbPosts > 0) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirmation");
                confirm.setHeaderText("Cette offre a déjà des postulations (" + nbPosts + ")");
                confirm.setContentText("Voulez-vous supprimer aussi les postulations puis supprimer l'offre ?");

                ButtonType btnOui = new ButtonType("Oui, supprimer tout", ButtonBar.ButtonData.OK_DONE);
                ButtonType btnNon = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
                confirm.getButtonTypes().setAll(btnOui, btnNon);

                Optional<ButtonType> res = confirm.showAndWait();
                if (res.isEmpty() || res.get() != btnOui) return;

                postService.supprimerParOffre(offre.getId()); // ✅ remove children first
            } else {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirmation");
                confirm.setHeaderText("Supprimer cette offre ?");
                confirm.setContentText("Titre : " + safe(offre.getTitre()) + "\nEntreprise : " + safe(offre.getEntreprise()));
                Optional<ButtonType> res = confirm.showAndWait();
                if (res.isEmpty() || res.get() != ButtonType.OK) return;
            }

            offreService.supprimer(offre.getId()); // ✅ now allowed
            loadData();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Offre supprimée.");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression impossible :\n" + e.getMessage());
        }
    }

    // ===================== EDIT (Publication + Expiration) =====================

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

        TextField niveau = new TextField(safe(offre.getNiveauQualification()));
        TextField expReq = new TextField(safe(offre.getExperienceRequise()));
        TextField competences = new TextField(safe(offre.getCompetencesRequises()));
        TextField secteur = new TextField(safe(offre.getSecteurActivite()));

        ComboBox<String> typeContrat = new ComboBox<>();
        typeContrat.getItems().setAll("CDI", "CDD", "Stage", "Freelance", "Alternance");
        typeContrat.setValue(safe(offre.getTypeContrat()).isEmpty() ? null : offre.getTypeContrat());

        DatePicker pubDate = new DatePicker(
                offre.getDatePublication() == null ? LocalDate.now() : offre.getDatePublication().toLocalDate()
        );

        DatePicker expDate = new DatePicker(
                offre.getDateExpiration() == null ? LocalDate.now().plusDays(7) : offre.getDateExpiration().toLocalDate()
        );

        grid.add(new Label("Titre"), 0, 0);               grid.add(titre, 1, 0);
        grid.add(new Label("Description"), 0, 1);         grid.add(desc, 1, 1);
        grid.add(new Label("Salaire (DT)"), 0, 2);        grid.add(salaire, 1, 2);
        grid.add(new Label("Type contrat"), 0, 3);        grid.add(typeContrat, 1, 3);
        grid.add(new Label("Localisation"), 0, 4);        grid.add(localisation, 1, 4);
        grid.add(new Label("Entreprise"), 0, 5);          grid.add(entreprise, 1, 5);
        grid.add(new Label("Contact (email)"), 0, 6);     grid.add(contact, 1, 6);

        grid.add(new Label("Publication"), 0, 7);         grid.add(pubDate, 1, 7);
        grid.add(new Label("Expiration"), 0, 8);          grid.add(expDate, 1, 8);

        grid.add(new Label("Niveau"), 0, 9);              grid.add(niveau, 1, 9);
        grid.add(new Label("Expérience requise"), 0, 10); grid.add(expReq, 1, 10);
        grid.add(new Label("Compétences (CSV)"), 0, 11);  grid.add(competences, 1, 11);
        grid.add(new Label("Secteur"), 0, 12);            grid.add(secteur, 1, 12);

        dialog.getDialogPane().setContent(grid);

        Node okBtn = dialog.getDialogPane().lookupButton(saveBtn);
        okBtn.setDisable(titre.getText().trim().isEmpty());
        titre.textProperty().addListener((obs, o, n) -> okBtn.setDisable(n.trim().isEmpty()));

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;

            if (titre.getText().trim().isEmpty()) { showAlert(Alert.AlertType.ERROR, "Erreur", "Le titre est obligatoire."); return null; }
            if (desc.getText().trim().isEmpty()) { showAlert(Alert.AlertType.ERROR, "Erreur", "La description est obligatoire."); return null; }
            if (typeContrat.getValue() == null || typeContrat.getValue().trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Le type de contrat est obligatoire."); return null;
            }
            if (!isValidEmail(contact.getText().trim())) { showAlert(Alert.AlertType.ERROR, "Erreur", "Email invalide."); return null; }
            if (pubDate.getValue() == null) { showAlert(Alert.AlertType.ERROR, "Erreur", "La date de publication est obligatoire."); return null; }
            if (expDate.getValue() == null) { showAlert(Alert.AlertType.ERROR, "Erreur", "La date d'expiration est obligatoire."); return null; }

            double sal;
            try {
                sal = Double.parseDouble(salaire.getText().trim());
                if (sal <= 0) throw new RuntimeException();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Salaire invalide (ex: 2500).");
                return null;
            }

            LocalDateTime newPublication = pubDate.getValue().atTime(0, 0);

            LocalDateTime newExpiration = expDate.getValue().atTime(
                    (offre.getDateExpiration() == null ? 23 : offre.getDateExpiration().getHour()),
                    (offre.getDateExpiration() == null ? 59 : offre.getDateExpiration().getMinute())
            );

            if (newPublication.isAfter(newExpiration)) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "La publication doit être avant l'expiration.");
                return null;
            }

            return new OffreEmploi(
                    offre.getId(),
                    titre.getText().trim(),
                    desc.getText().trim(),
                    sal,
                    typeContrat.getValue().trim(),
                    localisation.getText().trim(),
                    newPublication,
                    newExpiration,
                    niveau.getText().trim(),
                    expReq.getText().trim(),
                    competences.getText().trim(),
                    secteur.getText().trim(),
                    entreprise.getText().trim(),
                    contact.getText().trim()
            );
        });

        Optional<OffreEmploi> res = dialog.showAndWait();
        if (res.isEmpty()) return;

        try {
            offreService.modifier(res.get());
            loadData();
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

    private String safe(String s) {
        return (s == null) ? "" : s;
    }
}
