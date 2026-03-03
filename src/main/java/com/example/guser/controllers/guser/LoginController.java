package com.example.guser.controllers.guser;

import com.example.guser.AppRouter;
import com.example.guser.SceneManager;
import entities.guser.User;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import services.guser.UserService;
import session.SessionContext;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.InputStream;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    @FXML private TextField passwordVisibleField;
    @FXML private CheckBox showPassCheck;

    @FXML private Label errorLabel;
    @FXML private ImageView brandLogo;

    private UserService userService;

    @FXML
    public void initialize() {
        userService = new UserService();
        hideError();
        loadLogoSafe("/com/example/guser/images/logo.png", "/images/logo.png");

    }

    @FXML
    private void onTogglePassword() {
        boolean show = showPassCheck.isSelected();
        if (show) {
            passwordVisibleField.setText(passwordField.getText());
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
        } else {
            passwordField.setText(passwordVisibleField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
        }
    }
    // ========== SAFE RESOURCE LOADING ==========
    private void loadLogoSafe(String... paths) {
        try {
            for (String p : paths) {
                try (InputStream is = getClass().getResourceAsStream(p)) {
                    if (is != null) {
                        brandLogo.setImage(new Image(is));
                        return;
                    }
                }
            }
        } catch (Exception ignored) { }
        // fallback: no image, keep empty (or set a default placeholder in CSS)
        brandLogo.setImage(null);
    }

    @FXML
    private void onLogin() {
        hideError();
        try {
            String email = emailField.getText().trim();
            String pass = showPassCheck.isSelected() ? passwordVisibleField.getText() : passwordField.getText();

            User u = userService.login(email, pass);
            SessionContext.setCurrentUser(u);
            AppRouter.setRoot("/com/example/guser/guser/navbar.fxml", "Carrieri");//switch root to navbar
            AppNavController.getInstance().refreshAfterLogin();//bech tnahi l navbar disables


            switch (u.getRoles()) {
                case "CANDIDATE", "RECRUITER" -> {
                    session.ProfileViewContext.clear();
                    AppNavController.getInstance().show("/com/example/guser/guser/profile.fxml", "Carrieri • Profile");
                }
                case "ADMIN" -> AppNavController.getInstance().show("/com/example/guser/guser/admin_users.fxml", "Carrieri • Admin");

                default -> showError("Unknown role: " + u.getRoles());
            }

        } catch (Exception e) {
            showError(e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onGoSignup() {
        AppNavController.getInstance().show("/com/example/guser/guser/sign_up.fxml", "Carrieri • Sign up");

    }

    private void showError(String msg) {
        errorLabel.setText(msg == null ? "Login failed." : msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }


}
