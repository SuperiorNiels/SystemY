package NameServer;

public class Main {

    public static void main(String[] args) {
        NamingServer server = new NamingServer();
        try {
            server.addNode("10.0.0.1", "NODE-1");
            server.addNode("10.0.0.2", "NODE-2");
            server.addNode("10.0.0.3", "NODE-3");
            server.addNode("10.0.0.4", "NODE-4");
            server.addNode("10.0.0.5", "NODE-5");
            server.createXML("./data/output.xml");
        }
        catch (AlreadyExistsException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
