package edu.nyu.cs6103.movietickets.client.controller;
import edu.nyu.cs6103.movietickets.client.SceneManager;
import edu.nyu.cs6103.movietickets.shared.RequestType;
import edu.nyu.cs6103.movietickets.shared.dto.*;
import javafx.fxml.FXML; import javafx.scene.control.*;
public final class MovieManagementController extends AbstractClientController {
 @FXML private ListView<MovieDto> list; @FXML private TextField titleField,durationField,genreField,posterField;
 @FXML private TextArea descriptionField; @FXML private CheckBox activeField; @FXML private Label statusLabel;
 private MovieDto selected;
 @Override protected void onShow(Object p){
  list.setCellFactory(v->new ListCell<>(){protected void updateItem(MovieDto m,boolean e){super.updateItem(m,e);setText(e||m==null?null:m.title()+"  ["+(m.active()?"ACTIVE":"INACTIVE")+"]");}});
  list.getSelectionModel().selectedItemProperty().addListener((o,a,m)->populate(m)); refresh();
 }
 private void populate(MovieDto m){selected=m;if(m==null)return;titleField.setText(m.title());durationField.setText(""+m.durationMinutes());genreField.setText(m.genre());descriptionField.setText(m.description());posterField.setText(m.posterPath());activeField.setSelected(m.active());}
 @FXML private void clear(){selected=null;titleField.clear();durationField.clear();genreField.clear();descriptionField.clear();posterField.clear();activeField.setSelected(true);list.getSelectionModel().clearSelection();}
 @FXML private void refresh(){runNetwork(()->client.send(RequestType.GET_MOVIES,null),r->{if(requireSuccess(r))list.getItems().setAll(client.responseData(r,MovieListResponse.class).movies());},statusLabel);}
 @FXML private void save(){try{AdminMovieRequest d=new AdminMovieRequest(selected==null?0:selected.id(),titleField.getText(),Integer.parseInt(durationField.getText()),descriptionField.getText(),genreField.getText(),posterField.getText(),activeField.isSelected());RequestType t=selected==null?RequestType.ADMIN_CREATE_MOVIE:RequestType.ADMIN_UPDATE_MOVIE;runNetwork(()->client.send(t,d),r->{if(requireSuccess(r)){clear();refresh();}},statusLabel);}catch(NumberFormatException e){statusLabel.setText("Duration must be a whole number.");}}
 @FXML private void back(){scenes.show(SceneManager.View.ADMIN_DASHBOARD);}
}
