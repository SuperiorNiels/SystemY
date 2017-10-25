package NameServer;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Naming extends Remote {
    public String createXml() throws RemoteException;
    public void addNode(String ip, String name) throws RemoteException, AlreadyExistsException;
    public void removeNode() throws RemoteException;
    public void addFile(String name) throws RemoteException;
    public void removeFile(String fileName) throws RemoteException;
    public void getOwner(String fileName) throws RemoteException;
}
