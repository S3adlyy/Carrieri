package com.example.guser.controllers;

import com.example.guser.SceneManager;
import entities.User;
import javafx.scene.image.ImageView;
import services.UserService;
import utils.AlertUtils;
import utils.FileStorage;

import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.util.Duration;

import java.io.File;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;


import utils.S3KeyUtil;
import utils.S3StorageService;


public class SignupController {

    private static final PseudoClass ERROR_PC = PseudoClass.getPseudoClass("error");

    private static final Duration DUR_IN = Duration.millis(240);
    private static final Duration DUR_OUT = Duration.millis(180);

    private static final List<String> SKILL_CATALOG = Arrays.asList(
            "Java", "JavaFX", "Spring", "Spring Boot", "SQL", "MySQL", "PostgreSQL", "UML",
            "Git", "GitHub", "Docker", "Linux", "REST API", "JUnit", "Maven",
            "JavaScript", "TypeScript", "Node.js", "Python", "Django", "Flask",
            "HTML", "CSS", "React", "Angular",
            "Cybersecurity", "Ethical Hacking", "OWASP", "Networking",
            "Digital Marketing", "SEO", "Google Ads", "Meta Ads", "Content Marketing",
            "Project Management", "Scrum", "Communication", "Leadership", "Teamwork"
    );

    @FXML private ComboBox<String> roleCombo;
    @FXML private Label passwordStrengthLabel; // create in FXML near password

    private static final String WEAK = "Weak";
    private static final String MEDIUM = "Medium";
    private static final String STRONG = "Strong";

    // Simple patterns
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[\\p{L} .'-]{2,40}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0-9 +()\\-]{6,20}$");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);


    @FXML private TextField emailField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    // Steps
    @FXML private VBox step1Box;
    @FXML private VBox step2CandidateBox;
    @FXML private VBox step2RecruiterBox;
    @FXML private VBox step3CandidateBox;
    @FXML private VBox step3RecruiterBox;

    // Header
    @FXML private ProgressBar progressBar;
    @FXML private Text stepSubtitle;

    // Buttons
    @FXML private Button backBtn;
    @FXML private Button nextBtn;

    // Errors
    @FXML private Label errorLabel;
    @FXML private Label errorLabel2;
    @FXML private Label errorLabel3;
    @FXML private Label errorLabel4;
    @FXML private Label errorLabel5;

    // Candidate step2
    @FXML private TextField candidateLocationField;
    @FXML private TextField candidatePhoneField;
    @FXML private TextField schoolField;
    @FXML private TextField degreeField;
    @FXML private TextField fieldOfStudyField;
    @FXML private TextField graduationYearField;

    // Skills
    @FXML private TextField skillInputField;
    @FXML private ListView<String> skillSuggestList;
    @FXML private FlowPane skillsPane;

    @FXML private TextField githubField;
    @FXML private TextField portfolioField;

    // Recruiter step2
    @FXML private TextField recruiterLocationField;
    @FXML private TextField recruiterPhoneField;
    @FXML private TextField orgNameField;
    @FXML private TextField websiteField;
    @FXML private TextArea recruiterDescArea;

    // Candidate step3
    @FXML private TextField headlineField;
    @FXML private TextArea bioArea;
    @FXML private Label profilePicLabel;

    // Recruiter step3
    @FXML private Label logoLabel;

    // Live preview
    @FXML private VBox previewCard;
    @FXML private Circle previewAvatarCircle;
    @FXML private Label previewNameLabel;
    @FXML private Label previewRoleLabel;
    @FXML private Label previewPrimaryLabel;
    @FXML private Label previewLocationLabel;

    @FXML private Label previewAboutTitleLabel;
    @FXML private Label previewAboutLabel;

    @FXML private VBox previewEduBox;
    @FXML private Label previewEducationLabel;

    @FXML private VBox previewSkillsBox;
    @FXML private FlowPane previewSkillsPane;

    @FXML private Label previewLinksLabel;
    @FXML private Label previewPhoneLabel;

    private final UserService userService = new UserService();

    private int step = 1;

    private File pickedProfilePicFile = null;
    private File pickedLogoFile = null;

    // S3 config (you can move to a config file later)
    private static final String S3_BUCKET = "carrieri-storage-dev-islem";
    private static final String S3_REGION = "eu-west-3";

    @FXML private ImageView brandLogo;
    private final LinkedHashSet<String> skills = new LinkedHashSet<>();

    // Animation helpers
    private Animation previewAmbient;
    private PauseTransition previewNudgeDebounce;

    @FXML
    public void initialize() {
        roleCombo.getItems().setAll("CANDIDATE", "RECRUITER");
        roleCombo.getSelectionModel().select("CANDIDATE");

        setupAutocomplete();
        setupLivePreviewListeners();
        setupInlineClearOnTyping();
        setupAnimations();

        applyRoleVisibility();
        showStep(1, true);
        hideAllErrors();
        loadLogoSafe("/com/example/guser/images/logo.png", "/images/logo.png");
        updatePasswordStrengthUi();
        refreshPreview(true);
    }

    @FXML
    private void onRoleChange() {
        applyRoleVisibility();
        showStep(step, true);
        hideAllErrors();
        refreshPreview(true);
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
    // -------------------------
    // Wizard + Step animations
    // -------------------------
    private void applyRoleVisibility() {
        boolean isCandidate = "CANDIDATE".equals(roleCombo.getValue());

        step2CandidateBox.setVisible(isCandidate);
        step2CandidateBox.setManaged(isCandidate);
        step3CandidateBox.setVisible(isCandidate);
        step3CandidateBox.setManaged(isCandidate);

        step2RecruiterBox.setVisible(!isCandidate);
        step2RecruiterBox.setManaged(!isCandidate);
        step3RecruiterBox.setVisible(!isCandidate);
        step3RecruiterBox.setManaged(!isCandidate);
    }

    private VBox getActiveStepBox(int s) {
        boolean isCandidate = "CANDIDATE".equals(roleCombo.getValue());
        if (s == 1) return step1Box;
        if (s == 2) return isCandidate ? step2CandidateBox : step2RecruiterBox;
        return isCandidate ? step3CandidateBox : step3RecruiterBox;
    }

    private void showStep(int s, boolean animate) {
        step = s;

        setVisible(step1Box, false);
        setVisible(step2CandidateBox, false);
        setVisible(step2RecruiterBox, false);
        setVisible(step3CandidateBox, false);
        setVisible(step3RecruiterBox, false);

        double targetProgress;
        if (step == 1) {
            stepSubtitle.setText("Step 1 of 3 • Account");
            targetProgress = 0.33;
            backBtn.setDisable(true);
            nextBtn.setText("Next");
        } else if (step == 2) {
            stepSubtitle.setText("Step 2 of 3 • Profile");
            targetProgress = 0.66;
            backBtn.setDisable(false);
            nextBtn.setText("Next");
        } else {
            stepSubtitle.setText("Step 3 of 3 • Finish");
            targetProgress = 1.0;
            backBtn.setDisable(false);
            nextBtn.setText("Create account");
        }

        animateProgressTo(targetProgress);

        VBox active = getActiveStepBox(step);
        setVisible(active, true);
        if (animate) playEnter(active);
    }

    private void animateProgressTo(double target) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.millis(220),
                        new KeyValue(progressBar.progressProperty(), target, Interpolator.EASE_BOTH))
        );
        t.play();
    }

    private void playEnter(Node node) {
        node.setOpacity(0);
        node.setTranslateY(10);
        node.setScaleX(0.985);
        node.setScaleY(0.985);

        FadeTransition ft = new FadeTransition(DUR_IN, node);
        ft.setFromValue(0);
        ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(DUR_IN, node);
        tt.setFromY(10);
        tt.setToY(0);

        ScaleTransition st = new ScaleTransition(DUR_IN, node);
        st.setFromX(0.985);
        st.setFromY(0.985);
        st.setToX(1);
        st.setToY(1);

        ParallelTransition pt = new ParallelTransition(ft, tt, st);
        pt.setInterpolator(Interpolator.EASE_OUT);
        pt.play();
    }

    // -------------------------
    // Wizard buttons
    // -------------------------
    @FXML
    private void onNext() {
        hideAllErrors();
        clearAllErrorBorders();

        try {
            if (step == 1) {
                if (validateStep1()) showStep(2, true);
            } else if (step == 2) {
                if (validateStep2()) showStep(3, true);
            } else {
                if (validateStep3()) createAccount();
            }
        } catch (Exception e) {
            showErrorForCurrentStep(e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onBack() {
        hideAllErrors();
        clearAllErrorBorders();
        if (step > 1) showStep(step - 1, true);
    }

    @FXML
    private void onGoLogin() {
        SceneManager.switchTo("/com/example/guser/login.fxml", "Carrieri • Sign in");
    }

    // -------------------------
    // Autocomplete + Chips (animated)
    // -------------------------
    private void setupAutocomplete() {
        skillSuggestList.setVisible(false);
        skillSuggestList.setManaged(false);

        skillInputField.textProperty().addListener((obs, oldV, newV) -> {
            refreshSkillSuggestions(newV);
            refreshPreview(false);
        });

        skillSuggestList.setOnMouseClicked(e -> {
            String selected = skillSuggestList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                addSkill(selected);
                skillInputField.clear();
                hideSuggestions();
                refreshPreview(true);
            }
        });

        skillInputField.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) hideSuggestions();
        });
    }

    private void refreshSkillSuggestions(String typed) {
        String q = typed == null ? "" : typed.trim().toLowerCase();
        if (q.isEmpty()) { hideSuggestions(); return; }

        List<String> filtered = SKILL_CATALOG.stream()
                .filter(s -> s.toLowerCase().contains(q))
                .filter(s -> !skills.contains(normalizeSkill(s)))
                .limit(6)
                .collect(Collectors.toList());

        if (filtered.isEmpty()) hideSuggestions();
        else {
            skillSuggestList.setItems(FXCollections.observableArrayList(filtered));
            skillSuggestList.setVisible(true);
            skillSuggestList.setManaged(true);
        }
    }

    private void hideSuggestions() {
        skillSuggestList.setVisible(false);
        skillSuggestList.setManaged(false);
        skillSuggestList.getItems().clear();
    }

    @FXML
    private void onAddSkillFromInput() {
        String raw = skillInputField.getText();
        if (raw == null) return;
        String value = raw.trim();
        if (value.isEmpty()) return;

        addSkill(value);
        skillInputField.clear();
        hideSuggestions();
        refreshPreview(true);
    }

    private void addSkill(String value) {
        String normalized = normalizeSkill(value);
        if (normalized.isEmpty()) return;
        if (!skills.add(normalized)) return;

        HBox chip = createRemovableChip(normalized);
        chip.setOpacity(0);
        chip.setScaleX(0.9);
        chip.setScaleY(0.9);

        skillsPane.getChildren().add(chip);
        playChipIn(chip);
    }

    private HBox createRemovableChip(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("chip-label");

        Button x = new Button("x");
        x.getStyleClass().add("chip-x");

        HBox chip = new HBox(lbl, x);
        chip.getStyleClass().add("chip");

        x.setOnAction(e -> playChipOut(chip, () -> {
            skills.remove(text);
            skillsPane.getChildren().remove(chip);
            refreshPreview(true);
        }));

        return chip;
    }

    private void playChipIn(Node n) {
        FadeTransition ft = new FadeTransition(Duration.millis(160), n);
        ft.setFromValue(0);
        ft.setToValue(1);

        ScaleTransition st = new ScaleTransition(Duration.millis(160), n);
        st.setFromX(0.9);
        st.setFromY(0.9);
        st.setToX(1);
        st.setToY(1);

        ParallelTransition pt = new ParallelTransition(ft, st);
        pt.setInterpolator(Interpolator.EASE_OUT);
        pt.play();
    }

    private void playChipOut(Node n, Runnable after) {
        FadeTransition ft = new FadeTransition(DUR_OUT, n);
        ft.setFromValue(n.getOpacity());
        ft.setToValue(0);

        ScaleTransition st = new ScaleTransition(DUR_OUT, n);
        st.setFromX(n.getScaleX());
        st.setFromY(n.getScaleY());
        st.setToX(0.85);
        st.setToY(0.85);

        ParallelTransition pt = new ParallelTransition(ft, st);
        pt.setInterpolator(Interpolator.EASE_IN);
        pt.setOnFinished(e -> after.run());
        pt.play();
    }

    private static String normalizeSkill(String s) {
        String t = s.trim();
        if (t.isEmpty()) return "";
        String[] parts = t.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1));
            if (i < parts.length - 1) sb.append(" ");
        }
        return sb.toString();
    }

    private HBox createPreviewChip(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("chip-label");
        HBox chip = new HBox(lbl);
        chip.getStyleClass().add("chip");
        return chip;
    }
    private String evaluatePasswordStrength(String pass) {
        if (pass == null) pass = "";
        int score = 0;
        if (pass.length() >= 8) score++;
        if (pass.length() >= 12) score++;
        if (pass.matches(".*[a-z].*")) score++;
        if (pass.matches(".*[A-Z].*")) score++;
        if (pass.matches(".*\\d.*")) score++;
// at least one non-alphanumeric character
        if (pass.matches(".*[^A-Za-z0-9].*")) score++;

        if (score <= 2) return WEAK;
        if (score <= 4) return MEDIUM;
        return STRONG;
    }

    private void updatePasswordStrengthUi() {
        String pass = passwordField.getText();
        String strength = evaluatePasswordStrength(pass);

        if (isBlank(pass)) {
            passwordStrengthLabel.setText("");
            passwordStrengthLabel.getStyleClass().removeAll("pw-weak", "pw-medium", "pw-strong");
            return;
        }

        passwordStrengthLabel.setText("Password strength: " + strength);

        passwordStrengthLabel.getStyleClass().removeAll("pw-weak", "pw-medium", "pw-strong");
        switch (strength) {
            case WEAK -> passwordStrengthLabel.getStyleClass().add("pw-weak");
            case MEDIUM -> passwordStrengthLabel.getStyleClass().add("pw-medium");
            case STRONG -> passwordStrengthLabel.getStyleClass().add("pw-strong");
        }
    }


    // -------------------------
    // Pick images (optional)
    // -------------------------
    @FXML
    private void onPickProfilePic() {
        try {
            File file = pickImageFile("Choose profile picture");
            if (file == null) return;

            pickedProfilePicFile = file;
            profilePicLabel.setText(file.getName());
            refreshPreview(true); // still previews locally
        } catch (Exception e) {
            showErrorForCurrentStep("Image error: " + e.getMessage());
        }
    }


    @FXML
    private void onPickLogo() {
        try {
            File file = pickImageFile("Choose organization logo");
            if (file == null) return;

            pickedLogoFile = file;
            logoLabel.setText(file.getName());
            refreshPreview(true); // still previews locally
        } catch (Exception e) {
            showErrorForCurrentStep("Image error: " + e.getMessage());
        }
    }

    private File pickImageFile(String title) {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );
        return fc.showOpenDialog(nextBtn.getScene().getWindow());
    }

    // -------------------------
    // Live preview (with animation nudge)
    // -------------------------
    private void setupLivePreviewListeners() {
        roleCombo.valueProperty().addListener((obs, o, n) -> refreshPreview(true));

        firstNameField.textProperty().addListener((obs, o, n) -> refreshPreview(false));
        lastNameField.textProperty().addListener((obs, o, n) -> refreshPreview(false));

        // Candidate
        candidateLocationField.textProperty().addListener((obs, o, n) -> refreshPreview(false));
        candidatePhoneField.textProperty().addListener((obs, o, n) -> refreshPreview(false));
        schoolField.textProperty().addListener((obs, o, n) -> refreshPreview(false));
        degreeField.textProperty().addListener((obs, o, n) -> refreshPreview(false));
        fieldOfStudyField.textProperty().addListener((obs, o, n) -> refreshPreview(false));
        graduationYearField.textProperty().addListener((obs, o, n) -> refreshPreview(false));
        githubField.textProperty().addListener((obs, o, n) -> refreshPreview(false));
        portfolioField.textProperty().addListener((obs, o, n) -> refreshPreview(false));
        headlineField.textProperty().addListener((obs, o, n) -> refreshPreview(false));
        bioArea.textProperty().addListener((obs, o, n) -> refreshPreview(false));

        // Recruiter
        recruiterLocationField.textProperty().addListener((obs, o, n) -> refreshPreview(false));
        recruiterPhoneField.textProperty().addListener((obs, o, n) -> refreshPreview(false));
        orgNameField.textProperty().addListener((obs, o, n) -> refreshPreview(false));
        websiteField.textProperty().addListener((obs, o, n) -> refreshPreview(false));
        recruiterDescArea.textProperty().addListener((obs, o, n) -> refreshPreview(false));
    }

    private void refreshPreview(boolean nudge) {
        String role = roleCombo.getValue() == null ? "CANDIDATE" : roleCombo.getValue();
        boolean isCandidate = "CANDIDATE".equals(role);

        String name = (safe(firstNameField.getText()) + " " + safe(lastNameField.getText())).trim();
        if (name.isBlank()) name = "Your Name";
        previewNameLabel.setText(name);
        previewRoleLabel.setText(role);

        setVisible(previewEduBox, isCandidate);
        setVisible(previewSkillsBox, isCandidate);

        String loc = isCandidate ? safe(candidateLocationField.getText()) : safe(recruiterLocationField.getText());
        previewLocationLabel.setText(loc.isBlank() ? "Location" : loc);

        String phone = isCandidate ? safe(candidatePhoneField.getText()) : safe(recruiterPhoneField.getText());
        previewPhoneLabel.setText(phone.isBlank() ? "Phone: —" : "Phone: " + phone);

        if (isCandidate) {
            String headline = safe(headlineField.getText());
            previewPrimaryLabel.setText(headline.isBlank() ? "Headline" : headline);

            previewAboutTitleLabel.setText("About");
            String bio = safe(bioArea.getText());
            previewAboutLabel.setText(bio.isBlank() ? "Bio preview…" : ellipsize(bio, 170));

            String edu = buildEducationLine(
                    safe(degreeField.getText()),
                    safe(fieldOfStudyField.getText()),
                    safe(schoolField.getText()),
                    safe(graduationYearField.getText())
            );
            previewEducationLabel.setText(edu);

            previewSkillsPane.getChildren().clear();
            if (skills.isEmpty()) previewSkillsPane.getChildren().add(createPreviewChip("Add skills (optional)"));
            else {
                int count = 0;
                for (String s : skills) {
                    previewSkillsPane.getChildren().add(createPreviewChip(s));
                    if (++count >= 10) break;
                }
            }

            previewLinksLabel.setText(buildLinksLine(safe(githubField.getText()), safe(portfolioField.getText()), null));
            setPreviewCircleImage(previewAvatarCircle, pickedProfilePicFile == null ? null : pickedProfilePicFile.getAbsolutePath());
        } else {
            String org = safe(orgNameField.getText());
            previewPrimaryLabel.setText(org.isBlank() ? "Organization" : org);

            previewAboutTitleLabel.setText("Description");
            String desc = safe(recruiterDescArea.getText());
            previewAboutLabel.setText(desc.isBlank() ? "Description preview…" : ellipsize(desc, 170));

            previewLinksLabel.setText(buildLinksLine(null, null, safe(websiteField.getText())));
            setPreviewCircleImage(previewAvatarCircle, pickedLogoFile == null ? null : pickedLogoFile.getAbsolutePath());        }

        if (nudge) nudgePreviewCard();
    }

    private void nudgePreviewCard() {
        if (previewNudgeDebounce == null) {
            previewNudgeDebounce = new PauseTransition(Duration.millis(60));
        }
        previewNudgeDebounce.stop();
        previewNudgeDebounce.setOnFinished(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(160), previewCard);
            st.setFromX(1);
            st.setFromY(1);
            st.setToX(1.015);
            st.setToY(1.015);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.setInterpolator(Interpolator.EASE_BOTH);
            st.play();
        });
        previewNudgeDebounce.playFromStart();
    }

    private static String buildEducationLine(String degree, String field, String school, String year) {
        List<String> parts = new ArrayList<>();
        String a = joinNonEmpty(" • ", degree, field);
        if (!a.isBlank()) parts.add(a);
        if (!school.isBlank()) parts.add(school);
        if (!year.isBlank()) parts.add("Class of " + year);
        if (parts.isEmpty()) return "Degree • Field • School • Year";
        return String.join(" • ", parts);
    }

    private static String buildLinksLine(String github, String portfolio, String website) {
        List<String> links = new ArrayList<>();
        if (github != null && !github.isBlank()) links.add("GitHub");
        if (portfolio != null && !portfolio.isBlank()) links.add("Portfolio");
        if (website != null && !website.isBlank()) links.add("Website");
        return links.isEmpty() ? "Links: —" : "Links: " + String.join(" • ", links);
    }

    private static String joinNonEmpty(String sep, String a, String b) {
        a = a == null ? "" : a.trim();
        b = b == null ? "" : b.trim();
        if (a.isBlank() && b.isBlank()) return "";
        if (a.isBlank()) return b;
        if (b.isBlank()) return a;
        return a + sep + b;
    }

    private void setPreviewCircleImage(Circle circle, String path) {
        try {
            if (path == null || path.trim().isEmpty()) {
                circle.setFill(javafx.scene.paint.Paint.valueOf("rgba(255,255,255,0.18)"));
                return;
            }
            File f = new File(path);
            if (!f.exists()) {
                circle.setFill(javafx.scene.paint.Paint.valueOf("rgba(255,255,255,0.18)"));
                return;
            }
            Image img = new Image(f.toURI().toString(), false);
            circle.setFill(new ImagePattern(img));
        } catch (Exception e) {
            circle.setFill(javafx.scene.paint.Paint.valueOf("rgba(255,255,255,0.18)"));
        }
    }

    private static String ellipsize(String s, int max) {
        String t = s == null ? "" : s.trim().replaceAll("\\s+", " ");
        if (t.length() <= max) return t;
        return t.substring(0, Math.max(0, max - 1)) + "…";
    }

    // -------------------------
    // Ambient preview animation + setup
    // -------------------------
    private void setupAnimations() {
        // Ambient “breathing” effect on preview card
        ScaleTransition st = new ScaleTransition(Duration.seconds(5), previewCard);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.01);
        st.setToY(1.01);
        st.setAutoReverse(true);
        st.setCycleCount(Animation.INDEFINITE);
        st.setInterpolator(Interpolator.EASE_BOTH);

        FadeTransition ft = new FadeTransition(Duration.seconds(5), previewCard);
        ft.setFromValue(1.0);
        ft.setToValue(0.97);
        ft.setAutoReverse(true);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.setInterpolator(Interpolator.EASE_BOTH);

        previewAmbient = new ParallelTransition(st, ft);
        previewAmbient.play();
    }

    // -------------------------
    // Validation
    private boolean validateStep1() throws Exception {
        boolean ok = true;

        ok &= require(roleCombo, roleCombo.getValue(), "Role is required.");
        ok &= require(emailField, emailField.getText(), "Email is required.");
        ok &= require(firstNameField, firstNameField.getText(), "First name is required.");
        ok &= require(lastNameField, lastNameField.getText(), "Last name is required.");
        ok &= require(passwordField, passwordField.getText(), "Password is required.");
        ok &= require(confirmPasswordField, confirmPasswordField.getText(), "Confirm password is required.");

        if (!ok) return false;

        if (!isValidEmail(emailField.getText())) {
            setError(emailField, true);
            show(errorLabel, "Please enter a valid email address.");
            ok = false;
        }

        if (!isValidName(firstNameField.getText())) {
            setError(firstNameField, true);
            show(errorLabel, "First name should contain only letters.");
            ok = false;
        }
        if (!isValidName(lastNameField.getText())) {
            setError(lastNameField, true);
            show(errorLabel, "Last name should contain only letters.");
            ok = false;
        }

        String pass = passwordField.getText();
        if (!isBlank(pass) && pass.length() < 8) {
            setError(passwordField, true);
            ok = false;
            show(errorLabel, "Password must be at least 8 characters.");
        }

        if (!isBlank(pass) && !pass.equals(confirmPasswordField.getText())) {
            setError(confirmPasswordField, true);
            ok = false;
            show(errorLabel, "Passwords do not match.");
        }

        String strength = evaluatePasswordStrength(pass);
        if(STRONG.equals(strength)) {
            // ok
        } else if (MEDIUM.equals(strength)) {
            if (ok && isBlank(errorLabel.getText())) {
                show(errorLabel, "Tip: Add a symbol and uppercase letter to strengthen your password.");
            }
        } else { // WEAK
            if (ok && isBlank(errorLabel.getText())) {
                show(errorLabel, "Your password is weak. Use at least 12 chars, mix of upper/lowercase, numbers, and symbols.");
            }
        }

        if (!isBlank(emailField.getText())) {
            if (userService.getByEmail(emailField.getText().trim()) != null) {
                setError(emailField, true);
                ok = false;
                show(errorLabel, "Email already exists.");
            }
        }

        return ok;
    }

    private boolean validateStep2() {
        boolean isCandidate = "CANDIDATE".equals(roleCombo.getValue());
        boolean ok = true;

        if (isCandidate) {
            ok &= require(candidateLocationField, candidateLocationField.getText(), "Location is required.");
            ok &= require(schoolField, schoolField.getText(), "School is required.");
            ok &= require(degreeField, degreeField.getText(), "Degree is required.");
            ok &= require(fieldOfStudyField, fieldOfStudyField.getText(), "Field of study is required.");
            ok &= require(graduationYearField, graduationYearField.getText(), "Graduation year is required.");

            if (!isBlank(graduationYearField.getText())) {
                try {
                    parseYear(graduationYearField.getText());
                } catch (Exception e) {
                    setError(graduationYearField, true);
                    ok = false;
                    show(errorLabel2, e.getMessage());
                }
            }

            ok &= validateOptionalUrl(githubField);
            ok &= validateOptionalUrl(portfolioField);

            // Candidate text validation
            if (!isValidLocation(candidateLocationField.getText())) {
                setError(candidateLocationField, true);
                ok = false;
                show(errorLabel2, "Location should not contain numbers-only or symbols.");
            }

            if (!isValidPhone(candidatePhoneField.getText())) {
                setError(candidatePhoneField, true);
                ok = false;
                show(errorLabel2, "Phone must contain only digits and + (optional).");
            }

            if (!isLettersOnlyOrCommaSeparated(skillInputField.getText())) {
                setError(skillInputField, true);
                ok = false;
                show(errorLabel2, "Skills should be words separated by commas, no numbers.");
            }


            if (!ok && !errorLabel2.isVisible()) show(errorLabel2, "Please fix the highlighted fields.");
        } else {
            ok &= require(recruiterLocationField, recruiterLocationField.getText(), "Location is required.");
            ok &= require(orgNameField, orgNameField.getText(), "Organization name is required.");
            ok &= require(websiteField, websiteField.getText(), "Website URL is required.");
            ok &= require(recruiterDescArea, recruiterDescArea.getText(), "Description is required.");

            ok &= validateOptionalUrl(websiteField);
            if (!isValidLocation(recruiterLocationField.getText())) {
                setError(recruiterLocationField, true);
                ok = false;
                show(errorLabel3, "Location should not contain numbers-only or symbols.");
            }

            if (!isValidPhone(recruiterPhoneField.getText())) {
                setError(recruiterPhoneField, true);
                ok = false;
                show(errorLabel3, "Phone must contain only digits and + (optional).");
            }


            if (!ok && !errorLabel3.isVisible()) show(errorLabel3, "Please fix the highlighted fields.");
        }

        return ok;
    }

    private boolean validateStep3() {
        boolean isCandidate = "CANDIDATE".equals(roleCombo.getValue());
        boolean ok = true;

        if (isCandidate) {
            ok &= require(headlineField, headlineField.getText(), "Headline is required.");
            ok &= require(bioArea, bioArea.getText(), "Bio is required.");
            if (!ok && !errorLabel4.isVisible()) show(errorLabel4, "Please fix the highlighted fields.");
        } else {
            ok = true;
        }
        return ok;
    }

    private boolean validateOptionalUrl(TextField field) {
        String v = field.getText();
        if (isBlank(v)) return true;
        String t = v.trim().toLowerCase();
        if (!(t.startsWith("http://") || t.startsWith("https://"))) {
            setError(field, true);
            return false;
        }
        return true;
    }

    // -------------------------
    // Create account
    // -------------------------
    private void createAccount() throws Exception {
        String role = roleCombo.getValue();

        User u = new User();
        u.setFirstname(firstNameField.getText().trim());
        u.setLastname(lastNameField.getText().trim());
        u.setEmail(emailField.getText().trim());

        String pass = passwordField.getText();

        try (utils.S3StorageService s3 = new utils.S3StorageService(S3_BUCKET, S3_REGION)) {

            if ("CANDIDATE".equals(role)) {
                u.setLocation(candidateLocationField.getText().trim());
                u.setPhone(emptyToNull(candidatePhoneField.getText()));

                u.setSchool(schoolField.getText().trim());
                u.setDegree(degreeField.getText().trim());
                u.setFieldofstudy(fieldOfStudyField.getText().trim());
                u.setGraduationyear(parseYear(graduationYearField.getText()));

                u.setHardskills(skills.isEmpty() ? null : String.join(", ", skills));
                u.setSoftskills(null);

                u.setGithuburl(emptyToNull(githubField.getText()));
                u.setPortfoliourl(emptyToNull(portfolioField.getText()));

                u.setHeadline(headlineField.getText().trim());
                u.setBio(bioArea.getText().trim());

                // Upload profile pic (optional) -> store S3 KEY in DB
                if (pickedProfilePicFile != null) {
                    String ext = utils.S3KeyUtil.extNoDotOrDefault(pickedProfilePicFile.getName(), "jpg");
                    String key = utils.S3KeyUtil.profilePicKey(ext);
                    String ct = utils.S3KeyUtil.contentTypeFromExt(ext);

                    // Upload uses PutObject + RequestBody.fromFile
                    s3.uploadFile(pickedProfilePicFile, key, ct); // uploads to carrieri-storage-dev-islem [web:184]
                    u.setProfilepic(key);
                } else {
                    u.setProfilepic(null);
                }

                try{
                    userService.signupCandidate(u, pass);
                }catch (SQLException s){
                    AlertUtils.showError("Account Not Created.", s.getMessage());
                }

            } else if ("RECRUITER".equals(role)) {
                u.setLocation(recruiterLocationField.getText().trim());
                u.setPhone(emptyToNull(recruiterPhoneField.getText()));

                u.setOrgname(orgNameField.getText().trim());
                u.setWebsiteurl(websiteField.getText().trim());
                u.setDescription(recruiterDescArea.getText().trim());

                // Upload logo (optional) -> store S3 KEY in DB
                if (pickedLogoFile != null) {
                    String ext = utils.S3KeyUtil.extNoDotOrDefault(pickedLogoFile.getName(), "png");
                    String key = utils.S3KeyUtil.orgLogoKey(ext);
                    String ct = utils.S3KeyUtil.contentTypeFromExt(ext);

                    s3.uploadFile(pickedLogoFile, key, ct); // uploads to carrieri-storage-dev-islem [web:184]
                    u.setLogourl(key);
                } else {
                    u.setLogourl(null);
                }


                try{
                    userService.signupRecruiter(u, pass);
                }catch (SQLException s){
                    AlertUtils.showError("Account Not Created.", s.getMessage());
                }

            } else {
                AlertUtils.showError("Error", "Admins cannot sign up from UI.");
                throw new IllegalArgumentException("Admins cannot sign up from UI.");
            }
        }


        AlertUtils.showSuccess("Success!", "Account created successfully!");


        SceneManager.switchTo("/com/example/guser/login.fxml", "Carrieri • Sign in");
    }

    // -------------------------
    // Inline clear-on-type
    // -------------------------
    private void setupInlineClearOnTyping() {
        clearOnType(emailField);
        clearOnType(firstNameField);
        clearOnType(lastNameField);
        clearOnType(passwordField);
        clearOnType(confirmPasswordField);

        clearOnType(candidateLocationField);
        clearOnType(candidatePhoneField);
        clearOnType(schoolField);
        clearOnType(degreeField);
        clearOnType(fieldOfStudyField);
        clearOnType(graduationYearField);
        clearOnType(skillInputField);
        clearOnType(githubField);
        clearOnType(portfolioField);

        clearOnType(recruiterLocationField);
        clearOnType(recruiterPhoneField);
        clearOnType(orgNameField);
        clearOnType(websiteField);
        clearOnType(recruiterDescArea);

        clearOnType(headlineField);
        clearOnType(bioArea);
        clearOnType(passwordField);
        passwordField.textProperty().addListener((obs, o, n) -> updatePasswordStrengthUi());

    }

    private void clearOnType(Control c) {
        if (c instanceof TextInputControl tic) {
            tic.textProperty().addListener((obs, o, n) -> setError(c, false));
        } else {
            c.focusedProperty().addListener((obs, o, focused) -> {
                if (focused) setError(c, false);
            });
        }
    }

    private void setError(Control c, boolean on) {
        if (c == null) return;
        c.pseudoClassStateChanged(ERROR_PC, on);
    }

    private void clearAllErrorBorders() {
        setError(roleCombo, false);
        setError(emailField, false);
        setError(firstNameField, false);
        setError(lastNameField, false);
        setError(passwordField, false);
        setError(confirmPasswordField, false);

        setError(candidateLocationField, false);
        setError(candidatePhoneField, false);
        setError(schoolField, false);
        setError(degreeField, false);
        setError(fieldOfStudyField, false);
        setError(graduationYearField, false);
        setError(skillInputField, false);
        setError(githubField, false);
        setError(portfolioField, false);

        setError(recruiterLocationField, false);
        setError(recruiterPhoneField, false);
        setError(orgNameField, false);
        setError(websiteField, false);
        setError(recruiterDescArea, false);

        setError(headlineField, false);
        setError(bioArea, false);
    }

    private boolean require(Control c, String value, String msg) {
        if (isBlank(value)) {
            setError(c, true);
            showErrorForCurrentStep(msg);
            return false;
        }
        return true;
    }

    private boolean require(Control c, Object value, String msg) {
        String v = value == null ? "" : value.toString();
        return require(c, v, msg);
    }

    // -------------------------
    // Error labels
    // -------------------------
    private void hideAllErrors() {
        hide(errorLabel);
        hide(errorLabel2);
        hide(errorLabel3);
        hide(errorLabel4);
        hide(errorLabel5);
    }

    private void showErrorForCurrentStep(String msg) {
        if (step == 1) show(errorLabel, msg);
        else if (step == 2) {
            boolean isCandidate = "CANDIDATE".equals(roleCombo.getValue());
            if (isCandidate) show(errorLabel2, msg);
            else show(errorLabel3, msg);
        } else {
            boolean isCandidate = "CANDIDATE".equals(roleCombo.getValue());
            if (isCandidate) show(errorLabel4, msg);
            else show(errorLabel5, msg);
        }
    }

    private static void show(Label lbl, String msg) {
        lbl.setText(msg == null ? "Error" : msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
    }

    private static void hide(Label lbl) {
        lbl.setText("");
        lbl.setVisible(false);
        lbl.setManaged(false);
    }

    // -------------------------
    // Helpers
    // -------------------------
    private boolean isValidName(String s) {
        String t = safe(s);
        if (t.isEmpty()) return false;
        return NAME_PATTERN.matcher(t).matches();
    }

    private boolean isLettersOnlyOrCommaSeparated(String s) {
        String t = safe(s);
        if (t.isEmpty()) return true;
        for (String part : t.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            if (!p.matches("^[\\p{L} .'-]+$")) return false;
        }
        return true;
    }

    private boolean isValidLocation(String s) {
        String t = safe(s);
        if (t.isEmpty()) return false;
        return t.matches("^[\\p{L}0-9 .,'-]{2,60}$");
    }

    private boolean isValidEmail(String s) {
        String t = safe(s);
        if (t.isEmpty()) return false;
        return EMAIL_PATTERN.matcher(t).matches();
    }

    private boolean isValidPhone(String s) {
        String t = safe(s);
        if (t.isEmpty()) return true; // optional in your flow
        return PHONE_PATTERN.matcher(t).matches();
    }

    private static void setVisible(VBox box, boolean on) { box.setVisible(on); box.setManaged(on); }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static Integer parseYear(String s) {
        try {
            int y = Integer.parseInt(s.trim());
            if (y < 1900 || y > 2100) throw new NumberFormatException();
            return y;
        } catch (Exception e) {
            throw new IllegalArgumentException("Graduation year must be a valid year.");
        }
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
