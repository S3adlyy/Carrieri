package main;

import entities.Module;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;

import services.ModuleService;
import services.QuizAutoGenerator;
import services.LeconService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ModuleController {

    @FXML private TextField txtTitre;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtOrdre;
    @FXML private Label lblCoursInfo;
    @FXML private VBox formBox;
    @FXML private VBox tablePane;

    @FXML private TableView<Module> tableModules;
    @FXML private TableColumn<Module, Integer> colId;
    @FXML private TableColumn<Module, String> colTitre;
    @FXML private TableColumn<Module, String> colDescription;
    @FXML private TableColumn<Module, Integer> colOrdre;
    @FXML private TableColumn<Module, Void> colActions;

    // Buttons
    @FXML private Button btnAjouter;
    @FXML private Button btnToggleForm;

    private ModuleService moduleService = new ModuleService();
    private LeconService leconService = new LeconService();
    private ObservableList<Module> moduleList = FXCollections.observableArrayList();

    private int coursId = 0;
    private boolean modeAjoutApresCours = false;
    private boolean modulesAjoutes = false;
    private Module moduleSelectionne = null;
    private Scene previousScene;

    public void setPreviousScene(Scene previousScene) {
        this.previousScene = previousScene;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        setupNumericFieldsOnly();

        lblCoursInfo.setVisible(false);
        lblCoursInfo.setManaged(false);
        txtOrdre.setVisible(false);
        txtOrdre.setManaged(false);

        // Start with table visible and form hidden
        setFormVisible(false);
        setTableVisible(true);

        tableModules.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                moduleSelectionne = newSelection;
                chargerModuleFormulaire(newSelection);
            } else {
                moduleSelectionne = null;
            }
        });
    }

    private void setupNumericFieldsOnly() {
        // Restreindre le champ ordre aux nombres uniquement
        txtOrdre.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtOrdre.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }
    public void setCoursId(int id) {
        this.coursId = id;
        lblCoursInfo.setText("Cours ID: " + id);
        chargerModules();
    }

    public void setModeAjoutApresCours(boolean mode) {
        this.modeAjoutApresCours = mode;
    }

    public boolean aDesModulesAjoutes() {
        return modulesAjoutes;
    }

    private void chargerModuleFormulaire(Module module) {
        txtTitre.setText(module.getTitre());
        txtDescription.setText(module.getDescription());
        txtOrdre.setText(String.valueOf(module.getOrdre()));
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setVisible(false);

        // Titre - Éditable inline
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colTitre.setCellFactory(column -> new ModuleTitleCell(moduleService));

        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colDescription.setCellFactory(column -> new ModuleDescriptionCell(moduleService));

        // Ordre - Éditable inline
        colOrdre.setCellValueFactory(new PropertyValueFactory<>("ordre"));
        colOrdre.setCellFactory(column -> new ModuleOrdreCell(moduleService, moduleList));

        // Colonne Actions avec bouton de suppression uniquement
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button("🗑️");
            {
                btnDelete.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 5;");
                btnDelete.setOnAction(event -> {
                    Module module = getTableRow() != null ? getTableRow().getItem() : null;
                    if (module != null) {
                        supprimerModuleSelectionne(module);
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

        tableModules.setItems(moduleList);
    }

    private void chargerModules() {
        if (coursId > 0) {
            List<Module> modules = moduleService.getModulesByCours(coursId);
            moduleList.setAll(modules);
        }
    }

    @FXML
    private void ajouterModule() {
        if (!validerFormulaire()) return;

        int ordre = moduleList.stream().mapToInt(Module::getOrdre).max().orElse(0) + 1;

        // Vérifier que l'ordre n'existe pas déjà
        final int ordreVerif = ordre;
        boolean ordreExiste = moduleList.stream()
                .anyMatch(m -> m.getOrdre() == ordreVerif);
        
        if (ordreExiste) {
            showAlert(Alert.AlertType.WARNING, "⚠️ Ordre déjà utilisé", 
                    "Un module avec l'ordre " + ordre + " existe déjà.\nVeuillez choisir un autre ordre.");
            return;
        }

        Module module = new Module(
                txtTitre.getText().trim(),
                txtDescription.getText().trim(),
                ordre,
                coursId
        );

        try {
            if (coursId > 0) {
                moduleService.ajouter(module);
                modulesAjoutes = true;

                List<Module> modules = moduleService.getModulesByCours(coursId);
                for (Module m : modules) {
                    if (m.getTitre().equals(module.getTitre()) && m.getOrdre() == ordre) {
                        module = m;
                        break;
                    }
                }
            }

            moduleList.add(module);
            showAlert(Alert.AlertType.INFORMATION, "✅ Succès", "Module ajouté !");
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
    private void supprimerModule() {
        if (moduleSelectionne == null) {
            showAlert(Alert.AlertType.WARNING, "⚠️", "Sélectionnez un module à supprimer");
            return;
        }
        supprimerModuleSelectionne(moduleSelectionne);
    }

    private void supprimerModuleSelectionne(Module module) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("🗑️ Confirmation");
        confirm.setHeaderText("Supprimer le module");
        confirm.setContentText("Voulez-vous supprimer le module : \"" + module.getTitre() + "\" ?\n\nToutes les leçons associées seront également supprimées !");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (module.getId() > 0) {
                moduleService.supprimer(module.getId());
            }
            moduleList.remove(module);
            showAlert(Alert.AlertType.INFORMATION, "✅ Succès", "Module supprimé !");
            clearFields();
        }
    }

    @FXML
    private void ajouterLecons() {
        if (moduleSelectionne == null) {
            showAlert(Alert.AlertType.WARNING, "⚠️", "Sélectionnez un module d'abord");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/lecon.fxml"));
            Parent root = loader.load();

            LeconController controller = loader.getController();
            controller.setModuleId(moduleSelectionne.getId());
            controller.setModuleTitre(moduleSelectionne.getTitre());

            Stage stage = (Stage) txtTitre.getScene().getWindow();
            controller.setPreviousScene(stage.getScene());
            stage.setTitle("Gestion des leçons - " + moduleSelectionne.getTitre());
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Impossible d'ouvrir la gestion des leçons");
        }
    }

    // ============================================
    // GÉNÉRER AUTOMATIQUEMENT LE QUIZ DU MODULE
    // ============================================

    @FXML
    private void genererQuizAutomatique() {
        if (moduleSelectionne == null) {
            showAlert(Alert.AlertType.WARNING, "⚠️ Attention", "Sélectionnez d'abord un module");
            return;
        }

        List<entities.Lecon> lecons = leconService.getLeconsByModule(moduleSelectionne.getId());
        if (lecons == null || lecons.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "⚠️ Attention", "Impossible de générer un quiz : ce module ne contient aucune leçon.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("🤖 Génération intelligente");
        confirm.setHeaderText("Générer le quiz du module ?");
        confirm.setContentText("5 questions INTELLIGENTES seront créées à partir d'une banque de questions JavaFX.\n\nLes anciennes questions seront supprimées.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Supprimer les anciennes questions
                Connection con = utils.MyDatabase.getInstance().getConnection();

                String deleteReponses = "DELETE FROM reponse WHERE question_id IN (SELECT id FROM question_quiz WHERE module_id = ?) AND question_type = 'QUIZ'";
                PreparedStatement ps1 = con.prepareStatement(deleteReponses);
                ps1.setInt(1, moduleSelectionne.getId());
                ps1.executeUpdate();

                String deleteQuestions = "DELETE FROM question_quiz WHERE module_id = ?";
                PreparedStatement ps2 = con.prepareStatement(deleteQuestions);
                ps2.setInt(1, moduleSelectionne.getId());
                ps2.executeUpdate();

                // Générer avec le NOUVEAU générateur intelligent
                QuizAutoGenerator generator = new QuizAutoGenerator();
                generator.genererQuizModule(moduleSelectionne.getId());

                showAlert(Alert.AlertType.INFORMATION, "✅ Succès",
                        "Quiz généré avec 5 questions INTELLIGENTES !\n\nLes questions sont maintenant logiques.");

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "❌ Erreur", e.getMessage());
            }
        }
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
            errors.append("• Le titre du module est obligatoire\n");
        } else if (titre.length() < 3) {
            errors.append("• Le titre doit contenir au moins 3 caractères\n");
        } else if (titre.length() > 200) {
            errors.append("• Le titre ne peut pas dépasser 200 caractères\n");
        }

        // Validation de la description
        String description = txtDescription.getText().trim();
        if (description.isEmpty()) {
            errors.append("• La description est obligatoire\n");
        } else if (description.length() < 10) {
            errors.append("• La description doit contenir au moins 10 caractères\n");
        } else if (description.length() > 1000) {
            errors.append("• La description ne peut pas dépasser 1000 caractères\n");
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

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.WARNING, "⚠️ Validation", "Veuillez corriger :\n\n" + errors);
            return false;
        }

        return true;
    }

    private void clearFields() {
        txtTitre.clear();
        txtDescription.clear();
        txtOrdre.clear();
        tableModules.getSelectionModel().clearSelection();
        moduleSelectionne = null;
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
            moduleSelectionne = null;
            tableModules.getSelectionModel().clearSelection();
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

