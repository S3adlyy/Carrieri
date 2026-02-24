package com.example.guser.controllers;

import com.example.guser.SceneManager;
import entities.User;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.layout.VBox;
import session.ProfileViewContext;
import session.SessionContext;
import utils.S3StorageService;

import java.io.InputStream;
import java.time.Duration; // keep java.time.Duration ONLY

public class AppNavController {

    private static final String S3_BUCKET = "carrieri-storage-dev-islem";
    private static final String S3_REGION = "eu-west-3";

    public enum Route { PROFILE, PEOPLE, FEED, JOBS, CONNECT, ADMIN }

    @FXML private Button btnFeed;
    @FXML private Button btnJobs;
    @FXML private Button btnConnect;
    @FXML private Button btnUserProfile;
    @FXML private Button btnAdmin;
    @FXML private Button logoutBtn;

    @FXML private Circle navAvatar;
    @FXML private Label navNameLabel;
    @FXML private Label navRoleLabel;

    @FXML private ImageView brandLogo;

    @FXML private VBox sidebar;

    private Timeline expandAnimation;
    private Timeline collapseAnimation;
    private Button activeButton;
    private static final double COLLAPSED_W = 70;
    private static final double EXPANDED_W = 250;

    private Timeline widthAnim;
    private PauseTransition hoverDebounce;

    @FXML
    public void initialize() {
        loadLogoSafe("/com/example/guser/images/logo.png", "/images/logo.png");
        hydrateUserSection();

        setupAnimations();
        animateSidebarTo(COLLAPSED_W);


        if(SessionContext.getCurrentUser().getRoles().equals("ADMIN"))
            setActive(btnAdmin, Route.ADMIN);
        else
            setActive(btnUserProfile, Route.PROFILE);
    }

    // ========== ROUTES ==========
    @FXML
    private void onProfile() {
        if (!SessionContext.isLoggedIn()) return;
        int meId = SessionContext.getCurrentUser().getId();
        ProfileViewContext.viewUser(meId);
        SceneManager.switchTo("/com/example/guser/profile.fxml", "Carrieri • Profile");
        setActive(btnUserProfile, Route.PROFILE);
    }

    @FXML
    private void onLogout() {
        SessionContext.setCurrentUser(null);
        ProfileViewContext.viewUser(null);
        SceneManager.switchTo("/com/example/guser/login.fxml", "Carrieri • Sign in");
        clearActive();
        hydrateUserSection();
    }

    @FXML
    private void onJobs() {
        setActive(btnJobs, Route.JOBS);
        // SceneManager.switchTo("...", "Carrieri • Jobs");
    }

    @FXML
    private void onFeed() {
        setActive(btnFeed, Route.FEED);
        // SceneManager.switchTo("...", "Carrieri • Feed");
    }
    @FXML
    private void onAdmin() {
        setActive(btnAdmin, Route.ADMIN);
        SceneManager.switchTo("...", "Carrieri • Admin");
    }

    @FXML
    private void onConnect() {
        SceneManager.switchTo("/com/example/guser/connect.fxml", "Carrieri • Connect");
        setActive(btnConnect, Route.CONNECT);
    }

    // ========== ACTIVE STATE ==========
    void setActive(Button btn, Route route) {
        clearActive();
        activeButton = btn;
        if (activeButton != null) activeButton.getStyleClass().add("nav-button-active");
    }

    private void clearActive() {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-button-active");
            activeButton.getStyleClass().remove("nav-link-active"); // backward compat if any
        }
        activeButton = null;
    }

    // ========== USER SECTION ==========
    private void hydrateUserSection() {
        if (!SessionContext.isLoggedIn()) {
            navNameLabel.setText("Guest");
            navRoleLabel.setText("Not signed in");
            navAvatar.setFill(Color.web("#D1D5DB"));
            btnUserProfile.setDisable(true);
            logoutBtn.setDisable(true);
            return;
        }

        User me = SessionContext.getCurrentUser();
        if(btnUserProfile!=null)
            btnUserProfile.setDisable(false);
        else
            btnAdmin.setDisable(true);
        logoutBtn.setDisable(false);

        String fullName = (safe(me.getFirstname()) + " " + safe(me.getLastname())).trim();
        navNameLabel.setText(fullName.isBlank() ? "User" : fullName);
        navRoleLabel.setText(safe(me.getRoles()).isBlank() ? "Member" : me.getRoles());

        String key = "RECRUITER".equals(me.getRoles()) ? me.getLogourl() : me.getProfilepic();
        Platform.runLater(() -> renderCircleFromS3Key(navAvatar, key));
    }

    private void renderCircleFromS3Key(Circle circle, String s3Key) {
        try (S3StorageService s3 = new S3StorageService(S3_BUCKET, S3_REGION)) {
            if (s3Key == null || s3Key.isBlank()) return;
            String url = s3.presignedGetUrl(s3Key, Duration.ofMinutes(10)); // java.time.Duration
            Image img = new Image(url, false);
            if (!img.isError()) circle.setFill(new ImagePattern(img));
        } catch (Exception ignored) {}
    }

    private void setupAnimations() {
        widthAnim = new Timeline();
        hoverDebounce = new PauseTransition(javafx.util.Duration.millis(60));
    }

    private void animateSidebarTo(double targetW) {
        double current = sidebar.getWidth(); // real current width (smoother than prefWidth)

        if (Math.abs(current - targetW) < 0.5) return;

        widthAnim.stop();
        widthAnim.getKeyFrames().setAll(
                new KeyFrame(javafx.util.Duration.millis(220),
                        new KeyValue(sidebar.prefWidthProperty(), targetW, Interpolator.EASE_OUT),
                        new KeyValue(sidebar.minWidthProperty(),  targetW, Interpolator.EASE_OUT),
                        new KeyValue(sidebar.maxWidthProperty(),  targetW, Interpolator.EASE_OUT)
                )
        );
        widthAnim.playFromStart();
    }

    @FXML
    public void expandSidebar() {
        hoverDebounce.stop();
        hoverDebounce.setOnFinished(e -> animateSidebarTo(EXPANDED_W));
        hoverDebounce.playFromStart();
    }

    @FXML
    public void collapseSidebar() {
        hoverDebounce.stop();
        hoverDebounce.setOnFinished(e -> animateSidebarTo(COLLAPSED_W));
        hoverDebounce.playFromStart();
    }

    // ========== SAFE RESOURCE LOADING ==========
    private void loadLogoSafe(String... paths) {
        try {
            for (String p : paths) {
                try (InputStream is = getClass().getResourceAsStream(p)) {
                    if (is != null) {
                        brandLogo.setImage(new Image(is));
                        return;
                    }
                }
            }
        } catch (Exception ignored) {}
        brandLogo.setImage(null);
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
