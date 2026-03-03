package com.example.guser;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import services.guser.AboutAIApiService;

import java.io.IOException;

public class HelloApplication extends Application {
    private Stage primaryStage;
    @Override
    public void start(Stage stage) throws Exception {
        //SceneManager.init(stage);
        this.primaryStage = stage;
        AppHostServices.init(getHostServices());
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/guser/guser/login.fxml"));
        //Scene scene = new Scene(root, Screen.getPrimary().getVisualBounds().getWidth(),Screen.getPrimary().getVisualBounds().getHeight());
        Scene scene = new Scene(root, 1400,800);
        AppRouter.init(scene);
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setX(bounds.getMinX());
        primaryStage.setY(bounds.getMinY());
        primaryStage.setWidth(bounds.getWidth());
        primaryStage.setHeight(bounds.getHeight());
        //primaryStage.setMaximized(true);
        primaryStage.show();


        //SceneManager.switchTo("/com/example/guser/guser/login.fxml", "Carrieri • Sign in");

    }
}
