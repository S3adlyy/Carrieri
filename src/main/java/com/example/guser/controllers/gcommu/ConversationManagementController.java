package com.example.guser.controllers.gcommu;

import entities.gcommu.Conversation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import services.gcommu.ConversationService;
import services.gcommu.UserService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ConversationManagementController {

    @FXML private Label totalConversationsLabel;
    @FXML private Label activeConversationsLabel;
    @FXML private Label archivedConversationsLabel;
    @FXML private Label totalUsersLabel;
    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> statusFilter;
    @FXML private ChoiceBox<String> userFilter;
    @FXML private DatePicker dateFilter;
    @FXML private TableView<Conversation> conversationsTable;
    @FXML private TableColumn<Conversation, Integer> idColumn;
    @FXML private TableColumn<Conversation, LocalDateTime> dateColumn;
    @FXML private TableColumn<Conversation, Integer> user1Column;
    @FXML private TableColumn<Conversation, Integer> user2Column;
    @FXML private TableColumn<Conversation, String> lastMessageColumn;
    @FXML private TableColumn<Conversation, String> statusColumn;
    @FXML private Label tableInfo;
    @FXML private ChoiceBox<String> pageSizeChoice;
    @FXML private Label pageInfo;
    @FXML private Label lastUpdateLabel;

    private ConversationService conversationService;
    private UserService userService;
    private ObservableList<Conversation> conversationList;
    private FilteredList<Conversation> filteredData;
    private SortedList<Conversation> sortedData;

    private int currentPage = 1;
    private int pageSize = 10;
    private int totalPages = 1;

    @FXML
    public void initialize() {
        try {
            conversationService = new ConversationService();
            userService = new UserService();

            setupTable();
            loadConversations();
            updateStats();
            setupFilters();
            setupUserFilter();

            // Mise à jour de l'horodatage
            updateTimestamp();

        } catch (SQLException e) {
            showError("Erreur de chargement", "Impossible de charger les conversations: " + e.getMessage());
        }
    }

    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
        user1Column.setCellValueFactory(new PropertyValueFactory<>("user1Id"));
        user2Column.setCellValueFactory(new PropertyValueFactory<>("user2Id"));
        lastMessageColumn.setCellValueFactory(new PropertyValueFactory<>("dernierMessage"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Formatage de la date
        dateColumn.setCellFactory(column -> new TableCell<Conversation, LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(formatter));
                }
            }
        });

        // Style du statut
        statusColumn.setCellFactory(column -> new TableCell<Conversation, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(status.toUpperCase());
                    badge.getStyleClass().add("status-badge");

                    switch (status.toLowerCase()) {
                        case "active":
                            badge.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #10B981; -fx-padding: 4 12; -fx-background-radius: 20;");
                            break;
                        case "archived":
                            badge.setStyle("-fx-background-color: #F3F4F6; -fx-text-fill: #6B7280; -fx-padding: 4 12; -fx-background-radius: 20;");
                            break;
                        case "blocked":
                            badge.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #EF4444; -fx-padding: 4 12; -fx-background-radius: 20;");
                            break;
                        default:
                            badge.setStyle("-fx-padding: 4 12; -fx-background-radius: 20;");
                    }

                    setGraphic(badge);
                }
            }
        });

        // Action sur double-clic
        conversationsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Conversation selected = conversationsTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showConversationDetails(selected);
                }
            }
        });
    }

    private void loadConversations() throws SQLException {
        List<Conversation> conversations = conversationService.read();
        conversationList = FXCollections.observableArrayList(conversations);

        filteredData = new FilteredList<>(conversationList, p -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(conversationsTable.comparatorProperty());

        conversationsTable.setItems(sortedData);

        updateTableInfo();
        updatePagination();
    }

    private void setupFilters() {
        // Filtre de recherche
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });

        // Filtre de statut
        statusFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            applyFilters();
        });

        // Filtre par date
        dateFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            applyFilters();
        });

        // Pagination
        pageSizeChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                pageSize = Integer.parseInt(newVal);
                currentPage = 1;
                updatePagination();
            }
        });

        // Initialiser les options de statut
        statusFilter.getItems().clear();
        statusFilter.getItems().addAll("Tous les statuts", "active", "archived", "blocked");
        statusFilter.setValue("Tous les statuts");
    }

    private void setupUserFilter() {
        userFilter.getItems().clear();
        userFilter.getItems().add("Tous les utilisateurs");

        try {
            userService.getAllUsers().forEach(user ->
                userFilter.getItems().add("Utilisateur " + user.getId())
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }

        userFilter.setValue("Tous les utilisateurs");

        // Filtre utilisateur
        userFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            applyFilters();
        });
    }

    private void applyFilters() {
        filteredData.setPredicate(conversation -> {
            // Filtre de recherche
            String searchText = searchField.getText();
            if (searchText != null && !searchText.isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase();
                boolean matchesSearch = String.valueOf(conversation.getId()).contains(lowerCaseFilter)
                    || (conversation.getDernierMessage() != null
                    && conversation.getDernierMessage().toLowerCase().contains(lowerCaseFilter))
                    || String.valueOf(conversation.getUser1Id()).contains(lowerCaseFilter)
                    || String.valueOf(conversation.getUser2Id()).contains(lowerCaseFilter);

                if (!matchesSearch) return false;
            }

            // Filtre de statut
            String statusValue = statusFilter.getValue();
            if (statusValue != null && !statusValue.equals("Tous les statuts")) {
                String conversationStatus = conversation.getStatut();
                if (conversationStatus == null || !conversationStatus.equals(statusValue)) {
                    return false;
                }
            }

            // Filtre utilisateur
            String userValue = userFilter.getValue();
            if (userValue != null && !userValue.equals("Tous les utilisateurs")) {
                String[] parts = userValue.split(" ");
                if (parts.length > 1) {
                    try {
                        int userId = Integer.parseInt(parts[1]);
                        if (conversation.getUser1Id() != userId && conversation.getUser2Id() != userId) {
                            return false;
                        }
                    } catch (NumberFormatException e) {
                        // Ignorer si le format n'est pas valide
                    }
                }
            }

            // Filtre de date
            if (dateFilter.getValue() != null && conversation.getDateCreation() != null) {
                if (!conversation.getDateCreation().toLocalDate().equals(dateFilter.getValue())) {
                    return false;
                }
            }

            return true;
        });

        updateTableInfo();
        updatePagination();
    }

    private void updateStats() {
        try {
            List<Conversation> all = conversationService.read();
            long active = all.stream().filter(c -> "active".equals(c.getStatut())).count();
            long archived = all.stream().filter(c -> "archived".equals(c.getStatut())).count();
            int users = userService.getAllUsers().size();

            totalConversationsLabel.setText(String.valueOf(all.size()));
            activeConversationsLabel.setText(String.valueOf(active));
            archivedConversationsLabel.setText(String.valueOf(archived));
            totalUsersLabel.setText(String.valueOf(users));

        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les statistiques");
        }
    }

    private void updateTableInfo() {
        tableInfo.setText(filteredData.size() + " conversations");
    }

    private void updatePagination() {
        int totalItems = filteredData.size();
        totalPages = (int) Math.ceil((double) totalItems / pageSize);
        pageInfo.setText("Page " + currentPage + " sur " + totalPages);
    }

    private void updateTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        lastUpdateLabel.setText(formatter.format(LocalDateTime.now()));
    }

    @FXML
    private void refreshData() {
        try {
            loadConversations();
            updateStats();
            updateTimestamp();
            showSuccess("Rafraîchi", "Les données ont été mises à jour");
        } catch (SQLException e) {
            showError("Erreur", "Impossible de rafraîchir les données");
        }
    }

    @FXML
    private void resetFilters() {
        searchField.clear();
        statusFilter.setValue("Tous les statuts");
        userFilter.setValue("Tous les utilisateurs");
        dateFilter.setValue(null);
        applyFilters();
    }

    @FXML
    private void showNewConversationForm() {
        showInfo("Nouvelle conversation", "Formulaire de création à implémenter");
    }

    @FXML
    private void editConversation() {
        Conversation selected = conversationsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showInfo("Édition", "Édition de la conversation #" + selected.getId());
        } else {
            showWarning("Aucune sélection", "Veuillez sélectionner une conversation à modifier");
        }
    }

    @FXML
    private void viewDetails() {
        Conversation selected = conversationsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showConversationDetails(selected);
        } else {
            showWarning("Aucune sélection", "Veuillez sélectionner une conversation à visualiser");
        }
    }

    @FXML
    private void archiveConversation() {
        Conversation selected = conversationsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                selected.setStatut("archived");
                conversationService.update(selected);
                refreshData();
                showSuccess("Succès", "Conversation archivée");
            } catch (SQLException e) {
                showError("Erreur", "Impossible d'archiver la conversation");
            }
        } else {
            showWarning("Aucune sélection", "Veuillez sélectionner une conversation à archiver");
        }
    }

    @FXML
    private void deleteConversation() {
        Conversation selected = conversationsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showDeleteConfirmation(selected);
        } else {
            showWarning("Aucune sélection", "Veuillez sélectionner une conversation à supprimer");
        }
    }

    private void showDeleteConfirmation(Conversation conversation) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);

        VBox content = new VBox(10);
        content.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label mainMessage = new Label("Supprimer la conversation");
        mainMessage.getStyleClass().add("main-message");

        Label itemName = new Label("Conversation #" + conversation.getId());
        itemName.getStyleClass().add("item-name");

        Label warningMessage = new Label("Êtes-vous sûr de vouloir supprimer cette conversation ?");
        warningMessage.getStyleClass().add("warning-message");

        content.getChildren().addAll(mainMessage, itemName, warningMessage);
        confirm.getDialogPane().setContent(content);

        DialogPane dialogPane = confirm.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.getStyleClass().add("confirmation");
        dialogPane.getStylesheets().add(getClass().getResource("/Alert.css").toExternalForm());

        ButtonType okButtonType = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(okButtonType, cancelButtonType);

        Button okButton = (Button) dialogPane.lookupButton(okButtonType);
        Button cancelButton = (Button) dialogPane.lookupButton(cancelButtonType);

        okButton.getStyleClass().add("btn-danger");
        cancelButton.getStyleClass().add("btn-secondary");

        confirm.showAndWait().ifPresent(response -> {
            if (response == okButtonType) {
                try {
                    conversationService.supprimer(conversation.getId());
                    refreshData();
                    showSuccess("Succès", "Conversation supprimée");
                } catch (SQLException e) {
                    showError("Erreur", "Impossible de supprimer la conversation");
                }
            }
        });
    }

    @FXML
    private void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            updatePagination();
            // TODO: Implémenter le changement de page dans la vue
        }
    }

    @FXML
    private void nextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            updatePagination();
            // TODO: Implémenter le changement de page dans la vue
        }
    }

    private void showConversationDetails(Conversation conversation) {
        Alert details = new Alert(Alert.AlertType.INFORMATION);
        details.setTitle("Détails de la conversation");
        details.setHeaderText(null);

        VBox content = new VBox(10);
        content.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label titleLabel = new Label("Conversation #" + conversation.getId());
        titleLabel.getStyleClass().add("main-message");

        Label dateLabel = new Label("📅 Date: " + (conversation.getDateCreation() != null ?
            conversation.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A"));
        dateLabel.setStyle("-fx-font-size: 14px;");

        Label user1Label = new Label("👤 Utilisateur 1: " + conversation.getUser1Id());
        user1Label.setStyle("-fx-font-size: 14px;");

        Label user2Label = new Label("👤 Utilisateur 2: " + conversation.getUser2Id());
        user2Label.setStyle("-fx-font-size: 14px;");

        Label messageLabel = new Label("💬 Dernier message: " +
            (conversation.getDernierMessage() != null ? conversation.getDernierMessage() : "Aucun message"));
        messageLabel.setStyle("-fx-font-size: 14px;");
        messageLabel.setWrapText(true);

        Label statusLabel = new Label("📊 Statut: " + conversation.getStatut());
        statusLabel.setStyle("-fx-font-size: 14px;");

        content.getChildren().addAll(titleLabel, dateLabel, user1Label, user2Label, messageLabel, statusLabel);

        details.getDialogPane().setContent(content);

        DialogPane dialogPane = details.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.getStyleClass().add("info");
        dialogPane.getStylesheets().add(getClass().getResource("/Alert.css").toExternalForm());

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("btn-primary");
        }

        details.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.getStyleClass().add("info");
        dialogPane.getStylesheets().add(getClass().getResource("/Alert.css").toExternalForm());

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("btn-primary");
        }

        alert.showAndWait();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.getStyleClass().add("info");
        dialogPane.getStyleClass().add("success-alert");
        dialogPane.getStylesheets().add(getClass().getResource("/Alert.css").toExternalForm());

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("btn-primary");
        }

        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.getStyleClass().add("warning");
        dialogPane.getStylesheets().add(getClass().getResource("/Alert.css").toExternalForm());

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("btn-primary");
        }

        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.getStyleClass().add("error");
        dialogPane.getStylesheets().add(getClass().getResource("/Alert.css").toExternalForm());

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("btn-danger");
        }

        alert.showAndWait();
    }
}
