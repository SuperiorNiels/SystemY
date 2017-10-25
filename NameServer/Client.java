package NameServer;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class Client {
    private Naming NamingServer;
    private Scanner scanner;

    public Client(){
        try {
            //Gets the bank object
            Registry registry = LocateRegistry.getRegistry("127.0.0.1");
            //import the stub
             NamingServer = (Naming) registry.lookup("NamingServer");
        }catch (RemoteException e) {
            System.out.println("Problem connecting to the RMI server: " + e.getMessage());
        }catch (NotBoundException e) {
            System.out.println("Problem binding a registry to a stub: " + e.getMessage());
        }
    }

    public void communication(){
        System.out.println("press 1 to add a node to the server");
        System.out.println("press 2 to remove a node");
        System.out.println("press 3 to add a filename to the server");
        System.out.println("press 4 to remove a filename from the serer");
        System.out.println("press 5 to get the owner of the file");
        int answer = scanner.nextInt();
        try {
            switch(answer){
                case 1:
                    NamingServer.addNode();
                    break;
                case 2:
                    NamingServer.removeNode();
                    break;
                case 3:
                    NamingServer.addFile();
                    break;
                case 4:
                    NamingServer.removeFile();
                    break;
                case 5:
                    NamingServer.getOwner();
                    break;

            }
        }catch(RemoteException e){
            System.err.println("error with RMI server"+e.getMessage());
        }

    }
}

