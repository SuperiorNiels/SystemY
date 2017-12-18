import NameServer.NamingServer;

import Node.Node;
import GUI.GUI;

public class Main {
    public static void main(String[] args) {
        if(args[0].toLowerCase().equals("server")) {
            NamingServer server = new NamingServer();
            server.start();
        } else if(args[0].toLowerCase().equals("client")) {
            try {
                if (args[1].toLowerCase().equals("gui")) {
                    new GUI.GUI().createGUI();
                } else if (args[1].toLowerCase().equals("cli")) {
                    Node node = new Node();
                }
            } catch(Exception e) {
                System.err.println("Please enter correct parameter gui/cli");
            }
        }
    }
}

