package com.example.guser.controllers.guser;

import entities.guser.Track;
import entities.guser.Workspace;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.guser.TrackService;
import services.guser.WorkspaceService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import utils.guser.AlertUtils;


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

        final PseudoClass ERROR_PC = PseudoClass.getPseudoClass("error");

        try {
            Workspace ws = workspaceService.getOrCreateByCandidateId(candidateId);

            Dialog<TrackDraft> dialog = new Dialog<>();
            dialog.setTitle("Create track");
            dialog.setHeaderText(null);

            DialogPane pane = dialog.getDialogPane();
            pane.getStyleClass().addAll("wsp-dialog", "wsp-signupDialog");
            pane.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);

            // Ensure workspace.css is applied
            String wspCss = Objects.requireNonNull(
                    getClass().getResource("/com/example/guser/guser/workspace.css")
            ).toExternalForm();
            if (!pane.getStylesheets().contains(wspCss)) pane.getStylesheets().add(wspCss);

            Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
            if (okBtn != null) okBtn.setDisable(true);

            // --- Controls (native controls, styled by our card selectors) ---
            TextField titleField = new TextField();
            titleField.setPromptText("Title");
            titleField.getStyleClass().add("wsp-trackInput");

            TextArea descArea = new TextArea();
            descArea.setPromptText("Description (optional)");
            descArea.setPrefRowCount(4);
            descArea.getStyleClass().add("wsp-trackArea");

            ComboBox<String> categoryBox = new ComboBox<>(FXCollections.observableArrayList(
                    "PROJECT", "EDUCATION", "EXPERIENCE", "ACTIVITY"
            ));
            categoryBox.getSelectionModel().select("PROJECT");
            categoryBox.getStyleClass().add("wsp-trackInput");

            DatePicker startPicker = new DatePicker();
            startPicker.getStyleClass().add("wsp-trackInput");

            DatePicker endPicker = new DatePicker();
            endPicker.getStyleClass().add("wsp-trackInput");

            ComboBox<String> visibilityBox = new ComboBox<>(FXCollections.observableArrayList("PUBLIC", "PRIVATE"));
            visibilityBox.getSelectionModel().select("PRIVATE");
            visibilityBox.getStyleClass().add("wsp-trackInput");

            Label errorLabel = new Label();
            errorLabel.getStyleClass().add("wsp-trackError");
            errorLabel.setManaged(false);
            errorLabel.setVisible(false);
            errorLabel.setWrapText(true);

            // --- Layout: signup-like (label above field) ---
            VBox card = new VBox(12,
                    field("Title", titleField),
                    field("Category", categoryBox),
                    field("Start date", startPicker),
                    field("End date", endPicker),
                    field("Visibility", visibilityBox),
                    field("Description", descArea),
                    errorLabel
            );
            card.getStyleClass().add("wsp-trackCard");

            pane.setContent(card);

            Runnable clearErrors = () -> {
                titleField.pseudoClassStateChanged(ERROR_PC, false);
                categoryBox.pseudoClassStateChanged(ERROR_PC, false);
                startPicker.pseudoClassStateChanged(ERROR_PC, false);
                endPicker.pseudoClassStateChanged(ERROR_PC, false);
                visibilityBox.pseudoClassStateChanged(ERROR_PC, false);
                descArea.pseudoClassStateChanged(ERROR_PC, false);
                errorLabel.setText("");
                errorLabel.setManaged(false);
                errorLabel.setVisible(false);
            };

            java.util.function.Consumer<String> showError = (msg) -> {
                errorLabel.setText(msg);
                errorLabel.setManaged(true);
                errorLabel.setVisible(true);
            };

            Runnable validate = () -> {
                clearErrors.run();

                String t = titleField.getText() == null ? "" : titleField.getText().trim();
                LocalDate s = startPicker.getValue();
                LocalDate e = endPicker.getValue();

                if (t.length() < 3) {
                    titleField.pseudoClassStateChanged(ERROR_PC, true);
                    showError.accept("Title must be at least 3 characters.");
                    if (okBtn != null) okBtn.setDisable(true);
                    return;
                }

                if (s == null) {
                    startPicker.pseudoClassStateChanged(ERROR_PC, true);
                    showError.accept("Start date is required.");
                    if (okBtn != null) okBtn.setDisable(true);
                    return;
                }

                if (e != null && e.isBefore(s)) {
                    endPicker.pseudoClassStateChanged(ERROR_PC, true);
                    showError.accept("End date cannot be before start date.");
                    if (okBtn != null) okBtn.setDisable(true);
                    return;
                }

                if (okBtn != null) okBtn.setDisable(false);
            };

            titleField.textProperty().addListener((obs, o, v) -> validate.run());
            startPicker.valueProperty().addListener((obs, o, v) -> validate.run());
            endPicker.valueProperty().addListener((obs, o, v) -> validate.run());
            validate.run();

            dialog.setResultConverter(bt -> {
                if (bt != ButtonType.OK) return null;

                TrackDraft d = new TrackDraft();
                d.title = titleField.getText().trim();
                d.description = descArea.getText();
                d.category = categoryBox.getValue();
                d.start = startPicker.getValue();   // required
                d.end = endPicker.getValue();
                d.visibility = visibilityBox.getValue();
                return d;
            });

            Optional<TrackDraft> res = dialog.showAndWait();
            if (res.isEmpty()) return;

            TrackDraft d = res.get();

            // final hard validation
            if (d.start == null) {
                AlertUtils.showWarning("Missing start date", "Start date is required.");
                return;
            }
            if (d.end != null && d.end.isBefore(d.start)) {
                AlertUtils.showWarning("Invalid dates", "End date cannot be before start date.");
                return;
            }

            trackService.create(ws.getId(), d.title, d.description, d.category, d.start, d.end, d.visibility);

            setError(null);
            refreshTracks();
            AlertUtils.showSuccess("Track created", "Your new track was added successfully.");

        } catch (Exception e) {
            setError(e.getMessage());
            AlertUtils.showError("Could not create track", e.getMessage());
        }
    }

    private VBox field(String labelText, Node control) {
        Label l = new Label(labelText);
        l.getStyleClass().add("wsp-trackLabel");
        VBox box = new VBox(6, l, control);
        return box;
    }



    /**
     * Creates a signup-style field block:
     * Label (field-label), control, optional hint (field-hint).
     */
    private VBox fieldBlock(String labelText, Control control, String hintText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");

        VBox box = new VBox(5);
        box.getStyleClass().add("form-field");
        box.getChildren().addAll(label, control);

        if (hintText != null && !hintText.isBlank()) {
            Label hint = new Label(hintText);
            hint.getStyleClass().add("field-hint");
            hint.setWrapText(true);
            box.getChildren().add(hint);
        }

        return box;
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
