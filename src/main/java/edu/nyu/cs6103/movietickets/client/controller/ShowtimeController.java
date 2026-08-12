package edu.nyu.cs6103.movietickets.client.controller;

import edu.nyu.cs6103.movietickets.client.SceneManager;
import edu.nyu.cs6103.movietickets.shared.RequestType;
import edu.nyu.cs6103.movietickets.shared.dto.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.format.DateTimeFormatter;

public final class ShowtimeController extends AbstractClientController {
    @FXML private Label movieLabel;
    @FXML private Label statusLabel;
    @FXML private ListView<ShowtimeDto> showtimeList;
    private MovieDto movie;

    @Override protected void onShow(Object parameter) {
        movie = (MovieDto) parameter;
        movieLabel.setText(movie.title());
        showtimeList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(ShowtimeDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.startTime().format(DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a"))
                        + "    Theater " + item.theaterId() + "    $" + item.price().toPlainString());
            }
        });
        runNetwork(() -> client.send(RequestType.GET_SHOWTIMES, new ShowtimeRequest(movie.id())), response -> {
            if (requireSuccess(response)) showtimeList.getItems().setAll(client.responseData(response, ShowtimeListResponse.class).showtimes());
        }, statusLabel);
    }
    @FXML private void selectSeats() {
        ShowtimeDto showtime = showtimeList.getSelectionModel().getSelectedItem();
        if (showtime == null) { statusLabel.setText("Select a showtime first."); return; }
        scenes.show(SceneManager.View.SEAT_SELECTION, new SceneManager.ShowtimeSelection(movie, showtime));
    }
    @FXML private void back() { scenes.show(SceneManager.View.MOVIE_LIST); }
}
