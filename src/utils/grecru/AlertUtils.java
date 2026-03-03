package utils.grecru;

import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.geometry.Pos;

import java.util.Optional;

public final class AlertUtils {

    private static final String ALERT_CSS = "/com/example/guser/grecru/alert.css";

    private AlertUtils() {}

    // ==================== ALERTES DE BASE ====================

    public static void showSuccess(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);

        VBox content = new VBox(10);
        content.setAlignment(Pos.CENTER_LEFT);

        Label mainLabel = new Label(message);
        mainLabel.getStyleClass().add("main-message");
        mainLabel.setWrapText(true);

        content.getChildren().add(mainLabel);
        alert.getDialogPane().setContent(content);

        setIcon(alert, "✅");
        styleDialog(alert, AlertType.INFORMATION);
        alert.showAndWait();
    }

    public static void showWarning(String title, String message) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);

        VBox content = new VBox(12);
        content.setAlignment(Pos.CENTER_LEFT);

        Label mainLabel = new Label(message);
        mainLabel.getStyleClass().add("main-message");
        mainLabel.setWrapText(true);

        content.getChildren().add(mainLabel);
        alert.getDialogPane().setContent(content);

        setIcon(alert, "⚠");
        styleDialog(alert, AlertType.WARNING);
        alert.showAndWait();
    }

    public static void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);

        VBox content = new VBox(12);
        content.setAlignment(Pos.CENTER_LEFT);

        Label mainLabel = new Label(message);
        mainLabel.getStyleClass().add("main-message");
        mainLabel.setWrapText(true);

        content.getChildren().add(mainLabel);
        alert.getDialogPane().setContent(content);

        setIcon(alert, "❌");
        styleDialog(alert, AlertType.ERROR);
        alert.showAndWait();
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);

        VBox content = new VBox(12);
        content.setAlignment(Pos.CENTER_LEFT);

        Label mainLabel = new Label(message);
        mainLabel.getStyleClass().add("main-message");
        mainLabel.setWrapText(true);

        content.getChildren().add(mainLabel);
        alert.getDialogPane().setContent(content);

        setIcon(alert, "ℹ️");
        styleDialog(alert, AlertType.INFORMATION);
        alert.showAndWait();
    }

    // ==================== SUCCÈS AVEC INSTRUCTIONS ====================

    public static void showSuccessWithInstructions(String title, String mainMessage, String instructions) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);

        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER_LEFT);

        Label mainLabel = new Label(mainMessage);
        mainLabel.getStyleClass().add("main-message");
        mainLabel.setWrapText(true);

        Label instructionLabel = new Label(instructions);
        instructionLabel.getStyleClass().add("instruction-message");
        instructionLabel.setWrapText(true);

        content.getChildren().addAll(mainLabel, instructionLabel);

        alert.getDialogPane().setContent(content);
        alert.getDialogPane().getStyleClass().add("success-alert");

        setIcon(alert, "✅");
        styleDialog(alert, AlertType.INFORMATION);
        alert.showAndWait();
    }

    // ==================== ALERTE DE SÉLECTION ====================

    public static void showNoSelectionWarning(String elementType, String action) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("Aucun " + elementType + " sélectionné");
        alert.setHeaderText(null);
        alert.setContentText("Veuillez d'abord sélectionner un " + elementType + " dans la liste pour " + action + ".");

        setIcon(alert, "⚠");
        styleDialog(alert, AlertType.WARNING);
        alert.showAndWait();
    }

    public static void showNoSelectionWarning(String elementType) {
        showNoSelectionWarning(elementType, "effectuer cette action");
    }

    // ==================== CONFIRMATION DE SUPPRESSION ====================

    public static boolean showDeleteConfirmation(String elementType, String elementName, String consequences) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText(null);

        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER_LEFT);

        Label mainLabel = new Label("Supprimer " + elementType + " ?");
        mainLabel.getStyleClass().add("main-message");
        mainLabel.setWrapText(true);

        Label nameLabel = new Label(elementName);
        nameLabel.getStyleClass().add("item-name");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(450);

        Label warningLabel = new Label(consequences);
        warningLabel.getStyleClass().add("warning-message");
        warningLabel.setWrapText(true);

        content.getChildren().addAll(mainLabel, nameLabel, warningLabel);
        alert.getDialogPane().setContent(content);

        ButtonType btnSupprimer = new ButtonType("Supprimer", ButtonBar.ButtonData.YES);
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(btnSupprimer, btnAnnuler);

        setIcon(alert, "❓");
        styleDialog(alert, AlertType.CONFIRMATION);
        styleButtons(alert);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == btnSupprimer;
    }

    // ==================== CONFIRMATION GÉNÉRIQUE ====================

    public static boolean showConfirmation(String title, String question) {
        return showConfirmation(title, question, "Confirmer", "Annuler");
    }

    public static boolean showConfirmation(String title, String question, String btnConfirmText, String btnCancelText) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);

        VBox content = new VBox(12);
        content.setAlignment(Pos.CENTER_LEFT);

        Label mainLabel = new Label(question);
        mainLabel.getStyleClass().add("main-message");
        mainLabel.setWrapText(true);

        content.getChildren().add(mainLabel);
        alert.getDialogPane().setContent(content);

        ButtonType btnConfirm = new ButtonType(btnConfirmText, ButtonBar.ButtonData.YES);
        ButtonType btnCancel = new ButtonType(btnCancelText, ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(btnConfirm, btnCancel);

        setIcon(alert, "❓");
        styleDialog(alert, AlertType.CONFIRMATION);
        styleButtons(alert);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == btnConfirm;
    }

    // ==================== MÉTHODES DE STYLE ====================

    private static void styleDialog(Alert alert, AlertType type) {
        DialogPane dialogPane = alert.getDialogPane();

        switch (type) {
            case INFORMATION:
                dialogPane.getStyleClass().add("info");
                break;
            case WARNING:
                dialogPane.getStyleClass().add("warning");
                break;
            case ERROR:
                dialogPane.getStyleClass().add("error");
                break;
            case CONFIRMATION:
                dialogPane.getStyleClass().add("confirmation");
                break;
        }

        dialogPane.getStyleClass().add("glass-card");

        // Charger le CSS
        try {
            String css = AlertUtils.class.getResource(ALERT_CSS).toExternalForm();
            if (css != null && !dialogPane.getStylesheets().contains(css)) {
                dialogPane.getStylesheets().add(css);
            }
        } catch (Exception e) {
            System.err.println("⚠️ CSS alert.css non trouvé: " + ALERT_CSS);
        }

        // Ajuster la taille
        dialogPane.setMinHeight(Region.USE_PREF_SIZE);
        dialogPane.setMinWidth(480);
    }

    private static void styleButtons(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();
        ButtonType[] buttonTypes = alert.getButtonTypes().toArray(new ButtonType[0]);

        for (ButtonType btnType : buttonTypes) {
            Button btn = (Button) dialogPane.lookupButton(btnType);
            if (btn != null) {
                btn.getStyleClass().removeAll("btn-primary", "btn-secondary", "btn-danger");

                if (btnType == ButtonType.OK ||
                        btnType.getButtonData() == ButtonBar.ButtonData.YES) {
                    btn.getStyleClass().add("btn-primary");
                }
                else if (btnType == ButtonType.CANCEL ||
                        btnType.getButtonData() == ButtonBar.ButtonData.NO) {
                    btn.getStyleClass().add("btn-secondary");
                }
                else if (btnType.getText().toLowerCase().contains("supprimer")) {
                    btn.getStyleClass().add("btn-danger");
                }
                else {
                    btn.getStyleClass().add("btn-secondary");
                }
            }
        }
    }

    private static void setIcon(Alert alert, String icon) {
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 42px; -fx-min-width: 60px; -fx-alignment: center;");
        alert.setGraphic(iconLabel);
    }
}