package com.example.guser;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public final class SceneManager {
    private static Stage stage;

    private SceneManager() {}

    public static void init(Stage primaryStage) {
        stage = primaryStage;
       // stage.setMinWidth(1100);
        //stage.setMinHeight(700);
        stage.setMaximized(true);

    }

    public static void switchTo(String fxml, String title) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(SceneManager.class.getResource(fxml)));
            Scene scene = new Scene(root);
            scene.getStylesheets().add(Objects.requireNonNull(SceneManager.class.getResource("/com/example/guser/app.css")).toExternalForm());
            stage.setTitle(title);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException("Error loading view " + fxml + " : " + e.getMessage(), e);
        }
    }
}
