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
import java.util.*;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class FileManager extends Thread {
    private Path rootPath;
    private ArrayList<Thread> threadList;
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

    public FileManager(String rootPath,  Node rootNode) {
        this.rootPath = Paths.get(rootPath);
        this.rootNode = rootNode;
        this.map = new TreeMap<Integer, FileEntry>();
        //checks if all given subfolders are present
        if(!initDirectories())
            System.out.println("There was an error creating the sub directories");
        //starts a tcp listener that listens for tcp request
        TCPListenerService TCPListener = new TCPListenerService(rootPath);
        threadList = new ArrayList<Thread>();


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
            FileEntry entry = owner_stub.getFileEntry(file.getName());

            if(entry == null) {
                // This file entry enters the system for the first time.
                owner_stub.createFileEntry(owner, replicated, new Neighbour(rootNode.getName(), rootNode.getIp()),file.getName(),new HashSet<Neighbour>());
            } else {
                // This file has already been in the system and probably has been downloaded
                owner_stub.createFileEntry(owner, replicated, new Neighbour(rootNode.getName(), rootNode.getIp()),file.getName(),entry.getDownloads());
            }
        } catch (NotBoundException e) {
            System.err.println("The stub is not bound");
        } catch (MalformedURLException e) {
            System.err.println("Malformed URL");
        } catch (RemoteException e) {
            System.err.println("Problem with RMI connection: "+nameServerIp+" in fileManager.");
        }
    }

    /**
     * RMI function to remotely create a file entry on a node
     * @param owner
     * @param replicated
     * @param local
     * @param fileName
     */
    public void createFileEntry(Neighbour owner, Neighbour replicated, Neighbour local, String fileName,HashSet<Neighbour> downloads) {
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
            //adds the threads to a list
            threadList.add(send);
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
     * This function removes all the entries from the entries map
     */
    public void clearEntries() {
        map = new TreeMap<Integer, FileEntry>();
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
                            if(new File(rootPath+"/"+ LOCAL_FOLDER+"/"+event.context().toString()).exists()
                                    && !event.context().toString().startsWith("~")) {
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
        //resets the threadList
        threadList = new ArrayList<Thread>();
        int number = rootNode.getNumberOfNodesInNetwork();
        if(number > 1){
            try {
                //First all the replicated files
                TreeMap<Integer,FileEntry> replicatedFiles = getFilesMap(REPLICATED_FOLDER);
                if(replicatedFiles!=null) {
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

                                //Send file entry to new owner node
                                //the new owner node is always your previous node
                                //the replicated node can be one of 2 options:
                                //  - your previous
                                //  - the previous of the previous

                                NamingInterface namingStub = (NamingInterface) Naming.lookup("//" + nameServerIp + "/NamingServer");
                                 NodeInterface ownerStub = null;
                                //Check if you own the the file entry so this can be moved to your previous node
                                if(fiche.getOwner().getName().equals(rootNode.getName())){
                                    ownerStub = (NodeInterface) Naming.lookup("//"+prev.getIp()+"/Node");
                                }else{
                                    ownerStub = (NodeInterface) Naming.lookup("//"+namingStub.getOwner(fiche.getFileName()).getIp()+"/Node");
                                }
                                ownerStub.createFileEntry(prev,replicated,fiche.getLocal(),fiche.getFileName(),fiche.getDownloads());
                        } catch (NotBoundException | MalformedURLException | RemoteException e) {
                            System.err.println("RMI error in filemanager shutdown!");
                        }
                    }
                }

                //Second the local files
                TreeMap<Integer,FileEntry> localFiles = getFilesMap(LOCAL_FOLDER);
                if(localFiles!=null){
                    for (Map.Entry<Integer, FileEntry> entry : localFiles.entrySet()) {
                        Integer key = entry.getKey();
                        FileEntry fiche = entry.getValue();
                        NamingInterface namingStub = (NamingInterface) Naming.lookup("//" + nameServerIp + "/NamingServer");
                        NodeInterface ownerStub = null;
                        Neighbour owner = namingStub.getOwner(fiche.getFileName());
                        //Check if you own the the file entry so this can be moved to your previous node
                        if(owner.getName().equals(rootNode.getName())){
                            //If you are the owner set ownerStub to previous and create the entry
                            ownerStub = (NodeInterface) Naming.lookup("//"+prev.getIp()+"/Node");
                            ownerStub.createFileEntry(prev,prev,fiche.getLocal(),fiche.getFileName(),fiche.getDownloads());
                        }else{
                            ownerStub = (NodeInterface) Naming.lookup("//"+owner.getIp()+"/Node");
                        }
                        //Check the entry for downloads
                        ownerStub.remoteCheckFileEntry(fiche.getFileName(),new Neighbour(rootNode.getName(),rootNode.getIp()));
                    }
                }

                //third the download map
                TreeMap<Integer,FileEntry> downloadFiles = getFilesMap(DOWNLOAD_FOLDER);
                if(downloadFiles!=null){
                    for (Map.Entry<Integer, FileEntry> entry : downloadFiles.entrySet()) {
                        Integer key = entry.getKey();
                        FileEntry fiche = entry.getValue();
                        NodeInterface ownerStub = (NodeInterface) Naming.lookup("//"+fiche.getOwner().getIp()+"/Node");
                        ownerStub.remoteRemoveFromDownload(fiche.getFileName(),new Neighbour(rootNode.getName(),rootNode.getIp()));
                    }
                }

            } catch (RemoteException | NotBoundException | MalformedURLException e) {
                System.err.println("Error shutting down your filemanager!");
            }
        }


    }

    /**
     * This function check if a file has been downloaded before,
     * if not it removes the file from the system
     * else it removes the local field from the entry
     * @param filename
     * @param leavingNode
     */
    public void checkFileEntry(String filename,Neighbour leavingNode) {
        int filehash = calculateHash(filename);
        if(map.containsKey(filehash)){ //Check if map contains the filename to be sure
            if(map.get(filehash).getDownloads().size()==0){
                //File has never been downloaded ==> remove file from system
                map.remove(filehash);
                new File(rootPath+"/"+REPLICATED_FOLDER+"/"+filename).delete();
            }else{
                //File has been downloaded
                FileEntry fiche = map.get(filehash);
                fiche.setLocal(new Neighbour(leavingNode.getName(),"Out of System"));
                map.put(filehash,fiche);
            }
        }
    }

    /**
     * This function removes a specific node from the download arraylist from a given file entry
     * @param filename
     * @param leavingNode
     */
    public void removeFromDownload(String filename,Neighbour leavingNode) {
        int filehash = calculateHash(filename);
        if(map.containsKey(filehash)){
            FileEntry fiche = map.get(filehash);
            fiche.getDownloads().remove(leavingNode);
            map.put(filehash,fiche);
        }
    }

    /**
     * node checks all files he owns (via hash)
     * compares hash with new node (next)
     * if hash(file) is closer to hash next
     * send file to next, update nameserver about owner
     */
    public void updateFilesNewNode(int nextOfNextHash ){
        Neighbour next = rootNode.getNext();
        try {
            for(Iterator<Map.Entry<Integer, FileEntry>> it = map.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Integer, FileEntry> entry = it.next();
                FileEntry fiche = entry.getValue();
                int fileHash = calculateHash(fiche.getFileName());
                int myHash = calculateHash(rootNode.getName());
                int nextHash = calculateHash(rootNode.getNext().getName());
                //Check for filehash bigger then your next's hash
                //And the special case when a new highest node joins --> he needs all the lower than the lowest node file entries
                if ((fileHash > nextHash) || (fileHash < myHash && nextOfNextHash < nextHash )){
                    if (fiche.getReplicated().getName().equals(rootNode.getName())) {
                        // First replace file
                        rootNode.moveFile(rootPath + "/" + REPLICATED_FOLDER + "/" + fiche.getFileName(), rootPath + "/" + DOWNLOAD_FOLDER + "/" + fiche.getFileName());
                        //sent via tcp to next
                        sendFile(next.getIp(), PORT, rootPath + "/" + DOWNLOAD_FOLDER, fiche.getFileName(), REPLICATED_FOLDER);
                        //this node is now download location of file
                        fiche.addNode(new Neighbour(rootNode.getName(), rootNode.getIp()));
                    } else {
                        NodeInterface nodeStub = (NodeInterface) Naming.lookup("//" + fiche.getReplicated().getIp() + "/Node");
                        // First remote replace file
                        nodeStub.moveFile(rootPath + "/" + REPLICATED_FOLDER + "/" + fiche.getFileName(), rootPath + "/" + DOWNLOAD_FOLDER + "/" + fiche.getFileName());
                        //sent remote via tcp to next
                        nodeStub.remoteSendFile(next.getIp(), PORT, rootPath + "/" + DOWNLOAD_FOLDER, fiche.getFileName(), REPLICATED_FOLDER);
                        //the next node is now download location of file
                        fiche.addNode(new Neighbour(fiche.getReplicated().getName(), fiche.getReplicated().getIp()));
                    }

                    //update fileEntry: new node becomes owner of the file
                    fiche.setOwner(next);

                    //Via RMI set update the file fiche on the owner
                    NamingInterface namingStub = (NamingInterface) Naming.lookup("//" + nameServerIp + "/NamingServer");
                    NodeInterface nodeStub = null;
                    //Check if you own the the file entry so this can be moved to your previous node
                    if(namingStub.getOwner(fiche.getFileName()).getName().equals(rootNode.getName())){
                        //When you are the owner yourself
                        //Update the your file entry
                        nodeStub = (NodeInterface) Naming.lookup("//" + rootNode.getIp() + "/Node");
                    }else{
                        //When you are not the owner update delete your file entry and create an entry on the new owner
                        nodeStub = (NodeInterface) Naming.lookup("//" + namingStub.getOwner(fiche.getFileName()).getIp() + "/Node");
                        it.remove();
                    }
                    nodeStub.createFileEntry(fiche.getOwner(), next, fiche.getLocal(), fiche.getFileName(), fiche.getDownloads());
                }
            }
        } catch (RemoteException | NotBoundException | MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * This function return the contents of your target folder with all the fileEntries
     * This funtion get the owner of each file from the namingserver
     * Then requests the file entry from each owner for every file
     * @return
     * @throws RemoteException
     * @throws NotBoundException
     * @throws MalformedURLException
     */
    public TreeMap<Integer,FileEntry> getFilesMap(String target) throws RemoteException, NotBoundException, MalformedURLException {
        File[] files = new File(rootPath+"/"+target).listFiles();
        TreeMap<Integer,FileEntry> map = new TreeMap<Integer, FileEntry>();
        if(files!=null){
            NamingInterface namingStub = (NamingInterface) Naming.lookup("//"+nameServerIp+"/NamingServer");
            for(File f: files){
                Neighbour owner = namingStub.getOwner(f.getName());
                if(!owner.getName().equals(rootNode.getName())){
                    //You are not the owner get it from the owner
                    NodeInterface nodeStub = (NodeInterface) Naming.lookup("//"+owner.getIp()+"/Node");
                    String filename = f.getName();
                    map.put(calculateHash(f.getName()),nodeStub.getFileEntry(filename));
                }else{
                    //You are the owner put your file entry
                    map.put(calculateHash(f.getName()),getFileEntry(f.getName()));
                }
            }
        }else{
            return null; //The folder is empty!
        }
        return map;
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

    public TreeMap<Integer, FileEntry> getMap() {
        return map;
    }

    public void setMap(TreeMap<Integer, FileEntry> map) {
        this.map = map;
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

    /**
     * returns a list of running threads, used when shutdown is called so that every file gets send without error
     */
    public ArrayList<Thread> getThreadList(){
        return threadList;
    }
}
