package com.example.guser;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        SceneManager.init(stage);
        AppHostServices.init(getHostServices());
        SceneManager.switchTo("/com/example/guser/login.fxml", "Carrieri • Sign in");

    }
}
