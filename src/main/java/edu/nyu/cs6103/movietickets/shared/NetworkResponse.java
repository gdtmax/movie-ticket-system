package edu.nyu.cs6103.movietickets.shared;

import com.fasterxml.jackson.databind.JsonNode;
import edu.nyu.cs6103.movietickets.shared.dto.ErrorResponse;

import java.util.Objects;

public record NetworkResponse(
        String requestId,
        ResponseStatus status,
        JsonNode data,
        ErrorResponse error) {

    public NetworkResponse {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }
        requestId = requestId.trim();
        status = Objects.requireNonNull(status, "status must not be null");
        if (status == ResponseStatus.SUCCESS && error != null) {
            throw new IllegalArgumentException("A successful response cannot contain an error");
        }
        if (status == ResponseStatus.ERROR && error == null) {
            throw new IllegalArgumentException("An error response must contain an error");
        }
    }

    public boolean successful() {
        return status == ResponseStatus.SUCCESS;
    }
}
