# Test Plan

## Automated coverage

| Area | Representative test |
|---|---|
| Configuration | valid parsing and rejected invalid values |
| Database | initialization, pragmas, commit, rollback |
| Models/exceptions | invariants and structured error information |
| Security | BCrypt, login, token lifecycle, role authorization |
| Services | movie queries, booking, cancellation, ownership |
| JSON protocol | round trips, Java time, precise money, errors |
| Concurrency | 50 simultaneous same-seat and independent-seat requests |
| Integration | real server socket, client, registration, login, query, logout |
| Administration | transactional catalog writes, seat generation, role denial |

Run `mvn clean test`. Tests must use `application-test.properties` and temporary or
test-only database files. A passing release must have zero failed tests and no live
server threads after integration tests.

## Manual acceptance

1. Start server and two clients.
2. Register/login, browse movies, choose showtime and seats, confirm history.
3. Attempt the same seat from both clients; exactly one succeeds.
4. Cancel the winner and verify the seat becomes available.
5. Log in as admin; create/update a movie, theater, and showtime.
6. Verify a normal user receives authorization denial for admin requests.
7. Disconnect/reconnect the client and confirm the active session can continue.

## Release criteria

No password hashes or SQL stack traces appear in protocol responses. All monetary
values retain two-decimal accuracy. Database constraints stay enabled. UI network
operations remain off the JavaFX application thread.

Final verification on August 12, 2026: all 45 JUnit tests passed with zero skipped
or aborted tests. Separate protocol verification confirmed that a USER token receives
`AUTHORIZATION_DENIED` while an ADMIN token can manage every catalog resource.
