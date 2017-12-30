package GUI;


import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import Node.Node;
import javafx.stage.WindowEvent;
import java.io.IOException;

/**
 * LogoffController is the controller for the LogOffview.
 * LogOffview is created with JavaFXscenebuilder.
 * LogOffView is opened when a user clicks on the button "log off" or when the user clicks on the exit button on the mainview.
 * In the LogOffview the user is asked is it really wants to log off.
 * When the users clicks the yes button the node is propely shutdown and the application stops.
 * otherwise th logOffview closes and the mainview opens again.
 */
public class LogoffController {

    @FXML
    Button logoff_yes_bnt;
    private int viewWidth  = 379;
    private int viewHeight = 132;
    private Node node;
    private HeadController headController;
    private Scene view;
    private int delay;

    public void init(HeadController headcontroller){
        this.headController =headcontroller;
        this.delay = headcontroller.getDelay();
    }

    public void initData(){
        node = headController.getNode();
    }

    public void logOff() throws IOException {
        headController.toLoading();
        node.shutDown();
        headController.closeLoading();
        Stage currentWindow = (Stage) logoff_yes_bnt.getScene().getWindow();
        currentWindow.close();
        System.exit(0);
    }

    public void mainView(){
        //headController.toMain();
        Stage currentWindow = (Stage) logoff_yes_bnt.getScene().getWindow();
        currentWindow.close();
    }

    public void view(Parent root){
        if(view == null)
            view = new Scene(root,viewWidth,viewHeight);
        Stage stage = new Stage();
        stage.setResizable(false);
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(final WindowEvent arg0) {
                stage.close();
                headController.toLogoff();
            }
        });
        stage.setTitle("System Y logoff");
        stage.setScene(view);
        stage.show();
    }
}
