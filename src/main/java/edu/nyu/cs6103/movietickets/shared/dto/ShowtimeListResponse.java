package edu.nyu.cs6103.movietickets.shared.dto;

import java.util.List;

public record ShowtimeListResponse(List<ShowtimeDto> showtimes) {
    public ShowtimeListResponse {
        showtimes = List.copyOf(showtimes);
    }
}
