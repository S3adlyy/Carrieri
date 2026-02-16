package com.exemple.grecrutement;

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
                // Étape 1: Initialisation
                updateProgress(0.2, "🎓 Chargement de l'application...");
                Thread.sleep(400);

                // Étape 2: Chargement des styles
                updateProgress(0.4, "🎨 Chargement des styles...");
                Thread.sleep(400);

                // Étape 3: Connexion à la base de données
                updateProgress(0.6, "💾 Connexion à la base de données...");
                Thread.sleep(400);

                // Étape 4: Chargement du shell principal
                updateProgress(0.8, "⚙️ Préparation de l'interface...");
                FXMLLoader loader = new FXMLLoader(getClass().getResource("mission-shell.fxml"));
                Parent root = loader.load();

                // Étape 5: Finalisation
                updateProgress(1.0, "🚀 Démarrage...");
                Thread.sleep(300);

                Platform.runLater(() -> {
                    // Fermer le splash
                    closeSplashScreen();

                    // Afficher l'application principale
                    Scene scene = new Scene(root, 1400, 900);

                    // Ajouter les stylesheets
                    scene.getStylesheets().add(getClass().getResource("app.css").toExternalForm());

                    primaryStage.setScene(scene);
                    primaryStage.setTitle("Mission Management Platform");

                    // Ajouter l'icône à la fenêtre principale - MULTIPLE PATHES
                    addIconToStage(primaryStage);

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

                    // En cas d'erreur, essayer de charger quand même
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("mission-shell.fxml"));
                        Parent root = loader.load();
                        Scene scene = new Scene(root);
                        scene.getStylesheets().add(getClass().getResource("app.css").toExternalForm());
                        primaryStage.setScene(scene);
                        primaryStage.setTitle("Mission Management Platform");
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

    /**
     * Méthode utilitaire pour ajouter l'icône à un stage avec plusieurs chemins possibles
     */
    private void addIconToStage(Stage stage) {
        // Liste des chemins possibles pour le logo
        String[] possiblePaths = {
                "/images/logo.png",
                "/logo.png",
                "images/logo.png",
                "logo.png",
                "/com/example/grecrutement/images/logo.png",
                "com/example/grecrutement/images/logo.png"
        };

        boolean iconAdded = false;

        for (String path : possiblePaths) {
            try {
                InputStream is = getClass().getResourceAsStream(path);
                if (is != null) {
                    Image icon = new Image(is);
                    stage.getIcons().add(icon);
                    System.out.println("✅ Icône chargée avec succès depuis: " + path);
                    iconAdded = true;
                    break;
                }
            } catch (Exception e) {
                // Ignorer et essayer le chemin suivant
            }
        }

        if (!iconAdded) {
            System.err.println("❌ Logo non trouvé - Aucune icône ajoutée");
        }
    }

    private void createSplashScreen() {
        splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED);

        VBox splashLayout = new VBox(25);
        splashLayout.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #231942, #5E548E, #9F86C0);" +
                        "-fx-padding: 40;" +
                        "-fx-alignment: center;" +
                        "-fx-effect: dropshadow(gaussian, rgba(248,188,255,0.3), 30, 0.3, 0, 10);"
        );

        // Logo avec effet de glow - Version améliorée avec plusieurs chemins
        StackPane logoContainer = createLogoWithFallback();
        splashLayout.getChildren().add(logoContainer);

        // Titre avec effet de glow
        Label titleLabel = new Label("Carrieri");
        titleLabel.setStyle(
                "-fx-font-size: 36px;" +
                        "-fx-font-weight: 900;" +
                        "-fx-text-fill: white;" +
                        "-fx-letter-spacing: -0.5px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.3), 15, 0.3, 2, 2);"
        );

        // Sous-titre
        Label subtitleLabel = new Label("Mission Management Platform");
        subtitleLabel.setStyle(
                "-fx-font-size: 16px;" +
                        "-fx-text-fill: rgba(255,255,255,0.8);" +
                        "-fx-font-weight: 600;"
        );

        // Barre de progression avec style moderne
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setPrefHeight(10);
        progressBar.setStyle(
                "-fx-accent: #f8bcff;" +
                        "-fx-control-inner-background: rgba(255,255,255,0.2);" +
                        "-fx-background-radius: 5;" +
                        "-fx-effect: dropshadow(gaussian, rgba(248,188,255,0.3), 10, 0, 0, 2);"
        );

        // Label de statut
        statusLabel = new Label("Initialisation...");
        statusLabel.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.9);" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: 600;" +
                        "-fx-font-family: 'Segoe UI', 'Poppins', sans-serif;"
        );

        // Ajout des éléments
        splashLayout.getChildren().addAll(
                titleLabel,
                subtitleLabel,
                progressBar,
                statusLabel
        );

        // Création de la scène
        Scene splashScene = new Scene(splashLayout, 600, 500);
        splashStage.setScene(splashScene);
        splashStage.centerOnScreen();

        // Ajouter l'icône au splash screen
        addIconToStage(splashStage);

        splashStage.show();

        // Animation d'entrée pour le splash
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), splashLayout);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    /**
     * Crée un logo avec fallback si l'image n'est pas trouvée
     */
    private StackPane createLogoWithFallback() {
        StackPane logoContainer = new StackPane();

        // Essayer de charger l'image
        javafx.scene.image.ImageView logoView = new javafx.scene.image.ImageView();

        // Liste des chemins possibles
        String[] possiblePaths = {
                "/images/logo.png",
                "/logo.png",
                "images/logo.png",
                "logo.png",
                "/com/example/grecrutement/images/logo.png"
        };

        Image logoImage = null;
        String usedPath = null;

        for (String path : possiblePaths) {
            try {
                InputStream is = getClass().getResourceAsStream(path);
                if (is != null) {
                    logoImage = new Image(is);
                    usedPath = path;
                    break;
                }
            } catch (Exception e) {
                // Ignorer et continuer
            }
        }

        if (logoImage != null) {
            // Image trouvée
            logoView.setImage(logoImage);
            logoView.setFitWidth(100);
            logoView.setFitHeight(100);
            logoView.setPreserveRatio(true);
            logoView.setStyle(
                    "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.5), 20, 0, 0, 5);" +
                            "-fx-background-radius: 50;"
            );
            logoContainer.getChildren().add(logoView);
            System.out.println("✅ Logo chargé pour splash depuis: " + usedPath);
        } else {
            // Fallback stylé
            System.out.println("⚠️ Utilisation du fallback pour le logo (image non trouvée)");

            // Cercle de fond
            Circle circle = new Circle(50);
            circle.setFill(Color.rgb(248, 188, 255));
            circle.setStroke(Color.WHITE);
            circle.setStrokeWidth(3);

            DropShadow glow = new DropShadow();
            glow.setColor(Color.rgb(248, 188, 255, 0.5));
            glow.setRadius(20);
            circle.setEffect(glow);

            // Emoji au centre
            Label emojiLabel = new Label("🚀");
            emojiLabel.setStyle(
                    "-fx-font-size: 48px;" +
                            "-fx-text-fill: #231942;" +
                            "-fx-font-weight: bold;"
            );

            logoContainer.getChildren().addAll(circle, emojiLabel);
        }

        return logoContainer;
    }

    private void updateProgress(double value, String status) {
        Platform.runLater(() -> {
            progressBar.setProgress(value);
            statusLabel.setText(status);

            if (value >= 1.0) {
                statusLabel.setStyle(
                        "-fx-text-fill: #f8bcff;" +
                                "-fx-font-size: 14px;" +
                                "-fx-font-weight: 700;" +
                                "-fx-effect: dropshadow(gaussian, rgba(248,188,255,0.3), 5, 0, 0, 0);"
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
        launch(args);
    }
}