package com.example.guser.controllers.greclam;

import entities.greclam.Reclamation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import services.greclam.PrioriteService;
import services.greclam.ReclamationService;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class PrioriteDashboardController implements Initializable {

    @FXML private Label urgentCount;
    @FXML private Label hauteCount;
    @FXML private Label moyenneCount;
    @FXML private Label basseCount;
    @FXML private PieChart prioriteChart;

    private PrioriteService prioriteService = new PrioriteService();
    private ReclamationService reclamationService = new ReclamationService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chargerStatistiques();
    }

    private void chargerStatistiques() {
        try {
            List<Reclamation> list = reclamationService.read();
            Map<String, Integer> stats = prioriteService.getStatsPriorites(list);

            urgentCount.setText(String.valueOf(stats.get("URGENT")));
            hauteCount.setText(String.valueOf(stats.get("HAUTE")));
            moyenneCount.setText(String.valueOf(stats.get("MOYENNE")));
            basseCount.setText(String.valueOf(stats.get("BASSE")));

            // Créer le graphique
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                    new PieChart.Data("URGENT", stats.get("URGENT")),
                    new PieChart.Data("HAUTE", stats.get("HAUTE")),
                    new PieChart.Data("MOYENNE", stats.get("MOYENNE")),
                    new PieChart.Data("BASSE", stats.get("BASSE"))
            );

            prioriteChart.setData(pieData);
            prioriteChart.setTitle("Répartition des priorités");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}