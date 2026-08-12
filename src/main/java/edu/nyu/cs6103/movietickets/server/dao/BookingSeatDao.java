package edu.nyu.cs6103.movietickets.server.dao;

import edu.nyu.cs6103.movietickets.server.exception.DatabaseOperationException;
import edu.nyu.cs6103.movietickets.server.model.BookingSeat;
import edu.nyu.cs6103.movietickets.server.model.BookingStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class BookingSeatDao {

    public BookingSeat insert(Connection connection, long bookingId, long showtimeId,
                              long seatId, BigDecimal priceAtBooking) {
        String sql = """
                INSERT INTO booking_seats
                    (booking_id, showtime_id, seat_id, status, price_at_booking)
                VALUES (?, ?, ?, 'CONFIRMED', ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bookingId);
            statement.setLong(2, showtimeId);
            statement.setLong(3, seatId);
            statement.setBigDecimal(4, priceAtBooking);
            statement.executeUpdate();
            return new BookingSeat(
                    bookingId, showtimeId, seatId,
                    BookingStatus.CONFIRMED, priceAtBooking);
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to add seat to booking", exception);
        }
    }

    public List<BookingSeat> findByBookingId(Connection connection, long bookingId) {
        return findMany(connection,
                "SELECT * FROM booking_seats WHERE booking_id=? ORDER BY seat_id", bookingId,
                "Unable to list booking seats");
    }

    public List<BookingSeat> findConfirmedByShowtimeId(Connection connection, long showtimeId) {
        return findMany(connection,
                """
                SELECT * FROM booking_seats
                WHERE showtime_id=? AND status='CONFIRMED'
                ORDER BY seat_id
                """,
                showtimeId, "Unable to list confirmed showtime seats");
    }

    public boolean isSeatBooked(Connection connection, long showtimeId, long seatId) {
        String sql = """
                SELECT 1 FROM booking_seats
                WHERE showtime_id=? AND seat_id=? AND status='CONFIRMED'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, showtimeId);
            statement.setLong(2, seatId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to check seat availability", exception);
        }
    }

    public int cancelByBookingId(Connection connection, long bookingId) {
        String sql = """
                UPDATE booking_seats SET status='CANCELLED'
                WHERE booking_id=? AND status='CONFIRMED'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bookingId);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseOperationException("Unable to release booking seats", exception);
        }
    }

    private static List<BookingSeat> findMany(
            Connection connection, String sql, long id, String errorMessage) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                List<BookingSeat> seats = new ArrayList<>();
                while (result.next()) {
                    seats.add(map(result));
                }
                return List.copyOf(seats);
            }
        } catch (SQLException exception) {
            throw new DatabaseOperationException(errorMessage, exception);
        }
    }

    private static BookingSeat map(ResultSet result) throws SQLException {
        return new BookingSeat(
                result.getLong("booking_id"), result.getLong("showtime_id"),
                result.getLong("seat_id"),
                BookingStatus.fromDatabaseValue(result.getString("status")),
                result.getBigDecimal("price_at_booking"));
    }
}
