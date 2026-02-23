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
    @FXML private Button btnOffresTable;
    @FXML private Button btnOffreAdd;
    @FXML private Button btnPostulationsList;
    @FXML private Button btnPostulationsCandidats;
    @FXML private Button btnAdminDashboard;

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
        showOffresList();           // default view when app starts - Gérer les Offres
        setActiveButton(btnOffresList);
    }

    @FXML
    public void showOffresTable() {
        loadViewWithFade("offres-table.fxml");   // ← new FXML we'll create
        setActiveButton(btnOffresTable);
    }

    @FXML
    public void showOffresList() {
        loadViewWithFade("offres-list.fxml");
        setActiveButton(btnOffresList);
    }

    // Afficher les favoris (accessible depuis OffresListController, pas de bouton navbar)
    public void showFavorites() {
        loadViewWithFade("favorites-list.fxml");
        // Pas de setActiveButton car pas dans navbar
    }

    @FXML
    public void showOffreAdd() {
        loadViewWithFade("offre-add.fxml");
        setActiveButton(btnOffreAdd);
    }

    /*@FXML
    public void showPostulationsList() {
        loadViewWithFade("postulations-list.fxml");
        setActiveButton(btnPostulationsList);
    }*/

    @FXML
    public void showPostulationsCandidats() {
        loadViewWithFade("postulations-candidats.fxml");
        setActiveButton(btnPostulationsCandidats);
    }

    @FXML
    public void showAdminDashboard() {
        loadViewWithFade("admin-dashboard.fxml");
        setActiveButton(btnAdminDashboard);
    }

    //  ADDED: load postulations-list and pass selected offreId to filter
    public void showPostulationsForOffre(int offreId, String offreTitre) {
        loadViewWithFadeAndInit("/postulations-list.fxml", controller -> {
            if (controller instanceof PostulationsListController plc) {
                plc.setOffreFilter(offreId, offreTitre);
            }
        });
        setActiveButton(btnPostulationsList);
    }



    /**
     * Called from OffresListController when user clicks "Postuler" on a row
     */
    public void showPostuler(int offreId, String offreTitre) {
        loadViewWithFadeAndInit("/postuler.fxml", controller -> {
            if (controller instanceof PostulerPopupController postulerCtrl) {
                postulerCtrl.setOffreInfo(offreId, offreTitre);
            }
        });
    }

    /**
     * Afficher les statistiques d'une offre (interface complète au lieu de popup)
     */
    public void showOffreStats(entities.OffreEmploi offre) {
        loadViewWithFadeAndInit("/offre-stats-popup.fxml", controller -> {
            if (controller instanceof OffreStatsPopupController statsCtrl) {
                statsCtrl.setOffre(offre);
            }
        });
    }

    /**
     * Main loading method - tries to load FXML and shows error in UI if it fails
     */
    private void loadViewWithFade(String fxmlFileName) {
        loadViewWithFadeAndInit("/" + fxmlFileName, null);
    }

    private void loadViewWithFadeAndInit(String resourcePath, java.util.function.Consumer<Object> initializer) {
        try {
            // Debug: print what we're trying to load
            System.out.println("Loading view: " + resourcePath);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));

            if (loader.getLocation() == null) {
                throw new IllegalStateException("Resource not found: " + resourcePath);
            }

            Node view = loader.load();

            // Optional: run initializer (mainly for postuler)
            if (initializer != null) {
                initializer.accept(loader.getController());
            }

            contentPane.getChildren().setAll(view);

            // Fade animation
            FadeTransition ft = new FadeTransition(Duration.millis(250), view);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

        } catch (Exception e) {
            e.printStackTrace(); // still log to console

            // Show error directly in the UI
            Label errorLabel = new Label("Erreur de chargement de la page :\n" + e.getMessage());
            errorLabel.setStyle(
                    "-fx-font-size: 18px; " +
                            "-fx-text-fill: #dc2626; " +
                            "-fx-padding: 40; " +
                            "-fx-alignment: center;"
            );
            errorLabel.setWrapText(true);
            contentPane.getChildren().setAll(errorLabel);
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
