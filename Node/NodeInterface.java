package Node;



import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeInterface extends Remote {
    public Neighbour getPrevious() throws RemoteException;
    public Neighbour getNext() throws RemoteException;
    public void setNext(Neighbour nextNode) throws RemoteException;
    public void setPrevious(Neighbour previousNode) throws RemoteException;
    public void setNumberOfNodesInNetwork(int number) throws RemoteException;
    public void updateNode(Neighbour previous, Neighbour next) throws RemoteException;
    public void setNameServerIp(String ip) throws RemoteException;
    public void sendFile(String ip,int destPort, String filePath,String fileName) throws RemoteException;
    public void failedToAddNode(Exception e) throws RemoteException;
}
