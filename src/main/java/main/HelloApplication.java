package main;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.prefs.Preferences;

public class HelloApplication extends Application {

    private Stage primaryStage;
    private Stage splashStage;
    private ProgressBar progressBar;
    private Label statusLabel;

    // ===== Theme prefs =====
    private static final String PREFS_DARK_MODE = "darkMode";
    private static Preferences prefs;

    // Chemins CSS (comme ton fichier Main.java)
    private static final String CSS_LIGHT = "/css/theme-unified.css";
    private static final String CSS_DARK  = "/css/theme-dark.css";

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        prefs = Preferences.userNodeForPackage(HelloApplication.class);

        // Splash screen
        createSplashScreen();

        // Charger l'app en arrière-plan
        new Thread(this::loadApplication).start();
    }

    private void loadApplication() {
        try {
            updateProgress(0.2, "🎓 Chargement de l'application...");
            Thread.sleep(400);

            updateProgress(0.4, "🎨 Chargement des styles...");
            Thread.sleep(400);

            updateProgress(0.6, "💾 Connexion à la base de données...");
            Thread.sleep(400);

            updateProgress(0.8, "⚙️ Préparation de l'interface...");
            Parent root = FXMLLoader.load(getClass().getResource("/offres-shell.fxml"));

            updateProgress(1.0, "🚀 Démarrage...");
            Thread.sleep(300);

            Platform.runLater(() -> {
                closeSplashScreen();

                Scene scene = new Scene(root, 1400, 900);

                // ✅ CSS clair (toujours)
                try {
                    scene.getStylesheets().add(getClass().getResource(CSS_LIGHT).toExternalForm());
                } catch (Exception e) {
                    System.err.println("⚠️ CSS clair non trouvé: " + CSS_LIGHT);
                }

                // ✅ CSS sombre si activé
                if (isDarkMode()) {
                    try {
                        scene.getStylesheets().add(getClass().getResource(CSS_DARK).toExternalForm());
                    } catch (Exception e) {
                        System.err.println("⚠️ CSS sombre non trouvé: " + CSS_DARK);
                    }
                    scene.getRoot().getStyleClass().add("dark");
                }

                primaryStage.setTitle("Carrieri - Gestion des Offres d'Emploi");
                primaryStage.setScene(scene);

                addIconToStage(primaryStage);

                primaryStage.setMaximized(true);
                primaryStage.show();

                FadeTransition fadeIn = new FadeTransition(Duration.millis(400), root);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            });

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                closeSplashScreen();
                // fallback minimal
                primaryStage.setTitle("Carrieri - Gestion des Offres d'Emploi");
                primaryStage.show();
            });
        }
    }

    // ============================================
    // SPLASH SCREEN (style du fichier Main.java)
    // ============================================
    private void createSplashScreen() {
        splashStage = new Stage();
        splashStage.initStyle(StageStyle.TRANSPARENT);

        Rectangle background = new Rectangle(600, 400);
        background.setArcWidth(30);
        background.setArcHeight(30);
        background.setFill(Color.web("#231942"));

        DropShadow dropShadow = new DropShadow();
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.3));
        dropShadow.setRadius(20);
        dropShadow.setOffsetY(5);
        background.setEffect(dropShadow);

        Node logoNode;
        try {
            Image logoImage = new Image(getClass().getResourceAsStream("/images/logo.png"));
            ImageView logoView = new ImageView(logoImage);
            logoView.setFitWidth(120);
            logoView.setFitHeight(120);
            logoView.setPreserveRatio(true);

            Glow glow = new Glow();
            glow.setLevel(0.3);
            logoView.setEffect(glow);

            logoNode = logoView;
        } catch (Exception e) {
            Label fallbackLogo = new Label("💼");
            fallbackLogo.setStyle(
                    "-fx-font-size: 80px;" +
                            "-fx-text-fill: white;"
            );
            logoNode = fallbackLogo;
        }

        Label titleLabel = new Label("Carrieri");
        titleLabel.setStyle(
                "-fx-font-size: 36px;" +
                        "-fx-font-weight: 900;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-family: 'Segoe UI', 'System';" +
                        "-fx-letter-spacing: 1px;"
        );

        Label subtitleLabel = new Label("Gestion des Offres d'Emploi");
        subtitleLabel.setStyle(
                "-fx-font-size: 16px;" +
                        "-fx-text-fill: rgba(255,255,255,0.8);" +
                        "-fx-font-weight: 500;" +
                        "-fx-letter-spacing: 0.5px;"
        );

        Rectangle separator = new Rectangle(220, 2);
        separator.setFill(Color.web("#E0B1CB"));
        separator.setOpacity(0.5);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setPrefHeight(10);
        progressBar.setStyle(
                "-fx-accent: #E0B1CB;" +
                        "-fx-control-inner-background: rgba(255,255,255,0.15);" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-radius: 20;"
        );

        statusLabel = new Label("Initialisation...");
        statusLabel.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.9);" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: 600;"
        );

        Label versionLabel = new Label("Version 1.0.0");
        versionLabel.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.5);" +
                        "-fx-font-size: 11px;"
        );

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(520);
        content.getChildren().addAll(
                logoNode,
                titleLabel,
                subtitleLabel,
                separator,
                progressBar,
                statusLabel,
                versionLabel
        );

        StackPane splashLayout = new StackPane(background, content);
        StackPane.setAlignment(content, Pos.CENTER);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(350), splashLayout);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(350), splashLayout);
        scaleIn.setFromX(0.85);
        scaleIn.setFromY(0.85);
        scaleIn.setToX(1);
        scaleIn.setToY(1);

        SequentialTransition entrance = new SequentialTransition(fadeIn, scaleIn);

        Scene splashScene = new Scene(splashLayout, 600, 400);
        splashScene.setFill(Color.TRANSPARENT);
        splashStage.setScene(splashScene);
        splashStage.centerOnScreen();

        addIconToStage(splashStage);

        splashStage.show();
        entrance.play();
    }

    // ============================================
    // ICON
    // ============================================
    private void addIconToStage(Stage stage) {
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/logo.png")));
        } catch (Exception e) {
            System.err.println("⚠️ Logo non trouvé pour l'icône: /images/logo.png");
        }
    }

    // ============================================
    // THEME (optionnel, prêt pour ton toggle)
    // ============================================
    public static boolean isDarkMode() {
        return prefs != null && prefs.getBoolean(PREFS_DARK_MODE, false);
    }

    public static void setDarkMode(boolean darkMode) {
        if (prefs != null) prefs.putBoolean(PREFS_DARK_MODE, darkMode);
    }

    public static void toggleTheme() {
        boolean newMode = !isDarkMode();
        setDarkMode(newMode);
        applyThemeToAllStages();
    }

    private static void applyThemeToAllStages() {
        Platform.runLater(() -> {
            for (Stage stage : Stage.getWindows().stream()
                    .filter(w -> w instanceof Stage)
                    .map(w -> (Stage) w)
                    .toList()) {

                Scene scene = stage.getScene();
                if (scene == null || scene.getRoot() == null) continue;

                if (isDarkMode()) {
                    try {
                        String darkCssPath = HelloApplication.class.getResource(CSS_DARK).toExternalForm();
                        if (!scene.getStylesheets().contains(darkCssPath)) {
                            scene.getStylesheets().add(darkCssPath);
                        }
                    } catch (Exception e) {
                        System.err.println("⚠️ CSS sombre non trouvé: " + CSS_DARK);
                    }
                    if (!scene.getRoot().getStyleClass().contains("dark")) {
                        scene.getRoot().getStyleClass().add("dark");
                    }
                } else {
                    try {
                        String darkCssPath = HelloApplication.class.getResource(CSS_DARK).toExternalForm();
                        scene.getStylesheets().remove(darkCssPath);
                    } catch (Exception ignored) {}
                    scene.getRoot().getStyleClass().remove("dark");
                }
            }
        });
    }

    // ============================================
    // SPLASH HELPERS
    // ============================================
    private void updateProgress(double value, String status) {
        Platform.runLater(() -> {
            if (progressBar != null) progressBar.setProgress(value);
            if (statusLabel != null) statusLabel.setText(status);
        });
    }

    private void closeSplashScreen() {
        if (splashStage != null) {
            splashStage.close();
            splashStage = null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}