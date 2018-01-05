package GUI;

import javafx.application.Platform;

public class GUI_Controller {

    HeadController controller;

    public GUI_Controller(HeadController controller) {
        this.controller = controller;
    }

    public void closeWithError() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                controller.closeLoading();
                controller.toError("Failed to add node to the nameserver",true);
            }
        });
    }

    public void openWindow() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                controller.closeLoading();
                controller.toMain();
            }
        });
    }
}
