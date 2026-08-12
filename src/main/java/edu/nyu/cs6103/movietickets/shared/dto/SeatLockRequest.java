package edu.nyu.cs6103.movietickets.shared.dto;

import java.util.List;

public record SeatLockRequest(long showtimeId, List<Long> seatIds) {
    public SeatLockRequest {
        seatIds = seatIds == null ? List.of() : List.copyOf(seatIds);
    }
}
