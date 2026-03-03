package controllers;

import entities.Conversation;
import entities.Message;
import entities.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.ConversationService;
import services.MessageService;
import services.SpeechToTextService;
import services.UserService;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MessengerController {

    // ==================== FIELDS ====================
    @FXML private ListView<Conversation> conversationsListView;
    @FXML private ListView<User> onlineUsersListView;
    @FXML private ListView<Conversation> archivedListView;
    @FXML private Label chatWithLabel;
    @FXML private Label onlineStatusLabel;
    @FXML private VBox messagesContainer;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private TextField messageInputField;
    @FXML private Button sendButton;
    @FXML private Label chatAvatarInitials;
    @FXML private Label navAvatarInitials;
    @FXML private Label navNameLabel;
    @FXML private TextField searchField;
    @FXML private Label conversationsCount;
    @FXML private Label currentSectionTitle;
    @FXML private Button voiceRecordButton;

    @FXML private Button btnMessages;
    @FXML private Button btnOnline;
    @FXML private Button btnArchived;
    @FXML private Button btnSettings;
    @FXML private Button btnStatistics;
    @FXML private Button btnPreviousConversations;
    @FXML private Button btnUnread;
    @FXML private Button btnFavorites;
    @FXML private Button btnGroups;
    @FXML private Button filterAllBtn;
    @FXML private Button filterUnreadBtn;
    @FXML private Button filterGroupsBtn;
    @FXML private Button filterFavoritesBtn;

    private ConversationService conversationService;
    private MessageService messageService;
    private UserService userService;
    private SpeechToTextService speechService;
    private AudioRecorder audioRecorder;

    private Conversation currentConversation;
    private int currentUserId = 1;
    private Button activeButton = null;
    private User selectedUser;
    private List<User> favoriteUsers = new ArrayList<>();
    private boolean isRecordingVoice = false;

    // ==================== INITIALIZE ====================
    @FXML
    public void initialize() {
        System.out.println("Initializing MessengerController...");
        try {
            conversationService = new ConversationService();
            messageService = new MessageService();
            userService = new UserService();
            speechService = new SpeechToTextService();
            audioRecorder = new AudioRecorder();
            System.out.println("✅ Services initialisés avec succès");
        } catch (Exception e) {
            System.err.println("❌ Erreur initialisation services: " + e.getMessage());
            e.printStackTrace();
        }

        // Set user info
        if (navNameLabel != null) navNameLabel.setText("YB");
        if (navAvatarInitials != null) navAvatarInitials.setText("YB");

        // Setup lists
        setupConversationsList();
        setupOnlineUsersList();
        setupArchivedList();

        // Load data
        loadConversations();
        loadOnlineUsers();

        // Set active button
        if (btnMessages != null) setActiveButton(btnMessages);

        // Setup enter key handler
        setupEnterKeyHandler();

        // Vérification des composants FXML
        checkFXMLInjection();
    }

    // ==================== SETUP METHODS ====================
    private void checkFXMLInjection() {
        if (conversationsListView == null) System.err.println("❌ conversationsListView est null");
        if (onlineUsersListView == null) System.err.println("❌ onlineUsersListView est null");
        if (archivedListView == null) System.err.println("❌ archivedListView est null");
        if (messagesContainer == null) System.err.println("❌ messagesContainer est null");
        if (messageInputField == null) System.err.println("❌ messageInputField est null");
        if (sendButton == null) System.err.println("❌ sendButton est null");
        if (voiceRecordButton == null) System.err.println("❌ voiceRecordButton est null");
    }

    private void setupConversationsList() {
        if (conversationsListView == null) {
            System.err.println("❌ Cannot setup conversations list: conversationsListView is null");
            return;
        }

        conversationsListView.setCellFactory(lv -> new ListCell<Conversation>() {
            @Override
            protected void updateItem(Conversation conversation, boolean empty) {
                super.updateItem(conversation, empty);
                if (empty || conversation == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cell = createConversationCell(conversation);
                    setGraphic(cell);

                    setOnMouseClicked(event -> {
                        if (event.getClickCount() == 1) {
                            selectConversation(conversation);
                        }
                    });
                }
            }
        });
    }

    private void setupOnlineUsersList() {
        if (onlineUsersListView == null) {
            System.err.println("❌ Cannot setup online users list: onlineUsersListView is null");
            return;
        }

        onlineUsersListView.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cell = createOnlineUserCell(user);
                    setGraphic(cell);
                    setOnMouseClicked(event -> {
                        if (event.getClickCount() == 1) {
                            startNewConversation(user);
                        }
                    });
                }
            }
        });
    }

    private void setupArchivedList() {
        if (archivedListView == null) {
            System.err.println("❌ Cannot setup archived list: archivedListView is null");
            return;
        }

        archivedListView.setCellFactory(lv -> new ListCell<Conversation>() {
            @Override
            protected void updateItem(Conversation conversation, boolean empty) {
                super.updateItem(conversation, empty);
                if (empty || conversation == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cell = createConversationCell(conversation);
                    setGraphic(cell);
                    setOnMouseClicked(event -> {
                        if (event.getClickCount() == 1) {
                            showInfo("Info", "Cette conversation est archivée. Désarchiver pour continuer.");
                        }
                    });
                }
            }
        });
    }

    // ==================== CELL CREATION ====================
    private HBox createConversationCell(Conversation conversation) {
        HBox hbox = new HBox(10);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setStyle("-fx-padding: 10; -fx-background-color: transparent;");
        hbox.getStyleClass().add("conversation-item");

        int otherUserId = conversation.getUser1Id() == currentUserId ?
            conversation.getUser2Id() : conversation.getUser1Id();

        String otherUserName = "User " + otherUserId;
        try {
            if (userService != null) {
                User otherUser = userService.getUserById(otherUserId);
                if (otherUser != null && otherUser.getUsername() != null) {
                    otherUserName = otherUser.getUsername();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Label avatar = new Label(getUserInitials(otherUserName));
        avatar.getStyleClass().add("conversation-avatar");

        VBox infoBox = new VBox(5);
        Label nameLabel = new Label(otherUserName);
        nameLabel.getStyleClass().add("conversation-name");

        String lastMessage = conversation.getDernierMessage();
        Label lastMessageLabel = new Label(lastMessage != null && !lastMessage.isEmpty() ?
            (lastMessage.length() > 30 ? lastMessage.substring(0, 27) + "..." : lastMessage) :
            "No messages yet");
        lastMessageLabel.getStyleClass().add("conversation-preview");

        infoBox.getChildren().addAll(nameLabel, lastMessageLabel);

        Label timeLabel = new Label(formatTimeAgo(conversation.getDateCreation()));
        timeLabel.getStyleClass().add("conversation-time");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        hbox.getChildren().addAll(avatar, infoBox, spacer, timeLabel);
        return hbox;
    }

    private HBox createOnlineUserCell(User user) {
        HBox hbox = new HBox(10);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setStyle("-fx-padding: 10; -fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #8B5CF6; -fx-border-width: 2; -fx-border-radius: 12;");
        hbox.setPrefWidth(350);

        Label avatar = new Label(getUserInitials(user.getUsername()));
        avatar.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: " +
            (user.isFavorite() ? "#EF4444" : "#31A24C") + "; " +
            "-fx-text-fill: white; -fx-background-radius: 50%; -fx-pref-width: 45; -fx-pref-height: 45; -fx-alignment: center;");

        VBox infoBox = new VBox(5);
        Label nameLabel = new Label(user.getUsername());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #1F2937;");

        String subtitle = user.getHeadline() != null ? user.getHeadline() : "En ligne";
        Label statusLabel = new Label("● " + subtitle);
        statusLabel.setStyle("-fx-text-fill: #31A24C; -fx-font-size: 12px;");

        infoBox.getChildren().addAll(nameLabel, statusLabel);

        Button favoriteBtn = new Button(user.isFavorite() ? "❤️" : "🤍");
        favoriteBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 5;");
        favoriteBtn.setOnAction(e -> toggleFavorite(user));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        hbox.getChildren().addAll(avatar, infoBox, spacer, favoriteBtn);

        hbox.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1 && !event.getTarget().equals(favoriteBtn)) {
                startNewConversation(user);
            }
        });

        return hbox;
    }

    private String getUserInitials(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "?";
        String[] parts = fullName.split(" ");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return parts[0].substring(0, 1).toUpperCase();
    }

    // ==================== DATA LOADING ====================
    private void loadConversations() {
        if (conversationsListView == null) return;

        try {
            System.out.println("🔄 Chargement des conversations...");
            List<Conversation> conversations = conversationService.getConversationsByUser(currentUserId);

            if (conversations.isEmpty()) {
                Conversation conv1 = new Conversation();
                conv1.setId(1);
                conv1.setUser1Id(1);
                conv1.setUser2Id(2);
                conv1.setDernierMessage("bonjour");
                conv1.setDateCreation(LocalDateTime.now());

                Conversation conv2 = new Conversation();
                conv2.setId(2);
                conv2.setUser1Id(1);
                conv2.setUser2Id(3);
                conv2.setDernierMessage("No messages yet");
                conv2.setDateCreation(LocalDateTime.now());

                conversations.add(conv1);
                conversations.add(conv2);
            }

            conversationsListView.getItems().setAll(conversations);

            if (conversationsCount != null) {
                conversationsCount.setText(conversations.size() + " conversations");
            }
            System.out.println("✅ " + conversations.size() + " conversations chargées");
        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement conversations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadOnlineUsers() {
        if (onlineUsersListView == null) return;

        try {
            System.out.println("🔄 Chargement des utilisateurs en ligne...");
            List<User> users = userService.getAllUsers();
            users.removeIf(user -> user.getId() == currentUserId);

            if (users.isEmpty()) {
                User test1 = new User(2, "Jean Dupont");
                test1.setHeadline("Développeur");
                test1.setOnline(true);
                test1.setFavorite(true);

                User test2 = new User(3, "Marie Martin");
                test2.setHeadline("Designer");
                test2.setOnline(true);
                test2.setFavorite(false);

                User test3 = new User(4, "Pierre Durand");
                test3.setHeadline("Manager");
                test3.setOnline(true);
                test3.setFavorite(true);

                users.add(test1);
                users.add(test2);
                users.add(test3);
            } else {
                for (int i = 0; i < users.size() && i < 2; i++) {
                    users.get(i).setFavorite(true);
                }
            }

            onlineUsersListView.getItems().setAll(users);
            System.out.println("✅ " + users.size() + " utilisateurs chargés");

        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement utilisateurs: " + e.getMessage());
            e.printStackTrace();

            List<User> fallback = new ArrayList<>();
            User fb1 = new User(2, "Jean Dupont");
            fb1.setFavorite(true);
            User fb2 = new User(3, "Marie Martin");
            fb2.setFavorite(false);
            User fb3 = new User(4, "Pierre Durand");
            fb3.setFavorite(true);

            fallback.add(fb1);
            fallback.add(fb2);
            fallback.add(fb3);

            onlineUsersListView.getItems().setAll(fallback);
        }
    }

    private void loadArchivedConversations() {
        if (archivedListView == null) return;

        try {
            List<Conversation> archived = conversationService.getArchivedConversations(currentUserId);
            archivedListView.getItems().setAll(archived);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==================== CONVERSATION METHODS ====================
    private void selectConversation(Conversation conversation) {
        if (conversation == null) return;

        this.currentConversation = conversation;
        System.out.println("✅ Conversation sélectionnée: " + conversation.getId());

        int otherUserId = conversation.getUser1Id() == currentUserId ?
            conversation.getUser2Id() : conversation.getUser1Id();

        try {
            if (userService != null) {
                User otherUser = userService.getUserById(otherUserId);
                if (otherUser != null && otherUser.getUsername() != null) {
                    chatWithLabel.setText(otherUser.getUsername());
                    chatAvatarInitials.setText(getUserInitials(otherUser.getUsername()));
                } else {
                    chatWithLabel.setText("User " + otherUserId);
                    chatAvatarInitials.setText("U" + otherUserId);
                }
            }
        } catch (SQLException e) {
            chatWithLabel.setText("User " + otherUserId);
            chatAvatarInitials.setText("U" + otherUserId);
            e.printStackTrace();
        }

        onlineStatusLabel.setText("● En ligne");
        loadMessages(conversation.getId());
    }

    private void loadMessages(int conversationId) {
        if (messagesContainer == null) return;

        try {
            System.out.println("🔄 Chargement des messages...");
            messagesContainer.getChildren().clear();
            List<Message> messages = messageService.getMessagesByConversation(conversationId);

            for (Message message : messages) {
                addMessageToContainer(message);
            }

            messagesScrollPane.setVvalue(1.0);
            System.out.println("✅ " + messages.size() + " messages chargés");
        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement messages: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addMessageToContainer(Message message) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/message_bubble.fxml"));
            HBox messageBubble = loader.load();
            MessageBubbleController controller = loader.getController();

            boolean isSent = message.getExpediteurId() == currentUserId;

            if (!isSent && userService != null) {
                try {
                    User sender = userService.getUserById(message.getExpediteurId());
                    if (sender != null && sender.getUsername() != null) {
                        message.setSenderName(sender.getUsername());
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            MessageBubbleController.MessageActionListener listener = new MessageBubbleController.MessageActionListener() {
                @Override
                public void onEdit(Message msg, String newContent) {
                    editMessage(msg, newContent);
                }

                @Override
                public void onDelete(Message msg) {
                    deleteMessage(msg);
                }
            };

            controller.setMessage(message, isSent, listener);
            messagesContainer.getChildren().add(messageBubble);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addImageToContainer(Message message, File imageFile) {
        try {
            VBox imageContainer = new VBox(5);
            imageContainer.setStyle("-fx-padding: 5; -fx-background-color: #F3F4F6; -fx-background-radius: 12;");
            imageContainer.setMaxWidth(300);

            Image image = new Image(imageFile.toURI().toString());
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(250);
            imageView.setFitHeight(200);
            imageView.setPreserveRatio(true);
            imageView.setStyle("-fx-cursor: hand;");

            imageView.setOnMouseClicked(event -> showFullImage(imageFile));

            HBox infoBox = new HBox(10);
            infoBox.setAlignment(Pos.CENTER_LEFT);

            Label fileNameLabel = new Label("📷 " + imageFile.getName());
            fileNameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");

            Label timeLabel = new Label(formatTime(LocalDateTime.now()));
            timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #9CA3AF;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            infoBox.getChildren().addAll(fileNameLabel, spacer, timeLabel);

            imageContainer.getChildren().addAll(imageView, infoBox);

            HBox wrapper = new HBox();
            wrapper.setAlignment(message.getExpediteurId() == currentUserId ?
                Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            wrapper.getChildren().add(imageContainer);

            messagesContainer.getChildren().add(wrapper);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showFullImage(File imageFile) {
        try {
            Stage stage = new Stage();
            stage.setTitle("Image - " + imageFile.getName());

            Image image = new Image(imageFile.toURI().toString());
            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(800);
            imageView.setFitHeight(600);

            ScrollPane scrollPane = new ScrollPane(imageView);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);

            Scene scene = new Scene(scrollPane, 900, 700);
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void editMessage(Message message, String newContent) {
        try {
            message.setContenu(newContent);
            message.setStatut("edited");
            messageService.update(message);
            if (currentConversation != null) {
                loadMessages(currentConversation.getId());
            }
            showSuccess("Succès", "Message modifié avec succès");
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de modifier le message");
        }
    }

    private void deleteMessage(Message message) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer ce message ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                messageService.supprimer(message.getId());
                if (currentConversation != null) {
                    loadMessages(currentConversation.getId());
                }
                showSuccess("Succès", "Message supprimé");
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Erreur", "Impossible de supprimer le message");
            }
        }
    }

    @FXML
    private void sendMessage() {
        if (messageInputField == null) {
            System.err.println("❌ messageInputField est null");
            return;
        }

        if (currentConversation == null) {
            showInfo("Info", "Veuillez sélectionner une conversation d'abord");
            return;
        }

        String content = messageInputField.getText().trim();
        if (content.isEmpty()) {
            showInfo("Info", "Le message ne peut pas être vide");
            return;
        }

        if (!isFirstLetterUppercase(content)) {
            showWarning("Erreur", "Le message doit commencer par une majuscule");
            messageInputField.setStyle("-fx-border-color: #EF4444; -fx-border-width: 2;");
            return;
        }
        messageInputField.setStyle("");

        try {
            System.out.println("🔄 Envoi du message: " + content);

            Message message = new Message();
            message.setContenu(content);
            message.setDateEnvoi(LocalDateTime.now());
            message.setStatut("sent");
            message.setType("text");
            message.setConversationId(currentConversation.getId());
            message.setExpediteurId(currentUserId);
            message.setDestinataireId(
                currentConversation.getUser1Id() == currentUserId ?
                    currentConversation.getUser2Id() : currentConversation.getUser1Id()
            );

            messageService.ajouter(message);
            System.out.println("✅ Message sauvegardé avec ID: " + message.getId());

            conversationService.updateDernierMessage(currentConversation.getId(), content);
            addMessageToContainer(message);
            messageInputField.clear();

            if (messagesScrollPane != null) {
                messagesScrollPane.setVvalue(1.0);
            }
            loadConversations();

        } catch (SQLException e) {
            System.err.println("❌ Erreur envoi message: " + e.getMessage());
            e.printStackTrace();
            showError("Erreur", "Impossible d'envoyer le message: " + e.getMessage());
        }
    }

    @FXML
    private void sendImage() {
        if (currentConversation == null) {
            showInfo("Info", "Veuillez sélectionner une conversation d'abord");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");

        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
            "Fichiers images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp");
        fileChooser.getExtensionFilters().add(imageFilter);

        File selectedFile = fileChooser.showOpenDialog(messagesScrollPane.getScene().getWindow());

        if (selectedFile != null) {
            try {
                byte[] imageBytes = java.nio.file.Files.readAllBytes(selectedFile.toPath());
                String base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes);

                Message message = new Message();
                message.setContenu("[IMAGE] " + selectedFile.getName());
                message.setImageData(base64Image);
                message.setDateEnvoi(LocalDateTime.now());
                message.setStatut("sent");
                message.setType("image");
                message.setConversationId(currentConversation.getId());
                message.setExpediteurId(currentUserId);
                message.setDestinataireId(
                    currentConversation.getUser1Id() == currentUserId ?
                        currentConversation.getUser2Id() : currentConversation.getUser1Id()
                );

                messageService.ajouter(message);
                addImageToContainer(message, selectedFile);
                conversationService.updateDernierMessage(currentConversation.getId(), "📷 Image");
                showSuccess("Succès", "Image envoyée avec succès");

            } catch (IOException e) {
                showError("Erreur", "Impossible de charger l'image: " + e.getMessage());
                e.printStackTrace();
            } catch (SQLException e) {
                showError("Erreur", "Impossible de sauvegarder l'image: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void startNewConversation(User user) {
        if (user == null) return;

        try {
            System.out.println("🔄 Nouvelle conversation avec: " + user.getUsername());

            Conversation existing = conversationService.getConversationEntre(currentUserId, user.getId());
            if (existing == null) {
                Conversation newConv = new Conversation();
                newConv.setDateCreation(LocalDateTime.now());
                newConv.setStatut("active");
                newConv.setUser1Id(currentUserId);
                newConv.setUser2Id(user.getId());
                conversationService.ajouter(newConv);
                existing = newConv;
                System.out.println("✅ Nouvelle conversation créée");
            }

            selectConversation(existing);
            loadConversations();
            conversationsListView.getSelectionModel().select(existing);

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de créer la conversation");
        }
    }

    // ==================== NAVIGATION METHODS ====================
    @FXML
    public void showMessages() {
        setActiveButton(btnMessages);
        conversationsListView.setVisible(true);
        onlineUsersListView.setVisible(false);
        archivedListView.setVisible(false);
        loadConversations();
    }

    @FXML
    public void showOnlineUsers() {
        setActiveButton(btnOnline);
        conversationsListView.setVisible(false);
        onlineUsersListView.setVisible(true);
        archivedListView.setVisible(false);
        loadOnlineUsers();
    }

    @FXML
    public void showArchived() {
        setActiveButton(btnArchived);
        conversationsListView.setVisible(false);
        onlineUsersListView.setVisible(false);
        archivedListView.setVisible(true);
        loadArchivedConversations();
    }

    @FXML
    public void showPreviousConversations() {
        setActiveButton(btnPreviousConversations);
        try {
            List<Conversation> allConversations = conversationService.getAllConversations(currentUserId);
            conversationsListView.getItems().setAll(allConversations);

            if (conversationsCount != null) {
                conversationsCount.setText(allConversations.size() + " conversations");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de charger les conversations: " + e.getMessage());
        }
    }

    @FXML
    public void showSettings() {
        setActiveButton(btnSettings);
        showInfo("Settings", "Paramètres");
    }

    @FXML
    public void showStatistics() {
        setActiveButton(btnStatistics);
        showInfo("Statistics", "Statistiques");
    }

    @FXML
    public void showUnread() {
        showInfo("Filtre", "Messages non lus");
    }

    @FXML
    public void showFavorites() {
        showInfo("Filtre", "Favoris");
    }

    @FXML
    public void showGroups() {
        showInfo("Filtre", "Groupes");
    }

    @FXML
    public void filterAll() {
        loadConversations();
    }

    @FXML
    public void filterUnread() {
        showInfo("Info", "Filtre non lus à implémenter");
    }

    @FXML
    public void filterGroups() {
        showInfo("Info", "Filtre groupes à implémenter");
    }

    @FXML
    public void filterFavorites() {
        showInfo("Info", "Filtre favoris à implémenter");
    }

    // ==================== UTILITY METHODS ====================
    private void setActiveButton(Button button) {
        if (button == null) return;

        Button[] buttons = {btnMessages, btnOnline, btnArchived, btnSettings, btnStatistics, btnPreviousConversations};
        for (Button btn : buttons) {
            if (btn != null) {
                btn.getStyleClass().remove("active");
            }
        }
        button.getStyleClass().add("active");
        activeButton = button;
    }

    private void setupEnterKeyHandler() {
        if (messageInputField != null) {
            messageInputField.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    if (!event.isShiftDown()) {
                        sendMessage();
                        event.consume();
                    }
                }
            });
        }
    }

    private void toggleFavorite(User user) {
        if (user == null) return;

        user.setFavorite(!user.isFavorite());

        if (user.isFavorite()) {
            if (!favoriteUsers.contains(user)) {
                favoriteUsers.add(user);
            }
            showSuccess("Favori", user.getUsername() + " ajouté aux favoris ❤️");
        } else {
            favoriteUsers.remove(user);
            showInfo("Favori", user.getUsername() + " retiré des favoris");
        }

        refreshOnlineUsersList();
    }

    private void refreshOnlineUsersList() {
        if (onlineUsersListView == null) return;
        onlineUsersListView.refresh();
    }

    public void setSelectedUser(User user) {
        this.selectedUser = user;
        startNewConversation(user);
    }

    public void setCurrentUserId(int id) {
        this.currentUserId = id;
    }

    private boolean isFirstLetterUppercase(String message) {
        if (message == null || message.isEmpty()) return false;
        String trimmed = message.trim();
        if (trimmed.isEmpty()) return false;
        return Character.isUpperCase(trimmed.charAt(0));
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return time.format(formatter);
    }

    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        LocalDateTime now = LocalDateTime.now();
        if (dateTime.toLocalDate().equals(now.toLocalDate())) {
            return formatTime(dateTime);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
            return dateTime.format(formatter);
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlert(String title, String content) {
        showInfo(title, content);
    }

    @FXML
    private void showEmojiPicker() {
        EmojiPickerWithAPI picker = new EmojiPickerWithAPI();
        picker.showAndWait().ifPresent(emoji -> {
            String currentText = messageInputField.getText();
            int caretPosition = messageInputField.getCaretPosition();
            String newText = currentText.substring(0, caretPosition) + emoji +
                currentText.substring(caretPosition);
            messageInputField.setText(newText);
            messageInputField.positionCaret(caretPosition + emoji.length());
        });
    }

    @FXML
    private void sendFile() {
        if (currentConversation == null) {
            showInfo("Info", "Veuillez sélectionner une conversation d'abord");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier");

        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"),
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
            new FileChooser.ExtensionFilter("PDF", "*.pdf"),
            new FileChooser.ExtensionFilter("Documents", "*.doc", "*.docx", "*.txt", "*.xls", "*.xlsx"),
            new FileChooser.ExtensionFilter("Archives", "*.zip", "*.rar", "*.7z")
        );

        File selectedFile = fileChooser.showOpenDialog(messagesScrollPane.getScene().getWindow());

        if (selectedFile != null) {
            if (selectedFile.length() > 10 * 1024 * 1024) {
                showWarning("Erreur", "Le fichier est trop volumineux (max 10 MB)");
                return;
            }

            try {
                String fileName = selectedFile.getName();
                String fileExtension = getFileExtension(fileName).toLowerCase();
                long fileSize = selectedFile.length();

                System.out.println("📎 Fichier sélectionné: " + fileName + " (" + formatFileSize(fileSize) + ")");

                String fileType = determineFileType(fileExtension);

                byte[] fileBytes = java.nio.file.Files.readAllBytes(selectedFile.toPath());
                String base64File = java.util.Base64.getEncoder().encodeToString(fileBytes);

                Message message = new Message();
                message.setContenu("[" + fileType + "] " + fileName);
                message.setFileData(base64File);
                message.setFileName(fileName);
                message.setFileSize(fileSize);
                message.setFileType(fileType);
                message.setDateEnvoi(LocalDateTime.now());
                message.setStatut("sent");
                message.setType("file");
                message.setConversationId(currentConversation.getId());
                message.setExpediteurId(currentUserId);
                message.setDestinataireId(
                    currentConversation.getUser1Id() == currentUserId ?
                        currentConversation.getUser2Id() : currentConversation.getUser1Id()
                );

                messageService.ajouter(message);
                System.out.println("✅ Fichier sauvegardé avec ID: " + message.getId());

                addFileToContainer(message, selectedFile);

                String emoji = getFileEmoji(fileType);
                conversationService.updateDernierMessage(currentConversation.getId(), emoji + " " + fileName);

                showSuccess("Succès", "Fichier envoyé : " + fileName);

            } catch (IOException e) {
                System.err.println("❌ Erreur lecture fichier: " + e.getMessage());
                showError("Erreur", "Impossible de lire le fichier: " + e.getMessage());
                e.printStackTrace();
            } catch (SQLException e) {
                System.err.println("❌ Erreur base de données: " + e.getMessage());
                showError("Erreur", "Impossible de sauvegarder le fichier: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void addFileToContainer(Message message, File file) {
        try {
            String fileName = file.getName();
            String fileExtension = getFileExtension(fileName);
            String fileType = determineFileType(fileExtension);
            String fileEmoji = getFileEmoji(fileType);

            VBox fileContainer = new VBox(8);
            fileContainer.setStyle(
                "-fx-padding: 15;" +
                    "-fx-background-color: #F9FAFB;" +
                    "-fx-background-radius: 12;" +
                    "-fx-border-color: #8B5CF6;" +
                    "-fx-border-width: 1.5;" +
                    "-fx-border-radius: 12;" +
                    "-fx-effect: dropshadow(gaussian, rgba(139,92,246,0.2), 8, 0, 0, 2);"
            );
            fileContainer.setMaxWidth(350);

            HBox headerBox = new HBox(12);
            headerBox.setAlignment(Pos.CENTER_LEFT);

            Label fileIcon = new Label(fileEmoji);
            fileIcon.setStyle("-fx-font-size: 28px; -fx-min-width: 40;");

            VBox infoBox = new VBox(4);

            Label fileNameLabel = new Label(fileName);
            fileNameLabel.setStyle(
                "-fx-font-size: 14px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: #1F2937;"
            );
            fileNameLabel.setWrapText(true);

            HBox detailsBox = new HBox(10);
            detailsBox.setAlignment(Pos.CENTER_LEFT);

            Label sizeLabel = new Label(formatFileSize(file.length()));
            sizeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");

            Label typeLabel = new Label(fileType.toUpperCase());
            typeLabel.setStyle(
                "-fx-background-color: " + getFileTypeColor(fileType) + ";" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 10px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 2 8;" +
                    "-fx-background-radius: 12;"
            );

            detailsBox.getChildren().addAll(sizeLabel, typeLabel);

            infoBox.getChildren().addAll(fileNameLabel, detailsBox);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button downloadBtn = new Button("⬇️ Télécharger");
            downloadBtn.setStyle(
                "-fx-background-color: #8B5CF6;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 6 12;" +
                    "-fx-background-radius: 20;" +
                    "-fx-cursor: hand;"
            );
            downloadBtn.setOnAction(e -> downloadFile(message, file));

            headerBox.getChildren().addAll(fileIcon, infoBox, spacer, downloadBtn);

            HBox footerBox = new HBox();
            footerBox.setAlignment(Pos.CENTER_RIGHT);

            Label timeLabel = new Label(formatTime(LocalDateTime.now()));
            timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #9CA3AF;");

            footerBox.getChildren().add(timeLabel);

            fileContainer.getChildren().addAll(headerBox, footerBox);

            HBox wrapper = new HBox();
            wrapper.setAlignment(message.getExpediteurId() == currentUserId ?
                Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            wrapper.setPadding(new Insets(5, 10, 5, 10));
            wrapper.getChildren().add(fileContainer);

            messagesContainer.getChildren().add(wrapper);
            messagesScrollPane.setVvalue(1.0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void downloadFile(Message message, File originalFile) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le fichier");
            fileChooser.setInitialFileName(message.getFileName());

            String extension = getFileExtension(message.getFileName());
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichier " + extension.toUpperCase(), "*." + extension)
            );

            File saveFile = fileChooser.showSaveDialog(messagesScrollPane.getScene().getWindow());

            if (saveFile != null) {
                byte[] fileBytes = java.util.Base64.getDecoder().decode(message.getFileData());
                java.nio.file.Files.write(saveFile.toPath(), fileBytes);

                showSuccess("Succès", "Fichier enregistré : " + saveFile.getName());
            }
        } catch (IOException e) {
            showError("Erreur", "Impossible d'enregistrer le fichier");
            e.printStackTrace();
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " o";
        if (size < 1024 * 1024) return String.format("%.1f Ko", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f Mo", size / (1024.0 * 1024.0));
        return String.format("%.1f Go", size / (1024.0 * 1024.0 * 1024.0));
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1);
        }
        return "";
    }

    private String determineFileType(String extension) {
        extension = extension.toLowerCase();

        if (extension.matches("png|jpg|jpeg|gif|bmp|svg|webp")) {
            return "image";
        } else if (extension.matches("pdf")) {
            return "pdf";
        } else if (extension.matches("doc|docx|odt|txt|rtf")) {
            return "document";
        } else if (extension.matches("xls|xlsx|csv|ods")) {
            return "tableur";
        } else if (extension.matches("ppt|pptx|odp")) {
            return "presentation";
        } else if (extension.matches("zip|rar|7z|tar|gz")) {
            return "archive";
        } else if (extension.matches("mp3|wav|flac|aac|ogg")) {
            return "audio";
        } else if (extension.matches("mp4|avi|mkv|mov|wmv")) {
            return "video";
        } else {
            return "fichier";
        }
    }

    private String getFileEmoji(String fileType) {
        switch (fileType) {
            case "image": return "🖼️";
            case "pdf": return "📄";
            case "document": return "📝";
            case "tableur": return "📊";
            case "presentation": return "📽️";
            case "archive": return "🗜️";
            case "audio": return "🎵";
            case "video": return "🎬";
            default: return "📎";
        }
    }

    private String getFileTypeColor(String fileType) {
        switch (fileType) {
            case "image": return "#8B5CF6";
            case "pdf": return "#EF4444";
            case "document": return "#3B82F6";
            case "tableur": return "#10B981";
            case "presentation": return "#F59E0B";
            case "archive": return "#6B7280";
            case "audio": return "#EC4899";
            case "video": return "#8B5CF6";
            default: return "#6B7280";
        }
    }

    @FXML
    private void startAudioCall() {
        if (currentConversation == null) {
            showInfo("Info", "Veuillez sélectionner une conversation d'abord");
            return;
        }

        String otherUserName = chatWithLabel.getText();
        startJitsiCall(otherUserName, false);
    }

    @FXML
    private void startVideoCall() {
        if (currentConversation == null) {
            showInfo("Info", "Veuillez sélectionner une conversation d'abord");
            return;
        }

        String otherUserName = chatWithLabel.getText();
        startJitsiCall(otherUserName, true);
    }

    private static final String JITSI_APP_ID = "vpaas-magic-cookie-bb1aec7cdb2f49f1a35868a078689c3e";
    private static final String JITSI_KEY_ID = "vpaas-magic-cookie-bb1aec7cdb2f49f1a35868a078689c3e/ed72eb";
    private static final String JITSI_PRIVATE_KEY = """
        -----BEGIN PRIVATE KEY-----
        MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCfJSEEFZW1rX+M
        CTwehso+5wjm/YoYNc/a905KaNAIIHP4O1KHemd5kfL09i1EFuuvq8tR3M4xEMgS
        k/04zqh3KtIzJkv9nukRtGB33ieI2Oi1w16ZVxEGzf14u+p0+gyopqjjAjEG+SQb
        mUdJ4C32X9c0kkS0ntlQATaw2Y0r4pALanhqPAaz6YP8PUT9HFHEi6hZNOxiNNJF
        El/UzWpV7GZVDy/O80v3V2n5VWvaAre5W2FGSC5KpRN1rOt9toPsK5yvlsXna0CY
        WkuCWKZ+XU3VQjyo0rWSg34EfWimZ8z7sA8cA2SgDpcFiYzDBcFi2NKlmF98dW0U
        s1gZHhKZAgMBAAECggEBAJuKmWM5iXHgmZmPaQ/Y45BpFB1XTgHtjjJPpVg6aqCW
        v2Gs8TNyYfHCwLfEZ2d2E3JFsNlYurnf0fu+Yi6EZMjbtEfDtV+zUc45AlQFb5Rj
        k8YapbmvC/gfmg3HdBZSUc7/3G7KtIpBNJY40CEXFzUGZPp/vuDdpD16gi6E4mIx
        ZWHsjvWTWDPZQzppMz3HghA9ev5U5n2gtdadioCHR5YbxVYDI9KoQhft+YPMurQw
        mJBPJ+wO7FpZ57iZLzeXsxOm0vCFy8OYuLCwyCVG54Hrvk8kkdH3MDdjHojPMcpC
        M765NudS5MOxqEcnRvjQmJDWojSLLtIQI/VTOmCHWVECgYEA6oBd8Nk0CGubqzfl
        +p7RTqghPYhqATxED676mKpT/zVVLQEGqkITYKo/ZCm3DmB3ioSdQu9HAlD7ru/i
        6XLTrHe5FLvPzo/JsW93/KBkV/+DTN59KWARSBpiQSYS6PcaDkYv/0gu9RcMyoff
        CelWBTYiFB9Aljq6Yc9CyX/zv2UCgYEArbwvOrKZJFic22Ti5XKvZMZuxtYNfwH1
        HFKbdNuYszneTJ8DZw9zQhSdyDsc1JSZnRX0CXTQALreYGJPk/6ax1Oqu/GdthEZ
        fJwCwwzZwOO1AWdu2dNdYLOXXDolJ19CDPXiEfWkg9gRX77l12yGzrOa95Geudgz
        wo25Gyu9tSUCgYEAieJW2lJstPLJAqEImheeTNixSuQWKInOH0as1O16HFq4rZCn
        4Z6elD0mrveUSmDQiWM2sO/O2f7SLmehDdFVw1hWPGm+Y9/KIYnAjIum1NNJ2f4M
        rDJvluzkjxgbEF6TNIdym3FPIw+dlszTiZlo2gGdgiVqLat0giSDScXxhb0CgYA7
        JILXcpEbhGbxcOikv2Ph5IM1gQzbHal5WM2+/DqBWMPKRZ986A+OgUItNaJc47Ff
        fmezCGb+uJ6XJSB9+wBptpu6m0fbAPsyJlHyYF+IgHvP3Iwp7wAsuTZfmROo0Y7E
        pres8XhtwZI4i93mdfV2TEIG4TWtgmIsat6G118fWQKBgCQRglWWEaGqWPIp2MNy
        XOUxlDgi1uUNK5TxCTseS+Q/2LuYG6bPWYbUo/Xf2v93Jka0lf9uXcq8LvA8S78t
        1iYaZ0Dq8hKmGTnjgOCTNFbPIFZxeXkVT08kQwFvgC2HKI39sLi2E0Yz06DvaQld
        gNNuv7gjr2Xpva4Q3V7eOCqa
        -----END PRIVATE KEY-----
        """;

    private void startJitsiCall(String otherUserName, boolean isVideo) {
        try {
            String token = generateJitsiToken("Bensaid Youssef");

            if (token == null) {
                showError("Erreur", "Impossible de générer le token d'authentification");
                return;
            }

            String roomName = JITSI_APP_ID + "/" + currentConversation.getId();

            String jitsiUrl = "https://8x8.vc/" + roomName
                + "?jwt=" + token
                + "#config.startWithAudioMuted=false"
                + "&config.startWithVideoMuted=" + (!isVideo);

            openInBrowser(jitsiUrl);
            sendCallNotification(otherUserName, isVideo);

            System.out.println("🔗 URL de l'appel: " + jitsiUrl);

        } catch (Exception e) {
            showError("Erreur", "Impossible de démarrer l'appel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String generateJitsiToken(String userName) {
        try {
            String privateKeyContent = JITSI_PRIVATE_KEY
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

            byte[] keyBytes = java.util.Base64.getDecoder().decode(privateKeyContent);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = kf.generatePrivate(spec);

            Map<String, Object> userContext = new HashMap<>();
            userContext.put("name", userName);
            userContext.put("id", String.valueOf(currentUserId));

            Map<String, Object> context = new HashMap<>();
            context.put("user", userContext);
            context.put("features", Map.of(
                "livestreaming", true,
                "recording", true,
                "transcription", true,
                "outbound-call", true
            ));

            long now = System.currentTimeMillis();

            return Jwts.builder()
                .setHeaderParam("kid", JITSI_KEY_ID)
                .setHeaderParam("typ", "JWT")
                .claim("aud", "jitsi")
                .claim("iss", JITSI_APP_ID)
                .claim("sub", JITSI_APP_ID)
                .claim("room", "*")
                .claim("context", context)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + 3600000))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();

        } catch (Exception e) {
            System.err.println("❌ Erreur génération token Jitsi: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void openInBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
                } else if (os.contains("mac")) {
                    Runtime.getRuntime().exec("open " + url);
                } else {
                    Runtime.getRuntime().exec("xdg-open " + url);
                }
            }
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir le navigateur");
            e.printStackTrace();
        }
    }

    private void sendCallNotification(String otherUserName, boolean isVideo) {
        try {
            String callType = isVideo ? "📹 Appel vidéo" : "📞 Appel audio";
            String message = "🔔 " + callType + " démarré avec " + otherUserName;

            Message systemMessage = new Message();
            systemMessage.setContenu(message);
            systemMessage.setDateEnvoi(LocalDateTime.now());
            systemMessage.setStatut("sent");
            systemMessage.setType("system");
            systemMessage.setConversationId(currentConversation.getId());
            systemMessage.setExpediteurId(currentUserId);
            systemMessage.setDestinataireId(
                currentConversation.getUser1Id() == currentUserId ?
                    currentConversation.getUser2Id() : currentConversation.getUser1Id()
            );

            messageService.ajouter(systemMessage);
            addSystemMessageToContainer(systemMessage);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addSystemMessageToContainer(Message message) {
        try {
            HBox systemWrapper = new HBox();
            systemWrapper.setAlignment(Pos.CENTER);
            systemWrapper.setPadding(new Insets(10, 0, 10, 0));

            Label systemLabel = new Label(message.getContenu());
            systemLabel.setStyle(
                "-fx-background-color: #F3F4F6;" +
                    "-fx-text-fill: #6B7280;" +
                    "-fx-font-size: 12px;" +
                    "-fx-padding: 8 15;" +
                    "-fx-background-radius: 20;"
            );

            systemWrapper.getChildren().add(systemLabel);
            messagesContainer.getChildren().add(systemWrapper);
            messagesScrollPane.setVvalue(1.0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Plus besoin de languageSelector
// @FXML private ComboBox<SpeechToTextService.Language> languageSelector;

    @FXML
    private void toggleVoiceRecording() {
        if (currentConversation == null) {
            showAlert("Info", "Sélectionnez une conversation d'abord");
            return;
        }

        if (!isRecordingVoice) {
            audioRecorder.startRecording();
            isRecordingVoice = true;
            voiceRecordButton.setText("⏹️");
            voiceRecordButton.setStyle("-fx-background-color: #EF4444;");
        } else {
            voiceRecordButton.setText("⏳");
            voiceRecordButton.setDisable(true);

            new Thread(() -> {
                File audioFile = audioRecorder.stopRecording();
                isRecordingVoice = false;

                if (audioFile != null) {
                    // Détection automatique - plus besoin de passer la langue
                    String transcription = speechService.transcribeAudio(audioFile);

                    javafx.application.Platform.runLater(() -> {
                        String currentText = messageInputField.getText();
                        messageInputField.setText(currentText + transcription);

                        voiceRecordButton.setText("🎤");
                        voiceRecordButton.setDisable(false);
                        voiceRecordButton.setStyle("-fx-background-color: #374151;");

                        audioFile.delete();
                    });
                }
            }).start();
        }
    }
}
