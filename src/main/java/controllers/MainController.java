package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class MainController {

    @FXML
    private StackPane contentPane;

    private void loadPage(String fxmlPath) {
        try {
            // Les fichiers FXML sont à la racine de resources
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent page = loader.load();
            contentPane.getChildren().setAll(page);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors du chargement de: " + fxmlPath);
        }
    }

    @FXML
    private void showReclamations() {
        loadPage("/reclamationList.fxml");
    }

    @FXML
    private void showFeedback() {
        loadPage("/feedbackList.fxml");
    }

    @FXML
    private void showTraitements() {
        loadPage("/traitementList.fxml"); // Si vous avez ce fichier
    }

    @FXML
    private void handleLogout() {
        System.out.println("Déconnexion...");
        // Logique de déconnexion si nécessaire
    }
}