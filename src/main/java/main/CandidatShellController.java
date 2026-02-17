package main;

import entities.Cours;
import javafx.animation.*;
import javafx.application.Platform;
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
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class CandidatShellController implements Initializable {

    @FXML private StackPane contentPane;
    @FXML private VBox sidebar;

    @FXML private Button btnCatalogue;
    @FXML private Button btnMesCours;
    @FXML private Button btnCertificats;
    @FXML private Button btnProfil;

    @FXML private Label catalogueText;
    @FXML private Label mesCoursText;
    @FXML private Label certificatsText;
    @FXML private Label profilText;
    @FXML private Label brandText;
    @FXML private Label brandSubtext;
    @FXML private Label userName;
    @FXML private Label userRole;
    @FXML private Circle userAvatar;

    private Button activeButton = null;
    private static CandidatShellController instance;

    // Variables pour suivre le contexte
    private Cours coursActif = null;
    private int candidatId = 1; // À remplacer par l'ID connecté

    private Timeline expandAnimation;
    private Timeline collapseAnimation;

    public CandidatShellController() {
        instance = this;
    }

    public static CandidatShellController getInstance() {
        return instance;
    }
    @FXML
    public void switchToAdmin() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Changement de mode");
        confirm.setHeaderText("Passer en mode Administrateur");
        confirm.setContentText("Voulez-vous basculer vers l'espace Admin ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Stage stage = (Stage) sidebar.getScene().getWindow();

                // ✅ Sauvegarder TOUS les paramètres
                boolean etaitMaximized = stage.isMaximized();
                boolean etaitFullScreen = stage.isFullScreen();
                double width = stage.getWidth();
                double height = stage.getHeight();
                double x = stage.getX();
                double y = stage.getY();

                System.out.println("📊 Sauvegarde - Maximized: " + etaitMaximized +
                        ", Width: " + width + ", Height: " + height);

                Parent currentRoot = stage.getScene().getRoot();
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentRoot);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);

                fadeOut.setOnFinished(e -> {
                    try {
                        Parent newRoot = FXMLLoader.load(getClass().getResource("/main-shell.fxml"));
                        newRoot.setOpacity(0);

                        Scene scene = new Scene(newRoot, width, height);
                        stage.setScene(scene);
                        stage.setTitle("E-Learning - Administration");

                        stage.setX(x);
                        stage.setY(y);

                        if (etaitMaximized) {
                            Platform.runLater(() -> stage.setMaximized(true));
                        }

                        if (etaitFullScreen) {
                            Platform.runLater(() -> stage.setFullScreen(true));
                        }

                        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newRoot);
                        fadeIn.setFromValue(0);
                        fadeIn.setToValue(1);
                        fadeIn.play();

                    } catch (IOException ex) {
                        ex.printStackTrace();
                        showAlert("Erreur de chargement", "Impossible de charger l'espace admin");
                    }
                });

                fadeOut.play();

            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Erreur", "Impossible de basculer vers le mode Admin");
            }
        }
    }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        userName.setText("Bilal El Eter");
        userRole.setText("Candidat");
        // ✅ Installer le tooltip sur l'avatar
        Tooltip candidatTooltip = new Tooltip("Cliquer pour basculer en mode Admin");
        Tooltip.install(userAvatar, candidatTooltip);
        showCatalogue();
        setActiveButton(btnCatalogue);

        setupAnimations();

        // Animation de fondu initiale
        contentPane.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), contentPane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
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

    @FXML
    public void showCatalogue() {
        loadView("/cours_candidat.fxml");
        setActiveButton(btnCatalogue);
    }

    @FXML
    public void showMesCours() {
        // À implémenter si besoin
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText("Fonctionnalité à venir : Mes cours en progression");
        alert.showAndWait();
    }

    @FXML
    public void showCertificats() {
        // À implémenter si besoin
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText("Fonctionnalité à venir : Mes certificats");
        alert.showAndWait();
    }

    @FXML
    public void showProfil() {
        // À implémenter si besoin
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText("Fonctionnalité à venir : Profil candidat");
        alert.showAndWait();
    }

    // ✅ Méthode pour ouvrir un cours
    public void openCours(Cours cours) {
        this.coursActif = cours;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CoursPlayer.fxml"));
            Node view = loader.load();

            CoursPlayerController controller = loader.getController();
            controller.setCours(cours);
            controller.setCandidatId(candidatId);

            animateContentChange(view);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le cours: " + e.getMessage());
        }
    }

    // ✅ Méthode pour ouvrir un quiz
    public void openQuiz(int moduleId, String moduleTitre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/QuizModulePlayer.fxml"));
            Node view = loader.load();

            QuizModulePlayerController controller = loader.getController();
            controller.setModuleId(moduleId, candidatId);

            animateContentChange(view);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le quiz: " + e.getMessage());
        }
    }

    // ✅ Méthode pour ouvrir le test final
    public void openTestFinal(int coursId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TestFinalPlayer.fxml"));
            Node view = loader.load();

            TestFinalPlayerController controller = loader.getController();
            controller.setCoursId(coursId, candidatId);

            animateContentChange(view);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le test final: " + e.getMessage());
        }
    }

    // ✅ Retour au cours après quiz/test
    public void retourAuCours() {
        if (coursActif != null) {
            openCours(coursActif);
        } else {
            showCatalogue();
        }
    }

    @FXML
    public void logout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Déconnexion");
        alert.setHeaderText(null);
        alert.setContentText("Êtes-vous sûr de vouloir vous déconnecter ?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(500), sidebar.getScene().getRoot());
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> {
                    javafx.application.Platform.exit();
                    System.exit(0);
                });
                fadeOut.play();
            }
        });
    }

    private void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Node view = loader.load();
            animateContentChange(view);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la vue: " + fxmlFile);
        }
    }

    private void animateContentChange(Node newView) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), contentPane);
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}