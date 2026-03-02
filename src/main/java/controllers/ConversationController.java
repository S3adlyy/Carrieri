package controllers;

import entities.Conversation;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import services.ConversationService;

import java.sql.SQLException;
import java.util.List;

public class ConversationController {

    @FXML private TextField tfDernierMessage;
    @FXML private TextField tfStatut;
    @FXML private TextField tfUser1Id;
    @FXML private TextField tfUser2Id;
    @FXML private TableView<Conversation> tableConversations;
    @FXML private TableColumn<Conversation, Integer> colId;
    @FXML private TableColumn<Conversation, String> colDernierMessage;
    @FXML private TableColumn<Conversation, String> colStatut;
    @FXML private TableColumn<Conversation, Integer> colUser1Id;
    @FXML private TableColumn<Conversation, Integer> colUser2Id;
    @FXML private TableColumn<Conversation, String> colDateCreation;

    private ConversationService conversationService;

    public void initialize() {
        conversationService = new ConversationService();
        loadConversations();
    }

    private void loadConversations() {
        try {
            List<Conversation> conversations = conversationService.read();
            tableConversations.getItems().setAll(conversations);
        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les conversations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void addConversation(ActionEvent event) {
        try {
            if (tfDernierMessage.getText().trim().isEmpty() ||
                tfStatut.getText().trim().isEmpty() ||
                tfUser1Id.getText().trim().isEmpty() ||
                tfUser2Id.getText().trim().isEmpty()) {
                showWarning("Attention", "Tous les champs doivent être remplis");
                return;
            }

            Conversation c = new Conversation();
            c.setDernierMessage(tfDernierMessage.getText());
            c.setStatut(tfStatut.getText());
            c.setUser1Id(Integer.parseInt(tfUser1Id.getText()));
            c.setUser2Id(Integer.parseInt(tfUser2Id.getText()));
            conversationService.ajouter(c);
            loadConversations();
            clearFields();
            showSuccess("Succès", "Conversation ajoutée avec succès");
        } catch (SQLException e) {
            showError("Erreur", "Impossible d'ajouter la conversation: " + e.getMessage());
            e.printStackTrace();
        } catch (NumberFormatException e) {
            showWarning("Attention", "Les IDs doivent être des nombres valides");
        }
    }

    @FXML
    public void updateConversation(ActionEvent event) {
        Conversation selected = tableConversations.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                if (tfDernierMessage.getText().trim().isEmpty() ||
                    tfStatut.getText().trim().isEmpty() ||
                    tfUser1Id.getText().trim().isEmpty() ||
                    tfUser2Id.getText().trim().isEmpty()) {
                    showWarning("Attention", "Tous les champs doivent être remplis");
                    return;
                }

                selected.setDernierMessage(tfDernierMessage.getText());
                selected.setStatut(tfStatut.getText());
                selected.setUser1Id(Integer.parseInt(tfUser1Id.getText()));
                selected.setUser2Id(Integer.parseInt(tfUser2Id.getText()));
                conversationService.update(selected);
                loadConversations();
                showSuccess("Succès", "Conversation mise à jour avec succès");
            } catch (SQLException e) {
                showError("Erreur", "Impossible de mettre à jour la conversation: " + e.getMessage());
                e.printStackTrace();
            } catch (NumberFormatException e) {
                showWarning("Attention", "Les IDs doivent être des nombres valides");
            }
        } else {
            showWarning("Attention", "Veuillez sélectionner une conversation à modifier");
        }
    }

    @FXML
    public void deleteConversation(ActionEvent event) {
        Conversation selected = tableConversations.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showDeleteConfirmation(selected);
        } else {
            showWarning("Attention", "Veuillez sélectionner une conversation à supprimer");
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
                    loadConversations();
                    clearFields();
                    showSuccess("Succès", "Conversation supprimée avec succès");
                } catch (SQLException e) {
                    showError("Erreur", "Impossible de supprimer la conversation: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    public void handleTableSelection() {
        Conversation selected = tableConversations.getSelectionModel().getSelectedItem();
        if (selected != null) {
            tfDernierMessage.setText(selected.getDernierMessage());
            tfStatut.setText(selected.getStatut());
            tfUser1Id.setText(String.valueOf(selected.getUser1Id()));
            tfUser2Id.setText(String.valueOf(selected.getUser2Id()));
        }
    }

    private void clearFields() {
        tfDernierMessage.clear();
        tfStatut.clear();
        tfUser1Id.clear();
        tfUser2Id.clear();
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
