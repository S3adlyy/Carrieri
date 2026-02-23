package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Alert;
import java.io.IOException;

public class MainController {

    @FXML
    private StackPane contentPane;

    // Réclamations
    @FXML private void showReclamationList() { loadPage("/reclamationList.fxml"); }
    @FXML private void showReclamationForm() { loadPage("/reclamationForm.fxml"); }

    // Feedbacks
    @FXML private void showFeedbackList() { loadPage("/feedbackList.fxml"); }
    @FXML private void showFeedbackForm() { loadPage("/feedbackForm.fxml"); }

    // Traitements
    @FXML private void showTraitementList() { loadPage("/traitementList.fxml"); }
    @FXML private void showTraitementForm() { loadPage("/traitementForm.fxml"); }

    private void loadPage(String fxmlPath) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().setAll(page);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la page: " + fxmlPath);
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }
}