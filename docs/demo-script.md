# Final Demonstration Script

Target length: 8-10 minutes.

## 1. Architecture (1 minute)

Show `architecture.md`. Explain JavaFX client, JSON/TCP boundary, multithreaded
server, services, DAOs, and SQLite. Emphasize that the client never accesses SQLite.

## 2. Customer flow (3 minutes)

Start server and client. Register `demo-student`, log in, open a movie, choose a
showtime, select two green seats, and confirm. Open My Bookings and show the price,
seat IDs, and CONFIRMED status. Cancel it and refresh to show CANCELLED.

## 3. Concurrency (2 minutes)

Open two clients at the same showtime. Select the same available seat in both and
submit nearly together. Show one confirmation and one `SEAT_ALREADY_BOOKED` error.
Explain transaction rollback plus the partial unique index.

## 4. Administrator flow (2 minutes)

Log in as ADMIN. Create a movie, create a small theater with two rows and three seats,
then schedule the movie. Edit the price and demonstrate cancellation. Mention that
every write is re-authorized on the server.

## 5. Evidence and close (1-2 minutes)

Run `mvn clean test`. Point to concurrency and client/server integration tests,
database constraints, protocol documentation, and user manual. Close the client and
server cleanly. State that passwords are BCrypt hashes and tokens expire server-side.
