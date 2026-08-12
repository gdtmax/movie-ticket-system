package edu.nyu.cs6103.movietickets.server.exception;

/** Thrown when a client-supplied value fails a business validation rule. */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(requireMessage(message));
    }

    public ValidationException(String message, Throwable cause) {
        super(requireMessage(message), cause);
    }

    private static String requireMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Exception message must not be blank");
        }
        return message.trim();
    }
}
