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

    // Constantes pour les chemins
    private static final String CSS_PATH = "/css/theme-unified.css";
    private static final String SHELL_FXML_PATH = "/com/exemple/grecrutement/mission-shell.fxml";
    private static final String[] LOGO_PATHS = {
            "/images/logo.png",
            "/logo.png",
            "images/logo.png",
            "logo.png",
            "/com/example/grecrutement/images/logo.png",
            "/com/exemple/grecrutement/images/logo.png"
    };

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
                Thread.sleep(300);

                // Étape 2: Chargement des styles
                updateProgress(0.4, "🎨 Chargement des styles...");
                Thread.sleep(300);

                // Étape 3: Connexion à la base de données
                updateProgress(0.6, "💾 Connexion à la base de données...");
                Thread.sleep(300);

                // Étape 4: Chargement du shell principal (MissionShell)
                updateProgress(0.8, "⚙️ Préparation de l'interface...");

                // Charger le FXML avec le bon chemin
                FXMLLoader loader = new FXMLLoader(getClass().getResource(SHELL_FXML_PATH));

                // IMPORTANT: Si le chemin ne fonctionne pas, essayez sans le dossier com/exemple/grecrutement
                if (getClass().getResource(SHELL_FXML_PATH) == null) {
                    System.err.println("⚠️ Chemin FXML non trouvé: " + SHELL_FXML_PATH);
                    System.err.println("📁 Recherche dans le classpath...");

                    // Essayer des chemins alternatifs
                    String[] alternatePaths = {
                            "/mission-shell.fxml",
                            "mission-shell.fxml",
                            "/com/example/grecrutement/mission-shell.fxml"
                    };

                    for (String path : alternatePaths) {
                        if (getClass().getResource(path) != null) {
                            System.out.println("✅ FXML trouvé: " + path);
                            loader = new FXMLLoader(getClass().getResource(path));
                            break;
                        }
                    }
                }

                Parent root = loader.load();

                // Étape 5: Finalisation
                updateProgress(1.0, "🚀 Démarrage...");
                Thread.sleep(300);

                Platform.runLater(() -> {
                    // Fermer le splash
                    closeSplashScreen();

                    // Afficher l'application principale
                    Scene scene = new Scene(root, 1400, 900);

                    // Ajouter les stylesheets - UTILISER LE CSS UNIFIED
                    String cssPath = CSS_PATH;
                    if (getClass().getResource(cssPath) != null) {
                        scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
                        System.out.println("✅ CSS chargé: " + cssPath);
                    } else {
                        System.err.println("⚠️ CSS non trouvé: " + cssPath);
                        // Essayer des chemins alternatifs pour le CSS
                        String[] altCssPaths = {
                                "/theme-unified.css",
                                "/css/theme-unified.css",
                                "theme-unified.css"
                        };
                        for (String path : altCssPaths) {
                            if (getClass().getResource(path) != null) {
                                scene.getStylesheets().add(getClass().getResource(path).toExternalForm());
                                System.out.println("✅ CSS alternatif chargé: " + path);
                                break;
                            }
                        }
                    }

                    primaryStage.setScene(scene);
                    primaryStage.setTitle("Carrieri - Gestion de Recrutement");

                    // Ajouter l'icône à la fenêtre principale
                    addIconToStage(primaryStage);

                    primaryStage.setMaximized(true);
                    primaryStage.show();

                    // Animation de fondu
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(400), root);
                    fadeIn.setFromValue(0);
                    fadeIn.setToValue(1);
                    fadeIn.play();

                    System.out.println("✅ Application démarrée avec succès !");
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    closeSplashScreen();
                    showErrorAndFallback(e);
                });
            }
        }).start();
    }

    /**
     * Affiche une erreur et tente un chargement de secours
     */
    private void showErrorAndFallback(Exception e) {
        try {
            System.err.println("❌ Erreur lors du chargement: " + e.getMessage());

            // Créer une scène d'erreur simple
            StackPane errorRoot = new StackPane();
            errorRoot.setStyle("-fx-background-color: linear-gradient(to bottom right, #231942, #5E548E);");

            VBox errorBox = new VBox(20);
            errorBox.setStyle("-fx-alignment: center; -fx-padding: 40;");

            Label errorTitle = new Label("⚠️ Erreur de chargement");
            errorTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

            Label errorMsg = new Label("Impossible de charger l'interface principale.\n" + e.getMessage());
            errorMsg.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 14px;");

            Label fallbackMsg = new Label("Chargement de l'interface simplifiée...");
            fallbackMsg.setStyle("-fx-text-fill: #f8bcff; -fx-font-size: 12px; -fx-font-style: italic;");

            errorBox.getChildren().addAll(errorTitle, errorMsg, fallbackMsg);
            errorRoot.getChildren().add(errorBox);

            Scene fallbackScene = new Scene(errorRoot, 800, 600);
            primaryStage.setScene(fallbackScene);
            primaryStage.setTitle("Carrieri - Mode dégradé");
            addIconToStage(primaryStage);
            primaryStage.setMaximized(true);
            primaryStage.show();

            // Essayer de charger le shell après 2 secondes
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    Platform.runLater(() -> {
                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource(SHELL_FXML_PATH));
                            Parent root = loader.load();
                            Scene scene = new Scene(root, 1400, 900);
                            scene.getStylesheets().add(getClass().getResource(CSS_PATH).toExternalForm());
                            primaryStage.setScene(scene);
                        } catch (Exception ex) {
                            System.err.println("❌ Second essai échoué: " + ex.getMessage());
                        }
                    });
                } catch (InterruptedException ignored) {}
            }).start();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Méthode utilitaire pour ajouter l'icône à un stage avec plusieurs chemins possibles
     */
    private void addIconToStage(Stage stage) {
        boolean iconAdded = false;

        for (String path : LOGO_PATHS) {
            try {
                InputStream is = getClass().getResourceAsStream(path);
                if (is != null) {
                    Image icon = new Image(is);
                    stage.getIcons().add(icon);
                    System.out.println("✅ Icône chargée depuis: " + path);
                    iconAdded = true;
                    break;
                }
            } catch (Exception e) {
                // Ignorer et essayer le chemin suivant
            }
        }

        if (!iconAdded) {
            System.err.println("ℹ️ Logo non trouvé - Utilisation de l'icône par défaut");
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

        // Logo avec effet de glow
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

        // Barre de progression
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
                        "-fx-font-weight: 600;"
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

        javafx.scene.image.ImageView logoView = new javafx.scene.image.ImageView();

        Image logoImage = null;
        String usedPath = null;

        for (String path : LOGO_PATHS) {
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
            logoView.setImage(logoImage);
            logoView.setFitWidth(100);
            logoView.setFitHeight(100);
            logoView.setPreserveRatio(true);
            logoView.setStyle(
                    "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.5), 20, 0, 0, 5);"
            );
            logoContainer.getChildren().add(logoView);
            System.out.println("✅ Logo splash chargé depuis: " + usedPath);
        } else {
            System.out.println("ℹ️ Utilisation du fallback pour le logo");

            Circle circle = new Circle(50);
            circle.setFill(Color.rgb(248, 188, 255));
            circle.setStroke(Color.WHITE);
            circle.setStrokeWidth(3);

            DropShadow glow = new DropShadow();
            glow.setColor(Color.rgb(248, 188, 255, 0.5));
            glow.setRadius(20);
            circle.setEffect(glow);

            Label emojiLabel = new Label("🚀");
            emojiLabel.setStyle(
                    "-fx-font-size: 48px;" +
                            "-fx-text-fill: #231942;"
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
        launch(args);
    }
}