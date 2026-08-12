PRAGMA foreign_keys = ON;

-- Development-only accounts. Both use the password: password
-- This BCrypt hash is generated and verified by PasswordHasher (favre BCrypt).
INSERT OR IGNORE INTO users (username, password_hash, role)
VALUES
    ('admin', '$2a$12$9Oiwgihl3dvg7Lb9lb8Q6uAdE9xSwL8EWuxOYHeqAbFbcH0G.uYJa', 'ADMIN'),
    ('demo',  '$2a$12$9Oiwgihl3dvg7Lb9lb8Q6uAdE9xSwL8EWuxOYHeqAbFbcH0G.uYJa', 'USER');

-- Repair databases created by older project versions, but never overwrite a
-- password that no longer equals the known broken legacy seed hash.
UPDATE users
SET password_hash = '$2a$12$9Oiwgihl3dvg7Lb9lb8Q6uAdE9xSwL8EWuxOYHeqAbFbcH0G.uYJa'
WHERE username IN ('admin', 'demo') COLLATE NOCASE
  AND password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';

UPDATE users SET role = 'ADMIN' WHERE username = 'admin' COLLATE NOCASE;
UPDATE users SET role = 'USER' WHERE username = 'demo' COLLATE NOCASE;

INSERT OR IGNORE INTO movies
    (id, title, duration_minutes, description, genre, poster_path)
VALUES
    (1, 'Interstellar', 169, 'Explorers travel through a wormhole in space.', 'Science Fiction', 'images/interstellar.jpg'),
    (2, 'Spirited Away', 125, 'A young girl enters a world ruled by spirits.', 'Animation', 'images/spirited-away.jpg'),
    (3, 'The Grand Budapest Hotel', 100, 'A concierge and lobby boy become involved in a mystery.', 'Comedy', 'images/grand-budapest-hotel.jpg');

INSERT OR IGNORE INTO theaters (id, name, location)
VALUES
    (1, 'Cinema Hall 1', 'Main Building - First Floor'),
    (2, 'Cinema Hall 2', 'Main Building - Second Floor');

INSERT OR IGNORE INTO seats (id, theater_id, row_label, seat_number, is_accessible)
WITH RECURSIVE
    rows(row_number, row_label) AS (
        VALUES (1, 'A')
        UNION ALL
        SELECT row_number + 1, char(64 + row_number + 1)
        FROM rows
        WHERE row_number < 5
    ),
    numbers(seat_number) AS (
        VALUES (1)
        UNION ALL
        SELECT seat_number + 1
        FROM numbers
        WHERE seat_number < 8
    )
SELECT
    ((theater.id - 1) * 40) + ((rows.row_number - 1) * 8) + numbers.seat_number,
    theater.id,
    rows.row_label,
    numbers.seat_number,
    CASE WHEN rows.row_label = 'A' AND numbers.seat_number IN (1, 8) THEN 1 ELSE 0 END
FROM theaters AS theater
CROSS JOIN rows
CROSS JOIN numbers
WHERE theater.id IN (1, 2);

-- Add rolling demo showtimes. The (theater_id, start_time) unique constraint
-- makes this idempotent while allowing a long-lived database to receive new
-- future dates after the old demo showtimes have passed.
INSERT OR IGNORE INTO showtimes
    (movie_id, theater_id, start_time, price)
VALUES
    (1, 1, datetime('now', '+1 day', 'start of day', '+18 hours'), 15.00),
    (2, 2, datetime('now', '+1 day', 'start of day', '+19 hours'), 12.50),
    (3, 1, datetime('now', '+2 days', 'start of day', '+16 hours'), 13.00),
    (1, 2, datetime('now', '+2 days', 'start of day', '+20 hours'), 15.00);
