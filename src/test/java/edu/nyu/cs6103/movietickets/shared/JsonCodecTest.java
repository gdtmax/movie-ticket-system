package edu.nyu.cs6103.movietickets.shared;

import edu.nyu.cs6103.movietickets.shared.dto.BookingDto;
import edu.nyu.cs6103.movietickets.shared.dto.BookingResponse;
import edu.nyu.cs6103.movietickets.shared.dto.ErrorResponse;
import edu.nyu.cs6103.movietickets.shared.dto.LoginRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonCodecTest {

    private final JsonCodec codec = new JsonCodec();

    @Test
    void roundTripsATypedLoginRequestOnOneLine() {
        NetworkRequest original = codec.request(
                RequestType.LOGIN, null, new LoginRequest("alice", "password-123"));
        String json = codec.encode(original);
        NetworkRequest decoded = codec.decodeRequest(json);
        LoginRequest login = codec.requestDataAs(decoded, LoginRequest.class);

        assertFalse(json.contains("\n"));
        assertEquals(original.requestId(), decoded.requestId());
        assertEquals(RequestType.LOGIN, decoded.type());
        assertEquals("alice", login.username());
        assertEquals("password-123", login.password());
    }

    @Test
    void preservesMoneyAndTimeInBookingResponses() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 6, 16, 30);
        BookingDto booking = new BookingDto(
                25, 2, 1, "CONFIRMED", new BigDecimal("30.00"),
                createdAt, null, List.of(1L, 2L));
        NetworkResponse original = codec.success(
                "request-1", BookingResponse.single(booking));

        NetworkResponse decoded = codec.decodeResponse(codec.encode(original));
        BookingResponse response = codec.responseDataAs(decoded, BookingResponse.class);

        assertTrue(decoded.successful());
        assertEquals(new BigDecimal("30.00"), response.booking().totalPrice());
        assertEquals(createdAt, response.booking().createdAt());
        assertEquals(List.of(1L, 2L), response.booking().seatIds());
    }

    @Test
    void roundTripsStructuredErrors() {
        ErrorResponse error = new ErrorResponse(
                "SEAT_ALREADY_BOOKED", "Seat is no longer available",
                Map.of("showtimeId", "1", "seatId", "2"));
        NetworkResponse decoded = codec.decodeResponse(
                codec.encode(codec.error("request-2", error)));

        assertEquals(ResponseStatus.ERROR, decoded.status());
        assertEquals("2", decoded.error().details().get("seatId"));
    }

    @Test
    void rejectsMissingDataAndInconsistentResponseState() {
        NetworkRequest request = codec.request(RequestType.LOGOUT, "token", null);
        assertThrows(IllegalArgumentException.class,
                () -> codec.requestDataAs(request, LoginRequest.class));
        assertThrows(IllegalArgumentException.class,
                () -> new NetworkResponse(
                        "request", ResponseStatus.ERROR, null, null));
    }
}
