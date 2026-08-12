package edu.nyu.cs6103.movietickets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectSetupTest {

    @Test
    void runsOnTheConfiguredJavaVersion() {
        assertTrue(Runtime.version().feature() >= 22);
    }
}
