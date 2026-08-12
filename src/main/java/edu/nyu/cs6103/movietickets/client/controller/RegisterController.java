package edu.nyu.cs6103.movietickets.client.controller;

import edu.nyu.cs6103.movietickets.client.SceneManager;
import edu.nyu.cs6103.movietickets.shared.RequestType;
import edu.nyu.cs6103.movietickets.shared.dto.RegisterRequest;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public final class RegisterController extends AbstractClientController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label statusLabel;

    @FXML private void createAccount() {
        String user = usernameField.getText().trim();
        String password = passwordField.getText();
        if (user.isEmpty() || password.length() < 8) { statusLabel.setText("Use a username and at least 8 password characters."); return; }
        if (!password.equals(confirmField.getText())) { statusLabel.setText("Passwords do not match."); return; }
        runNetwork(() -> client.send(RequestType.REGISTER, new RegisterRequest(user, password)), response -> {
            if (requireSuccess(response)) scenes.showLogin();
        }, statusLabel);
    }
    @FXML private void back() { scenes.showLogin(); }
}
