package com.exemple.grecrutement;

import entities.Mission;
import entities.RenduMission;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import javafx.scene.input.MouseEvent;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MissionShellController {

    private static MissionShellController instance;
    private Map<Button, Timeline> buttonAnimations = new HashMap<>();
    private Button activeButton = null;

    @FXML
    private StackPane contentPane;

    @FXML
    private VBox navbarContainer;

    @FXML
    private Button btnAddMission;

    @FXML
    private Button btnMissionList;

    @FXML
    private Button btnSubmitRendu;

    @FXML
    private Button btnRenduList;

    @FXML
    private Circle navAvatar;

    @FXML
    private Label navNameLabel;

    @FXML
    private ImageView brandLogo;

    public MissionShellController() {
        instance = this;
    }

    public static MissionShellController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        loadLogo();
        setupLiquidAnimations();
        setupButtonHoverEffects();
        showAddMission();
        setActiveButton(btnAddMission);

        // Set user info (example - replace with actual user data)
        navNameLabel.setText("Saadli Wassim");

        // Fade in animation for content
        contentPane.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), contentPane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void loadLogo() {
        try {
            // Try multiple possible paths
            InputStream inputStream = null;
            String[] possiblePaths = {
                    "/images/logo.png",
                    "/com/exemple/grecrutement/images/logo.png",
                    "/logo.png",
                    "images/logo.png"
            };

            for (String path : possiblePaths) {
                inputStream = getClass().getResourceAsStream(path);
                if (inputStream != null) {
                    System.out.println("✅ Logo found at: " + path);
                    break;
                }
            }

            if (inputStream != null) {
                Image image = new Image(inputStream);
                brandLogo.setImage(image);
                // Optional: set fit dimensions
                brandLogo.setFitWidth(36);
                brandLogo.setFitHeight(36);
                brandLogo.setPreserveRatio(true);
            } else {
                System.err.println("❌ Logo not found in any of the attempted paths");
                // Fallback: create a colored circle as placeholder
                createPlaceholderLogo();
            }
        } catch (Exception e) {
            System.err.println("Error loading logo: " + e.getMessage());
            createPlaceholderLogo();
        }
    }

    private void createPlaceholderLogo() {
        // Create a colored circle as placeholder logo
        Circle placeholder = new Circle(18);
        placeholder.setFill(Color.rgb(139, 92, 246)); // Purple color
        placeholder.setEffect(new DropShadow(10, Color.rgb(139, 92, 246, 0.5)));

        // Replace the ImageView with a Circle (you'll need to adjust FXML)
        // For now, just set a default image on the ImageView
        brandLogo.setImage(null);
    }

    // =========================
    // LIQUID ANIMATIONS SETUP
    // =========================

    private void setupLiquidAnimations() {
        Button[] buttons = {btnAddMission, btnMissionList, btnSubmitRendu, btnRenduList};

        for (Button button : buttons) {
            // Create liquid hover animation
            Timeline liquidHover = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(button.scaleXProperty(), 1),
                            new KeyValue(button.scaleYProperty(), 1),
                            new KeyValue(button.translateXProperty(), 0),
                            new KeyValue(button.effectProperty(), null)
                    ),
                    new KeyFrame(Duration.millis(200),
                            new KeyValue(button.scaleXProperty(), 1.05),
                            new KeyValue(button.scaleYProperty(), 1.05),
                            new KeyValue(button.translateXProperty(), 4),
                            new KeyValue(button.effectProperty(),
                                    new DropShadow(BlurType.GAUSSIAN,
                                            Color.rgb(94, 84, 142, 0.4),
                                            15, 0.2, 2, 2))
                    )
            );

            liquidHover.setCycleCount(1);
            buttonAnimations.put(button, liquidHover);

            // Add mouse event handlers
            button.setOnMouseEntered(e -> {
                if (button != activeButton) {
                    liquidHover.playFromStart();
                }
            });

            button.setOnMouseExited(e -> {
                if (button != activeButton) {
                    if (liquidHover.getStatus() == Animation.Status.RUNNING) {
                        liquidHover.stop();
                    }
                    resetButtonScale(button);
                }
            });

            // Add click ripple effect
            button.setOnMousePressed(e -> createLiquidRipple(button, e));
        }
    }

    private void setupButtonHoverEffects() {
        // Add subtle glow animation to all buttons
        Button[] buttons = {btnAddMission, btnMissionList, btnSubmitRendu, btnRenduList};

        for (Button button : buttons) {
            Glow glow = new Glow(0);

            Timeline glowAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(glow.levelProperty(), 0)
                    ),
                    new KeyFrame(Duration.millis(1500),
                            new KeyValue(glow.levelProperty(), 0.3)
                    ),
                    new KeyFrame(Duration.millis(3000),
                            new KeyValue(glow.levelProperty(), 0)
                    )
            );

            glowAnimation.setCycleCount(Timeline.INDEFINITE);

            // Only play for non-active buttons
            if (button != activeButton) {
                button.setEffect(glow);
                glowAnimation.play();
            }
        }
    }

    private void resetButtonScale(Button button) {
        Timeline reset = new Timeline(
                new KeyFrame(Duration.millis(150),
                        new KeyValue(button.scaleXProperty(), 1),
                        new KeyValue(button.scaleYProperty(), 1),
                        new KeyValue(button.translateXProperty(), 0),
                        new KeyValue(button.effectProperty(), null)
                )
        );
        reset.play();
    }

    private void createLiquidRipple(Button button, MouseEvent event) {
        Circle ripple = new Circle();
        ripple.setFill(Color.rgb(255, 255, 255, 0.3));
        ripple.setCenterX(event.getX());
        ripple.setCenterY(event.getY());
        ripple.setRadius(0);

        // Get the parent container (the VBox of nav links)
        VBox buttonContainer = (VBox) button.getParent();
        buttonContainer.getChildren().add(ripple);

        Timeline rippleAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(ripple.radiusProperty(), 0),
                        new KeyValue(ripple.opacityProperty(), 0.7)
                ),
                new KeyFrame(Duration.millis(600),
                        new KeyValue(ripple.radiusProperty(),
                                Math.max(button.getWidth(), button.getHeight()) * 1.5),
                        new KeyValue(ripple.opacityProperty(), 0)
                )
        );

        rippleAnimation.setOnFinished(e -> {
            if (buttonContainer.getChildren().contains(ripple)) {
                buttonContainer.getChildren().remove(ripple);
            }
        });

        rippleAnimation.play();
    }

    private void setActiveButton(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("liquid-btn-active");
            resetButtonScale(activeButton);
        }

        activeButton = button;
        button.getStyleClass().add("liquid-btn-active");

        // Add active state animation
        Timeline activeAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(button.scaleXProperty(), 1),
                        new KeyValue(button.scaleYProperty(), 1)
                ),
                new KeyFrame(Duration.millis(200),
                        new KeyValue(button.scaleXProperty(), 1.05),
                        new KeyValue(button.scaleYProperty(), 1.05),
                        new KeyValue(button.translateXProperty(), 4)
                )
        );
        activeAnimation.setCycleCount(2);
        activeAnimation.setAutoReverse(true);
        activeAnimation.play();
    }

    // =========================
    // MISSION NAVIGATION
    // =========================

    @FXML
    public void showAddMission() {
        loadViewWithAnimation("mission-add.fxml");
        setActiveButton(btnAddMission);
    }

    @FXML
    public void showMissionList() {
        loadViewWithAnimation("mission-list.fxml");
        setActiveButton(btnMissionList);
    }

    public void showEditMission(Mission mission) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/exemple/grecrutement/mission-edit.fxml")
            );
            Node view = loader.load();

            MissionEditController controller = loader.getController();
            controller.setMission(mission);

            animateContentChange(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // RENDU NAVIGATION
    // =========================

    @FXML
    public void showRenduAdd() {
        showRenduAddWithMissionId(null);
        setActiveButton(btnSubmitRendu);
    }

    public void showRenduAddWithMissionId(Integer missionId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/exemple/grecrutement/rendu-add.fxml")
            );
            Node view = loader.load();

            RenduAddController controller = loader.getController();
            if (missionId != null) {
                controller.setMissionId(missionId);
            }

            animateContentChange(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Show interview scheduling screen for a selected rendu/candidate
     */
    public void showScheduleInterview(RenduMission rendu) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/exemple/grecrutement/entretien-create.fxml")
            );
            Node view = loader.load();

            EntretienCreateController controller = loader.getController();
            controller.setRenduMission(rendu);

            animateContentChange(view);

            System.out.println("✅ Interview scheduling screen loaded for Rendu #" + rendu.getId());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error loading interview scheduling screen: " + e.getMessage());
        }
    }

    @FXML
    public void showRenduList() {
        loadViewWithAnimation("rendu-list.fxml");
        setActiveButton(btnRenduList);
    }

    @FXML
    public void showStatistics() {
        loadViewWithAnimation("rendu-stats.fxml");
    }

    // =========================
    // CORE LOADER WITH ANIMATION
    // =========================

    private void loadViewWithAnimation(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/exemple/grecrutement/" + fxml)
            );
            Node view = loader.load();
            animateContentChange(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void animateContentChange(Node newView) {
        // Fade out current content
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), contentPane);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        fadeOut.setOnFinished(e -> {
            // Change content
            contentPane.getChildren().setAll(newView);

            // Fade in new content
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), contentPane);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });

        fadeOut.play();
    }

    // =========================
    // UTILITY METHODS
    // =========================

    public void setUserName(String name) {
        if (navNameLabel != null) {
            navNameLabel.setText(name);
        }
    }

    public void setUserAvatar(Color color) {
        if (navAvatar != null) {
            navAvatar.setFill(color);
        }
    }
}