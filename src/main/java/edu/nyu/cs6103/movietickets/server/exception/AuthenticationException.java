package edu.nyu.cs6103.movietickets.server.exception;

/** Thrown when credentials or a session token cannot authenticate a user. */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(requireMessage(message));
    }

    public AuthenticationException(String message, Throwable cause) {
        super(requireMessage(message), cause);
    }

    private static String requireMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Exception message must not be blank");
        }
        return message.trim();
    }
}
