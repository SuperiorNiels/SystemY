package NameServer;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class Client {
    private NamingInterface NamingServer;
    private Scanner scanner;

    public Client(){
        try {
            //Gets the bank object
            Registry registry = LocateRegistry.getRegistry("127.0.0.1");
            //import the stub
            NamingServer = (NamingInterface) registry.lookup("NamingServer");
        }catch (RemoteException e) {
            System.out.println("Problem connecting to the RMI server: " + e.getMessage());
        }catch (NotBoundException e) {
            System.out.println("Problem binding a registry to a stub: " + e.getMessage());
        }
    }

    public void communication(){
        boolean inLoop = true;
        while(inLoop) {
            String name, ip;
            scanner = new Scanner(System.in);
            System.out.println("press 1 to add a node to the server");
            System.out.println("press 2 to remove a node");
            System.out.println("press 3 to get the owner of the file");
            System.out.println("press 4 to export an xml file");
            System.out.println("Press 5 to exit");
            int answer = scanner.nextInt();
            try {
                switch (answer) {
                    case 1:
                        System.out.print("Node Name :");
                        name = scanner.next();
                        System.out.print("Ip: ");
                        ip = scanner.next();
                        try {
                            NamingServer.addNode(ip, name);
                        } catch (AlreadyExistsException e) {
                            System.err.println("Hash, name or ip already exists");
                        }
                        break;
                    case 2:
                        System.out.print("Node Name: ");
                        name = scanner.next();
                        try{
                            NamingServer.removeNode(name);
                        }catch(NullPointerException e){
                            System.err.println("Given name doesn't exist");
                        }

                        break;
                    case 3:
                        System.out.println("FileName: ");
                        name = scanner.next();
                        System.out.println("The owner ip is "+NamingServer.getOwner(name));
                        break;
                    case 4:
                        try {
                            NamingServer.createXML("./data/output.xml");
                        } catch (Exception e) {
                            System.out.println("error exporting xml " + e.getMessage());
                        }
                        break;
                    case 5:
                        System.out.println("bye bye!");
                        inLoop= false;
                        break;
                    default:
                        System.out.println("Wrong input");
                        inLoop = true;
                        break;
                }
            } catch (RemoteException e) {
                System.err.println("error with RMI server" + e.getMessage());
            }
        }

    }
}

