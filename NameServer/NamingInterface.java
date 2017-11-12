package NameServer;

import Node.Node;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NamingInterface extends Remote {
    public void createXML(String path) throws RemoteException,Exception;
    public void addNode(String ip, String name) throws RemoteException, AlreadyExistsException;
    public void removeNode(String name) throws RemoteException, NullPointerException;
    public String getOwner(String fileName) throws RemoteException;
    public Node findNextNode(String nameFailedNode) throws RemoteException;
    public Node findPreviousNode(String nameFailedNode) throws RemoteException;
    public int getNumberOfNodes() throws RemoteException;
}
