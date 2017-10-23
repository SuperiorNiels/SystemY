package NameServer;

public class Main {

    public static void main(String[] args) {
        NameServer.NamingServer server = new NameServer.NamingServer();
        server.updateMap(4,"10.0.0.1","NODE-1");
        server.updateMap(12,"10.0.0.2","NODE-2");
        server.updateMap(36,"10.0.0.3","NODE-3");
        server.updateMap(10,"10.0.0.4","NODE-4");
        server.updateMap(78,"10.0.0.5","NODE-5");
        if(!server.createXML().isEmpty()) {
            System.out.println("XML created.");
        }
    }
}
