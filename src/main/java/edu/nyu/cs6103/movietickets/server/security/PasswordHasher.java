package edu.nyu.cs6103.movietickets.server.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

import java.nio.charset.StandardCharsets;

/** Creates and verifies BCrypt password hashes. */
public final class PasswordHasher {

    public static final int DEFAULT_COST = 12;
    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

    private final int cost;

    public PasswordHasher() {
        this(DEFAULT_COST);
    }

    public PasswordHasher(int cost) {
        if (cost < 4 || cost > 16) {
            throw new IllegalArgumentException("BCrypt cost must be between 4 and 16");
        }
        this.cost = cost;
    }

    public String hash(String plainPassword) {
        validatePassword(plainPassword);
        return BCrypt.withDefaults().hashToString(cost, plainPassword.toCharArray());
    }

    public boolean verify(String plainPassword, String passwordHash) {
        if (plainPassword == null || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        if (plainPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            return false;
        }
        try {
            return BCrypt.verifyer()
                    .verify(plainPassword.toCharArray(), passwordHash.trim())
                    .verified;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public int cost() {
        return cost;
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes");
        }
    }
}
