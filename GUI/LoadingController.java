package GUI;


import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

/**
 * LoadingController is the controller for the loadingView.
 * LoadingView is created with JavaFXScenebuilder.
 * the loadingView is opened when the user makes a RMI call.
 * This call can take some time, the loadingView notific the users that it can take some time.
 * When the RMI Call is finished this view will automaticly close.
 */
public class LoadingController {

    HeadController headController;
    Scene view;
    Stage stage;

    private int viewWidth  = 260;
    private int viewHeight = 90;
    public void init(HeadController headcontroller){
        this.headController =headcontroller;
    }

    public void view(Parent root){
        if(view == null)
            view = new Scene(root,viewWidth,viewHeight);
        if(stage == null)
            stage = new Stage();

        stage.setResizable(false);
        stage.setTitle("SystemY Loading");
        stage.setScene(view);
        stage.show();
    }

    public void close(){
        stage.close();
    }
}
