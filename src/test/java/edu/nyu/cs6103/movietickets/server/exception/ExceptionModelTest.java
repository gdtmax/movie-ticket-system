package edu.nyu.cs6103.movietickets.server.exception;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExceptionModelTest {

    @Test
    void resourceNotFoundCarriesStructuredContext() {
        ResourceNotFoundException exception =
                new ResourceNotFoundException("Showtime", 42L);

        assertEquals("Showtime", exception.resourceType());
        assertEquals(42L, exception.resourceId());
        assertEquals("Showtime not found: 42", exception.getMessage());
    }

    @Test
    void seatConflictCarriesShowtimeAndSeatIdentifiers() {
        SeatAlreadyBookedException exception =
                new SeatAlreadyBookedException(10, 25);

        assertEquals(10, exception.showtimeId());
        assertEquals(25, exception.seatId());
        assertTrue(exception.getMessage().contains("25"));
        assertTrue(exception.getMessage().contains("10"));
    }

    @Test
    void databaseExceptionPreservesTheOriginalCause() {
        SQLException cause = new SQLException("private SQL details");
        DatabaseOperationException exception =
                new DatabaseOperationException("Unable to create booking", cause);

        assertSame(cause, exception.getCause());
        assertEquals("Unable to create booking", exception.getMessage());
    }

    @Test
    void exceptionMessagesMustNotBeBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new ValidationException("  "));
        assertThrows(IllegalArgumentException.class,
                () -> new AuthenticationException(null));
        assertThrows(IllegalArgumentException.class,
                () -> new AuthorizationException(""));
        assertThrows(IllegalArgumentException.class,
                () -> new DatabaseOperationException(" "));
    }

    @Test
    void structuredExceptionsRejectInvalidContext() {
        assertThrows(IllegalArgumentException.class,
                () -> new ResourceNotFoundException("", 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ResourceNotFoundException("Movie", null));
        assertThrows(IllegalArgumentException.class,
                () -> new SeatAlreadyBookedException(0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new SeatAlreadyBookedException(1, -1));
    }
}
