package com.example.guser.controllers;

import entities.Track;
import entities.Workspace;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import services.TrackService;
import services.WorkspaceService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;


public class WorkspaceController {

    @FXML private Button newTrackBtn;

    @FXML private VBox tracksListPane;
    @FXML private ListView<Track> tracksListView;

    // fx:include fx:id="trackSection" source="track.fxml"
    @FXML private VBox trackSection; // root node of included FXML (type matches track.fxml root VBox)
    @FXML private TrackController trackSectionController; // injected included controller [web:464][web:465]

    @FXML private Label workspaceErrorLabel;

    private final WorkspaceService workspaceService = new WorkspaceService();
    private final TrackService trackService = new TrackService();

    private final ObservableList<Track> tracks = FXCollections.observableArrayList();

    private int candidateId;
    private int viewerUserId;
    private boolean ownerMode;

    @FXML
    private void initialize() {
        tracksListView.setItems(tracks);
        installTrackCells();

        tracksListView.getSelectionModel().selectedItemProperty().addListener((obs, o, t) -> {
            if (t != null) openTrack(t);
        });

        setError(null);
        showListPane();
    }

    public void initContext(int candidateId, int viewerUserId, boolean ownerMode) {
        this.candidateId = candidateId;
        this.viewerUserId = viewerUserId;
        this.ownerMode = ownerMode;

        newTrackBtn.setVisible(ownerMode);
        newTrackBtn.setManaged(ownerMode);

        refreshTracks();
    }

    private void refreshTracks() {
        try {
            Workspace ws = workspaceService.getOrCreateByCandidateId(candidateId);
            List<Track> list = trackService.listByWorkspace(ws.getId(), !ownerMode);
            tracks.setAll(list);
            setError(null);
        } catch (SQLException e) {
            setError(e.getMessage());
        }
    }

    private void openTrack(Track t) {
        if (trackSectionController == null) {
            setError("trackSectionController is null. Check workspace.fxml include fx:id=\"trackSection\" and track.fxml fx:controller.");
            return;
        }

        showDetailPane();

        trackSectionController.initContext(
                candidateId,
                viewerUserId,
                ownerMode,
                t,
                () -> {
                    tracksListView.getSelectionModel().clearSelection();
                    showListPane();
                    refreshTracks();
                }
        );
    }

    private void showListPane() {
        tracksListPane.setVisible(true);
        tracksListPane.setManaged(true);

        if (trackSection != null) {
            trackSection.setVisible(false);
            trackSection.setManaged(false);
        }
        if (trackSectionController != null) trackSectionController.hide();
    }

    private void showDetailPane() {
        tracksListPane.setVisible(false);
        tracksListPane.setManaged(false);

        if (trackSection != null) {
            trackSection.setVisible(true);
            trackSection.setManaged(true);
        }
        if (trackSectionController != null) trackSectionController.show();
    }

    @FXML
    private void onNewTrack() {
        if (!ownerMode) return;

        try {
            Workspace ws = workspaceService.getOrCreateByCandidateId(candidateId);

            Dialog<TrackDraft> dialog = new Dialog<>();
            dialog.setTitle("New Track");
            dialog.setHeaderText(null);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
            dialog.getDialogPane().getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/com/example/guser/workspace.css")).toExternalForm()
            );
            Node ok = dialog.getDialogPane().lookupButton(ButtonType.OK);
            Node cancel = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
            if (ok != null) ok.getStyleClass().add("prf-actBtn");
            if (cancel != null) cancel.getStyleClass().add("prf-outlineBtn");


            TextField titleField = new TextField();
            titleField.setPromptText("Title (e.g., Fitness App, Internship at Delice)");

            TextArea descArea = new TextArea();
            descArea.setPromptText("Description (optional)");
            descArea.setPrefRowCount(4);

            ComboBox<String> categoryBox = new ComboBox<>(FXCollections.observableArrayList(
                    "PROJECT", "EDUCATION", "EXPERIENCE", "ACTIVITY"
            ));
            categoryBox.getSelectionModel().select("PROJECT");

            DatePicker startPicker = new DatePicker();
            DatePicker endPicker = new DatePicker();

            ComboBox<String> visibilityBox = new ComboBox<>(FXCollections.observableArrayList("PUBLIC", "PRIVATE"));
            visibilityBox.getSelectionModel().select("PRIVATE");

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);

            int r = 0;
            grid.addRow(r++, new Label("Title"), titleField);
            grid.addRow(r++, new Label("Category"), categoryBox);
            grid.addRow(r++, new Label("Start date"), startPicker);
            grid.addRow(r++, new Label("End date"), endPicker);
            grid.addRow(r++, new Label("Visibility"), visibilityBox);
            grid.addRow(r++, new Label("Description"), descArea);

            dialog.getDialogPane().setContent(grid);

            Node okBtn = dialog.getDialogPane().lookupButton(ButtonType.OK);
            okBtn.setDisable(true);
            titleField.textProperty().addListener((obs, o, v) -> okBtn.setDisable(v == null || v.trim().isEmpty()));

            dialog.setResultConverter(bt -> {
                if (bt != ButtonType.OK) return null;
                TrackDraft d = new TrackDraft();
                d.title = titleField.getText().trim();
                d.description = descArea.getText();
                d.category = categoryBox.getValue();
                d.start = startPicker.getValue();
                d.end = endPicker.getValue();
                d.visibility = visibilityBox.getValue();
                return d;
            });

            Optional<TrackDraft> res = dialog.showAndWait();
            if (res.isEmpty()) return;

            TrackDraft d = res.get();

            if (d.start != null && d.end != null && d.end.isBefore(d.start)) {
                setError("End date cannot be before start date.");
                return;
            }

            trackService.create(ws.getId(), d.title, d.description, d.category, d.start, d.end, d.visibility);

            setError(null);
            refreshTracks();

        } catch (Exception e) {
            setError(e.getMessage());
        }
    }

    private static class TrackDraft {
        String title;
        String description;
        String category;
        java.time.LocalDate start;
        java.time.LocalDate end;
        String visibility;
    }

    private void installTrackCells() {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        tracksListView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Track t, boolean empty) {
                super.updateItem(t, empty);

                if (empty || t == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().removeAll("wsp-trackCell", "wsp-trackCellSelected");
                    return;
                }

                // Root card
                VBox card = new VBox(8);
                card.getStyleClass().add("wsp-trackCard");

                // --- Top row: title + visibility icon ---
                String vis = t.getVisibility() == null ? "" : t.getVisibility().trim().toUpperCase();
                boolean isPrivate = "PRIVATE".equals(vis);

                Label visIcon = new Label(isPrivate ? "🔒" : "🔓");
                visIcon.getStyleClass().add("wsp-trackVisIcon");

                Label title = new Label(t.getTitle() == null ? "Untitled" : t.getTitle());
                title.getStyleClass().add("wsp-trackCardTitle");

                HBox top = new HBox(10, visIcon, title);
                top.setAlignment(Pos.CENTER_LEFT);

                // --- Meta row: category chip + dates ---
                String cat = t.getCategory() == null ? "UNCATEGORIZED" : t.getCategory().toUpperCase();
                Label chip = new Label(cat);
                chip.getStyleClass().add("wsp-trackChip");

                String start = t.getStartDate() == null ? "—" : df.format(t.getStartDate());
                String end = t.getEndDate() == null ? "Present" : df.format(t.getEndDate());

                Label dates = new Label(start + "  →  " + end);
                dates.getStyleClass().add("wsp-trackDates");

                HBox meta = new HBox(10, chip, dates);
                meta.setAlignment(Pos.CENTER_LEFT);

                // --- Description preview (optional) ---
                String desc = t.getDescription() == null ? "" : t.getDescription().trim();
                if (desc.length() > 120) desc = desc.substring(0, 120) + "…";

                Label descLabel = new Label(desc.isEmpty() ? "No description" : desc);
                descLabel.getStyleClass().add("wsp-trackDesc");
                descLabel.setWrapText(true);

                card.getChildren().setAll(top, meta, descLabel);

                setGraphic(card);
                setText(null);

                // Cell classes for selection styling
                getStyleClass().add("wsp-trackCell");
                pseudoClassStateChanged(PseudoClass.getPseudoClass("track-selected"), isSelected());
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                pseudoClassStateChanged(PseudoClass.getPseudoClass("track-selected"), selected);
            }
        });
    }


    private void setError(String msg) {
        boolean show = msg != null && !msg.isBlank();
        workspaceErrorLabel.setText(show ? msg : "");
        workspaceErrorLabel.setVisible(show);
        workspaceErrorLabel.setManaged(show);
    }
}
