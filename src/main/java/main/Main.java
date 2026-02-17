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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class Main extends Application {

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
                // Étape 1: Initialisation
                updateProgress(0.2, "🎓 Chargement de l'application...");
                Thread.sleep(400);

                // Étape 2: Chargement du CSS
                updateProgress(0.4, "🎨 Chargement des styles...");
                Thread.sleep(400);

                // Étape 3: Connexion à la base de données
                updateProgress(0.6, "💾 Connexion à la base de données...");
                Thread.sleep(400);

                // Étape 4: Chargement du shell principal
                updateProgress(0.8, "⚙️ Préparation de l'interface...");
                Parent root = FXMLLoader.load(getClass().getResource("/main-shell.fxml"));

                // Étape 5: Finalisation
                updateProgress(1.0, "🚀 Démarrage...");
                Thread.sleep(300);

                Platform.runLater(() -> {
                    // Fermer le splash
                    closeSplashScreen();

                    // Afficher l'application principale
                    Scene scene = new Scene(root, 1400, 900);
                    primaryStage.setScene(scene);
                    primaryStage.setTitle("Carrieri");

                    // Ajouter l'icône à la fenêtre principale
                    try {
                        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/logo.png")));
                    } catch (Exception e) {
                        System.err.println("Logo non trouvé dans /images/logo.png");
                    }

                    primaryStage.setMaximized(true);
                    primaryStage.show();

                    // Animation de fondu
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(400), root);
                    fadeIn.setFromValue(0);
                    fadeIn.setToValue(1);
                    fadeIn.play();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    closeSplashScreen();
                    primaryStage.show();
                });
            }
        }).start();
    }

    private void createSplashScreen() {
        splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED);

        VBox splashLayout = new VBox(25);
        splashLayout.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #231942, #5E548E);" +
                        "-fx-padding: 40;" +
                        "-fx-alignment: center;"
        );

        // Logo avec effet de glow - VERSION IMAGE
        javafx.scene.image.ImageView logoView = new javafx.scene.image.ImageView();
        try {
            Image logoImage = new Image(getClass().getResourceAsStream("/images/logo.png"));
            logoView.setImage(logoImage);
            logoView.setFitWidth(100);
            logoView.setFitHeight(100);
            logoView.setPreserveRatio(true);
            logoView.setStyle(
                    "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.5), 20, 0, 0, 5);"
            );
        } catch (Exception e) {
            // Fallback si l'image n'existe pas
            Label fallbackLogo = new Label("🎓");
            fallbackLogo.setStyle(
                    "-fx-font-size: 72px;" +
                            "-fx-background-color: white;" +
                            "-fx-background-radius: 50;" +
                            "-fx-padding: 25;" +
                            "-fx-text-fill: #231942;" +
                            "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.5), 20, 0, 0, 5);"
            );
            splashLayout.getChildren().add(fallbackLogo);
        }

        // Titre
        Label titleLabel = new Label("Carrieri");
        titleLabel.setStyle(
                "-fx-font-size: 28px;" +
                        "-fx-font-weight: 900;" +
                        "-fx-text-fill: white;" +
                        "-fx-letter-spacing: -0.5px;"
        );

        // Sous-titre
        Label subtitleLabel = new Label("Gestion des Etudes");
        subtitleLabel.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-text-fill: rgba(255,255,255,0.7);"
        );

        // Barre de progression
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(350);
        progressBar.setPrefHeight(8);
        progressBar.setStyle(
                "-fx-accent: #E0B1CB;" +
                        "-fx-control-inner-background: rgba(255,255,255,0.2);"
        );

        // Label de statut
        statusLabel = new Label("Initialisation...");
        statusLabel.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.9);" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: 600;"
        );

        // Ajout des éléments
        splashLayout.getChildren().addAll(
                logoView,
                titleLabel,
                subtitleLabel,
                progressBar,
                statusLabel
        );

        // Création de la scène
        Scene splashScene = new Scene(splashLayout, 550, 450);
        splashStage.setScene(splashScene);
        splashStage.centerOnScreen();

        // Ajouter l'icône au splash screen
        try {
            splashStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/logo.png")));
        } catch (Exception e) {
            System.err.println("Logo non trouvé pour l'icône du splash");
        }

        splashStage.show();
    }

    private void updateProgress(double value, String status) {
        Platform.runLater(() -> {
            progressBar.setProgress(value);
            statusLabel.setText(status);

            if (value >= 1.0) {
                statusLabel.setStyle(
                        "-fx-text-fill: #10b981;" +
                                "-fx-font-size: 13px;" +
                                "-fx-font-weight: 600;"
                );
            }
        });
    }

    private void closeSplashScreen() {
        if (splashStage != null) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), splashStage.getScene().getRoot());
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
        launch(args);
    }
}