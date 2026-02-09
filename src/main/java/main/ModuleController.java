package main;

import entities.Module;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.ModuleService;

import java.util.List;

public class ModuleController {

    @FXML
    private TextField txtTitre;

    @FXML
    private TextArea txtDescription;

    @FXML
    private TextField txtOrdre;

    @FXML
    private TextField txtCoursId;

    @FXML
    private TableView<Module> tableModules;

    @FXML
    private TableColumn<Module, Integer> colId;

    @FXML
    private TableColumn<Module, String> colTitre;

    @FXML
    private TableColumn<Module, String> colDescription;

    @FXML
    private TableColumn<Module, Integer> colOrdre;

    @FXML
    private TableColumn<Module, Integer> colCoursId;

    private ModuleService moduleService = new ModuleService();
    private ObservableList<Module> moduleList = FXCollections.observableArrayList();

    private Module selectedModule = null;

    @FXML
    public void initialize() {

        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()).asObject());
        colTitre.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getTitre()));
        colDescription.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDescription()));
        colOrdre.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getOrdre()).asObject());
        colCoursId.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getCoursId()).asObject());

        loadModules();

        tableModules.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedModule = newVal;
            if (newVal != null) {
                txtTitre.setText(newVal.getTitre());
                txtDescription.setText(newVal.getDescription());
                txtOrdre.setText(String.valueOf(newVal.getOrdre()));
                txtCoursId.setText(String.valueOf(newVal.getCoursId()));
            }
        });
    }

    private void loadModules() {
        List<Module> list = moduleService.getAll();
        moduleList.setAll(list);
        tableModules.setItems(moduleList);
    }

    @FXML
    private void ajouterModule() {
        Module m = new Module(
                txtTitre.getText(),
                txtDescription.getText(),
                Integer.parseInt(txtOrdre.getText()),
                Integer.parseInt(txtCoursId.getText())
        );

        moduleService.ajouter(m);
        loadModules();
        clearFields();
    }

    @FXML
    private void modifierModule() {
        if (selectedModule == null) return;

        selectedModule.setTitre(txtTitre.getText());
        selectedModule.setDescription(txtDescription.getText());
        selectedModule.setOrdre(Integer.parseInt(txtOrdre.getText()));
        selectedModule.setCoursId(Integer.parseInt(txtCoursId.getText()));

        moduleService.modifier(selectedModule);
        loadModules();
    }

    @FXML
    private void supprimerModule() {
        if (selectedModule == null) return;

        moduleService.supprimer(selectedModule.getId());
        loadModules();
        clearFields();
    }

    private void clearFields() {
        txtTitre.clear();
        txtDescription.clear();
        txtOrdre.clear();
        txtCoursId.clear();
        selectedModule = null;
    }
}
