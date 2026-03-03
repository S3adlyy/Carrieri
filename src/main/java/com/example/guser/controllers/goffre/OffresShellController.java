package com.example.guser.controllers.goffre;

import entities.goffre.OffreEmploi;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class OffresShellController {

    private static OffresShellController instance;

    @FXML private StackPane contentPane;
    @FXML private VBox sidebar;
    @FXML private Circle navAvatar;
    @FXML private Label navNameLabel;
    @FXML private Button btnTheme;

    // Boutons de navigation
    @FXML private Button btnOffresList;
    @FXML private Button btnOffresTable;
    @FXML private Button btnOffreAdd;
    @FXML private Button btnPostulationsCandidats;
    @FXML private Button btnAdminDashboard;

    private Button activeButton = null;

    // Animations pour la sidebar
    private Timeline expandAnimation;
    private Timeline collapseAnimation;

    public OffresShellController() {
        instance = this;
    }

    public static OffresShellController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        navNameLabel.setText("Ons Nagara");
        showOffresList();           // vue par défaut
        setActiveButton(btnOffresList);

        // Initialiser le bouton de thème
        setupThemeButton();

        // Configurer les animations de la sidebar
        setupAnimations();

        // Tooltip pour l'avatar (optionnel)
        Tooltip.install(navAvatar, new Tooltip("Admin connecté"));
    }

    // ============================================
    // ANIMATIONS SIDEBAR
    // ============================================
    private void setupAnimations() {
        expandAnimation = new Timeline(
                new KeyFrame(Duration.millis(300),
                        new KeyValue(sidebar.prefWidthProperty(), 250, Interpolator.EASE_BOTH),
                        new KeyValue(sidebar.minWidthProperty(), 250, Interpolator.EASE_BOTH),
                        new KeyValue(sidebar.maxWidthProperty(), 250, Interpolator.EASE_BOTH)
                )
        );

        collapseAnimation = new Timeline(
                new KeyFrame(Duration.millis(300),
                        new KeyValue(sidebar.prefWidthProperty(), 70, Interpolator.EASE_BOTH),
                        new KeyValue(sidebar.minWidthProperty(), 70, Interpolator.EASE_BOTH),
                        new KeyValue(sidebar.maxWidthProperty(), 70, Interpolator.EASE_BOTH)
                )
        );
    }

    @FXML
    public void expandSidebar() {
        expandAnimation.play();
    }

    @FXML
    public void collapseSidebar() {
        collapseAnimation.play();
    }

    // ============================================
    // GESTION DU THÈME (clair/sombre)
    // ============================================
    private void setupThemeButton() {
        updateThemeIcon();
        btnTheme.setOnAction(e -> toggleTheme());
    }

    private void toggleTheme() {
        HelloApplication.toggleTheme(); // utilise la méthode statique de votre classe principale
        updateThemeIcon();
    }

    private void updateThemeIcon() {
        if (HelloApplication.isDarkMode()) {
            btnTheme.setText("☀"); // soleil = mode sombre actif (pour passer au clair)
        } else {
            btnTheme.setText("🌙"); // lune = mode clair actif (pour passer au sombre)
        }
    }

    // ============================================
    // NAVIGATION PRINCIPALE (vues pleine page)
    // ============================================
    @FXML
    public void showOffresTable() {
        loadViewWithFade("com/example/guser/goffre/offres-table.fxml");
        setActiveButton(btnOffresTable);
    }

    @FXML
    public void showOffresList() {
        loadViewWithFade("com/example/guser/goffre/offres-list.fxml");
        setActiveButton(btnOffresList);
    }

    public void showFavorites() {
        loadViewWithFade("com/example/guser/goffre/favorites-list.fxml");
        // Pas de setActiveButton car pas dans navbar
    }

    @FXML
    public void showOffreAdd() {
        loadViewWithFade("com/example/guser/goffre/offre-add.fxml");
        setActiveButton(btnOffreAdd);
    }

    @FXML
    public void showPostulationsCandidats() {
        loadViewWithFade("com/example/guser/goffre/postulations-candidats.fxml");
        setActiveButton(btnPostulationsCandidats);
    }

    @FXML
    public void showAdminDashboard() {
        loadViewWithFade("com/example/guser/goffre/admin-dashboard.fxml");
        setActiveButton(btnAdminDashboard);
    }

    public void showPostulationsForOffre(int offreId, String offreTitre) {
        loadViewWithFadeAndInit("/com/example/guser/goffre/postulations-list.fxml", controller -> {
            if (controller instanceof PostulationsListController plc) {
                plc.setOffreFilter(offreId, offreTitre);
            }
        });
        setActiveButton(btnPostulationsCandidats); // ou un autre bouton ?
    }

    // ============================================
    // NOUVELLES VUES (postuler, stats, qrcode) en pleine page
    // ============================================
    public void showPostuler(int offreId, String offreTitre) {
        loadViewWithFadeAndInit("/com/example/guser/goffre/postuler.fxml", controller -> {
            if (controller instanceof PostulerPopupController) {
                ((PostulerPopupController) controller).setOffreInfo(offreId, offreTitre);
            }
        });
    }

    public void showOffreStats(OffreEmploi offre) {
        loadViewWithFadeAndInit("/com/example/guser/goffre/offre-stats-popup.fxml", controller -> {
            if (controller instanceof OffreStatsPopupController) {
                ((OffreStatsPopupController) controller).setOffre(offre);
            }
        });
    }

    // ============================================
    // MÉTHODE POUR AFFICHER LE QR CODE (MISE À JOUR)
    // ============================================
    public void showQRCode(OffreEmploi offre) {
        loadViewWithFadeAndInit("/com/example/guser/goffre/qrcode.fxml", controller -> {
            if (controller instanceof QRCodeController) {
                ((QRCodeController) controller).initData(offre);
            }
        });
    }

    // ============================================
    // CHARGEMENT DES VUES PRINCIPALES (sans overlay)
    // ============================================
    private void loadViewWithFade(String fxmlFileName) {
        loadViewWithFadeAndInit("/" + fxmlFileName, null);
    }

    private void loadViewWithFadeAndInit(String resourcePath, java.util.function.Consumer<Object> initializer) {
        try {
            System.out.println("Loading view: " + resourcePath);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
            if (loader.getLocation() == null) {
                throw new IllegalStateException("Resource not found: " + resourcePath);
            }
            Node view = loader.load();
            if (initializer != null) {
                initializer.accept(loader.getController());
            }
            contentPane.getChildren().setAll(view);
            FadeTransition ft = new FadeTransition(Duration.millis(250), view);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de la vue " + resourcePath + " : " + e.getMessage());
            e.printStackTrace(System.err);
            Label errorLabel = new Label("Erreur de chargement de la page :\n" + e.getMessage());
            errorLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #dc2626; -fx-padding: 40; -fx-alignment: center;");
            errorLabel.setWrapText(true);
            contentPane.getChildren().setAll(errorLabel);
        }
    }

    private void setActiveButton(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-button-active");
        }
        activeButton = button;
        if (button != null && !button.getStyleClass().contains("nav-button-active")) {
            button.getStyleClass().add("nav-button-active");
        }
    }

    // Déconnexion (simple fermeture)
    @FXML
    public void logout() {
        Platform.exit();
        System.exit(0);
    }
}