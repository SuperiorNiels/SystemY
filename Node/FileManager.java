package Node;

import NameServer.NamingInterface;
import Network.SendTCP;
import Network.TCPListenerService;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class FileManager extends Thread {
    private Path rootPath;
    private static final int PORT = 6000;
    private WatchService watcher;
    private String nameServerIp;
    private WatchKey key;
    private Node rootNode; // Root node is node that created the filemanager
    private static final String REPLICATED_FOLDER = "replicated";
    private static final String LOCAL_FOLDER = "local";
    private static final String DOWNLOAD_FOLDER = "download";

    //Map contains the file entries of your owned files!
    private TreeMap<Integer, FileEntry> map;

    public FileManager(String rootPath, Node rootNode) {
        this.rootPath = Paths.get(rootPath);
        this.rootNode = rootNode;
        this.map = new TreeMap<Integer, FileEntry>();
        //checks if all given subfolders are present
        if(!initDirectories())
            System.out.println("There was an error creating the sub directories");
        //starts a tcp listener that listens for tcp request
        TCPListenerService TCPListener = new TCPListenerService(rootPath);

        try {
            watcher = FileSystems.getDefault().newWatchService();
            registerRecursive(this.rootPath);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *Fills in the nameServerIp
     *Gets a list of local files and replicates all of them to the right destination
     */
    public void initialize() {
        nameServerIp = rootNode.getNameServerIp();
        if(rootNode.getNumberOfNodesInNetwork() != 0) {
            File folder = new File(rootPath+"/"+LOCAL_FOLDER);
            File[] fileList = folder.listFiles();
            try {
                for (File file : fileList) {
                    replicate(file);
                }
            } catch (NullPointerException e) {
                System.out.println("No files in local folder. No replication needed.");
            }
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
            //System.out.println("Replicating file");
            NamingInterface namingStub = (NamingInterface) Naming.lookup("//"+nameServerIp+"/NamingServer");
            //start RMI
            //get the owner of each file
            Neighbour owner = namingStub.getOwner(file.getName());
            Neighbour replicated = null;
            if (owner.getIp().equals(rootNode.getIp())) {
                //This node is the owner of the file = replicate it to the previous node
                sendFile(rootNode.getPrevious().getIp(), PORT, rootPath+"/"+LOCAL_FOLDER, file.getName(),REPLICATED_FOLDER);
                replicated = rootNode.getPrevious();
            } else{
                //replicate it to the owner of the file
                sendFile(owner.getIp(), PORT, rootPath+"/"+LOCAL_FOLDER, file.getName(),REPLICATED_FOLDER);
                replicated = owner;
            }

            NodeInterface owner_stub = (NodeInterface) Naming.lookup("//"+owner.getIp()+"/Node");
            owner_stub.createFileEntry(owner, replicated, new Neighbour(rootNode.getName(), rootNode.getIp()),file.getName(),new ArrayList());
        } catch (NotBoundException e) {
            System.err.println("The stub is not bound");
        } catch (MalformedURLException e) {
            System.err.println("Malformed URL");
        } catch (RemoteException e) {
            System.err.println("Problem with RMI connection"+nameServerIp);
        }
    }

    /**
     * RMI function to remotely create a file entry on a node
     * @param owner
     * @param replicated
     * @param local
     * @param fileName
     */
    public void createFileEntry(Neighbour owner, Neighbour replicated, Neighbour local, String fileName,ArrayList<Neighbour> downloads) {
        FileEntry new_entry = new FileEntry(owner, replicated, local, fileName,downloads);
        map.put(calculateHash(fileName), new_entry);
    }

    public FileEntry getFileEntry(String fileName) throws NullPointerException {
        return map.get(calculateHash(fileName));
    }

    /**
     * Function that is used to send a file over tcp connection
     * This function can be called using RMI!
     * @param ip ip of destination
     * @param destPort port of destination
     * @param srcFilePath source path of the file that you want to send
     * @param fileName name of the file
     * @param destFolder name of the folder where the file has to be saved at the destination
     */
    public synchronized void sendFile(String ip,int destPort,String srcFilePath,String fileName,String destFolder){
        try {
            //System.out.println("Sending file: "+fileName);
            //opens a send socket with a given destination ip and destination port
            Socket sendSocket = new Socket(ip,destPort);
            //sends the given file to the given ip
            SendTCP send = new SendTCP(sendSocket,srcFilePath,fileName,destFolder);
        } catch (IOException e) {
            System.err.println("Problem opening port "+destPort+" ");
            e.printStackTrace();
        }

    }

    /**
     * function that prints the content of the treemap
     * is mainly used for debugging purposes
     */
    public void printMap() {
        System.out.println("FileEntries of node: "+ rootNode.toString());
        for(Integer i : map.keySet()) {
            FileEntry entry = map.get(i);
            System.out.println("File: "+entry.getFileName()+" Hash: "+i);
            try { System.out.println("\t Local: "+entry.getLocal().toString()); }
            catch (NullPointerException e) { System.out.println("\t Local: NULL"); }
            try { System.out.println("\t Replicated: "+entry.getReplicated().toString()); }
            catch (NullPointerException e) { System.out.println("\t Replicated: NULL"); }
            System.out.println("\t Downloads:");
            for(Neighbour node : entry.getDownloads()) {
                System.out.print("\t\t"+node.toString()+"\n");
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
                            if(new File(rootPath+"/"+ LOCAL_FOLDER+"/"+event.context().toString()).exists()) {
                                replicate(new File(event.context().toString()));
                            }
                            break;
                        case "ENTRY_MODIFY":
                            //System.out.println("File modified.");
                            break;
                        case "ENTRY_DELETE":
                            //System.out.println("File deleted.");
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

    /**
     * Shutdown send all replicated files on this node to the previous node
     * And check if the previous node had the file locally, if so send to the previous of the previous
     * @param prev the previous node
     */
    public void shutdown(Neighbour prev) {
        try {
            TreeMap<Integer,FileEntry> replicatedFiles = getReplicatedMapFiles();
            if(replicatedFiles!=null){
                for (Map.Entry<Integer, FileEntry> entry : replicatedFiles.entrySet()) {
                    Integer key = entry.getKey();
                    FileEntry fiche = entry.getValue();
                    Neighbour replicated = null;
                    try {
                        //Get RMI to the previous node
                        NodeInterface nodeStub = (NodeInterface) Naming.lookup("//"+prev.getIp()+"/Node");
                        if (calculateHash(fiche.getLocal().getName()) == calculateHash(prev.getName())) {
                            //send replicate to prev of prev
                            if(!nodeStub.getPrevious().equals(fiche.getLocal())) { //Check if the prev has a prev
                                sendFile(nodeStub.getPrevious().getIp(), PORT, rootPath + "/" + REPLICATED_FOLDER, fiche.getFileName(), REPLICATED_FOLDER);
                                replicated = nodeStub.getPrevious();
                            }
                        }else{
                            //send replicate to prev
                            sendFile(prev.getIp(), PORT, rootPath+"/"+REPLICATED_FOLDER, fiche.getFileName(),REPLICATED_FOLDER);
                            replicated = prev;
                        }

                        Neighbour owner = fiche.getOwner();

                        //Send file entry to new owner node
                        //the new owner node is always your previous node
                        //the replicated node can be one of 2 options:
                        //  - your previous
                        //  - the previous of the previous
                        nodeStub.createFileEntry(prev,replicated,fiche.getLocal(),fiche.getFileName(),fiche.getDownloads());
                    } catch (NotBoundException | MalformedURLException | RemoteException e) {
                        System.err.println("RMI error in filemanager shutdown!");
                    }
                }
            }
        } catch (RemoteException | NotBoundException | MalformedURLException e) {
            System.err.println("Error shutting down your filemananger!");
        }

    }

    /**
     * node checks all files he owns (via hash)
     * compares hash with new node (next)
     * if hash(file) is closer to hash next
     * send file to next, update nameserver about owner
     */
    public void updateFilesNewNode(){
        Neighbour next = rootNode.getNext();
        int hashNext = calculateHash(next.getName());
        //for every file
        try {
            TreeMap<Integer,FileEntry> replicatedFiles = getReplicatedMapFiles();
            if(replicatedFiles!=null){
                for (Map.Entry<Integer, FileEntry> entry : replicatedFiles.entrySet()) {
                    FileEntry fiche = entry.getValue();
                    int hashFile = calculateHash(fiche.getLocal().getName());
                    int myhash = calculateHash(rootNode.getName());
                    int nextHash = calculateHash(rootNode.getNext().getName());
                    if(hashFile > hashNext && myhash<nextHash) {
                        // First replace file
                        new File(rootPath+"/"+REPLICATED_FOLDER+"/"+fiche.getFileName()).renameTo(new File(rootPath+"/"+DOWNLOAD_FOLDER+"/"+fiche.getFileName()));
                        //sent via tcp to next
                        sendFile(next.getIp(),PORT,rootPath+"/"+DOWNLOAD_FOLDER, fiche.getFileName(),REPLICATED_FOLDER);
                        //update fileEntry: new node becomes owner of the file
                        fiche.setOwner(next);
                        //this node is now download location of file
                        fiche.addNode(new Neighbour(rootNode.getName(),rootNode.getIp()));

                        //Via RMI set update the file fiche on the owner
                        NamingInterface namingStub = (NamingInterface) Naming.lookup("//"+nameServerIp+"/NamingServer");
                        NodeInterface nodeStub = (NodeInterface) Naming.lookup("//"+namingStub.getOwner(fiche.getFileName()).getIp()+"/Node");
                        nodeStub.createFileEntry(namingStub.getOwner(fiche.getFileName()),next,fiche.getLocal(),fiche.getFileName(),fiche.getDownloads());
                    }
                }
            }
        } catch (RemoteException | NotBoundException | MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * This function return the contents of your replicated folder with all the fileEntries
     * This funtion get the owner of each file from the namingserver
     * Then requests the file entry from each owner for every file
     * @return
     * @throws RemoteException
     * @throws NotBoundException
     * @throws MalformedURLException
     */
    private TreeMap<Integer,FileEntry> getReplicatedMapFiles() throws RemoteException, NotBoundException, MalformedURLException {
        File[] files = new File(rootPath+"/"+REPLICATED_FOLDER).listFiles();
        TreeMap<Integer,FileEntry> replicatedFiles = new TreeMap<Integer, FileEntry>();
        if(files!=null){
            NamingInterface namingStub = (NamingInterface) Naming.lookup("//"+nameServerIp+"/NamingServer");
            for(File f: files){
                Neighbour owner = namingStub.getOwner(f.getName());
                NodeInterface nodeStub = (NodeInterface) Naming.lookup("//"+owner.getIp()+"/Node");
                String filename = f.getName();
                replicatedFiles.put(calculateHash(f.getName()),nodeStub.getFileEntry(filename));
            }
        }else{
            return null; //Your replicated map is empty!
        }
        return replicatedFiles;
    }

    /**
     * This function checks if the subfolder replicated, downloaded and local are present.
     * If these subfolders aren't prt, they are created
     * Also clear download folder and replicated folder
     * return true if the operation ended succesfully
     * return false if there was an error creating on of the files
     */
    private boolean initDirectories(){
        File folder = new File(rootPath.toString());
        if(!folder.exists()) {
            if(!new File(rootPath.toString()).mkdir())
                return false;
            System.out.println("Created root folder.");
        }
        File[] fileList = folder.listFiles();
        ArrayList <String> folderList = new ArrayList<String>();
        folderList.add(REPLICATED_FOLDER);
        folderList.add(DOWNLOAD_FOLDER);
        folderList.add(LOCAL_FOLDER);

        //first checks if all folders are present
        try {
            String[] files;
            for (File file : fileList) {
                switch (file.getName()) {
                    case REPLICATED_FOLDER:
                        files = file.list();
                        for (String f : files) {
                            new File(file.getPath(), f).delete();
                        }
                        folderList.remove(REPLICATED_FOLDER);
                        break;
                    case LOCAL_FOLDER:
                        folderList.remove(LOCAL_FOLDER);
                        break;
                    case DOWNLOAD_FOLDER:
                        files = file.list();
                        for (String f : files) {
                            new File(file.getPath(), f).delete();
                        }
                        folderList.remove(DOWNLOAD_FOLDER);
                        break;
                    default:
                        //do nothing
                }
            }
        } catch (NullPointerException e) {
            System.err.println("Directory initialization failed.");
        }
        if(!folderList.isEmpty()){
            for(String folderName : folderList){
                if(!new File(rootPath+"/"+folderName).mkdir())
                    return false;
                System.out.println("created "+folderName+" folder");
            }
        }
        return true;
    }

    /**
     * Delete content of given folder
     * @param folder
     */
    private static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) { //if null the folder is empty
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
    }
}
