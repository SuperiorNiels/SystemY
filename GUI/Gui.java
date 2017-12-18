package GUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * this Class starts when the user opens the application.
 * The headcontroller is created and the openingview opens.
 */

public class Gui extends Application {


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        HeadController controller = new HeadController();
        controller.toLogin();
    }
}
