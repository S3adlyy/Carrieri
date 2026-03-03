package com.example.guser.controllers.getude;

import com.example.guser.controllers.guser.AppNavController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import services.getude.ChatbotGroqService;

import java.io.IOException;

public class ChatbotController {

    @FXML private VBox messageContainer;
    @FXML private TextField txtQuestion;
    @FXML private Button btnEnvoyer;
    @FXML private Label lblContexte;
    @FXML private Label lblStatut;
    @FXML private ScrollPane scrollPane;

    private ChatbotGroqService chatbotService;
    private String coursActuel = "";
    private String moduleActuel = "";
    private String leconActuelle = "";
    private Image logoImage;
    private StackPane contentPaneParent;

    @FXML
    public void initialize() {
        chatbotService = new ChatbotGroqService();

        // Charger le logo
        try {
            logoImage = new Image(getClass().getResourceAsStream("@../../../../../resources/com/example/guser/getude/images/logo_chatbot.png"));
        } catch (Exception e) {
            System.err.println("Logo non trouvé");
        }

        txtQuestion.requestFocus();

        txtQuestion.textProperty().addListener((obs, oldVal, newVal) -> {
            btnEnvoyer.setDisable(newVal.trim().isEmpty());
        });

        btnEnvoyer.setDisable(true);
        lblStatut.setText("✅ Prêt - Mode gratuit");
    }

    public void setContentPane(StackPane contentPane) {
        this.contentPaneParent = contentPane;
        System.out.println("✅ contentPane reçu dans ChatbotController");
    }

    public void setContexte(String cours, String module, String lecon) {
        this.coursActuel = cours != null ? cours : "";
        this.moduleActuel = module != null ? module : "";
        this.leconActuelle = lecon != null ? lecon : "";

        StringBuilder contexte = new StringBuilder();
        if (!this.coursActuel.isEmpty()) {
            contexte.append("📚 ").append(this.coursActuel);
        }
        if (!this.moduleActuel.isEmpty()) {
            contexte.append(" | 📖 ").append(this.moduleActuel);
        }
        if (!this.leconActuelle.isEmpty()) {
            contexte.append(" | 📝 ").append(this.leconActuelle);
        }

        lblContexte.setText(contexte.length() > 0 ? contexte.toString() : "Cours: Aucun");
        chatbotService.setContexte(this.coursActuel, this.moduleActuel, this.leconActuelle);
    }

    @FXML
    private void envoyerQuestion() {
        String question = txtQuestion.getText().trim();
        if (question.isEmpty()) return;

        ajouterMessageUtilisateur(question);
        txtQuestion.clear();

        txtQuestion.setDisable(true);
        btnEnvoyer.setDisable(true);
        lblStatut.setText("🤔 Réflexion en cours...");

        new Thread(() -> {
            try {
                String reponse = chatbotService.poserQuestion(question);

                Platform.runLater(() -> {
                    ajouterMessageBot(reponse);
                    txtQuestion.setDisable(false);
                    btnEnvoyer.setDisable(true);
                    lblStatut.setText("✅ Prêt");
                    txtQuestion.requestFocus();
                });

            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    ajouterMessageBot("❌ Erreur de communication. Veuillez réessayer.");
                    txtQuestion.setDisable(false);
                    btnEnvoyer.setDisable(true);
                    lblStatut.setText("❌ Erreur");
                });
            }
        }).start();
    }

    private void ajouterMessageUtilisateur(String message) {
        HBox messageBox = new HBox(10);
        messageBox.setAlignment(Pos.CENTER_RIGHT);

        VBox bulle = new VBox(5);
        bulle.getStyleClass().add("message-bulle-utilisateur");

        Label lblMessage = new Label(message);
        lblMessage.setWrapText(true);

        Label lblHeure = new Label(java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        lblHeure.getStyleClass().add("message-heure-utilisateur");

        bulle.getChildren().addAll(lblMessage, lblHeure);

        Text avatar = new Text("👤");
        avatar.getStyleClass().add("user-avatar-text");

        messageBox.getChildren().addAll(bulle, avatar);
        messageContainer.getChildren().add(messageBox);
        scrollToBottom();
    }

    private void ajouterMessageBot(String message) {
        HBox messageBox = new HBox(10);
        messageBox.setAlignment(Pos.CENTER_LEFT);

        // Avatar du bot
        Node avatarNode;
        if (logoImage != null) {
            ImageView logoBot = new ImageView(logoImage);
            logoBot.setFitWidth(30);
            logoBot.setFitHeight(30);
            logoBot.setPreserveRatio(true);
            logoBot.getStyleClass().add("bot-avatar");
            avatarNode = logoBot;
        } else {
            Text fallback = new Text("🤖");
            fallback.setStyle("-fx-font-size: 25px;");
            avatarNode = fallback;
        }

        VBox bulle = new VBox(5);
        bulle.getStyleClass().add("message-bulle-bot");

        Label lblMessage = new Label(message);
        lblMessage.setWrapText(true);

        Label lblHeure = new Label(java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        lblHeure.getStyleClass().add("message-heure-bot");

        bulle.getChildren().addAll(lblMessage, lblHeure);

        messageBox.getChildren().addAll(avatarNode, bulle);
        messageContainer.getChildren().add(messageBox);
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (scrollPane != null) {
            scrollPane.setVvalue(1.0);
        }
    }


    @FXML
    private void fermer() {
        System.out.println("🔴 Bouton X cliqué - fermeture du chatbot");

        AppNavController controller = AppNavController.getInstance();
        if (controller != null) {
            controller.etudeFermerChatbot();
        } else if (contentPaneParent != null) {
            Node chatbotNode = (Node) txtQuestion.getScene().getRoot();
            contentPaneParent.getChildren().remove(chatbotNode);
        }
    }
}