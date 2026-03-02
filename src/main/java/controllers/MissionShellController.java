package controllers;  // Make sure this matches your package structure

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
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;

public class MissionShellController implements Initializable {

    @FXML private Button btnMessages;
    @FXML private Button btnContacts;
    @FXML private StackPane contentPane;
    @FXML private VBox sidebar;
    @FXML private ImageView brandLogo;
    @FXML private Button btnTheme;
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
        userName.setText("Bensaid Youssef");
        userRole.setText("Administrateur");
        brandText.setText("Carrieri");
        brandSubtext.setText("Communication");

        // Load the logo
        loadLogo();

        // Afficher la vue par défaut (contacts)
        showContactsView();
        setActiveButton(btnContacts);

        // Tooltip pour l'avatar
        Tooltip tooltip = new Tooltip("Profil utilisateur");
        Tooltip.install(userAvatar, tooltip);

        // Initialiser le bouton de thème
        setupThemeButton();

        // Configurer les animations
        setupAnimations();

        System.out.println("✅ MissionShellController initialisé avec succès");
    }

    private void loadLogo() {
        try {
            if (brandLogo == null) {
                System.err.println("❌ brandLogo is null - check FXML fx:id");
                return;
            }

            // Try to load logo from resources
            InputStream inputStream = getClass().getResourceAsStream("/images/logo.png");

            if (inputStream != null) {
                System.out.println("✅ Logo found");
                Image image = new Image(inputStream);
                brandLogo.setImage(image);
                brandLogo.setFitWidth(36);
                brandLogo.setFitHeight(36);
                brandLogo.setPreserveRatio(true);
            } else {
                // If no logo found, use a text fallback
                System.err.println("❌ Logo not found");
                useFallbackLogo();
            }

        } catch (Exception e) {
            System.err.println("Error loading logo: " + e.getMessage());
            useFallbackLogo();
        }
    }

    private void useFallbackLogo() {
        if (brandLogo != null) {
            brandLogo.setVisible(false);

            // Get the parent StackPane
            StackPane parent = (StackPane) brandLogo.getParent();

            // Create a text label as fallback
            Label fallbackLabel = new Label("📱");
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
            showInfo("Thème", "Mode sombre activé");
        } else {
            btnTheme.setText("🌙");
            if (sidebar.getScene() != null && sidebar.getScene().getRoot() != null) {
                sidebar.getScene().getRoot().getStyleClass().remove("dark-theme");
            }
            showInfo("Thème", "Mode clair activé");
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
    public void showMessagesView() {
        loadView("/message.fxml");
        setActiveButton(btnMessages);
    }

    @FXML
    public void showContactsView() {
        loadView("/contacts-view.fxml");
        setActiveButton(btnContacts);
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

            // If loading the messages view, you might want to set the current user
            if (fxmlFile.equals("/message.fxml") && loader.getController() instanceof MessengerController) {
                MessengerController controller = loader.getController();
                controller.setCurrentUserId(1); // Set default user ID
            }

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

    @FXML
    public void logout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);

        VBox content = new VBox(10);
        content.setAlignment(Pos.CENTER_LEFT);

        Label mainMessage = new Label("Déconnexion");
        mainMessage.getStyleClass().add("main-message");

        Label warningMessage = new Label("Êtes-vous sûr de vouloir vous déconnecter ?");
        warningMessage.getStyleClass().add("warning-message");

        content.getChildren().addAll(mainMessage, warningMessage);
        confirm.getDialogPane().setContent(content);

        DialogPane dialogPane = confirm.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.getStyleClass().add("confirmation");
        dialogPane.getStylesheets().add(getClass().getResource("/Alert.css").toExternalForm());

        ButtonType okButtonType = new ButtonType("Déconnexion", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(okButtonType, cancelButtonType);

        Button okButton = (Button) dialogPane.lookupButton(okButtonType);
        Button cancelButton = (Button) dialogPane.lookupButton(cancelButtonType);

        okButton.getStyleClass().add("btn-danger");
        cancelButton.getStyleClass().add("btn-secondary");

        confirm.showAndWait().ifPresent(response -> {
            if (response == okButtonType) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(500), sidebar.getScene().getRoot());
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> {
                    Platform.exit();
                    System.exit(0);
                });
                fadeOut.play();
            }
        });
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.getStyleClass().add("info");
        dialogPane.getStylesheets().add(getClass().getResource("/Alert.css").toExternalForm());

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("btn-primary");
        }

        alert.showAndWait();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.getStyleClass().add("info");
        dialogPane.getStyleClass().add("success-alert");
        dialogPane.getStylesheets().add(getClass().getResource("/Alert.css").toExternalForm());

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("btn-primary");
        }

        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.getStyleClass().add("warning");
        dialogPane.getStylesheets().add(getClass().getResource("/Alert.css").toExternalForm());

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("btn-primary");
        }

        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.getStyleClass().add("error");
        dialogPane.getStylesheets().add(getClass().getResource("/Alert.css").toExternalForm());

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("btn-danger");
        }

        alert.showAndWait();
    }
}
