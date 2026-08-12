PRAGMA foreign_keys = ON;

INSERT INTO users (id, username, password_hash, role)
VALUES
    (1, 'test-admin', 'test-hash', 'ADMIN'),
    (2, 'test-user', 'test-hash', 'USER');

INSERT INTO movies (id, title, duration_minutes, description, genre)
VALUES (1, 'Test Movie', 120, 'Movie used by automated tests.', 'Test');

INSERT INTO theaters (id, name, location)
VALUES
    (1, 'Test Hall 1', 'Test Location'),
    (2, 'Test Hall 2', 'Test Location');

INSERT INTO seats (id, theater_id, row_label, seat_number)
VALUES
    (1, 1, 'A', 1),
    (2, 1, 'A', 2),
    (3, 1, 'A', 3),
    (4, 2, 'A', 1);

INSERT INTO showtimes (id, movie_id, theater_id, start_time, price)
VALUES
    (1, 1, 1, '2030-01-01 18:00:00', 10.00),
    (2, 1, 2, '2030-01-01 18:00:00', 12.00);
