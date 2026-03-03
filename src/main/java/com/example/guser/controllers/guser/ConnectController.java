package com.example.guser.controllers.guser;

import com.example.guser.SceneManager;
import entities.guser.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import services.guser.ProfileService;
import session.ProfileViewContext;
import session.SessionContext;
import utils.guser.S3StorageService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConnectController {

    private static final String S3_BUCKET = "carrieri-storage-dev-islem";
    private static final String S3_REGION = "eu-west-3";


    private final ProfileService profileService = new ProfileService();

    @FXML private TextField searchNameField;
    @FXML private TextField searchLocationField;
    @FXML private ComboBox<String> roleFilterBox;

    @FXML private VBox resultsBox;
    @FXML private Label resultsCountLabel;

    // optional navbar include
    @FXML private AppNavController appNavController;

    private List<User> allSuggestions = new ArrayList<>();

    @FXML
    public void initialize() {
        if (!SessionContext.isLoggedIn()) {
            AppNavController.getInstance().show("/com/example/guser/guser/login.fxml", "Carrieri • Profile");
            return;
        }


        if (roleFilterBox != null && roleFilterBox.getSelectionModel().isEmpty()) {
            roleFilterBox.getSelectionModel().select("Any");
        }

        loadSuggestions();
        installLiveFilters();
    }

    private void loadSuggestions() {
        resultsBox.getChildren().clear();
        resultsCountLabel.setText("");

        try {
            User me = SessionContext.getCurrentUser();
            List<User> suggestions = profileService.suggestPeopleYouMayKnow(
                    me.getId(),
                    me.getRoles()
            );
            // store full set for client-side filtering
            allSuggestions = new ArrayList<>();
            for (User u : suggestions) {
                if (u.getId() == me.getId()) continue;
                allSuggestions.add(u);
            }

            applyFilters();

        } catch (Exception e) {
            Label err = new Label("Could not load recommendations.");
            err.getStyleClass().add("prf-muted");
            resultsBox.getChildren().add(err);
        }
    }

    private void installLiveFilters() {
        if (searchNameField != null) {
            searchNameField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        }
        if (searchLocationField != null) {
            searchLocationField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        }
        if (roleFilterBox != null) {
            roleFilterBox.valueProperty().addListener((obs, oldV, newV) -> applyFilters());
        }
    }

    private void applyFilters() {
        if (resultsBox == null) return;
        resultsBox.getChildren().clear();

        String nameQuery = safe(searchNameField != null ? searchNameField.getText() : "").toLowerCase(Locale.ROOT);
        String locQuery  = safe(searchLocationField != null ? searchLocationField.getText() : "").toLowerCase(Locale.ROOT);
        String roleFilter = roleFilterBox != null && roleFilterBox.getValue() != null
                ? roleFilterBox.getValue()
                : "Any";

        List<User> filtered = new ArrayList<>();
        for (User u : allSuggestions) {
            if (!nameQuery.isBlank()) {
                String fullName = (safe(u.getFirstname()) + " " + safe(u.getLastname())).toLowerCase(Locale.ROOT);
                if (!fullName.contains(nameQuery)) continue;
            }
            if (!locQuery.isBlank()) {
                String loc = safe(u.getLocation()).toLowerCase(Locale.ROOT);
                if (!loc.contains(locQuery)) continue;
            }
            if (!"Any".equalsIgnoreCase(roleFilter)) {
                if ("Candidate".equalsIgnoreCase(roleFilter) && !"CANDIDATE".equals(u.getRoles())) continue;
                if ("Recruiter".equalsIgnoreCase(roleFilter) && !"RECRUITER".equals(u.getRoles())) continue;
            }
            filtered.add(u);
        }

        if (filtered.isEmpty()) {
            Label none = new Label("No profiles match your filters yet.");
            none.getStyleClass().add("prf-muted");
            resultsBox.getChildren().add(none);
            resultsCountLabel.setText("0 results");
            return;
        }

        for (User u : filtered) {
            resultsBox.getChildren().add(buildPersonRow(u));
        }
        resultsCountLabel.setText(filtered.size() + " results");
    }

    private Node buildPersonRow(User u) {
        HBox row = new HBox(10);
        row.getStyleClass().add("connect-person-card");

        Circle avatar = new Circle(18);
        avatar.getStyleClass().add("connect-avatar");
        avatar.setFill(javafx.scene.paint.Paint.valueOf("rgba(0,0,0,0.08)"));
        String key = "RECRUITER".equals(u.getRoles())
                ? safe(u.getLogourl())
                : safe(u.getProfilepic());
        Platform.runLater(() -> renderCircleFromS3Key(avatar, key));

        Label name = new Label((safe(u.getFirstname()) + " " + safe(u.getLastname())).trim());
        name.getStyleClass().add("prf-personName");

        String sub = "RECRUITER".equals(u.getRoles())
                ? safe(u.getOrgname())
                : safe(u.getHeadline());
        Label subLabel = new Label(sub.isBlank() ? safe(u.getRoles()) : sub);
        subLabel.getStyleClass().add("prf-personSub");

        Label meta = new Label(safe(u.getLocation()));
        meta.getStyleClass().add("prf-personMeta");

        VBox text = new VBox(2, name, subLabel, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button viewBtn = new Button("View");
        viewBtn.getStyleClass().add("connect-view");
        viewBtn.setOnAction(e -> openProfile(u.getId()));

        Button connectBtn = new Button("Connect");
        connectBtn.getStyleClass().add("connect-action");
        connectBtn.setOnAction(e -> {
            // TODO: call service to send connection request
            connectBtn.setText("Requested");
            connectBtn.setDisable(true);
        });

        row.setOnMouseClicked(e -> openProfile(u.getId()));

        // prevent row click from firing when pressing buttons
        viewBtn.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> e.consume());
        connectBtn.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> e.consume());

        row.getChildren().addAll(avatar, text, spacer, viewBtn, connectBtn);
        return row;
    }

    private void openProfile(int userId) {
        ProfileViewContext.viewUser(userId);
        AppNavController.getInstance().show("/com/example/guser/guser/profile.fxml", "Carrieri • Profile");

    }

    private void renderCircleFromS3Key(Circle circle, String s3Key) {
        try (S3StorageService s3 = new S3StorageService(S3_BUCKET, S3_REGION)) {
            if (s3Key == null || s3Key.isBlank()) return;
            String url = s3.presignedGetUrl(s3Key, Duration.ofMinutes(10));
            ImagePattern pattern = new ImagePattern(new javafx.scene.image.Image(url, false));
            if (!pattern.getImage().isError()) {
                circle.setFill(pattern);
            }
        } catch (Exception ignored) { }
    }

    // === Handlers from FXML ===
    @FXML
    private void onClearFilters() {
        if (searchNameField != null) searchNameField.clear();
        if (searchLocationField != null) searchLocationField.clear();
        if (roleFilterBox != null) roleFilterBox.getSelectionModel().select("Any");
        applyFilters();
    }

    @FXML
    private void onRefresh() {
        loadSuggestions();
    }

    // === Helpers ===
    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
