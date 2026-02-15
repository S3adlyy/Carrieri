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
    @FXML private TextField txtOrdre;
    @FXML private ComboBox<Module> comboModules;
    @FXML private Label lblInfo;
    @FXML private VBox formBox;
    @FXML private VBox tablePane;

    // Vidéo fields - SIMPLE
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

    // ============================================
    // SERVICES & DATA
    // ============================================

    private LeconService leconService = new LeconService();
    private ModuleService moduleService = new ModuleService();
    private ObservableList<Lecon> leconList = FXCollections.observableArrayList();

    // State variables
    private int coursId = 0;
    private int moduleId = 0;
    private Module moduleSelectionne = null;
    private Lecon leconSelectionnee = null;

    // Video variables - SIMPLE
    private byte[] videoBytes = null;
    private String videoNom = null;

    // Previous scene support
    private Scene previousScene;

    // ============================================
    // INITIALIZATION
    // ============================================

    @FXML
    public void initialize() {
        setupTableColumns();
        setupComboBoxes();
        setupVideoChooser();  // ✅ VERSION SIMPLE
        setupNumericFieldsOnly();

        lblInfo.setVisible(false);
        lblInfo.setManaged(false);
        txtOrdre.setVisible(false);
        txtOrdre.setManaged(false);

        // Start with table visible and form hidden
        setFormVisible(false);
        setTableVisible(true);

        comboModules.valueProperty().addListener((obs, oldModule, newModule) -> {
            if (newModule != null) {
                moduleSelectionne = newModule;
                chargerLeconsParModule(newModule.getId());
            }
        });

        tableLecons.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                leconSelectionnee = newSelection;
                chargerLeconFormulaire(newSelection);
            } else {
                leconSelectionnee = null;
                txtOrdre.clear();
            }
        });
    }

    // ============================================
    // PUBLIC METHODS - CALLED BY OTHER CONTROLLERS
    // ============================================

    public void setModuleId(int id) {
        this.moduleId = id;
        this.coursId = 0;
        lblInfo.setText("Module ID: " + id);
        comboModules.setDisable(true);

        Module module = getModuleById(id);
        if (module != null) {
            moduleSelectionne = module;
            comboModules.getItems().clear();
            comboModules.getItems().add(module);
            comboModules.setValue(module);
        }

        chargerLeconsParModule(id);
    }

    public void setModuleTitre(String titre) {
        lblInfo.setText("Module: " + titre);
    }

    public void setCoursId(int id) {
        this.coursId = id;
        this.moduleId = 0;
        lblInfo.setText("Cours ID: " + id);
        lblInfo.setVisible(true);
        lblInfo.setManaged(true);
        comboModules.setDisable(false);
        chargerModules();

        // Charger TOUTES les leçons du cours
        chargerToutesLeconsParCours(id);
    }

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

    // ============================================
    // VIDEO - SIMPLE SANS COMPRESSION
    // ============================================

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
                    // ✅ AUCUNE COMPRESSION - Lecture directe
                    videoBytes = Files.readAllBytes(file.toPath());
                    videoNom = file.getName();

                    // Afficher les infos
                    String taille = formatTaille(videoBytes.length);
                    lblVideoNom.setText(videoNom + " (" + taille + ")");
                    lblTailleVideo.setText("✅ Prêt pour MySQL - " + taille);

                    // ✅ Alerte si trop gros pour MySQL
                    if (videoBytes.length > 50 * 1024 * 1024) {
                        showAlert(Alert.AlertType.WARNING, "⚠️ Attention",
                                "Vidéo de " + taille + "\n" +
                                        "Assurez-vous que max_allowed_packet est à 64M dans MySQL");
                    }

                } catch (IOException ex) {
                    ex.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Impossible de lire le fichier vidéo");
                }
            }
        });
    }

    private String formatTaille(long taille) {
        if (taille < 1024) {
            return taille + " B";
        } else if (taille < 1024 * 1024) {
            return (taille / 1024) + " KB";
        } else if (taille < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", taille / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", taille / (1024.0 * 1024.0 * 1024.0));
        }
    }

    // ============================================
    // PUBLIC METHODS
    // ============================================

    public void setPreviousScene(Scene previousScene) {
        this.previousScene = previousScene;
    }

    // ============================================
    // PRIVATE HELPER METHODS
    // ============================================

    private Module getModuleById(int id) {
        try {
            List<Module> modules = moduleService.getAll();
            for (Module m : modules) {
                if (m.getId() == id) {
                    return m;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void chargerModules() {
        if (coursId > 0) {
            List<Module> modules = moduleService.getModulesByCours(coursId);
            comboModules.setItems(FXCollections.observableArrayList(modules));
            leconList.clear();
        }
    }

    private void setupNumericFieldsOnly() {
        // Restreindre le champ ordre aux nombres uniquement
        txtOrdre.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtOrdre.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }

    private void chargerLeconsParModule(int moduleId) {
        leconList.clear();
        List<Lecon> lecons = leconService.getLeconsByModule(moduleId);
        leconList.setAll(lecons);
        tableLecons.setItems(leconList);
        tableLecons.refresh();
    }

    private void chargerLeconFormulaire(Lecon lecon) {
        txtTitre.setText(lecon.getTitre());
        txtContenu.setText(lecon.getContenu());
        txtOrdre.setText(String.valueOf(lecon.getOrdre()));

        if (lecon.getVideo() != null && lecon.getVideo().length > 0) {
            videoBytes = lecon.getVideo();
            videoNom = "Vidéo";
            String taille = formatTaille(videoBytes.length);
            lblVideoNom.setText("🎬 Vidéo (" + taille + ")");
            lblTailleVideo.setText("Chargée: " + taille);
        } else {
            videoBytes = null;
            videoNom = null;
            lblVideoNom.setText("Aucune vidéo");
            lblTailleVideo.setText("");
        }
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setVisible(false);

        // Titre - Éditable inline
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colTitre.setCellFactory(column -> new LeconTitleCell(leconService));

        colContenu.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        colContenu.setCellFactory(column -> new LeconContenuCell(leconService));

        // Vidéo - Éditable inline
        colVideo.setCellValueFactory(new PropertyValueFactory<>("video"));
        colVideo.setCellFactory(column -> new LeconVideoCell(leconService));

        // Ordre - Éditable inline
        colOrdre.setCellValueFactory(new PropertyValueFactory<>("ordre"));
        colOrdre.setCellFactory(column -> new LeconOrdreCell(leconService, leconList));

        // Colonne Actions avec bouton de suppression uniquement
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button("🗑️");
            {
                btnDelete.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 5;");
                btnDelete.setOnAction(event -> {
                    Lecon lecon = getTableRow() != null ? getTableRow().getItem() : null;
                    if (lecon != null) {
                        supprimerLeconSelectionnee(lecon);
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDelete);
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
            showAlert(Alert.AlertType.WARNING, "⚠️", "Veuillez sélectionner un module");
            return;
        }

        int ordre = leconList.stream().mapToInt(Lecon::getOrdre).max().orElse(0) + 1;

        // Vérifier que l'ordre n'existe pas déjà dans ce module
        final int ordreVerif = ordre;
        final int moduleVerif = targetModuleId;
        boolean ordreExiste = leconList.stream()
                .anyMatch(l -> l.getOrdre() == ordreVerif && l.getModuleId() == moduleVerif);
        
        if (ordreExiste) {
            showAlert(Alert.AlertType.WARNING, "⚠️ Ordre déjà utilisé", 
                    "Une leçon avec l'ordre " + ordre + " existe déjà dans ce module.\nVeuillez choisir un autre ordre.");
            return;
        }

        Lecon lecon = new Lecon(
                txtTitre.getText().trim(),
                txtContenu.getText().trim(),
                videoBytes,           // ✅ BYTES ORIGINAUX - AUCUNE COMPRESSION
                ordre,
                targetModuleId
        );

        try {
            leconService.ajouter(lecon);
            chargerLeconsParModule(targetModuleId);
            showAlert(Alert.AlertType.INFORMATION, "✅ Succès", "Leçon ajoutée !");
            clearFields();
            // Return to table view
            setFormVisible(false);
            setTableVisible(true);
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.WARNING, "⚠️ Validation", e.getMessage());
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                showAlert(Alert.AlertType.WARNING, "⚠️ Validation", e.getCause().getMessage());
            } else {
                showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Erreur lors de l'ajout: " + e.getMessage());
            }
        }
    }

    @FXML
    private void modifierLecon() {
        if (leconSelectionnee == null) {
            showAlert(Alert.AlertType.WARNING, "⚠️", "Sélectionnez une leçon à modifier");
            return;
        }

        if (!validerFormulaire()) return;

        int ordre = 1;
        try {
            ordre = Integer.parseInt(txtOrdre.getText().trim());
        } catch (NumberFormatException e) {
            ordre = leconSelectionnee.getOrdre();
        }

        // Vérifier que l'ordre n'est pas utilisé par une autre leçon du même module
        final int ordreVerif = ordre;
        final int moduleVerif = leconSelectionnee.getModuleId();
        boolean ordreExiste = leconList.stream()
                .anyMatch(l -> l.getOrdre() == ordreVerif && 
                              l.getModuleId() == moduleVerif && 
                              l.getId() != leconSelectionnee.getId());
        
        if (ordreExiste) {
            showAlert(Alert.AlertType.WARNING, "⚠️ Ordre déjà utilisé", 
                    "Une autre leçon a déjà l'ordre " + ordre + " dans ce module.\nVeuillez choisir un autre ordre.");
            return;
        }

        leconSelectionnee.setTitre(txtTitre.getText().trim());
        leconSelectionnee.setContenu(txtContenu.getText().trim());

        if (videoBytes != null) {
            leconSelectionnee.setVideo(videoBytes);  // ✅ BYTES ORIGINAUX
        }

        leconSelectionnee.setOrdre(ordre);

        try {
            leconService.modifier(leconSelectionnee);
            chargerLeconsParModule(leconSelectionnee.getModuleId());
            showAlert(Alert.AlertType.INFORMATION, "✅ Succès", "Leçon modifiée !");
            clearFields();
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.WARNING, "⚠️ Validation", e.getMessage());
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                showAlert(Alert.AlertType.WARNING, "⚠️ Validation", e.getCause().getMessage());
            } else {
                showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Erreur lors de la modification: " + e.getMessage());
            }
        }
    }

    @FXML
    private void supprimerLecon() {
        if (leconSelectionnee == null) {
            showAlert(Alert.AlertType.WARNING, "⚠️", "Sélectionnez une leçon à supprimer");
            return;
        }
        supprimerLeconSelectionnee(leconSelectionnee);
    }

    private void supprimerLeconSelectionnee(Lecon lecon) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("🗑️ Confirmation");
        confirm.setHeaderText("Supprimer la leçon");
        confirm.setContentText("Voulez-vous supprimer la leçon : \"" + lecon.getTitre() + "\" ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            int moduleLecon = lecon.getModuleId();
            if (lecon.getId() > 0) {
                leconService.supprimer(lecon.getId());
            }
            chargerLeconsParModule(moduleLecon);
            showAlert(Alert.AlertType.INFORMATION, "✅ Succès", "Leçon supprimée !");
            clearFields();
        }
    }

    @FXML
    private void annuler() {
        clearFields();
    }

    @FXML
    private void fermer() {
        Stage stage = (Stage) txtTitre.getScene().getWindow();
        if (previousScene != null) {
            stage.setScene(previousScene);
            stage.show();
        } else {
            stage.close();
        }
    }

    private boolean validerFormulaire() {
        StringBuilder errors = new StringBuilder();

        // Validation du titre
        String titre = txtTitre.getText().trim();
        if (titre.isEmpty()) {
            errors.append("• Le titre est obligatoire\n");
        } else if (titre.length() < 3) {
            errors.append("• Le titre doit contenir au moins 3 caractères\n");
        } else if (titre.length() > 200) {
            errors.append("• Le titre ne peut pas dépasser 200 caractères\n");
        }

        // Validation du contenu
        String contenu = txtContenu.getText().trim();
        if (contenu.isEmpty()) {
            errors.append("• Le contenu est obligatoire\n");
        } else if (contenu.length() < 10) {
            errors.append("• Le contenu doit contenir au moins 10 caractères\n");
        } else if (contenu.length() > 10000) {
            errors.append("• Le contenu ne peut pas dépasser 10000 caractères\n");
        }

        // Validation de l'ordre
        String ordreText = txtOrdre.getText().trim();
        if (!ordreText.isEmpty()) {
            try {
                int ordre = Integer.parseInt(ordreText);
                if (ordre <= 0) {
                    errors.append("• L'ordre doit être un nombre positif\n");
                } else if (ordre > 100) {
                    errors.append("• L'ordre ne peut pas dépasser 100\n");
                }
            } catch (NumberFormatException e) {
                errors.append("• L'ordre doit être un nombre entier valide\n");
            }
        }

        // Validation de la vidéo (taille maximale = limite MySQL max_allowed_packet)
        if (videoBytes != null && videoBytes.length > 64 * 1024 * 1024) { // 64 MB max
            errors.append("• La vidéo ne peut pas dépasser 64 MB (limite MySQL)\n");
        }

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.WARNING, "⚠️ Validation", "Veuillez corriger :\n\n" + errors);
            return false;
        }

        return true;
    }

    private void clearFields() {
        txtTitre.clear();
        txtContenu.clear();
        txtOrdre.clear();

        // ✅ Réinitialiser vidéo
        videoBytes = null;
        videoNom = null;
        lblVideoNom.setText("Aucune vidéo");
        lblTailleVideo.setText("");

        tableLecons.getSelectionModel().clearSelection();
        leconSelectionnee = null;
        txtTitre.requestFocus();
    }

    private void setFormVisible(boolean visible) {
        if (formBox != null) {
            formBox.setVisible(visible);
            formBox.setManaged(visible);
        }
        if (btnToggleForm != null) {
            btnToggleForm.setText(visible ? "✖️" : "➕");
            btnToggleForm.setStyle(visible ?
                "-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-background-radius: 20; -fx-font-size: 16px; -fx-padding: 8 12;" :
                "-fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 20; -fx-font-size: 16px; -fx-padding: 8 12;");
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
            // Show form for adding, hide table
            clearFields();
            leconSelectionnee = null;
            tableLecons.getSelectionModel().clearSelection();
            setTableVisible(false);
            setFormVisible(true);
        } else {
            // Hide form, show table
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
