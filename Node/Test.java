package Node;

import java.util.Scanner;

public class Test {
    public static void main(String[] args) {
        FileManager manager = new FileManager("./files/", new Neighbour("niels", "10.0.1.3"));
        manager.initialize();
        manager.start();
        manager.addLocalNodeToFileEntry(5957, new Neighbour("dieter","10.0.1.10"));
        manager.addLocalNodeToFileEntry(5957, new Neighbour("jamie","10.0.1.2"));
        manager.addLocalNodeToFileEntry(5957, new Neighbour("alex","10.0.1.69"));
        manager.removeLocalNodeFromFileEntry(5957, new Neighbour("dieter","10.0.1.10"));
        while(true) {
            Scanner scan = new Scanner(System.in);
            String in = scan.nextLine();
            if(in.toLowerCase().equals("print")) {
                manager.printMap();
            }
        }
    }
}
