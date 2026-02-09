package com.exemple.grecrutement;

import entities.RenduMission;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.RenduMissionService;

import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.LineChart;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class RenduStatsController implements Initializable {

    @FXML private Label lblTotalSubmissions;
    @FXML private Label lblSuccessRate;
    @FXML private Label lblAvgScore;
    @FXML private Label lblTopPerformer;
    @FXML private Label lblTopScore;
    @FXML private Label lblTotalTrend;
    @FXML private Label lblSuccessTrend;
    @FXML private Label lblAvgScoreTrend;

    @FXML
    private BarChart<String, Number> scoreDistributionChart;

    @FXML
    private PieChart statusPieChart;

    @FXML
    private BarChart<String, Number> missionPerformanceChart;

    @FXML
    private LineChart<String, Number> timelineChart;

    @FXML private Label lblOverallScore;
    @FXML private Label lblMinScore;
    @FXML private Label lblMaxScore;
    @FXML private Label lblSubmissionCount;
    @FXML private Label lblAcceptedCount;
    @FXML private Label lblRejectedCount;
    @FXML private Label lblAvgEvalTime;
    @FXML private Label lblFastestEval;
    @FXML private Label lblSlowestEval;
    @FXML private Label lblLastUpdated;



    private ObservableList<RenduMission> renduList;
    private RenduMissionService service = new RenduMissionService();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadData();
        setupCharts();
        updateLastUpdated();
    }

    private void loadData() {
        try {
            renduList = FXCollections.observableArrayList(service.afficherRenduMissions());
            updateSummaryStats();
            updateDetailedMetrics();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load data: " + e.getMessage());
        }
    }

    private void updateSummaryStats() {
        if (renduList == null || renduList.isEmpty()) {
            setDefaultValues();
            return;
        }

        // Total submissions
        int total = renduList.size();
        lblTotalSubmissions.setText(String.valueOf(total));

        // Success rate
        long acceptedCount = renduList.stream()
                .filter(r -> isAccepted(r.getResultat()))
                .count();
        double successRate = (double) acceptedCount / total * 100;
        lblSuccessRate.setText(String.format("%.1f%%", successRate));

        // Average score
        double avgScore = renduList.stream()
                .mapToInt(RenduMission::getScore)
                .average()
                .orElse(0);
        lblAvgScore.setText(String.format("%.1f%%", avgScore));

        // Top performer
        RenduMission topPerformer = renduList.stream()
                .max(Comparator.comparingInt(RenduMission::getScore))
                .orElse(null);

        if (topPerformer != null) {
            lblTopPerformer.setText("Candidate #" + topPerformer.getCandidatId());
            lblTopScore.setText(String.format("%d%%", topPerformer.getScore()));
        } else {
            lblTopPerformer.setText("N/A");
            lblTopScore.setText("0%");
        }

        // Trends (simulated for now)
        lblTotalTrend.setText("↗ +" + (total > 0 ? total : 0) + "%");
        lblSuccessTrend.setText("↗ +" + String.format("%.1f", successRate) + "%");
        lblAvgScoreTrend.setText("↗ +" + String.format("%.1f", avgScore) + "%");
    }

    private void updateDetailedMetrics() {
        if (renduList == null || renduList.isEmpty()) {
            return;
        }

        // Score metrics
        int minScore = renduList.stream()
                .mapToInt(RenduMission::getScore)
                .min()
                .orElse(0);
        int maxScore = renduList.stream()
                .mapToInt(RenduMission::getScore)
                .max()
                .orElse(0);
        double overallAvg = renduList.stream()
                .mapToInt(RenduMission::getScore)
                .average()
                .orElse(0);

        lblOverallScore.setText(String.format("%.1f%%", overallAvg));
        lblMinScore.setText(String.format("%d%%", minScore));
        lblMaxScore.setText(String.format("%d%%", maxScore));

        // Count metrics
        long acceptedCount = renduList.stream()
                .filter(r -> isAccepted(r.getResultat()))
                .count();
        long rejectedCount = renduList.stream()
                .filter(r -> !isAccepted(r.getResultat()))
                .count();

        lblSubmissionCount.setText(String.valueOf(renduList.size()));
        lblAcceptedCount.setText(String.valueOf(acceptedCount));
        lblRejectedCount.setText(String.valueOf(rejectedCount));

        // Time metrics (simulated for now)
        lblAvgEvalTime.setText("2.4s");
        lblFastestEval.setText("0.8s");
        lblSlowestEval.setText("5.2s");
    }

    private void setupCharts() {
        setupScoreDistributionChart();
        setupStatusPieChart();
        setupMissionPerformanceChart();
        setupTimelineChart();
    }

    private void setupScoreDistributionChart() {
        scoreDistributionChart.setTitle("Score Distribution");

        // Create score ranges
        Map<String, Integer> scoreRanges = new LinkedHashMap<>();
        scoreRanges.put("0-20", 0);
        scoreRanges.put("21-40", 0);
        scoreRanges.put("41-60", 0);
        scoreRanges.put("61-80", 0);
        scoreRanges.put("81-100", 0);

        // Count submissions in each range
        if (renduList != null) {
            for (RenduMission rendu : renduList) {
                int score = rendu.getScore();
                if (score <= 20) scoreRanges.put("0-20", scoreRanges.get("0-20") + 1);
                else if (score <= 40) scoreRanges.put("21-40", scoreRanges.get("21-40") + 1);
                else if (score <= 60) scoreRanges.put("41-60", scoreRanges.get("41-60") + 1);
                else if (score <= 80) scoreRanges.put("61-80", scoreRanges.get("61-80") + 1);
                else scoreRanges.put("81-100", scoreRanges.get("81-100") + 1);
            }
        }

        // Create series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Submissions");

        for (Map.Entry<String, Integer> entry : scoreRanges.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        scoreDistributionChart.getData().clear();
        scoreDistributionChart.getData().add(series);
    }

    private void setupStatusPieChart() {
        statusPieChart.setTitle("Submission Status");

        if (renduList == null || renduList.isEmpty()) {
            statusPieChart.getData().clear();
            return;
        }

        // Count statuses
        Map<String, Long> statusCounts = renduList.stream()
                .collect(Collectors.groupingBy(
                        r -> {
                            String result = r.getResultat().toLowerCase();
                            if (isAccepted(result)) return "Accepted";
                            else if (result.contains("rejected") || result.contains("failed")) return "Rejected";
                            else if (result.contains("pending")) return "Pending";
                            else return "Other";
                        },
                        Collectors.counting()
                ));

        // Create pie chart data
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Map.Entry<String, Long> entry : statusCounts.entrySet()) {
            pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }

        statusPieChart.setData(pieChartData);
    }

    private void setupMissionPerformanceChart() {
        missionPerformanceChart.setTitle("Mission Performance");

        if (renduList == null || renduList.isEmpty()) {
            missionPerformanceChart.getData().clear();
            return;
        }

        // Group by mission and calculate average score
        Map<Integer, Double> missionAverages = renduList.stream()
                .collect(Collectors.groupingBy(
                        RenduMission::getMissionId,
                        Collectors.averagingInt(RenduMission::getScore)
                ));

        // Sort by mission ID
        List<Map.Entry<Integer, Double>> sortedEntries = missionAverages.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(10) // Show top 10 missions
                .collect(Collectors.toList());

        // Create series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Avg Score");

        for (Map.Entry<Integer, Double> entry : sortedEntries) {
            series.getData().add(new XYChart.Data<>(
                    "Mission #" + entry.getKey(),
                    Math.round(entry.getValue() * 10) / 10.0
            ));
        }

        missionPerformanceChart.getData().clear();
        missionPerformanceChart.getData().add(series);
    }

    private void setupTimelineChart() {
        timelineChart.setTitle("Submission Timeline");

        if (renduList == null || renduList.isEmpty()) {
            timelineChart.getData().clear();
            return;
        }

        // Group by date (simplified - using submission count per day)
        Map<String, Long> dailyCounts = new TreeMap<>();

        // Initialize last 7 days
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dailyCounts.put(date.format(formatter), 0L);
        }

        // Count submissions (simulated for now)
        Random random = new Random();
        for (String date : dailyCounts.keySet()) {
            dailyCounts.put(date, (long) (random.nextInt(20) + 5));
        }

        // Create series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Daily Submissions");

        for (Map.Entry<String, Long> entry : dailyCounts.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        timelineChart.getData().clear();
        timelineChart.getData().add(series);
    }

    private boolean isAccepted(String result) {
        if (result == null) return false;
        String lower = result.toLowerCase();
        return lower.contains("accepted") ||
                lower.contains("success") ||
                lower.contains("passed") ||
                lower.contains("réussi");
    }

    private void setDefaultValues() {
        lblTotalSubmissions.setText("0");
        lblSuccessRate.setText("0%");
        lblAvgScore.setText("0%");
        lblTopPerformer.setText("N/A");
        lblTopScore.setText("0%");
        lblTotalTrend.setText("↗ +0%");
        lblSuccessTrend.setText("↗ +0%");
        lblAvgScoreTrend.setText("↗ +0%");

        lblOverallScore.setText("0%");
        lblMinScore.setText("0%");
        lblMaxScore.setText("0%");
        lblSubmissionCount.setText("0");
        lblAcceptedCount.setText("0");
        lblRejectedCount.setText("0");
        lblAvgEvalTime.setText("0s");
        lblFastestEval.setText("0s");
        lblSlowestEval.setText("0s");
    }

    private void updateLastUpdated() {
        lblLastUpdated.setText(dateFormat.format(new Date()));
    }

    @FXML
    private void goBackToList() {
        MissionShellController.getInstance().showRenduList();
    }




    @FXML
    private void exportAsPNG() {
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Feature Not Available");
            alert.setHeaderText("PNG Export Currently Unavailable");
            alert.setContentText("The PNG export feature requires additional dependencies.\n\n" +
                    "Please use the CSV or HTML export options instead.");
            alert.showAndWait();

            // Alternative: Offer to export as CSV instead
            Alert choiceAlert = new Alert(Alert.AlertType.CONFIRMATION);
            choiceAlert.setTitle("Export as CSV Instead?");
            choiceAlert.setHeaderText("PNG export not available");
            choiceAlert.setContentText("Would you like to export the data as CSV instead?");

            if (choiceAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                exportAsCSV(); // Call CSV export instead
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Export feature unavailable: " + e.getMessage());
        }
    }

    @FXML
    private void exportAsCSV() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Statistics Data as CSV");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("CSV File", "*.csv")
            );
            fileChooser.setInitialFileName("rendu-stats-data-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".csv");

            File file = fileChooser.showSaveDialog(lblTotalSubmissions.getScene().getWindow());
            if (file != null) {
                try (FileWriter writer = new FileWriter(file)) {
                    // Write header
                    writer.write("Metric,Value\n");

                    // Write summary stats
                    writer.write("Total Submissions," + lblTotalSubmissions.getText() + "\n");
                    writer.write("Success Rate," + lblSuccessRate.getText() + "\n");
                    writer.write("Average Score," + lblAvgScore.getText() + "\n");
                    writer.write("Top Performer," + lblTopPerformer.getText() + "\n");
                    writer.write("Top Score," + lblTopScore.getText() + "\n");

                    // Write detailed metrics
                    writer.write("Overall Average Score," + lblOverallScore.getText() + "\n");
                    writer.write("Minimum Score," + lblMinScore.getText() + "\n");
                    writer.write("Maximum Score," + lblMaxScore.getText() + "\n");
                    writer.write("Total Submissions Count," + lblSubmissionCount.getText() + "\n");
                    writer.write("Accepted Count," + lblAcceptedCount.getText() + "\n");
                    writer.write("Rejected Count," + lblRejectedCount.getText() + "\n");

                    // Write chart data
                    writer.write("\nScore Distribution,\n");
                    for (XYChart.Data<String, Number> data : scoreDistributionChart.getData().get(0).getData()) {
                        writer.write(data.getXValue() + "," + data.getYValue() + "\n");
                    }

                    writer.write("\nStatus Distribution,\n");
                    for (PieChart.Data data : statusPieChart.getData()) {
                        writer.write(data.getName() + "," + data.getPieValue() + "\n");
                    }

                    showAlert("Success", "Statistics data exported successfully to:\n" + file.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to export CSV: " + e.getMessage());
        }
    }

    @FXML
    private void exportReport() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Full Statistics Report");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("HTML Report", "*.html")
            );
            fileChooser.setInitialFileName("rendu-stats-report-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".html");

            File file = fileChooser.showSaveDialog(lblTotalSubmissions.getScene().getWindow());
            if (file != null) {
                generateHTMLReport(file);
                showAlert("Success", "Full report exported successfully to:\n" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to export report: " + e.getMessage());
        }
    }

    private void generateHTMLReport(File file) throws Exception {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("<!DOCTYPE html>\n");
            writer.write("<html>\n");
            writer.write("<head>\n");
            writer.write("    <title>Rendu Submission Statistics Report</title>\n");
            writer.write("    <style>\n");
            writer.write("        body { font-family: 'Segoe UI', Arial, sans-serif; margin: 40px; color: #231942; }\n");
            writer.write("        .header { text-align: center; margin-bottom: 40px; }\n");
            writer.write("        .summary-cards { display: flex; gap: 20px; margin-bottom: 40px; flex-wrap: wrap; }\n");
            writer.write("        .card { background: #FFFFFF; border: 1px solid rgba(35,25,66,0.12); border-radius: 16px; padding: 25px; flex: 1; min-width: 200px; box-shadow: 0 8px 25px rgba(35,25,66,0.1); }\n");
            writer.write("        .card-label { color: rgba(35,25,66,0.65); font-size: 14px; font-weight: 700; }\n");
            writer.write("        .card-value { color: #231942; font-size: 32px; font-weight: 900; margin: 8px 0; }\n");
            writer.write("        .section { margin: 40px 0; }\n");
            writer.write("        .section-title { color: #231942; font-size: 24px; font-weight: 900; margin-bottom: 20px; }\n");
            writer.write("        .metrics-table { width: 100%; border-collapse: collapse; }\n");
            writer.write("        .metrics-table th, .metrics-table td { padding: 12px; text-align: left; border-bottom: 1px solid rgba(35,25,66,0.1); }\n");
            writer.write("        .metrics-table th { color: rgba(35,25,66,0.8); font-weight: 700; }\n");
            writer.write("        .footer { margin-top: 40px; text-align: center; color: rgba(35,25,66,0.6); font-size: 14px; }\n");
            writer.write("        .accepted { color: #059669; }\n");
            writer.write("        .rejected { color: #DC2626; }\n");
            writer.write("        .warning { color: #D97706; }\n");
            writer.write("    </style>\n");
            writer.write("</head>\n");
            writer.write("<body>\n");

            // Header
            writer.write("    <div class=\"header\">\n");
            writer.write("        <h1>📊 Rendu Submission Statistics Report</h1>\n");
            writer.write("        <p>Generated on " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "</p>\n");
            writer.write("    </div>\n");

            // Summary Cards
            writer.write("    <div class=\"summary-cards\">\n");
            writer.write("        <div class=\"card\">\n");
            writer.write("            <div class=\"card-label\">Total Submissions</div>\n");
            writer.write("            <div class=\"card-value\">" + lblTotalSubmissions.getText() + "</div>\n");
            writer.write("        </div>\n");
            writer.write("        <div class=\"card\">\n");
            writer.write("            <div class=\"card-label\">Success Rate</div>\n");
            writer.write("            <div class=\"card-value accepted\">" + lblSuccessRate.getText() + "</div>\n");
            writer.write("        </div>\n");
            writer.write("        <div class=\"card\">\n");
            writer.write("            <div class=\"card-label\">Average Score</div>\n");
            writer.write("            <div class=\"card-value warning\">" + lblAvgScore.getText() + "</div>\n");
            writer.write("        </div>\n");
            writer.write("        <div class=\"card\">\n");
            writer.write("            <div class=\"card-label\">Top Performer</div>\n");
            writer.write("            <div class=\"card-value\">" + lblTopPerformer.getText() + "</div>\n");
            writer.write("            <div>" + lblTopScore.getText() + "</div>\n");
            writer.write("        </div>\n");
            writer.write("    </div>\n");

            // Detailed Metrics
            writer.write("    <div class=\"section\">\n");
            writer.write("        <h2 class=\"section-title\">📋 Detailed Performance Metrics</h2>\n");
            writer.write("        <table class=\"metrics-table\">\n");
            writer.write("            <tr><th>Metric</th><th>Value</th><th>Min</th><th>Max</th></tr>\n");
            writer.write("            <tr><td>Overall Score</td><td>" + lblOverallScore.getText() + "</td><td>" + lblMinScore.getText() + "</td><td>" + lblMaxScore.getText() + "</td></tr>\n");
            writer.write("            <tr><td>Submissions Count</td><td>" + lblSubmissionCount.getText() + "</td><td class=\"accepted\">" + lblAcceptedCount.getText() + " Accepted</td><td class=\"rejected\">" + lblRejectedCount.getText() + " Rejected</td></tr>\n");
            writer.write("            <tr><td>Avg Evaluation Time</td><td>" + lblAvgEvalTime.getText() + "</td><td>" + lblFastestEval.getText() + "</td><td>" + lblSlowestEval.getText() + "</td></tr>\n");
            writer.write("        </table>\n");
            writer.write("    </div>\n");

            // Chart Data
            writer.write("    <div class=\"section\">\n");
            writer.write("        <h2 class=\"section-title\">📈 Chart Data</h2>\n");

            // Score Distribution
            writer.write("        <h3>Score Distribution</h3>\n");
            writer.write("        <table class=\"metrics-table\">\n");
            writer.write("            <tr><th>Score Range</th><th>Count</th></tr>\n");
            for (XYChart.Data<String, Number> data : scoreDistributionChart.getData().get(0).getData()) {
                writer.write("            <tr><td>" + data.getXValue() + "</td><td>" + data.getYValue() + "</td></tr>\n");
            }
            writer.write("        </table>\n");

            // Status Distribution
            writer.write("        <h3>Status Distribution</h3>\n");
            writer.write("        <table class=\"metrics-table\">\n");
            writer.write("            <tr><th>Status</th><th>Count</th><th>Percentage</th></tr>\n");
            int total = Integer.parseInt(lblTotalSubmissions.getText().replace("%", ""));
            for (PieChart.Data data : statusPieChart.getData()) {
                double percentage = (data.getPieValue() / total) * 100;
                writer.write("            <tr><td>" + data.getName() + "</td><td>" + data.getPieValue() + "</td><td>" + String.format("%.1f", percentage) + "%</td></tr>\n");
            }
            writer.write("        </table>\n");

            writer.write("    </div>\n");

            // Footer
            writer.write("    <div class=\"footer\">\n");
            writer.write("        <p>Report generated by AI Code Submission System</p>\n");
            writer.write("        <p>Last updated: " + lblLastUpdated.getText() + "</p>\n");
            writer.write("    </div>\n");

            writer.write("</body>\n");
            writer.write("</html>\n");
        }
    }

    @FXML
    private void refreshData() {
        loadData();
        updateLastUpdated();
        showAlert("Refreshed", "Statistics data has been refreshed.");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}