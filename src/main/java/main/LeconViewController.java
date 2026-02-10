package main;

import entities.Lecon;
import entities.Module;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import services.LeconService;
import services.ProgressionLeconService;

import java.util.List;

public class LeconViewController {

    @FXML
    private VBox boxLecons;

    private LeconService leconService = new LeconService();
    private ProgressionLeconService progressionService = new ProgressionLeconService();

    private int candidatId = 1;

    public void setModule(Module module) {

        List<Lecon> lecons = leconService.getLeconsByModule(module.getId());

        boxLecons.getChildren().clear();

        for (Lecon l : lecons) {
            Button btn = new Button(l.getOrdre() + " - " + l.getTitre());
            btn.setOnAction(e -> {
                progressionService.marquerTerminee(candidatId, l.getId());
                btn.setText(l.getTitre() + " ✓");
            });

            boxLecons.getChildren().add(btn);
        }
    }
}
