package edu.nyu.cs6103.movietickets.shared.dto;

public record LoginResponse(long userId, String username, String role, String token) {
}
