package main;

import entities.Module;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import services.ModuleService;
import services.QuizAutoGenerator;
import services.LeconService;
import utils.AlertUtils;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class ModuleController implements Initializable {

    @FXML private TextField txtTitre;
    @FXML private TextArea txtDescription;
    @FXML private Label lblCoursInfo;
    @FXML private VBox formBox;
    @FXML private VBox tablePane;
    @FXML private TableView<Module> tableModules;
    @FXML private TableColumn<Module, Integer> colId;
    @FXML private TableColumn<Module, String> colTitre;
    @FXML private TableColumn<Module, String> colDescription;
    @FXML private TableColumn<Module, Integer> colOrdre;
    @FXML private TableColumn<Module, Void> colActions;
    @FXML private Button btnAjouter;
    @FXML private Button btnToggleForm;

    // ✅ LABELS D'ERREUR
    @FXML private Label errorTitre;
    @FXML private Label errorDescription;

    private ModuleService moduleService = new ModuleService();
    private LeconService leconService = new LeconService();
    private ObservableList<Module> moduleList = FXCollections.observableArrayList();
    private int coursId = 0;
    private String coursTitre = "";
    private Module moduleSelectionne = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();

        setFormVisible(false);
        setTableVisible(true);

        setupValidation();

        tableModules.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                moduleSelectionne = newSelection;
                MainShellController.getInstance().setCurrentModule(newSelection.getId(), newSelection.getTitre());
            } else {
                moduleSelectionne = null;
            }
        });
    }

    private void setupValidation() {
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

        txtDescription.textProperty().addListener((obs, oldVal, newVal) -> {
            String desc = newVal != null ? newVal.trim() : "";
            if (desc.isEmpty()) {
                showError(errorDescription, "La description est obligatoire");
            } else if (desc.length() < 10) {
                showError(errorDescription, "La description doit contenir au moins 10 caractères");
            } else if (desc.length() > 1000) {
                showError(errorDescription, "La description ne peut pas dépasser 1000 caractères");
            } else if (isOnlyDigits(desc)) {
                showError(errorDescription, "La description ne peut pas être composée uniquement de chiffres");
            } else {
                hideError(errorDescription);
            }
        });
    }

    private void showError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        errorLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11px; -fx-font-weight: 600;");
        // Ajouter la classe 'error' au champ correspondant
        if (errorLabel == errorTitre) {
            txtTitre.getStyleClass().add("error");
        } else if (errorLabel == errorDescription) {
            txtDescription.getStyleClass().add("error");
        }
    }

    // ✅ MODIFIEZ hideError() pour RETIRER la classe error
    private void hideError(Label errorLabel) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Retirer la classe 'error' du champ
        if (errorLabel == errorTitre) {
            txtTitre.getStyleClass().remove("error");
        } else if (errorLabel == errorDescription) {
            txtDescription.getStyleClass().remove("error");
        }
    }

    private boolean isOnlyDigits(String text) {
        if (text == null || text.isEmpty()) return false;
        String textWithoutSpaces = text.replaceAll("\\s+", "");
        return textWithoutSpaces.matches("\\d+");
    }

    public void setCoursInfo(int id, String titre) {
        this.coursId = id;
        this.coursTitre = titre;
        lblCoursInfo.setText("📌 " + titre);
        chargerModules();
    }

    public void setCoursId(int id) {
        this.coursId = id;
        this.coursTitre = "Cours #" + id;
        lblCoursInfo.setText("📌 Cours #" + id);
        chargerModules();
    }

    private void chargerModuleFormulaire(Module module) {
        txtTitre.setText(module.getTitre());
        txtDescription.setText(module.getDescription());
    }

    private void setupTableColumns() {

        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colTitre.setCellFactory(column -> ModuleCell.getTitleCell());

        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colDescription.setCellFactory(column -> ModuleCell.getDescriptionCell());

        colOrdre.setCellValueFactory(new PropertyValueFactory<>("ordre"));
        colOrdre.setCellFactory(column -> ModuleCell.getOrdreCell(moduleList));

        colActions.setCellFactory(param -> new TableCell<Module, Void>() {
            private final Button btnDelete = new Button("🗑️");
            private final HBox actions = new HBox(5, btnDelete);

            {
                actions.setAlignment(javafx.geometry.Pos.CENTER);
                btnDelete.getStyleClass().addAll("action-button", "btn-delete-gradient-light");

                btnDelete.setOnAction(event -> {
                    Module module = getTableRow() != null ? getTableRow().getItem() : null;
                    if (module != null) {
                        supprimerModule(module);
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

        tableModules.setItems(moduleList);
    }

    private void chargerModules() {
        if (coursId > 0) {
            try {
                List<Module> modules = moduleService.getModulesByCours(coursId);
                moduleList.setAll(modules);
                System.out.println("📚 Modules chargés pour " + coursTitre + ": " + modules.size());
            } catch (Exception e) {
                AlertUtils.showError("❌ Erreur", "Erreur lors du chargement des modules :\n" + e.getMessage());
            }
        }
    }

    // ============================================
    // AJOUTER MODULE - MODIFIÉ
    // ============================================
    @FXML
    private void ajouterModule() {
        if (!validerFormulaire()) return;

        int ordre = moduleList.stream().mapToInt(Module::getOrdre).max().orElse(0) + 1;

        Module module = new Module(
                txtTitre.getText().trim(),
                txtDescription.getText().trim(),
                ordre,
                coursId
        );

        try {
            if (coursId > 0) {
                moduleService.ajouter(module);

                // ✅ NOUVELLE ALERTE AVEC INSTRUCTIONS
                AlertUtils.showSuccessWithInstructions(
                        "✅ Module créé avec succès",
                        "Le module \"" + txtTitre.getText() + "\" a été ajouté au cours.",
                        "Utilisez le bouton 📖 pour ajouter des leçons à ce module.\n\n" +
                                "Vous pourrez également générer un quiz pour ce module après avoir ajouté des leçons."
                );

                chargerModules();
                clearFields();
                setFormVisible(false);
                setTableVisible(true);
            } else {
                AlertUtils.showError("❌ Erreur", "ID de cours invalide");
            }

        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message.contains("titre")) {
                showError(errorTitre, message);
            } else if (message.contains("description")) {
                showError(errorDescription, message);
            } else {
                AlertUtils.showWarning("⚠️ Validation", message);
            }
        } catch (Exception e) {
            AlertUtils.showError("❌ Erreur", "Impossible d'ajouter le module :\n" + e.getMessage());
        }
    }

    // ============================================
    // SUPPRIMER MODULE - MODIFIÉ
    // ============================================
    private void supprimerModule(Module module) {
        boolean confirmed = AlertUtils.showDeleteConfirmation(
                "module",
                module.getTitre(),
                "Toutes les leçons associées à ce module seront également supprimées définitivement.\n\nCette action est irréversible !"
        );

        if (confirmed) {
            try {
                moduleService.supprimer(module.getId());

                if (moduleSelectionne != null && moduleSelectionne.getId() == module.getId()) {
                    MainShellController.getInstance().resetCurrentModule();
                }

                AlertUtils.showSuccess("✅ Suppression réussie", "Le module a été supprimé avec succès.");
                chargerModules();
                clearFields();
            } catch (Exception e) {
                AlertUtils.showError("❌ Erreur", "Impossible de supprimer le module :\n" + e.getMessage());
            }
        }
    }

    // ============================================
    // GÉNÉRER QUIZ AUTOMATIQUE - MODIFIÉ
    // ============================================
    @FXML
    private void genererQuizAutomatique() {
        if (moduleSelectionne == null) {
            AlertUtils.showNoSelectionWarning("module", "générer un quiz");
            return;
        }

        List<entities.Lecon> lecons = leconService.getLeconsByModule(moduleSelectionne.getId());
        if (lecons == null || lecons.isEmpty()) {
            AlertUtils.showWarning("⚠ Impossible de générer le quiz",
                    "Le module \"" + moduleSelectionne.getTitre() + "\" ne contient aucune leçon.\n\n" +
                            "Ajoutez d'abord des leçons à ce module pour pouvoir générer un quiz.");
            return;
        }

        boolean confirmed = AlertUtils.showConfirmation(
                "🤖 Génération automatique du quiz",
                "5 questions seront créées à partir du contenu des leçons.\n\n" +
                        "⚠ Les anciennes questions du quiz seront définitivement supprimées.\n\n" +
                        "Voulez-vous continuer ?",
                "Oui, générer",
                "Non, annuler"
        );

        if (confirmed) {
            try {
                Connection con = utils.MyDatabase.getInstance().getConnection();

                String deleteReponses = "DELETE FROM reponse WHERE question_id IN (SELECT id FROM question_quiz WHERE module_id = ?) AND question_type = 'QUIZ'";
                PreparedStatement ps1 = con.prepareStatement(deleteReponses);
                ps1.setInt(1, moduleSelectionne.getId());
                ps1.executeUpdate();

                String deleteQuestions = "DELETE FROM question_quiz WHERE module_id = ?";
                PreparedStatement ps2 = con.prepareStatement(deleteQuestions);
                ps2.setInt(1, moduleSelectionne.getId());
                ps2.executeUpdate();

                QuizAutoGenerator generator = new QuizAutoGenerator();
                generator.genererQuizModule(moduleSelectionne.getId());

                AlertUtils.showSuccess("✅ Quiz généré avec succès",
                        "Le quiz du module \"" + moduleSelectionne.getTitre() + "\" a été généré avec 5 questions.\n\n" +
                                "Les candidats pourront maintenant passer ce quiz après avoir terminé toutes les leçons du module.");

            } catch (SQLException e) {
                e.printStackTrace();
                AlertUtils.showError("❌ Erreur", "Impossible de générer le quiz :\n" + e.getMessage());
            }
        }
    }

    @FXML
    private void toggleForm() {
        boolean isVisible = formBox != null && formBox.isVisible();
        if (!isVisible) {
            clearFields();
            moduleSelectionne = null;
            tableModules.getSelectionModel().clearSelection();
            setTableVisible(false);
            setFormVisible(true);
        } else {
            setFormVisible(false);
            setTableVisible(true);
        }
    }

    private boolean validerFormulaire() {
        StringBuilder errors = new StringBuilder();

        String titre = txtTitre.getText().trim();
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

        String description = txtDescription.getText().trim();
        if (description.isEmpty()) {
            showError(errorDescription, "La description est obligatoire");
        } else if (description.length() < 10) {
            showError(errorDescription, "La description doit contenir au moins 10 caractères");
        } else if (description.length() > 1000) {
            showError(errorDescription, "La description ne peut pas dépasser 1000 caractères");
        } else if (isOnlyDigits(description)) {
            showError(errorDescription, "La description ne peut pas être composée uniquement de chiffres");
        } else {
            hideError(errorDescription);
        }
        if (errors.length() > 0) {
            AlertUtils.showWarning("⚠️ Formulaire incomplet", errors.toString());
            return false;
        }
        return true;
    }

    private void clearFields() {
        txtTitre.clear();
        txtDescription.clear();
        tableModules.getSelectionModel().clearSelection();
        moduleSelectionne = null;

        // ✅ RETIRER LES CLASSES ERROR
        txtTitre.getStyleClass().remove("error");
        txtDescription.getStyleClass().remove("error");

        hideError(errorTitre);
        hideError(errorDescription);
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
}