package edu.nyu.cs6103.movietickets.shared.dto;

import java.util.Map;

public record ErrorResponse(String code, String message, Map<String, String> details) {

    public ErrorResponse {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        code = code.trim();
        message = message.trim();
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    public ErrorResponse(String code, String message) {
        this(code, message, Map.of());
    }
}
