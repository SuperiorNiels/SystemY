package NameServer;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NamingInterface extends Remote {
    public void createXML(String path) throws RemoteException,Exception;
    public void addNode(String ip, String name) throws RemoteException, AlreadyExistsException;
    public void removeNode(String name) throws RemoteException;
    public int addFile(String name) throws RemoteException;
    public void removeFile(String fileName) throws RemoteException;
    public String getOwner(String fileName) throws RemoteException;
}
