package edu.nyu.cs6103.movietickets.shared.dto;

public record MovieDto(
        long id,
        String title,
        int durationMinutes,
        String description,
        String genre,
        String posterPath,
        boolean active) {
}
