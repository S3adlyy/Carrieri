package com.example.guser.controllers.grecru;

import com.example.guser.controllers.guser.AppNavController;
import utils.grecru.AlertUtils;
import entities.grecru.RenduMission;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import services.grecru.RenduMissionService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class RenduListController implements Initializable {

    @FXML private TableView<RenduMission> table;
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
        setupDoubleClickHandler();
        Platform.runLater(() -> {
            System.out.println("table height=" + table.getHeight());
            System.out.println("table prefHeight=" + table.getPrefHeight());
            System.out.println("items=" + table.getItems().size());
        });


        // Apply table styling after table is populated
        Platform.runLater(this::applyTableStyling);
    }

    private void configureTable() {
        // Configure columns with correct property names
        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));
        colResultat.setCellValueFactory(new PropertyValueFactory<>("resultat"));
        colMission.setCellValueFactory(new PropertyValueFactory<>("missionId"));
        colCandidat.setCellValueFactory(new PropertyValueFactory<>("candidatId"));

        // Custom cell factory for Score with progress bar (YOUR ORIGINAL)
        colScore.setCellFactory(new Callback<TableColumn<RenduMission, Integer>, TableCell<RenduMission, Integer>>() {
            @Override
            public TableCell<RenduMission, Integer> call(TableColumn<RenduMission, Integer> param) {
                return new TableCell<RenduMission, Integer>() {
                    private final ProgressBar progressBar = new ProgressBar();
                    private final Label scoreLabel = new Label();
                    private final HBox container = new HBox(10, progressBar, scoreLabel);

                    {
                        container.setAlignment(Pos.CENTER_LEFT);
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

        // Custom cell factory for Result with colored badges (YOUR ORIGINAL)
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
                            badge.setAlignment(Pos.CENTER);
                            badge.setStyle("-fx-padding: 4 12; -fx-background-radius: 20; -fx-font-size: 12px; -fx-font-weight: bold;");

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

        // Custom cell factory for Mission (YOUR ORIGINAL)
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
                            container.setAlignment(Pos.CENTER_LEFT);
                            setGraphic(container);
                            setText(null);
                        }
                    }
                };
            }
        });

        // Custom cell factory for Candidate (YOUR ORIGINAL)
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
                            container.setAlignment(Pos.CENTER_LEFT);
                            setGraphic(container);
                            setText(null);
                        }
                    }
                };
            }
        });

        // Add Actions column with buttons (YOUR ORIGINAL)
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
                        buttons.setAlignment(Pos.CENTER);

                        viewBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4; -fx-cursor: hand;");
                        codeBtn.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4; -fx-cursor: hand;");
                        deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4; -fx-cursor: hand;");

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
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    /**
     * Table styling - kept exactly as you had it
     */
    private void applyTableStyling() {
        // ============ MISSIONLIST TABLE STYLING ============

        // 1. Table background and border styling
        table.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 5;"
        );

        // 2. Header styling (purple gradient like MissionList)
        String headerStyle =
                "-fx-background-color: linear-gradient(to right, #faf5ff, #f3e8ff);" +
                        "-fx-text-fill: #5b21b6;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 12px 10px;" +
                        "-fx-border-color: transparent transparent #f3e8ff transparent;" +
                        "-fx-border-width: 0 0 2 0;";

        colScore.setStyle(headerStyle);
        colResultat.setStyle(headerStyle);
        colMission.setStyle(headerStyle);
        colCandidat.setStyle(headerStyle);

        // Style the Actions column header
        for (TableColumn<?, ?> col : table.getColumns()) {
            if (col.getText().equals("Actions")) {
                col.setStyle(headerStyle);
                break;
            }
        }

        // 3. Row styling with alternating colors and hover effect
        table.setRowFactory(tv -> {
            TableRow<RenduMission> row = new TableRow<RenduMission>() {
                @Override
                protected void updateItem(RenduMission item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("");
                    } else {
                        // Alternating row colors
                        if (getIndex() % 2 == 0) {
                            setStyle("-fx-background-color: white;");
                        } else {
                            setStyle("-fx-background-color: #faf9ff;");
                        }

                        // Purple hover effect (like MissionList)
                        setOnMouseEntered(e ->
                                setStyle("-fx-background-color: #f3e8ff;")
                        );
                        setOnMouseExited(e -> {
                            if (getIndex() % 2 == 0) {
                                setStyle("-fx-background-color: white;");
                            } else {
                                setStyle("-fx-background-color: #faf9ff;");
                            }
                        });
                    }
                }
            };

            // Double-click handler
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    RenduMission selectedRendu = row.getItem();

                    // Debug information
                    System.out.println("=== DOUBLE CLICK DEBUG ===");
                    System.out.println("Row index: " + row.getIndex());
                    System.out.println("Row empty? " + row.isEmpty());
                    System.out.println("Selected rendu object: " + selectedRendu);

                    if (selectedRendu != null) {
                        System.out.println("Rendu ID: " + selectedRendu.getId());
                        System.out.println("Rendu Score: " + selectedRendu.getScore());
                        System.out.println("Rendu Mission ID: " + selectedRendu.getMissionId());
                        scheduleInterviewWithCandidate(selectedRendu);
                    } else {
                        System.out.println("ERROR: selectedRendu is null even though row.isEmpty() is false!");

                        // Alternative: try to get from table's selection model
                        RenduMission fromTable = table.getSelectionModel().getSelectedItem();
                        System.out.println("From table selection model: " + fromTable);

                        if (fromTable != null) {
                            scheduleInterviewWithCandidate(fromTable);
                        }
                    }
                }
            });
            return row;
        });

        // 4. Style the filter controls to match the purple theme
        String comboBoxStyle =
                "-fx-background-color: white;" +
                        "-fx-border-color: #f3e8ff;" +
                        "-fx-border-radius: 20;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 4 12;";

        if (filterStatus != null) filterStatus.setStyle(comboBoxStyle);
        if (filterMission != null) filterMission.setStyle(comboBoxStyle);

        if (searchField != null) {
            searchField.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-border-color: #f3e8ff;" +
                            "-fx-border-radius: 20;" +
                            "-fx-background-radius: 20;" +
                            "-fx-padding: 8 16;"
            );
            searchField.setPromptText("🔍 Search...");
        }

        // 5. Style the stats labels to match MissionList
        String statsValueStyle =
                "-fx-font-size: 28px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #5b21b6;";

        String statsLabelStyle =
                "-fx-font-size: 14px;" +
                        "-fx-text-fill: #6b7280;";

        if (lblTotal != null) lblTotal.setStyle(statsValueStyle);
        if (lblSuccessRate != null) lblSuccessRate.setStyle(statsValueStyle);
        if (lblAvgScore != null) lblAvgScore.setStyle(statsValueStyle);

        table.refresh();
    }

    // ============ UPDATED ALERT METHODS ============


    /**
     * Show styled submission details alert
     */
    private void viewDetails(RenduMission rendu) {
        String content = String.format(
                "📊 Score: %d%%\n" +
                        "🏷️ Status: %s\n" +
                        "🎯 Mission ID: %d\n" +
                        "👤 Candidate ID: %d\n" +
                        "📅 Date: %s\n" +
                        "💬 Feedback: %s\n" +
                        "🌐 Language: %s",
                rendu.getScore(),
                rendu.getResultat(),
                rendu.getMissionId(),
                rendu.getCandidatId(),
                rendu.getDateRendu() != null ? rendu.getDateRendu() : "N/A",
                rendu.getFeedback() != null ? rendu.getFeedback() : "No feedback",
                rendu.getLangue() != null ? rendu.getLangue() : "Python"
        );

        AlertUtils.showInfo("Submission Details", content);
    }

    /**
     * Show styled code view dialog
     */
    private void viewCode(RenduMission rendu) {
        TextArea codeArea = new TextArea(rendu.getCodeSolution());
        codeArea.setEditable(false);
        codeArea.setWrapText(true);
        codeArea.setPrefSize(700, 500);
        codeArea.setStyle("-fx-font-family: 'Monaco', 'Consolas', monospace; -fx-font-size: 14px; -fx-background-color: #1e1e2f; -fx-text-fill: #e0e0e0;");

        Label header = new Label("📝 Submitted Code");
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #5b21b6;");

        Label metaInfo = new Label(String.format(
                "Score: %d%% | Status: %s | Mission #%d | Candidate #%d",
                rendu.getScore(),
                rendu.getResultat(),
                rendu.getMissionId(),
                rendu.getCandidatId()
        ));
        metaInfo.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

        VBox container = new VBox(10, header, metaInfo, codeArea);
        container.setPadding(new Insets(20));

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Submitted Code");
        dialog.getDialogPane().setContent(container);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(720, 600);

        dialog.showAndWait();
    }

    /**
     * Show styled delete confirmation alert
     */
    private void deleteRendu(RenduMission rendu) {
        String details = String.format(
                "📊 Score: %d%%\n" +
                        "🏷️ Status: %s\n" +
                        "🎯 Mission #%d\n" +
                        "👤 Candidate #%d",
                rendu.getScore(),
                rendu.getResultat(),
                rendu.getMissionId(),
                rendu.getCandidatId()
        );

        if (AlertUtils.showDeleteConfirmation("submission", details,
                "❌ This action cannot be undone.")) {
            try {
                service.supprimerRenduMission(rendu.getId());
                load();
                AlertUtils.showSuccess("Success", "Submission deleted successfully.");
            } catch (Exception e) {
                AlertUtils.showError("Delete Error", "Failed to delete submission: " + e.getMessage());
            }
        }
    }

    /**
     * Show styled bulk delete confirmation alert
     */
    @FXML
    private void deleteSelected() {
        List<RenduMission> selected = table.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            AlertUtils.showWarning("No Selection", "Please select one or more submissions to delete.");
            return;
        }

        StringBuilder details = new StringBuilder();
        for (int i = 0; i < Math.min(selected.size(), 5); i++) {
            RenduMission r = selected.get(i);
            details.append("• ").append(r.getResultat()).append(" - Score: ").append(r.getScore()).append("%\n");
        }
        if (selected.size() > 5) {
            details.append("• ... and ").append(selected.size() - 5).append(" more\n");
        }

        if (AlertUtils.showDeleteConfirmation(
                selected.size() + " submission(s)",
                details.toString(),
                "❌ This action cannot be undone.")) {

            int successCount = 0;
            int failCount = 0;
            StringBuilder errors = new StringBuilder();

            for (RenduMission rendu : selected) {
                try {
                    service.supprimerRenduMission(rendu.getId());
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    errors.append("• Failed to delete submission #").append(rendu.getId())
                            .append(": ").append(e.getMessage()).append("\n");
                }
            }

            load();

            if (failCount == 0) {
                AlertUtils.showSuccess("Deletion Complete",
                        "✓ Successfully deleted " + successCount + " submission(s).");
            } else {
                AlertUtils.showWarning("Deletion Partial",
                        "✓ Successfully deleted: " + successCount + "\n" +
                                "✗ Failed to delete: " + failCount + "\n\n" +
                                errors.toString());
            }
        }
    }

    /**
     * Show styled export success alert
     */
    @FXML
    private void exportToCSV() {
        try {
            List<RenduMission> itemsToExport = table.getSelectionModel().getSelectedItems();
            if (itemsToExport.isEmpty()) {
                itemsToExport = table.getItems();
            }

            if (itemsToExport.isEmpty()) {
                AlertUtils.showInfo("Info", "No data to export.");
                return;
            }

            StringBuilder csv = new StringBuilder();
            csv.append("Score,Result,Mission ID,Candidate ID,Feedback\n");

            for (RenduMission rendu : itemsToExport) {
                csv.append(rendu.getScore()).append(",")
                        .append("\"").append(rendu.getResultat().replace("\"", "\"\"")).append("\",")
                        .append(rendu.getMissionId()).append(",")
                        .append(rendu.getCandidatId()).append(",")
                        .append("\"").append(rendu.getFeedback() != null ? rendu.getFeedback().replace("\"", "\"\"") : "").append("\"\n");
            }

            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Export Submissions to CSV");
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );
            java.io.File file = fileChooser.showSaveDialog(table.getScene().getWindow());

            if (file != null) {
                try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                    writer.write(csv.toString());
                    AlertUtils.showSuccess("Export Successful",
                            "✓ Exported " + itemsToExport.size() + " submissions to:\n" + file.getAbsolutePath());
                }
            }

        } catch (Exception e) {
            AlertUtils.showError("Export Failed", "Failed to export: " + e.getMessage());
        }
    }

    /**
     * Show styled filter help alert
     */
    @FXML
    private void showFilter() {
        AlertUtils.showInfo("Filter Help",
                "Use the filter controls above to filter submissions:\n\n" +
                        "• Status: Filter by submission status\n" +
                        "• Mission: Filter by mission ID\n" +
                        "• Search: Search in candidate IDs and results\n\n" +
                        "💡 Tip: Double-click any row to schedule an interview!"
        );
    }

    /**
     * Show styled statistics info
     */


    // ============ ALL YOUR ORIGINAL METHODS BELOW - UNCHANGED ============

    @FXML
    private void load() {
        try {
            List<RenduMission> rendus = service.afficherRenduMissions();
            renduList = FXCollections.observableArrayList(rendus);
            filteredData = new FilteredList<>(renduList, p -> true);
            SortedList<RenduMission> sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(table.comparatorProperty());
            table.setItems(sortedData);
            System.out.println("items in table = " + table.getItems().size());
            table.refresh();

            updateStats();
            populateMissionFilter();
            System.out.println("✅ Loaded " + rendus.size() + " submissions");
        } catch (Exception e) {
            AlertUtils.showError("Error", "Failed to load submissions: " + e.getMessage());
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

        double avgScore = renduList.stream()
                .mapToInt(RenduMission::getScore)
                .average()
                .orElse(0);
        lblAvgScore.setText(String.format("%.1f%%", avgScore));
    }

    private void setupFilters() {
        filterStatus.getItems().addAll("All", "Accepted", "Rejected", "Pending", "Error");
        filterStatus.setValue("All");

        filterStatus.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            applyFilters();
        });

        filterMission.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            applyFilters();
        });

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

            String missionFilter = filterMission.getValue();
            if (missionFilter != null && !missionFilter.equals("All Missions")) {
                try {
                    int missionId = Integer.parseInt(missionFilter);
                    if (rendu.getMissionId() != missionId) return false;
                } catch (NumberFormatException e) {}
            }

            String searchText = searchField.getText().toLowerCase();
            if (searchText != null && !searchText.isEmpty()) {
                boolean matchesSearch = String.valueOf(rendu.getCandidatId()).contains(searchText) ||
                        String.valueOf(rendu.getMissionId()).contains(searchText) ||
                        rendu.getResultat().toLowerCase().contains(searchText) ||
                        String.valueOf(rendu.getScore()).contains(searchText);

                if (!matchesSearch) return false;
            }

            return true;
        });

        updateStats();
    }

    @FXML
    private void clearFilters() {
        filterStatus.setValue("All");
        filterMission.setValue("All Missions");
        searchField.clear();
        applyFilters();
    }

    @FXML
    private void viewDetails() {
        RenduMission selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            viewDetails(selected);
        } else {
            AlertUtils.showWarning("No Selection", "Please select a submission first.");
        }
    }

    private void setupDoubleClickHandler() {
        // Double-click is handled in the row factory
    }

    private void scheduleInterviewWithCandidate(RenduMission rendu) {
        try {
            System.out.println("🎯 Scheduling interview for submission");
            System.out.println("Rendu after first passing in rendu list: "+rendu);
            AppNavController.getInstance().showScheduleInterview(rendu);
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("Navigation Error",
                    "Could not load interview scheduling form: " + e.getMessage());
        }
    }

    private HBox createScoreDisplay(int score) {
        ProgressBar progressBar = new ProgressBar(score / 100.0);
        progressBar.getStyleClass().add("rendu-progress-bar");

        if (score >= 80) {
            progressBar.setStyle("-fx-accent: #10b981;");
        } else if (score >= 60) {
            progressBar.setStyle("-fx-accent: #f59e0b;");
        } else if (score >= 40) {
            progressBar.setStyle("-fx-accent: #f97316;");
        } else {
            progressBar.setStyle("-fx-accent: #ef4444;");
        }

        Label scoreLabel = new Label(score + "%");
        scoreLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e1b3a; -fx-font-size: 14px;");

        HBox container = new HBox(12, progressBar, scoreLabel);
        container.setAlignment(Pos.CENTER_LEFT);

        return container;
    }

    private Label createStatusBadge(String status) {
        Label badge = new Label(status);
        badge.getStyleClass().add("status-badge");

        String statusLower = status.toLowerCase();
        if (statusLower.contains("accepted") || statusLower.contains("success") ||
                statusLower.contains("passed") || statusLower.contains("réussi")) {
            badge.getStyleClass().add("status-accepted");
        } else if (statusLower.contains("rejected") || statusLower.contains("failed") ||
                statusLower.contains("échec")) {
            badge.getStyleClass().add("status-rejected");
        } else if (statusLower.contains("pending") || statusLower.contains("en attente")) {
            badge.getStyleClass().add("status-pending");
        } else if (statusLower.contains("error") || statusLower.contains("erreur")) {
            badge.getStyleClass().add("status-error");
        } else {
            badge.setStyle("-fx-background-color: #f3e8ff; -fx-text-fill: #5b21b6; " +
                    "-fx-border-color: #e9d5ff; -fx-border-width: 1; -fx-border-radius: 30;");
        }

        return badge;
    }

    private HBox createMissionBadge(int missionId) {
        Label icon = new Label("🎯");
        icon.setStyle("-fx-font-size: 14px;");

        Label idLabel = new Label("Mission #" + missionId);
        idLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 13px;");

        HBox container = new HBox(8, icon, idLabel);
        container.getStyleClass().add("mission-badge");
        container.setAlignment(Pos.CENTER_LEFT);

        return container;
    }

    /**
     * Creates a styled candidate badge
     */
    private HBox createCandidateBadge(int candidateId) {
        Label icon = new Label("👤");
        icon.setStyle("-fx-font-size: 14px;");

        Label idLabel = new Label("Candidate #" + candidateId);
        idLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 13px;");

        HBox container = new HBox(8, icon, idLabel);
        container.getStyleClass().add("candidate-badge");
        container.setAlignment(Pos.CENTER_LEFT);

        return container;
    }
    @FXML
    private void showStatistics() {
        AppNavController.getInstance().showStatistics();
    }
}