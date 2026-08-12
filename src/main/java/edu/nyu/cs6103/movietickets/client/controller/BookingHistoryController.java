package edu.nyu.cs6103.movietickets.client.controller;

import edu.nyu.cs6103.movietickets.client.SceneManager;
import edu.nyu.cs6103.movietickets.shared.RequestType;
import edu.nyu.cs6103.movietickets.shared.dto.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public final class BookingHistoryController extends AbstractClientController {
    @FXML private ListView<BookingDto> bookingList;
    @FXML private Label statusLabel;

    @Override protected void onShow(Object parameter) {
        bookingList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(BookingDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "Booking #" + item.id() + "  •  " + item.status()
                        + "\nShowtime " + item.showtimeId() + "  •  Seats " + item.seatIds() + "  •  $" + item.totalPrice());
            }
        });
        refresh();
    }

    @FXML private void refresh() {
        runNetwork(() -> client.send(RequestType.GET_BOOKING_HISTORY, new BookingHistoryRequest()), response -> {
            if (requireSuccess(response)) bookingList.getItems().setAll(client.responseData(response, BookingResponse.class).bookings());
        }, statusLabel);
    }
    @FXML private void cancel() {
        BookingDto booking = bookingList.getSelectionModel().getSelectedItem();
        if (booking == null || !"CONFIRMED".equalsIgnoreCase(booking.status())) { statusLabel.setText("Select a confirmed booking."); return; }
        runNetwork(() -> client.send(RequestType.CANCEL_BOOKING, new CancelBookingRequest(booking.id())), response -> {
            if (requireSuccess(response)) refresh();
        }, statusLabel);
    }
    @FXML private void back() { scenes.show(SceneManager.View.MOVIE_LIST); }
}
