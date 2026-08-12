package edu.nyu.cs6103.movietickets.server.exception;

/** Thrown when an authenticated user is not permitted to perform an action. */
public class AuthorizationException extends RuntimeException {

    public AuthorizationException(String message) {
        super(requireMessage(message));
    }

    public AuthorizationException(String message, Throwable cause) {
        super(requireMessage(message), cause);
    }

    private static String requireMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Exception message must not be blank");
        }
        return message.trim();
    }
}
