package com.example.guser.controllers.gcommu;

import entities.gcommu.Conversation;
import entities.gcommu.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import services.gcommu.ConversationService;
import services.gcommu.UserService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ContactsController {

    @FXML private Label totalContactsLabel;
    @FXML private Label onlineContactsLabel;
    @FXML private Label activeConversationsLabel;
    @FXML private TextField globalSearchField;
    @FXML private TextField searchField;
    @FXML private ListView<User> contactsListView;
    @FXML private ListView<Conversation> recentConversationsList;
    @FXML private VBox selectedContactCard;
    @FXML private VBox emptyState;
    @FXML private Label selectedAvatarInitials;
    @FXML private Label selectedContactName;
    @FXML private Label selectedContactHeadline;
    @FXML private Circle selectedStatus;

    @FXML private Button allTab;
    @FXML private Button onlineTab;
    @FXML private Button favoritesTab;

    private UserService userService;
    private ConversationService conversationService;
    private ObservableList<User> allUsers;
    private FilteredList<User> filteredUsers;
    private User selectedUser;
    private int currentUserId = 1; // À remplacer par l'ID de l'utilisateur connecté

    @FXML
    public void initialize() {
        try {
            userService = new UserService();
            conversationService = new ConversationService();

            loadUsers();
            setupContactsList();
            setupSearch();
            updateStats(); // Maintenant updateStats() utilisera les mêmes données que loadUsers()
            loadRecentConversations();

            // Ajouter un listener pour surveiller les changements dans allUsers
            allUsers.addListener((javafx.collections.ListChangeListener<User>) change -> {
                updateStats();
            });

        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les contacts: " + e.getMessage());
        }
    }

    private void loadUsers() throws SQLException {
        List<User> users = userService.getAllUsers();
        users.removeIf(user -> user.getId() == currentUserId);

        // Ajouter des statuts aléatoires pour la démo
        for (User user : users) {
            user.setOnline(Math.random() > 0.5);
        }

        allUsers = FXCollections.observableArrayList(users);
        filteredUsers = new FilteredList<>(allUsers, p -> true);
        contactsListView.setItems(filteredUsers);
    }

    private void setupContactsList() {
        contactsListView.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setGraphic(createContactCell(user));
                    setOnMouseClicked(event -> {
                        if (event.getClickCount() == 1) {
                            selectContact(user);
                        }
                        if (event.getClickCount() == 2) {
                            openChat(user);
                        }
                    });
                }
            }
        });
    }

    private HBox createContactCell(User user) {
        HBox hbox = new HBox(12);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.getStyleClass().add("contact-item");
        hbox.setPrefWidth(350);

        // Avatar
        Label avatar = new Label(getInitials(user.getUsername()));
        avatar.getStyleClass().add("contact-avatar");

        // Info
        VBox infoBox = new VBox(4);
        Label nameLabel = new Label(user.getUsername());
        nameLabel.getStyleClass().add("contact-name");

        String statusText = user.isOnline() ? "● En ligne" : "● Hors ligne";
        Label statusLabel = new Label(statusText);
        statusLabel.getStyleClass().add("contact-status");
        if (user.isOnline()) {
            statusLabel.getStyleClass().add("online");
        }

        infoBox.getChildren().addAll(nameLabel, statusLabel);

        // Boutons d'action (Modifier et Supprimer)
        HBox actionsBox = new HBox(5);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        /*Button editBtn = new Button("✎");
        editBtn.getStyleClass().add("contact-action-btn");
        editBtn.setOnAction(event -> {
            event.consume();
            modifyContact(user);
        });

        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("contact-action-btn");
        deleteBtn.getStyleClass().add("delete");
        deleteBtn.setOnAction(event -> {
            event.consume();
            deleteContact(user);
        });

        actionsBox.getChildren().addAll(editBtn, deleteBtn);*/

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        hbox.getChildren().addAll(avatar, infoBox, spacer, actionsBox);

        // Clic sur la carte pour sélectionner
        hbox.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1 ) {
                selectContact(user);
            }
            if (event.getClickCount() == 2) {
                openChat(user);
            }
        });

        return hbox;
    }
    private void selectContact(User user) {
        this.selectedUser = user;

        // Afficher la carte de contact
        selectedContactCard.setVisible(true);
        selectedContactCard.setManaged(true);
        emptyState.setVisible(false);
        emptyState.setManaged(false);

        // Mettre à jour les infos
        selectedAvatarInitials.setText(getInitials(user.getUsername()));
        selectedContactName.setText(user.getUsername());

        String headline = user.getHeadline() != null ? user.getHeadline() : "Contact";
        selectedContactHeadline.setText(headline);

        // Mettre à jour le statut
        if (user.isOnline()) {
            selectedStatus.getStyleClass().setAll("status-online");
        } else {
            selectedStatus.getStyleClass().setAll("status-offline");
        }
    }

    @FXML
    private void startChat() {
        if (selectedUser != null) {
            openChat(selectedUser);
        }
    }

    private void openChat(User user) {
        try {
            // Ouvrir la fenêtre de chat
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/guser/gcommu/message.fxml"));
            Stage chatStage = new Stage();
            chatStage.setTitle("Chat avec " + user.getUsername());
            chatStage.setScene(new Scene(loader.load(), 1400, 900));

            // Passer l'utilisateur sélectionné au contrôleur de chat
            MessengerController chatController = loader.getController();
            chatController.setSelectedUser(user);
            chatController.setCurrentUserId(currentUserId);

            chatStage.show();

            // Fermer la fenêtre des contacts (optionnel)
            // ((Stage) contactsListView.getScene().getWindow()).close();

        } catch (IOException e) {
            showError("Erreur", "Impossible d'ouvrir la conversation");
            e.printStackTrace();
        }
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredUsers.setPredicate(user -> {
                if (newVal == null || newVal.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newVal.toLowerCase();
                return user.getUsername().toLowerCase().contains(lowerCaseFilter);
            });
        });


    }

    private void updateStats() {
        // Utiliser allUsers au lieu de recharger depuis la base de données
        if (allUsers != null) {
            long online = allUsers.stream().filter(User::isOnline).count();

            totalContactsLabel.setText(String.valueOf(allUsers.size()));
            onlineContactsLabel.setText(String.valueOf(online));

            // Pour les conversations, on garde l'appel à la base de données
            try {
                List<Conversation> conversations = conversationService.getConversationsByUser(currentUserId);
                activeConversationsLabel.setText(String.valueOf(conversations.size()));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadRecentConversations() throws SQLException {
        List<Conversation> recent = conversationService.getConversationsByUser(currentUserId);
        ObservableList<Conversation> recentList = FXCollections.observableArrayList(recent);

        recentConversationsList.setCellFactory(lv -> new ListCell<Conversation>() {
            @Override
            protected void updateItem(Conversation conv, boolean empty) {
                super.updateItem(conv, empty);
                if (empty || conv == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setGraphic(createRecentItem(conv));
                    setOnMouseClicked(event -> {
                        if (event.getClickCount() == 1) {
                            // Ouvrir la conversation
                            try {
                                int otherUserId = conv.getUser1Id() == currentUserId ?
                                    conv.getUser2Id() : conv.getUser1Id();
                                User otherUser = userService.getUserById(otherUserId);
                                openChat(otherUser);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });

        recentConversationsList.setItems(recentList);
    }

    private HBox createRecentItem(Conversation conv) {
        HBox hbox = new HBox(10);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.getStyleClass().add("recent-item");

        int otherUserId = conv.getUser1Id() == currentUserId ?
            conv.getUser2Id() : conv.getUser1Id();

        String otherUserName = "User " + otherUserId;
        try {
            User otherUser = userService.getUserById(otherUserId);
            if (otherUser != null) {
                otherUserName = otherUser.getUsername();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Label avatar = new Label(getInitials(otherUserName));
        avatar.getStyleClass().add("recent-avatar");

        VBox infoBox = new VBox(2);
        Label nameLabel = new Label(otherUserName);
        nameLabel.getStyleClass().add("recent-name");

        String lastMsg = conv.getDernierMessage();
        Label msgLabel = new Label(lastMsg != null ? lastMsg : "Aucun message");
        msgLabel.getStyleClass().add("recent-message");

        infoBox.getChildren().addAll(nameLabel, msgLabel);

        Label timeLabel = new Label(formatTimeAgo(conv.getDateCreation()));
        timeLabel.getStyleClass().add("recent-time");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        hbox.getChildren().addAll(avatar, infoBox, spacer, timeLabel);
        return hbox;
    }

    @FXML
    private void showAllContacts() {
        updateTabStyle(allTab);
        filteredUsers.setPredicate(user -> true);
    }

    @FXML
    private void showOnlineContacts() {
        updateTabStyle(onlineTab);
        filteredUsers.setPredicate(User::isOnline);
    }

    @FXML
    private void showFavoriteContacts() {
        updateTabStyle(favoritesTab);
        filteredUsers.setPredicate(User::isFavorite);
    }

    @FXML
    private void addContact() {
        // Créer une boîte de dialogue
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un contact");
        dialog.setHeaderText("Nouveau contact");

        // Appliquer le style CSS
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        dialog.getDialogPane().getStyleClass().add("confirmation");

        // Boutons
        ButtonType saveButtonType = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Styliser les boutons
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelButtonType);

        saveButton.getStyleClass().add("btn-primary");
        cancelButton.getStyleClass().add("btn-secondary");

        // Formulaire
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField firstNameField = new TextField();
        firstNameField.setPromptText("Prénom (ex: Jean)");

        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Nom (ex: Dupont)");

        // Labels pour les messages d'erreur
        Label firstNameError = new Label();
        firstNameError.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 11px;");

        Label lastNameError = new Label();
        lastNameError.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 11px;");

        grid.add(new Label("Prénom:"), 0, 0);
        grid.add(firstNameField, 1, 0);
        grid.add(firstNameError, 1, 1);
        grid.add(new Label("Nom:"), 0, 2);
        grid.add(lastNameField, 1, 2);
        grid.add(lastNameError, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Validation en temps réel
        firstNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !isFirstLetterUppercase(newVal)) {
                firstNameError.setText("❌ Le prénom doit commencer par une majuscule");
                firstNameField.setStyle("-fx-border-color: #EF4444; -fx-border-width: 2;");
            } else {
                firstNameError.setText("");
                firstNameField.setStyle("");
            }
        });

        lastNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !isFirstLetterUppercase(newVal)) {
                lastNameError.setText("❌ Le nom doit commencer par une majuscule");
                lastNameField.setStyle("-fx-border-color: #EF4444; -fx-border-width: 2;");
            } else {
                lastNameError.setText("");
                lastNameField.setStyle("");
            }
        });

        // Désactiver le bouton Ajouter si la validation échoue
        saveButton.setDisable(true);

        // Activer/désactiver le bouton selon la validation
        firstNameField.textProperty().addListener((obs, oldVal, newVal) -> validateForm(firstNameField, lastNameField, saveButton, firstNameError, lastNameError));
        lastNameField.textProperty().addListener((obs, oldVal, newVal) -> validateForm(firstNameField, lastNameField, saveButton, firstNameError, lastNameError));

        // Focus
        Platform.runLater(() -> firstNameField.requestFocus());

        // Récupération des données
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String firstName = firstNameField.getText().trim();
                String lastName = lastNameField.getText().trim();

                // Capitaliser la première lettre (au cas où)
                firstName = capitalizeFirstLetter(firstName);
                lastName = capitalizeFirstLetter(lastName);

                User newUser = new User();
                newUser.setFirstName(firstName);
                newUser.setLastName(lastName);
                newUser.setUsername(firstName + " " + lastName);
                newUser.setEmail("");
                newUser.setOnline(true);
                newUser.setActive(true);
                newUser.setType("contact");
                newUser.setFavorite(false);

                return newUser;
            }
            return null;
        });

        // Traitement du résultat
        Optional<User> result = dialog.showAndWait();

        result.ifPresent(newUser -> {
            try {
                userService.ajouter(newUser);
                loadUsers();
                updateStats();
                showSuccess("Succès", newUser.getUsername() + " a été ajouté !");
            } catch (SQLException e) {
                showError("Erreur", "Impossible d'ajouter: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Valide le formulaire et active/désactive le bouton Ajouter
     */
    private void validateForm(TextField firstNameField, TextField lastNameField, Button addButton,
                              Label firstNameError, Label lastNameError) {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();

        boolean firstNameValid = !firstName.isEmpty() && isFirstLetterUppercase(firstName);
        boolean lastNameValid = !lastName.isEmpty() && isFirstLetterUppercase(lastName);

        if (!firstName.isEmpty() && !isFirstLetterUppercase(firstName)) {
            firstNameError.setText("❌ Le prénom doit commencer par une majuscule");
            firstNameField.setStyle("-fx-border-color: #EF4444; -fx-border-width: 2;");
        } else if (!firstName.isEmpty()) {
            firstNameError.setText("");
            firstNameField.setStyle("");
        }

        if (!lastName.isEmpty() && !isFirstLetterUppercase(lastName)) {
            lastNameError.setText("❌ Le nom doit commencer par une majuscule");
            lastNameField.setStyle("-fx-border-color: #EF4444; -fx-border-width: 2;");
        } else if (!lastName.isEmpty()) {
            lastNameError.setText("");
            lastNameField.setStyle("");
        }

        addButton.setDisable(!(firstNameValid && lastNameValid));
    }

    /**
     * Vérifie si la première lettre est une majuscule
     */
    private boolean isFirstLetterUppercase(String text) {
        if (text == null || text.isEmpty()) return false;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return false;
        char firstChar = trimmed.charAt(0);
        return Character.isUpperCase(firstChar);
    }

    /**
     * Met la première lettre en majuscule et le reste en minuscule
     */
    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) return text;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return text;
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }

    @FXML
    private void callContact() {
        if (selectedUser != null) {
            showInfo("Appel", "Appel de " + selectedUser.getUsername());
        }
    }

    @FXML
    private void videoCall() {
        if (selectedUser != null) {
            showInfo("Appel vidéo", "Appel vidéo avec " + selectedUser.getUsername());
        }
    }

    @FXML
    private void toggleFavorite() {
        if (selectedUser != null) {
            selectedUser.setFavorite(!selectedUser.isFavorite());
            contactsListView.refresh();
            String msg = selectedUser.isFavorite() ?
                "ajouté aux favoris" : "retiré des favoris";
            showInfo("Favoris", selectedUser.getUsername() + " " + msg);
        }
    }

    private void updateTabStyle(Button activeTab) {
        Button[] tabs = {allTab, onlineTab, favoritesTab};
        for (Button tab : tabs) {
            tab.getStyleClass().setAll("filter-tab");
        }
        activeTab.getStyleClass().setAll("filter-tab-active");
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "?";
        String[] parts = fullName.split(" ");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return parts[0].substring(0, 1).toUpperCase();
    }

    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        LocalDateTime now = LocalDateTime.now();
        if (dateTime.toLocalDate().equals(now.toLocalDate())) {
            return DateTimeFormatter.ofPattern("HH:mm").format(dateTime);
        } else {
            return DateTimeFormatter.ofPattern("dd/MM").format(dateTime);
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Appliquer le style CSS
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.getStyleClass().add("info");

        // Charger la feuille de style
        dialogPane.getStylesheets().add(getClass().getResource("/com/example/guser/gcommu/Alert.css").toExternalForm());

        // Styliser le bouton
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

        // Appliquer le style CSS
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.getStyleClass().add("info");
        dialogPane.getStyleClass().add("success-alert");

        // Charger la feuille de style
        dialogPane.getStylesheets().add(getClass().getResource("/com/example/guser/gcommu/Alert.css").toExternalForm());

        // Styliser le bouton
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

        // Appliquer le style CSS
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.getStyleClass().add("error");

        // Charger la feuille de style
        dialogPane.getStylesheets().add(getClass().getResource("/com/example/guser/gcommu/Alert.css").toExternalForm());

        // Styliser le bouton
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("btn-danger");
        }

        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Appliquer le style CSS
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.getStyleClass().add("warning");

        // Charger la feuille de style
        dialogPane.getStylesheets().add(getClass().getResource("/com/example/guser/gcommu/Alert.css").toExternalForm());

        // Styliser le bouton
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("btn-primary");
        }

        alert.showAndWait();
    }

    private void showAlert(String title, String message) {
        showInfo(title, message);
    }

    /**
     * Modifier un contact
     */
    private void modifyContact(User user) {
        // Créer une boîte de dialogue de modification
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Modifier le contact");
        dialog.setHeaderText("Modifier " + user.getUsername());

        // Appliquer le style CSS
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        dialog.getDialogPane().getStyleClass().add("confirmation");

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Styliser les boutons
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelButtonType);

        saveButton.getStyleClass().add("btn-primary");
        cancelButton.getStyleClass().add("btn-secondary");

        // Formulaire pré-rempli
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField firstNameField = new TextField(user.getFirstName());
        firstNameField.setPromptText("Prénom");

        TextField lastNameField = new TextField(user.getLastName());
        lastNameField.setPromptText("Nom");

        // Labels pour les messages d'erreur
        Label firstNameError = new Label();
        firstNameError.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 11px;");

        Label lastNameError = new Label();
        lastNameError.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 11px;");

        grid.add(new Label("Prénom:"), 0, 0);
        grid.add(firstNameField, 1, 0);
        grid.add(firstNameError, 1, 1);
        grid.add(new Label("Nom:"), 0, 2);
        grid.add(lastNameField, 1, 2);
        grid.add(lastNameError, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Validation en temps réel
        saveButton.setDisable(true);

        firstNameField.textProperty().addListener((obs, oldVal, newVal) ->
            validateField(firstNameField, firstNameError, saveButton, lastNameField, lastNameError));
        lastNameField.textProperty().addListener((obs, oldVal, newVal) ->
            validateField(lastNameField, lastNameError, saveButton, firstNameField, firstNameError));

        // Récupération des données modifiées
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String firstName = firstNameField.getText().trim();
                String lastName = lastNameField.getText().trim();

                firstName = capitalizeFirstLetter(firstName);
                lastName = capitalizeFirstLetter(lastName);

                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setUsername(firstName + " " + lastName);

                return user;
            }
            return null;
        });

        Optional<User> result = dialog.showAndWait();

        result.ifPresent(updatedUser -> {
            try {
                // Mettre à jour dans la base de données
                userService.update(updatedUser);

                // Recharger la liste
                loadUsers();

                showSuccess("Succès", "Contact modifié !");
            } catch (SQLException e) {
                showError("Erreur", "Impossible de modifier: " + e.getMessage());
            }
        });
    }

    /**
     * Supprimer un contact
     */
    private void deleteContact(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);

        // Créer un contenu personnalisé
        VBox content = new VBox(10);
        content.setAlignment(Pos.CENTER_LEFT);

        Label mainMessage = new Label("Supprimer le contact");
        mainMessage.getStyleClass().add("main-message");

        Label itemName = new Label(user.getUsername());
        itemName.getStyleClass().add("item-name");

        Label warningMessage = new Label("Êtes-vous sûr de vouloir supprimer ce contact ?");
        warningMessage.getStyleClass().add("warning-message");

        content.getChildren().addAll(mainMessage, itemName, warningMessage);
        confirm.getDialogPane().setContent(content);

        // Appliquer le style CSS
        DialogPane dialogPane = confirm.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.getStyleClass().add("confirmation");

        // Charger la feuille de style
        dialogPane.getStylesheets().add(getClass().getResource("/com/example/guser/gcommu/Alert.css").toExternalForm());

        // Remplacer les boutons standards par des boutons stylisés
        ButtonType okButtonType = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(okButtonType, cancelButtonType);

        Button okButton = (Button) dialogPane.lookupButton(okButtonType);
        Button cancelButton = (Button) dialogPane.lookupButton(cancelButtonType);

        okButton.getStyleClass().add("btn-danger");
        cancelButton.getStyleClass().add("btn-secondary");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == okButtonType) {
            try {
                // Supprimer de la base de données
                userService.supprimer(user.getId());

                // Recharger la liste
                loadUsers();
                updateStats();

                // Cacher la carte de sélection si c'était le contact sélectionné
                if (selectedUser != null && selectedUser.getId() == user.getId()) {
                    selectedContactCard.setVisible(false);
                    selectedContactCard.setManaged(false);
                    emptyState.setVisible(true);
                    emptyState.setManaged(true);
                }

                showSuccess("Succès", "Contact supprimé !");
            } catch (SQLException e) {
                showError("Erreur", "Impossible de supprimer: " + e.getMessage());
            }
        }
    }

    /**
     * Valider un champ individuel
     */
    private void validateField(TextField field, Label errorLabel, Button saveButton,
                               TextField otherField, Label otherError) {
        String text = field.getText().trim();

        if (!text.isEmpty() && !isFirstLetterUppercase(text)) {
            errorLabel.setText("❌ Doit commencer par une majuscule");
            field.setStyle("-fx-border-color: #EF4444; -fx-border-width: 2;");
        } else if (!text.isEmpty()) {
            errorLabel.setText("");
            field.setStyle("");
        } else {
            errorLabel.setText("❌ Champ requis");
            field.setStyle("-fx-border-color: #EF4444; -fx-border-width: 2;");
        }

        // Vérifier si tous les champs sont valides
        boolean firstNameValid = !otherField.getText().trim().isEmpty() &&
            isFirstLetterUppercase(otherField.getText().trim());
        boolean lastNameValid = !field.getText().trim().isEmpty() &&
            isFirstLetterUppercase(field.getText().trim());

        saveButton.setDisable(!(firstNameValid && lastNameValid));
    }
}
