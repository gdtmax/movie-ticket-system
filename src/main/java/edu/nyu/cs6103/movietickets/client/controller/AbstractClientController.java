package edu.nyu.cs6103.movietickets.client.controller;

import edu.nyu.cs6103.movietickets.client.*;
import edu.nyu.cs6103.movietickets.shared.NetworkResponse;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

public abstract class AbstractClientController implements ClientController {
    protected SocketClient client;
    protected Session session;
    protected SceneManager scenes;

    @Override
    public final void configure(SocketClient client, Session session, SceneManager scenes, Object parameter) {
        this.client = client;
        this.session = session;
        this.scenes = scenes;
        onShow(parameter);
    }

    protected void onShow(Object parameter) { }

    protected final <T> void runNetwork(NetworkCall<T> call, Consumer<T> success, Label status) {
        if (status != null) status.setText("Loading...");
        CompletableFuture.supplyAsync(() -> {
            try {
                if (!client.isConnected()) client.connect();
                return call.execute();
            } catch (Exception exception) {
                throw new CompletionException(exception);
            }
        }).whenComplete((result, failure) -> Platform.runLater(() -> {
            if (status != null) status.setText("");
            if (failure != null) {
                Throwable cause = failure.getCause() == null ? failure : failure.getCause();
                showError("Connection error", connectionMessage(cause));
            } else {
                success.accept(result);
            }
        }));
    }

    protected final boolean requireSuccess(NetworkResponse response) {
        if (response.successful()) return true;
        showError(response.error().code(), response.error().message());
        if ("AUTHENTICATION_REQUIRED".equals(response.error().code())) {
            session.clear();
            scenes.showLogin();
        }
        return false;
    }

    protected final void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message == null ? "Unknown error" : message);
        alert.showAndWait();
    }

    private static String connectionMessage(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof ConnectException || current instanceof SocketException) {
                return "Unable to connect to the server. Please make sure the server is running, then try again.";
            }
            current = current.getCause();
        }
        return failure.getMessage() == null
                ? "Unable to complete the request. Please try again."
                : failure.getMessage();
    }

    @FunctionalInterface protected interface NetworkCall<T> { T execute() throws IOException; }
}
