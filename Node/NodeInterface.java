package Node;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashSet;

public interface NodeInterface extends Remote {
    Neighbour getPrevious() throws RemoteException;
    Neighbour getNext() throws RemoteException;
    void setNext(Neighbour nextNode) throws RemoteException;
    void setPrevious(Neighbour previousNode) throws RemoteException;
    void updateNode(Neighbour previous, Neighbour next) throws RemoteException;
    void failedToAddNode() throws RemoteException;
    void createFileEntry(Neighbour owner, Neighbour replicated, Neighbour local, String fileName, HashSet<Neighbour> downloads) throws RemoteException;
    FileEntry getFileEntry(String fileName) throws RemoteException, NullPointerException;
    void remoteSendFile(String ip,int destPort,String srcFilePath,String fileName,String destFolder,boolean notifDownloader) throws RemoteException;
    void moveFile(String from,String to) throws RemoteException;
    void remoteCheckFileEntry(String filename,Neighbour leavingNode) throws RemoteException;
    void remoteRemoveFromDownload(String filename,Neighbour leavingNode) throws RemoteException;
    void downloadFile(String filename) throws RemoteException;
    void fileDownloaded(String filename) throws RemoteException;
    int getCurrentNumberDownloads() throws RemoteException;
    Neighbour getDownloadLocation(String filename, Neighbour want_download) throws RemoteException;
    void deleteFileOwner(String filename,boolean shuttingDown)throws RemoteException;
    void deleteFile(String target)throws RemoteException;
}
