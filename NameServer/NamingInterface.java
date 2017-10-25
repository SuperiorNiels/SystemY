package NameServer;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NamingInterface extends Remote {
    public String createXML() throws RemoteException,Exception;
    public void addNode(String ip, String name) throws RemoteException, AlreadyExistsException;
    public void removeNode(String name) throws RemoteException;
    public void addFile(String name) throws RemoteException;
    public void removeFile(String fileName) throws RemoteException;
    public void getOwner(String fileName) throws RemoteException;
}
