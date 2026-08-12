package edu.nyu.cs6103.movietickets.server.concurrency;

import edu.nyu.cs6103.movietickets.server.exception.SeatAlreadyBookedException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** In-memory short-lived seat holds; the database remains the final booking authority. */
public final class SeatLockManager {
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(2);

    private final Duration ttl;
    private final Clock clock;
    private final Map<SeatKey, Hold> holds = new HashMap<>();

    public SeatLockManager() { this(DEFAULT_TTL, Clock.systemUTC()); }

    public SeatLockManager(Duration ttl, Clock clock) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Seat lock TTL must be positive");
        }
        this.ttl = ttl;
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public synchronized Instant lock(String owner, long showtimeId, List<Long> seatIds) {
        validate(owner, showtimeId, seatIds);
        purgeExpired();
        for (long seatId : seatIds) {
            Hold existing = holds.get(new SeatKey(showtimeId, seatId));
            if (existing != null && !existing.owner().equals(owner)) {
                throw new SeatAlreadyBookedException(showtimeId, seatId);
            }
        }
        Instant expiresAt = clock.instant().plus(ttl);
        for (long seatId : seatIds) {
            holds.put(new SeatKey(showtimeId, seatId), new Hold(owner, expiresAt));
        }
        return expiresAt;
    }

    public synchronized void assertNotLockedByOther(
            String owner, long showtimeId, List<Long> seatIds) {
        validate(owner, showtimeId, seatIds);
        purgeExpired();
        for (long seatId : seatIds) {
            Hold existing = holds.get(new SeatKey(showtimeId, seatId));
            if (existing != null && !existing.owner().equals(owner)) {
                throw new SeatAlreadyBookedException(showtimeId, seatId);
            }
        }
    }

    public synchronized boolean isLockedByOther(String owner, long showtimeId, long seatId) {
        purgeExpired();
        Hold hold = holds.get(new SeatKey(showtimeId, seatId));
        return hold != null && !hold.owner().equals(owner);
    }

    public synchronized void release(String owner, long showtimeId, List<Long> seatIds) {
        if (owner == null || owner.isBlank() || seatIds == null) return;
        for (long seatId : seatIds) {
            SeatKey key = new SeatKey(showtimeId, seatId);
            Hold hold = holds.get(key);
            if (hold != null && hold.owner().equals(owner)) holds.remove(key);
        }
    }

    private void purgeExpired() {
        Instant now = clock.instant();
        holds.values().removeIf(hold -> !now.isBefore(hold.expiresAt()));
    }

    private static void validate(String owner, long showtimeId, List<Long> seatIds) {
        if (owner == null || owner.isBlank()) throw new IllegalArgumentException("Lock owner is required");
        if (showtimeId <= 0) throw new IllegalArgumentException("showtimeId must be positive");
        if (seatIds == null || seatIds.isEmpty() || seatIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("At least one valid seat is required");
        }
    }

    private record SeatKey(long showtimeId, long seatId) { }
    private record Hold(String owner, Instant expiresAt) { }
}
