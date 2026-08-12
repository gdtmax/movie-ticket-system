package edu.nyu.cs6103.movietickets.client.controller;

import edu.nyu.cs6103.movietickets.client.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public final class LoginController extends AbstractClientController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    @FXML private void login() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) { statusLabel.setText("Enter username and password."); return; }
        runNetwork(() -> client.login(username, password), response -> {
            if (!requireSuccess(response)) return;
            passwordField.clear();
            scenes.show(session.isAdmin() ? SceneManager.View.ADMIN_DASHBOARD : SceneManager.View.MOVIE_LIST);
        }, statusLabel);
    }

    @FXML private void register() { scenes.show(SceneManager.View.REGISTER); }
}
