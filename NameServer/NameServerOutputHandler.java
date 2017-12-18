package NameServer;

import GUI.ServerController;

public class NameServerOutputHandler {

    private int mode = 0; // 0 = cli, 1 = gui
    private ServerController controller;

    public NameServerOutputHandler() {
        // Run in cli mode
    }

    public NameServerOutputHandler(ServerController controller) {
        mode = 1;
        this.controller = controller;
    }

    public void print(String string) {
        if(mode==1) {
            controller.update(string);
        } else {
            System.out.println(string);
        }
    }

    public void printError(String string) {
        if(mode==1) {
            controller.updateError(string);
        } else {
            System.err.println(string);
        }
    }

}
