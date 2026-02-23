package main;

import entities.Cours;
import entities.Module;
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
import services.CoursService;
import services.ModuleService;
import utils.AlertUtils;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class MainShellController implements Initializable {

    @FXML private StackPane contentPane;
    @FXML private VBox sidebar;

    @FXML private Button btnCours;
    @FXML private Button btnModules;
    @FXML private Button btnLecons;
    @FXML private Button btnTheme;  // NOUVEAU BOUTON

    @FXML private Label coursText;
    @FXML private Label modulesText;
    @FXML private Label leconsText;
    @FXML private Label brandText;
    @FXML private Label brandSubtext;
    @FXML private Label userName;
    @FXML private Label userRole;
    @FXML private Circle userAvatar;

    private Button activeButton = null;
    private static MainShellController instance;

    // Variables pour le cours actif
    private int currentCoursId = 0;
    private String currentCoursTitre = "";
    private boolean hasCoursActif = false;

    // Variables pour le module actif
    private int currentModuleId = 0;
    private String currentModuleTitre = "";
    private boolean hasModuleActif = false;

    private CoursService coursService = new CoursService();
    private ModuleService moduleService = new ModuleService();

    private Timeline expandAnimation;
    private Timeline collapseAnimation;

    public MainShellController() {
        instance = this;
    }

    public static MainShellController getInstance() {
        return instance;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        userName.setText("Bilal El Eter");
        userRole.setText("Admin");

        showCoursView();
        setActiveButton(btnCours);

        // Tooltip pour l'avatar
        Tooltip tooltip = new Tooltip("Cliquer pour basculer en mode Candidat");
        Tooltip.install(userAvatar, tooltip);

        // Initialiser le bouton de thème
        setupThemeButton();

        setupAnimations();
    }

    // ============================================
    // GESTION DU THÈME
    // ============================================

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
    // NAVIGATION
    // ============================================

    @FXML
    public void showCoursView() {
        loadView("/Cours.fxml");
        setActiveButton(btnCours);
    }

    @FXML
    public void showModulesView() {
        if (hasCoursActif) {
            try {
                Cours cours = coursService.getById(currentCoursId);
                if (cours == null) {
                    resetCurrentCours();
                    showCoursView();
                    return;
                }
                showModulesViewWithCours(currentCoursId, currentCoursTitre);
            } catch (SQLException e) {
                resetCurrentCours();
                showCoursView();
            }
        } else {
            AlertUtils.showNoSelectionWarning("cours", "afficher ses modules");
            showCoursView();
        }
    }

    @FXML
    public void showLeconsView() {
        System.out.println("📋 showLeconsView appelé");
        System.out.println("   hasModuleActif: " + hasModuleActif);
        System.out.println("   currentModuleId: " + currentModuleId);
        System.out.println("   currentModuleTitre: " + currentModuleTitre);
        System.out.println("   hasCoursActif: " + hasCoursActif);

        if (hasModuleActif) {
            System.out.println("✅ Module actif trouvé: " + currentModuleTitre);
            Module module = moduleService.getModuleById(currentModuleId);
            if (module == null) {
                System.out.println("❌ Module introuvable en base, réinitialisation");
                resetCurrentModule();
                showModulesView();
                return;
            }
            System.out.println("✅ Module valide, affichage des leçons");
            showLeconsViewWithModule(currentModuleId, currentModuleTitre);
        } else if (hasCoursActif) {
            System.out.println("⚠️ Aucun module actif mais cours actif: " + currentCoursTitre);
            AlertUtils.showNoSelectionWarning("module", "afficher ses leçons");
            showModulesView();
        } else {
            System.out.println("⚠️ Aucun cours actif");
            AlertUtils.showNoSelectionWarning("cours", "afficher ses modules");
            showCoursView();
        }
    }

    // ============================================
    // MÉTHODES DE NAVIGATION
    // ============================================

    public void setCurrentCours(int coursId, String coursTitre) {
        this.currentCoursId = coursId;
        this.currentCoursTitre = coursTitre;
        this.hasCoursActif = true;
        this.hasModuleActif = false;
    }

    public void setCurrentModule(int moduleId, String moduleTitre) {
        this.currentModuleId = moduleId;
        this.currentModuleTitre = moduleTitre;
        this.hasModuleActif = true;
    }

    public void resetCurrentCours() {
        this.currentCoursId = 0;
        this.currentCoursTitre = "";
        this.hasCoursActif = false;
    }

    public void resetCurrentModule() {
        this.currentModuleId = 0;
        this.currentModuleTitre = "";
        this.hasModuleActif = false;
    }

    public void showModulesViewWithCours(int coursId, String coursTitre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Module.fxml"));
            Node view = loader.load();
            ModuleController controller = loader.getController();
            controller.setCoursInfo(coursId, coursTitre);
            animateContentChange(view);
            setActiveButton(btnModules);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showLeconsViewWithModule(int moduleId, String moduleTitre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Lecon.fxml"));
            Node view = loader.load();
            LeconController controller = loader.getController();
            controller.setModuleInfo(moduleId, moduleTitre);
            animateContentChange(view);
            setActiveButton(btnLecons);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void switchToCandidat() {
        if (AlertUtils.showConfirmation("Changement de mode",
                "Voulez-vous basculer vers l'espace Candidat ?")) {
            try {
                Stage stage = (Stage) sidebar.getScene().getWindow();
                boolean etaitMaximized = stage.isMaximized();
                double width = stage.getWidth();
                double height = stage.getHeight();
                double x = stage.getX();
                double y = stage.getY();

                Parent currentRoot = stage.getScene().getRoot();
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentRoot);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);

                fadeOut.setOnFinished(e -> {
                    try {
                        Parent newRoot = FXMLLoader.load(getClass().getResource("/CandidatShell.fxml"));
                        newRoot.setOpacity(0);

                        Scene scene = new Scene(newRoot, width, height);
                        stage.setScene(scene);
                        stage.setTitle("E-Learning - Espace Candidat");

                        stage.setX(x);
                        stage.setY(y);

                        if (etaitMaximized) {
                            Platform.runLater(() -> stage.setMaximized(true));
                        }

                        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newRoot);
                        fadeIn.setFromValue(0);
                        fadeIn.setToValue(1);
                        fadeIn.play();

                        // ✅ CORRECTION
                        Platform.runLater(() -> {
                            AlertUtils.showSuccess("✅ Bascule réussie",
                                    "Vous êtes maintenant dans l'espace Candidat.");
                        });

                    } catch (IOException ex) {
                        ex.printStackTrace();
                        Platform.runLater(() -> {
                            AlertUtils.showError("Erreur", "Impossible de charger l'espace candidat");
                        });
                    }
                });

                fadeOut.play();

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    AlertUtils.showError("Erreur", "Impossible de basculer");
                });
            }
        }
    }

    @FXML
    public void logout() {
        if (AlertUtils.showConfirmation("Déconnexion", "Êtes-vous sûr de vouloir vous déconnecter ?")) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), sidebar.getScene().getRoot());
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                Platform.exit();
                System.exit(0);
            });
            fadeOut.play();
        }
    }

    private void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Node view = loader.load();
            animateContentChange(view);
        } catch (IOException e) {
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
}