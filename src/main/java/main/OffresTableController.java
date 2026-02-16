package main;

import entities.OffreEmploi;
import entities.Postulation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import services.OffreEmploiService;
import services.PostulationService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

        // ✅ INLINE EDIT ENABLED
        tableOffres.setEditable(true);

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

        // ===== Inline editing (cell factories + commit => DB update) =====
        enableInlineEditing();

        // ===== Actions (3 icons) =====
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnPosts = iconBtn("👥", "Voir postulations");
            private final Button btnEdit  = iconBtn("✏", "Modifier (dialog)");
            private final Button btnDel   = iconBtn("🗑", "Supprimer");

            private final HBox box = new HBox(10, btnPosts, btnEdit, btnDel);

            {
                box.setAlignment(Pos.CENTER);

                btnPosts.getStyleClass().addAll("icon-btn", "icon-btn-neutral");
                btnEdit.getStyleClass().addAll("icon-btn", "icon-btn-edit");
                btnDel.getStyleClass().addAll("icon-btn", "icon-btn-delete");

                btnPosts.setOnAction(e -> {
                    OffreEmploi o = getTableView().getItems().get(getIndex());
                    goToPostulations(o);
                });

                btnEdit.setOnAction(e -> {
                    OffreEmploi o = getTableView().getItems().get(getIndex());
                    openEditDialog(o);
                });

                btnDel.setOnAction(e -> {
                    OffreEmploi o = getTableView().getItems().get(getIndex());
                    handleDelete(o);
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        // Search
        txtSearch.textProperty().addListener((obs, o, n) -> applyFilter());

        loadData();
    }

    // ===================== INLINE EDITING (NO SETTERS) =====================

    private void enableInlineEditing() {

        // TITRE
        colTitre.setCellFactory(TextFieldTableCell.forTableColumn());
        colTitre.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            if (nv.isEmpty()) { showAlert(Alert.AlertType.ERROR, "Erreur", "Titre obligatoire."); refreshRow(old); return; }
            OffreEmploi updated = copy(old, nv, null, null, null, null, null, null, null, null, null, null, null, null);
            saveInline(old, updated, "Titre");
        });

        // DESCRIPTION
        colDescription.setCellFactory(TextFieldTableCell.forTableColumn());
        colDescription.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            if (nv.isEmpty()) { showAlert(Alert.AlertType.ERROR, "Erreur", "Description obligatoire."); refreshRow(old); return; }
            OffreEmploi updated = copy(old, null, nv, null, null, null, null, null, null, null, null, null, null, null);
            saveInline(old, updated, "Description");
        });

        // SALAIRE
        colSalaire.setCellFactory(tc -> new TextFieldTableCell<>(new StringConverter<>() {
            @Override public String toString(Double object) {
                return object == null ? "" : String.format("%.0f", object);
            }
            @Override public Double fromString(String string) {
                try {
                    String s = safe(string);
                    if (s.isEmpty()) return null;
                    return Double.parseDouble(s);
                } catch (Exception e) {
                    return null;
                }
            }
        }));
        colSalaire.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            Double nv = ev.getNewValue();
            if (nv == null || nv <= 0) { showAlert(Alert.AlertType.ERROR, "Erreur", "Salaire invalide."); refreshRow(old); return; }
            OffreEmploi updated = copy(old, null, null, nv, null, null, null, null, null, null, null, null, null, null);
            saveInline(old, updated, "Salaire");
        });

        // TYPE CONTRAT (dropdown)
        colType.setCellFactory(ComboBoxTableCell.forTableColumn("CDI", "CDD", "Stage", "Freelance", "Alternance"));
        colType.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            if (nv.isEmpty()) { showAlert(Alert.AlertType.ERROR, "Erreur", "Type contrat obligatoire."); refreshRow(old); return; }
            OffreEmploi updated = copy(old, null, null, null, nv, null, null, null, null, null, null, null, null, null);
            saveInline(old, updated, "Type contrat");
        });

        // LOCALISATION
        colLocalisation.setCellFactory(TextFieldTableCell.forTableColumn());
        colLocalisation.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            if (nv.isEmpty()) { showAlert(Alert.AlertType.ERROR, "Erreur", "Localisation obligatoire."); refreshRow(old); return; }
            OffreEmploi updated = copy(old, null, null, null, null, nv, null, null, null, null, null, null, null, null);
            saveInline(old, updated, "Localisation");
        });

        // PUBLICATION (dd/MM/yyyy)
        colPublication.setCellFactory(tc -> new TextFieldTableCell<>(new StringConverter<>() {
            @Override public String toString(LocalDateTime object) {
                return object == null ? "" : dateFmt.format(object);
            }
            @Override public LocalDateTime fromString(String string) {
                try {
                    LocalDate d = LocalDate.parse(safe(string), dateFmt);
                    return d.atTime(0, 0);
                } catch (Exception e) {
                    return null;
                }
            }
        }));
        colPublication.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            LocalDateTime nv = ev.getNewValue();
            if (nv == null) { showAlert(Alert.AlertType.ERROR, "Erreur", "Format publication invalide (dd/MM/yyyy)."); refreshRow(old); return; }

            // publication <= expiration
            LocalDateTime exp = old.getDateExpiration();
            if (exp != null && nv.isAfter(exp)) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Publication doit être avant expiration.");
                refreshRow(old);
                return;
            }

            OffreEmploi updated = copy(old, null, null, null, null, null, nv, null, null, null, null, null, null, null);
            saveInline(old, updated, "Publication");
        });

        // EXPIRATION (dd/MM/yyyy)
        colExpiration.setCellFactory(tc -> new TextFieldTableCell<>(new StringConverter<>() {
            @Override public String toString(LocalDateTime object) {
                return object == null ? "" : dateFmt.format(object);
            }
            @Override public LocalDateTime fromString(String string) {
                try {
                    LocalDate d = LocalDate.parse(safe(string), dateFmt);
                    return d.atTime(23, 59);
                } catch (Exception e) {
                    return null;
                }
            }
        }));
        colExpiration.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            LocalDateTime nv = ev.getNewValue();
            if (nv == null) { showAlert(Alert.AlertType.ERROR, "Erreur", "Format expiration invalide (dd/MM/yyyy)."); refreshRow(old); return; }

            LocalDateTime pub = old.getDatePublication();
            if (pub != null && pub.isAfter(nv)) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Expiration doit être après publication.");
                refreshRow(old);
                return;
            }

            OffreEmploi updated = copy(old, null, null, null, null, null, null, nv, null, null, null, null, null, null);
            saveInline(old, updated, "Expiration");
        });

        // NIVEAU
        colNiveau.setCellFactory(TextFieldTableCell.forTableColumn());
        colNiveau.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            OffreEmploi updated = copy(old, null, null, null, null, null, null, null, nv, null, null, null, null, null);
            saveInline(old, updated, "Niveau");
        });

        // EXPERIENCE
        colExperience.setCellFactory(TextFieldTableCell.forTableColumn());
        colExperience.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            OffreEmploi updated = copy(old, null, null, null, null, null, null, null, null, nv, null, null, null, null);
            saveInline(old, updated, "Expérience");
        });

        // COMPETENCES
        colCompetences.setCellFactory(TextFieldTableCell.forTableColumn());
        colCompetences.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            OffreEmploi updated = copy(old, null, null, null, null, null, null, null, null, null, nv, null, null, null);
            saveInline(old, updated, "Compétences");
        });

        // SECTEUR
        colSecteur.setCellFactory(TextFieldTableCell.forTableColumn());
        colSecteur.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            OffreEmploi updated = copy(old, null, null, null, null, null, null, null, null, null, null, nv, null, null);
            saveInline(old, updated, "Secteur");
        });

        // ENTREPRISE
        colEntreprise.setCellFactory(TextFieldTableCell.forTableColumn());
        colEntreprise.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String nv = safe(ev.getNewValue());
            if (nv.isEmpty()) { showAlert(Alert.AlertType.ERROR, "Erreur", "Entreprise obligatoire."); refreshRow(old); return; }
            OffreEmploi updated = copy(old, null, null, null, null, null, null, null, null, null, null, null, nv, null);
            saveInline(old, updated, "Entreprise");
        });

        // CONTACT
        colContact.setCellFactory(TextFieldTableCell.forTableColumn());
        colContact.setOnEditCommit(ev -> {
            OffreEmploi old = ev.getRowValue();
            String email = safe(ev.getNewValue());
            if (!isValidEmail(email)) { showAlert(Alert.AlertType.ERROR, "Erreur", "Email invalide."); refreshRow(old); return; }
            OffreEmploi updated = copy(old, null, null, null, null, null, null, null, null, null, null, null, null, email);
            saveInline(old, updated, "Contact");
        });
    }

    private void saveInline(OffreEmploi oldOffre, OffreEmploi updated, String fieldName) {
        try {
            offreService.modifier(updated);

            // replace in lists so TableView reflects new object (no setters)
            replaceById(allData, updated);
            replaceById(filtered, updated);
            tableOffres.refresh();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Modification impossible (" + fieldName + ") :\n" + e.getMessage());
            loadData(); // rollback from DB
        }
    }

    private void refreshRow(OffreEmploi offre) {
        // revert UI to existing item values
        tableOffres.refresh();
    }

    private void replaceById(ObservableList<OffreEmploi> list, OffreEmploi updated) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() == updated.getId()) {
                list.set(i, updated);
                return;
            }
        }
    }

    /**
     * copy helper: any null parameter means keep old value
     */
    private OffreEmploi copy(
            OffreEmploi o,
            String titre,
            String description,
            Double salaire,
            String typeContrat,
            String localisation,
            LocalDateTime datePublication,
            LocalDateTime dateExpiration,
            String niveauQualification,
            String experienceRequise,
            String competencesRequises,
            String secteurActivite,
            String entreprise,
            String contactRecruteur
    ) {
        return new OffreEmploi(
                o.getId(),
                (titre != null) ? titre : o.getTitre(),
                (description != null) ? description : o.getDescription(),
                (salaire != null) ? salaire : o.getSalaire(),
                (typeContrat != null) ? typeContrat : o.getTypeContrat(),
                (localisation != null) ? localisation : o.getLocalisation(),
                (datePublication != null) ? datePublication : o.getDatePublication(),
                (dateExpiration != null) ? dateExpiration : o.getDateExpiration(),
                (niveauQualification != null) ? niveauQualification : o.getNiveauQualification(),
                (experienceRequise != null) ? experienceRequise : o.getExperienceRequise(),
                (competencesRequises != null) ? competencesRequises : o.getCompetencesRequises(),
                (secteurActivite != null) ? secteurActivite : o.getSecteurActivite(),
                (entreprise != null) ? entreprise : o.getEntreprise(),
                (contactRecruteur != null) ? contactRecruteur : o.getContactRecruteur()
        );
    }

    // ===================== DATA / FILTER =====================

    @FXML
    private void handleRefresh() {
        loadData();
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

    // ===================== ACTION: POSTULATIONS (FILTERED) =====================

    private void goToPostulations(OffreEmploi offre) {
        OffresShellController shell = OffresShellController.getInstance();
        if (shell != null) {
            shell.showPostulationsForOffre(offre.getId(), offre.getTitre());
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Shell non disponible.");
        }
    }

    // ===================== DELETE (FK SAFE) =====================

    private void handleDelete(OffreEmploi offre) {
        try {
            List<Postulation> posts = postService.afficherParOffre(offre.getId());

            if (!posts.isEmpty()) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirmation");
                confirm.setHeaderText("Cette offre a déjà des postulations (" + posts.size() + ")");
                confirm.setContentText("Voulez-vous supprimer aussi les postulations puis supprimer l'offre ?");

                ButtonType btnOui = new ButtonType("Oui, supprimer tout", ButtonBar.ButtonData.OK_DONE);
                ButtonType btnNon = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
                confirm.getButtonTypes().setAll(btnOui, btnNon);

                Optional<ButtonType> res = confirm.showAndWait();
                if (res.isEmpty() || res.get() != btnOui) return;

                for (Postulation p : posts) {
                    postService.supprimer(p.getId());
                }
            } else {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirmation");
                confirm.setHeaderText("Supprimer cette offre ?");
                confirm.setContentText("Titre : " + safe(offre.getTitre()) + "\nEntreprise : " + safe(offre.getEntreprise()));
                Optional<ButtonType> res = confirm.showAndWait();
                if (res.isEmpty() || res.get() != ButtonType.OK) return;
            }

            offreService.supprimer(offre.getId());
            loadData();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Offre supprimée.");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression impossible :\n" + e.getMessage());
        }
    }

    // ===================== EDIT DIALOG (button only) =====================

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

        TextField salaire = new TextField(offre.getSalaire() == 0 ? "" : String.valueOf((int) offre.getSalaire()));
        TextField localisation = new TextField(safe(offre.getLocalisation()));
        TextField entreprise = new TextField(safe(offre.getEntreprise()));
        TextField contact = new TextField(safe(offre.getContactRecruteur()));

        ComboBox<String> typeContrat = new ComboBox<>();
        typeContrat.getItems().setAll("CDI", "CDD", "Stage", "Freelance", "Alternance");
        typeContrat.setValue(safe(offre.getTypeContrat()).isEmpty() ? null : offre.getTypeContrat());

        TextField niveau = new TextField(safe(offre.getNiveauQualification()));
        TextField experience = new TextField(safe(offre.getExperienceRequise()));
        TextField competences = new TextField(safe(offre.getCompetencesRequises()));
        TextField secteur = new TextField(safe(offre.getSecteurActivite()));

        DatePicker pubDate = new DatePicker(
                offre.getDatePublication() == null ? LocalDate.now() : offre.getDatePublication().toLocalDate()
        );
        DatePicker expDate = new DatePicker(
                offre.getDateExpiration() == null ? LocalDate.now().plusDays(7) : offre.getDateExpiration().toLocalDate()
        );

        grid.addRow(0, new Label("Titre"), titre);
        grid.addRow(1, new Label("Description"), desc);
        grid.addRow(2, new Label("Salaire (DT)"), salaire);
        grid.addRow(3, new Label("Type contrat"), typeContrat);
        grid.addRow(4, new Label("Localisation"), localisation);

        grid.addRow(5, new Label("Publication"), pubDate);
        grid.addRow(6, new Label("Expiration"), expDate);

        grid.addRow(7, new Label("Niveau"), niveau);
        grid.addRow(8, new Label("Expérience"), experience);
        grid.addRow(9, new Label("Compétences"), competences);
        grid.addRow(10, new Label("Secteur"), secteur);

        grid.addRow(11, new Label("Entreprise"), entreprise);
        grid.addRow(12, new Label("Contact (email)"), contact);

        dialog.getDialogPane().setContent(grid);

        Node okBtn = dialog.getDialogPane().lookupButton(saveBtn);
        okBtn.setDisable(titre.getText().trim().isEmpty());
        titre.textProperty().addListener((obs, o, n) -> okBtn.setDisable(n.trim().isEmpty()));

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;

            if (safe(titre.getText()).isEmpty()) { showAlert(Alert.AlertType.ERROR, "Erreur", "Le titre est obligatoire."); return null; }
            if (safe(desc.getText()).isEmpty())  { showAlert(Alert.AlertType.ERROR, "Erreur", "La description est obligatoire."); return null; }
            if (typeContrat.getValue() == null || safe(typeContrat.getValue()).isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Le type de contrat est obligatoire."); return null;
            }
            if (!isValidEmail(safe(contact.getText()))) { showAlert(Alert.AlertType.ERROR, "Erreur", "Email invalide."); return null; }

            double sal;
            try {
                sal = Double.parseDouble(safe(salaire.getText()));
                if (sal <= 0) throw new RuntimeException();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Salaire invalide (ex: 2500).");
                return null;
            }

            if (pubDate.getValue() == null || expDate.getValue() == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Publication et expiration sont obligatoires."); return null;
            }
            if (expDate.getValue().isBefore(pubDate.getValue())) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Expiration doit être après publication."); return null;
            }

            LocalDateTime pub = pubDate.getValue().atTime(0, 0);
            LocalDateTime exp = expDate.getValue().atTime(23, 59);

            return new OffreEmploi(
                    offre.getId(),
                    safe(titre.getText()),
                    safe(desc.getText()),
                    sal,
                    safe(typeContrat.getValue()),
                    safe(localisation.getText()),
                    pub,
                    exp,
                    safe(niveau.getText()),
                    safe(experience.getText()),
                    safe(competences.getText()),
                    safe(secteur.getText()),
                    safe(entreprise.getText()),
                    safe(contact.getText())
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

    private Button iconBtn(String icon, String tip) {
        Button b = new Button(icon);
        b.setTooltip(new Tooltip(tip));
        b.setMinWidth(36);
        b.setPrefWidth(36);
        b.setMinHeight(32);
        b.setPrefHeight(32);
        return b;
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }
}
