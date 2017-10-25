package NameServer;

public class Main {

    public static void main(String[] args) {
        NamingServer server = new NamingServer();
        try {
            server.addMap(4, "10.0.0.1", "NODE-1");
            server.addMap(12, "10.0.0.2", "NODE-2");
            server.addMap(36, "10.0.0.3", "NODE-3");
            server.addMap(10, "10.0.0.4", "NODE-4");
            server.addMap(78, "10.0.0.5", "NODE-5");
        }
        catch (AlreadyExistsException e) {
            e.printStackTrace();
        }
        if(!server.createXML().isEmpty()) {
            System.out.println("XML created.");
        }
    }
}
