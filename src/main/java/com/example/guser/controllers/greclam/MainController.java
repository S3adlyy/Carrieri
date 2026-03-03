package com.example.guser.controllers.greclam;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private StackPane contentPane;
    @FXML private VBox sidebar;

    @FXML private Button btnReclamations;
    @FXML private Button btnFeedbacks;
    @FXML private Button btnTraitements;
    @FXML private Button btnAjoutReclamation;
    @FXML private Button btnAjoutFeedback;
    // @FXML private Button btnAjoutTraitement;  ← SUPPRIMÉ

    @FXML private Button btnTheme;

    @FXML private Label reclamationsText;
    @FXML private Label feedbacksText;
    @FXML private Label traitementsText;
    @FXML private Label ajoutReclamationText;
    @FXML private Label ajoutFeedbackText;
    // @FXML private Label ajoutTraitementText;  ← SUPPRIMÉ
    @FXML private Label brandText;
    @FXML private Label brandSubtext;
    @FXML private Label userName;
    @FXML private Label userRole;
    @FXML private Circle userAvatar;
    @FXML private Button btnDashboard; // Ajoutez dans le FXML

    @FXML
    public void showDashboard() {
        loadView("/com/example/guser/greclam/dashboard.fxml");
        setActiveButton(btnDashboard);
    }

    private Button activeButton = null;
    private static MainController instance;

    private Timeline expandAnimation;
    private Timeline collapseAnimation;

    private boolean isDarkMode = false;
    private static final String CSS_LIGHT = "/com/example/guser/greclam/css/ThemeUnified.css";
    private static final String CSS_DARK = "/css/ThemeDark.css";

    public MainController() {
        instance = this;
    }

    public static MainController getInstance() {
        return instance;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        userName.setText("Admin User");
        userRole.setText("Administrateur");

        showReclamationList();
        setActiveButton(btnReclamations);

        Tooltip tooltip = new Tooltip("Cliquer pour basculer en mode Candidat");
        Tooltip.install(userAvatar, tooltip);

        setupThemeButton();
        setupAnimations();
    }

    private void setupThemeButton() {
        updateThemeIcon();
        btnTheme.setOnAction(e -> toggleTheme());
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        applyTheme();
        updateThemeIcon();
    }

    private void applyTheme() {
        Scene scene = sidebar.getScene();
        if (scene != null) {
            if (isDarkMode) {
                try {
                    String darkCss = getClass().getResource(CSS_DARK).toExternalForm();
                    if (!scene.getStylesheets().contains(darkCss)) {
                        scene.getStylesheets().add(darkCss);
                    }
                    scene.getRoot().getStyleClass().add("dark");
                } catch (Exception e) {
                    System.err.println("⚠️ CSS sombre non trouvé");
                }
            } else {
                try {
                    String darkCss = getClass().getResource(CSS_DARK).toExternalForm();
                    scene.getStylesheets().remove(darkCss);
                    scene.getRoot().getStyleClass().remove("dark");
                } catch (Exception e) {
                    // Ignorer
                }
            }
        }
    }

    private void updateThemeIcon() {
        if (isDarkMode) {
            btnTheme.setText("☀");
        } else {
            btnTheme.setText("🌙");
        }
    }

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
    // NAVIGATION
    // ============================================

    @FXML
    public void showReclamationList() {
        loadView("/com/example/guser/greclam/reclamationList.fxml");
        setActiveButton(btnReclamations);
    }



    @FXML
    public void showReclamationForm() {
        loadView("/com/example/guser/greclam/reclamationForm.fxml");
        setActiveButton(btnAjoutReclamation);
    }
    @FXML
    public void showFeedbackList() {
        loadView("/com/example/guser/greclam/feedbackList.fxml");
        setActiveButton(btnFeedbacks);
    }

    @FXML
    public void showFeedbackForm() {
        // CORRECTION: Charger le formulaire, pas la liste
        loadView("/com/example/guser/greclam/feedbackForm.fxml");
        setActiveButton(btnAjoutFeedback);
    }


    @FXML
    public void showTraitementList() {
        loadView("/com/example/guser/greclam/traitementList.fxml");
        setActiveButton(btnTraitements);
    }

    // ✅ MÉTHODE showTraitementForm SUPPRIMÉE (car plus de bouton)

    private void loadView(String fxmlFile) {
        try {
            // Debug: afficher le chemin demandé
            System.out.println("Tentative de chargement: " + fxmlFile);

            // Essayer différents chemins si le premier échoue
            URL resource = getClass().getResource(fxmlFile);

            if (resource == null && fxmlFile.startsWith("/")) {
                // Essayer sans le slash
                String alternative = fxmlFile.substring(1);
                System.out.println("Essai alternatif: " + alternative);
                resource = getClass().getResource(alternative);
            }

            if (resource == null) {
                // Essayer avec ClassLoader
                String path = fxmlFile.startsWith("/") ? fxmlFile.substring(1) : fxmlFile;
                resource = getClass().getClassLoader().getResource(path);
                System.out.println("Essai ClassLoader: " + path + " -> " + resource);
            }

            if (resource == null) {
                // Lister les ressources disponibles
                System.err.println("❌ FXML non trouvé: " + fxmlFile);
                listAvailableResources();
                showAlert("Erreur", "Fichier introuvable: " + fxmlFile);
                return;
            }

            System.out.println("✅ FXML trouvé: " + resource);
            FXMLLoader loader = new FXMLLoader(resource);
            Node view = loader.load();
            animateContentChange(view);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la page: " + fxmlFile + "\n" + e.getMessage());
        }
    }

    private void listAvailableResources() {
        System.out.println("=== Ressources disponibles ===");
        try {
            var resources = getClass().getClassLoader().getResources("com/example/guser/greclam/");
            while (resources.hasMoreElements()) {
                var url = resources.nextElement();
                System.out.println("Dossier: " + url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void animateContentChange(Node newView) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), contentPane);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        fadeOut.setOnFinished(e -> {
            contentPane.getChildren().setAll(newView);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), contentPane);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });

        fadeOut.play();
    }

    private void setActiveButton(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-button-active");
        }

        activeButton = button;
        if (activeButton != null) {
            activeButton.getStyleClass().add("nav-button-active");
        }
    }

    @FXML
    public void logout() {
        if (showConfirmation("Déconnexion", "Êtes-vous sûr de vouloir vous déconnecter ?")) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), sidebar.getScene().getRoot());
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                System.exit(0);
            });
            fadeOut.play();
        }
    }

    private boolean showConfirmation(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }

    public void setContent(Parent content) {
        contentPane.getChildren().setAll(content);
    }
}