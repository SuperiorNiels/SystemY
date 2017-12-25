package GUI;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class NameAlreadyExistController {

    private int viewWidth  = 244;
    private int viewHeight = 81;
    private Scene view;
    private HeadController headController;

    public void init(HeadController headcontroller){
        this.headController =headcontroller;
    }

    public void view(Parent root){
        if(view == null)
            view = new Scene(root,viewWidth,viewHeight);
        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(view);
        stage.show();
    }

    public void shutdown(){
        try {
            Runtime.getRuntime().exec("java -jar SystemY.jar client gui");
        } catch (IOException e) {
            System.out.println("problem opening java -jar SystemY.jar client gui");
        }
        System.exit(0);
    }
}
