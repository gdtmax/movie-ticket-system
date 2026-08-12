package edu.nyu.cs6103.movietickets.client;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import edu.nyu.cs6103.movietickets.client.controller.ClientController;
import edu.nyu.cs6103.movietickets.shared.dto.MovieDto;
import edu.nyu.cs6103.movietickets.shared.dto.ShowtimeDto;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/** Owns the primary stage and provides one navigation point for all FXML views. */
public final class SceneManager {
    public static final double DEFAULT_WIDTH = 1100;
    public static final double DEFAULT_HEIGHT = 720;
    private static final String CSS_RESOURCE = "/css/application.css";

    private final Stage primaryStage;
    private final SocketClient socketClient;
    private final Session session;
    private View currentView;

    public SceneManager(Stage primaryStage, SocketClient socketClient, Session session) {
        this.primaryStage = Objects.requireNonNull(primaryStage, "primaryStage must not be null");
        this.socketClient = Objects.requireNonNull(socketClient, "socketClient must not be null");
        this.session = Objects.requireNonNull(session, "session must not be null");
        primaryStage.setTitle("NYU Movie Tickets");
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(560);
    }

    public void show(View view) { show(view, null); }

    public void show(View view, Object parameter) {
        Objects.requireNonNull(view, "view must not be null");
        try {
            LoadedView loaded = load(view);
            Parent root = loaded.root();
            if (!(loaded.controller() instanceof ClientController controller)) {
                throw new IllegalStateException("View has no ClientController: " + view.resource());
            }
            controller.configure(socketClient, session, this, parameter);
            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
                URL stylesheet = requireResource(CSS_RESOURCE);
                scene.getStylesheets().add(stylesheet.toExternalForm());
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
            currentView = view;
            primaryStage.show();
        } catch (RuntimeException exception) {
            System.getLogger(SceneManager.class.getName()).log(
                    System.Logger.Level.ERROR, "Unable to show " + view, exception);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation error");
            alert.setHeaderText("Unable to open " + view.name().replace('_', ' ').toLowerCase());
            alert.setContentText("The page could not be loaded. See the client console for details.");
            alert.showAndWait();
        }
    }

    public void showLogin() { show(View.LOGIN); }

    public View currentView() { return currentView; }

    public Stage primaryStage() { return primaryStage; }

    LoadedView load(View view) {
        URL resource = requireResource(view.resource());
        try {
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            return new LoadedView(root, loader.getController());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load view: " + view.resource(), exception);
        }
    }

    record LoadedView(Parent root, Object controller) { }
    public record ShowtimeSelection(MovieDto movie, ShowtimeDto showtime) { }

    private static URL requireResource(String path) {
        URL resource = SceneManager.class.getResource(path);
        if (resource == null) throw new IllegalStateException("Missing application resource: " + path);
        return resource;
    }

    public enum View {
        LOGIN("/fxml/login-view.fxml"),
        REGISTER("/fxml/register-view.fxml"),
        MOVIE_LIST("/fxml/movie-list-view.fxml"),
        SHOWTIME("/fxml/showtime-view.fxml"),
        SEAT_SELECTION("/fxml/seat-selection-view.fxml"),
        BOOKING_HISTORY("/fxml/booking-history-view.fxml"),
        ADMIN_DASHBOARD("/fxml/admin-dashboard-view.fxml"),
        MOVIE_MANAGEMENT("/fxml/movie-management-view.fxml"),
        THEATER_MANAGEMENT("/fxml/theater-management-view.fxml"),
        SHOWTIME_MANAGEMENT("/fxml/showtime-management-view.fxml");

        private final String resource;
        View(String resource) { this.resource = resource; }
        public String resource() { return resource; }
    }
}
