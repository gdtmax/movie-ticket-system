package edu.nyu.cs6103.movietickets.client.controller;
import edu.nyu.cs6103.movietickets.client.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
public final class AdminDashboardController extends AbstractClientController {
 @FXML private Label adminLabel;
 @Override protected void onShow(Object p) { adminLabel.setText("Administrator: " + session.currentUser().orElseThrow().username()); }
 @FXML private void movies(){scenes.show(SceneManager.View.MOVIE_MANAGEMENT);}
 @FXML private void theaters(){scenes.show(SceneManager.View.THEATER_MANAGEMENT);}
 @FXML private void showtimes(){scenes.show(SceneManager.View.SHOWTIME_MANAGEMENT);}
 @FXML private void logout(){runNetwork(client::logout,r->{if(requireSuccess(r))scenes.showLogin();},null);}
}
