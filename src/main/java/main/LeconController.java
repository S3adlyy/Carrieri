package main;

import entities.Lecon;
import entities.Module;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.LeconService;
import services.ModuleService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

public class LeconController {

    // ============================================
    // FXML FIELDS
    // ============================================

    @FXML private TextField txtTitre;
    @FXML private TextArea txtContenu;
    @FXML private ComboBox<Module> comboModules;
    @FXML private Label lblInfo;  // Gardé pour compatibilité
    @FXML private Label lblModuleInfo;  // Label pour le titre du module
    @FXML private VBox formBox;
    @FXML private VBox tablePane;

    // Vidéo fields
    @FXML private Button btnChoisirVideo;
    @FXML private Label lblVideoNom;
    @FXML private Label lblTailleVideo;

    // TableView
    @FXML private TableView<Lecon> tableLecons;
    @FXML private TableColumn<Lecon, Integer> colId;
    @FXML private TableColumn<Lecon, String> colTitre;
    @FXML private TableColumn<Lecon, String> colContenu;
    @FXML private TableColumn<Lecon, byte[]> colVideo;
    @FXML private TableColumn<Lecon, Integer> colOrdre;
    @FXML private TableColumn<Lecon, Void> colActions;

    // Buttons
    @FXML private Button btnAjouter;
    @FXML private Button btnToggleForm;

    // ✅ LABELS D'ERREUR
    @FXML private Label errorModule;
    @FXML private Label errorTitre;
    @FXML private Label errorContenu;
    @FXML private Label charCountLabel;
    // ============================================
    // SERVICES & DATA
    // ============================================

    private LeconService leconService = new LeconService();
    private ModuleService moduleService = new ModuleService();
    private ObservableList<Lecon> leconList = FXCollections.observableArrayList();

    // State variables
    private int coursId = 0;
    private int moduleId = 0;
    private String moduleTitre = "";  // Stocker le titre du module
    private boolean hasModuleActif = false;  // Indique si un module est sélectionné
    private Module moduleSelectionne = null;
    private Lecon leconSelectionnee = null;

    // Video variables
    private byte[] videoBytes = null;
    private String videoNom = null;

    // Previous scene support
    private Scene previousScene;
    private boolean wasMaximized = false;

    // ============================================
    // INITIALIZATION
    // ============================================

    @FXML
    public void initialize() {
        setupTableColumns();
        setupComboBoxes();
        setupVideoChooser();

        setFormVisible(false);
        setTableVisible(true);

        // ✅ VALIDATION EN TEMPS RÉEL
        setupValidation();
        txtContenu.textProperty().addListener((obs, oldVal, newVal) -> {
            if (charCountLabel != null) {
                int length = newVal != null ? newVal.length() : 0;
                charCountLabel.setText(length + "/500");

                if (length < 500) {
                    charCountLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: 600; -fx-background-color: #fee2e2; -fx-padding: 4 10; -fx-background-radius: 20;");
                } else if (length > 9000) {
                    charCountLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: 600; -fx-background-color: #f3e8ff; -fx-padding: 4 10; -fx-background-radius: 20;");
                } else {
                    charCountLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: 600; -fx-background-color: #d1fae5; -fx-padding: 4 10; -fx-background-radius: 20;");
                }
            }
        });
        comboModules.valueProperty().addListener((obs, oldModule, newModule) -> {
            if (newModule != null) {
                moduleSelectionne = newModule;
                chargerLeconsParModule(newModule.getId());
                hideError(errorModule);
            } else if (moduleId == 0) {
                showError(errorModule, "Veuillez sélectionner un module");
            }
        });

        tableLecons.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                leconSelectionnee = newSelection;
            } else {
                leconSelectionnee = null;
            }
        });
    }

    // ✅ NOUVELLE MÉTHODE POUR LA VALIDATION EN TEMPS RÉEL
    private void setupValidation() {
        // Validation du titre
        txtTitre.textProperty().addListener((obs, oldVal, newVal) -> {
            String titre = newVal != null ? newVal.trim() : "";
            if (titre.isEmpty()) {
                showError(errorTitre, "Le titre est obligatoire");
            } else if (titre.length() < 3) {
                showError(errorTitre, "Le titre doit contenir au moins 3 caractères");
            } else if (titre.length() > 200) {
                showError(errorTitre, "Le titre ne peut pas dépasser 200 caractères");
            } else if (isOnlyDigits(titre)) {
                showError(errorTitre, "Le titre ne peut pas être composé uniquement de chiffres");
            } else {
                hideError(errorTitre);
            }
        });

        txtContenu.textProperty().addListener((obs, oldVal, newVal) -> {
            String contenu = newVal != null ? newVal.trim() : "";
            if (contenu.isEmpty()) {
                showError(errorContenu, "Le contenu est obligatoire");
            } else if (contenu.length() < 500) {  // ✅ Changé de 10 à 500
                showError(errorContenu, "Le contenu doit contenir au moins 500 caractères");
            } else if (contenu.length() > 10000) {
                showError(errorContenu, "Le contenu ne peut pas dépasser 10000 caractères");
            } else if (isOnlyDigits(contenu)) {
                showError(errorContenu, "Le contenu ne peut pas être composé uniquement de chiffres");
            } else {
                hideError(errorContenu);
            }
        });
    }

    // ✅ MÉTHODES UTILITAIRES POUR LES ERREURS
    private void showError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError(Label errorLabel) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private boolean isOnlyDigits(String text) {
        if (text == null || text.isEmpty()) return false;
        String textWithoutSpaces = text.replaceAll("\\s+", "");
        return textWithoutSpaces.matches("\\d+");
    }

    // ============================================
    // PUBLIC METHODS - CALLED BY OTHER CONTROLLERS
    // ============================================

    // ✅ Méthode principale pour passer l'ID et le TITRE du module (depuis le bouton 📖)
    public void setModuleInfo(int id, String titre) {
        System.out.println("📌 CHARGEMENT: Leçons du module " + id + " - " + titre);
        this.moduleId = id;
        this.moduleTitre = titre;
        this.hasModuleActif = true;

        if (lblModuleInfo != null) {
            lblModuleInfo.setText("📌 Module: " + titre);
        }

        comboModules.setDisable(true);

        Module module = getModuleById(id);
        if (module != null) {
            moduleSelectionne = module;
            comboModules.getItems().clear();
            comboModules.getItems().add(module);
            comboModules.setValue(module);
            hideError(errorModule);
        }

        chargerLeconsParModule(id);
    }

    // Méthode pour les leçons depuis un cours (toutes les leçons du cours)
    public void setCoursId(int id) {
        System.out.println("📌 CHARGEMENT: Toutes les leçons du cours " + id);
        this.coursId = id;
        this.moduleId = 0;
        this.hasModuleActif = false;

        if (lblModuleInfo != null) {
            lblModuleInfo.setText("📌 Cours: Toutes les leçons");
        }

        comboModules.setDisable(false);
        chargerModules();
        chargerToutesLeconsParCours(id);
    }

    public void setPreviousScene(Scene previousScene) {
        this.previousScene = previousScene;
        Stage stage = (Stage) (previousScene != null ? previousScene.getWindow() : null);
        if (stage != null) {
            this.wasMaximized = stage.isMaximized();
        }
    }

    // ============================================
    // PRIVATE HELPER METHODS
    // ============================================

    private void chargerToutesLeconsParCours(int coursId) {
        leconList.clear();
        List<Module> modules = moduleService.getModulesByCours(coursId);
        for (Module module : modules) {
            List<Lecon> lecons = leconService.getLeconsByModule(module.getId());
            leconList.addAll(lecons);
        }
        tableLecons.setItems(leconList);
        tableLecons.refresh();
    }

    private void setupVideoChooser() {
        btnChoisirVideo.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choisir une vidéo");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Vidéos MP4", "*.mp4"),
                    new FileChooser.ExtensionFilter("Tous les formats", "*.mp4", "*.avi", "*.mov", "*.mkv")
            );

            File file = fileChooser.showOpenDialog(btnChoisirVideo.getScene().getWindow());
            if (file != null) {
                try {
                    videoBytes = Files.readAllBytes(file.toPath());
                    videoNom = file.getName();

                    String taille = formatTaille(videoBytes.length);
                    lblVideoNom.setText(videoNom + " (" + taille + ")");
                    lblTailleVideo.setText("✅ Prêt - " + taille);

                    if (videoBytes.length > 50 * 1024 * 1024) {
                        showAlert(Alert.AlertType.WARNING, "⚠️ Attention",
                                "Vidéo de " + taille + "\nAssurez-vous que max_allowed_packet est à 64M dans MySQL");
                    }

                } catch (IOException ex) {
                    ex.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Impossible de lire le fichier vidéo");
                }
            }
        });
    }

    private String formatTaille(long taille) {
        if (taille < 1024) return taille + " B";
        if (taille < 1024 * 1024) return (taille / 1024) + " KB";
        if (taille < 1024 * 1024 * 1024) return String.format("%.1f MB", taille / (1024.0 * 1024.0));
        return String.format("%.2f GB", taille / (1024.0 * 1024.0 * 1024.0));
    }

    private Module getModuleById(int id) {
        try {
            List<Module> modules = moduleService.getAll();
            return modules.stream().filter(m -> m.getId() == id).findFirst().orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void chargerModules() {
        if (coursId > 0) {
            List<Module> modules = moduleService.getModulesByCours(coursId);
            comboModules.setItems(FXCollections.observableArrayList(modules));
            leconList.clear();
        }
    }

    private void chargerLeconsParModule(int moduleId) {
        leconList.clear();
        List<Lecon> lecons = leconService.getLeconsByModule(moduleId);
        leconList.setAll(lecons);
        tableLecons.setItems(leconList);
        tableLecons.refresh();
        System.out.println("📚 Leçons chargées pour " + moduleTitre + ": " + lecons.size());
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setVisible(false);

        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colTitre.setCellFactory(column -> new LeconTitleCell(leconService));

        // ✅ CONTENU - Affichage sur plusieurs lignes
        colContenu.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        colContenu.setCellFactory(column -> new LeconContenuCell(leconService));
        colVideo.setCellValueFactory(new PropertyValueFactory<>("video"));
        colVideo.setCellFactory(column -> new LeconVideoCell(leconService));

        colOrdre.setCellValueFactory(new PropertyValueFactory<>("ordre"));
        colOrdre.setCellFactory(column -> new LeconOrdreCell(leconService, leconList));

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button("🗑️");
            private final HBox actions = new HBox(5, btnDelete);

            {
                actions.setAlignment(javafx.geometry.Pos.CENTER);
                btnDelete.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");

                btnDelete.setOnAction(event -> {
                    Lecon lecon = getTableRow() != null ? getTableRow().getItem() : null;
                    if (lecon != null) {
                        supprimerLecon(lecon);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });

        tableLecons.setItems(leconList);
    }

    private void setupComboBoxes() {
        comboModules.setCellFactory(lv -> new ListCell<Module>() {
            @Override
            protected void updateItem(Module item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText("Module " + item.getOrdre() + " - " + item.getTitre());
            }
        });

        comboModules.setButtonCell(new ListCell<Module>() {
            @Override
            protected void updateItem(Module item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitre());
            }
        });
    }

    // ============================================
    // CRUD OPERATIONS
    // ============================================

    @FXML
    private void ajouterLecon() {
        if (!validerFormulaire()) return;

        int targetModuleId = 0;
        if (moduleId > 0) {
            targetModuleId = moduleId;
        } else if (comboModules.getValue() != null) {
            targetModuleId = comboModules.getValue().getId();
        } else {
            showError(errorModule, "Veuillez sélectionner un module");
            return;
        }

        int ordre = leconList.stream().mapToInt(Lecon::getOrdre).max().orElse(0) + 1;

        final int ordreVerif = ordre;
        final int moduleVerif = targetModuleId;
        boolean ordreExiste = leconList.stream()
                .anyMatch(l -> l.getOrdre() == ordreVerif && l.getModuleId() == moduleVerif);

        if (ordreExiste) {
            showError(errorContenu, "Une leçon avec l'ordre " + ordre + " existe déjà.");
            return;
        }

        Lecon lecon = new Lecon(
                txtTitre.getText().trim(),
                txtContenu.getText().trim(),
                videoBytes,
                ordre,
                targetModuleId
        );

        try {
            leconService.ajouter(lecon);
            showAlert(Alert.AlertType.INFORMATION, "✅ Succès", "Leçon ajoutée !");
            chargerLeconsParModule(targetModuleId);
            clearFields();
            setFormVisible(false);
            setTableVisible(true);

        } catch (IllegalArgumentException e) {
            // ✅ Capturer les erreurs de validation
            String message = e.getMessage();
            if (message.contains("titre")) {
                showError(errorTitre, message);
            } else if (message.contains("contenu")) {
                showError(errorContenu, message);
            } else if (message.contains("module")) {
                showError(errorModule, message);
            } else {
                showAlert(Alert.AlertType.WARNING, "⚠️ Validation", message);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "❌ Erreur", e.getMessage());
        }
    }

    private void supprimerLecon(Lecon lecon) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("🗑️ Confirmation");
        confirm.setHeaderText("Supprimer la leçon");
        confirm.setContentText("Voulez-vous supprimer la leçon : \"" + lecon.getTitre() + "\" ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                leconService.supprimer(lecon.getId());
                showAlert(Alert.AlertType.INFORMATION, "✅ Succès", "Leçon supprimée !");
                chargerLeconsParModule(lecon.getModuleId());
                clearFields();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "❌ Erreur", e.getMessage());
            }
        }
    }

    @FXML
    private void annuler() {
        clearFields();
    }

    private boolean validerFormulaire() {
        StringBuilder errors = new StringBuilder();

        // Validation du module
        if (moduleId == 0 && (comboModules.getValue() == null)) {
            errors.append("• Veuillez sélectionner un module\n");
            showError(errorModule, "Veuillez sélectionner un module");
        } else {
            hideError(errorModule);
        }

        String titre = txtTitre.getText().trim();
        if (titre.isEmpty()) {
            errors.append("• Le titre est obligatoire\n");
            showError(errorTitre, "Le titre est obligatoire");
        } else if (titre.length() < 3) {
            errors.append("• Le titre doit contenir au moins 3 caractères\n");
            showError(errorTitre, "Le titre doit contenir au moins 3 caractères");
        } else if (titre.length() > 200) {
            errors.append("• Le titre ne peut pas dépasser 200 caractères\n");
            showError(errorTitre, "Le titre ne peut pas dépasser 200 caractères");
        } else if (isOnlyDigits(titre)) {
            errors.append("• Le titre ne peut pas être composé uniquement de chiffres\n");
            showError(errorTitre, "Le titre ne peut pas être composé uniquement de chiffres");
        } else {
            hideError(errorTitre);
        }

        String contenu = txtContenu.getText().trim();
        if (contenu.isEmpty()) {
            errors.append("• Le contenu est obligatoire\n");
            showError(errorContenu, "Le contenu est obligatoire");
        } else if (contenu.length() < 500) {  // ✅ Changé de 10 à 500
            errors.append("• Le contenu doit contenir au moins 500 caractères\n");
            showError(errorContenu, "Le contenu doit contenir au moins 500 caractères");
        } else if (contenu.length() > 10000) {
            errors.append("• Le contenu ne peut pas dépasser 10000 caractères\n");
            showError(errorContenu, "Le contenu ne peut pas dépasser 10000 caractères");
        } else if (isOnlyDigits(contenu)) {
            errors.append("• Le contenu ne peut pas être composé uniquement de chiffres\n");
            showError(errorContenu, "Le contenu ne peut pas être composé uniquement de chiffres");
        } else {
            hideError(errorContenu);
        }

        if (errors.length() > 0) {
            return false;
        }
        return true;
    }

    private void clearFields() {
        txtTitre.clear();
        txtContenu.clear();

        videoBytes = null;
        videoNom = null;
        lblVideoNom.setText("Aucune vidéo");
        lblTailleVideo.setText("");

        tableLecons.getSelectionModel().clearSelection();
        leconSelectionnee = null;

        // ✅ Réinitialiser le compteur
        if (charCountLabel != null) {
            charCountLabel.setText("0/500");
            charCountLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-weight: 600; -fx-background-color: #f3e8ff; -fx-padding: 4 10; -fx-background-radius: 20;");
        }

        // Cacher les erreurs
        hideError(errorModule);
        hideError(errorTitre);
        hideError(errorContenu);
    }

    private void setFormVisible(boolean visible) {
        if (formBox != null) {
            formBox.setVisible(visible);
            formBox.setManaged(visible);
        }
    }

    private void setTableVisible(boolean visible) {
        if (tablePane != null) {
            tablePane.setVisible(visible);
            tablePane.setManaged(visible);
        }
    }

    @FXML
    private void toggleForm() {
        boolean isVisible = formBox != null && formBox.isVisible();
        if (!isVisible) {
            clearFields();
            leconSelectionnee = null;
            tableLecons.getSelectionModel().clearSelection();
            setTableVisible(false);
            setFormVisible(true);
        } else {
            setFormVisible(false);
            setTableVisible(true);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}