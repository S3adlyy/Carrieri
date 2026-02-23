package com.exemple.grecrutement;

import entities.Entretien;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import services.EntretienService;
import utils.AlertUtils;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class CalendarViewController implements Initializable {

    @FXML private Label totalInterviewsLabel;
    @FXML private Label todayInterviewsLabel;
    @FXML private Label upcomingInterviewsLabel;
    @FXML private Label monthYearLabel;
    @FXML private Label upcomingCount;
    @FXML private GridPane calendarGrid;
    @FXML private ListView<HBox> upcomingListView;
    @FXML private ComboBox<String> viewFilterCombo;
    @FXML private Button prevMonthButton;
    @FXML private Button nextMonthButton;
    @FXML private Button todayButton;

    private YearMonth currentYearMonth;
    private final DateTimeFormatter monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter displayDateFormatter = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH);

    private EntretienService entretienService;
    private List<Entretien> allInterviews;
    private ObservableList<HBox> upcomingItems;

    // Color constants
    private static final String COLOR_PRIMARY = "#231942";
    private static final String COLOR_SECONDARY = "#5E548E";
    private static final String COLOR_ACCENT = "#9F86C0";
    private static final String COLOR_SUCCESS = "#10B981";
    private static final String COLOR_ERROR = "#EF4444";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        entretienService = new EntretienService();
        allInterviews = new ArrayList<>();
        upcomingItems = FXCollections.observableArrayList();

        // Initialize current month
        currentYearMonth = YearMonth.now();

        // Setup filter combo
        setupFilterCombo();

        // Setup upcoming list view
        upcomingListView.setItems(upcomingItems);
        upcomingListView.setCellFactory(param -> new ListCell<HBox>() {
            @Override
            protected void updateItem(HBox item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(item);
                }
            }
        });

        // Load interviews
        loadInterviews();

        // Add listeners
        viewFilterCombo.setOnAction(e -> filterInterviews());

        // Initial render
        renderCalendar();
        updateStats();
        updateUpcomingList();
    }

    private void setupFilterCombo() {
        viewFilterCombo.setItems(FXCollections.observableArrayList(
                "All Interviews",
                "Scheduled",
                "Completed",
                "Cancelled"
        ));
        viewFilterCombo.getSelectionModel().select(0);
    }

    private void loadInterviews() {
        try {
            allInterviews = entretienService.getAllEntretiens();
            System.out.println("✅ Loaded " + allInterviews.size() + " interviews");
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtils.showError("Erreur", "Impossible de charger les entretiens: " + e.getMessage());
        }
    }

    private void renderCalendar() {
        calendarGrid.getChildren().clear();
        monthYearLabel.setText(currentYearMonth.format(monthYearFormatter).toUpperCase());

        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int firstDayOfWeek = firstOfMonth.getDayOfWeek().getValue() - 1; // 0 = Monday

        LocalDate startDate = firstOfMonth.minusDays(firstDayOfWeek);

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                LocalDate date = startDate.plusDays(row * 7 + col);
                VBox dayCell = createDayCell(date);
                calendarGrid.add(dayCell, col, row);
            }
        }
    }

    private VBox createDayCell(LocalDate date) {
        VBox cell = new VBox(5);
        cell.getStyleClass().add("day-cell");
        cell.setPadding(new Insets(10));

        // Check if this is current month
        if (date.getMonth() == currentYearMonth.getMonth()) {
            cell.getStyleClass().add("day-cell-current-month");
        } else {
            cell.getStyleClass().add("day-cell-other-month");
        }

        // Check if today
        if (date.equals(LocalDate.now())) {
            cell.getStyleClass().add("day-cell-today");
        }

        // Day number
        Label dayNumber = new Label(String.valueOf(date.getDayOfMonth()));
        dayNumber.getStyleClass().add("day-number");
        if (date.getMonth() != currentYearMonth.getMonth()) {
            dayNumber.getStyleClass().add("day-number-other-month");
        }
        cell.getChildren().add(dayNumber);

        // Add interviews for this date
        List<Entretien> dayInterviews = getInterviewsForDate(date);
        for (Entretien interview : dayInterviews) {
            Node indicator = createInterviewIndicator(interview);
            cell.getChildren().add(indicator);
        }

        // Add click handler
        cell.setOnMouseClicked(e -> showDayDetails(date, dayInterviews));

        return cell;
    }

    private Node createInterviewIndicator(Entretien interview) {
        HBox indicator = new HBox(5);
        indicator.setAlignment(Pos.CENTER_LEFT);
        indicator.setPadding(new Insets(4, 8, 4, 8));
        indicator.getStyleClass().add("interview-indicator");

        // Set style based on status
        String status = interview.getStatus() != null ? interview.getStatus().toLowerCase() : "scheduled";
        switch (status) {
            case "completed":
                indicator.getStyleClass().add("indicator-completed");
                break;
            case "cancelled":
                indicator.getStyleClass().add("indicator-cancelled");
                break;
            default:
                indicator.getStyleClass().add("indicator-scheduled");
        }

        // Time
        Label timeLabel = new Label(interview.getDateEntretien().format(timeFormatter));
        timeLabel.getStyleClass().add("indicator-time");

        // Type (abbreviated)
        String type = interview.getType() != null ? interview.getType() : "Interview";
        String shortType = type.length() > 8 ? type.substring(0, 6) + "..." : type;
        Label typeLabel = new Label(shortType);
        typeLabel.getStyleClass().add("indicator-title");

        indicator.getChildren().addAll(timeLabel, typeLabel);

        // Tooltip with full details
        Tooltip tooltip = new Tooltip(createInterviewTooltipText(interview));
        tooltip.getStyleClass().add("interview-tooltip");
        Tooltip.install(indicator, tooltip);

        // Click handler
        indicator.setOnMouseClicked(e -> {
            e.consume();
            showInterviewDetails(interview);
        });

        return indicator;
    }

    private String createInterviewTooltipText(Entretien interview) {
        return String.format("Type: %s\nDate: %s\nTime: %s\nStatus: %s",
                interview.getType() != null ? interview.getType() : "N/A",
                interview.getDateEntretien().format(dateFormatter),
                interview.getDateEntretien().format(timeFormatter),
                interview.getStatus() != null ? interview.getStatus() : "Scheduled"
        );
    }

    private List<Entretien> getInterviewsForDate(LocalDate date) {
        return allInterviews.stream()
                .filter(i -> i.getDateEntretien().toLocalDate().equals(date))
                .collect(Collectors.toList());
    }

    private void filterInterviews() {
        String filter = viewFilterCombo.getValue();
        if (filter == null || filter.equals("All Interviews")) {
            renderCalendar();
            updateUpcomingList();
        } else {
            // Apply filter to calendar
            calendarGrid.getChildren().clear();
            monthYearLabel.setText(currentYearMonth.format(monthYearFormatter).toUpperCase());

            LocalDate firstOfMonth = currentYearMonth.atDay(1);
            int firstDayOfWeek = firstOfMonth.getDayOfWeek().getValue() - 1;
            LocalDate startDate = firstOfMonth.minusDays(firstDayOfWeek);

            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 7; col++) {
                    LocalDate date = startDate.plusDays(row * 7 + col);
                    VBox dayCell = createFilteredDayCell(date, filter);
                    calendarGrid.add(dayCell, col, row);
                }
            }

            // Update upcoming list with filter
            updateUpcomingList(filter);
        }
    }

    private VBox createFilteredDayCell(LocalDate date, String filter) {
        VBox cell = createDayCell(date);

        // Remove existing indicators and add filtered ones
        cell.getChildren().removeIf(node -> node instanceof HBox);

        List<Entretien> dayInterviews = getInterviewsForDate(date).stream()
                .filter(i -> matchesFilter(i, filter))
                .collect(Collectors.toList());

        for (Entretien interview : dayInterviews) {
            Node indicator = createInterviewIndicator(interview);
            cell.getChildren().add(indicator);
        }

        return cell;
    }

    private boolean matchesFilter(Entretien interview, String filter) {
        String status = interview.getStatus() != null ? interview.getStatus().toLowerCase() : "scheduled";
        switch (filter) {
            case "Scheduled":
                return status.equals("scheduled") || status.equals("planifié");
            case "Completed":
                return status.equals("completed") || status.equals("terminé");
            case "Cancelled":
                return status.equals("cancelled") || status.equals("annulé");
            default:
                return true;
        }
    }

    private void updateStats() {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);

        long total = allInterviews.size();
        long todayCount = allInterviews.stream()
                .filter(i -> i.getDateEntretien().toLocalDate().equals(today))
                .count();
        long upcoming = allInterviews.stream()
                .filter(i -> {
                    LocalDate date = i.getDateEntretien().toLocalDate();
                    return date.isAfter(today) && date.isBefore(nextWeek);
                })
                .count();

        totalInterviewsLabel.setText(String.valueOf(total));
        todayInterviewsLabel.setText(String.valueOf(todayCount));
        upcomingInterviewsLabel.setText(String.valueOf(upcoming));
    }

    private void updateUpcomingList() {
        updateUpcomingList("All Interviews");
    }

    private void updateUpcomingList(String filter) {
        upcomingItems.clear();

        LocalDate now = LocalDate.now();
        List<Entretien> upcoming = allInterviews.stream()
                .filter(i -> i.getDateEntretien().toLocalDate().isAfter(now.minusDays(1)))
                .filter(i -> matchesFilter(i, filter))
                .sorted(Comparator.comparing(Entretien::getDateEntretien))
                .limit(10)
                .collect(Collectors.toList());

        upcomingCount.setText("(" + upcoming.size() + ")");

        for (Entretien interview : upcoming) {
            upcomingItems.add(createUpcomingItem(interview));
        }
    }

    private HBox createUpcomingItem(Entretien interview) {
        HBox item = new HBox(20);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12, 15, 12, 15));
        item.getStyleClass().add("upcoming-item");

        // Time
        Label timeLabel = new Label(interview.getDateEntretien().format(timeFormatter));
        timeLabel.getStyleClass().add("upcoming-time");

        // Date
        Label dateLabel = new Label(interview.getDateEntretien().format(dateFormatter));
        dateLabel.getStyleClass().add("upcoming-time");
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5E548E;");

        // Type
        Label typeLabel = new Label(interview.getType() != null ? interview.getType() : "Interview");
        typeLabel.getStyleClass().add("upcoming-type");

        // Candidate name (placeholder - you can enhance this with real data)
        Label nameLabel = new Label("Candidat #" + interview.getPostulationId());
        nameLabel.getStyleClass().add("upcoming-name");

        // Status badge
        Label statusLabel = new Label(interview.getStatus() != null ? interview.getStatus() : "Planifié");
        statusLabel.getStyleClass().add("upcoming-status-badge");

        String status = interview.getStatus() != null ? interview.getStatus().toLowerCase() : "scheduled";
        switch (status) {
            case "completed":
                statusLabel.getStyleClass().add("status-completed");
                break;
            case "cancelled":
                statusLabel.getStyleClass().add("status-cancelled");
                break;
            default:
                statusLabel.getStyleClass().add("status-scheduled");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        item.getChildren().addAll(timeLabel, dateLabel, typeLabel, nameLabel, spacer, statusLabel);

        // Click handler
        item.setOnMouseClicked(e -> showInterviewDetails(interview));

        // Hover effect
        item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: #F4F1FB;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-background-color: white;"));

        return item;
    }

    private void showDayDetails(LocalDate date, List<Entretien> interviews) {
        if (interviews.isEmpty()) {
            AlertUtils.showInfo("Aucun entretien",
                    "Aucun entretien programmé pour le " + date.format(displayDateFormatter));
            return;
        }

        // Build a detailed message for the day
        StringBuilder message = new StringBuilder();
        message.append("📅 ").append(date.format(displayDateFormatter)).append("\n\n");
        message.append("Total: ").append(interviews.size()).append(" entretien(s)\n\n");

        for (int i = 0; i < interviews.size(); i++) {
            Entretien interview = interviews.get(i);
            String status = interview.getStatus() != null ? interview.getStatus() : "Planifié";
            String statusEmoji = getStatusEmoji(status);

            message.append(i + 1).append(". ")
                    .append(statusEmoji).append(" ")
                    .append(interview.getDateEntretien().format(timeFormatter)).append(" - ")
                    .append(interview.getType() != null ? interview.getType() : "Entretien").append("\n")
                    .append("   Statut: ").append(status).append("\n")
                    .append("   Candidat #").append(interview.getPostulationId()).append("\n\n");
        }

        AlertUtils.showInfo("Entretiens du " + date.format(dateFormatter), message.toString());
    }

    private String getStatusEmoji(String status) {
        if (status == null) return "🟣";
        switch (status.toLowerCase()) {
            case "completed": return "✅";
            case "cancelled": return "❌";
            case "scheduled": return "🟣";
            default: return "🟣";
        }
    }

    private void showInterviewDetails(Entretien interview) {
        String formattedDate = interview.getDateEntretien().format(displayDateFormatter);
        String formattedTime = interview.getDateEntretien().format(timeFormatter);
        String status = interview.getStatus() != null ? interview.getStatus() : "Planifié";
        String type = interview.getType() != null ? interview.getType() : "Non spécifié";

        String message = String.format(
                "📅 Date: %s\n" +
                        "⏰ Heure: %s\n" +
                        "🎯 Type: %s\n" +
                        "📊 Statut: %s %s\n" +
                        "🆔 Postulation: %d\n" +
                        "🆔 Entretien: %d",
                formattedDate,
                formattedTime,
                type,
                getStatusEmoji(status),
                status,
                interview.getPostulationId(),
                interview.getId()
        );

        AlertUtils.showInfo("Détails de l'entretien", message);
    }

    @FXML
    private void previousMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1);
        renderCalendar();
    }

    @FXML
    private void nextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1);
        renderCalendar();
    }

    @FXML
    private void goToToday() {
        currentYearMonth = YearMonth.now();
        renderCalendar();
    }

    public void refreshCalendar() {
        loadInterviews();
        renderCalendar();
        updateStats();
        updateUpcomingList();
        AlertUtils.showSuccess("Actualisation", "Le calendrier a été mis à jour avec succès");
    }

    // Optional: Add a method to get candidate names from the database
    private String getCandidateName(int postulationId) {
        try {
            // You can implement this method to fetch actual candidate names
            // For now, return a placeholder
            return "Candidat #" + postulationId;
        } catch (Exception e) {
            return "Candidat #" + postulationId;
        }
    }
}