# System Architecture

## Overview

The system is a desktop client/server application. JavaFX clients never access the
database directly. They exchange newline-delimited JSON over persistent TCP sockets
with a multithreaded Java server, which owns authentication, authorization, business
rules, transactions, and SQLite access.

```text
JavaFX FXML + Controllers
          |
SocketClient + Session
          |  TCP / UTF-8 JSON
MovieTicketServer -> ClientHandler -> RequestRouter
          |
Authentication / Movie / Theater / Showtime / Seat / Booking / Admin Services
          |
DAO + TransactionManager
          |
SQLite
```

## Responsibilities

- `shared`: protocol envelopes, request types, statuses, DTOs, and JSON codec.
- `client`: application lifecycle, scene navigation, session, socket transport.
- `client.controller`: UI events and asynchronous server requests.
- `server`: socket acceptance, connection workers, and request routing.
- `server.service`: validation, authorization, and transactional use cases.
- `server.dao`: prepared SQL and domain-object mapping.
- `server.model`: immutable domain records and enums.

The dependency direction is UI -> protocol -> server -> service -> DAO. Public DTOs
never contain password hashes or JDBC objects. The server is independently runnable
from `MovieTicketServer`; the client starts from `MovieTicketClientApp`.

## Main flows

Login returns an opaque session token. `SocketClient` automatically attaches it to
protected requests. Booking performs all seat inserts in one transaction. Admin
requests are authorized by `requireAdmin` on the server, regardless of the UI shown.

## Technology

JDK 22, Maven, JavaFX 22, SQLite JDBC, Jackson, BCrypt, and JUnit 5.
