package edu.nyu.cs6103.movietickets.shared.dto;

import java.util.List;

public record CreateBookingRequest(long showtimeId, List<Long> seatIds) {
    public CreateBookingRequest {
        seatIds = List.copyOf(seatIds);
    }
}
