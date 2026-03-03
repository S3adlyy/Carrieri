package com.example.guser.controllers.gcommu;

import entities.gcommu.Message;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;
import java.util.Base64;

public class MessageBubbleController {

    @FXML private VBox sentMessageWrapper;
    @FXML private VBox receivedMessageWrapper;
    @FXML private VBox editBox;

    @FXML private Label sentMessageLabel;
    @FXML private Label sentTimeLabel;
    @FXML private Label sentStatusLabel;
    @FXML private Label sentEditedLabel;

    @FXML private Label senderNameLabel;
    @FXML private Label receivedMessageLabel;
    @FXML private Label receivedTimeLabel;
    @FXML private Label receivedEditedLabel;

    @FXML private TextArea editTextArea;
    @FXML private HBox receivedActionsBox;

    @FXML private Button sentEditButton;
    @FXML private Button sentDeleteButton;
    @FXML private Button receivedEditButton;
    @FXML private Button receivedDeleteButton;
    @FXML private Button cancelEditButton;
    @FXML private Button saveEditButton;

    // Nouveaux champs pour les images
    @FXML private VBox sentImageContainer;
    @FXML private VBox receivedImageContainer;
    @FXML private ImageView sentImageView;
    @FXML private ImageView receivedImageView;
    @FXML private Label sentImageFileName;
    @FXML private Label receivedImageFileName;

    private Message currentMessage;
    private boolean isSent;
    private MessageActionListener actionListener;

    public interface MessageActionListener {
        void onEdit(Message message, String newContent);
        void onDelete(Message message);
    }

    @FXML
    public void initialize() {
        // Initialisation
    }

    public void setMessage(Message message, boolean isSent, MessageActionListener listener) {
        this.currentMessage = message;
        this.isSent = isSent;
        this.actionListener = listener;

        if (isSent) {
            showSentMessage();
        } else {
            showReceivedMessage();
        }
    }

    private void showSentMessage() {
        sentMessageWrapper.setVisible(true);
        sentMessageWrapper.setManaged(true);
        receivedMessageWrapper.setVisible(false);
        receivedMessageWrapper.setManaged(false);

        // Vérifier si c'est une image
        if ("image".equals(currentMessage.getType()) && currentMessage.getImageData() != null && !currentMessage.getImageData().isEmpty()) {
            // Afficher l'image
            try {
                byte[] imageBytes = Base64.getDecoder().decode(currentMessage.getImageData());
                Image image = new Image(new ByteArrayInputStream(imageBytes));
                sentImageView.setImage(image);
                sentImageView.setFitWidth(200);
                sentImageView.setFitHeight(150);
                sentImageView.setPreserveRatio(true);

                // Cacher le texte, montrer l'image
                sentMessageLabel.setVisible(false);
                sentMessageLabel.setManaged(false);
                sentImageContainer.setVisible(true);
                sentImageContainer.setManaged(true);

                // Extraire le nom du fichier du contenu
                String fileName = currentMessage.getContenu().replace("[IMAGE] ", "");
                sentImageFileName.setText(fileName);

            } catch (Exception e) {
                e.printStackTrace();
                sentMessageLabel.setText("❌ Image non disponible");
                sentMessageLabel.setVisible(true);
                sentMessageLabel.setManaged(true);
                sentImageContainer.setVisible(false);
                sentImageContainer.setManaged(false);
            }
        } else {
            // Afficher le texte
            sentMessageLabel.setText(currentMessage.getContenu());
            sentMessageLabel.setVisible(true);
            sentMessageLabel.setManaged(true);
            sentImageContainer.setVisible(false);
            sentImageContainer.setManaged(false);
        }

        sentTimeLabel.setText(formatTime(currentMessage.getDateEnvoi()));

        if ("edited".equals(currentMessage.getStatut())) {
            sentEditedLabel.setVisible(true);
        } else {
            sentEditedLabel.setVisible(false);
        }
    }

    private void showReceivedMessage() {
        receivedMessageWrapper.setVisible(true);
        receivedMessageWrapper.setManaged(true);
        sentMessageWrapper.setVisible(false);
        sentMessageWrapper.setManaged(false);

        String senderDisplay = currentMessage.getSenderName() != null ?
            currentMessage.getSenderName() : "User " + currentMessage.getExpediteurId();
        senderNameLabel.setText(senderDisplay);

        // Vérifier si c'est une image
        if ("image".equals(currentMessage.getType()) && currentMessage.getImageData() != null && !currentMessage.getImageData().isEmpty()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(currentMessage.getImageData());
                Image image = new Image(new ByteArrayInputStream(imageBytes));
                receivedImageView.setImage(image);
                receivedImageView.setFitWidth(200);
                receivedImageView.setFitHeight(150);
                receivedImageView.setPreserveRatio(true);

                // Cacher le texte, montrer l'image
                receivedMessageLabel.setVisible(false);
                receivedMessageLabel.setManaged(false);
                receivedImageContainer.setVisible(true);
                receivedImageContainer.setManaged(true);

                // Extraire le nom du fichier du contenu
                String fileName = currentMessage.getContenu().replace("[IMAGE] ", "");
                receivedImageFileName.setText(fileName);

            } catch (Exception e) {
                e.printStackTrace();
                receivedMessageLabel.setText("❌ Image non disponible");
                receivedMessageLabel.setVisible(true);
                receivedMessageLabel.setManaged(true);
                receivedImageContainer.setVisible(false);
                receivedImageContainer.setManaged(false);
            }
        } else {
            receivedMessageLabel.setText(currentMessage.getContenu());
            receivedMessageLabel.setVisible(true);
            receivedMessageLabel.setManaged(true);
            receivedImageContainer.setVisible(false);
            receivedImageContainer.setManaged(false);
        }

        receivedTimeLabel.setText(formatTime(currentMessage.getDateEnvoi()));

        if ("edited".equals(currentMessage.getStatut())) {
            receivedEditedLabel.setVisible(true);
        } else {
            receivedEditedLabel.setVisible(false);
        }
    }

    @FXML
    private void editMessage() {
        // Cacher les wrappers de message
        sentMessageWrapper.setVisible(false);
        sentMessageWrapper.setManaged(false);
        receivedMessageWrapper.setVisible(false);
        receivedMessageWrapper.setManaged(false);

        // Afficher la boîte d'édition
        editBox.setVisible(true);
        editBox.setManaged(true);

        // Remplir le texte à éditer
        editTextArea.setText(currentMessage.getContenu());
        editTextArea.requestFocus();
    }

    @FXML
    private void deleteMessage() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer le message");
        alert.setHeaderText(null);
        alert.setContentText("Êtes-vous sûr de vouloir supprimer ce message ?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && actionListener != null) {
                actionListener.onDelete(currentMessage);
            }
        });
    }

    @FXML
    private void saveEdit() {
        String newContent = editTextArea.getText().trim();
        if (!newContent.isEmpty() && actionListener != null) {
            actionListener.onEdit(currentMessage, newContent);
        } else {
            cancelEdit();
        }
    }

    @FXML
    private void cancelEdit() {
        editBox.setVisible(false);
        editBox.setManaged(false);

        if (isSent) {
            sentMessageWrapper.setVisible(true);
            sentMessageWrapper.setManaged(true);
        } else {
            receivedMessageWrapper.setVisible(true);
            receivedMessageWrapper.setManaged(true);
        }
    }

    private String formatTime(java.time.LocalDateTime time) {
        if (time == null) return "";
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        return time.format(formatter);
    }
}
