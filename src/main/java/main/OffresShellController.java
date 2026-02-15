package main;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class OffresShellController {

    private static OffresShellController instance;

    @FXML private StackPane contentPane;

    @FXML private Button btnOffresList;
    @FXML private Button btnOffreAdd;
    @FXML private Button btnPostulationsList; // New button

    @FXML private Circle navAvatar;
    @FXML private Label navNameLabel;

    private Button activeButton = null;

    public OffresShellController() {
        instance = this;
    }

    public static OffresShellController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        navNameLabel.setText("Ons Nagara");
        showOffreAdd();
        setActiveButton(btnOffreAdd);
    }

    @FXML
    public void showOffresList() {
        loadViewWithFade("offres-list.fxml");
        setActiveButton(btnOffresList);
    }

    @FXML
    public void showOffreAdd() {
        loadViewWithFade("offre-add.fxml");
        setActiveButton(btnOffreAdd);
    }

    @FXML
    public void showPostulationsList() { // New method
        loadViewWithFade("postulations-list.fxml");
        setActiveButton(btnPostulationsList);
    }

    /** Called from OffresListController when user clicks "Postuler" on a row */
    public void showPostuler(int offreId, String offreTitre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/postuler.fxml"));
            Node view = loader.load();

            // Pass offre info to the PostulerController
            PostulerController ctrl = loader.getController();
            ctrl.setOffreInfo(offreId, offreTitre);

            contentPane.getChildren().setAll(view);

            FadeTransition ft = new FadeTransition(Duration.millis(250), view);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadViewWithFade(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/" + fxml));
            Node view = loader.load();

            contentPane.getChildren().setAll(view);

            FadeTransition ft = new FadeTransition(Duration.millis(250), view);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setActiveButton(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("liquid-btn-active");
        }
        activeButton = button;
        if (button != null && !button.getStyleClass().contains("liquid-btn-active")) {
            button.getStyleClass().add("liquid-btn-active");
        }
    }
}