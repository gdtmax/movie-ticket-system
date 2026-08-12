package edu.nyu.cs6103.movietickets.client;

import edu.nyu.cs6103.movietickets.shared.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SessionTest {
    @Test void authenticateAndClearSession() {
        Session session = new Session();
        assertFalse(session.isAuthenticated());
        assertThrows(IllegalStateException.class, session::requireToken);
        session.authenticate(new LoginResponse(7, "alice", "ADMIN", "token-123"));
        assertTrue(session.isAuthenticated());
        assertTrue(session.isAdmin());
        assertEquals("token-123", session.requireToken());
        assertEquals("alice", session.currentUser().orElseThrow().username());
        session.clear();
        assertFalse(session.isAuthenticated());
    }
}
