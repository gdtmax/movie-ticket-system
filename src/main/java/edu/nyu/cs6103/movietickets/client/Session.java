package edu.nyu.cs6103.movietickets.client;

import edu.nyu.cs6103.movietickets.shared.dto.LoginResponse;
import java.util.Objects;
import java.util.Optional;

/** Thread-safe state for the currently authenticated client user. */
public final class Session {
    private volatile State state;

    public boolean isAuthenticated() { return state != null; }

    public boolean isAdmin() {
        State current = state;
        return current != null && "ADMIN".equalsIgnoreCase(current.role());
    }

    public Optional<State> currentUser() { return Optional.ofNullable(state); }

    public String tokenOrNull() {
        State current = state;
        return current == null ? null : current.token();
    }

    public String requireToken() {
        State current = state;
        if (current == null) throw new IllegalStateException("The client is not logged in");
        return current.token();
    }

    public void authenticate(LoginResponse response) {
        Objects.requireNonNull(response, "response must not be null");
        state = new State(response.userId(), response.username(), response.role(), response.token());
    }

    public void clear() { state = null; }

    public record State(long userId, String username, String role, String token) {
        public State {
            if (userId <= 0) throw new IllegalArgumentException("userId must be positive");
            username = requireNonBlank(username, "username");
            role = requireNonBlank(role, "role");
            token = requireNonBlank(token, "token");
        }

        private static String requireNonBlank(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return value.trim();
        }
    }
}
