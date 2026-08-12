package edu.nyu.cs6103.movietickets.server.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/** Generates URL-safe session tokens using a cryptographically secure source. */
public final class TokenGenerator {

    public static final int DEFAULT_TOKEN_BYTES = 32;

    private final SecureRandom secureRandom;
    private final int tokenBytes;

    public TokenGenerator() {
        this(new SecureRandom(), DEFAULT_TOKEN_BYTES);
    }

    TokenGenerator(SecureRandom secureRandom, int tokenBytes) {
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom must not be null");
        if (tokenBytes < 16) {
            throw new IllegalArgumentException("tokenBytes must be at least 16");
        }
        this.tokenBytes = tokenBytes;
    }

    public String generate() {
        byte[] bytes = new byte[tokenBytes];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
