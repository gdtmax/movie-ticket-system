package edu.nyu.cs6103.movietickets.server.concurrency;

import edu.nyu.cs6103.movietickets.server.exception.SeatAlreadyBookedException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SeatLockManagerTest {
    @Test void locksAtomicallyReleasesAndExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2030-01-01T00:00:00Z"));
        SeatLockManager manager = new SeatLockManager(Duration.ofSeconds(30), clock);

        Instant expires = manager.lock("session-a", 7, List.of(1L, 2L));
        assertEquals(clock.instant().plusSeconds(30), expires);
        assertFalse(manager.isLockedByOther("session-a", 7, 1));
        assertTrue(manager.isLockedByOther("session-b", 7, 1));
        assertThrows(SeatAlreadyBookedException.class,
                () -> manager.lock("session-b", 7, List.of(1L, 3L)));

        manager.release("session-a", 7, List.of(1L));
        assertFalse(manager.isLockedByOther("session-b", 7, 1));
        clock.advance(Duration.ofSeconds(31));
        assertFalse(manager.isLockedByOther("session-b", 7, 2));
        assertDoesNotThrow(() -> manager.lock("session-b", 7, List.of(2L)));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        MutableClock(Instant instant) { this.instant = instant; }
        void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
