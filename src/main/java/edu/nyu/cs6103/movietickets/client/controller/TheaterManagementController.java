package edu.nyu.cs6103.movietickets.client.controller;
import edu.nyu.cs6103.movietickets.client.SceneManager; import edu.nyu.cs6103.movietickets.shared.RequestType; import edu.nyu.cs6103.movietickets.shared.dto.*;
import javafx.fxml.FXML; import javafx.scene.control.*;
public final class TheaterManagementController extends AbstractClientController {
 @FXML private ListView<TheaterDto> list; @FXML private TextField nameField,locationField,rowsField,seatsField; @FXML private Label statusLabel; private TheaterDto selected;
 @Override protected void onShow(Object p){list.setCellFactory(v->new ListCell<>(){protected void updateItem(TheaterDto t,boolean e){super.updateItem(t,e);setText(e||t==null?null:t.name()+" — "+t.location());}});list.getSelectionModel().selectedItemProperty().addListener((o,a,t)->populate(t));refresh();}
 private void populate(TheaterDto t){selected=t;if(t==null)return;nameField.setText(t.name());locationField.setText(t.location());rowsField.setDisable(true);seatsField.setDisable(true);}
 @FXML private void clear(){selected=null;nameField.clear();locationField.clear();rowsField.setText("5");seatsField.setText("10");rowsField.setDisable(false);seatsField.setDisable(false);list.getSelectionModel().clearSelection();}
 @FXML private void refresh(){runNetwork(()->client.send(RequestType.GET_THEATERS,null),r->{if(requireSuccess(r))list.getItems().setAll(client.responseData(r,TheaterDto[].class));},statusLabel);}
 @FXML private void save(){try{AdminTheaterRequest d=new AdminTheaterRequest(selected==null?0:selected.id(),nameField.getText(),locationField.getText(),Integer.parseInt(rowsField.getText()),Integer.parseInt(seatsField.getText()));RequestType t=selected==null?RequestType.ADMIN_CREATE_THEATER:RequestType.ADMIN_UPDATE_THEATER;runNetwork(()->client.send(t,d),r->{if(requireSuccess(r)){clear();refresh();}},statusLabel);}catch(NumberFormatException e){statusLabel.setText("Rows and seats must be whole numbers.");}}
 @FXML private void back(){scenes.show(SceneManager.View.ADMIN_DASHBOARD);}
}
