package GUI;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import Node.Node;
import Node.Neighbour;
import javafx.stage.WindowEvent;


import java.io.IOException;


/**
 * NetworkController is the controller for the networkview.
 * networkview is created by JavaFxScenebuilder.
 * In the networkview the user can see how many nodes theire are in the network
 * and who his previous and next node are.
 */
public class NetworkController {

    private int viewWidth  = 300;
    private int viewHeight = 200;
    private Node node;
    private HeadController headController;

    @FXML
    private Label numberOfNodeLabel;
    @FXML
    private Label nextLabel;
    @FXML
    private Label previousLabel;
    private Scene view;

    public void init(HeadController headcontroller){
        this.headController =headcontroller;
    }

    public void initData(){
        this.node = headController.getNode();
        numberOfNodeLabel.setText(Integer.toString(node.getNumberOfNodesInNetwork()+1));
        Neighbour temp = node.getNext();
        nextLabel.setText("Name: " + temp.getName() + "\nIP: " + temp.getIp());
        temp = node.getPrevious();
        previousLabel.setText("Name: " + temp.getName() + "\nIP: " + temp.getIp());
    }

    public void mainView() throws IOException {
        Stage currentWindow = (Stage) nextLabel.getScene().getWindow();
        currentWindow.close();
    }

    public void view(Parent root){
        initData();
        if(view == null)
            view = new Scene(root,viewWidth,viewHeight);
        Stage stage = new Stage();
        stage.setResizable(false);

        stage.setTitle("SystemY Network");
        stage.setScene(view);
        stage.show();
    }
}
