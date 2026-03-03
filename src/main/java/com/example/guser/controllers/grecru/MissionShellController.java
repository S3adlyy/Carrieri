package com.example.guser.controllers.grecru;

import entities.grecru.Mission;
import entities.grecru.RenduMission;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import services.grecru.MissionService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;

public class MissionShellController implements Initializable {

    @FXML private Button btnCalendar;
    @FXML private StackPane contentPane;
    @FXML private VBox sidebar;
    @FXML private ImageView brandLogo;  // This should now be properly injected
    @FXML private Button btnMission;
    @FXML private Button btnAddMission;
    @FXML private Button btnRendu;
    @FXML private Button btnStats;
    @FXML private Button btnEntretien;
    @FXML private Button btnTheme;
    @FXML private Label missionText;
    @FXML private Label renduText;
    @FXML private Label statsText;
    @FXML private Label entretienText;
    @FXML private Label brandText;
    @FXML private Label brandSubtext;
    @FXML private Label userName;
    @FXML private Label userRole;
    @FXML private Circle userAvatar;

    private Button activeButton = null;
    private static MissionShellController instance;

    private Timeline expandAnimation;
    private Timeline collapseAnimation;

    public MissionShellController() {
        instance = this;
    }

    public static MissionShellController getInstance() {
        return instance;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        userName.setText("Admin User");
        userRole.setText("Administrateur");
        brandText.setText("Carrieri");
        brandSubtext.setText("Gestion Recrutement");

        // Load the logo
        loadLogo();

        // Afficher la vue par défaut (ajout mission)
        showMissionAddView();
        setActiveButton(btnAddMission);

        // Tooltip pour l'avatar
        Tooltip tooltip = new Tooltip("Profil utilisateur");
        Tooltip.install(userAvatar, tooltip);

        // Initialiser le bouton de thème
        setupThemeButton();

        // Configurer les animations
        setupAnimations();

        System.out.println("✅ MissionShellController initialisé avec succès");
        System.out.println("brandLogo is " + (brandLogo != null ? "connected" : "null"));
    }

    private void loadLogo() {
        try {
            // Check if brandLogo is properly injected
            if (brandLogo == null) {
                System.err.println("❌ brandLogo is null - check FXML fx:id");
                return;
            }

            // Try multiple possible paths
            InputStream inputStream = null;
            String[] possiblePaths = {
                    "/images/logo.png",
                    "/com/example/guser/images/logo.png",
                    "/logo.png",
                    "/images/carrieri-logo.png",
                    "/com/exemple/grecrutement/logo.png"
            };

            for (String path : possiblePaths) {
                inputStream = getClass().getResourceAsStream(path);
                if (inputStream != null) {
                    System.out.println("✅ Logo found at: " + path);
                    Image image = new Image(inputStream);
                    brandLogo.setImage(image);
                    brandLogo.setFitWidth(36);
                    brandLogo.setFitHeight(36);
                    brandLogo.setPreserveRatio(true);
                    return;
                }
            }

            // If no logo found, use a colored circle as fallback
            System.err.println("❌ Logo not found in any of the attempted paths");
            useFallbackLogo();

        } catch (Exception e) {
            System.err.println("Error loading logo: " + e.getMessage());
            useFallbackLogo();
        }
    }

    private void useFallbackLogo() {
        // Since we can't easily replace ImageView with a Circle in FXML,
        // we'll hide the ImageView and show a text label in its parent
        if (brandLogo != null) {
            brandLogo.setVisible(false);

            // Get the parent StackPane
            StackPane parent = (StackPane) brandLogo.getParent();

            // Create a text label as fallback
            Label fallbackLabel = new Label("📘");
            fallbackLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #5E548E; -fx-font-weight: bold;");
            StackPane.setAlignment(fallbackLabel, Pos.CENTER);

            // Add it to the parent
            parent.getChildren().add(fallbackLabel);

            System.out.println("✅ Using fallback text logo");
        }
    }

    // ============================================
    // GESTION DU THÈME
    // ============================================

    private void setupThemeButton() {
        updateThemeIcon();
        btnTheme.setOnAction(e -> toggleTheme());
    }

    private void toggleTheme() {
        boolean isDarkMode = btnTheme.getText().equals("🌙");
        if (isDarkMode) {
            btnTheme.setText("☀");
            if (sidebar.getScene() != null && sidebar.getScene().getRoot() != null) {
                sidebar.getScene().getRoot().getStyleClass().add("dark-theme");
            }
        } else {
            btnTheme.setText("🌙");
            if (sidebar.getScene() != null && sidebar.getScene().getRoot() != null) {
                sidebar.getScene().getRoot().getStyleClass().remove("dark-theme");
            }
        }
    }

    private void updateThemeIcon() {
        btnTheme.setText("🌙");
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
    // MÉTHODES DE NAVIGATION PRINCIPALES
    // ============================================

    @FXML
    public void showMissionList() {
        loadView("/com/example/guser/grecru/mission-list.fxml");
        setActiveButton(btnMission);
    }

    @FXML
    public void showMissionAddView() {
        loadView("/com/example/guser/grecru/mission-add.fxml");
        setActiveButton(btnAddMission);
    }

    @FXML
    public void showRenduList() {
        loadView("/com/example/guser/grecru/rendu-list.fxml");
        setActiveButton(btnRendu);
    }

    @FXML
    public void showStatistics() {
        loadView("/com/example/guser/grecru/rendu-stats.fxml");
        setActiveButton(btnStats);
    }

    @FXML
    public void showEntretienView() {
        loadView("/com/example/guser/grecru/entretien-create.fxml");
        setActiveButton(btnEntretien);
    }

    @FXML
    public void showCalendarView() {
        loadView("/com/example/guser/grecru/calendar-view.fxml");
        setActiveButton(btnCalendar);
    }

    // ============================================
    // MÉTHODES SPÉCIFIQUES
    // ============================================

    public void showMissionEditView(int missionId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/guser/grecru/mission-edit.fxml"));
            Node view = loader.load();
            MissionEditController controller = loader.getController();
            Mission mission = getMissionById(missionId);
            if (mission != null) {
                controller.setMission(mission);
            }
            animateContentChange(view);
            setActiveButton(btnMission);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorPlaceholder("Erreur chargement MissionEdit");
        }
    }

    public void showRenduAddWithMissionId(int missionId) {
        try {
            System.out.println("📂 Loading rendu-add.fxml for mission ID: " + missionId);

            // Try multiple possible paths
            String[] possiblePaths = {
                    "/com/example/guser/grecru/rendu-add.fxml",
                    "/rendu-add.fxml",
                    "rendu-add.fxml"
            };

            URL fxmlUrl = null;
            for (String path : possiblePaths) {
                fxmlUrl = getClass().getResource(path);
                if (fxmlUrl != null) {
                    System.out.println("✅ FXML found at: " + path);
                    break;
                }
            }

            if (fxmlUrl == null) {
                throw new IOException("Could not find rendu-add.fxml in any of the expected paths");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Node view = loader.load();
            System.out.println("✅ FXML loaded successfully");

            RenduAddController controller = loader.getController();
            System.out.println("✅ Controller obtained");

            controller.setMissionId(missionId);
            System.out.println("✅ Mission ID set to: " + missionId);

            animateContentChange(view);
            setActiveButton(btnRendu);
            System.out.println("✅ View changed successfully");

        } catch (IOException e) {
            System.err.println("❌ IOException loading rendu-add.fxml: " + e.getMessage());
            e.printStackTrace();
            showErrorPlaceholder("Erreur chargement RenduAdd: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            showErrorPlaceholder("Erreur: " + e.getMessage());
        }
    }

    public void showScheduleInterview(RenduMission rendu) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/guser/grecru/entretien-create.fxml"));
            Node view = loader.load();
            EntretienCreateController controller = loader.getController();
            controller.setRenduMission(rendu);
            animateContentChange(view);
            setActiveButton(btnEntretien);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorPlaceholder("Erreur chargement Entretien");
        }
    }

    public void refreshCurrentView() {
        if (activeButton == btnMission) showMissionList();
        else if (activeButton == btnAddMission) showMissionAddView();
        else if (activeButton == btnRendu) showRenduList();
        else if (activeButton == btnStats) showStatistics();
        else if (activeButton == btnEntretien) showEntretienView();
        else if (activeButton == btnCalendar) showCalendarView();
    }

    // ============================================
    // MÉTHODES UTILITAIRES
    // ============================================

    private void loadView(String fxmlFile) {
        try {
            System.out.println("📂 Chargement: " + fxmlFile);
            URL resourceUrl = getClass().getResource(fxmlFile);
            if (resourceUrl == null) {
                System.err.println("❌ Fichier non trouvé: " + fxmlFile);
                showErrorPlaceholder("Fichier non trouvé: " + fxmlFile);
                return;
            }
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Node view = loader.load();
            animateContentChange(view);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorPlaceholder("Erreur: " + fxmlFile);
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

    private void showErrorPlaceholder(String message) {
        Label errorLabel = new Label("❌ " + message);
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 16px; -fx-font-weight: bold;");
        StackPane.setAlignment(errorLabel, Pos.CENTER);
        contentPane.getChildren().setAll(errorLabel);
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

    private Mission getMissionById(int id) {
        try {
            MissionService service = new MissionService();
            return service.getById(id);
        } catch (Exception e) {
            System.err.println("Erreur récupération mission: " + e.getMessage());
            return null;
        }
    }

    @FXML
    public void logout() {
        if (showConfirmation("Déconnexion", "Êtes-vous sûr de vouloir vous déconnecter ?")) {
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

    private boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}