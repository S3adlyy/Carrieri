package com.example.guser.controllers.grecru;

import com.example.guser.controllers.guser.AppNavController;
import entities.grecru.Mission;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.util.converter.IntegerStringConverter;
import services.grecru.MissionService;
import utils.grecru.AlertUtils;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class MissionListController implements Initializable {

    @FXML private TableView<Mission> missionTable;
    @FXML private TableColumn<Mission, String> descriptionColumn;
    @FXML private TableColumn<Mission, Integer> scoreColumn;
    @FXML private TableColumn<Mission, String> creatorColumn;
    @FXML private TableColumn<Mission, String> dateColumn;

    @FXML private Label lblTotalMissions;
    @FXML private Label lblAvgScore;
    @FXML private Label lblActiveMissions;

    @FXML private VBox statsCard1;
    @FXML private VBox statsCard2;
    @FXML private VBox statsCard3;

    private final MissionService missionService = new MissionService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Make table editable
        missionTable.setEditable(true);

        configureTable();
        addActionsColumn();
        loadMissions();
        styleStatsCards();

        // Force apply styling after table is populated
        Platform.runLater(() -> {
            applyModernStyling();
            setupKeyboardShortcuts();
        });
    }

    private void configureTable() {
        // Clear existing columns
        missionTable.getColumns().clear();

        // Re-add columns in correct order
        missionTable.getColumns().addAll(descriptionColumn, scoreColumn, creatorColumn, dateColumn);

        // Configure column properties
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("score_min"));

        // Creator column with proper display
        creatorColumn.setCellValueFactory(cellData -> {
            Integer creatorId = cellData.getValue().getCreated_by_id();
            String displayText = creatorId != null ? "User #" + creatorId : "Unknown";
            return new javafx.beans.property.SimpleStringProperty(displayText);
        });

        // Date column
        dateColumn.setCellValueFactory(cellData -> {
            try {
                if (cellData.getValue().getCreated_at() != null) {
                    String formattedDate = cellData.getValue().getCreated_at().format(dateFormatter);
                    return new javafx.beans.property.SimpleStringProperty(formattedDate);
                }
                return new javafx.beans.property.SimpleStringProperty("N/A");
            } catch (Exception e) {
                return new javafx.beans.property.SimpleStringProperty("N/A");
            }
        });

        // Make columns editable
        missionTable.setEditable(true);

        // === DESCRIPTION COLUMN - EDITABLE ===
        descriptionColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        descriptionColumn.setOnEditCommit(event -> {
            Mission mission = event.getRowValue();
            String newDescription = event.getNewValue();
            mission.setDescription(newDescription);
            updateMission(mission);
        });

        // === SCORE COLUMN - EDITABLE WITH BADGE ===
        scoreColumn.setCellFactory(column -> {
            return new TextFieldTableCell<Mission, Integer>(new IntegerStringConverter()) {
                @Override
                public void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        Label scoreBadge = new Label(item + "%");
                        scoreBadge.setStyle(getScoreStyle(item));
                        scoreBadge.setPrefWidth(60);
                        scoreBadge.setAlignment(Pos.CENTER);
                        setGraphic(scoreBadge);
                        setText(null);
                    }
                }
            };
        });
        scoreColumn.setOnEditCommit(event -> {
            Mission mission = event.getRowValue();
            Integer newScore = event.getNewValue();
            if (newScore >= 0 && newScore <= 100) {
                mission.setScore_min(newScore);
                updateMission(mission);
            }
        });

        // === CREATOR COLUMN - EDITABLE WITH BADGE ===
        creatorColumn.setCellFactory(column -> {
            return new TextFieldTableCell<Mission, String>() {
                @Override
                public void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        Mission mission = getTableView().getItems().get(getIndex());
                        if (mission != null) {
                            Label creatorBadge = new Label("👤 " + item);
                            creatorBadge.setStyle(
                                    "-fx-background-color: #f3e8ff;" +
                                            "-fx-text-fill: #6d28d9;" +
                                            "-fx-padding: 4 10;" +
                                            "-fx-background-radius: 20;" +
                                            "-fx-font-size: 12px;" +
                                            "-fx-font-weight: bold;"
                            );
                            setGraphic(creatorBadge);
                            setText(null);
                        }
                    }
                }
            };
        });
        creatorColumn.setOnEditCommit(event -> {
            Mission mission = event.getRowValue();
            String newValue = event.getNewValue();
            try {
                int creatorId;
                if (newValue.startsWith("User #")) {
                    creatorId = Integer.parseInt(newValue.substring(6));
                } else if (newValue.startsWith("👤 User #")) {
                    creatorId = Integer.parseInt(newValue.substring(9));
                } else {
                    creatorId = Integer.parseInt(newValue);
                }
                mission.setCreated_by_id(creatorId);
                updateMission(mission);
            } catch (NumberFormatException e) {
                AlertUtils.showError("Invalid ID", "Please enter a valid number");
                loadMissions();
            }
        });

        // === DATE COLUMN - DISPLAY ONLY ===
        dateColumn.setCellFactory(column -> new TableCell<Mission, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #6b7280; -fx-alignment: CENTER; -fx-font-size: 13px;");
                }
            }
        });
    }

    private String getScoreStyle(int score) {
        String color;
        String bgColor;
        if (score >= 80) {
            color = "#0a5e3c"; // Dark green
            bgColor = "#e6f7ed"; // Light green
        } else if (score >= 60) {
            color = "#92400e"; // Dark orange
            bgColor = "#fef3c7"; // Light orange
        } else {
            color = "#991b1b"; // Dark red
            bgColor = "#fee2e2"; // Light red
        }

        return "-fx-background-color: " + bgColor + ";" +
                "-fx-text-fill: " + color + ";" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 4 12;" +
                "-fx-background-radius: 20;" +
                "-fx-border-radius: 20;" +
                "-fx-border-color: transparent;" +
                "-fx-font-size: 12px;";
    }

    /**
     * Add Actions column with modern styling and refresh button in header
     */
    private void addActionsColumn() {
        TableColumn<Mission, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(140);
        actionsCol.setStyle("-fx-alignment: CENTER;");

        // Create header with refresh button
        HBox headerBox = new HBox(8);
        headerBox.setAlignment(Pos.CENTER);

        Label headerIcon = new Label("⚙️");
        headerIcon.setStyle("-fx-font-size: 14px;");

        Button refreshBtn = new Button("🔄");
        refreshBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #5b21b6;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 2 6;" +
                        "-fx-background-radius: 12;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-weight: bold;"
        );

        // Tooltip for refresh button
        Tooltip refreshTooltip = new Tooltip("Refresh table (Ctrl+R)");
        refreshTooltip.setStyle(
                "-fx-background-color: #5b21b6;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 11px;" +
                        "-fx-padding: 6;" +
                        "-fx-background-radius: 6;"
        );
        refreshBtn.setTooltip(refreshTooltip);

        // Hover effect for refresh button
        refreshBtn.setOnMouseEntered(e ->
                refreshBtn.setStyle(
                        "-fx-background-color: #f3e8ff;" +
                                "-fx-text-fill: #5b21b6;" +
                                "-fx-font-size: 14px;" +
                                "-fx-padding: 2 6;" +
                                "-fx-background-radius: 12;" +
                                "-fx-cursor: hand;" +
                                "-fx-font-weight: bold;"
                )
        );

        refreshBtn.setOnMouseExited(e ->
                refreshBtn.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-text-fill: #5b21b6;" +
                                "-fx-font-size: 14px;" +
                                "-fx-padding: 2 6;" +
                                "-fx-background-radius: 12;" +
                                "-fx-cursor: hand;" +
                                "-fx-font-weight: bold;"
                )
        );

        // Refresh action
        refreshBtn.setOnAction(e -> {
            loadMissions();
            showRefreshSuccess();
        });

        headerBox.getChildren().addAll(headerIcon, refreshBtn);
        actionsCol.setGraphic(headerBox);

        // Cell factory for delete button
        actionsCol.setCellFactory(col -> new TableCell<Mission, Void>() {
            private final Button deleteBtn = new Button("🗑️");

            {
                deleteBtn.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-text-fill: #dc2626;" +
                                "-fx-padding: 6 10;" +
                                "-fx-background-radius: 8;" +
                                "-fx-border-radius: 8;" +
                                "-fx-border-color: #fee2e2;" +
                                "-fx-border-width: 1.5;" +
                                "-fx-cursor: hand;" +
                                "-fx-font-size: 13px;" +
                                "-fx-font-weight: bold;"
                );

                // Hover effect
                deleteBtn.setOnMouseEntered(e ->
                        deleteBtn.setStyle(
                                "-fx-background-color: #fee2e2;" +
                                        "-fx-text-fill: #dc2626;" +
                                        "-fx-padding: 6 10;" +
                                        "-fx-background-radius: 8;" +
                                        "-fx-border-radius: 8;" +
                                        "-fx-border-color: #dc2626;" +
                                        "-fx-border-width: 1.5;" +
                                        "-fx-cursor: hand;" +
                                        "-fx-font-size: 13px;" +
                                        "-fx-font-weight: bold;"
                        )
                );

                deleteBtn.setOnMouseExited(e ->
                        deleteBtn.setStyle(
                                "-fx-background-color: white;" +
                                        "-fx-text-fill: #dc2626;" +
                                        "-fx-padding: 6 10;" +
                                        "-fx-background-radius: 8;" +
                                        "-fx-border-radius: 8;" +
                                        "-fx-border-color: #fee2e2;" +
                                        "-fx-border-width: 1.5;" +
                                        "-fx-cursor: hand;" +
                                        "-fx-font-size: 13px;" +
                                        "-fx-font-weight: bold;"
                        )
                );

                deleteBtn.setTooltip(new Tooltip("Delete Mission"));

                deleteBtn.setOnAction(e -> {
                    Mission mission = getTableView().getItems().get(getIndex());
                    deleteMission(mission);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteBtn);
                }
            }
        });

        missionTable.getColumns().add(actionsCol);
    }

    /**
     * Show refresh success indicator
     */
    private void showRefreshSuccess() {
        Platform.runLater(() -> {
            Label refreshLabel = new Label("✓ Refreshed");
            refreshLabel.setStyle(
                    "-fx-text-fill: #5b21b6;" +
                            "-fx-font-weight: bold;" +
                            "-fx-font-size: 12px;" +
                            "-fx-padding: 4 12;" +
                            "-fx-background-color: #f3e8ff;" +
                            "-fx-background-radius: 20;" +
                            "-fx-effect: dropshadow(gaussian, rgba(91, 33, 182, 0.1), 8, 0, 0, 2);"
            );

            // Show at the top of the table
            HBox container = new HBox(refreshLabel);
            container.setAlignment(Pos.CENTER);
            container.setPadding(new Insets(10, 0, 0, 0));

            // Add to table header area
            StackPane header = (StackPane) missionTable.lookup(".column-header-background");
            if (header != null) {
                header.getChildren().add(container);

                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        Platform.runLater(() -> {
                            if (header.getChildren().contains(container)) {
                                header.getChildren().remove(container);
                            }
                        });
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
        });
    }

    /**
     * Setup keyboard shortcuts
     */
    private void setupKeyboardShortcuts() {
        if (missionTable.getScene() != null) {
            missionTable.getScene().setOnKeyPressed(event -> {
                if (event.isControlDown() && event.getCode().toString().equals("R")) {
                    loadMissions();
                    showRefreshSuccess();
                    event.consume();
                }
            });
        }
    }

    private void styleStatsCards() {
        // Style for stats values
        String statsValueStyle =
                "-fx-font-size: 32px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #5b21b6;"; // Deep purple

        // Style for stats labels
        String statsLabelStyle =
                "-fx-font-size: 14px;" +
                        "-fx-text-fill: #6b7280;";

        lblTotalMissions.setStyle(statsValueStyle);
        lblAvgScore.setStyle(statsValueStyle);
        lblActiveMissions.setStyle(statsValueStyle);

        // Style stats cards
        Platform.runLater(() -> {
            if (statsCard1 != null) {
                statsCard1.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-background-radius: 16;" +
                                "-fx-padding: 20;" +
                                "-fx-effect: dropshadow(gaussian, rgba(91, 33, 182, 0.08), 20, 0, 0, 4);" +
                                "-fx-border-color: #f3e8ff;" +
                                "-fx-border-radius: 16;" +
                                "-fx-border-width: 1;"
                );
            }
            if (statsCard2 != null) {
                statsCard2.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-background-radius: 16;" +
                                "-fx-padding: 20;" +
                                "-fx-effect: dropshadow(gaussian, rgba(91, 33, 182, 0.08), 20, 0, 0, 4);" +
                                "-fx-border-color: #f3e8ff;" +
                                "-fx-border-radius: 16;" +
                                "-fx-border-width: 1;"
                );
            }
            if (statsCard3 != null) {
                statsCard3.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-background-radius: 16;" +
                                "-fx-padding: 20;" +
                                "-fx-effect: dropshadow(gaussian, rgba(91, 33, 182, 0.08), 20, 0, 0, 4);" +
                                "-fx-border-color: #f3e8ff;" +
                                "-fx-border-radius: 16;" +
                                "-fx-border-width: 1;"
                );
            }
        });
    }

    private void applyModernStyling() {
        // Modern table styling with purple theme
        missionTable.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #f3e8ff;" +
                        "-fx-border-radius: 16;" +
                        "-fx-background-radius: 16;" +
                        "-fx-padding: 0;" +
                        "-fx-effect: dropshadow(gaussian, rgba(91, 33, 182, 0.05), 20, 0, 0, 4);"
        );

        // Style headers with purple gradient
        String headerStyle =
                "-fx-background-color: linear-gradient(to right, #faf5ff, #f3e8ff);" +
                        "-fx-text-fill: #5b21b6;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 15px 10px;" +
                        "-fx-border-color: transparent transparent #f3e8ff transparent;" +
                        "-fx-border-width: 0 0 2 0;";

        descriptionColumn.setStyle(headerStyle);
        scoreColumn.setStyle(headerStyle);
        creatorColumn.setStyle(headerStyle);
        dateColumn.setStyle(headerStyle);

        // Style the table rows
        missionTable.setRowFactory(tv -> {
            TableRow<Mission> row = new TableRow<Mission>() {
                @Override
                protected void updateItem(Mission item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("");
                        setTooltip(null);
                    } else {
                        // Clean white background with subtle hover effect
                        String baseStyle = "-fx-background-color: white; -fx-border-color: transparent;";
                        String hoverStyle = "-fx-background-color: #faf5ff; -fx-border-color: #f3e8ff; -fx-border-width: 0 0 1 0;";

                        setStyle(baseStyle);

                        // Add hover effect
                        setOnMouseEntered(e -> setStyle(hoverStyle));
                        setOnMouseExited(e -> setStyle(baseStyle));

                        // Tooltip with mission summary AND full description
                        String description = item.getDescription();

                        Tooltip tip = new Tooltip(
                                "🎯 Mission #" + item.getId() + "\n\n" +
                                        "📝 Description:\n" + description + "\n\n" +
                                        "📊 Min Score: " + item.getScore_min() + "%\n" +
                                        "👤 Created by: User #" + item.getCreated_by_id() + "\n" +
                                        "📅 Created: " + (item.getCreated_at() != null ?
                                        item.getCreated_at().format(dateFormatter) : "N/A")
                        );
                        tip.setStyle(
                                "-fx-background-color: #5b21b6;" +
                                        "-fx-text-fill: white;" +
                                        "-fx-font-size: 12px;" +
                                        "-fx-padding: 12;" +
                                        "-fx-background-radius: 8;" +
                                        "-fx-wrap-text: true;" +
                                        "-fx-max-width: 500px;"
                        );
                        setTooltip(tip);
                    }
                }
            };

            // Double click for submission
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Mission selectedMission = row.getItem();
                    navigateToRenduAdd(selectedMission);
                }
            });

            return row;
        });

        missionTable.refresh();
    }

    private void updateMission(Mission mission) {
        try {
            missionService.update(mission);
            showQuickSuccess();
            calculateStats();
        } catch (Exception e) {
            AlertUtils.showError("Update Error", "Failed to update mission: " + e.getMessage());
            loadMissions();
        }
    }

    private void showQuickSuccess() {
        Platform.runLater(() -> {
            Label successLabel = new Label("✓ Saved");
            successLabel.setStyle(
                    "-fx-text-fill: #5b21b6;" +
                            "-fx-font-weight: bold;" +
                            "-fx-font-size: 12px;" +
                            "-fx-padding: 4 12;" +
                            "-fx-background-color: #f3e8ff;" +
                            "-fx-background-radius: 20;"
            );

            int selectedIndex = missionTable.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                TableRow<?> row = (TableRow<?>) missionTable.lookup(".table-row-cell:selected");
                if (row != null) {
                    HBox container = new HBox(successLabel);
                    container.setAlignment(Pos.CENTER_RIGHT);
                    container.setPadding(new Insets(0, 15, 0, 0));
                    row.setGraphic(container);

                    new Thread(() -> {
                        try {
                            Thread.sleep(2000);
                            Platform.runLater(() -> row.setGraphic(null));
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }).start();
                }
            }
        });
    }

    private void loadMissions() {
        try {
            missionTable.setItems(
                    FXCollections.observableArrayList(missionService.read())
            );
            calculateStats();
        } catch (Exception e) {
            AlertUtils.showError("Load Error", e.getMessage());
            e.printStackTrace();
        }
    }

    private void calculateStats() {
        try {
            int total = missionTable.getItems().size();
            lblTotalMissions.setText(String.valueOf(total));

            if (total > 0) {
                double avgScore = missionTable.getItems().stream()
                        .mapToInt(Mission::getScore_min)
                        .average()
                        .orElse(0);
                lblAvgScore.setText(String.format("%.0f%%", avgScore));
            } else {
                lblAvgScore.setText("0%");
            }

            long active = missionTable.getItems().stream()
                    .filter(m -> m.getScore_min() > 0)
                    .count();
            lblActiveMissions.setText(String.valueOf(active));

        } catch (Exception e) {
            System.err.println("Error calculating stats: " + e.getMessage());
        }
    }

    private void navigateToRenduAdd(Mission mission) {
        try {
            AppNavController.getInstance().showRenduAddWithMissionId(mission.getId());
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("Navigation Error", "Could not load submission form: " + e.getMessage());
        }
    }

    /**
     * Delete mission with styled confirmation dialog
     */
    private void deleteMission(Mission selected) {
        if (AlertUtils.showDeleteConfirmation(
                "mission",
                selected.getDescription(),
                "📊 Minimum Score: " + selected.getScore_min() + "%\n❌ This action cannot be undone."
        )) {
            try {
                missionService.supprimer(selected.getId());
                loadMissions();
                AlertUtils.showSuccess("Success", "Mission has been deleted successfully!");
            } catch (Exception e) {
                AlertUtils.showError("Delete Error", e.getMessage());
            }
        }
    }
}