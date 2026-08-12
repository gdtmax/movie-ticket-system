package edu.nyu.cs6103.movietickets.client;

import edu.nyu.cs6103.movietickets.server.config.ServerConfig;
import javafx.application.Application;
import javafx.stage.Stage;

/** JavaFX client entry point. */
public final class MovieTicketClientApp extends Application {
    private Session session;
    private SocketClient socketClient;
    private SceneManager sceneManager;

    @Override
    public void start(Stage primaryStage) {
        ServerConfig config = ServerConfig.load();
        session = new Session();
        socketClient = new SocketClient(config, session);
        sceneManager = new SceneManager(primaryStage, socketClient, session);
        sceneManager.showLogin();
    }

    @Override
    public void stop() {
        if (socketClient != null) socketClient.close();
        if (session != null) session.clear();
    }

    public Session session() { return session; }
    public SocketClient socketClient() { return socketClient; }
    public SceneManager sceneManager() { return sceneManager; }

    public static void main(String[] args) { launch(args); }
}
