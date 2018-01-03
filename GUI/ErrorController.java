package GUI;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ErrorController {

    HeadController headController;
    private Scene view;
    private Stage stage;
    private int viewWidth  = 350;
    private int viewHeight = 77;
    @FXML
    private Label string;
    @FXML
    Button ok_button;

    private Boolean close = false;

    public void init(HeadController headcontroller){
        this.headController = headcontroller;
    }

    public void view(Parent root){
        if(view == null)
            view = new Scene(root,viewWidth,viewHeight);
        if(stage == null)
            stage = new Stage();

        stage.setResizable(false);
        stage.setTitle("SystemY Error");
        stage.setScene(view);
        stage.show();
    }

    public void close(){
        stage.close();
        if(close) {
            System.exit(1);
        }
    }

    public void setString(String text){
        string.setText(text);
    }

    public void setClose(Boolean close) {
        this.close = close;
    }
}
