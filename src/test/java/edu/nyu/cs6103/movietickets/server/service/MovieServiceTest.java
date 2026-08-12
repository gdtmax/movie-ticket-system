package edu.nyu.cs6103.movietickets.server.service;

import edu.nyu.cs6103.movietickets.server.dao.MovieDao;
import edu.nyu.cs6103.movietickets.server.db.DatabaseInitializer;
import edu.nyu.cs6103.movietickets.server.db.DatabaseManager;
import edu.nyu.cs6103.movietickets.server.exception.ResourceNotFoundException;
import edu.nyu.cs6103.movietickets.server.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MovieServiceTest {

    @TempDir
    Path tempDirectory;

    private MovieService movieService;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager manager = new DatabaseManager(
                "jdbc:sqlite:" + tempDirectory.resolve("movies.db"));
        new DatabaseInitializer(manager).initialize(
                testResource("test-schema.sql"), testResource("test-seed.sql"));
        movieService = new MovieService(manager, new MovieDao());
    }

    @Test
    void listsAndLoadsAvailableMovies() {
        assertEquals(1, movieService.getAvailableMovies().size());
        assertEquals("Test Movie", movieService.getMovie(1).title());
    }

    @Test
    void distinguishesInvalidAndMissingMovieIdentifiers() {
        assertThrows(ValidationException.class, () -> movieService.getMovie(0));
        assertThrows(ResourceNotFoundException.class, () -> movieService.getMovie(999));
    }

    private static Path testResource(String name) throws Exception {
        return Path.of(MovieServiceTest.class.getClassLoader().getResource(name).toURI());
    }
}
