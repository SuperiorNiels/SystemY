package NameServer;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Naming extends Remote {
    public String createXml() throws RemoteException;
    public void addNode() throws RemoteException;
    public void removeNode() throws RemoteException;
    public void addFile() throws RemoteException;
    public void removeFile() throws RemoteException;
    public void getOwner() throws RemoteException;
}
