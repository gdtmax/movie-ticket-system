# User Manual

## Start the application

Requirements: JDK 22 and Maven 3.9+. From the project directory, open two terminals.

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass=edu.nyu.cs6103.movietickets.server.MovieTicketServer
```

In the second terminal:

```bash
mvn javafx:run
```

Server settings and database path are in `src/main/resources/application.properties`.
Development seed accounts are `admin` / `password` and `demo` / `password`.
They are demonstration data only and must not be used in a deployed system.

## Customer workflow

Create an account or sign in. Select a movie, then a showtime. Green seats are
available, gray seats are booked, and orange seats are your current selection.
Confirm the booking to see it in **My Bookings**. Select a confirmed booking and
choose **Cancel Selected Booking** to release its seats. Use **Logout** to invalidate
the token; merely closing the network connection does not log the account out.

Clicking **Continue to Payment** first holds the selected seats for two minutes.
The payment dialog is a classroom-demo confirmation and never collects real card
details. Cancelling the dialog releases the hold; confirming creates the booking in
one database transaction.

## Administrator workflow

An ADMIN account opens the dashboard after login. Movie Management creates or edits
catalog records and availability. Theater Management creates a hall and its fixed
row/seat layout; existing layouts are intentionally not resized. Showtime Management
selects a movie/theater, date in `yyyy-MM-dd HH:mm`, price, and status.

## Troubleshooting

- Connection error: start the server and confirm host/port match the properties;
  the client remains open and the same action can be retried.
- Seat no longer available: another client booked it; the view refreshes.
- Authentication required: sign in again because the token is invalid or expired.
- Database locked: wait briefly; bounded server retries handle normal contention.
