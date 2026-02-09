package com.exemple.grecrutement;

import entities.Mission;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import java.util.HashMap;
import java.util.Map;

public class MissionShellController {

    private static MissionShellController instance;
    private Map<Button, Timeline> buttonAnimations = new HashMap<>();
    private Button activeButton = null;

    @FXML
    private StackPane contentPane;

    @FXML
    private HBox navbarContainer;

    @FXML
    private Button btnAddMission;

    @FXML
    private Button btnMissionList;

    @FXML
    private Button btnSubmitRendu;

    @FXML
    private Button btnRenduList;

    public MissionShellController() {
        instance = this;
    }

    public static MissionShellController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        setupLiquidAnimations();
        setupButtonHoverEffects();
        showAddMission();
        setActiveButton(btnAddMission);
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
                            new KeyValue(button.effectProperty(), null)
                    ),
                    new KeyFrame(Duration.millis(200),
                            new KeyValue(button.scaleXProperty(), 1.08),
                            new KeyValue(button.scaleYProperty(), 1.08),
                            new KeyValue(button.effectProperty(),
                                    new DropShadow(BlurType.GAUSSIAN,
                                            Color.rgb(106, 17, 203, 0.6),
                                            25, 0.25, 0, 8))
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
                if (button != activeButton && liquidHover.getStatus() == Animation.Status.RUNNING) {
                    liquidHover.stop();
                    resetButtonScale(button);
                }
            });

            // Add click ripple effect
            button.setOnMousePressed(e -> createLiquidRipple(button, e));
        }
    }

    private void setupButtonHoverEffects() {
        // Add subtle floating animation to all buttons
        Button[] buttons = {btnAddMission, btnMissionList, btnSubmitRendu, btnRenduList};

        for (Button button : buttons) {
            Timeline floatAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(button.translateYProperty(), 0)
                    ),
                    new KeyFrame(Duration.millis(2000),
                            new KeyValue(button.translateYProperty(), -2)
                    ),
                    new KeyFrame(Duration.millis(4000),
                            new KeyValue(button.translateYProperty(), 0)
                    )
            );

            floatAnimation.setCycleCount(Timeline.INDEFINITE);
            floatAnimation.setAutoReverse(true);
            floatAnimation.play();
        }
    }

    private void resetButtonScale(Button button) {
        Timeline reset = new Timeline(
                new KeyFrame(Duration.millis(150),
                        new KeyValue(button.scaleXProperty(), 1),
                        new KeyValue(button.scaleYProperty(), 1)
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

        StackPane buttonPane = (StackPane) button.getParent();
        buttonPane.getChildren().add(ripple);

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
            if (buttonPane.getChildren().contains(ripple)) {
                buttonPane.getChildren().remove(ripple);
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
                        new KeyValue(button.scaleXProperty(), 1.05),
                        new KeyValue(button.scaleYProperty(), 1.05)
                )
        );
        activeAnimation.setCycleCount(1);
        activeAnimation.play();
    }

    // =========================
    // MISSION NAVIGATION
    // =========================

    @FXML
    public void showAddMission() {
        loadView("mission-add.fxml");
        setActiveButton(btnAddMission);
    }

    @FXML
    public void showMissionList() {
        loadView("mission-list.fxml");
        setActiveButton(btnMissionList);
    }

    public void showEditMission(Mission mission) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("mission-edit.fxml")
            );
            Node view = loader.load();

            MissionEditController controller = loader.getController();
            controller.setMission(mission);

            contentPane.getChildren().setAll(view);
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

    @FXML
    public void showStatistics() {
        loadView("rendu-stats.fxml");
        // Note: You don't have a stats button in navbar, so we won't set active button
        // Or you can create one if needed
    }

    public void showRenduAddWithMissionId(Integer missionId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("rendu-add.fxml")
            );
            Node view = loader.load();

            RenduAddController controller = loader.getController();
            if (missionId != null) {
                controller.setMissionId(missionId);
            }

            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showRenduList() {
        loadView("rendu-list.fxml");
        setActiveButton(btnRenduList);
    }

    // =========================
    // CORE LOADER
    // =========================

    private void loadView(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Node view = loader.load();
            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}