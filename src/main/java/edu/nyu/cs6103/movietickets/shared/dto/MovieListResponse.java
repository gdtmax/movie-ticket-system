package edu.nyu.cs6103.movietickets.shared.dto;

import java.util.List;

public record MovieListResponse(List<MovieDto> movies) {
    public MovieListResponse {
        movies = List.copyOf(movies);
    }
}
