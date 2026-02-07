package com.example.guser.controllers;

import com.example.guser.SceneManager;
import entities.User;
import javafx.event.ActionEvent;
import services.ProfileService;
import session.ProfileViewContext;
import session.SessionContext;
import utils.S3KeyUtil;
import utils.S3StorageService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.scene.image.ImageView;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ProfileController {

    private enum Card { BASIC, ABOUT, EDU, SKILLS, LINKS, ORG }

    private static final String S3_BUCKET = "carrieri-storage-dev-islem";
    private static final String S3_REGION = "eu-west-3";

    private final ProfileService profileService = new ProfileService();

    private User targetUser;
    private boolean owner;

    // Included navbar controller (injected via fx:include fx:id="appNav")
    @FXML private AppNavController appNavController;

    // Header
    @FXML private Circle avatarCircle;
    @FXML private Label fullNameLabel;
    @FXML private Label subTitleLabel;
    @FXML private Label metaLabel;

    @FXML private Button headerEditBtn;
    @FXML private Button headerBackBtn;
    @FXML private Button headerSaveBtn;
    @FXML private Button changePicBtn;
    @FXML private TextField headerHeadlineField;


    @FXML private Label headerBioLabel;
    @FXML private Label statsViewsLabel;
    @FXML private Label statsConnectionsLabel;
    @FXML private Label statsOpenToLabel;



    @FXML private Label errorLabel;

    // Sections
    @FXML private VBox candidateSection;
    @FXML private VBox recruiterSection;

    // People you may know
    @FXML private VBox peopleBox;

    // BASIC
    @FXML private VBox basicViewBox;
    @FXML private VBox basicEditBox;
    @FXML private Button basicPencilBtn;
    @FXML private Button basicBackBtn;
    @FXML private Button basicSaveBtn;

    @FXML private Label firstNameView;
    @FXML private Label lastNameView;
    @FXML private Label locationView;
    @FXML private Label phoneView;

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField locationField;
    @FXML private TextField phoneField;

    // ABOUT
    @FXML private VBox aboutViewBox;
    @FXML private VBox aboutEditBox;
    @FXML private Button aboutPencilBtn;
    @FXML private Button aboutBackBtn;
    @FXML private Button aboutSaveBtn;

    //@FXML private Label headlineView;
    @FXML private Label bioView;

    //@FXML private TextField headlineField;
    @FXML private TextArea bioArea;

    // EDUCATION
    @FXML private VBox eduViewBox;
    @FXML private VBox eduEditBox;
    @FXML private Button eduPencilBtn;
    @FXML private Button eduBackBtn;
    @FXML private Button eduSaveBtn;

    @FXML private Label schoolView;
    @FXML private Label degreeView;
    @FXML private Label fieldView;
    @FXML private Label gradYearView;
    @FXML private ImageView eduLogoImage;
    private static final String DEMO_EDU_LOGO_URL = "https://via.placeholder.com/64x64.png?text=UTM";


    @FXML private TextField schoolField;
    @FXML private TextField degreeField;
    @FXML private TextField fieldOfStudyField;
    @FXML private TextField graduationYearField;

    // SKILLS
    @FXML private VBox skillsViewBox;
    @FXML private VBox skillsEditBox;
    @FXML private Button skillsPencilBtn;
    @FXML private Button skillsBackBtn;
    @FXML private Button skillsSaveBtn;

    @FXML private FlowPane hardSkillsPane;
    @FXML private FlowPane softSkillsPane;
    @FXML private TextArea hardSkillsArea;
    @FXML private TextArea softSkillsArea;

    // LINKS
    @FXML private VBox linksViewBox;
    @FXML private VBox linksEditBox;
    @FXML private Button linksPencilBtn;
    @FXML private Button linksBackBtn;
    @FXML private Button linksSaveBtn;

    @FXML private Label githubView;
    @FXML private Label portfolioView;

    @FXML private TextField githubField;
    @FXML private TextField portfolioField;

    // ORG (Recruiter)
    @FXML private VBox orgViewBox;
    @FXML private VBox orgEditBox;
    @FXML private Button orgPencilBtn;
    @FXML private Button orgBackBtn;
    @FXML private Button orgSaveBtn;

    @FXML private Label orgNameView;
    @FXML private Label websiteView;
    @FXML private Label orgDescView;

    @FXML private TextField orgNameField;
    @FXML private TextField websiteField;
    @FXML private TextArea orgDescArea;

    // Workspace
    @FXML private Label workspaceHintLabel;

    @FXML
    public void initialize() {
        hideError();

        if (!SessionContext.isLoggedIn()) {
            SceneManager.switchTo("/com/example/guser/login.fxml", "Carrieri • Sign in");
            return;
        }

        // Navbar setup (customizable per page)
        if (appNavController != null) {
            appNavController.setActive(AppNavController.Route.PROFILE);
            appNavController.setBackVisible(true);
            appNavController.setSearchVisible(true);
            appNavController.setBackAction(() -> {
                // Back goes to my profile (or you can implement a stack later)
                int me = SessionContext.getCurrentUser().getId();
                ProfileViewContext.viewUser(me);
                SceneManager.switchTo("/com/example/guser/profile.fxml", "Carrieri • Profile");
            });
        }

        int me = SessionContext.getCurrentUser().getId();
        Integer targetId = ProfileViewContext.getTargetUserId();
        if (targetId == null) targetId = me;

        loadProfile(targetId);
    }

    private void loadProfile(int userId) {
        try {
            User u = profileService.getById(userId);
            if (u == null) throw new IllegalArgumentException("User not found.");

            targetUser = u;
            owner = (SessionContext.getCurrentUser().getId() == u.getId());

// placeholders until backend exists:
            if (statsViewsLabel != null) statsViewsLabel.setText("—");
            if (statsConnectionsLabel != null) statsConnectionsLabel.setText("—");

            if (statsOpenToLabel != null) {
                statsOpenToLabel.setText("CANDIDATE".equals(u.getRoles()) ? "Open to work" : "Hiring");
            }

            boolean isCandidate = "CANDIDATE".equals(u.getRoles());
            boolean isRecruiter = "RECRUITER".equals(u.getRoles());

            setVisibleManaged(candidateSection, isCandidate);
            setVisibleManaged(recruiterSection, isRecruiter);

            // header buttons
            setVisibleManaged(headerEditBtn, owner);
            setVisibleManaged(headerBackBtn, false);
            setVisibleManaged(headerSaveBtn, false);
            setVisibleManaged(changePicBtn, false);

            // fill edit fields
            firstNameField.setText(n(u.getFirstname()));
            lastNameField.setText(n(u.getLastname()));
            locationField.setText(n(u.getLocation()));
            phoneField.setText(n(u.getPhone()));

            bioArea.setText(n(u.getBio()));

            schoolField.setText(n(u.getSchool()));
            degreeField.setText(n(u.getDegree()));
            fieldOfStudyField.setText(n(u.getFieldofstudy()));
            graduationYearField.setText(u.getGraduationyear() == null ? "" : String.valueOf(u.getGraduationyear()));
            // demo education logo (replace later with a real per-school image)
            if (eduLogoImage != null) {
                eduLogoImage.setImage(new Image(DEMO_EDU_LOGO_URL, true));
            }

            hardSkillsArea.setText(n(u.getHardskills()));
            softSkillsArea.setText(n(u.getSoftskills()));

            githubField.setText(n(u.getGithuburl()));
            portfolioField.setText(n(u.getPortfoliourl()));

            orgNameField.setText(n(u.getOrgname()));
            websiteField.setText(n(u.getWebsiteurl()));
            orgDescArea.setText(n(u.getDescription()));

            // view labels
            firstNameView.setText(blankToDash(u.getFirstname()));
            lastNameView.setText(blankToDash(u.getLastname()));
            locationView.setText(blankToDash(u.getLocation()));
            phoneView.setText(blankToDash(u.getPhone()));

            bioView.setText(blankToDash(u.getBio()));

            schoolView.setText(blankToDash(u.getSchool()));
            degreeView.setText(blankToDash(u.getDegree()));
            fieldView.setText(blankToDash(u.getFieldofstudy()));
            gradYearView.setText(u.getGraduationyear() == null ? "—" : String.valueOf(u.getGraduationyear()));

            githubView.setText(blankToDash(u.getGithuburl()));
            portfolioView.setText(blankToDash(u.getPortfoliourl()));

            orgNameView.setText(blankToDash(u.getOrgname()));
            websiteView.setText(blankToDash(u.getWebsiteurl()));
            orgDescView.setText(blankToDash(u.getDescription()));

            // chips
            renderSkillChips(hardSkillsPane, u.getHardskills());
            renderSkillChips(softSkillsPane, u.getSoftskills());

            // close all edits
            closeAllEdits();

            // pencils visible only for owner
            setVisibleManaged(basicPencilBtn, owner);
            setVisibleManaged(aboutPencilBtn, owner && isCandidate);
            setVisibleManaged(eduPencilBtn, owner && isCandidate);
            setVisibleManaged(skillsPencilBtn, owner && isCandidate);
            setVisibleManaged(linksPencilBtn, owner && isCandidate);
            setVisibleManaged(orgPencilBtn, owner && isRecruiter);



            refreshHeaderTexts();

            String key = isRecruiter ? u.getLogourl() : u.getProfilepic();
            System.out.println("picture get key:" + key);
            Platform.runLater(() -> renderAvatarFromS3Key(key));

            workspaceHintLabel.setText("Workspace: coming next (tracks, files, videos, folders).");

            loadPeopleSuggestions();

        } catch (Exception e) {
            showError(e.getMessage());
            e.printStackTrace();
        }
    }

    private void refreshHeaderTexts() {
        String name = (n(targetUser.getFirstname()) + " " + n(targetUser.getLastname())).trim();
        fullNameLabel.setText(name.isBlank() ? "Profile" : name);

        if ("RECRUITER".equals(targetUser.getRoles())) subTitleLabel.setText(blankToDash(targetUser.getOrgname()));
        else subTitleLabel.setText(blankToDash(targetUser.getHeadline()));

        StringBuilder meta = new StringBuilder(targetUser.getRoles());
        if (!n(targetUser.getLocation()).isBlank()) meta.append(" • ").append(targetUser.getLocation().trim());
        metaLabel.setText(meta.toString());
    }

    @FXML private void onHeaderEdit() {
        if (!owner) return;

        setVisibleManaged(headerEditBtn, false);
        setVisibleManaged(headerBackBtn, true);
        setVisibleManaged(headerSaveBtn, true);
        setVisibleManaged(changePicBtn, true);

        // show headline editor in hero (candidate only)
        if ("CANDIDATE".equals(targetUser.getRoles())) {
            setVisibleManaged(subTitleLabel, false);
            setVisibleManaged(headerHeadlineField, true);
            if (headerHeadlineField != null) headerHeadlineField.setText(n(targetUser.getHeadline()));
        }

        // open BASIC and ABOUT/ORG together (your existing behavior)
        openCard(Card.BASIC, true);
        if ("CANDIDATE".equals(targetUser.getRoles())) openCard(Card.ABOUT, false);
        if ("RECRUITER".equals(targetUser.getRoles())) openCard(Card.ORG, false);
    }


    @FXML private void onHeaderBack() {
        if (!owner) return;

        // restore hero headline view
        setVisibleManaged(headerHeadlineField, false);
        setVisibleManaged(subTitleLabel, true);

        loadProfile(targetUser.getId());
    }


    @FXML private void onHeaderSave() {
        if (!owner) return;

        // Apply headline from hero editor first (so About headline doesn't override it)
        if ("CANDIDATE".equals(targetUser.getRoles())) {
            if (headerHeadlineField != null) {
                targetUser.setHeadline(emptyToNull(headerHeadlineField.getText()));
                // keep About editor synced if it exists
                if (headerHeadlineField != null) headerHeadlineField.setText(n(targetUser.getHeadline()));
            }
        }

        // Save basic + about/org then reload (exits edit mode) — your old flow
        onBasicSave();
        if ("CANDIDATE".equals(targetUser.getRoles())) onAboutSave();
        if ("RECRUITER".equals(targetUser.getRoles())) onOrgSave();

        // restore hero headline view state (loadProfile will also reset buttons)
        setVisibleManaged(headerHeadlineField, false);
        setVisibleManaged(subTitleLabel, true);

        loadProfile(targetUser.getId());
    }


    // ===== BASIC =====
    @FXML private void onBasicEdit() { if (owner) openCard(Card.BASIC, true); }
    @FXML private void onBasicBack() { if (owner) closeCard(Card.BASIC); }

    @FXML private void onBasicSave() {
        if (!owner) return;
        try {
            hideError();

            targetUser.setFirstname(req(firstNameField.getText(), "First name required"));
            targetUser.setLastname(req(lastNameField.getText(), "Last name required"));
            targetUser.setLocation(emptyToNull(locationField.getText()));
            targetUser.setPhone(emptyToNull(phoneField.getText()));

            if ("CANDIDATE".equals(targetUser.getRoles())) profileService.updateCandidateProfile(targetUser);
            else if ("RECRUITER".equals(targetUser.getRoles())) profileService.updateRecruiterProfile(targetUser);

            SessionContext.setCurrentUser(targetUser);

            // Important: exit edit mode + refresh UI
            loadProfile(targetUser.getId());

        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    // ===== ABOUT =====
    @FXML private void onAboutEdit() { if (owner) openCard(Card.ABOUT, true); }
    @FXML private void onAboutBack() { if (owner) closeCard(Card.ABOUT); }

    @FXML private void onAboutSave() {
        if (!owner) return;
        try {
            hideError();

            // About is bio-only now
            if (bioArea != null) targetUser.setBio(emptyToNull(bioArea.getText()));

            profileService.updateCandidateProfile(targetUser);
            SessionContext.setCurrentUser(targetUser);
            loadProfile(targetUser.getId());
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }


    // ===== EDUCATION =====
    @FXML private void onEducationEdit() { if (owner) openCard(Card.EDU, true); }
    @FXML private void onEducationBack() { if (owner) closeCard(Card.EDU); }

    @FXML private void onEducationSave() {
        if (!owner) return;
        try {
            hideError();
            targetUser.setSchool(emptyToNull(schoolField.getText()));
            targetUser.setDegree(emptyToNull(degreeField.getText()));
            targetUser.setFieldofstudy(emptyToNull(fieldOfStudyField.getText()));
            targetUser.setGraduationyear(parseSmallIntOrNull(graduationYearField.getText()));
            profileService.updateCandidateProfile(targetUser);
            SessionContext.setCurrentUser(targetUser);
            loadProfile(targetUser.getId());
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    // ===== SKILLS =====
    @FXML private void onSkillsEdit() { if (owner) openCard(Card.SKILLS, true); }
    @FXML private void onSkillsBack() { if (owner) closeCard(Card.SKILLS); }

    @FXML private void onSkillsSave() {
        if (!owner) return;
        try {
            hideError();
            targetUser.setHardskills(emptyToNull(hardSkillsArea.getText()));
            targetUser.setSoftskills(emptyToNull(softSkillsArea.getText()));
            profileService.updateCandidateProfile(targetUser);
            SessionContext.setCurrentUser(targetUser);
            loadProfile(targetUser.getId());
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    // ===== LINKS =====
    @FXML private void onLinksEdit() { if (owner) openCard(Card.LINKS, true); }
    @FXML private void onLinksBack() { if (owner) closeCard(Card.LINKS); }

    @FXML private void onLinksSave() {
        if (!owner) return;
        try {
            hideError();
            targetUser.setGithuburl(emptyToNull(githubField.getText()));
            targetUser.setPortfoliourl(emptyToNull(portfolioField.getText()));
            profileService.updateCandidateProfile(targetUser);
            SessionContext.setCurrentUser(targetUser);
            loadProfile(targetUser.getId());
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    // ===== ORG =====
    @FXML private void onOrgEdit() { if (owner) openCard(Card.ORG, true); }
    @FXML private void onOrgBack() { if (owner) closeCard(Card.ORG); }

    @FXML private void onOrgSave() {
        if (!owner) return;
        try {
            hideError();
            targetUser.setOrgname(emptyToNull(orgNameField.getText()));
            targetUser.setWebsiteurl(emptyToNull(websiteField.getText()));
            targetUser.setDescription(emptyToNull(orgDescArea.getText()));
            profileService.updateRecruiterProfile(targetUser);
            SessionContext.setCurrentUser(targetUser);
            loadProfile(targetUser.getId());
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    // ===== Picture =====
    @FXML
    private void onChangePicture() {
        hideError();
        if (!owner || targetUser == null) return;

        try {
            File file = pickImageFile();
            if (file == null) return;

            String ext = S3KeyUtil.extNoDotOrDefault(file.getName(), "jpg");
            String ct = S3KeyUtil.contentTypeFromExt(ext);

            try (S3StorageService s3 = new S3StorageService(S3_BUCKET, S3_REGION)) {
                if ("RECRUITER".equals(targetUser.getRoles())) {
                    String key = S3KeyUtil.orgLogoKey(ext);
                    s3.uploadFile(file, key, ct);
                    profileService.updateLogoKey(targetUser.getId(), key);
                    targetUser.setLogourl(key);
                    renderAvatarFromS3Key(key);
                } else {
                    String key = S3KeyUtil.profilePicKey(ext);
                    s3.uploadFile(file, key, ct);
                    profileService.updateProfilePicKey(targetUser.getId(), key);
                    targetUser.setProfilepic(key);
                    renderAvatarFromS3Key(key);
                }
            }
        } catch (Exception e) {
            showError("Picture upload failed: " + e.getMessage());
        }
    }

    private File pickImageFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose image");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );
        return fc.showOpenDialog(avatarCircle.getScene().getWindow());
    }

    // ===== Edit state helpers =====
    private void openCard(Card card, boolean closeOthers) {
        if (closeOthers) closeAllEdits();
        setCardEditing(card, true);
    }

    private void closeCard(Card card) {
        setCardEditing(card, false);
    }

    private void closeAllEdits() {
        setCardEditing(Card.BASIC, false);
        setCardEditing(Card.ABOUT, false);
        setCardEditing(Card.EDU, false);
        setCardEditing(Card.SKILLS, false);
        setCardEditing(Card.LINKS, false);
        setCardEditing(Card.ORG, false);
    }

    private void setCardEditing(Card card, boolean editing) {
        // if a card doesn't exist (e.g., recruiter has no candidate cards), just ignore
        switch (card) {
            case BASIC -> setCardEditing(editing, basicViewBox, basicEditBox, basicPencilBtn, basicSaveBtn, basicBackBtn);
            case ABOUT -> setCardEditing(editing, aboutViewBox, aboutEditBox, aboutPencilBtn, aboutSaveBtn, aboutBackBtn);
            case EDU -> setCardEditing(editing, eduViewBox, eduEditBox, eduPencilBtn, eduSaveBtn, eduBackBtn);
            case SKILLS -> setCardEditing(editing, skillsViewBox, skillsEditBox, skillsPencilBtn, skillsSaveBtn, skillsBackBtn);
            case LINKS -> setCardEditing(editing, linksViewBox, linksEditBox, linksPencilBtn, linksSaveBtn, linksBackBtn);
            case ORG -> setCardEditing(editing, orgViewBox, orgEditBox, orgPencilBtn, orgSaveBtn, orgBackBtn);
        }
    }

    private static void setCardEditing(boolean editing, VBox viewBox, VBox editBox, Button pencil, Button save, Button back) {
        setVisibleManaged(viewBox, !editing);
        setVisibleManaged(editBox, editing);
        setVisibleManaged(pencil, !editing);
        setVisibleManaged(save, editing);
        setVisibleManaged(back, editing);
    }

    private static void setVisibleManaged(Node n, boolean on) {
        if (n == null) return;
        n.setVisible(on);
        n.setManaged(on);
    }

    // ===== Suggestions =====
    private void loadPeopleSuggestions() {
        try {
            if (peopleBox == null) return;
            peopleBox.getChildren().clear();

            List<User> suggestions = profileService.suggestPeopleYouMayKnow(
                    SessionContext.getCurrentUser().getId(),
                    SessionContext.getCurrentUser().getRoles()
            );

            if (suggestions.isEmpty()) {
                Label none = new Label("No suggestions yet.");
                none.getStyleClass().add("prf-muted");
                peopleBox.getChildren().add(none);
                return;
            }

            for (entities.User u : suggestions) {
                if (u.getId() == SessionContext.getCurrentUser().getId()) continue;
                peopleBox.getChildren().add(buildPersonRow(u));
            }

        } catch (Exception e) {
            Label err = new Label("Could not load suggestions.");
            err.getStyleClass().add("prf-muted");
            peopleBox.getChildren().add(err);
        }
    }

    private HBox peopleRow(User u) {
        Circle c = new Circle(16);
        c.setFill(javafx.scene.paint.Paint.valueOf("rgba(0,0,0,0.08)"));

        String key = "RECRUITER".equals(u.getRoles()) ? u.getLogourl() : u.getProfilepic();
        System.out.println("picture get key:" + key);
        Platform.runLater(() -> renderCircleFromS3Key(c, key));

        Label name = new Label((n(u.getFirstname()) + " " + n(u.getLastname())).trim());
        name.getStyleClass().add("prf-value");

        Label sub = new Label("RECRUITER".equals(u.getRoles()) ? blankToDash(u.getOrgname()) : blankToDash(u.getHeadline()));
        sub.getStyleClass().add("prf-muted");

        VBox texts = new VBox(2, name, sub);

        Button view = new Button("View");
        view.getStyleClass().addAll("prf-btn", "prf-btnSoft", "prf-miniBtn");
        view.setOnAction(e -> {
            ProfileViewContext.viewUser(u.getId());
            SceneManager.switchTo("/com/example/guser/profile.fxml", "Carrieri • Profile");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        return new HBox(10, c, texts, spacer, view);
    }

    private Node buildPersonRow(entities.User u) {
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox();
        row.getStyleClass().add("prf-personRow");

        Circle avatar = new Circle(18);
        avatar.setFill(javafx.scene.paint.Paint.valueOf("rgba(0,0,0,0.08)"));
        //avatar.setStyle("-fx-fill: rgba(35,25,66,0.10);");
        String key = "RECRUITER".equals(u.getRoles())
                ? n(u.getLogourl())
                : n(u.getProfilepic());
        Platform.runLater(() -> renderCircleFromS3Key(avatar, key));


        javafx.scene.control.Label name = new javafx.scene.control.Label(
                (n(u.getFirstname()) + " " + n(u.getLastname())).trim()
        );
        name.getStyleClass().add("prf-personName");

        // Show headline if candidate, or org name if recruiter (adapt getters to your model)
        String sub = "RECRUITER".equals(u.getRoles())
                ? n(u.getOrgname())
                : n(u.getHeadline());
        javafx.scene.control.Label subLabel = new javafx.scene.control.Label(sub.isBlank() ? n(u.getRoles()) : sub);
        subLabel.getStyleClass().add("prf-personSub");

        javafx.scene.control.Label meta = new javafx.scene.control.Label(n(u.getLocation()));
        meta.getStyleClass().add("prf-personMeta");

        javafx.scene.layout.VBox text = new javafx.scene.layout.VBox(2, name, subLabel, meta);
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.control.Button connect = new javafx.scene.control.Button("Connect");
        connect.getStyleClass().add("prf-connectBtn");

        // Make row open profile (no "View" button)
        row.setOnMouseClicked(e -> openProfile(u.getId()));

        // Clicking the button should NOT trigger row click
        connect.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> e.consume());
        connect.setOnAction(e -> {
            // TODO: implement connect/follow action
        });

        row.getChildren().addAll(avatar, text, spacer, connect);
        return row;
    }

    @FXML
    private void onShowAllPeople(ActionEvent event) {
        // TODO: later navigate to a full People page
        // For now, just a safe no-op or show a message
        System.out.println("Show all people clicked");
    }

    private void openProfile(int userId) {
        session.ProfileViewContext.viewUser(userId);
        com.example.guser.SceneManager.switchTo("/com/example/guser/profile.fxml", "Carrieri • Profile");
    }



    // ===== Rendering =====
    private void renderAvatarFromS3Key(String s3Key) {
        try (S3StorageService s3 = new S3StorageService(S3_BUCKET, S3_REGION)) {
            if (s3Key == null || s3Key.isBlank()) return;
            String url = s3.presignedGetUrl(s3Key, Duration.ofMinutes(10));
            Image img = new Image(url, false);
            if (!img.isError()) avatarCircle.setFill(new ImagePattern(img));
        } catch (Exception ignored) {}
    }

    private void renderCircleFromS3Key(Circle circle, String s3Key) {
        try (S3StorageService s3 = new S3StorageService(S3_BUCKET, S3_REGION)) {
            if (s3Key == null || s3Key.isBlank()) return;
            String url = s3.presignedGetUrl(s3Key, Duration.ofMinutes(10));
            Image img = new Image(url, false);
            if (!img.isError()) circle.setFill(new ImagePattern(img));
        } catch (Exception ignored) {}
    }

    private void renderSkillChips(FlowPane pane, String csv) {
        if (pane == null) return;
        pane.getChildren().clear();

        List<String> skills = new ArrayList<>();
        if (csv != null) {
            for (String raw : csv.split(",")) {
                String t = raw.trim();
                if (!t.isEmpty()) skills.add(t);
            }
        }

        if (skills.isEmpty()) {
            Label none = new Label("—");
            none.getStyleClass().add("prf-muted");
            pane.getChildren().add(none);
            return;
        }

        for (String s : skills) {
            Label chip = new Label(s);
            chip.getStyleClass().add("prf-chip");
            pane.getChildren().add(chip);
        }
    }

    // ===== small helpers =====
    private static Integer parseSmallIntOrNull(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.isEmpty()) return null;
        int v = Integer.parseInt(t);
        if (v < 1900 || v > 2100) throw new IllegalArgumentException("Graduation year looks invalid.");
        return v;
    }

    private static String n(String s) { return s == null ? "" : s; }
    private static String blankToDash(String s) { return (s == null || s.trim().isEmpty()) ? "—" : s.trim(); }
    private static String emptyToNull(String s) { return (s == null || s.trim().isEmpty()) ? null : s.trim(); }

    private static String req(String s, String msg) {
        if (s == null || s.trim().isEmpty()) throw new IllegalArgumentException(msg);
        return s.trim();
    }

    private void showError(String msg) {
        errorLabel.setText(msg == null ? "Error" : msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
