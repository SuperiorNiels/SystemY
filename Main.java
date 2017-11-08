import NameServer.NamingServer;

public class Main {
    public static void main(String[] args) {
        if(args[0].toLowerCase().equals("server")) {
            NamingServer server = new NamingServer();
            server.start();
        } else if(args[0].toLowerCase().equals("client")) {

        }
    }
}
