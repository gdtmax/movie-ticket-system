# Concurrency Design

## Server threads

`MovieTicketServer` uses a fixed worker pool. Each accepted socket gets one
`ClientHandler`; a handler may process multiple sequential requests. `SocketClient`
serializes exchanges on one connection, while different clients run concurrently.

## Booking correctness

The in-memory `SeatLockManager` atomically coordinates requests targeting the same
showtime/seat. Holds are owned by a session token, expire after two minutes, and are
released after payment cancellation or booking completion. The database partial
unique index is the final source of truth, so correctness remains intact across
threads and after a server restart. A booking header and all selected seats commit
atomically or roll back together.

SQLite can report `SQLITE_BUSY` during concurrent write upgrades. `BookingService`
retries the whole transaction with bounded randomized backoff. Unique seat conflicts
are not retried; they become `SEAT_ALREADY_BOOKED` responses.

## Shared state and shutdown

Server sessions use concurrent collections. Client session state is published with a
volatile immutable record. Server shutdown closes the listener, stops acceptance,
waits for workers, then interrupts remaining workers after a bounded timeout.
