# Database Design

## Tables

| Table | Purpose | Important relationships |
|---|---|---|
| `users` | Accounts and USER/ADMIN role | Parent of bookings |
| `movies` | Movie catalog and active flag | Parent of showtimes |
| `theaters` | Physical halls | Parent of seats and showtimes |
| `seats` | Fixed row/number layout | Belongs to one theater |
| `showtimes` | Movie, theater, time, price | Parent of bookings |
| `bookings` | Order header and status | Belongs to user/showtime |
| `booking_seats` | Seats and captured price | Joins booking, showtime, seat |

## Integrity rules

- Usernames are unique without case sensitivity.
- Theater name/location and theater/time combinations are unique.
- Seat row/number is unique inside a theater.
- Money is nonnegative and booking cancellation timestamps match status.
- Foreign keys use restrictive deletion to preserve history.
- A partial unique index allows only one CONFIRMED owner of a seat/showtime.
- Triggers ensure a selected seat belongs to the showtime's theater.

## Lifecycle

`DatabaseInitializer` applies `database/schema.sql` and optional seed data in one
transaction. Every connection enables foreign keys, WAL, and a busy timeout.
Cancelled bookings remain for history while cancelled booking-seat rows release the
unique active-seat constraint. Tests use isolated scripts in `src/test/resources`.
