package edu.nyu.cs6103.movietickets.server.exception;

/** Thrown when a confirmed booking already owns a requested showtime seat. */
public class SeatAlreadyBookedException extends RuntimeException {

    private final long showtimeId;
    private final long seatId;

    public SeatAlreadyBookedException(long showtimeId, long seatId) {
        super(buildMessage(showtimeId, seatId));
        if (showtimeId <= 0) {
            throw new IllegalArgumentException("showtimeId must be greater than zero");
        }
        if (seatId <= 0) {
            throw new IllegalArgumentException("seatId must be greater than zero");
        }
        this.showtimeId = showtimeId;
        this.seatId = seatId;
    }

    public long showtimeId() {
        return showtimeId;
    }

    public long seatId() {
        return seatId;
    }

    private static String buildMessage(long showtimeId, long seatId) {
        if (showtimeId <= 0 || seatId <= 0) {
            throw new IllegalArgumentException("showtimeId and seatId must be greater than zero");
        }
        return "Seat " + seatId + " is already booked for showtime " + showtimeId;
    }
}
