package edu.nyu.cs6103.movietickets.shared;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

public record NetworkRequest(
        String requestId,
        RequestType type,
        String token,
        JsonNode data) {

    public NetworkRequest {
        requestId = requireNonBlank(requestId, "requestId");
        type = Objects.requireNonNull(type, "type must not be null");
        token = normalizeOptional(token);
    }

    public boolean authenticated() {
        return token != null;
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
