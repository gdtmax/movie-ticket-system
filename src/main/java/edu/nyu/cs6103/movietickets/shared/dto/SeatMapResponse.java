package edu.nyu.cs6103.movietickets.shared.dto;

import java.util.List;

public record SeatMapResponse(
        ShowtimeDto showtime,
        List<SeatDto> seats,
        long availableCount,
        long bookedCount) {

    public SeatMapResponse {
        seats = List.copyOf(seats);
    }
}
