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

    public void initialize() {
        try {
            watcher = FileSystems.getDefault().newWatchService();
            registerRecursive(root);
            File folder = new File(root+LOCAL_PATH);
            File [] fileList = folder.listFiles();
            for(File file : fileList) {
                replicate(file);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    public  void addMapEntry(){

    }

    public void replicate(File file) {
        try {
            NamingInterface namingStub = (NamingInterface) Naming.lookup("//"+nameServerIp+"/NamingServer");
            //start RMI
            //get the owner of each file
            Neighbour owner = namingStub.getOwner(file.getName());
            Neighbour replicated;
            if (owner.getIp().equals(root_node.getIp())) {
                //This node is the owner of the file = replicate it to the previous node
                sendFile(root_node.getPrevious().getIp(), PORT, REPLICATED_PATH, file.getName());
                replicated = root_node.getPrevious();
            } else {
                //replicate it to the owner of the file
                sendFile(owner.getIp(), PORT, REPLICATED_PATH, file.getName());
                replicated = owner;
            }
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

    private void receiveFileEntry(int fileHash,FileEntry entry){
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
            try {
                NodeInterface nodeStub = (NodeInterface) Naming.lookup("//"+prev.getIp()+"/Node");
                if (calculateHash(fiche.getLocal().getName()) == calculateHash(prev.getName())) {
                    //send replicate to prev of prev
                    sendFile(nodeStub.getPrevious().getIp(), PORT, REPLICATED_PATH, new File("/replicated/"+fiche.getFileName()).getName());
                }else{
                    //send replicate to prev
                    sendFile(prev.getIp(), PORT, REPLICATED_PATH, new File("/replicated/"+fiche.getFileName()).getName());
                }

                Neighbour owner = fiche.getOwner();

                //Send file entry to node


            } catch (NotBoundException | MalformedURLException | RemoteException e) {
                e.printStackTrace();
            }

        }
    }

}
