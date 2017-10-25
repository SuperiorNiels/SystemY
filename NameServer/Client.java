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
        String name,ip;

        System.out.println("press 1 to add a node to the server");
        System.out.println("press 2 to remove a node");
        System.out.println("press 3 to add a filename to the server");
        System.out.println("press 4 to remove a filename from the serer");
        System.out.println("press 5 to get the owner of the file");
        System.out.println("press 6 to export an xml file");
        int answer = scanner.nextInt();
        try {
            switch(answer){
                case 1:

                    System.out.print("Node Name :");
                    name = scanner.next();
                    System.out.print("Ip :");
                    ip = scanner.next();
                    try{
                        NamingServer.addNode(ip , name);
                    }catch(AlreadyExistsException e){
                        System.out.println("Hash, name or ip already exists");
                    }

                    break;
                case 2:
                    System.out.print("Node Name :");
                    name = scanner.next();
                    NamingServer.removeNode(name);
                    break;
                case 3:
                    System.out.println("FileName: ");
                    name = scanner.next();
                    NamingServer.addFile(name);
                    break;
                case 4:
                    System.out.println("FileName: ");
                    name = scanner.next();
                    NamingServer.removeFile(name);
                    break;
                case 5:
                    System.out.println("FileName: ");
                    name = scanner.next();
                    NamingServer.getOwner(name);
                    break;
                case 6:
                    try {
                        NamingServer.createXML("test");
                    }catch(Exception e){
                        System.out.println("error exporting xml "+e.getMessage());
                    }

            }
        }catch(RemoteException e){
            System.err.println("error with RMI server"+e.getMessage());
        }

    }
}

