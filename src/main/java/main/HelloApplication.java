package main;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.InputStream;

public class HelloApplication extends Application {

    private Stage primaryStage;
    private Stage splashStage;
    private ProgressBar progressBar;
    private Label statusLabel;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;

        // Afficher le splash screen
        createSplashScreen();

        // Charger l'application en arrière-plan
        new Thread(() -> {
            try {
                updateProgress(0.2, "🎓 Chargement de l'application...");
                Thread.sleep(400);

                updateProgress(0.4, "🎨 Chargement des styles...");
                Thread.sleep(400);

                updateProgress(0.6, "💾 Connexion à la base de données...");
                Thread.sleep(400);

                updateProgress(0.8, "⚙️ Préparation de l'interface...");
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/offres-shell.fxml"));
                Parent root = loader.load();

                updateProgress(1.0, "🚀 Démarrage...");
                Thread.sleep(300);

                Platform.runLater(() -> {
                    closeSplashScreen();

                    Scene scene = new Scene(root, 1280, 900);
                    primaryStage.setTitle("Gestion des Offres d'Emploi");
                    primaryStage.setScene(scene);
                    addIconToStage(primaryStage);
                    primaryStage.setMaximized(true);
                    primaryStage.show();

                    // Animation de fondu à l'ouverture
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(400), root);
                    fadeIn.setFromValue(0);
                    fadeIn.setToValue(1);
                    fadeIn.play();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    closeSplashScreen();
                    // Fallback: charger sans splash en cas d'erreur
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/offres-shell.fxml"));
                        Parent root = loader.load();
                        Scene scene = new Scene(root, 1280, 900);
                        primaryStage.setTitle("Gestion des Offres d'Emploi");
                        primaryStage.setScene(scene);
                        addIconToStage(primaryStage);
                        primaryStage.setMaximized(true);
                        primaryStage.show();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        }).start();
    }

    private void createSplashScreen() {
        splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED);

        VBox splashLayout = new VBox(25);
        splashLayout.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #231942, #5E548E, #9F86C0);" +
                        "-fx-padding: 40;" +
                        "-fx-alignment: center;"
        );

        // Logo
        StackPane logoContainer = createLogoWithFallback();

        // Titre
        Label titleLabel = new Label("Carrieri");
        titleLabel.setStyle(
                "-fx-font-size: 36px;" +
                        "-fx-font-weight: 900;" +
                        "-fx-text-fill: white;" +
                        "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.3), 15, 0.3, 2, 2);"
        );

        // Sous-titre
        Label subtitleLabel = new Label("Gestion des Offres d'Emploi");
        subtitleLabel.setStyle(
                "-fx-font-size: 16px;" +
                        "-fx-text-fill: rgba(255,255,255,0.8);" +
                        "-fx-font-weight: 600;"
        );

        // Barre de progression
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setPrefHeight(10);
        progressBar.setStyle(
                "-fx-accent: #f8bcff;" +
                        "-fx-control-inner-background: rgba(255,255,255,0.2);" +
                        "-fx-background-radius: 5;"
        );

        // Label de statut
        statusLabel = new Label("Initialisation...");
        statusLabel.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.9);" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: 600;"
        );

        splashLayout.getChildren().addAll(
                logoContainer,
                titleLabel,
                subtitleLabel,
                progressBar,
                statusLabel
        );

        Scene splashScene = new Scene(splashLayout, 600, 400);
        splashStage.setScene(splashScene);
        splashStage.centerOnScreen();
        addIconToStage(splashStage);
        splashStage.show();

        // Animation d'entrée
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), splashLayout);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private StackPane createLogoWithFallback() {
        StackPane logoContainer = new StackPane();

        String[] possiblePaths = {
                "/images/logo.png",
                "/logo.png",
                "images/logo.png",
                "logo.png"
        };

        Image logoImage = null;
        for (String path : possiblePaths) {
            try {
                InputStream is = getClass().getResourceAsStream(path);
                if (is != null) {
                    logoImage = new Image(is);
                    break;
                }
            } catch (Exception e) {
                // essayer le chemin suivant
            }
        }

        if (logoImage != null) {
            javafx.scene.image.ImageView logoView = new javafx.scene.image.ImageView(logoImage);
            logoView.setFitWidth(100);
            logoView.setFitHeight(100);
            logoView.setPreserveRatio(true);
            logoContainer.getChildren().add(logoView);
        } else {
            // Fallback: cercle avec emoji
            Circle circle = new Circle(50);
            circle.setFill(Color.rgb(248, 188, 255));
            circle.setStroke(Color.WHITE);
            circle.setStrokeWidth(3);

            DropShadow glow = new DropShadow();
            glow.setColor(Color.rgb(248, 188, 255, 0.5));
            glow.setRadius(20);
            circle.setEffect(glow);

            Label emojiLabel = new Label("💼");
            emojiLabel.setStyle("-fx-font-size: 48px;");

            logoContainer.getChildren().addAll(circle, emojiLabel);
        }

        return logoContainer;
    }

    private void addIconToStage(Stage stage) {
        String[] possiblePaths = {
                "/images/logo.png",
                "/logo.png",
                "images/logo.png",
                "logo.png"
        };

        for (String path : possiblePaths) {
            try {
                InputStream is = getClass().getResourceAsStream(path);
                if (is != null) {
                    stage.getIcons().add(new Image(is));
                    break;
                }
            } catch (Exception e) {
                // essayer le chemin suivant
            }
        }
    }

    private void updateProgress(double value, String status) {
        Platform.runLater(() -> {
            progressBar.setProgress(value);
            statusLabel.setText(status);
            if (value >= 1.0) {
                statusLabel.setStyle(
                        "-fx-text-fill: #f8bcff;" +
                                "-fx-font-size: 14px;" +
                                "-fx-font-weight: 700;"
                );
            }
        });
    }

    private void closeSplashScreen() {
        if (splashStage != null) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), splashStage.getScene().getRoot());
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                splashStage.close();
                splashStage = null;
            });
            fadeOut.play();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}