package NameServer;

public class Main {

    public static void main(String[] args) {
        if(args[0].toLowerCase().equals("server")) {
            NamingServer server = new NamingServer();
        } else if(args[0].toLowerCase().equals("client")) {
            Client client = new Client();
            client.communication();
        } else {
            System.out.println("Please use parameter server/client.");
        }
    }
}
