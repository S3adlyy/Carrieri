package com.example.guser.controllers;

import entities.Artifact;
import entities.FileObject;
import entities.Snapshot;
import entities.Track;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import services.*;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.text.Text;




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
        if (!ownerMode) return;

        TextInputDialog d = new TextInputDialog("Snapshot message");
        d.setHeaderText("Create Snapshot");
        d.setContentText("Message:");
        Optional<String> res = d.showAndWait();
        if (res.isEmpty()) return;

        try {
            snapshotService.createSnapshot(candidateId, track.getId(), viewerUserId,
                    "Snapshot", res.get(), false);

            if (viewMode == ViewMode.PROGRESS) {
                refreshSnapshotsAndTimeline();
                if (!snapshots.isEmpty()) selectSnapshot(snapshots.get(0));
            }

            setError(null);
        } catch (Exception e) {
            setError(e.getMessage());
        }
    }

    @FXML
    private void onAddArtifact() {
        if (!ownerMode || track == null) return;

        Dialog<ArtifactDraft> dialog = new Dialog<>();
        dialog.setTitle("Add Artifact");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        TextField nameField = new TextField();
        nameField.setPromptText("Artifact name (e.g., repo, report, demo)");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Description (optional)");
        descArea.setPrefRowCount(3);

        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList(
                "CODE", "DOCUMENT", "IMAGE", "VIDEO", "TEXT", "LINK"
        ));
        typeBox.getSelectionModel().select("CODE");

        TextField languageField = new TextField();
        languageField.setPromptText("Language (optional, for CODE)");

        TextArea textArea = new TextArea();
        textArea.setPromptText("Text / URL (for TEXT/LINK)");
        textArea.setPrefRowCount(4);

        Label uploadHint = new Label("For CODE/DOCUMENT/IMAGE/VIDEO you will upload a file (or folder→zip for CODE) after creating.");
        uploadHint.getStyleClass().add("prf-muted");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        int r = 0;
        grid.addRow(r++, new Label("Name"), nameField);
        grid.addRow(r++, new Label("Type"), typeBox);
        grid.addRow(r++, new Label("Language"), languageField);
        grid.addRow(r++, new Label("Text/URL"), textArea);
        grid.addRow(r++, new Label("Description"), descArea);
        grid.add(uploadHint, 0, r++, 2, 1);

        dialog.getDialogPane().setContent(grid);

        Runnable refreshFields = () -> {
            String type = safeUpper(typeBox.getValue());
            boolean isCode = type.equals("CODE");
            boolean isTextLike = type.equals("TEXT") || type.equals("LINK");

            languageField.setDisable(!isCode);
            textArea.setDisable(!isTextLike);

            if (!isTextLike) textArea.clear();
            if (!isCode) languageField.clear();
        };
        typeBox.valueProperty().addListener((obs, o, v) -> refreshFields.run());
        refreshFields.run();

        Node okBtn = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setDisable(true);

        Runnable validate = () -> {
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            String type = safeUpper(typeBox.getValue());

            boolean ok = !name.isEmpty();
            if (ok && ("TEXT".equals(type) || "LINK".equals(type))) {
                String txt = textArea.getText() == null ? "" : textArea.getText().trim();
                ok = !txt.isEmpty();
            }
            okBtn.setDisable(!ok);
        };

        nameField.textProperty().addListener((obs, o, v) -> validate.run());
        typeBox.valueProperty().addListener((obs, o, v) -> validate.run());
        textArea.textProperty().addListener((obs, o, v) -> validate.run());
        validate.run();

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;

            ArtifactDraft d = new ArtifactDraft();
            d.name = nameField.getText().trim();
            d.description = descArea.getText();
            d.type = safeUpper(typeBox.getValue());
            d.language = (languageField.getText() == null) ? null : languageField.getText().trim();
            d.textContent = textArea.getText();
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
                    (d.type.equals("CODE") ? emptyToNull(d.language) : null),
                    ((d.type.equals("TEXT") || d.type.equals("LINK")) ? emptyToNull(d.textContent) : null)
            );

            // Upload immediately for file-based artifacts
            if (d.type.equals("CODE")) {
                promptUploadForCode(created);
            } else if (d.type.equals("DOCUMENT") || d.type.equals("IMAGE") || d.type.equals("VIDEO")) {
                promptUploadSingleFile(created, d.type);
            }

            // Refresh UI depending on mode (CURRENT vs PROGRESS)
            if (viewMode == ViewMode.CURRENT) {
                refreshCurrentArtifacts();
                showPlaceholder("Select an artifact.");
                showInfo("Artifact added. Upload done (if selected). Create a snapshot when you want to record progress.");
            } else {
                refreshSnapshotsAndTimeline();
                if (!snapshots.isEmpty()) selectSnapshot(snapshots.get(0)); // oldest auto-selected (Option A)
                else {
                    selectedSnapshot = null;
                    artifactRows.clear();
                    showPlaceholder("No snapshots yet. Create one.");
                }
                showInfo("Artifact added. Create a new snapshot to include it in the timeline.");
            }

            setError(null);

        } catch (Exception e) {
            setError(e.getMessage());
        }
    }


    private static class ArtifactDraft {
        String name;
        String description;
        String type;
        String language;
        String textContent;
    }
    private void refreshArtifactsUI() {
        try {
            if (selectedSnapshot != null) {
                refreshArtifactsForSnapshot(selectedSnapshot);
            } else {
                // Show artifacts without snapshot mapping (fileObjectId unknown)
                List<Artifact> artifacts = artifactService.listActiveByTrack(track.getId());
                artifactRows.setAll(buildGroupedRows(artifacts));
                showPlaceholder("Create a snapshot to freeze versions.");
            }
            setError(null);
        } catch (Exception e) {
            setError(e.getMessage());
        }
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

            case "VIDEO" -> showDownloadPanel(
                    artifact.getArtifactName() + " (Video)",
                    fileObjectId,
                    artifact.getArtifactName()
            );

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

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/guser/code_viewer.fxml"));
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
            case "LINK" -> "🔗";
            case "TEXT" -> "📝";
            default -> "•";
        };
    }

    private void installArtifactCells() {
        artifactsListView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(ArtifactRow row, boolean empty) {
                super.updateItem(row, empty);
                setText(null);
                setGraphic(null);
                setDisable(false);
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

                Artifact a = row.artifact;
                String type = safeUpper(a.getArtifactType());

                Label icon = new Label(iconForType(type));
                icon.getStyleClass().add("wsp-artIcon");

                Label name = new Label(nullToEmpty(a.getArtifactName()));
                name.getStyleClass().add("wsp-artName");

                String metaTxt = type + ((a.getLanguage() == null || a.getLanguage().isBlank()) ? "" : (" • " + a.getLanguage()));
                Label meta = new Label(metaTxt);
                meta.getStyleClass().add("wsp-artMeta");

                VBox texts = new VBox(2, name, meta);

                HBox root = new HBox(10, icon, texts);
                root.setAlignment(Pos.CENTER_LEFT);
                root.setPadding(new Insets(2, 6, 2, 6));

                if (viewMode == ViewMode.PROGRESS && selectedSnapshot != null && row.fileObjectId == null) {
                    Label miss = new Label("No file in this snapshot");
                    miss.getStyleClass().add("wsp-artWarn");
                    root.getChildren().add(miss);
                }

                setGraphic(root);
            }
        });
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
            case "LINK" -> "Links";
            case "TEXT" -> "Text";
            default -> t;
        };
    }

    private static String safeUpper(String s) { return s == null ? "" : s.trim().toUpperCase(); }
    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    // --- UI row type: no new VM files needed ---
    private enum ArtifactRowKind { HEADER, ITEM }

    private static class ArtifactRow {
        final ArtifactRowKind kind;
        final String headerTitle;
        final Artifact artifact;
        Integer fileObjectId;

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
        ButtonType cancel = ButtonType.CANCEL;
        choice.getButtonTypes().setAll(folderBtn, zipBtn, cancel);

        Optional<ButtonType> res = choice.showAndWait();
        if (res.isEmpty() || res.get() == cancel) return;

        if (res.get() == folderBtn) {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select project folder");
            File dir = dc.showDialog(trackDetailPane.getScene().getWindow());
            if (dir == null) return;

            File zipped = zipDirectoryToTemp(dir.toPath());
            if (overLimit(zipped)) {
                zipped.delete();
                throw new IllegalArgumentException("ZIP is larger than 50MB.");
            }

            fileObjectService.uploadNewVersion(candidateId, track.getId(), artifact.getId(), zipped);
            zipped.delete();

            showInfo("Code uploaded. Now create a snapshot to freeze this version.");
            return;
        }

        // ZIP upload
        FileChooser fc = new FileChooser();
        fc.setTitle("Select ZIP");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP", "*.zip"));
        File zip = fc.showOpenDialog(trackDetailPane.getScene().getWindow());
        if (zip == null) return;

        if (overLimit(zip)) throw new IllegalArgumentException("File is larger than 50MB.");

        fileObjectService.uploadNewVersion(candidateId, track.getId(), artifact.getId(), zip);
        showInfo("ZIP uploaded. Now create a snapshot to freeze this version.");
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
