package Node;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeInterface extends Remote {
    public Neighbour getPrevious() throws RemoteException;
    public Neighbour getNext() throws RemoteException;
    public void setNext(Neighbour nextNode) throws RemoteException;
    public void setPrevious(Neighbour previousNode) throws RemoteException;
}
