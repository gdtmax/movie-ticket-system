package edu.nyu.cs6103.movietickets.server.exception;

/**
 * Wraps a low-level database failure before it leaves the DAO layer.
 * Client responses must not expose the wrapped cause or SQL details.
 */
public class DatabaseOperationException extends RuntimeException {

    public DatabaseOperationException(String message) {
        super(requireMessage(message));
    }

    public DatabaseOperationException(String message, Throwable cause) {
        super(requireMessage(message), cause);
    }

    private static String requireMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Exception message must not be blank");
        }
        return message.trim();
    }
}
