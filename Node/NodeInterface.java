package Node;

import com.sun.org.apache.regexp.internal.RE;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeInterface extends Remote {
    public Neighbour getPrevious() throws RemoteException;
    public Neighbour getNext() throws RemoteException;
    public void setNext(Neighbour nextNode) throws RemoteException;
    public void setPrevious(Neighbour previousNode) throws RemoteException;
    public void setNumberOfNodesInNetwork(int number) throws RemoteException;
    public void updateNode(Neighbour previous, Neighbour next) throws RemoteException;
}
