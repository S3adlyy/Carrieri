package com.example.guser.controllers.guser;

import entities.guser.Artifact;
import entities.guser.FileObject;
import entities.guser.Snapshot;
import entities.guser.Track;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import services.guser.*;
import utils.guser.AlertUtils;


public class TrackController {

    @FXML private VBox trackDetailPane;

    @FXML private Button backToTracksBtn;
    @FXML private Label trackTitleLabel;
    @FXML private ComboBox<String> trackVisibilityCombo;
    @FXML private Button createSnapshotBtn;

    @FXML private ListView<ArtifactRow> artifactsListView;

    @FXML private StackPane artifactViewerHost;
    @FXML private Label viewerPlaceholderLabel;

    @FXML private Label trackErrorLabel;

    // Services (your exact constructors)
    private final TrackService trackService = new TrackService();
    private final ArtifactService artifactService = new ArtifactService();
    private final SnapshotItemService snapshotItemService = new SnapshotItemService();

    private final String bucket = "carrieri-storage-dev-islem";   // TODO: centralize config
    private final String region = "eu-west-3";   // TODO: centralize config

    private final SnapshotService snapshotService = new SnapshotService(bucket, region);
    private final FileObjectService fileObjectService = new FileObjectService(bucket, region);
    private final CodeBrowseService codeBrowseService = new CodeBrowseService(bucket, region);

    private final ObservableList<Snapshot> snapshots = FXCollections.observableArrayList();
    private final ObservableList<ArtifactRow> artifactRows = FXCollections.observableArrayList();

    private int candidateId;
    private int viewerUserId;
    private boolean ownerMode;
    private Track track;
    private Runnable onBack;

    private Snapshot selectedSnapshot;

    private static final long MAX_UPLOAD_BYTES = 50L * 1024 * 1024; // 50MB

    //new fields after timeline ui

    @FXML private VBox progressPane;
    @FXML private ScrollPane timelineScroll;
    @FXML private HBox timelineSegmentsBox;
    @FXML private Button prevSnapshotBtn;
    @FXML private Button nextSnapshotBtn;
    @FXML private ComboBox<Snapshot> snapshotJumpCombo;
    @FXML private VBox selectedSnapshotCard;
    @FXML private Label selectedSnapshotTitleLabel;
    @FXML private Label selectedSnapshotMetaLabel;


    @FXML private Label artifactsTitleLabel;
// remove snapshotsListView field from FXML + controller (not used anymore)
private enum ViewMode { CURRENT, PROGRESS }
    private ViewMode viewMode = ViewMode.CURRENT;

// In progress mode, selectedSnapshot is used; in current mode it can be null.

    private final ToggleGroup timelineGroup = new ToggleGroup();
    private final Map<Toggle, Snapshot> snapshotByToggle = new HashMap<>();

    @FXML private ToggleButton currentModeBtn;
    @FXML private ToggleButton progressModeBtn;

    private final ToggleGroup modeGroup = new ToggleGroup();
    private boolean previewShown = false;

    @FXML private Label trackTypeLabel;
    @FXML private Label trackDatesLabel;

    @FXML private VBox previewPane;
    @FXML private VBox artifactsPane;
    @FXML private Button closePreviewBtn;
    @FXML private SplitPane artifactSplitPane;

    //video and audio preview
    private MediaPlayer currentPlayer;

    @FXML private Button addArtifactBtn;







    @FXML
    private void initialize() {
        trackVisibilityCombo.setItems(FXCollections.observableArrayList("PUBLIC", "PRIVATE"));

        artifactsListView.setItems(artifactRows);
        installArtifactCells(); // we will replace this method in section G (icons + better headers)

        // Start state: no preview
        hidePreviewPane();
        closePreview();
        setError(null);

        // Mode tabs
        currentModeBtn.setToggleGroup(modeGroup);
        progressModeBtn.setToggleGroup(modeGroup);
        currentModeBtn.setSelected(true);

        modeGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) {
                modeGroup.selectToggle(oldT != null ? oldT : currentModeBtn);
                return;
            }

            closePreview();
            hidePreviewPane();

            if (newT == currentModeBtn) setViewMode(ViewMode.CURRENT);
            else setViewMode(ViewMode.PROGRESS);
        });

        // Artifact click -> show preview pane and open
        artifactsListView.getSelectionModel().selectedItemProperty().addListener((obs, o, row) -> {
            if (row == null || row.kind != ArtifactRowKind.ITEM) return;

            showPreviewPane();
            openArtifactRowByMode(row);
        });

        // Progress navigation
        prevSnapshotBtn.setOnAction(e -> {
            closePreview();
            hidePreviewPane();
            selectPrevSnapshot();
        });

        nextSnapshotBtn.setOnAction(e -> {
            closePreview();
            hidePreviewPane();
            selectNextSnapshot();
        });

        snapshotJumpCombo.setVisibleRowCount(12);
        snapshotJumpCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Snapshot s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? null : formatSnapshotLabel(s));
            }
        });
        snapshotJumpCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Snapshot s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? "Jump to snapshot" : formatSnapshotLabel(s));
            }
        });
        snapshotJumpCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, s) -> {
            if (s == null) return;
            closePreview();
            hidePreviewPane();
            selectSnapshot(s);
        });

        hide();
        showPlaceholder("Select an artifact.");
    }

    private void hidePreviewPane() {
        if (artifactSplitPane == null || artifactsPane == null || previewPane == null) return;

        previewPane.setVisible(false);
        previewPane.setManaged(false);

        if (artifactSplitPane.getItems().size() != 1 || artifactSplitPane.getItems().get(0) != artifactsPane) {
            artifactSplitPane.getItems().setAll(artifactsPane);
        }
        previewShown = false;
    }

    private void showPreviewPane() {
        if (artifactSplitPane == null || artifactsPane == null || previewPane == null) return;

        previewPane.setVisible(true);
        previewPane.setManaged(true);

        if (artifactSplitPane.getItems().size() != 2) {
            artifactSplitPane.getItems().setAll(artifactsPane, previewPane);
            Platform.runLater(() -> artifactSplitPane.setDividerPositions(0.40));
        }
        previewShown = true;
    }

    private void closePreview() {
        // Stop/dispose video audio player if any
        try {
            if (currentPlayer != null) {
                currentPlayer.stop();
                currentPlayer.dispose();
                currentPlayer = null;
            }
        } catch (Exception ignored) {}

        //dispose of zip files
        try {
            Node n = artifactViewerHost.getChildren().isEmpty() ? null : artifactViewerHost.getChildren().get(0);
            if (n != null && n.getUserData() instanceof CodeViewerController c) c.dispose();
        } catch (Exception ignored) {}

        showPlaceholder("Select an artifact.");
    }
    @FXML
    private void onClosePreview() {
        closePreview();
        hidePreviewPane();
    }



    private void setViewMode(ViewMode m) {
        this.viewMode = m;
        closePreview();
        boolean progress = (m == ViewMode.PROGRESS);
        progressPane.setVisible(progress);
        progressPane.setManaged(progress);

        if (progress) {
            artifactsTitleLabel.setText("Snapshot artifacts");
            // Ensure snapshots loaded and timeline built
            refreshSnapshotsAndTimeline();
            if (!snapshots.isEmpty()) {
                // auto-select oldest snapshot (option A)
                selectSnapshot(snapshots.get(0));
            } else {
                selectedSnapshot = null;
                artifactRows.clear();
                showPlaceholder("No snapshots yet. Create one.");
            }
        } else {
            artifactsTitleLabel.setText("Current track artifacts");
            selectedSnapshot = null;
            refreshCurrentArtifacts();
            showPlaceholder("Select an artifact.");
        }
    }


    public void initContext(int candidateId, int viewerUserId, boolean ownerMode, Track track, Runnable onBack) {
        this.candidateId = candidateId;
        this.viewerUserId = viewerUserId;
        this.ownerMode = ownerMode;
        this.track = track;
        this.onBack = onBack;

        trackTitleLabel.setText(track.getTitle());
        // NEW: header meta
        trackTypeLabel.setText(safeUpper(track.getCategory()));   // change getter name if needed
        trackDatesLabel.setText(buildTrackDateRange(track));  // helper below

// NEW: reset preview state when opening a track
        closePreview();
        hidePreviewPane();


        trackVisibilityCombo.getSelectionModel().select(track.getVisibility());
        trackVisibilityCombo.setDisable(!ownerMode);

        createSnapshotBtn.setVisible(ownerMode);
        createSnapshotBtn.setManaged(ownerMode);

        addArtifactBtn.setVisible(ownerMode);
        addArtifactBtn.setManaged(ownerMode);


        refreshCurrentArtifacts();
        setViewMode(ViewMode.CURRENT);
    }
    private String buildTrackDateRange(Track t) {
        // Replace these with your real fields/getters:
        LocalDate start = t.getStartDate();
        LocalDate end   = t.getEndDate();



        // Example when you have LocalDate:
        DateTimeFormatter df = DateTimeFormatter.ofPattern("MMM dd, yyyy");
         String s = start == null ? "—" : df.format(start);
         String e = end == null ? "—" : df.format(end);
         return s + "  →  " + e;
    }


    public void show() {
        trackDetailPane.setVisible(true);
        trackDetailPane.setManaged(true);
    }

    public void hide() {
        trackDetailPane.setVisible(false);
        trackDetailPane.setManaged(false);
    }

    @FXML
    private void onBackToTracks() {
        if (onBack != null) onBack.run();
    }

    @FXML
    private void onVisibilityChanged() {
        if (!ownerMode || track == null) return;
        String v = trackVisibilityCombo.getSelectionModel().getSelectedItem();
        if (v == null) return;

        try {
            trackService.updateVisibility(track.getId(), v);
            track.setVisibility(v);
            setError(null);
        } catch (SQLException e) {
            setError(e.getMessage());
        }
    }

    @FXML
    private void onCreateSnapshot() {
        if (!ownerMode || track == null) return;

        final PseudoClass ERROR_PC = PseudoClass.getPseudoClass("error");

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Create Snapshot");
        dialog.setHeaderText(null);

        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/com/example/guser/guser/workspace.css")
        ).toExternalForm());
        pane.getStyleClass().addAll("wsp-dialog", "wsp-signupDialog");
        pane.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        pane.setPrefWidth(520);

        Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
        if (okBtn != null) okBtn.setDisable(true);

        Label title = new Label("Snapshot message");
        title.getStyleClass().add("wsp-trackLabel");

        TextArea msgArea = new TextArea();
        msgArea.setPromptText("What did you accomplish today? (short summary)");
        msgArea.setPrefRowCount(4);
        msgArea.setWrapText(true);

        Label hint = new Label("Tip: keep it short and specific (1–2 sentences).");
        hint.getStyleClass().add("wsp-hint");

        Label error = new Label();
        error.getStyleClass().add("wsp-trackError");
        error.setManaged(false);
        error.setVisible(false);
        error.setWrapText(true);

        VBox card = new VBox(10, title, msgArea, hint, error);
        card.getStyleClass().add("wsp-trackCard");
        pane.setContent(card);

        Runnable validate = () -> {
            String msg = msgArea.getText() == null ? "" : msgArea.getText().trim();

            msgArea.pseudoClassStateChanged(ERROR_PC, false);
            error.setText("");
            error.setManaged(false);
            error.setVisible(false);

            String emsg = null;
            if (msg.isEmpty()) emsg = "Message is required.";
            else if (msg.length() > 280) emsg = "Message is too long (max 280 characters).";

            boolean ok = (emsg == null);
            if (okBtn != null) okBtn.setDisable(!ok);

            if (!ok) {
                msgArea.pseudoClassStateChanged(ERROR_PC, true);
                error.setText(emsg);
                error.setManaged(true);
                error.setVisible(true);
            }
        };

        msgArea.textProperty().addListener((obs, o, v) -> validate.run());
        validate.run();

        dialog.setResultConverter(bt -> bt == ButtonType.OK ? msgArea.getText().trim() : null);

        Optional<String> res = dialog.showAndWait();
        if (res.isEmpty()) return;

        String message = res.get();

        try {
            snapshotService.createSnapshot(
                    candidateId,
                    track.getId(),
                    viewerUserId,
                    "Snapshot",
                    message,
                    false
            );

            if (viewMode == ViewMode.PROGRESS) {
                refreshSnapshotsAndTimeline();
                if (!snapshots.isEmpty()) selectSnapshot(snapshots.get(0));
            }

            setError(null);
            AlertUtils.showSuccess("Snapshot created", "Your progress was recorded.");

        } catch (Exception e) {
            setError(e.getMessage());
            AlertUtils.showError("Could not create snapshot", e.getMessage());
        }
    }


    @FXML
    private void onAddArtifact() {
        if (!ownerMode || track == null) return;

        final PseudoClass ERROR_PC = PseudoClass.getPseudoClass("error");

        Dialog<ArtifactDraft> dialog = new Dialog<>();
        dialog.setTitle("Add Artifact");
        dialog.setHeaderText(null);

        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/com/example/guser/guser/workspace.css")
        ).toExternalForm());
        pane.getStyleClass().addAll("wsp-dialog", "wsp-signupDialog");
        pane.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        pane.setPrefWidth(560);

        Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
        if (okBtn != null) okBtn.setDisable(true);

        // --- Controls ---
        TextField nameField = new TextField();
        nameField.setPromptText("Artifact name (e.g., repo, report, demo)");

        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList(
                "CODE", "DOCUMENT", "IMAGE", "VIDEO", "AUDIO", "TEXT", "LINK"
        ));
        typeBox.getSelectionModel().select("CODE");

        TextField languageField = new TextField();
        languageField.setPromptText("Language (optional, for CODE)");

        TextArea textArea = new TextArea();
        textArea.setPromptText("Text (for TEXT) or URL (for LINK)");
        textArea.setPrefRowCount(4);
        textArea.setWrapText(true);

        TextArea descArea = new TextArea();
        descArea.setPromptText("Description (optional)");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);

        Label uploadHint = new Label(
                "For CODE/DOCUMENT/IMAGE/VIDEO/AUDIO you will upload a file (or folder→zip for CODE) after creating."
        );
        uploadHint.getStyleClass().add("wsp-hint");

        Label error = new Label();
        error.getStyleClass().add("wsp-trackError");
        error.setManaged(false);
        error.setVisible(false);
        error.setWrapText(true);

        // Layout: signup-like (label above control)
        VBox card = new VBox(12,
                fieldBox("Name", nameField),
                fieldBox("Type", typeBox),
                fieldBox("Language", languageField),
                fieldBox("Text / URL", textArea),
                fieldBox("Description", descArea),
                uploadHint,
                error
        );
        card.getStyleClass().add("wsp-trackCard");

        pane.setContent(card);

        // --- UX toggles based on type ---
        Runnable refreshFields = () -> {
            String type = safeUpper(typeBox.getValue());
            boolean isCode = "CODE".equals(type);
            boolean isText = "TEXT".equals(type);
            boolean isLink = "LINK".equals(type);
            boolean isTextLike = isText || isLink;

            languageField.setDisable(!isCode);
            textArea.setDisable(!isTextLike);

            // Don't aggressively clear user input; only clear when field becomes irrelevant
            if (!isCode) languageField.clear();
            if (!isTextLike) textArea.clear();

            if (isText) textArea.setPromptText("Write the text content…");
            else if (isLink) textArea.setPromptText("https://example.com");
            else textArea.setPromptText("Text (for TEXT) or URL (for LINK)");
        };
        typeBox.valueProperty().addListener((obs, o, v) -> refreshFields.run());
        refreshFields.run();

        // --- Validation + data control ---
        Runnable validate = () -> {
            // reset
            error.setText("");
            error.setManaged(false);
            error.setVisible(false);

            nameField.pseudoClassStateChanged(ERROR_PC, false);
            typeBox.pseudoClassStateChanged(ERROR_PC, false);
            languageField.pseudoClassStateChanged(ERROR_PC, false);
            textArea.pseudoClassStateChanged(ERROR_PC, false);
            descArea.pseudoClassStateChanged(ERROR_PC, false);

            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            String type = safeUpper(typeBox.getValue());
            String lang = languageField.getText() == null ? "" : languageField.getText().trim();
            String txt = textArea.getText() == null ? "" : textArea.getText().trim();
            String desc = descArea.getText() == null ? "" : descArea.getText().trim();

            String emsg = null;

            // common rules
            if (name.isEmpty()) emsg = "Name is required.";
            else if (name.length() > 60) emsg = "Name is too long (max 60 characters).";
            else if (type.isEmpty()) emsg = "Type is required.";
            else if (desc.length() > 600) emsg = "Description is too long (max 600 characters).";

            // type-specific rules
            if (emsg == null && "CODE".equals(type)) {
                if (lang.length() > 40) emsg = "Language is too long (max 40 characters).";
            }
            if (emsg == null && ("TEXT".equals(type))) {
                if (txt.isEmpty()) emsg = "Text content is required for type TEXT.";
                else if (txt.length() > 5000) emsg = "Text is too long (max 5000 characters).";
            }
            if (emsg == null && ("LINK".equals(type))) {
                if (txt.isEmpty()) emsg = "URL is required for type LINK.";
                else if (!isValidHttpUrl(txt)) emsg = "Please enter a valid URL starting with http:// or https://";
                else if (txt.length() > 1000) emsg = "URL is too long (max 1000 characters).";
            }

            boolean ok = (emsg == null);
            if (okBtn != null) okBtn.setDisable(!ok);

            if (!ok) {
                // highlight likely culprit
                if (emsg.startsWith("Name")) nameField.pseudoClassStateChanged(ERROR_PC, true);
                else if (emsg.startsWith("Type")) typeBox.pseudoClassStateChanged(ERROR_PC, true);
                else if (emsg.startsWith("Language")) languageField.pseudoClassStateChanged(ERROR_PC, true);
                else if (emsg.contains("Text") || emsg.contains("URL")) textArea.pseudoClassStateChanged(ERROR_PC, true);
                else if (emsg.startsWith("Description")) descArea.pseudoClassStateChanged(ERROR_PC, true);

                error.setText(emsg);
                error.setManaged(true);
                error.setVisible(true);
            }
        };

        nameField.textProperty().addListener((obs, o, v) -> validate.run());
        typeBox.valueProperty().addListener((obs, o, v) -> validate.run());
        languageField.textProperty().addListener((obs, o, v) -> validate.run());
        textArea.textProperty().addListener((obs, o, v) -> validate.run());
        descArea.textProperty().addListener((obs, o, v) -> validate.run());
        validate.run();

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;

            ArtifactDraft d = new ArtifactDraft();
            d.name = nameField.getText().trim();
            d.description = emptyToNull(descArea.getText());
            d.type = safeUpper(typeBox.getValue());
            d.language = emptyToNull(languageField.getText());
            d.textContent = emptyToNull(textArea.getText());
            return d;
        });

        Optional<ArtifactDraft> res = dialog.showAndWait();
        if (res.isEmpty()) return;

        ArtifactDraft d = res.get();

        try {
            Artifact created = artifactService.create(
                    track.getId(),
                    d.name,
                    d.description,
                    d.type,
                    ("CODE".equals(d.type) ? emptyToNull(d.language) : null),
                    (("TEXT".equals(d.type) || "LINK".equals(d.type)) ? emptyToNull(d.textContent) : null)
            );

            // Upload for file-based artifacts
            if ("CODE".equals(d.type)) {
                promptUploadForCode(created);
            } else if ("DOCUMENT".equals(d.type) || "IMAGE".equals(d.type) || "VIDEO".equals(d.type) || "AUDIO".equals(d.type)) {
                promptUploadSingleFile(created, d.type);
            }

            // Refresh UI depending on mode
            if (viewMode == ViewMode.CURRENT) {
                refreshCurrentArtifacts();
                showPlaceholder("Select an artifact.");
                AlertUtils.showSuccess("Artifact added", "Upload now if this artifact requires a file.");
            } else {
                refreshSnapshotsAndTimeline();
                if (!snapshots.isEmpty()) selectSnapshot(snapshots.get(0));
                else {
                    selectedSnapshot = null;
                    artifactRows.clear();
                    showPlaceholder("No snapshots yet. Create one.");
                }
                AlertUtils.showSuccess("Artifact added", "Create a new snapshot to record it in your timeline.");
            }

            setError(null);

        } catch (Exception e) {
            setError(e.getMessage());
            AlertUtils.showError("Could not add artifact", e.getMessage());
        }
    }

    /* ------- helpers ------- */

    private VBox fieldBox(String labelText, Node control) {
        Label l = new Label(labelText);
        l.getStyleClass().add("wsp-trackLabel");
        VBox box = new VBox(6, l, control);
        return box;
    }

    private boolean isValidHttpUrl(String s) {
        try {
            URI u = URI.create(s.trim());
            String scheme = u.getScheme();
            return (scheme != null) && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    && u.getHost() != null;
        } catch (Exception e) {
            return false;
        }
    }



    private static class ArtifactDraft {
        String name;
        String description;
        String type;
        String language;
        String textContent;
    }



    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }


    private void refreshSnapshotsAndTimeline() {
        try {
            List<Snapshot> list = snapshotService.listByTrack(track.getId());
            // Option A: oldest -> newest
            list.sort(Comparator.comparing(Snapshot::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));

            snapshots.setAll(list);

            snapshotJumpCombo.setItems(FXCollections.observableArrayList(snapshots));

            buildTimelineSegments();

            setError(null);
        } catch (SQLException e) {
            setError(e.getMessage());
        }
    }


    private void buildTimelineSegments() {
        timelineSegmentsBox.getChildren().clear();
        snapshotByToggle.clear();
        timelineGroup.getToggles().clear();

        if (snapshots.isEmpty()) {
            selectedSnapshotTitleLabel.setText("");
            selectedSnapshotMetaLabel.setText("");
            return;
        }

        for (Snapshot s : snapshots) {
            ToggleButton seg = new ToggleButton();
            seg.getStyleClass().add("wsp-seg");
            seg.setToggleGroup(timelineGroup);
            seg.setFocusTraversable(false);
            seg.setText(""); // segment only

            Tooltip tt = new Tooltip(formatSnapshotTooltip(s));
            Tooltip.install(seg, tt);

            snapshotByToggle.put(seg, s);

            seg.setOnAction(e -> selectSnapshot(s));

            timelineSegmentsBox.getChildren().add(seg);
        }
    }
    private String formatSnapshotLabel(Snapshot s) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String date = (s.getCreatedAt() == null) ? "" : fmt.format(s.getCreatedAt());
        String msg = nullToEmpty(s.getMessage()).trim();
        if (msg.length() > 50) msg = msg.substring(0, 50) + "…";
        return date + " — " + (msg.isEmpty() ? "Snapshot" : msg);
    }

    private String formatSnapshotTooltip(Snapshot s) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String date = (s.getCreatedAt() == null) ? "" : fmt.format(s.getCreatedAt());
        return s.getTitle() + "\n" + date + "\n" + nullToEmpty(s.getMessage());
    }




    private void selectSnapshot(Snapshot s) {
        closePreview();
        hidePreviewPane();

        selectedSnapshot = s;

        selectedSnapshotTitleLabel.setText(
                (s.getMessage() == null || s.getMessage().isBlank()) ? "Snapshot" : s.getMessage().trim()
        );

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE, MMM dd yyyy • HH:mm");
        String date = (s.getCreatedAt() == null) ? "" : fmt.format(s.getCreatedAt());

        selectedSnapshotMetaLabel.setText(date + "   •   " + (s.isFinal() ? "FINAL" : "SNAPSHOT"));


        for (Toggle t : timelineGroup.getToggles()) {
            if (snapshotByToggle.get(t) == s) {
                timelineGroup.selectToggle(t);
                break;
            }
        }

        refreshSnapshotArtifacts(s);
    }

    private void selectPrevSnapshot() {
        if (snapshots.isEmpty() || selectedSnapshot == null) return;
        int idx = snapshots.indexOf(selectedSnapshot);
        if (idx <= 0) return;
        selectSnapshot(snapshots.get(idx - 1));
    }

    private void selectNextSnapshot() {
        if (snapshots.isEmpty() || selectedSnapshot == null) return;
        int idx = snapshots.indexOf(selectedSnapshot);
        if (idx < 0 || idx >= snapshots.size() - 1) return;
        selectSnapshot(snapshots.get(idx + 1));
    }
    private void refreshCurrentArtifacts() {
        try {
            List<Artifact> artifacts = artifactService.listActiveByTrack(track.getId());
            List<ArtifactRow> rows = buildGroupedRows(artifacts);

            // In current mode fileObjectId is not precomputed
            for (ArtifactRow r : rows) if (r.kind == ArtifactRowKind.ITEM) r.fileObjectId = null;

            artifactRows.setAll(rows);
            setError(null);
        } catch (Exception e) {
            setError(e.getMessage());
        }
    }

    private void refreshSnapshotArtifacts(Snapshot s) {
        try {
            List<SnapshotItemService.SnapshotArtifactRow> inSnap =
                    snapshotItemService.listArtifactsInSnapshot(s.getId());

            List<Artifact> artifacts = inSnap.stream().map(r -> r.artifact).toList();
            List<ArtifactRow> rows = buildGroupedRows(artifacts);

            Map<Integer, Integer> fileIdByArtifactId = new HashMap<>();
            for (var r : inSnap) fileIdByArtifactId.put(r.artifact.getId(), r.fileObjectId);

            for (ArtifactRow r : rows) {
                if (r.kind == ArtifactRowKind.ITEM) {
                    r.fileObjectId = fileIdByArtifactId.get(r.artifact.getId());
                }
            }

            artifactRows.setAll(rows);
            setError(null);
        } catch (Exception e) {
            setError(e.getMessage());
        }
    }
    private void openArtifactRowByMode(ArtifactRow row) {
        if (viewMode == ViewMode.CURRENT) {
            openArtifactCurrent(row.artifact);
        } else {
            openArtifactSnapshot(row);
        }
    }
    private void openArtifactCurrent(Artifact artifact) {
        try {
            // For TEXT/LINK you can still show content even if no file uploaded
            String type = safeUpper(artifact.getArtifactType());
            if ("TEXT".equals(type)) {
                openTextPreviewOnly(artifact);
                return;
            }
            if ("LINK".equals(type)) {
                openLinkPreviewOnly(artifact);
                return;
            }

            FileObject latest = fileObjectService.findLatestByArtifact(artifact.getId());
            if (latest == null) {
                showPlaceholder("No file uploaded for this artifact yet.");
                return;
            }
            openByTypeWithFileObjectId(artifact, latest.getId());
        } catch (Exception e) {
            setError(e.getMessage());
        }
    }

    private void openArtifactSnapshot(ArtifactRow row) {
        if (row.fileObjectId == null) {
            showPlaceholder("This artifact has no file in this snapshot.");
            return;
        }
        openByTypeWithFileObjectId(row.artifact, row.fileObjectId);
    }


    private void refreshArtifactsForSnapshot(Snapshot s) {
        try {
            List<Artifact> artifacts = artifactService.listActiveByTrack(track.getId());
            List<ArtifactRow> rows = buildGroupedRows(artifacts);

            for (ArtifactRow r : rows) {
                if (r.kind == ArtifactRowKind.ITEM) {
                    int fileObjectId = snapshotItemService.findFileObjectId(s.getId(), r.artifact.getId());
                    r.fileObjectId = (fileObjectId == 0 ? null : fileObjectId);
                }
            }

            artifactRows.setAll(rows);

            ArtifactRow firstWithVersion = artifactRows.stream()
                    .filter(r -> r.kind == ArtifactRowKind.ITEM && r.fileObjectId != null)
                    .findFirst()
                    .orElse(null);

            if (firstWithVersion != null) artifactsListView.getSelectionModel().select(firstWithVersion);
            else showPlaceholder("No artifact versions captured in this snapshot yet.");

            setError(null);
        } catch (SQLException e) {
            setError(e.getMessage());
        }
    }





    private List<ArtifactRow> buildGroupedRows(List<Artifact> artifacts) {
        List<String> order = List.of("CODE", "DOCUMENT", "IMAGE", "VIDEO", "LINK", "TEXT");
        Map<String, List<Artifact>> byType = artifacts.stream()
                .collect(Collectors.groupingBy(a -> safeUpper(a.getArtifactType()), LinkedHashMap::new, Collectors.toList()));

        List<ArtifactRow> out = new ArrayList<>();
        for (String t : order) {
            List<Artifact> list = byType.get(t);
            if (list == null || list.isEmpty()) continue;
            out.add(ArtifactRow.header(typeLabel(t)));
            for (Artifact a : list) out.add(ArtifactRow.item(a));
        }
        for (Map.Entry<String, List<Artifact>> e : byType.entrySet()) {
            if (order.contains(e.getKey())) continue;
            out.add(ArtifactRow.header(typeLabel(e.getKey())));
            for (Artifact a : e.getValue()) out.add(ArtifactRow.item(a));
        }
        return out;
    }


    private void openByTypeWithFileObjectId(Artifact artifact, int fileObjectId) {
        String type = safeUpper(artifact.getArtifactType());
        switch (type) {
            case "CODE" -> openCodeArtifact(fileObjectId);

            case "TEXT" -> openTextArtifactFromSnapshotFile(artifact, fileObjectId);

            case "IMAGE" -> openImageArtifact(artifact, fileObjectId);

            case "DOCUMENT" -> showDownloadPanel(
                    artifact.getArtifactName() + " (Document)",
                    fileObjectId,
                    artifact.getArtifactName()
            );

            case "VIDEO" -> openVideoArtifact(artifact, fileObjectId);
            case "AUDIO" -> openVideoArtifact(artifact, fileObjectId); //media player works with mp3 wav

            case "LINK" -> openLinkPreviewOnly(artifact);

            default -> showDownloadPanel(
                    artifact.getArtifactName(),
                    fileObjectId,
                    artifact.getArtifactName()
            );
        }
    }
    private void openTextPreviewOnly(Artifact artifact) {
        TextArea ta = new TextArea(artifact.getTextContent() == null ? "" : artifact.getTextContent());
        ta.setEditable(false);
        ta.setWrapText(true);
        VBox box = new VBox(10, ta);
        box.getStyleClass().add("wsp-viewBox");
        artifactViewerHost.getChildren().setAll(box);
    }

    private void openLinkPreviewOnly(Artifact artifact) {
        String urlText = artifact.getTextContent() == null ? "" : artifact.getTextContent().trim();
        Hyperlink link = new Hyperlink(urlText.isBlank() ? "(empty link)" : urlText);
        link.getStyleClass().add("wsp-link");
        link.setOnAction(e -> {
            try {
                if (!urlText.isBlank()) com.example.guser.AppHostServices.get().showDocument(urlText);
            } catch (Exception ex) {
                setError(ex.getMessage());
            }
        });
        VBox box = new VBox(10, link);
        box.getStyleClass().add("wsp-viewBox");
        artifactViewerHost.getChildren().setAll(box);
    }

    private void openTextArtifactFromSnapshotFile(Artifact artifact, int fileObjectId) {
        // Keep your existing behavior: show textContent + download
        // (you can later improve by fetching the snapshot file and showing it)
        TextArea ta = new TextArea(artifact.getTextContent() == null ? "" : artifact.getTextContent());
        ta.setEditable(false);
        ta.setWrapText(true);

        Button dl = new Button("Download file");
        dl.getStyleClass().add("prf-outlineBtn");
        dl.setOnAction(e -> {
            try {
                downloadToDisk(fileObjectId, artifact.getArtifactName());
                setError(null);
            } catch (Exception ex) {
                setError(ex.getMessage());
            }
        });

        VBox box = new VBox(10, ta, dl);
        box.getStyleClass().add("wsp-viewBox");
        artifactViewerHost.getChildren().setAll(box);
    }

    private void openVideoArtifact(Artifact artifact, int fileObjectId) {
        closePreview();

        try {
            FileObject fo = fileObjectService.findById(fileObjectId);
            if (fo == null) { showPlaceholder("Missing file record."); return; }

            String url = fileObjectService.presignedDownloadUrl(fo.getStorageKey(), Duration.ofMinutes(10));

            Media media = new Media(url);
            MediaPlayer player = new MediaPlayer(media);
            currentPlayer = player;

            MediaView mv = new MediaView(player);
            if("VIDEO".equals(artifact.getArtifactType())){
                mv.setSmooth(true);
                // Fill width, but cap height
                mv.fitWidthProperty().bind(artifactViewerHost.widthProperty().subtract(24));
                mv.fitHeightProperty().bind(artifactViewerHost.heightProperty().multiply(0.70)); // only ~55% of preview
                //mv.setFitHeight(420); // hard cap for big windows
            }
            mv.setPreserveRatio(true);


            StackPane videoStage = new StackPane(mv);
            videoStage.getStyleClass().add("wsp-videoStage");

            Button play = new Button("Play");
            play.getStyleClass().add("prf-actBtn");
            play.setOnAction(e -> player.play());

            Button pause = new Button("Pause");
            pause.getStyleClass().add("prf-outlineBtn");
            pause.setOnAction(e -> player.pause());

            Button fs = new Button("Fullscreen");
            fs.getStyleClass().add("prf-outlineBtn");
            fs.setOnAction(e -> {
                Window scene = mv.getScene().getWindow();
                mv.fitWidthProperty().bind(scene.widthProperty());
                mv.fitHeightProperty().bind(scene.heightProperty());
                mv.setPreserveRatio(true);
            });


            Button download = new Button("Download");
            download.getStyleClass().add("wsp-downloadBtn");
            download.setOnAction(e -> {
                try { downloadToDisk(fileObjectId, artifact.getArtifactName()); }
                catch (Exception ex) { setError(ex.getMessage()); }
            });

            HBox controls = new HBox(10, play, pause, fs, download);
            controls.setAlignment(Pos.CENTER_LEFT);
            controls.getStyleClass().add("wsp-videoControls");

            VBox box = new VBox(10);
            box.getChildren().addAll(videoStage, controls);
            box.getStyleClass().addAll("wsp-viewBox", "wsp-videoBox");

            artifactViewerHost.getChildren().setAll(box);

            player.play();

        } catch (MediaException mx) {
            showVideoFallback(artifact, fileObjectId, mx.getMessage());
        } catch (Exception e) {
            showVideoFallback(artifact, fileObjectId, e.getMessage());
        }
    }

    private void showVideoFallback(Artifact artifact, int fileObjectId, String err) {
        try {
            FileObject fo = fileObjectService.findById(fileObjectId);
            String url = (fo == null) ? null : fileObjectService.presignedDownloadUrl(fo.getStorageKey(), Duration.ofMinutes(10));

            Label title = new Label("Video preview not supported on this machine");
            title.getStyleClass().add("wsp-viewTitle");

            Label msg = new Label(err == null ? "" : err);
            msg.getStyleClass().add("prf-muted");
            msg.setWrapText(true);

            Button openExternal = new Button("Open externally");
            openExternal.getStyleClass().add("prf-actBtn");
            openExternal.setDisable(url == null || url.isBlank());
            openExternal.setOnAction(e -> com.example.guser.AppHostServices.get().showDocument(url));

            Button download = new Button("Download");
            download.getStyleClass().add("wsp-downloadBtn");
            download.setOnAction(e -> {
                try { downloadToDisk(fileObjectId, artifact.getArtifactName()); }
                catch (Exception ex) { setError(ex.getMessage()); }
            });

            VBox box = new VBox(10, title, msg, new HBox(10, openExternal, download));
            box.getStyleClass().add("wsp-viewBox");

            artifactViewerHost.getChildren().setAll(box);
        } catch (Exception e) {
            showPlaceholder("Video preview failed.");
        }
    }



    private static String safeFileName(String base, String extNoDot) {
        String b = (base == null || base.isBlank()) ? "artifact" : base.trim();
        b = b.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (extNoDot != null && !extNoDot.isBlank()) {
            String ext = extNoDot.toLowerCase();
            if (!b.toLowerCase().endsWith("." + ext)) b = b + "." + ext;
        }
        return b;
    }

    private void showDownloadPanel(String title, int fileObjectId, String suggestedName) {
        Label t = new Label(title);
        t.getStyleClass().add("wsp-viewTitle");

        Button dl = new Button("Download");
        dl.getStyleClass().add("prf-outlineBtn");
        dl.getStyleClass().add("wsp-downloadBtn");

        dl.setOnAction(e -> {
            try {
                downloadToDisk(fileObjectId, suggestedName);
                setError(null);
            } catch (Exception ex) {
                setError(ex.getMessage());
            }
        });

        VBox box = new VBox(10, t, dl);
        box.getStyleClass().add("wsp-viewBox");
        artifactViewerHost.getChildren().setAll(box);
    }

    private void openImageArtifact(Artifact artifact, int fileObjectId) {
        try {
            FileObject fo = fileObjectService.findById(fileObjectId);
            if (fo == null) { showPlaceholder("Missing file_object record."); return; }

            String url = fileObjectService.presignedDownloadUrl(fo.getStorageKey(), Duration.ofMinutes(10));

            ImageView iv = new ImageView(new Image(url, true));
            iv.setPreserveRatio(true);
            iv.setSmooth(true);

            iv.fitWidthProperty().bind(artifactViewerHost.widthProperty().subtract(60));
            iv.fitHeightProperty().bind(artifactViewerHost.heightProperty().subtract(140));





            Button dl = new Button("Download");
            dl.getStyleClass().add("prf-outlineBtn");
            dl.getStyleClass().add("wsp-downloadBtn");

            dl.setOnAction(e -> {
                try {
                    downloadToDisk(fileObjectId, safeFileName(artifact.getArtifactName(), "png"));
                    setError(null);
                } catch (Exception ex) {
                    setError(ex.getMessage());
                }
            });

            VBox box = new VBox(10, iv, dl);
            box.getStyleClass().add("wsp-viewBox");
            artifactViewerHost.getChildren().setAll(box);
            setError(null);
        } catch (Exception e) {
            setError(e.getMessage());
        }
    }



    private void openTextLikeArtifact(Artifact artifact, int fileObjectId) {
        String txt = artifact.getTextContent() == null ? "" : artifact.getTextContent();

        TextArea ta = new TextArea(txt);
        ta.setEditable(false);
        ta.setWrapText(true);

        VBox box = new VBox(10);
        box.getStyleClass().add("wsp-viewBox");
        box.getChildren().add(ta);

        if ("LINK".equalsIgnoreCase(artifact.getArtifactType())) {
            Button open = new Button("Open link");
            open.getStyleClass().add("prf-outlineBtn");
            open.setOnAction(e -> {
                try {
                    String url = txt.trim();
                    if (!url.isBlank()) com.example.guser.AppHostServices.get().showDocument(url);
                } catch (Exception ex) {
                    setError(ex.getMessage());
                }
            });
            box.getChildren().add(open);
        }

        Button dl = new Button("Download snapshot file");
        dl.getStyleClass().add("prf-outlineBtn");
        dl.getStyleClass().add("wsp-downloadBtn");
        dl.setOnAction(e -> {
            try {
                downloadToDisk(fileObjectId, safeFileName(artifact.getArtifactName(), "txt"));
                setError(null);
            } catch (Exception ex) {
                setError(ex.getMessage());
            }
        });
        box.getChildren().add(dl);

        artifactViewerHost.getChildren().setAll(box);
    }


    private void openCodeArtifact(int fileObjectId) {
        try {
            FileObject fo = fileObjectService.findById(fileObjectId);
            if (fo == null) { showPlaceholder("Missing file_object record."); return; }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/guser/guser/code_viewer.fxml"));
            Node node = loader.load();

            CodeViewerController c = loader.getController();
            c.init(codeBrowseService, fileObjectService, fo.getStorageKey());

            node.setUserData(c); // <-- add this line
            artifactViewerHost.getChildren().setAll(node);
            setError(null);
        } catch (Exception e) {
            setError(e.getMessage());
        }
    }


    private void showPlaceholder(String msg) {
        viewerPlaceholderLabel.setText(msg);
        artifactViewerHost.getChildren().setAll(viewerPlaceholderLabel);
    }


    private String iconForType(String t) {
        return switch (safeUpper(t)) {
            case "CODE" -> "📁";
            case "DOCUMENT" -> "📄";
            case "IMAGE" -> "🖼";
            case "VIDEO" -> "🎬";
            case "AUDIO" -> "🎬";
            case "LINK" -> "🔗";
            case "TEXT" -> "📝";
            default -> "•";
        };
    }
    private void renameArtifact(Artifact a) {
        if (!ownerMode) return;

        TextInputDialog d = new TextInputDialog(a.getArtifactName());
        d.setTitle("Rename artifact");
        d.setHeaderText("Rename artifact");
        d.setContentText("New name:");

        // Attach your app stylesheet to this dialog
        d.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/example/guser/guser/workspace.css").toExternalForm()
        );
        d.getDialogPane().getStyleClass().add("wsp-dialog"); // optional, if you added CSS rules

        Optional<String> res = d.showAndWait();
        if (res.isEmpty()) return;

        String newName = res.get() == null ? "" : res.get().trim();
        if (newName.isBlank()) {
            showInfo("Name cannot be empty.");
            return;
        }

        try {
            artifactService.rename(a.getId(), newName); // you added rename(...) in ArtifactService
            closePreview();

            if (viewMode == ViewMode.CURRENT) refreshCurrentArtifacts();
            else if (selectedSnapshot != null) refreshSnapshotArtifacts(selectedSnapshot);

            setError(null);
        } catch (Exception e) {
            setError(e.getMessage());
        }
    }

    private void deleteArtifact(Artifact a) {
        if (!ownerMode) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete artifact");
        confirm.setHeaderText("Delete \"" + nullToEmpty(a.getArtifactName()) + "\"?");
        confirm.setContentText("This removes it from the current track.");

        // Custom buttons so we can style “Delete”
        ButtonType deleteBt = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBt = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(deleteBt, cancelBt);

        // Attach your app stylesheet to this dialog
        confirm.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/example/guser/guser/workspace.css").toExternalForm()
        );
        confirm.getDialogPane().getStyleClass().add("wsp-dialog"); // optional

        // Optional: make Delete red (requires .wsp-dangerBtn CSS rule)
        Node deleteBtn = confirm.getDialogPane().lookupButton(deleteBt);
        if (deleteBtn != null) deleteBtn.getStyleClass().add("wsp-dangerBtn");

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != deleteBt) return;

        try {
            artifactService.softDelete(a.getId());
            closePreview();

            if (viewMode == ViewMode.CURRENT) refreshCurrentArtifacts();
            else if (selectedSnapshot != null) refreshSnapshotArtifacts(selectedSnapshot);

            setError(null);
        } catch (Exception e) {
            setError(e.getMessage());
        }
    }


    private void installArtifactCells() {
        artifactsListView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(ArtifactRow row, boolean empty) {
                super.updateItem(row, empty);

                setText(null);
                setGraphic(null);
                setDisable(false);
                setContextMenu(null);

                getStyleClass().remove("wsp-headerCell");

                if (empty || row == null) return;

                if (row.kind == ArtifactRowKind.HEADER) {
                    Label pill = new Label(row.headerTitle);
                    pill.getStyleClass().add("wsp-headerPill");

                    setGraphic(pill);
                    setDisable(true);
                    getStyleClass().add("wsp-headerCell");
                    return;
                }

                // ITEM ROW
                Artifact a = row.artifact;
                String type = safeUpper(a.getArtifactType());

                Label icon = new Label(iconForType(type));
                icon.getStyleClass().add("wsp-artIcon");

                Label name = new Label(nullToEmpty(a.getArtifactName()));
                name.getStyleClass().add("wsp-artName");

                String metaTxt = type
                        + ((a.getLanguage() == null || a.getLanguage().isBlank()) ? "" : (" • " + a.getLanguage()));

                // If snapshot mode and no file: show hint
                boolean missingInSnapshot = (viewMode == ViewMode.PROGRESS && selectedSnapshot != null && row.fileObjectId == null);
                if (missingInSnapshot) metaTxt += " • No version in snapshot";

                Label meta = new Label(metaTxt);
                meta.getStyleClass().add("wsp-artMeta");

                VBox texts = new VBox(2, name, meta);

                // 3-dots menu button
                Button moreBtn = new Button("⋯");
                moreBtn.getStyleClass().add("wsp-moreBtn");
                moreBtn.setFocusTraversable(false);
                moreBtn.setMinWidth(34);
                moreBtn.setPrefWidth(34);

                ContextMenu cm = buildArtifactMenu(row);

                moreBtn.setOnAction(e -> {
                    if (cm == null) return;
                    cm.show(moreBtn, Side.BOTTOM, 0, 0);
                });

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox root = new HBox(10.0, (Node) icon, (Node) texts, (Node) spacer, moreBtn);
                root.setAlignment(Pos.CENTER_LEFT);
                root.setPadding(new Insets(2, 6, 2, 6));

                setGraphic(root);

                // Right-click context menu
                setContextMenu(cm);

                // Also allow right-click anywhere on row graphic (some OS don’t show contextMenu reliably)
                root.setOnMousePressed(me -> {
                    if (me.getButton() == MouseButton.SECONDARY) {
                        if (cm != null) cm.show(root, me.getScreenX(), me.getScreenY());
                        me.consume();
                    }
                });
            }
        });
    }


    public static String humanSize(long bytes) {
        if (bytes < 0) return "—";
        final long KiB = 1024L;
        final long MiB = KiB * 1024L;
        final long GiB = MiB * 1024L;
        final long TiB = GiB * 1024L;

        if (bytes >= TiB) return String.format(Locale.US, "%.1f TiB", (double) bytes / TiB);
        if (bytes >= GiB) return String.format(Locale.US, "%.1f GiB", (double) bytes / GiB);
        if (bytes >= MiB) return String.format(Locale.US, "%.1f MiB", (double) bytes / MiB);
        if (bytes >= KiB) return String.format(Locale.US, "%.1f KiB", (double) bytes / KiB);
        return bytes + " B";
    }



    private void setError(String msg) {
        boolean show = msg != null && !msg.isBlank();
        trackErrorLabel.setText(show ? msg : "");
        trackErrorLabel.setVisible(show);
        trackErrorLabel.setManaged(show);
    }

    private String typeLabel(String t) {
        return switch (t) {
            case "CODE" -> "Code";
            case "DOCUMENT" -> "Documents";
            case "IMAGE" -> "Images";
            case "VIDEO" -> "Videos";
            case "AUDIO" -> "Audios";
            case "LINK" -> "Links";
            case "TEXT" -> "Text";
            default -> t;
        };
    }

    private static String safeUpper(String s) { return s == null ? "" : s.trim().toUpperCase(); }
    private static String nullToEmpty(String s) { return s == null ? "" : s; }
    private ContextMenu buildArtifactMenu(ArtifactRow row) {
        String type = safeUpper(row.artifact.getArtifactType());
        boolean canEdit = ownerMode && viewMode == ViewMode.CURRENT; // editing only in CURRENT

        MenuItem upload = new MenuItem("Upload new version…");
        upload.setDisable(!canEdit);
        upload.setOnAction(e -> {
            try {
                if ("CODE".equals(type)) {
                    promptUploadForCode(row.artifact);
                } else if ("DOCUMENT".equals(type) || "IMAGE".equals(type) || "VIDEO".equals(type)|| "AUDIO".equals(type)) {
                    promptUploadSingleFile(row.artifact, type);
                } else {
                    showInfo("This artifact type has no file upload.");
                }
                setError(null);
                refreshCurrentArtifacts();
            } catch (Exception ex) {
                setError(ex.getMessage());
            }
        });

        MenuItem download = new MenuItem(viewMode == ViewMode.PROGRESS ? "Download snapshot version" : "Download latest");
        download.setOnAction(e -> {
            try {
                Integer fid = row.fileObjectId;
                if (fid == null) {
                    showInfo(viewMode == ViewMode.PROGRESS
                            ? "No file in this snapshot for this artifact."
                            : "No file uploaded yet for this artifact.");
                    return;
                }
                downloadToDisk(fid, row.artifact.getArtifactName());
                setError(null);
            } catch (Exception ex) {
                setError(ex.getMessage());
            }
        });

        MenuItem openLink = new MenuItem("Open link");
        openLink.setVisible("LINK".equals(type));
        openLink.setOnAction(e -> {
            try {
                String url = row.artifact.getTextContent() == null ? "" : row.artifact.getTextContent().trim();
                if (!url.isBlank()) com.example.guser.AppHostServices.get().showDocument(url);
            } catch (Exception ex) {
                setError(ex.getMessage());
            }
        });

        MenuItem rename = new MenuItem("Rename…");
        rename.setDisable(!canEdit);
        rename.setOnAction(e -> renameArtifact(row.artifact));

        MenuItem delete = new MenuItem("Delete…");
        delete.setDisable(!canEdit);
        delete.setOnAction(e -> deleteArtifact(row.artifact));

        ContextMenu cm = new ContextMenu();

        // Order feels good
        if (ownerMode) cm.getItems().addAll(upload, rename);
        cm.getItems().add(download);
        if ("LINK".equals(type)) cm.getItems().add(openLink);
        if (ownerMode) cm.getItems().add(delete);

        return cm;
    }



    // --- UI row type: no new VM files needed ---
    private enum ArtifactRowKind { HEADER, ITEM }

    private static class ArtifactRow {
        final ArtifactRowKind kind;
        final String headerTitle;
        final Artifact artifact;
        Integer fileObjectId;
        Long fileSize;

        private ArtifactRow(ArtifactRowKind kind, String headerTitle, Artifact artifact) {
            this.kind = kind;
            this.headerTitle = headerTitle;
            this.artifact = artifact;
        }
        static ArtifactRow header(String title) { return new ArtifactRow(ArtifactRowKind.HEADER, title, null); }
        static ArtifactRow item(Artifact a) { return new ArtifactRow(ArtifactRowKind.ITEM, null, a); }
    }

    private boolean overLimit(File f) {
        return f != null && f.length() > MAX_UPLOAD_BYTES;
    }

    private void showInfo(String msg) {
        setError(msg); // reuse your error label area as info for now
    }

    private void promptUploadForCode(Artifact artifact) throws Exception {
        Alert choice = new Alert(Alert.AlertType.CONFIRMATION);
        choice.setTitle("Upload code");
        choice.setHeaderText("Choose upload method");

        ButtonType folderBtn = new ButtonType("Choose folder (zip)");
        ButtonType zipBtn = new ButtonType("Choose ZIP");
        ButtonType fileBtn = new ButtonType("Choose single file");
        ButtonType cancel = ButtonType.CANCEL;

        // Put cancel last (more natural)
        choice.getButtonTypes().setAll(folderBtn, zipBtn, fileBtn, cancel);

        // Optional: apply your dialog stylesheet (same pattern you use elsewhere)
        choice.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/example/guser/guser/workspace.css").toExternalForm()
        );
        choice.getDialogPane().getStyleClass().add("wsp-dialog");

        Optional<ButtonType> res = choice.showAndWait();
        if (res.isEmpty() || res.get() == cancel) return;

        // 1) Folder -> zip
        if (res.get() == folderBtn) {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select project folder");
            File dir = dc.showDialog(trackDetailPane.getScene().getWindow());
            if (dir == null) return;

            File zipped = zipDirectoryToTemp(dir.toPath());
            try {
                if (overLimit(zipped)) throw new IllegalArgumentException("ZIP is larger than 50MB.");
                fileObjectService.uploadNewVersion(candidateId, track.getId(), artifact.getId(), zipped);
            } finally {
                try { zipped.delete(); } catch (Exception ignored) {}
            }

            showInfo("Code uploaded. Now create a snapshot to freeze this version.");
            return;
        }

        // 2) ZIP upload
        if (res.get() == zipBtn) {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select ZIP");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP files", "*.zip"));

            File zip = fc.showOpenDialog(trackDetailPane.getScene().getWindow());
            if (zip == null) return;

            if (overLimit(zip)) throw new IllegalArgumentException("File is larger than 50MB.");

            fileObjectService.uploadNewVersion(candidateId, track.getId(), artifact.getId(), zip);
            showInfo("ZIP uploaded. Now create a snapshot to freeze this version.");
            return;
        }

        // 3) Single file upload
        if (res.get() == fileBtn) {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select code file");

            // Keep it flexible: show common code extensions, but allow any file.
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Code files",
                            "*.java", "*.kt", "*.py", "*.js", "*.ts", "*.tsx", "*.jsx",
                            "*.html", "*.css", "*.scss",
                            "*.json", "*.xml", "*.yml", "*.yaml",
                            "*.md", "*.txt",
                            "*.sql",
                            "*.c", "*.cpp", "*.h", "*.hpp",
                            "*.cs", "*.go", "*.php", "*.rb", "*.rs"
                    ),
                    new FileChooser.ExtensionFilter("All files", "*.*")
            );

            File f = fc.showOpenDialog(trackDetailPane.getScene().getWindow());
            if (f == null) return;

            if (overLimit(f)) throw new IllegalArgumentException("File is larger than 50MB.");

            fileObjectService.uploadNewVersion(candidateId, track.getId(), artifact.getId(), f);
            showInfo("File uploaded. Now create a snapshot to freeze this version.");
        }
    }


    private void promptUploadSingleFile(Artifact artifact, String type) throws Exception {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select file to upload");

        // Simple filters (you can expand later)
        if ("DOCUMENT".equals(type)) {
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.docx", "*.pptx", "*.txt"),
                    new FileChooser.ExtensionFilter("All files", "*.*")
            );
        } else if ("IMAGE".equals(type)) {
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"),
                    new FileChooser.ExtensionFilter("All files", "*.*")
            );
        } else if ("VIDEO".equals(type)) {
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.mov"),
                    new FileChooser.ExtensionFilter("All files", "*.*")
            );
        }
        else if ("AUDIO".equals(type)) {
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Audios", "*.mp3", "*.wav"),
                    new FileChooser.ExtensionFilter("All files", "*.*")
            );
        }

        File f = fc.showOpenDialog(trackDetailPane.getScene().getWindow());
        if (f == null) return;

        if (overLimit(f)) throw new IllegalArgumentException("File is larger than 50MB.");

        fileObjectService.uploadNewVersion(candidateId, track.getId(), artifact.getId(), f);
        showInfo("File uploaded. Now create a snapshot to freeze this version.");
    }
    private File zipDirectoryToTemp(Path rootDir) throws IOException {
        // Excludes (simple): you can tweak
        List<String> excludedNames = List.of(".git", "node_modules", "target", "dist", "build", ".idea");

        Path zipPath = Files.createTempFile("carrieri-code-", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            Files.walk(rootDir)
                    .filter(p -> !Files.isDirectory(p))
                    .forEach(p -> {
                        try {
                            Path rel = rootDir.relativize(p);
                            // skip excluded folders
                            for (Path part : rel) {
                                if (excludedNames.contains(part.toString())) return;
                            }

                            String entryName = rel.toString().replace("\\", "/");
                            zos.putNextEntry(new ZipEntry(entryName));
                            Files.copy(p, zos);
                            zos.closeEntry();
                        } catch (IOException ex) {
                            throw new UncheckedIOException(ex);
                        }
                    });
        } catch (UncheckedIOException ex) {
            try { Files.deleteIfExists(zipPath); } catch (Exception ignored) {}
            throw ex.getCause();
        }

        return zipPath.toFile();
    }

    private void downloadToDisk(int fileObjectId, String baseNameNoExt) throws Exception {
        FileObject fo = fileObjectService.findById(fileObjectId);
        if (fo == null) throw new IllegalStateException("Missing file_object record.");

        String url = fileObjectService.presignedDownloadUrl(fo.getStorageKey(), Duration.ofMinutes(10));
        String ext = extFromMimeOrKey(fo); // see below
        String suggested = safeFileName(baseNameNoExt, ext);

        FileChooser fc = new FileChooser();
        fc.setTitle("Save file");
        fc.setInitialFileName(suggested);
        File dest = fc.showSaveDialog(trackDetailPane.getScene().getWindow());
        if (dest == null) return;

        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<Path> res = client.send(req, HttpResponse.BodyHandlers.ofFile(dest.toPath()));
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new IllegalStateException("Download failed (HTTP " + res.statusCode() + ")");
        }
    }

    private static String extFromMimeOrKey(FileObject fo) {
        String mt = fo.getMimeType() == null ? "" : fo.getMimeType().toLowerCase();
        if (mt.contains("zip")) return "zip";
        if (mt.contains("pdf")) return "pdf";
        if (mt.contains("png")) return "png";
        if (mt.contains("jpeg") || mt.contains("jpg")) return "jpg";
        if (mt.contains("webp")) return "webp";
        if (mt.contains("mp4")) return "mp4";
        if (mt.contains("quicktime")) return "mov";
        if (mt.contains("plain")) return "txt";

        String key = fo.getStorageKey() == null ? "" : fo.getStorageKey();
        int i = key.lastIndexOf('.');
        if (i > 0 && i < key.length() - 1) return key.substring(i + 1).toLowerCase();
        return "bin";
    }







}
