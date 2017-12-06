package Node;



import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface NodeInterface extends Remote {
    public Neighbour getPrevious() throws RemoteException;
    public Neighbour getNext() throws RemoteException;
    public void setNext(Neighbour nextNode) throws RemoteException;
    public void setPrevious(Neighbour previousNode) throws RemoteException;
    public void updateNode(Neighbour previous, Neighbour next) throws RemoteException;
    public void setNameServerIp(String ip) throws RemoteException;
    public void failedToAddNode(Exception e) throws RemoteException;
    public void createFileEntry(Neighbour owner, Neighbour replicated, Neighbour local, String fileName, ArrayList<Neighbour> downloads) throws RemoteException;
    public FileEntry getFileEntry(String fileName) throws RemoteException, NullPointerException;
    public void remoteSendFile(String ip,int destPort,String srcFilePath,String fileName,String destFolder) throws RemoteException;
    public void moveFile(String from,String to) throws RemoteException;
    public void remoteCheckFileEntry(String filename,Neighbour leavingNode) throws RemoteException;
    public void remoteRemoveFromDownload(String filename,Neighbour leavingNode) throws RemoteException;
}
