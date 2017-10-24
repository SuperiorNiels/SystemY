package NameServer;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FileMapRemote extends Remote {

    void addFile(String fileName, String location) throws RemoteException;
    String getLocation(String fileName) throws RemoteException;

}
