package main;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.ChatbotGroqService;

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

    // ✅ AJOUT: Référence au contentPane
    private StackPane contentPaneParent;

    @FXML
    public void initialize() {
        chatbotService = new ChatbotGroqService();

        txtQuestion.requestFocus();

        txtQuestion.textProperty().addListener((obs, oldVal, newVal) -> {
            btnEnvoyer.setDisable(newVal.trim().isEmpty());
        });

        btnEnvoyer.setDisable(true);
        lblStatut.setText("✅ Prêt - Mode gratuit");
    }

    // ✅ NOUVELLE MÉTHODE pour recevoir le contentPane
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
        bulle.setStyle("-fx-background-color: #5E548E; -fx-background-radius: 15 0 15 15; -fx-padding: 10; -fx-max-width: 300;");

        Label lblMessage = new Label(message);
        lblMessage.setWrapText(true);
        lblMessage.setStyle("-fx-text-fill: white;");

        Label lblHeure = new Label(java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        lblHeure.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.7);");

        bulle.getChildren().addAll(lblMessage, lblHeure);

        Text avatar = new Text("👤");
        avatar.setStyle("-fx-font-size: 20px;");

        messageBox.getChildren().addAll(bulle, avatar);

        messageContainer.getChildren().add(messageBox);
        scrollToBottom();
    }

    private void ajouterMessageBot(String message) {
        HBox messageBox = new HBox(10);
        messageBox.setAlignment(Pos.CENTER_LEFT);

        // ✅ LOGO DU BOT (le même que dans le header)
        ImageView logoBot = new ImageView(new Image(getClass().getResourceAsStream("/images/logo_chatbot.png")));
        logoBot.setFitWidth(35); // Un peu plus petit que le header
        logoBot.setFitHeight(35);
        logoBot.setPreserveRatio(true);

        // Optionnel: ajouter un effet de style
        logoBot.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        VBox bulle = new VBox(5);
        bulle.setStyle("-fx-background-color: #f3e8ff; -fx-background-radius: 0 15 15 15; -fx-padding: 10; -fx-max-width: 250;");

        Label lblMessage = new Label(message);
        lblMessage.setWrapText(true);
        lblMessage.setStyle("-fx-text-fill: #231942;");

        Label lblHeure = new Label(java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        lblHeure.setStyle("-fx-font-size: 10px; -fx-text-fill: #6b7280;");

        bulle.getChildren().addAll(lblMessage, lblHeure);

        messageBox.getChildren().addAll(logoBot, bulle);

        messageContainer.getChildren().add(messageBox);
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (scrollPane != null) {
            scrollPane.setVvalue(1.0);
        }
    }

    @FXML
    private void exempleJava() {
        txtQuestion.setText("C'est quoi Java ?");
        envoyerQuestion();
    }

    @FXML
    private void exempleJavaFX() {
        txtQuestion.setText("Explique-moi JavaFX");
        envoyerQuestion();
    }

    @FXML
    private void exempleClasse() {
        txtQuestion.setText("C'est quoi une classe et un objet ?");
        envoyerQuestion();
    }

    // ✅ MÉTHODE FERMER CORRIGÉE
    @FXML
    private void fermer() {
        System.out.println("🔴 Bouton X cliqué - fermeture du chatbot");

        // Appeler directement la méthode du contrôleur parent
        CandidatShellController controller = CandidatShellController.getInstance();
        if (controller != null) {
            controller.fermerChatbot();
        } else {
            // Fallback: essayer de fermer via la référence contentPane
            if (contentPaneParent != null) {
                Node chatbotNode = (Node) txtQuestion.getScene().getRoot();
                contentPaneParent.getChildren().remove(chatbotNode);
            }
        }
    }
}