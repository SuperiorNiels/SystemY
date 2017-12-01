package Node;

import NameServer.NamingInterface;
import Network.SendTCP;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.TreeMap;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class FileManager extends Thread {
    private Path root;
    private static final int PORT = 6000;
    private String nameServerIp;
    private WatchService watcher;
    private WatchKey key;
    private Node root_node; // Root node is node name that created the filemanager

    private static final String REPLICATED_PATH = "/replicated";
    private static final String LOCAL_PATH = "/local";
    private static final String DOWNLOAD_PATH = "/download";

    private TreeMap<Integer, FileEntry> map;

    public FileManager(String root, Node root_node) {
        this.root = Paths.get(root);
        this.nameServerIp = root_node.getNameServerIp();
        this.root_node = root_node;
        this.map = new TreeMap<Integer, FileEntry>();
    }

    /**
     * initiliazes the file manager
     * first it places a watcher on a given directory
     * afterwards, it gets a list of local files and replicates all of them to the right destination
     */
    public void initialize() {
        try {
            watcher = FileSystems.getDefault().newWatchService();
            registerRecursive(root);
            if(root_node.getNumberOfNodesInNetwork() != 0) {
                File folder = new File(root + LOCAL_PATH);
                File[] fileList = folder.listFiles();
                for (File file : fileList) {
                    replicate(file);
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Function that replicates a given file to the owner of the file.
     * If the owner of the file is the node itself, the file should be replicated to the previous node.
     * This method uses the sendTCP class, which sends a file through tcp.
     * Every other node has a tcp listener running that accepts the socket request, and handles the file receiving in
     * the receiveTCP method.
     * @param file file that has to be replicated
     */
    public void replicate(File file) {
        try {
            NamingInterface namingStub = (NamingInterface) Naming.lookup("//"+nameServerIp+"/NamingServer");
            //start RMI
            //get the owner of each file
            Neighbour owner = namingStub.getOwner(file.getName());
            Neighbour replicated = null;
            if (owner.getIp().equals(root_node.getIp())) {
                //This node is the owner of the file = replicate it to the previous node
                sendFile(root_node.getPrevious().getIp(), PORT, REPLICATED_PATH, file.getName());
                replicated = root_node.getPrevious();
            } else{
                //replicate it to the owner of the file
                sendFile(owner.getIp(), PORT, REPLICATED_PATH, file.getName());
                replicated = owner;
            }
            //You are the first node in the system, the map is empty, don't replicate!
            FileEntry new_entry = new FileEntry(owner, replicated, new Neighbour(root_node.getName(), root_node.getIp()),file.getName());
            map.put(calculateHash(file.getName()), new_entry);
        } catch (NotBoundException e) {
            System.err.println("The stub is not bound");
        } catch (MalformedURLException e) {
            System.err.println("Malformed URL");
        } catch (RemoteException e) {
            System.err.println("Problem with RMI connection");
        }
    }

    /**
     * Function that is used to send a file over tcp connection
     * This function can be called using RMI!
     * @param ip ip of destination
     * @param destPort port of destination
     * @param filePath path of the file
     * @param fileName name of the file
     */
    public void sendFile(String ip,int destPort,String filePath,String fileName){
        try {
            //opens a send socket with a given destination ip and destination port
            Socket sendSocket = new Socket(ip,destPort);
            //sends the given file to the given ip
            SendTCP send = new SendTCP(sendSocket,filePath,fileName);
        } catch (IOException e) {
            System.err.println("Problem opening port "+destPort);
        }

    }
    /**
     * function that prints the content of the treemap
     * is mainly used for debugging purposes
     */
    public void printMap() {
        System.out.println("FileManager Map of node: "+root_node.toString());
        for(Integer i : map.keySet()) {
            System.out.println("Hash: "+i+" ; Downloads: ");
            FileEntry entry = map.get(i);
            for(Neighbour node : entry.getDownloads()) {
                System.out.print("\t"+node.toString()+"\n");
            }
        }
    }

    public void addDownloadNodeToFileEntry(int hash, Neighbour node) {
        try {
            FileEntry entry = map.get(hash);
            entry.addNode(node);
        }catch (NullPointerException e) {
            System.err.println("FileEntry for hash: "+hash+" not found.");
        }
    }

    public void removeDownloadNodeFromFileEntry(int hash, Neighbour node) {
        try {
            FileEntry entry = map.get(hash);
            entry.removeNode(node);
        }catch (NullPointerException e) {
            System.err.println("FileEntry for hash: "+hash+" not found.");
        }
    }

    private void registerRecursive(final Path root) throws IOException {
        // register all subfolders
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * watcher thread that watches the folders that contain
     */
    public void run() {
        try {
            while ((key = watcher.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    switch (event.kind().toString()) {
                        case "ENTRY_CREATE":
                            System.out.println("File created.");
                            break;
                        case "ENTRY_MODIFY":
                            System.out.println("File modified.");
                            break;
                        case "ENTRY_DELETE":
                            System.out.println("File deleted.");
                            break;
                    }
                }
                key.reset();
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculates the hash of a given String
     * @param name
     * @return
     */
    private int calculateHash(String name) {
        return Math.abs(name.hashCode() % 32768);
    }

    public void receiveFileEntry(int fileHash,FileEntry entry){
        this.map.put(fileHash,entry);
    }

    /**
     * Shutdown send all replicated files on this node to the previous node
     * And check if the previous node had the file locally, if so send to the previous of the previous
     * @param prev the previous node
     */
    public void shutdown(Neighbour prev) {
        for (Map.Entry<Integer, FileEntry> entry : map.entrySet()) {
            Integer key = entry.getKey();
            FileEntry fiche = entry.getValue();
            Neighbour replicated = null;
            try {
                //Get RMI to the previous node
                NodeInterface nodeStub = (NodeInterface) Naming.lookup("//"+prev.getIp()+"/Node");
                if (calculateHash(fiche.getLocal().getName()) == calculateHash(prev.getName())) {
                    //send replicate to prev of prev
                    sendFile(nodeStub.getPrevious().getIp(), PORT, REPLICATED_PATH, new File("/replicated/"+fiche.getFileName()).getName());
                    replicated = nodeStub.getPrevious();
                }else{
                    //send replicate to prev
                    sendFile(prev.getIp(), PORT, REPLICATED_PATH, new File("/replicated/"+fiche.getFileName()).getName());
                    replicated = prev;
                }

                Neighbour owner = fiche.getOwner();

                //Send file entry to new owner node
                //the new owner node is always your previous node
                //the replicated node can be one of 2 options:
                //  - your previous
                //  - the previous of the previous
                nodeStub.receiveFileEntry(key,new FileEntry(prev,replicated,fiche.getLocal(),fiche.getFileName()));
            } catch (NotBoundException | MalformedURLException | RemoteException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * node checks all files he owns (via hash)
     * compares hash with new node (next)
     * if hash(file) is closer to hash next
     * send file to next, update nameserver about owner
     * @param next
     */
    public void updateFilesNewNode(Neighbour current,Neighbour next, int destPort){
        int hashNext = calculateHash(next.getName());
        //for every file
        for (Map.Entry<Integer, FileEntry> entry : map.entrySet()) {
            FileEntry fiche = entry.getValue();
            int hashFile = calculateHash(fiche.getLocal().getName());
            if(hashFile > hashNext){
                //sent via tcp to next
                sendFile(next.getIp(),destPort,REPLICATED_PATH, new File(REPLICATED_PATH+fiche.getFileName()).getName());
                //update fileEntry: new node becomes owner of the file
                fiche.setOwner(next);
                //this node is now download location of file
                fiche.addNode(current);
            }
        }
    }

}
