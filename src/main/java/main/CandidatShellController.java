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
import utils.AlertUtils;

import java.io.IOException;
import java.net.URL;
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
    @FXML private Button btnTheme;
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

    // ============================================
    // BASCULE VERS ADMIN - MODIFIÉ
    // ============================================
    @FXML
    public void switchToAdmin() {
        boolean confirmed = AlertUtils.showConfirmation(
                "🔄 Changement de mode",
                "Voulez-vous basculer vers l'espace Administrateur ?\n\n" +
                        "Vous pourrez gérer les cours, modules et leçons.",
                "Oui, basculer",
                "Non, rester"
        );

        if (confirmed) {
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

                        AlertUtils.showSuccess("✅ Bascule réussie", "Vous êtes maintenant dans l'espace Administrateur.");

                    } catch (IOException ex) {
                        ex.printStackTrace();
                        AlertUtils.showError("❌ Erreur de chargement",
                                "Impossible de charger l'espace administrateur.\n\n" + ex.getMessage());
                    }
                });

                fadeOut.play();

            } catch (Exception e) {
                e.printStackTrace();
                AlertUtils.showError("❌ Erreur", "Impossible de basculer vers le mode Admin");
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
        setupThemeButton();
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
    // ✅ AJOUTER CES MÉTHODES
    private void setupThemeButton() {
        updateThemeIcon();
        btnTheme.setOnAction(e -> toggleTheme());
    }

    private void toggleTheme() {
        Main.toggleTheme();
        updateThemeIcon();
    }

    private void updateThemeIcon() {
        if (Main.isDarkMode()) {
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

    @FXML
    public void showCatalogue() {
        loadView("/cours_candidat.fxml");
        setActiveButton(btnCatalogue);
    }

    // ============================================
    // FONCTIONNALITÉS À VENIR - MODIFIÉES
    // ============================================
    @FXML
    public void showMesCours() {
        AlertUtils.showInfo("📚 Mes cours",
                "Cette fonctionnalité arrivera très bientôt !\n\n" +
                        "Vous pourrez suivre votre progression dans tous vos cours.");
    }

    // Modifiez la méthode showCertificats() :
    @FXML
    public void showCertificats() {
        loadView("/certificats_candidat.fxml");
        setActiveButton(btnCertificats);
    }

    @FXML
    public void showProfil() {
        AlertUtils.showInfo("👤 Mon profil",
                "Cette fonctionnalité arrivera très bientôt !\n\n" +
                        "Vous pourrez modifier vos informations personnelles.");
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
            AlertUtils.showError("❌ Erreur", "Impossible d'ouvrir le cours:\n\n" + e.getMessage());
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

            AlertUtils.showInfo("📝 Quiz du module",
                    "Vous allez passer le quiz du module \"" + moduleTitre + "\".\n\n" +
                            "Répondez aux 5 questions pour valider ce module.");

        } catch (IOException e) {
            e.printStackTrace();
            AlertUtils.showError("❌ Erreur", "Impossible d'ouvrir le quiz:\n\n" + e.getMessage());
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

            AlertUtils.showInfo("🎯 Test final",
                    "Vous allez passer le test final du cours.\n\n" +
                            "15 questions pour valider l'ensemble du cours. Bonne chance !");

        } catch (IOException e) {
            e.printStackTrace();
            AlertUtils.showError("❌ Erreur", "Impossible d'ouvrir le test final:\n\n" + e.getMessage());
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

    // ============================================
    // DÉCONNEXION - MODIFIÉE
    // ============================================
    @FXML
    public void logout() {
        boolean confirmed = AlertUtils.showConfirmation(
                "🔒 Déconnexion",
                "Êtes-vous sûr de vouloir vous déconnecter ?\n\n" +
                        "Votre progression sera sauvegardée automatiquement.",
                "Oui, me déconnecter",
                "Non, rester connecté"
        );

        if (confirmed) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), sidebar.getScene().getRoot());
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                javafx.application.Platform.exit();
                System.exit(0);
            });
            fadeOut.play();

            AlertUtils.showSuccess("👋 Au revoir !", "Déconnexion réussie. À bientôt !");
        }
    }

    private void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Node view = loader.load();
            animateContentChange(view);
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtils.showError("❌ Erreur", "Impossible de charger la vue:\n\n" + fxmlFile);
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
}