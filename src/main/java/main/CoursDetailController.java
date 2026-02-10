package main;

import entities.Cours;
import entities.Module;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.ModuleService;

import java.util.List;

public class CoursDetailController {

    @FXML
    private Label lblTitre;

    @FXML
    private VBox boxModules;

    private ModuleService moduleService = new ModuleService();

    public void setCours(Cours cours) {
        lblTitre.setText(cours.getTitre());

        List<Module> modules = moduleService.getModulesByCours(cours.getId());

        boxModules.getChildren().clear();

        for (Module m : modules) {
            Label lbl = new Label("Module " + m.getOrdre() + " : " + m.getTitre());
            lbl.setStyle("-fx-font-size:16px; -fx-padding:10;");
            lbl.setOnMouseClicked(e -> ouvrirLecons(m));
            boxModules.getChildren().add(lbl);
        }
    }

    private void ouvrirLecons(Module module) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/LeconView.fxml"));
            Parent root = loader.load();

            LeconViewController controller = loader.getController();
            controller.setModule(module);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle(module.getTitre());
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
