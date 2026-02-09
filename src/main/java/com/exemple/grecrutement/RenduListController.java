package com.exemple.grecrutement;

import entities.RenduMission;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;
import services.RenduMissionService;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class RenduListController implements Initializable {

    @FXML private TableView<RenduMission> table;
    @FXML private TableColumn<RenduMission, Integer> colId;
    @FXML private TableColumn<RenduMission, Integer> colScore;
    @FXML private TableColumn<RenduMission, String> colResultat;
    @FXML private TableColumn<RenduMission, Integer> colMission;
    @FXML private TableColumn<RenduMission, Integer> colCandidat;

    @FXML private Label lblTotal;
    @FXML private Label lblSuccessRate;
    @FXML private Label lblAvgScore;
    @FXML private ComboBox<String> filterStatus;
    @FXML private ComboBox<String> filterMission;
    @FXML private TextField searchField;

    private ObservableList<RenduMission> renduList;
    private FilteredList<RenduMission> filteredData;
    private RenduMissionService service = new RenduMissionService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configureTable();
        load();
        setupFilters();
    }

    private void configureTable() {
        // Configure columns with correct property names from your RenduMission entity
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));
        colResultat.setCellValueFactory(new PropertyValueFactory<>("resultat"));
        colMission.setCellValueFactory(new PropertyValueFactory<>("missionId"));
        colCandidat.setCellValueFactory(new PropertyValueFactory<>("candidatId"));

        // Custom cell factory for Score with progress bar
        colScore.setCellFactory(new Callback<TableColumn<RenduMission, Integer>, TableCell<RenduMission, Integer>>() {
            @Override
            public TableCell<RenduMission, Integer> call(TableColumn<RenduMission, Integer> param) {
                return new TableCell<RenduMission, Integer>() {
                    private final ProgressBar progressBar = new ProgressBar();
                    private final Label scoreLabel = new Label();
                    private final HBox container = new HBox(10, progressBar, scoreLabel);

                    {
                        container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                        progressBar.setPrefWidth(80);
                        scoreLabel.setStyle("-fx-font-weight: bold;");
                    }

                    @Override
                    protected void updateItem(Integer score, boolean empty) {
                        super.updateItem(score, empty);

                        if (empty || score == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            double progress = score / 100.0;
                            progressBar.setProgress(progress);

                            // Set color based on score
                            if (score >= 80) {
                                progressBar.setStyle("-fx-accent: #10b981;");
                                scoreLabel.setTextFill(Color.web("#10b981"));
                            } else if (score >= 60) {
                                progressBar.setStyle("-fx-accent: #f59e0b;");
                                scoreLabel.setTextFill(Color.web("#f59e0b"));
                            } else if (score >= 40) {
                                progressBar.setStyle("-fx-accent: #f97316;");
                                scoreLabel.setTextFill(Color.web("#f97316"));
                            } else {
                                progressBar.setStyle("-fx-accent: #ef4444;");
                                scoreLabel.setTextFill(Color.web("#ef4444"));
                            }

                            scoreLabel.setText(score + "%");
                            setGraphic(container);
                            setText(null);
                        }
                    }
                };
            }
        });

        // Custom cell factory for Result with colored badges
        colResultat.setCellFactory(new Callback<TableColumn<RenduMission, String>, TableCell<RenduMission, String>>() {
            @Override
            public TableCell<RenduMission, String> call(TableColumn<RenduMission, String> param) {
                return new TableCell<RenduMission, String>() {
                    @Override
                    protected void updateItem(String resultat, boolean empty) {
                        super.updateItem(resultat, empty);

                        if (empty || resultat == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            Label badge = new Label(resultat);
                            badge.setMaxWidth(Double.MAX_VALUE);
                            badge.setAlignment(javafx.geometry.Pos.CENTER);
                            badge.setStyle("-fx-padding: 4 12; -fx-background-radius: 20; -fx-font-size: 12px; -fx-font-weight: bold;");

                            // Determine status color
                            String resultLower = resultat.toLowerCase();
                            if (resultLower.contains("accepted") || resultLower.contains("success") ||
                                    resultLower.contains("passed") || resultLower.contains("réussi")) {
                                badge.setStyle(badge.getStyle() + "-fx-background-color: #10b981; -fx-text-fill: white;");
                            } else if (resultLower.contains("rejected") || resultLower.contains("failed") ||
                                    resultLower.contains("fail") || resultLower.contains("échec")) {
                                badge.setStyle(badge.getStyle() + "-fx-background-color: #ef4444; -fx-text-fill: white;");
                            } else if (resultLower.contains("pending") || resultLower.contains("en attente")) {
                                badge.setStyle(badge.getStyle() + "-fx-background-color: #f59e0b; -fx-text-fill: white;");
                            } else if (resultLower.contains("error") || resultLower.contains("erreur")) {
                                badge.setStyle(badge.getStyle() + "-fx-background-color: #6b7280; -fx-text-fill: white;");
                            } else {
                                badge.setStyle(badge.getStyle() + "-fx-background-color: #3b82f6; -fx-text-fill: white;");
                            }

                            setGraphic(badge);
                            setText(null);
                        }
                    }
                };
            }
        });

        // Custom cell factory for Mission ID
        colMission.setCellFactory(new Callback<TableColumn<RenduMission, Integer>, TableCell<RenduMission, Integer>>() {
            @Override
            public TableCell<RenduMission, Integer> call(TableColumn<RenduMission, Integer> param) {
                return new TableCell<RenduMission, Integer>() {
                    @Override
                    protected void updateItem(Integer missionId, boolean empty) {
                        super.updateItem(missionId, empty);

                        if (empty || missionId == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            HBox container = new HBox(8);
                            Label icon = new Label("🎯");
                            Label idLabel = new Label("Mission #" + missionId);
                            idLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #3b82f6;");
                            container.getChildren().addAll(icon, idLabel);
                            container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                            setGraphic(container);
                            setText(null);
                        }
                    }
                };
            }
        });

        // Custom cell factory for Candidate ID
        colCandidat.setCellFactory(new Callback<TableColumn<RenduMission, Integer>, TableCell<RenduMission, Integer>>() {
            @Override
            public TableCell<RenduMission, Integer> call(TableColumn<RenduMission, Integer> param) {
                return new TableCell<RenduMission, Integer>() {
                    @Override
                    protected void updateItem(Integer candidatId, boolean empty) {
                        super.updateItem(candidatId, empty);

                        if (empty || candidatId == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            HBox container = new HBox(8);
                            Label icon = new Label("👤");
                            Label idLabel = new Label("Candidate #" + candidatId);
                            idLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #8b5cf6;");
                            container.getChildren().addAll(icon, idLabel);
                            container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                            setGraphic(container);
                            setText(null);
                        }
                    }
                };
            }
        });

        // Add Actions column with buttons
        TableColumn<RenduMission, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(150);
        actionsCol.setCellFactory(new Callback<TableColumn<RenduMission, Void>, TableCell<RenduMission, Void>>() {
            @Override
            public TableCell<RenduMission, Void> call(TableColumn<RenduMission, Void> param) {
                return new TableCell<RenduMission, Void>() {
                    private final Button viewBtn = new Button("👁️");
                    private final Button codeBtn = new Button("📝");
                    private final Button deleteBtn = new Button("🗑️");
                    private final HBox buttons = new HBox(5, viewBtn, codeBtn, deleteBtn);

                    {
                        buttons.setAlignment(javafx.geometry.Pos.CENTER);

                        // Style buttons
                        viewBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4; -fx-cursor: hand;");
                        codeBtn.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4; -fx-cursor: hand;");
                        deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4; -fx-cursor: hand;");

                        // Button actions
                        viewBtn.setOnAction(e -> {
                            RenduMission rendu = getTableView().getItems().get(getIndex());
                            viewDetails(rendu);
                        });

                        codeBtn.setOnAction(e -> {
                            RenduMission rendu = getTableView().getItems().get(getIndex());
                            viewCode(rendu);
                        });

                        deleteBtn.setOnAction(e -> {
                            RenduMission rendu = getTableView().getItems().get(getIndex());
                            deleteRendu(rendu);
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
                };
            }
        });

        table.getColumns().add(actionsCol);

        // Enable multiple selection
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    @FXML
    private void load() {
        try {
            // Use your service's afficherRenduMissions() method
            List<RenduMission> rendus = service.afficherRenduMissions();
            renduList = FXCollections.observableArrayList(rendus);

            // Setup filtered list
            filteredData = new FilteredList<>(renduList, p -> true);

            // Setup sorted list
            SortedList<RenduMission> sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(table.comparatorProperty());

            table.setItems(sortedData);
            updateStats();

            // Populate mission filter
            populateMissionFilter();

            System.out.println("✅ Loaded " + rendus.size() + " submissions");

        } catch (Exception e) {
            showAlert("Error", "Failed to load submissions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateStats() {
        if (renduList == null || renduList.isEmpty()) {
            lblTotal.setText("0");
            lblSuccessRate.setText("0%");
            lblAvgScore.setText("0%");
            return;
        }

        lblTotal.setText(String.valueOf(renduList.size()));

        // Count accepted submissions (adjust based on your result strings)
        long acceptedCount = renduList.stream()
                .filter(r -> {
                    String result = r.getResultat().toLowerCase();
                    return result.contains("accepted") ||
                            result.contains("success") ||
                            result.contains("passed") ||
                            result.contains("réussi");
                })
                .count();

        double successRate = (double) acceptedCount / renduList.size() * 100;
        lblSuccessRate.setText(String.format("%.1f%%", successRate));

        // Calculate average score
        double avgScore = renduList.stream()
                .mapToInt(RenduMission::getScore)
                .average()
                .orElse(0);
        lblAvgScore.setText(String.format("%.1f%%", avgScore));
    }

    private void setupFilters() {
        // Initialize filter options
        filterStatus.getItems().addAll("All", "Accepted", "Rejected", "Pending", "Error");
        filterStatus.setValue("All");

        // Add listener for status filter
        filterStatus.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            applyFilters();
        });

        // Add listener for mission filter
        filterMission.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            applyFilters();
        });

        // Add listener for search field
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }

    private void populateMissionFilter() {
        if (renduList != null) {
            List<String> missionIds = renduList.stream()
                    .map(r -> String.valueOf(r.getMissionId()))
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            filterMission.getItems().clear();
            filterMission.getItems().add("All Missions");
            filterMission.getItems().addAll(missionIds);
            filterMission.setValue("All Missions");
        }
    }

    @FXML
    private void applyFilters() {
        if (filteredData == null) return;

        filteredData.setPredicate(rendu -> {
            // Status filter
            String statusFilter = filterStatus.getValue();
            if (statusFilter != null && !statusFilter.equals("All")) {
                String result = rendu.getResultat().toLowerCase();
                boolean matchesStatus = false;

                switch (statusFilter) {
                    case "Accepted":
                        matchesStatus = result.contains("accepted") ||
                                result.contains("success") ||
                                result.contains("passed");
                        break;
                    case "Rejected":
                        matchesStatus = result.contains("rejected") ||
                                result.contains("failed") ||
                                result.contains("échec");
                        break;
                    case "Pending":
                        matchesStatus = result.contains("pending") ||
                                result.contains("en attente");
                        break;
                    case "Error":
                        matchesStatus = result.contains("error") ||
                                result.contains("erreur");
                        break;
                }

                if (!matchesStatus) return false;
            }

            // Mission filter
            String missionFilter = filterMission.getValue();
            if (missionFilter != null && !missionFilter.equals("All Missions")) {
                try {
                    int missionId = Integer.parseInt(missionFilter);
                    if (rendu.getMissionId() != missionId) return false;
                } catch (NumberFormatException e) {
                    // If not a number, skip this filter
                }
            }

            // Search filter
            String searchText = searchField.getText().toLowerCase();
            if (searchText != null && !searchText.isEmpty()) {
                // Search in candidate ID
                boolean matchesSearch = String.valueOf(rendu.getCandidatId()).contains(searchText) ||
                        String.valueOf(rendu.getMissionId()).contains(searchText) ||
                        rendu.getResultat().toLowerCase().contains(searchText) ||
                        String.valueOf(rendu.getScore()).contains(searchText);

                if (!matchesSearch) return false;
            }

            return true;
        });

        updateStats(); // Update stats based on filtered data
    }

    @FXML
    private void clearFilters() {
        filterStatus.setValue("All");
        filterMission.setValue("All Missions");
        searchField.clear();
        applyFilters();
    }

    @FXML
    private void exportToCSV() {
        try {
            // Get selected or all items
            List<RenduMission> itemsToExport = table.getSelectionModel().getSelectedItems();
            if (itemsToExport.isEmpty()) {
                itemsToExport = table.getItems();
            }

            if (itemsToExport.isEmpty()) {
                showAlert("Info", "No data to export.");
                return;
            }

            StringBuilder csv = new StringBuilder();
            // Header
            csv.append("ID,Score,Result,Mission ID,Candidate ID,Feedback\n");

            // Data
            for (RenduMission rendu : itemsToExport) {
                csv.append(rendu.getId()).append(",")
                        .append(rendu.getScore()).append(",")
                        .append("\"").append(rendu.getResultat().replace("\"", "\"\"")).append("\",")
                        .append(rendu.getMissionId()).append(",")
                        .append(rendu.getCandidatId()).append(",")
                        .append("\"").append(rendu.getFeedback() != null ? rendu.getFeedback().replace("\"", "\"\"") : "").append("\"\n");
            }

            // Create and show file chooser
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Export Submissions to CSV");
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );
            java.io.File file = fileChooser.showSaveDialog(table.getScene().getWindow());

            if (file != null) {
                try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                    writer.write(csv.toString());
                    showAlert("Success", "Exported " + itemsToExport.size() + " submissions to:\n" + file.getAbsolutePath());
                }
            }

        } catch (Exception e) {
            showAlert("Error", "Failed to export: " + e.getMessage());
        }
    }

    @FXML
    private void viewDetails() {
        RenduMission selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            viewDetails(selected);
        } else {
            showAlert("No Selection", "Please select a submission first.");
        }
    }

    private void viewDetails(RenduMission rendu) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Submission Details");
        alert.setHeaderText("Submission #" + rendu.getId());
        alert.setContentText(
                "📊 Score: " + rendu.getScore() + "%\n" +
                        "🏷️ Status: " + rendu.getResultat() + "\n" +
                        "🎯 Mission ID: " + rendu.getMissionId() + "\n" +
                        "👤 Candidate ID: " + rendu.getCandidatId() + "\n" +
                        "📅 Date: " + rendu.getDateRendu() + "\n" +
                        "💬 Feedback: " + (rendu.getFeedback() != null ? rendu.getFeedback() : "No feedback") + "\n" +
                        "🌐 Language: " + (rendu.getLangue() != null ? rendu.getLangue() : "Python")
        );
        alert.setGraphic(new Label("📋"));
        alert.showAndWait();
    }

    private void viewCode(RenduMission rendu) {
        TextArea codeArea = new TextArea(rendu.getCodeSolution());
        codeArea.setEditable(false);
        codeArea.setWrapText(true);
        codeArea.setPrefSize(700, 500);
        codeArea.setStyle("-fx-font-family: 'Monaco', 'Consolas', monospace; -fx-font-size: 14px;");

        // Add line numbers
        VBox container = new VBox();
        container.setSpacing(10);

        Label header = new Label("Submitted Code - Submission #" + rendu.getId());
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        container.getChildren().addAll(header, codeArea);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Submitted Code");
        dialog.getDialogPane().setContent(container);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(720, 550);
        dialog.showAndWait();
    }

    private void deleteRendu(RenduMission rendu) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Submission #" + rendu.getId());
        confirm.setContentText("Are you sure you want to delete this submission?\nThis action cannot be undone.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                service.supprimerRenduMission(rendu.getId());
                load(); // Reload the table
                showAlert("Success", "Submission #" + rendu.getId() + " deleted successfully.");
            } catch (Exception e) {
                showAlert("Error", "Failed to delete submission: " + e.getMessage());
            }
        }
    }

    @FXML
    private void deleteSelected() {
        List<RenduMission> selected = table.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showAlert("No Selection", "Please select one or more submissions to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Bulk Deletion");
        confirm.setHeaderText("Delete " + selected.size() + " submission(s)");
        confirm.setContentText("Are you sure you want to delete the selected submissions?\nThis action cannot be undone.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            int successCount = 0;
            int failCount = 0;

            for (RenduMission rendu : selected) {
                try {
                    service.supprimerRenduMission(rendu.getId());
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    System.err.println("Failed to delete submission #" + rendu.getId() + ": " + e.getMessage());
                }
            }

            load(); // Reload the table

            String message = "Deletion completed:\n" +
                    "✓ Successfully deleted: " + successCount + "\n" +
                    (failCount > 0 ? "✗ Failed to delete: " + failCount : "");
            showAlert("Deletion Results", message);
        }
    }

    @FXML
    private void showStatistics() {
        MissionShellController.getInstance().showStatistics();
    }
    @FXML
    private void showFilter() {
        // This can be expanded to show a more advanced filter dialog
        showAlert("Advanced Filter", "Use the filter controls above to filter submissions.\n" +
                "• Status: Filter by submission status\n" +
                "• Mission: Filter by mission ID\n" +
                "• Search: Search in candidate IDs and results");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}