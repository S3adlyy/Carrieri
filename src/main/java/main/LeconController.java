package main;

import entities.Lecon;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.LeconService;

import java.util.List;

public class LeconController {

    @FXML
    private TextField txtTitre;

    @FXML
    private TextArea txtContenu;

    @FXML
    private TextField txtVideoUrl;

    @FXML
    private TextField txtOrdre;

    @FXML
    private TextField txtModuleId;

    @FXML
    private TableView<Lecon> tableLecon; // <-- ici le nom correspond au FXML

    @FXML
    private TableColumn<Lecon, Integer> colId;

    @FXML
    private TableColumn<Lecon, String> colTitre;

    @FXML
    private TableColumn<Lecon, Integer> colOrdre;

    private LeconService leconService = new LeconService();
    private ObservableList<Lecon> leconList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()).asObject());
        colTitre.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getTitre()));
        colOrdre.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getOrdre()).asObject());
    }

    @FXML
    private void chargerParModule() {
        int moduleId = Integer.parseInt(txtModuleId.getText());
        List<Lecon> list = leconService.getLeconsByModule(moduleId);
        leconList.setAll(list);
        tableLecon.setItems(leconList); // <-- ici aussi
    }

    @FXML
    private void ajouterLecon() {
        Lecon l = new Lecon(
                txtTitre.getText(),
                txtContenu.getText(),
                txtVideoUrl.getText(),
                Integer.parseInt(txtOrdre.getText()),
                Integer.parseInt(txtModuleId.getText())
        );

        leconService.ajouter(l);
        chargerParModule();
        clearFields();
    }

    private void clearFields() {
        txtTitre.clear();
        txtContenu.clear();
        txtVideoUrl.clear();
        txtOrdre.clear();
    }
}
