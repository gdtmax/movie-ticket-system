# Network Protocol

## Transport and framing

The client and server communicate over a persistent TCP socket using UTF-8.
Each complete line contains exactly one JSON request or response. A sender must
terminate every message with `\n`; embedded text line breaks are JSON-escaped.

```text
one UTF-8 JSON object + \n = one network message
```

The maximum accepted line length will be enforced by the socket layer. Clients
must wait for the response with the matching `requestId` before treating a request
as complete.

## Request envelope

```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "CREATE_BOOKING",
  "token": "authenticated-session-token",
  "data": {
    "showtimeId": 1,
    "seatIds": [1, 2]
  }
}
```

- `requestId` is required and unique per request.
- `type` is a value from `RequestType`.
- `token` is null only for `REGISTER` and `LOGIN`.
- `data` is the DTO associated with the request type.
- The server determines user identity and role from `token`; user or role values
  supplied inside `data` never grant authorization.

## Successful response

```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "SUCCESS",
  "data": {
    "booking": {
      "id": 25,
      "userId": 2,
      "showtimeId": 1,
      "status": "CONFIRMED",
      "totalPrice": 30.00,
      "createdAt": "2026-08-06T16:30:00",
      "cancelledAt": null,
      "seatIds": [1, 2]
    },
    "bookings": []
  },
  "error": null
}
```

## Error response

```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ERROR",
  "data": null,
  "error": {
    "code": "SEAT_ALREADY_BOOKED",
    "message": "The selected seat is no longer available.",
    "details": {
      "showtimeId": "1",
      "seatId": "2"
    }
  }
}
```

Planned stable error codes:

| Code | Meaning |
|---|---|
| `INVALID_REQUEST` | JSON or request data is invalid |
| `AUTHENTICATION_REQUIRED` | Token is absent, invalid, or expired |
| `AUTHORIZATION_DENIED` | User lacks permission |
| `RESOURCE_NOT_FOUND` | Requested entity does not exist |
| `SEAT_ALREADY_BOOKED` | Another confirmed booking owns the seat |
| `DATABASE_ERROR` | A database operation failed |
| `INTERNAL_ERROR` | An unexpected server failure occurred |

Server logs may retain technical causes and SQL errors. Error responses sent to
clients must not contain password hashes, SQL statements, stack traces, database
paths, or other internal details.

## Request data mapping

| Request type | Data DTO | Authentication |
|---|---|---|
| `REGISTER` | `RegisterRequest` | No |
| `LOGIN` | `LoginRequest` | No |
| `LOGOUT` | `LogoutRequest` | Yes |
| `GET_MOVIES` | none | Yes |
| `GET_MOVIE` | `MovieRequest` | Yes |
| `GET_THEATERS` | none | Yes |
| `GET_THEATER` | `TheaterRequest` | Yes |
| `GET_SHOWTIMES` | `ShowtimeRequest` | Yes |
| `GET_SEAT_MAP` | `SeatMapRequest` | Yes |
| `LOCK_SEATS` | `SeatLockRequest` | Yes |
| `RELEASE_SEATS` | `SeatLockRequest` | Yes |
| `CREATE_BOOKING` | `CreateBookingRequest` | Yes |
| `CANCEL_BOOKING` | `CancelBookingRequest` | Yes |
| `GET_BOOKING_HISTORY` | `BookingHistoryRequest` | Yes |

Admin request types always require a valid administrator session. Their detailed
management DTOs are `AdminMovieRequest`, `AdminTheaterRequest`, and
`AdminShowtimeRequest`. Create requests use ID zero; update requests require a
positive existing ID. The server always verifies the token has the ADMIN role.
