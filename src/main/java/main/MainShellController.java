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

    // Services pour vérifier l'existence
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

    // ============================================
    // BASCULE VERS CANDIDAT - MODIFIÉ
    // ============================================
    @FXML
    public void switchToCandidat() {
        boolean confirmed = AlertUtils.showConfirmation(
                "🔄 Changement de mode",
                "Voulez-vous basculer vers l'espace Candidat ?\n\n" +
                        "Vous pourrez voir les cours comme un candidat et suivre votre progression.",
                "Oui, basculer",
                "Non, rester"
        );

        if (confirmed) {
            try {
                Stage stage = (Stage) sidebar.getScene().getWindow();

                // ✅ Sauvegarder TOUS les paramètres de la fenêtre
                boolean etaitMaximized = stage.isMaximized();
                boolean etaitFullScreen = stage.isFullScreen();
                double width = stage.getWidth();
                double height = stage.getHeight();
                double x = stage.getX();
                double y = stage.getY();

                System.out.println("📊 Sauvegarde - Maximized: " + etaitMaximized +
                        ", Width: " + width + ", Height: " + height);

                // Animation de fondu
                Parent currentRoot = stage.getScene().getRoot();
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentRoot);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);

                fadeOut.setOnFinished(e -> {
                    try {
                        Parent newRoot = FXMLLoader.load(getClass().getResource("/candidat-shell.fxml"));
                        newRoot.setOpacity(0);

                        // ✅ Créer la scène avec les dimensions sauvegardées
                        Scene scene = new Scene(newRoot, width, height);
                        stage.setScene(scene);
                        stage.setTitle("E-Learning - Espace Candidat");

                        // ✅ Restaurer la position
                        stage.setX(x);
                        stage.setY(y);

                        // ✅ Restaurer l'état maximized APRÈS avoir défini la scène
                        if (etaitMaximized) {
                            Platform.runLater(() -> {
                                stage.setMaximized(true);
                                System.out.println("✅ Mode maximized restauré");
                            });
                        }

                        if (etaitFullScreen) {
                            Platform.runLater(() -> stage.setFullScreen(true));
                        }

                        // Animation d'entrée
                        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newRoot);
                        fadeIn.setFromValue(0);
                        fadeIn.setToValue(1);
                        fadeIn.play();

                        AlertUtils.showSuccess("✅ Bascule réussie", "Vous êtes maintenant dans l'espace Candidat.");

                    } catch (IOException ex) {
                        ex.printStackTrace();
                        AlertUtils.showError("❌ Erreur de chargement",
                                "Impossible de charger l'espace candidat.\n\n" + ex.getMessage());
                    }
                });

                fadeOut.play();

            } catch (Exception e) {
                e.printStackTrace();
                AlertUtils.showError("❌ Erreur", "Impossible de basculer vers le mode Candidat");
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        userName.setText("Bilal El Eter");
        userRole.setText("Admin");

        showCoursView();
        setActiveButton(btnCours);

        // Installer le tooltip
        Tooltip tooltip = new Tooltip("Cliquer pour basculer en mode Candidat");
        Tooltip.install(userAvatar, tooltip);

        setupAnimations();
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
    public void showCoursView() {
        loadView("/cours.fxml");
        setActiveButton(btnCours);
    }

    // ============================================
    // AFFICHER MODULES - MODIFIÉ
    // ============================================
    @FXML
    public void showModulesView() {
        if (hasCoursActif) {
            try {
                // ✅ Vérifier si le cours existe toujours
                Cours cours = coursService.getById(currentCoursId);
                if (cours == null) {
                    // Le cours n'existe plus, réinitialiser
                    resetCurrentCours();
                    AlertUtils.showWarning("⚠️ Cours introuvable",
                            "Le cours sélectionné n'existe plus ou a été supprimé.");
                    showCoursView();
                    return;
                }
                showModulesViewWithCours(currentCoursId, currentCoursTitre);
            } catch (SQLException e) {
                resetCurrentCours();
                AlertUtils.showError("❌ Erreur", "Impossible de vérifier le cours :\n" + e.getMessage());
                showCoursView();
            }
        } else {
            AlertUtils.showNoSelectionWarning("cours", "afficher ses modules");
            showCoursView();
        }
    }

    // ============================================
    // AFFICHER LEÇONS - MODIFIÉ
    // ============================================
    @FXML
    public void showLeconsView() {
        if (hasModuleActif) {
            // ✅ Vérifier si le module existe toujours
            Module module = moduleService.getModuleById(currentModuleId);
            if (module == null) {
                // Le module n'existe plus, réinitialiser
                resetCurrentModule();
                AlertUtils.showWarning("⚠️ Module introuvable",
                        "Le module sélectionné n'existe plus ou a été supprimé.");
                showModulesView();
                return;
            }
            showLeconsViewWithModule(currentModuleId, currentModuleTitre);
        } else if (hasCoursActif) {
            AlertUtils.showNoSelectionWarning("module", "afficher ses leçons");
            showModulesView();
        } else {
            AlertUtils.showNoSelectionWarning("cours", "afficher ses modules");
            showCoursView();
        }
    }

    public void setCurrentCours(int coursId, String coursTitre) {
        this.currentCoursId = coursId;
        this.currentCoursTitre = coursTitre;
        this.hasCoursActif = true;
        // Quand on sélectionne un nouveau cours, on réinitialise le module actif
        this.hasModuleActif = false;
        System.out.println("✅ Cours actif: " + coursTitre + " (ID: " + coursId + ")");

        // ✅ Notification optionnelle
        // AlertUtils.showInfo("ℹ️ Cours sélectionné", "Cours actif : \"" + coursTitre + "\"");
    }

    public void setCurrentModule(int moduleId, String moduleTitre) {
        this.currentModuleId = moduleId;
        this.currentModuleTitre = moduleTitre;
        this.hasModuleActif = true;
        System.out.println("✅ Module actif: " + moduleTitre + " (ID: " + moduleId + ")");

        // ✅ Notification optionnelle
        // AlertUtils.showInfo("ℹ️ Module sélectionné", "Module actif : \"" + moduleTitre + "\"");
    }

    public void resetCurrentCours() {
        this.currentCoursId = 0;
        this.currentCoursTitre = "";
        this.hasCoursActif = false;
        System.out.println("🔄 Cours actif réinitialisé");
    }

    public void resetCurrentModule() {
        this.currentModuleId = 0;
        this.currentModuleTitre = "";
        this.hasModuleActif = false;
        System.out.println("🔄 Module actif réinitialisé");
    }

    public void showModulesViewWithCours(int coursId, String coursTitre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/module.fxml"));
            Node view = loader.load();
            ModuleController controller = loader.getController();
            controller.setCoursInfo(coursId, coursTitre);
            animateContentChange(view);
            setActiveButton(btnModules);
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtils.showError("❌ Erreur", "Impossible de charger la vue des modules.");
        }
    }

    public void showLeconsViewWithCours(int coursId, String coursTitre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/lecon.fxml"));
            Node view = loader.load();
            LeconController controller = loader.getController();
            controller.setCoursId(coursId);
            animateContentChange(view);
            setActiveButton(btnLecons);
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtils.showError("❌ Erreur", "Impossible de charger la vue des leçons.");
        }
    }

    public void showLeconsViewWithModule(int moduleId, String moduleTitre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/lecon.fxml"));
            Node view = loader.load();
            LeconController controller = loader.getController();
            controller.setModuleInfo(moduleId, moduleTitre);
            animateContentChange(view);
            setActiveButton(btnLecons);
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtils.showError("❌ Erreur", "Impossible de charger la vue des leçons.");
        }
    }

    // ============================================
    // DÉCONNEXION - MODIFIÉ
    // ============================================
    @FXML
    public void logout() {
        boolean confirmed = AlertUtils.showConfirmation(
                "🔒 Déconnexion",
                "Êtes-vous sûr de vouloir vous déconnecter ?\n\n" +
                        "Toutes les modifications non sauvegardées seront perdues.",
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
            AlertUtils.showError("❌ Erreur", "Impossible de charger la vue : " + fxmlFile);
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