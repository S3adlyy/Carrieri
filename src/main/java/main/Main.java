package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/cours.fxml"));
        Scene scene = new Scene(loader.load());

        // Appliquer le style CSS
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // Configuration de la fenêtre
        stage.setScene(scene);
        stage.setTitle("🎓 Gestion des Cours - Système Académique");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);

        // Icone de l'application (optionnel)
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.png")));
        } catch (Exception e) {
            // Pas d'icône, on continue
        }

        // Afficher la fenêtre
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}