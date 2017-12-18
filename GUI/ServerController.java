package GUI;

import NameServer.NamingServer;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

/**
 * ServerController is the controller for serverView.
 * severView is created with JavaFXsceneBuilder.
 * In the serverView the user can see all the notifications it receives.
 */
public class ServerController {

    private int viewWidth  = 600;
    private int viewHeight = 400;
    private Scene view;
    private HeadController headController;
    @FXML
    private ListView<String> listView;
    public void init(HeadController headcontroller){
        this.headController =headcontroller;
    }

    public void initData(){
        NamingServer server = new NamingServer();
        server.start(this);
    }
    public void update(String message){
        listView.getItems().add(message);
    }

    public void updateError(String message){
        listView.getItems().add(message);
    }

    public void view(Parent root){
        if(view == null)
            view = new Scene(root,viewWidth,viewHeight);
        Stage stage = new Stage();
        stage.setResizable(false);
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(final WindowEvent arg0) {
                System.exit(0);
            }
        });
        stage.setTitle("SystemY serverview");
        stage.setScene(view);
        stage.show();
    }
}
