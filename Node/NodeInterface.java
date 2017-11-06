package Node;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeInterface extends Remote {
    public Node getPrevious() throws RemoteException;
    public Node getNext() throws RemoteException;
    public void setNext(Node nextNode) throws RemoteException;
    public void setPrevious(Node previousNode) throws RemoteException;
}
