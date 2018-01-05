package NameServer;


import java.rmi.Remote;
import java.rmi.RemoteException;
import Node.Neighbour;

public interface NamingInterface extends Remote {
    void createXML(String path) throws RemoteException,Exception;
    void addNode(String ip, String name) throws RemoteException, AlreadyExistsException;
    void removeNode(String name) throws RemoteException, NullPointerException;
    Neighbour getOwner(String fileName) throws RemoteException;
    Neighbour findNextNode(String nameFailedNode) throws RemoteException;
    Neighbour findPreviousNode(String nameFailedNode) throws RemoteException;
    int getNumberOfNodes() throws RemoteException;
}
