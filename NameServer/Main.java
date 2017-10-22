package NameServer;

public class Main {

    public static void main(String[] args) {
        Mapper mapper = new Mapper();
        mapper.updateMap(4,"10.0.0.1","NODE-1");
        mapper.updateMap(12,"10.0.0.2","NODE-2");
        mapper.updateMap(36,"10.0.0.3","NODE-3");
        mapper.updateMap(10,"10.0.0.4","NODE-4");
        mapper.createXML();
    }
}
