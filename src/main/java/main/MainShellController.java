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

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

import static main.AlertUtils.showAlert;

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

    @FXML
    public void switchToCandidat() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Changement de mode");
        confirm.setHeaderText("Passer en mode Candidat");
        confirm.setContentText("Voulez-vous basculer vers l'espace Candidat ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
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
                            // Petit délai pour que la scène soit bien prise en compte
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

                    } catch (IOException ex) {
                        ex.printStackTrace();
                        showAlert(Alert.AlertType.ERROR,"Erreur de chargement", "Impossible de charger l'espace candidat");
                    }
                });

                fadeOut.play();

            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR,"Erreur", "Impossible de basculer vers le mode Candidat");
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

    @FXML
    public void showModulesView() {
        if (hasCoursActif) {
            try {
                // ✅ Vérifier si le cours existe toujours
                Cours cours = coursService.getById(currentCoursId);
                if (cours == null) {
                    // Le cours n'existe plus, réinitialiser
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
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Aucun cours sélectionné");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez d'abord sélectionner un cours dans la liste.");
            alert.showAndWait();
            showCoursView();
        }
    }

    @FXML
    public void showLeconsView() {
        if (hasModuleActif) {
            // ✅ Vérifier si le module existe toujours
            Module module = moduleService.getModuleById(currentModuleId);
            if (module == null) {
                // Le module n'existe plus, réinitialiser
                resetCurrentModule();
                showModulesView();
                return;
            }
            showLeconsViewWithModule(currentModuleId, currentModuleTitre);
        } else if (hasCoursActif) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Aucun module sélectionné");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez d'abord sélectionner un module dans la liste.");
            alert.showAndWait();
            showModulesView();
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Aucun cours sélectionné");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez d'abord sélectionner un cours.");
            alert.showAndWait();
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
    }

    public void setCurrentModule(int moduleId, String moduleTitre) {
        this.currentModuleId = moduleId;
        this.currentModuleTitre = moduleTitre;
        this.hasModuleActif = true;
        System.out.println("✅ Module actif: " + moduleTitre + " (ID: " + moduleId + ")");
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