package Node;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeInterface extends Remote {
    public String getPrevious() throws RemoteException;
    public String getNext() throws RemoteException;
    public void setNext(String ip) throws RemoteException;
    public void setPrevious(String ip) throws RemoteException;
}
