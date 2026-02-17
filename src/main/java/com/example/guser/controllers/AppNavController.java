package com.example.guser.controllers;

import com.example.guser.SceneManager;
import entities.User;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import java.time.Duration;
import session.ProfileViewContext;
import session.SessionContext;
import utils.S3StorageService;

import java.io.InputStream;
import java.time.temporal.ChronoUnit;

public class AppNavController {

    private static final String S3_BUCKET = "carrieri-storage-dev-islem";
    private static final String S3_REGION = "eu-west-3";

    public enum Route { PROFILE, PEOPLE, FEED, JOBS, CONNECT }

    @FXML private StackPane contentPane;

    @FXML private Button btnFeed;
    @FXML private Button btnJobs;
    @FXML private Button btnConnect;
    @FXML private Button btnUserProfile;
    @FXML private Button logoutBtn;

    @FXML private Circle navAvatar;
    @FXML private Label navNameLabel;
    @FXML private Label navRoleLabel;

    @FXML private ImageView brandLogo;

    private Button activeButton;

    @FXML
    public void initialize() {
        loadLogoSafe("/com/example/guser/images/logo.png", "/images/logo.png");
        hydrateUserSection();


        // Default page
        //showAddMission();
        //setActive(btnAddMission, Route.ADD_MISSION);
        setActive(btnUserProfile, Route.PROFILE);

        // nice fade in (nahit l content pane fel navbar.fxml raditha just VBOX DONC HEDHI TAAMEL ERREUR
        /*contentPane.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(250), contentPane);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();*/
    }


    @FXML
    private void onProfile() {
        if (!SessionContext.isLoggedIn()) return;
        int meId = SessionContext.getCurrentUser().getId();
        ProfileViewContext.viewUser(meId);
        SceneManager.switchTo("/com/example/guser/profile.fxml", "Carrieri • Sign in");
        setActive(btnUserProfile, Route.PROFILE);
    }

    @FXML
    private void onLogout() {
        SessionContext.setCurrentUser(null);
        ProfileViewContext.viewUser(null);
        // If you use SceneManager, call it here; otherwise load login into contentPane
        SceneManager.switchTo("/com/example/guser/login.fxml", "Carrieri • Sign in");
        clearActive();
        hydrateUserSection();
    }


    // ========== ACTIVE STATE ==========
    void setActive(Button btn, Route route) {
        clearActive();
        activeButton = btn;
        if (activeButton != null) activeButton.getStyleClass().add("nav-link-active");
    }

    private void clearActive() {
        if (activeButton != null) activeButton.getStyleClass().remove("nav-link-active");
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
        btnUserProfile.setDisable(false);
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
            String url = s3.presignedGetUrl(s3Key, Duration.ofMinutes(10));
            Image img = new Image(url, false);
            if (!img.isError()) circle.setFill(new ImagePattern(img));
        } catch (Exception ignored) {}
    }

    @FXML
    private void onPeople() {
        // later: switch to candidate directory / recruiter directory
        SceneManager.switchTo("/com/example/guser/profile.fxml", "Carrieri • Profile");
    }

    @FXML
    private void onJobs() {
        // placeholder
        setActive(btnJobs, Route.JOBS);
    }
    @FXML
    private void onFeed() {
        // placeholder
        setActive(btnFeed, Route.FEED);
    }
    @FXML
    private void onConnect() {
        // placeholder
        setActive(btnConnect, Route.CONNECT);
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
        } catch (Exception ignored) { }
        // fallback: no image, keep empty (or set a default placeholder in CSS)
        brandLogo.setImage(null);
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
