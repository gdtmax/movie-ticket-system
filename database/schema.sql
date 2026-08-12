PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT NOT NULL COLLATE NOCASE,
    password_hash TEXT NOT NULL,
    role          TEXT NOT NULL DEFAULT 'USER'
                  CHECK (role IN ('USER', 'ADMIN')),
    created_at    TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS movies (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    title            TEXT NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
    description      TEXT NOT NULL DEFAULT '',
    genre            TEXT NOT NULL DEFAULT '',
    poster_path      TEXT,
    active           INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    created_at       TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS theaters (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT NOT NULL,
    location   TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (name, location)
);

CREATE TABLE IF NOT EXISTS seats (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    theater_id   INTEGER NOT NULL,
    row_label    TEXT NOT NULL,
    seat_number  INTEGER NOT NULL CHECK (seat_number > 0),
    is_accessible INTEGER NOT NULL DEFAULT 0 CHECK (is_accessible IN (0, 1)),
    FOREIGN KEY (theater_id) REFERENCES theaters (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    UNIQUE (theater_id, row_label, seat_number),
    UNIQUE (id, theater_id)
);

CREATE TABLE IF NOT EXISTS showtimes (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    movie_id   INTEGER NOT NULL,
    theater_id INTEGER NOT NULL,
    start_time TEXT NOT NULL,
    price      NUMERIC NOT NULL CHECK (price >= 0),
    status     TEXT NOT NULL DEFAULT 'SCHEDULED'
               CHECK (status IN ('SCHEDULED', 'CANCELLED')),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (movie_id) REFERENCES movies (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    FOREIGN KEY (theater_id) REFERENCES theaters (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    UNIQUE (theater_id, start_time),
    UNIQUE (id, theater_id)
);

CREATE TABLE IF NOT EXISTS bookings (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER NOT NULL,
    showtime_id  INTEGER NOT NULL,
    status       TEXT NOT NULL DEFAULT 'CONFIRMED'
                 CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    total_price  NUMERIC NOT NULL CHECK (total_price >= 0),
    created_at   TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TEXT,
    FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    FOREIGN KEY (showtime_id) REFERENCES showtimes (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    UNIQUE (id, showtime_id),
    CHECK (
        (status = 'CONFIRMED' AND cancelled_at IS NULL)
        OR (status = 'CANCELLED' AND cancelled_at IS NOT NULL)
    )
);

CREATE TABLE IF NOT EXISTS booking_seats (
    booking_id      INTEGER NOT NULL,
    showtime_id     INTEGER NOT NULL,
    seat_id         INTEGER NOT NULL,
    status          TEXT NOT NULL DEFAULT 'CONFIRMED'
                    CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    price_at_booking NUMERIC NOT NULL CHECK (price_at_booking >= 0),
    PRIMARY KEY (booking_id, seat_id),
    FOREIGN KEY (booking_id, showtime_id)
        REFERENCES bookings (id, showtime_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (showtime_id) REFERENCES showtimes (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    FOREIGN KEY (seat_id) REFERENCES seats (id)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

-- Only one CONFIRMED booking may own a seat for a given showtime. Cancelled
-- records remain as booking history and no longer block a new reservation.
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_showtime_seat
    ON booking_seats (showtime_id, seat_id)
    WHERE status = 'CONFIRMED';

CREATE INDEX IF NOT EXISTS idx_showtimes_movie_start
    ON showtimes (movie_id, start_time);

CREATE INDEX IF NOT EXISTS idx_bookings_user_created
    ON bookings (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_booking_seats_booking
    ON booking_seats (booking_id);

-- SQLite CHECK constraints cannot compare values in other tables. These
-- triggers guarantee that a booked seat belongs to the showtime's theater.
CREATE TRIGGER IF NOT EXISTS validate_booking_seat_theater_insert
BEFORE INSERT ON booking_seats
FOR EACH ROW
WHEN (
    SELECT theater_id FROM seats WHERE id = NEW.seat_id
) <> (
    SELECT theater_id FROM showtimes WHERE id = NEW.showtime_id
)
BEGIN
    SELECT RAISE(ABORT, 'seat does not belong to showtime theater');
END;

CREATE TRIGGER IF NOT EXISTS validate_booking_seat_theater_update
BEFORE UPDATE OF showtime_id, seat_id ON booking_seats
FOR EACH ROW
WHEN (
    SELECT theater_id FROM seats WHERE id = NEW.seat_id
) <> (
    SELECT theater_id FROM showtimes WHERE id = NEW.showtime_id
)
BEGIN
    SELECT RAISE(ABORT, 'seat does not belong to showtime theater');
END;
