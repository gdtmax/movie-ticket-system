package edu.nyu.cs6103.movietickets.server.service;
import edu.nyu.cs6103.movietickets.server.config.ServerConfig; import edu.nyu.cs6103.movietickets.server.dao.*; import edu.nyu.cs6103.movietickets.server.db.*; import edu.nyu.cs6103.movietickets.shared.dto.*;
import org.junit.jupiter.api.Test; import org.junit.jupiter.api.io.TempDir; import java.math.BigDecimal; import java.nio.file.Path; import java.time.LocalDateTime; import static org.junit.jupiter.api.Assertions.*;
class AdminServiceTest {
 @TempDir Path temp;
 @Test void createsAndUpdatesCatalogResourcesTransactionally() throws Exception {
  ServerConfig cfg=new ServerConfig("localhost",5051,4,"jdbc:sqlite:"+temp.resolve("admin.db"),1000,5000);DatabaseManager db=new DatabaseManager(cfg);new DatabaseInitializer(db).initialize(Path.of("src/test/resources/test-schema.sql"),null);TransactionManager tx=new TransactionManager(db);MovieDao movies=new MovieDao();TheaterDao theaters=new TheaterDao();SeatDao seats=new SeatDao();ShowtimeDao shows=new ShowtimeDao();AdminService service=new AdminService(tx,movies,theaters,seats,shows);
  var movie=service.createMovie(new AdminMovieRequest(0,"New Movie",100,"Description","Drama",null,true));var theater=service.createTheater(new AdminTheaterRequest(0,"New Hall","NYU",2,3));try(var c=db.getConnection()){assertEquals(6,seats.findByTheaterId(c,theater.id()).size());}
  var show=service.createShowtime(new AdminShowtimeRequest(0,movie.id(),theater.id(),LocalDateTime.of(2030,2,1,19,0),new BigDecimal("18.50"),"SCHEDULED"));var updated=service.updateShowtime(new AdminShowtimeRequest(show.id(),movie.id(),theater.id(),show.startTime(),new BigDecimal("20.00"),"CANCELLED"));assertEquals("CANCELLED",updated.status().toDatabaseValue());assertEquals(new BigDecimal("20.00"),updated.price());
 }
}
