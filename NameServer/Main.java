package NameServer;

public class Main {

    public static void main(String[] args) {
        try {
            if (args[0].toLowerCase().equals("server")) {
                NamingServer server = new NamingServer();
            } else if (args[0].toLowerCase().equals("client")) {
                NameServerTest client = new NameServerTest();
                client.communication();
            } else {
                System.out.println("Please use parameter server/client.");
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("No parameter used, please use server/client");
        }
    }
}
