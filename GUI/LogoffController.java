package GUI;


import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import Node.Node;
import javafx.stage.WindowEvent;

import java.io.IOException;

public class LogoffController {

    @FXML
    Button logoff_yes_bnt;
    private int viewWidth  = 379;
    private int viewHeight = 132;
    private Node node;
    private HeadController headController;
    private Scene view;

    public void init(HeadController headcontroller){
        this.headController =headcontroller;
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
    };

    /**
     * close current (logoff) window
     * go back to mainview
     */
    public void mainView(){
        headController.toMain();
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
