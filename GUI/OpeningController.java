package GUI;

import NameServer.NamingServer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class OpeningController {

    private int viewWidth  = 346;
    private int viewHeight = 169;
    @FXML
    private Button serverButton;
    @FXML
    private Button clientButton;
    private HeadController headController;
    private Scene view;

    public void Client(){
        Stage currentWindow = (Stage) clientButton.getScene().getWindow();
        currentWindow.close();
        headController.toLogin();
    }

    public void Server(){
        Stage currentWindow = (Stage) serverButton.getScene().getWindow();
        currentWindow.close();
        headController.toServer();
    }

    public void init(HeadController headcontroller){
        this.headController =headcontroller;
    }

    public void view(Parent root){
        if(view == null)
            view = new Scene(root,viewWidth,viewHeight);
        Stage stage = new Stage();
        stage.setResizable(false);
        stage.setTitle("SystemY StartUp");
        stage.setScene(view);
        stage.show();
    }
}
