package edu.nyu.cs6103.movietickets.server.security;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private final PasswordHasher passwordHasher = new PasswordHasher(4);

    @Test
    void hashesAndVerifiesPasswordsWithUniqueSalts() {
        String first = passwordHasher.hash("correct-password");
        String second = passwordHasher.hash("correct-password");

        assertNotEquals("correct-password", first);
        assertNotEquals(first, second);
        assertTrue(passwordHasher.verify("correct-password", first));
        assertFalse(passwordHasher.verify("wrong-password", first));
    }

    @Test
    void rejectsInvalidInputsWithoutThrowingDuringVerification() {
        assertThrows(IllegalArgumentException.class, () -> passwordHasher.hash(" "));
        assertThrows(IllegalArgumentException.class,
                () -> passwordHasher.hash("x".repeat(73)));
        assertFalse(passwordHasher.verify(null, "hash"));
        assertFalse(passwordHasher.verify("password", "not-a-bcrypt-hash"));
    }

    @Test
    void generatesUrlSafeNonRepeatingTokens() {
        TokenGenerator generator = new TokenGenerator();
        Set<String> tokens = new HashSet<>();
        for (int index = 0; index < 100; index++) {
            String token = generator.generate();
            assertTrue(token.matches("[A-Za-z0-9_-]+"));
            assertTrue(tokens.add(token));
        }
    }
}
