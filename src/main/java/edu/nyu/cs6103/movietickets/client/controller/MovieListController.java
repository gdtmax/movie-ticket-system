package edu.nyu.cs6103.movietickets.client.controller;

import edu.nyu.cs6103.movietickets.client.SceneManager;
import edu.nyu.cs6103.movietickets.shared.RequestType;
import edu.nyu.cs6103.movietickets.shared.dto.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public final class MovieListController extends AbstractClientController {
    @FXML private Label welcomeLabel;
    @FXML private Label statusLabel;
    @FXML private ListView<MovieDto> movieList;

    @Override protected void onShow(Object parameter) {
        welcomeLabel.setText("Welcome, " + session.currentUser().orElseThrow().username());
        movieList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(MovieDto movie, boolean empty) {
                super.updateItem(movie, empty);
                setText(empty || movie == null ? null : movie.title() + "  •  " + movie.genre()
                        + "  •  " + movie.durationMinutes() + " min\n" + movie.description());
            }
        });
        runNetwork(() -> client.send(RequestType.GET_MOVIES, null), response -> {
            if (requireSuccess(response)) movieList.getItems().setAll(client.responseData(response, MovieListResponse.class).movies());
        }, statusLabel);
    }

    @FXML private void showTimes() {
        MovieDto movie = movieList.getSelectionModel().getSelectedItem();
        if (movie == null) { statusLabel.setText("Select a movie first."); return; }
        scenes.show(SceneManager.View.SHOWTIME, movie);
    }
    @FXML private void history() { scenes.show(SceneManager.View.BOOKING_HISTORY); }
    @FXML private void logout() { runNetwork(client::logout, r -> { if (requireSuccess(r)) scenes.showLogin(); }, statusLabel); }
}
