import NameServer.NamingServer;

import java.util.Scanner;
import Node.Node;

public class Main {
    public static void main(String[] args) {
        if(args[0].toLowerCase().equals("server")) {
            NamingServer server = new NamingServer();
            server.start();
        } else if(args[0].toLowerCase().equals("client")) {
            Node node = new Node();
            node.bootstrap();
        }
    }
}

