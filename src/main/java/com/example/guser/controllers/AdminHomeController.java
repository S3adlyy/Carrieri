package com.example.guser.controllers;

import com.example.guser.SceneManager;
import javafx.fxml.FXML;
import session.SessionContext;

public class AdminHomeController {

    @FXML
    private void logout() {
        SessionContext.clear();
        SceneManager.switchTo("/com/example/guser/login.fxml", "Carrieri • Sign in");
    }
}
