# Java Online Movie Ticket System

CS6103 Summer 2026 final project: a JavaFX client and multithreaded Java server for online movie ticket reservations.

## Current status

- Step 1: Maven project skeleton and core dependencies
- Step 2: validated server, database, and socket configuration
- Step 3: SQLite schema, constraints, indexes, triggers, and seed data
- Step 4: configured JDBC connections, database initialization, and transaction management
- Step 5: immutable, validated server-side domain models
- Step 6: typed server exception hierarchy for validation, security, resources, conflicts, and database failures
- Step 7: JDBC DAO layer for users, movies, theaters, seats, showtimes, bookings, and booking seats
- Step 8: BCrypt passwords, secure tokens, authentication, authorization, and concurrent sessions
- Step 9: base movie, theater, showtime, and seat-map business services
- Step 10: atomic multi-seat booking, history, ownership checks, cancellation, and seat release
- Step 11: synchronized 50-client concurrent booking tests
- Step 12: shared DTOs and newline-delimited JSON request/response protocol
- Step 13: multithreaded TCP socket server and request router
- Step 14: persistent client Socket communication and Session
- Step 15: JavaFX application and scene-management framework
- Step 16: complete customer JavaFX workflow
- Step 17: transactional administrator services and management UI
- Step 18: integration testing, final documentation, and demonstration plan

## Demo accounts

On every server startup the schema and idempotent demo seed are applied. The seed
repairs only the known broken legacy password hash and does not overwrite registered
users or booking history.

```text
Normal user: demo / password
Administrator: admin / password
```

## Prerequisites

- JDK 22
- Apache Maven 3.9+

## Verify the project

```bash
mvn clean test
mvn clean compile
```

The application architecture and feature set are complete. See `docs/user-manual.md`
for startup and usage instructions and `docs/demo-script.md` for the final presentation.

Start both commands from the repository root (the directory containing `pom.xml`):

```bash
mvn clean test
mvn exec:java -Dexec.mainClass=edu.nyu.cs6103.movietickets.server.MovieTicketServer
```

In a second terminal:

```bash
mvn javafx:run
```

To launch the server from another working directory, set
`-Dmovie.tickets.home=/absolute/path/to/repository`. This prevents SQLite from
silently creating a second database relative to an unrelated directory.

## Configuration

Normal runtime settings are stored in `src/main/resources/application.properties`.
Automated tests load `src/test/resources/application-test.properties`, which uses
a separate database URL and network port so tests cannot modify normal runtime data.

## Database scripts

- `database/schema.sql` defines the production schema.
- `database/seed.sql` provides development sample data.
- `src/test/resources/test-schema.sql` and `test-seed.sql` provide isolated,
  deterministic test fixtures.

The database enforces foreign keys and prevents more than one confirmed booking
from owning the same seat for the same showtime. Cancelled booking records remain
available as history while their seats can be booked again.

`DatabaseManager` configures every SQLite connection with foreign keys, WAL mode,
and a busy timeout. `DatabaseInitializer` executes the schema and optional seed in
one transaction. `TransactionManager` gives later services a single shared JDBC
connection and guarantees commit or rollback for each business operation.

The server domain layer uses immutable Java records, `BigDecimal` for monetary
values, `LocalDateTime` for timestamps, and enums for roles and booking states.
Model constructors validate identifiers, required values, money precision, and
booking cancellation consistency before data reaches the DAO layer.

The exception package gives the future service and request-routing layers stable
failure categories without exposing raw SQL errors to clients. Resource and seat
conflict exceptions retain structured identifiers for consistent error responses.

DAO methods use prepared statements and map rows into immutable domain records.
Transaction-sensitive write methods accept an existing JDBC connection and never
commit or roll back independently, allowing services to compose atomic operations.

Passwords are stored only as BCrypt hashes. Session tokens use 256 bits of secure
random data, are URL-safe, expire after eight hours, and are held in a thread-safe
server-side session store. Authentication responses do not reveal whether a supplied
username exists, and administrator authorization is always enforced on the server.

Base services now expose active movies, theaters and their fixed seat layouts,
scheduled future showtimes, and per-showtime seat maps. Seat availability is derived
by combining theater seats with confirmed booking seats on one database connection.

`BookingService` treats each multi-seat reservation and cancellation as one database
transaction. Database uniqueness remains the final defense against concurrent double
booking; any seat conflict rolls back the order and every seat inserted before it.

Concurrency tests use 50 worker threads and latches to release requests at the same
instant. They verify both high-contention same-seat booking and independent-seat
throughput, then confirm the final state directly from the database.
SQLite `SQLITE_BUSY` write-upgrade conflicts are handled with bounded whole-transaction
retries and randomized backoff; uniqueness conflicts are never retried and are returned
as seat-booking conflicts.

Client and server now share typed DTOs and a common JSON codec. TCP messages use a
request ID, request type, optional session token, typed data payload, and a matching
success or error response. Full framing and error-code rules are in
`docs/network-protocol.md`.

## Server

The multithreaded TCP server accepts persistent client connections and delegates them
to a fixed-size worker pool. Each connection can send multiple newline-delimited JSON
requests. Request routing performs session checks, invokes services, maps domain models
to public DTOs, and converts known exceptions into stable error responses.

## Client networking

`SocketClient` maintains a persistent UTF-8 TCP connection, sends one JSON request
per line, verifies matching request IDs, applies timeouts, and automatically adds
the logged-in token to protected requests. `Session` stores the authenticated user
and distinguishes network disconnection from an explicit logout.

## JavaFX client framework

`MovieTicketClientApp` owns the client lifecycle, while `SceneManager` centralizes
navigation across the ten FXML views. The initial login view and shared stylesheet
establish the visual foundation shared by the completed customer and administrator
controllers.

The complete customer UI now supports registration, login, movie and showtime
browsing, live seat selection, booking confirmation, booking history, cancellation,
logout, reconnect-safe sessions, and non-blocking network operations.

Selected seats receive a two-minute server-side hold before the simulated payment
confirmation. Holds are isolated by session, expire automatically, and are released
when payment is cancelled or booking finishes. SQLite transactions and the partial
unique index remain the final protection against double booking. Payment is a demo
confirmation step; no external payment gateway or real card data is used.

Administrator accounts now have a dedicated dashboard and transactional management
screens for movies, theaters with generated seat layouts, and showtimes. Every write
request is authorized again on the server before the DAO transaction is executed.
