package com.exemple.grecrutement;

import entities.Mission;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import services.MissionService;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class MissionListController implements Initializable {

    @FXML private TableView<Mission> missionTable;
    @FXML private TableColumn<Mission, Integer> idColumn;
    @FXML private TableColumn<Mission, String> descriptionColumn;
    @FXML private TableColumn<Mission, Integer> scoreColumn;
    @FXML private TableColumn<Mission, String> creatorColumn;
    @FXML private TableColumn<Mission, String> dateColumn;
    // No need to declare actionsCol in FXML since we'll add it programmatically

    @FXML private Label lblTotalMissions;
    @FXML private Label lblAvgScore;
    @FXML private Label lblActiveMissions;

    private final MissionService missionService = new MissionService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Remove ALL CSS from the table first
        missionTable.getStyleClass().clear();
        missionTable.setStyle("");

        configureTable();
        addActionsColumn(); // Add this method call
        loadMissions();
        setupDoubleClickHandler();

        // Force apply styling after table is populated
        Platform.runLater(this::forceBlackText);
    }

    private void configureTable() {
        // Remove any existing styles
        idColumn.setStyle("");
        descriptionColumn.setStyle("");
        scoreColumn.setStyle("");
        creatorColumn.setStyle("");
        dateColumn.setStyle("");

        // Configure columns with proper property names
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("score_min"));

        // Creator column
        creatorColumn.setCellValueFactory(cellData -> {
            Integer creatorId = cellData.getValue().getCreated_by_id();
            return new javafx.beans.property.SimpleStringProperty(
                    creatorId != null ? "User #" + creatorId : "Unknown"
            );
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

        // Set simple cell factories that force black text
        setSimpleCellFactories();

        // Apply table styling
        missionTable.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #ddd;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 0;" +
                        "-fx-control-inner-background: white;" +
                        "-fx-text-fill: black;" +
                        "-fx-font-size: 14px;"
        );

        // Style column headers
        String headerStyle =
                "-fx-background-color: #0a66c2;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 12 15;" +
                        "-fx-alignment: CENTER;";

        idColumn.setStyle(headerStyle);
        descriptionColumn.setStyle(headerStyle);
        scoreColumn.setStyle(headerStyle);
        creatorColumn.setStyle(headerStyle);
        dateColumn.setStyle(headerStyle);
    }

    /**
     * Add Actions column with Edit and Delete buttons
     */
    private void addActionsColumn() {
        TableColumn<Mission, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(120);
        actionsCol.setStyle("-fx-alignment: CENTER;");

        // Add graphic to header
        Label headerIcon = new Label("⚙️");
        headerIcon.setStyle("-fx-font-size: 14px;");
        actionsCol.setGraphic(headerIcon);

        actionsCol.setCellFactory(col -> new TableCell<Mission, Void>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final HBox buttons = new HBox(5, editBtn, deleteBtn);

            {
                buttons.setAlignment(javafx.geometry.Pos.CENTER);

                // Style buttons
                editBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4; -fx-cursor: hand; -fx-font-size: 12px;");
                deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4; -fx-cursor: hand; -fx-font-size: 12px;");

                // Tooltips
                editBtn.setTooltip(new Tooltip("Edit Mission"));
                deleteBtn.setTooltip(new Tooltip("Delete Mission"));

                // Button actions
                editBtn.setOnAction(e -> {
                    Mission mission = getTableView().getItems().get(getIndex());
                    editMission(mission);
                });

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
                    setGraphic(buttons);
                }
            }
        });

        missionTable.getColumns().add(actionsCol);
    }

    private void setSimpleCellFactories() {
        // ID Column
        idColumn.setCellFactory(col -> new TableCell<Mission, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(item));
                    setStyle("-fx-text-fill: #0a66c2; -fx-alignment: CENTER; -fx-font-weight: bold;");
                }
            }
        });

        // Description Column
        descriptionColumn.setCellFactory(col -> new TableCell<Mission, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    // Show first 100 characters
                    String displayText = item.length() > 100 ? item.substring(0, 100) + "..." : item;
                    setText(displayText);

                    // Tooltip with full description
                    if (item.length() > 100) {
                        Tooltip tooltip = new Tooltip(item);
                        tooltip.setStyle("-fx-font-size: 12px;");
                        setTooltip(tooltip);
                    }

                    setStyle("-fx-text-fill: black; -fx-alignment: CENTER_LEFT;");
                }
            }
        });

        // Score Column
        scoreColumn.setCellFactory(col -> new TableCell<Mission, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item + "%");

                    // Color based on score
                    String color;
                    if (item >= 80) {
                        color = "#10b981";
                    } else if (item >= 60) {
                        color = "#f59e0b";
                    } else {
                        color = "#ef4444";
                    }

                    setStyle("-fx-text-fill: " + color + "; -fx-alignment: CENTER; -fx-font-weight: bold;");
                }
            }
        });

        // Creator Column
        creatorColumn.setCellFactory(col -> new TableCell<Mission, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #3b82f6; -fx-alignment: CENTER; -fx-font-weight: bold;");
                }
            }
        });

        // Date Column
        dateColumn.setCellFactory(col -> new TableCell<Mission, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #6b7280; -fx-alignment: CENTER; -fx-font-weight: bold;");
                }
            }
        });
    }

    private void forceBlackText() {
        // Force black text on all existing cells
        missionTable.refresh();

        // Also style the table rows
        missionTable.setRowFactory(tv -> {
            TableRow<Mission> row = new TableRow<Mission>() {
                @Override
                protected void updateItem(Mission item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("");
                    } else {
                        // Alternate row colors for better readability
                        if (getIndex() % 2 == 0) {
                            setStyle("-fx-background-color: white;");
                        } else {
                            setStyle("-fx-background-color: #f8f9fa;");
                        }
                    }
                }
            };

            // Double click handler
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Mission selectedMission = row.getItem();
                    navigateToRenduAdd(selectedMission);
                }
            });

            return row;
        });
    }

    private void loadMissions() {
        try {
            missionTable.setItems(
                    FXCollections.observableArrayList(missionService.read())
            );

            // Calculate stats
            calculateStats();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Load Error", e.getMessage());
            e.printStackTrace();
        }
    }

    private void calculateStats() {
        try {
            int total = missionTable.getItems().size();
            lblTotalMissions.setText(String.valueOf(total));

            // Calculate average score
            if (total > 0) {
                double avgScore = missionTable.getItems().stream()
                        .mapToInt(Mission::getScore_min)
                        .average()
                        .orElse(0);
                lblAvgScore.setText(String.format("%.0f%%", avgScore));
            } else {
                lblAvgScore.setText("0%");
            }

            // Count active missions (score > 0)
            long active = missionTable.getItems().stream()
                    .filter(m -> m.getScore_min() > 0)
                    .count();
            lblActiveMissions.setText(String.valueOf(active));

        } catch (Exception e) {
            System.err.println("Error calculating stats: " + e.getMessage());
        }
    }

    private void setupDoubleClickHandler() {
        missionTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Mission selected = missionTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    navigateToRenduAdd(selected);
                }
            }
        });
    }

    private void navigateToRenduAdd(Mission mission) {
        try {
            MissionShellController.getInstance().showRenduAddWithMissionId(mission.getId());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Could not load submission form: " + e.getMessage());
        }
    }

    /**
     * Delete mission (called from Actions column)
     */
    private void deleteMission(Mission selected) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Mission #" + selected.getId());
        confirm.setContentText("Are you sure you want to delete this mission?\n\nDescription: " +
                selected.getDescription() + "\nThis action cannot be undone.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                missionService.supprimer(selected.getId());
                loadMissions();
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Mission #" + selected.getId() + " deleted successfully!");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Delete Error", e.getMessage());
            }
        }
    }

    /**
     * Edit mission (called from Actions column)
     */
    private void editMission(Mission selected) {
        MissionShellController.getInstance().showEditMission(selected);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}