package edu.nyu.cs6103.movietickets.shared.dto;

import java.time.Instant;
import java.util.List;

public record SeatLockResponse(long showtimeId, List<Long> seatIds, Instant expiresAt) {
    public SeatLockResponse {
        seatIds = List.copyOf(seatIds);
    }
}
