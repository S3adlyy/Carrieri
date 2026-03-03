package com.example.guser.controllers.grecru;

import com.example.guser.SceneManager;
import com.example.guser.controllers.guser.AppNavController;
import entities.grecru.Mission;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import services.grecru.MissionService;
import javafx.application.Platform;
import session.SessionContext;
import utils.grecru.AlertUtils;

import java.sql.SQLException;

public class MissionAddController {

    @FXML
    private TextArea descriptionField;

    @FXML
    private TextField scoreField;

    @FXML
    private TextField createdByIdField;

    @FXML
    private Label descriptionError;

    @FXML
    private Label scoreError;

    @FXML
    private Label creatorIdError;

    @FXML
    private Label characterCountLabel;

    @FXML
    private Button submitButton;

    private final MissionService missionService = new MissionService();
    private PauseTransition debounceTimer = new PauseTransition(Duration.millis(500));

    @FXML
    private void initialize() {
        setupRealTimeValidation();
        setupCharacterCounter();
        setupSubmitButtonState();
    }

    private void setupRealTimeValidation() {
        // Validation description avec debounce
        descriptionField.textProperty().addListener((observable, oldValue, newValue) -> {
            debounceTimer.setOnFinished(event -> validateDescription(newValue));
            debounceTimer.playFromStart();
        });

        // Validation score en temps réel
        scoreField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateScore(newValue);
        });

        // Validation creator ID en temps réel
        /*createdByIdField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateCreatorId(newValue);
        });*/

        // Validation numérique
        addNumericValidation(scoreField);
        //addNumericValidation(createdByIdField);

        // Focus listeners pour nettoyer les styles
        descriptionField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateDescription(descriptionField.getText());
        });

        scoreField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateScore(scoreField.getText());
        });

        /*createdByIdField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateCreatorId(createdByIdField.getText());
        });*/
    }

    private void setupCharacterCounter() {
        descriptionField.textProperty().addListener((observable, oldValue, newValue) -> {
            int length = newValue != null ? newValue.length() : 0;
            characterCountLabel.setText(length + "/1000");

            if (length > 950) {
                characterCountLabel.setStyle("-fx-text-fill: #ffaa00;");
            } else if (length >= 50) {
                characterCountLabel.setStyle("-fx-text-fill: #00C851;");
            } else {
                characterCountLabel.setStyle("-fx-text-fill: #888888;");
            }
        });
    }

    private void setupSubmitButtonState() {
        submitButton.disableProperty().bind(
                descriptionField.textProperty().isEmpty()
                        .or(scoreField.textProperty().isEmpty())
        );
    }

    private void addNumericValidation(TextField field) {
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty()) {
                if (!newValue.matches("\\d*")) {
                    field.setText(newValue.replaceAll("[^\\d]", ""));
                }
            }
        });
    }

    private boolean validateDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            showFieldError(descriptionField, descriptionError, "La description est obligatoire");
            return false;
        }

        String trimmedDesc = description.trim();
        if (trimmedDesc.length() < 50) {
            showFieldError(descriptionField, descriptionError,
                    "Minimum 50 caractères (actuel: " + trimmedDesc.length() + ")");
            return false;
        }

        if (trimmedDesc.length() > 1000) {
            showFieldError(descriptionField, descriptionError,
                    "Maximum 1000 caractères (actuel: " + trimmedDesc.length() + ")");
            return false;
        }

        clearFieldError(descriptionField, descriptionError);
        return true;
    }

    private boolean validateScore(String score) {
        if (score == null || score.trim().isEmpty()) {
            showFieldError(scoreField, scoreError, "Le score est obligatoire");
            return false;
        }

        try {
            int scoreValue = Integer.parseInt(score.trim());
            if (scoreValue < 0) {
                showFieldError(scoreField, scoreError, "Le score ne peut pas être négatif");
                return false;
            }
            if (scoreValue > 100) {
                showFieldError(scoreField, scoreError, "Le score maximum est 100");
                return false;
            }
        } catch (NumberFormatException e) {
            showFieldError(scoreField, scoreError, "Veuillez entrer un nombre valide");
            return false;
        }

        clearFieldError(scoreField, scoreError);
        return true;
    }

    private boolean validateCreatorId(String creatorId) {
        /*if (creatorId == null || creatorId.trim().isEmpty()) {
            showFieldError(createdByIdField, creatorIdError, "L'ID du créateur est obligatoire");
            return false;
        }

        try {
            int idValue = Integer.parseInt(creatorId.trim());
            if (idValue <= 0) {
                showFieldError(createdByIdField, creatorIdError, "L'ID doit être positif");
                return false;
            }
        } catch (NumberFormatException e) {
            showFieldError(createdByIdField, creatorIdError, "ID invalide");
            return false;
        }

        clearFieldError(createdByIdField, creatorIdError);*/
        return true;
    }

    // New method to validate unique description
    private boolean validateUniqueDescription(String description) {
        try {
            // Trim and normalize the description for comparison
            String normalizedDesc = description.trim();

            // Check if description already exists (case-insensitive)
            if (missionService.existsByDescriptionIgnoreCase(normalizedDesc)) {
                showFieldError(descriptionField, descriptionError,
                        "Une mission avec cette description existe déjà. Veuillez modifier la description pour la rendre unique.");
                return false;
            }
            return true;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification de la description: " + e.getMessage());
            e.printStackTrace();
            // Show a temporary error but allow submission? Better to show error
            AlertUtils.showError("Erreur de vérification",
                    "Impossible de vérifier l'unicité de la description. Veuillez réessayer.");
            return false;
        }
    }

    private void showFieldError(Control field, Label errorLabel, String errorMessage) {
        field.setStyle("-fx-border-color: #ff4444; -fx-border-width: 2; -fx-border-radius: 5; " +
                "-fx-background-radius: 5;");

        errorLabel.setText(errorMessage);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);

        Tooltip errorTooltip = new Tooltip(errorMessage);
        errorTooltip.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-padding: 5;");
        field.setTooltip(errorTooltip);

        DropShadow dropShadow = new DropShadow();
        dropShadow.setColor(Color.rgb(255, 68, 68, 0.3));
        dropShadow.setRadius(10);
        field.setEffect(dropShadow);
    }

    private void clearFieldError(Control field, Label errorLabel) {
        field.setStyle("");
        field.setTooltip(null);
        field.setEffect(null);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private boolean validateAllFields() {
        boolean isValid = true;
        isValid &= validateDescription(descriptionField.getText());
        isValid &= validateScore(scoreField.getText());
        //isValid &= validateCreatorId(createdByIdField.getText());

        // Only check uniqueness if description is valid
        if (isValid && validateDescription(descriptionField.getText())) {
            isValid &= validateUniqueDescription(descriptionField.getText().trim());
        }

        return isValid;
    }

    @FXML
    private void ajouterMission() {
        System.out.println("=== Début ajouterMission ===");

        if (!validateAllFields()) {
            System.out.println("Validation échouée");
            AlertUtils.showError("Erreur de validation",
                    "Veuillez corriger les erreurs dans le formulaire.");
            return;
        }

        try {
            String description = descriptionField.getText().trim();
            int scoreMin = Integer.parseInt(scoreField.getText().trim());
            int creatorId = SessionContext.getCurrentUser().getId();

            System.out.println("Création mission: " + description + ", score=" + scoreMin + ", creator=" + SessionContext.getCurrentUser().getId());

            Mission mission = new Mission(description, scoreMin, creatorId);

            System.out.println("Appel à missionService.ajouter()...");
            missionService.ajouter(mission);
            System.out.println("Mission ajoutée avec succès!");

            // Redirection immédiate vers la liste des missions
            //System.out.println("Redirection vers la liste des missions...");
            //navigateToMissionList();

        } catch (NumberFormatException e) {
            System.err.println("Erreur de format: " + e.getMessage());
            AlertUtils.showError("Erreur de format",
                    "Veuillez entrer des nombres valides.");
        } catch (Exception e) {
            System.err.println("Erreur système: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur système",
                    "Une erreur est survenue: " + e.getMessage());
        }
    }

    private void navigateToMissionList() {
        try {
            System.out.println("Récupération de l'instance AppNavController...");
            AppNavController controller = AppNavController.getInstance();

            if (controller != null) {
                System.out.println("Controller trouvé, appel de showMissionList()...");

                // Utiliser Platform.runLater pour être sûr que l'opération UI est sur le bon thread
                Platform.runLater(() -> {
                    try {
                        controller.onMissionShow(null);
                        System.out.println("Navigation réussie!");
                    } catch (Exception e) {
                        System.err.println("Erreur pendant la navigation: " + e.getMessage());
                        e.printStackTrace();
                        AlertUtils.showError("Erreur de navigation",
                                "Impossible de retourner à la liste des missions.");
                    }
                });
            } else {
                System.err.println("AppNavController.getInstance() a retourné null!");
                AlertUtils.showError("Erreur de navigation",
                        "Impossible de trouver le contrôleur principal.");
            }
        } catch (Exception e) {
            System.err.println("Exception dans navigateToMissionList: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur de navigation",
                    "Une erreur est survenue: " + e.getMessage());
        }
    }

    @FXML
    private void resetForm() {
        clearFields();
        clearAllErrors();
    }

    private void clearFields() {
        descriptionField.clear();
        scoreField.clear();
        //createdByIdField.clear();
    }

    private void clearAllErrors() {
        clearFieldError(descriptionField, descriptionError);
        clearFieldError(scoreField, scoreError);
        //clearFieldError(createdByIdField, creatorIdError);
    }

    @FXML
    private void showHelp() {
        AlertUtils.showInfo("Aide - Ajout de mission",
                "• Description : 50 à 1000 caractères\n" +
                        "• Score : Entre 0 et 100\n" +
                        "• ID Créateur : Nombre positif\n\n" +
                        "Tous les champs sont obligatoires."
        );
    }

    @FXML
    private void handleDescriptionPaste() {
        String content = descriptionField.getText();
        if (content != null && content.length() > 1000) {
            descriptionField.setText(content.substring(0, 1000));
            descriptionField.positionCaret(1000);
        }
    }
}