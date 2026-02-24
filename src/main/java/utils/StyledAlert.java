package utils;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * Classe utilitaire pour créer des alertes stylées selon le thème de l'application.
 * Utilise le fichier alert.css pour le design.
 */
public class StyledAlert {

    private static void styleAlert(Alert alert, String styleClass) {
        DialogPane dialogPane = alert.getDialogPane();

        // Charger le CSS alert.css
        try {
            String alertCss = StyledAlert.class.getResource("/css/alert.css").toExternalForm();
            if (!dialogPane.getStylesheets().contains(alertCss)) {
                dialogPane.getStylesheets().add(alertCss);
            }
        } catch (Exception e) {
            System.err.println("⚠️ CSS alert.css non trouvé");
        }

        // Ajouter la classe de style spécifique (confirmation, error, warning, info)
        if (styleClass != null && !styleClass.isEmpty()) {
            dialogPane.getStyleClass().add(styleClass);
        }

        // Ajouter la classe glass-card pour l'effet verre
        dialogPane.getStyleClass().add("glass-card");
    }

    /**
     * Stylise les boutons par défaut d'une alerte.
     * Pour ERROR, le premier bouton devient btn-danger, les autres btn-secondary.
     * Pour les autres types, le premier devient btn-primary.
     */
    private static void styleDefaultButtons(Alert alert, Alert.AlertType type) {
        DialogPane pane = alert.getDialogPane();
        int btnIndex = 0;
        for (ButtonType bt : alert.getButtonTypes()) {
            Node button = pane.lookupButton(bt);
            if (button != null) {
                button.getStyleClass().add("button");
                if (btnIndex == 0) {
                    if (type == Alert.AlertType.ERROR) {
                        button.getStyleClass().add("btn-danger");
                    } else {
                        button.getStyleClass().add("btn-primary");
                    }
                } else {
                    button.getStyleClass().add("btn-secondary");
                }
                btnIndex++;
            }
        }
    }

    /**
     * Applique les styles primaire/secondaire aux boutons personnalisés.
     * @param alert L'alerte contenant les boutons
     * @param buttonTypes Les types de boutons à styler (ceux avec YES/OK_DONE deviennent primaires, les autres secondaires)
     */
    public static void styleCustomButtons(Alert alert, ButtonType... buttonTypes) {
        DialogPane pane = alert.getDialogPane();
        for (ButtonType bt : buttonTypes) {
            Node button = pane.lookupButton(bt);
            if (button != null) {
                button.getStyleClass().add("button");
                if (bt.getButtonData() == ButtonBar.ButtonData.YES ||
                        bt.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                    button.getStyleClass().add("btn-primary");
                } else {
                    button.getStyleClass().add("btn-secondary");
                }
            }
        }
    }

    // ============================
    // Méthodes de création d'alertes
    // ============================

    public static Alert success(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        styleAlert(alert, "info");
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        styleDefaultButtons(alert, Alert.AlertType.INFORMATION);
        return alert;
    }

    public static Alert error(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        styleAlert(alert, "error");
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        styleDefaultButtons(alert, Alert.AlertType.ERROR);
        return alert;
    }

    public static Alert warning(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        styleAlert(alert, "warning");
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        styleDefaultButtons(alert, Alert.AlertType.WARNING);
        return alert;
    }

    public static Alert confirmation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        styleAlert(alert, "confirmation");
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        styleDefaultButtons(alert, Alert.AlertType.CONFIRMATION);
        return alert;
    }

    public static Alert info(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        styleAlert(alert, "info");
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        styleDefaultButtons(alert, Alert.AlertType.INFORMATION);
        return alert;
    }

    /**
     * Crée une alerte de confirmation de suppression avec un message personnalisé.
     * @param itemName Nom de l'élément à supprimer (ou description)
     * @return L'alerte configurée
     */
    public static Alert deleteConfirmation(String itemName) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        styleAlert(alert, "confirmation");
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("⚠️ Confirmer la suppression");

        // Contenu personnalisé
        VBox content = new VBox(16);
        content.getStyleClass().add("vbox");

        Label mainMessage = new Label("Êtes-vous sûr de vouloir supprimer cet élément ?");
        mainMessage.getStyleClass().add("main-message");
        mainMessage.setWrapText(true);

        Label itemLabel = new Label(itemName);
        itemLabel.getStyleClass().add("item-name");
        itemLabel.setWrapText(true);

        Label warningMessage = new Label("⚠️ Cette action est irréversible !");
        warningMessage.getStyleClass().add("warning-message");
        warningMessage.setWrapText(true);

        content.getChildren().addAll(mainMessage, itemLabel, warningMessage);
        alert.getDialogPane().setContent(content);

        // Boutons personnalisés
        ButtonType btnDelete = new ButtonType("🗑️ Supprimer");
        ButtonType btnCancel = new ButtonType("❌ Annuler");
        alert.getButtonTypes().setAll(btnDelete, btnCancel);

        // Styliser les boutons : Supprimer en rouge (btn-danger), Annuler en outline (btn-secondary)
        DialogPane pane = alert.getDialogPane();
        Node deleteButton = pane.lookupButton(btnDelete);
        if (deleteButton != null) {
            deleteButton.getStyleClass().addAll("button", "btn-danger");
        }
        Node cancelButton = pane.lookupButton(btnCancel);
        if (cancelButton != null) {
            cancelButton.getStyleClass().addAll("button", "btn-secondary");
        }

        return alert;
    }

    // ============================
    // Méthodes d'affichage simplifiées (showAndWait)
    // ============================

    public static void showSuccess(String title, String message) {
        success(title, null, message).showAndWait();
    }

    public static void showError(String title, String message) {
        error(title, "❌ Erreur", message).showAndWait();
    }

    public static void showWarning(String title, String message) {
        warning(title, "⚠️ Attention", message).showAndWait();
    }

    public static void showInfo(String title, String message) {
        info(title, null, message).showAndWait();
    }

    public static boolean showConfirmation(String title, String message) {
        return confirmation(title, null, message).showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}