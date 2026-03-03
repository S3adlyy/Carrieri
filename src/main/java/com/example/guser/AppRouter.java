package com.example.guser;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class AppRouter {
    private static Scene scene;

    private AppRouter() {}

    public static void init(Scene s) { scene = s; }

    public static void setRoot(String fxml, String title) {
        try {
            Parent root = FXMLLoader.load(AppRouter.class.getResource(fxml));
            scene.setRoot(root);
            if (scene.getWindow() instanceof Stage st) st.setTitle(title);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load: " + fxml, e);
        }
    }
}
