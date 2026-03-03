package utils.getude;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Optional;

public final class AlertUtils {

    private static final String ALERT_CSS = "/com/example/guser/getude/css/Alert.css";
    private static final String THEME_CSS = "/com/example/guser/getude/css/ThemeUnified.css";

    private AlertUtils() {}

    // ==================== ALERTES DE BASE ====================

    public static void showAlert(AlertType type, String title, String msg) {
        showAlert(type, title, null, msg);
    }

    public static void showAlert(AlertType type, String title, String header, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(msg);

        setIcon(alert, type);
        styleDialog(alert, type);
        alert.showAndWait();
    }

    // ==================== SUCCÈS AVEC MESSAGE COMPLET ====================

    /**
     * Alerte de succès avec message détaillé et instructions
     */
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

        setIcon(alert, AlertType.INFORMATION);
        styleDialog(alert, AlertType.INFORMATION);
        alert.showAndWait();
    }

    /**
     * Alerte de succès simple mais complète
     */
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

        setIcon(alert, AlertType.INFORMATION);
        styleDialog(alert, AlertType.INFORMATION);
        alert.showAndWait();
    }

    // ==================== ALERTES DE SÉLECTION ====================

    /**
     * Alerte pour "Aucun élément sélectionné" - VERSION COMPLÈTE
     */
    /**
     * Alerte pour "Aucun élément sélectionné" - VERSION TRÈS SIMPLE
     */
    public static void showNoSelectionWarning(String elementType, String action) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("Aucun " + elementType + " sélectionné");  // ← Titre de la fenêtre
        alert.setHeaderText(null);  // ← Pas de header
        alert.setContentText("Veuillez d'abord sélectionner un " + elementType + " dans la liste pour " + action + ".");

        setIcon(alert, AlertType.WARNING);
        styleDialog(alert, AlertType.WARNING);
        alert.showAndWait();
    }

    /**
     * Version simplifiée pour compatibilité
     */
    public static void showNoSelectionWarning(String elementType) {
        showNoSelectionWarning(elementType, "effectuer cette action");
    }

    // ==================== CONFIRMATION DE SUPPRESSION ====================

    /**
     * Confirmation de suppression avec nom de l'élément - VERSION COMPLÈTE
     */
    public static boolean showDeleteConfirmation(String elementType, String elementName, String consequences) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText(null);

        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER_LEFT);

        String elementTypeCapitalized = elementType.substring(0, 1).toUpperCase() + elementType.substring(1);

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

        setIcon(alert, AlertType.CONFIRMATION);
        styleDialog(alert, AlertType.CONFIRMATION);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == btnSupprimer;
    }

    /**
     * Version simplifiée pour les cours (avec conséquences par défaut)
     */
    public static boolean showDeleteConfirmation(String courseName) {
        return showDeleteConfirmation(
                "cours",
                courseName,
                "Tous les modules et leçons associés seront également supprimés définitivement."
        );
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

        setIcon(alert, AlertType.CONFIRMATION);
        styleDialog(alert, AlertType.CONFIRMATION);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == btnConfirm;
    }

    // ==================== ALERTES RACCOURCIES ====================

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

        setIcon(alert, AlertType.ERROR);
        styleDialog(alert, AlertType.ERROR);
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

        setIcon(alert, AlertType.WARNING);
        styleDialog(alert, AlertType.WARNING);
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

        setIcon(alert, AlertType.INFORMATION);
        styleDialog(alert, AlertType.INFORMATION);
        alert.showAndWait();
    }

    // ==================== MÉTHODES DE STYLE ====================

    private static void styleDialog(Alert alert, AlertType type) {
        DialogPane dialogPane = alert.getDialogPane();

        // Classes de style selon le type
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

        // Charger les CSS
        loadStylesheets(dialogPane);

        // Styliser les boutons
        styleButtons(alert, dialogPane);

        // Ajuster la taille
        dialogPane.setMinHeight(Region.USE_PREF_SIZE);
        dialogPane.setMinWidth(480);
    }

    private static void loadStylesheets(DialogPane dialogPane) {
        try {
            String alertCss = AlertUtils.class.getResource(ALERT_CSS).toExternalForm();
            if (alertCss != null && !dialogPane.getStylesheets().contains(alertCss)) {
                dialogPane.getStylesheets().add(alertCss);
            }
        } catch (Exception e) {
            System.err.println("⚠️ CSS Alert.css non trouvé: " + ALERT_CSS);
        }

        try {
            String themeCss = AlertUtils.class.getResource(THEME_CSS).toExternalForm();
            if (themeCss != null && !dialogPane.getStylesheets().contains(themeCss)) {
                dialogPane.getStylesheets().add(themeCss);
            }
        } catch (Exception e) {
            System.err.println("⚠️ CSS ThemeUnified.css non trouvé: " + THEME_CSS);
        }
    }

    private static void styleButtons(Alert alert, DialogPane dialogPane) {
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

    private static void setIcon(Alert alert, AlertType type) {
        Label iconLabel = new Label();
        iconLabel.setStyle("-fx-font-size: 42px; -fx-min-width: 60px; -fx-alignment: center;");

        switch (type) {
            case INFORMATION:
                iconLabel.setText("✅");
                break;
            case WARNING:
                iconLabel.setText("⚠");
                break;
            case ERROR:
                iconLabel.setText("❌");
                break;
            case CONFIRMATION:
                iconLabel.setText("❓");
                break;
        }

        alert.setGraphic(iconLabel);

    }
}