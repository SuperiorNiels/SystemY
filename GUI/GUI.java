package GUI;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * this Class starts when the user opens the application.
 * The headcontroller is created and the openingview opens.
 */

public class GUI extends Application {

    HeadController controller;

    public void createGUI() {
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        controller = new HeadController();
        controller.toLogin();
        controller.setLoginGui();
    }
}
