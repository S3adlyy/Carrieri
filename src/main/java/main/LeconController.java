package main;

import entities.Lecon;
import entities.Module;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
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
    @FXML private ComboBox<String> comboType;
    @FXML private ComboBox<Module> comboModules;
    @FXML private Label lblInfo;

    // Vidéo fields - SIMPLE
    @FXML private Button btnChoisirVideo;
    @FXML private Label lblVideoNom;
    @FXML private Label lblTailleVideo;

    // TableView
    @FXML private TableView<Lecon> tableLecons;
    @FXML private TableColumn<Lecon, Integer> colId;
    @FXML private TableColumn<Lecon, String> colTitre;
    @FXML private TableColumn<Lecon, String> colContenu;
    @FXML private TableColumn<Lecon, String> colVideo;
    @FXML private TableColumn<Lecon, Integer> colOrdre;
    @FXML private TableColumn<Lecon, String> colType;
    @FXML private TableColumn<Lecon, Void> colActions;

    // Buttons
    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnAnnuler;

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

    // ============================================
    // INITIALIZATION
    // ============================================

    @FXML
    public void initialize() {
        setupTableColumns();
        setupComboBoxes();
        setupVideoChooser();  // ✅ VERSION SIMPLE

        comboType.setItems(FXCollections.observableArrayList("Leçon", "Quiz", "Examen"));
        comboType.setValue("Leçon");

        btnModifier.setDisable(true);
        btnSupprimer.setDisable(true);

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
                btnModifier.setDisable(false);
                btnSupprimer.setDisable(false);
            } else {
                leconSelectionnee = null;
                btnModifier.setDisable(true);
                btnSupprimer.setDisable(true);
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
        comboModules.setDisable(false);
        chargerModules();
        leconList.clear();
        tableLecons.setItems(leconList);
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

        String type = lecon.getType();
        if (type == null) comboType.setValue("Leçon");
        else if (type.equals("QUIZ")) comboType.setValue("Quiz");
        else if (type.equals("EXAM")) comboType.setValue("Examen");
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colContenu.setCellValueFactory(new PropertyValueFactory<>("contenu"));

        colVideo.setCellValueFactory(data -> {
            byte[] video = data.getValue().getVideo();
            if (video != null && video.length > 0) {
                return new javafx.beans.property.SimpleStringProperty("🎥 " + formatTaille(video.length));
            } else {
                return new javafx.beans.property.SimpleStringProperty("❌");
            }
        });

        colOrdre.setCellValueFactory(new PropertyValueFactory<>("ordre"));

        colType.setCellValueFactory(data -> {
            String type = data.getValue().getType();
            String display = type == null ? "Leçon" :
                    type.equals("QUIZ") ? "Quiz" :
                            type.equals("EXAM") ? "Examen" : "Leçon";
            return new javafx.beans.property.SimpleStringProperty(display);
        });

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("✏️");
            private final Button btnDelete = new Button("🗑️");
            private final HBox box = new HBox(5);
            {
                btnEdit.setStyle("-fx-background-color: #E0B1CB; -fx-text-fill: white; -fx-background-radius: 5;");
                btnEdit.setOnAction(event -> {
                    Lecon lecon = getTableView().getItems().get(getIndex());
                    tableLecons.getSelectionModel().select(lecon);
                });

                btnDelete.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-background-radius: 5;");
                btnDelete.setOnAction(event -> {
                    Lecon lecon = getTableView().getItems().get(getIndex());
                    supprimerLeconSelectionnee(lecon);
                });

                box.getChildren().addAll(btnEdit, btnDelete);
                box.setAlignment(javafx.geometry.Pos.CENTER);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
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

        int ordre = 1;
        try {
            ordre = Integer.parseInt(txtOrdre.getText().trim());
        } catch (NumberFormatException e) {
            ordre = leconList.size() + 1;
        }

        String type = comboType.getValue();
        if ("Quiz".equals(type)) type = "QUIZ";
        else if ("Examen".equals(type)) type = "EXAM";
        else type = null;

        Lecon lecon = new Lecon(
                txtTitre.getText().trim(),
                txtContenu.getText().trim(),
                videoBytes,           // ✅ BYTES ORIGINAUX - AUCUNE COMPRESSION
                ordre,
                targetModuleId
        );

        lecon.setType(type);
        leconService.ajouter(lecon);

        chargerLeconsParModule(targetModuleId);
        showAlert(Alert.AlertType.INFORMATION, "✅ Succès", "Leçon ajoutée !");
        clearFields();
    }

    @FXML
    private void modifierLecon() {
        if (leconSelectionnee == null) {
            showAlert(Alert.AlertType.WARNING, "⚠️", "Sélectionnez une leçon à modifier");
            return;
        }

        int ordre = 1;
        try {
            ordre = Integer.parseInt(txtOrdre.getText().trim());
        } catch (NumberFormatException e) {
            ordre = leconSelectionnee.getOrdre();
        }

        String type = comboType.getValue();
        if ("Quiz".equals(type)) type = "QUIZ";
        else if ("Examen".equals(type)) type = "EXAM";
        else type = null;

        leconSelectionnee.setTitre(txtTitre.getText().trim());
        leconSelectionnee.setContenu(txtContenu.getText().trim());

        if (videoBytes != null) {
            leconSelectionnee.setVideo(videoBytes);  // ✅ BYTES ORIGINAUX
        }

        leconSelectionnee.setOrdre(ordre);
        leconSelectionnee.setType(type);

        leconService.modifier(leconSelectionnee);

        chargerLeconsParModule(leconSelectionnee.getModuleId());
        showAlert(Alert.AlertType.INFORMATION, "✅ Succès", "Leçon modifiée !");
        clearFields();
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
        stage.close();
    }

    private boolean validerFormulaire() {
        if (txtTitre.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "⚠️", "Le titre est obligatoire");
            return false;
        }
        return true;
    }

    private void clearFields() {
        txtTitre.clear();
        txtContenu.clear();
        txtOrdre.clear();
        comboType.setValue("Leçon");

        // ✅ Réinitialiser vidéo
        videoBytes = null;
        videoNom = null;
        lblVideoNom.setText("Aucune vidéo");
        lblTailleVideo.setText("");

        tableLecons.getSelectionModel().clearSelection();
        leconSelectionnee = null;
        btnModifier.setDisable(true);
        btnSupprimer.setDisable(true);
        txtTitre.requestFocus();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}