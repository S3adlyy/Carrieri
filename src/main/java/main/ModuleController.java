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
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import services.ModuleService;

import java.util.List;
import java.util.Optional;

public class ModuleController {

    @FXML private TextField txtTitre;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtOrdre;
    @FXML private Label lblCoursInfo;

    @FXML private TableView<Module> tableModules;
    @FXML private TableColumn<Module, Integer> colId;
    @FXML private TableColumn<Module, String> colTitre;
    @FXML private TableColumn<Module, String> colDescription;
    @FXML private TableColumn<Module, Integer> colOrdre;
    @FXML private TableColumn<Module, Void> colActions;

    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnAnnuler;

    private ModuleService moduleService = new ModuleService();
    private ObservableList<Module> moduleList = FXCollections.observableArrayList();

    private int coursId = 0;
    private boolean modeAjoutApresCours = false;
    private boolean modulesAjoutes = false;
    private Module moduleSelectionne = null;

    @FXML
    public void initialize() {
        setupTableColumns();

        btnModifier.setDisable(true);
        btnSupprimer.setDisable(true);

        tableModules.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                moduleSelectionne = newSelection;
                chargerModuleFormulaire(newSelection);
                btnModifier.setDisable(false);
                btnSupprimer.setDisable(false);
            } else {
                moduleSelectionne = null;
                btnModifier.setDisable(true);
                btnSupprimer.setDisable(true);
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
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colOrdre.setCellValueFactory(new PropertyValueFactory<>("ordre"));

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("✏️ Modifier");
            private final Button btnDelete = new Button("🗑️ Supprimer");
            private final HBox box = new HBox(5);

            {
                btnEdit.setStyle("-fx-background-color: #E0B1CB; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 5;");
                btnEdit.setOnAction(event -> {
                    Module module = getTableView().getItems().get(getIndex());
                    tableModules.getSelectionModel().select(module);
                    chargerModuleFormulaire(module);
                });

                btnDelete.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 5;");
                btnDelete.setOnAction(event -> {
                    Module module = getTableView().getItems().get(getIndex());
                    supprimerModuleSelectionne(module); // ← RENOMMÉ
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

        int ordre = 1;
        try {
            ordre = Integer.parseInt(txtOrdre.getText().trim());
        } catch (NumberFormatException e) {
            ordre = moduleList.size() + 1;
        }

        Module module = new Module(
                txtTitre.getText().trim(),
                txtDescription.getText().trim(),
                ordre,
                coursId
        );

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
    }

    @FXML
    private void modifierModule() {
        if (moduleSelectionne == null) {
            showAlert(Alert.AlertType.WARNING, "⚠️", "Sélectionnez un module à modifier");
            return;
        }

        int ordre = 1;
        try {
            ordre = Integer.parseInt(txtOrdre.getText().trim());
        } catch (NumberFormatException e) {
            ordre = moduleSelectionne.getOrdre();
        }

        moduleSelectionne.setTitre(txtTitre.getText().trim());
        moduleSelectionne.setDescription(txtDescription.getText().trim());
        moduleSelectionne.setOrdre(ordre);

        if (coursId > 0) {
            moduleService.modifier(moduleSelectionne);
        }

        tableModules.refresh();
        showAlert(Alert.AlertType.INFORMATION, "✅ Succès", "Module modifié !");
        clearFields();
    }

    @FXML
    private void supprimerModule() {
        if (moduleSelectionne == null) {
            showAlert(Alert.AlertType.WARNING, "⚠️", "Sélectionnez un module à supprimer");
            return;
        }
        supprimerModuleSelectionne(moduleSelectionne);
    }

    private void supprimerModuleSelectionne(Module module) { // ← NOUVELLE MÉTHODE
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
    private void annuler() {
        clearFields();
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

            Stage stage = new Stage();
            stage.setTitle("Gestion des leçons - " + moduleSelectionne.getTitre());
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "❌ Erreur", "Impossible d'ouvrir la gestion des leçons");
        }
    }

    @FXML
    private void fermer() {
        Stage stage = (Stage) txtTitre.getScene().getWindow();
        stage.close();
    }

    private boolean validerFormulaire() {
        if (txtTitre.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "⚠️", "Le titre du module est obligatoire");
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