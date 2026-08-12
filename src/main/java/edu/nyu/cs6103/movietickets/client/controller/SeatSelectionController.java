package edu.nyu.cs6103.movietickets.client.controller;

import edu.nyu.cs6103.movietickets.client.SceneManager;
import edu.nyu.cs6103.movietickets.shared.NetworkResponse;
import edu.nyu.cs6103.movietickets.shared.RequestType;
import edu.nyu.cs6103.movietickets.shared.dto.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class SeatSelectionController extends AbstractClientController {
    @FXML private Label titleLabel;
    @FXML private Label summaryLabel;
    @FXML private Label statusLabel;
    @FXML private FlowPane seatPane;
    private SceneManager.ShowtimeSelection selection;
    private final Set<Long> selected = new HashSet<>();

    @Override protected void onShow(Object parameter) {
        if (!(parameter instanceof SceneManager.ShowtimeSelection validSelection)) {
            throw new IllegalArgumentException("Seat selection requires a movie and showtime");
        }
        selection = validSelection;
        titleLabel.setText(selection.movie().title() + " - Select seats");
        loadSeats();
    }

    private void loadSeats() {
        selected.clear();
        runNetwork(() -> client.send(RequestType.GET_SEAT_MAP,
                new SeatMapRequest(selection.showtime().id())), response -> {
            if (!requireSuccess(response)) return;
            SeatMapResponse map = client.responseData(response, SeatMapResponse.class);
            seatPane.getChildren().clear();
            for (SeatDto seat : map.seats()) {
                ToggleButton button = new ToggleButton(seat.rowLabel() + seat.seatNumber());
                button.setUserData(seat.id());
                button.getStyleClass().add("seat-button");
                boolean available = "AVAILABLE".equalsIgnoreCase(seat.status());
                button.setDisable(!available);
                if (!available) button.getStyleClass().add("seat-booked");
                Tooltip.install(button, new Tooltip(seat.status()));
                button.selectedProperty().addListener((ignored, oldValue, chosen) -> {
                    if (chosen) selected.add(seat.id()); else selected.remove(seat.id());
                    updateSummary();
                });
                seatPane.getChildren().add(button);
            }
            statusLabel.setText(map.availableCount() + " seat(s) available");
            updateSummary();
        }, statusLabel);
    }

    private void updateSummary() {
        BigDecimal total = selection.showtime().price()
                .multiply(BigDecimal.valueOf(selected.size()));
        summaryLabel.setText(selected.size() + " seat(s) selected - Total $" + total.toPlainString());
    }

    @FXML private void book() {
        if (selected.isEmpty()) {
            statusLabel.setText("Select at least one available seat.");
            return;
        }
        ArrayList<Long> seatIds = new ArrayList<>(selected);
        runNetwork(() -> client.send(RequestType.LOCK_SEATS,
                new SeatLockRequest(selection.showtime().id(), seatIds)), lockResponse -> {
            if (!requireSuccess(lockResponse)) { loadSeats(); return; }
            SeatLockResponse lock = client.responseData(lockResponse, SeatLockResponse.class);
            Alert payment = new Alert(Alert.AlertType.CONFIRMATION);
            payment.setTitle("Payment confirmation");
            payment.setHeaderText("Confirm simulated payment");
            payment.setContentText("Seats: " + seatIds + "\nTotal: $"
                    + selection.showtime().price().multiply(BigDecimal.valueOf(seatIds.size()))
                    + "\nSeat hold expires at: " + lock.expiresAt());
            Optional<ButtonType> choice = payment.showAndWait();
            if (choice.isEmpty() || choice.get() != ButtonType.OK) {
                releaseSeats(seatIds);
                return;
            }
            completeBooking(seatIds);
        }, statusLabel);
    }

    private void completeBooking(ArrayList<Long> seatIds) {
        runNetwork(() -> client.send(RequestType.CREATE_BOOKING,
                new CreateBookingRequest(selection.showtime().id(), seatIds)), response -> {
            if (!requireSuccess(response)) { loadSeats(); return; }
            BookingDto booking = client.responseData(response, BookingResponse.class).booking();
            new Alert(Alert.AlertType.INFORMATION, "Booking #" + booking.id()
                    + " confirmed. Total: $" + booking.totalPrice()).showAndWait();
            scenes.show(SceneManager.View.BOOKING_HISTORY);
        }, statusLabel);
    }

    private void releaseSeats(ArrayList<Long> seatIds) {
        runNetwork(() -> client.send(RequestType.RELEASE_SEATS,
                new SeatLockRequest(selection.showtime().id(), seatIds)),
                ignored -> loadSeats(), statusLabel);
    }

    @FXML private void back() { scenes.show(SceneManager.View.SHOWTIME, selection.movie()); }
}
