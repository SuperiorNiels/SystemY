package Node;

import Agents.AgentHandler;
import Agents.FileRequest;
import GUI.GUI_Controller;
import NameServer.NamingInterface;
import Network.MulticastService;
import Network.PollingService;

import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Node implements NodeInterface, Observer {
    private Neighbour previous = null;
    private Neighbour next = null;
    private String ip = null;
    private String name = null;
    private String rootPath = "./files/";
    private String namingServerIp = null;
    private MulticastService multicast;
    private FileManager manager = new FileManager(rootPath,this);
    private boolean running = true;
    private boolean gui = false;
    private volatile boolean logged_in = false;
    private volatile boolean failedNode = false;

    // AgentHandler, handler for fileAgent and failureAgent
    private AgentHandler agentHandler;
    // Files map updates by file agent
    private TreeMap<String, FileRequest> files = new TreeMap<>();
    private GUI_Controller guicontroller;

    private ArrayList<String> filesToRemove = new ArrayList<>();

    public Node() {
        Scanner input = new Scanner(System.in);
        System.out.println("Hostname: ");
        this.name = input.nextLine();
        bootstrap();
    }

    public Node(String name,String ip, GUI_Controller guicontroller) {
        this.name = name;
        this.ip   = ip;
        this.gui = true;
        this.guicontroller = guicontroller;
    }

    public Boolean getLoggedIn() {
        return logged_in;
    }

    public TreeMap<String, FileRequest> getFiles() {
        return files;
    }

    public void setFiles(TreeMap<String, FileRequest> files) {
        this.files = files;
    }

    public boolean isRunning() {
        return running;
    }

    private void printFiles() {
        if(files.size() != 0) {
            int i = 0;
            for (String filename : files.keySet()) {
                System.out.println("File "+i+": ");
                System.out.println("\tName: "+filename);
                System.out.println("\tFile Request: " + files.get(filename).toString());
                i++;
            }
        } else {
            System.out.println("No files found!");
        }
    }

    /**
     * Start the node, this method also starts a multicast service.
     * Bootstraps the node
     */
    public void bootstrap() {
        //starts the watcher thread that watches the map with files
        try {
            multicast = null;
            // update ip
            if (!gui) {
                multicast = new MulticastService("224.0.0.1", 4446);
                ip = multicast.getIpAddress();
            }else{
                multicast = new MulticastService(this.ip,"224.0.0.1", 4446);
            }
            Neighbour self = new Neighbour(name, ip);
            //set your neighbours as yourself
            updateNode(self, self);
            //adds this node to the observers of the multicast
            multicast.addObserver(this);
            //starts the multicast thread
            multicast.start();
            startRMI();
            // Start fileManger and agentHandler
            agentHandler = new AgentHandler(this);
            manager.start();
            //sends the multicast to the network
            multicast.sendMulticast("00;" + name + ";" + ip);
            System.out.println("Node started.");

            // Start accept check for name server. When no response from the name server comes, shutdown node.
            new WaitForNameServer(this).start();

            // Start a new polling service
            new PollingService(this).start();

            while(running && !gui) {
                Scanner input = new Scanner(System.in);
                String command = input.nextLine();
                String parts[] = command.split(" ");
                if (parts[0].toLowerCase().equals("multicast")) {
                    if (parts.length != 1) {
                        multicast.sendMulticast(parts[1]);
                    } else {
                        System.err.println("Please enter a message to multicast.");
                    }
                } else if (parts[0].toLowerCase().equals("print")) {
                    try {
                        if (parts[1].toLowerCase().equals("nodes")) {
                            System.out.println("Previous: " + previous.toString());
                            System.out.println("Next: " + next.toString());
                            System.out.println("#nodes in network: " + getNumberOfNodesInNetwork());
                        } else if (parts[1].toLowerCase().equals("hash")) {
                            System.out.println(calculateHash(name));
                        } else if (parts[1].toLowerCase().equals("entries")) {
                            manager.printMap();
                        } else if (parts[1].toLowerCase().equals("files")) {
                            printFiles();
                        } else {
                            System.out.println("Enter correct parameter for what to print.");
                        }
                    } catch (Exception e) {
                        System.out.println("Enter parameter for what to print.");
                    }
                } else if (parts[0].toLowerCase().equals("shutdown")) {
                    System.out.println("shutting down.");
                    shutDown();
                    //closes the socket
                    multicast.terminate();
                    //stops SystemY process
                    System.exit(0);
                } else if (parts[0].toLowerCase().equals("fail")) {
                    failure(previous);
                } else if (parts[0].toLowerCase().equals("download")) {
                    try {
                        String filename = parts[1].toLowerCase().trim();
                        if (files.containsKey(filename)) {
                            startDownload(filename);
                        } else {
                            System.out.println("File not found in system.");
                        }
                    } catch (Exception e) {
                        System.out.println("Please enter a filename as parameter.");
                    }
                } else if(parts[0].toLowerCase().equals("open")) {
                    try {
                        String filename = parts[1].toLowerCase().trim();
                        if (files.containsKey(filename)) {
                            openFile(filename);
                        } else {
                            System.out.println("File not found in system.");
                        }
                    } catch (Exception e) {
                        System.out.println("Please enter a filename as parameter.");
                    }
                } else if(parts[0].toLowerCase().equals("owner")) {
                    try {
                        String filename = parts[1];
                        if(filename != null) {
                            NamingInterface namingStub = (NamingInterface) Naming.lookup("//"+namingServerIp+"/NamingServer");
                            Neighbour node = namingStub.getOwner(filename);
                            System.out.println("Owner: "+node.toString());                       }
                    } catch (Exception e) {
                        System.out.println("Enter filename as parameter.");
                    }
                } else if(parts[0].toLowerCase().equals("lremove")) {
                    try {
                        String filename = parts[1];
                        if(filename != null) {
                            locallyRemoveFile(filename);
                        }
                    } catch (Exception e) {
                        System.out.println("Enter filename as parameter.");
                    }
                } else if(parts[0].toLowerCase().equals("nremove")) {
                    try {
                        String filename = parts[1];
                        if(filename != null) {
                            deleteFileOwner(filename,false);
                        }
                    } catch (Exception e) {
                        System.out.println("Enter filename as parameter.");
                    }
                } else if(parts[0].toLowerCase().equals("removenode")) {
                    try {
                        String nodename = parts[1];
                        NamingInterface namingStub = (NamingInterface) Naming.lookup("//" + namingServerIp + "/NamingServer");
                        namingStub.removeNode(nodename);
                        System.out.println("Node removed from name server.");
                    } catch (RemoteException e) {
                        System.out.println("RMI to nameserver failed.");
                    } catch (NullPointerException e) {
                        System.out.println("Failed to remove node. Node not found.");
                    } catch (MalformedURLException e) {
                        System.out.println("Malformed URL");
                    } catch (NotBoundException e) {
                        System.out.println("Not bound");
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.out.println("Enter name of node to remove.");
                    }
                } else {
                    System.err.println("Command not found.");
                }
            }
        } catch (IOException e) {
            System.err.println("IOException: multicast failed.");
        }
    }

    /**
     * Method that gets executed when a multicast message is received
     * @param observable, observable
     * @param o, object
     */
    @Override
    public void update(Observable observable, Object o) {
        String message = o.toString();
        String parts[] = message.split(";");
        switch (parts[0]) {
            case "00":
                System.out.println("New node detected.");
                System.out.println("Name: " + parts[1] + " IP: " + parts[2]);
                break;
            case "01":
                System.out.println("Nameserver message received. #hosts: " + parts[1]);
                // fills in the ip of the nameserver
                namingServerIp = parts[4];
                // checks if you are the new node that just joined
                if (!name.equals(parts[2])) {
                    try {
                        updateNeighbours(parts[2], parts[3]);
                    } catch (NodeAlreadyExistsException e) {
                        System.err.println("The hash of the new node is the same as mine!");
                        // Handle error?
                    }
                } else {
                    // you are the new node that just joined
                    logged_in = true;
                    if(gui)
                        guicontroller.openWindow();
                    Neighbour self = new Neighbour(name, ip);
                    while (getNumberOfNodesInNetwork() != 0 && (previous.equals(self) || next.equals(self))) {
                        // wait till your neighbours are set
                        System.out.print("Waiting for neighbors to change... \r");
                    }
                    // initialize your filemanager
                    manager.initialize();
                }
                break;
            case "02":
                //Handle a failed node
                if (parts[1].equals("fail-detected")) {
                    System.out.print("Failed node detected. ");
                    System.out.println("Name: " + parts[2] + " IP: " + parts[3]);
                    if (getNumberOfNodesInNetwork() > 0) {
                        failedNode = true;
                    }
                } else if (parts[1].equals("fail-fixed")) {
                    failedNode = false;
                }
                break;
        }
    }

    /**
     * Starts the RMI server
     */
    private void startRMI() {
        try {
            System.setProperty("java.rmi.server.hostname",ip);
            // Start the RMI-server
            NodeInterface stub = (NodeInterface) UnicastRemoteObject.exportObject(this,0);
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.bind("Node", stub);
        } catch (RemoteException e) {
            System.err.println("Remote exception: "+e.getMessage());
        } catch (AlreadyBoundException e) {
            System.err.println("Port already bound");
        }
    }

    public void setNext(Neighbour next) {
        this.next = next;
    }

    public void setPrevious(Neighbour previous) {
        this.previous = previous;
    }

    public Neighbour getNext() {
        return this.next;
    }

    public Neighbour getPrevious() {
        return this.previous;
    }

    public String getIp() {
        return ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * gets the number of nodes
     * @return the number of nodes (without yourself = -1)
     */
    public int getNumberOfNodesInNetwork() {
        int Nodes = 0;
        try {
            NamingInterface namingStub = (NamingInterface) Naming.lookup("//"+namingServerIp+"/NamingServer");
            Nodes = namingStub.getNumberOfNodes();
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            System.err.printf("Problem with RMI when asking number of nodes.");
        }
        return Nodes-1;
    }

    /**
     * This method updates the nodes next and previous neighbours
     * and starts the method to update the replicated files when this nodes becomes the previous.
     * @param new_name, String name of the new node (received via multicast)
     * @param new_ip, String ip address of the new node
     */
    private void updateNeighbours(String new_name, String new_ip) throws NodeAlreadyExistsException {
        //multiple nodes in the network
        if(getNumberOfNodesInNetwork() > 1) {
            int my_hash = calculateHash(name);
            int myNext = calculateHash(next.getName());
            int myPrevious = calculateHash(previous.getName());
            int new_hash = calculateHash(new_name);

            if(my_hash == new_hash) throw new NodeAlreadyExistsException();

            if((new_hash < myNext  && new_hash > my_hash) || (myNext < my_hash && (my_hash < new_hash || myNext > new_hash))) {
                //I'm the previous node
                //The new node becomes your next
                //The new node will have your next as next
                //The new node will have you as previous
                //Update next with new node
                Neighbour previous_next = next;
                next = new Neighbour(new_name, new_ip);
                // update the files.
                manager.updateFilesNewNode(myNext);
                System.out.println("New node is my new next: RMI to "+new_ip);
                try {
                    NodeInterface stub = (NodeInterface) Naming.lookup("//"+new_ip+"/Node");
                    stub.updateNode(new Neighbour(name,ip), previous_next);
                }
                catch (Exception e) {
                    System.err.println("RMI to node failed.");
                }
            } else if((myPrevious < new_hash && new_hash < my_hash) || (myPrevious > my_hash && (myPrevious < new_hash || new_hash < my_hash))) {
                //I'm the next node
                //The new node becomes your previous
                //The new node will have your previous as previous
                //The new node will have you as next
                // update previous with new node
                previous = new Neighbour(new_name, new_ip);
            }
        } else {
            //Only 1 node in network, new node is next and previous.
            manager.clearEntries();
            Neighbour new_neighbour = new Neighbour(new_name, new_ip);
            Neighbour self = new Neighbour(name, ip);
            updateNode(new_neighbour, new_neighbour);
            manager.updateFilesNewNode(Integer.MAX_VALUE); //Give max integer so the check for new highest node is always false
            manager.initialize();
            System.out.println("I am the only node, new node added: RMI to "+new_ip);
            try {
                NodeInterface stub = (NodeInterface) Naming.lookup("//" + new_ip + "/Node");
                stub.updateNode(self, self);
            } catch (Exception e) {
                System.err.println("RMI to node failed.");
                //e.printStackTrace();
            }
            // Start a fileAgent
            agentHandler.startAgent(agentHandler.createNewFileAgent());
        }
    }

    /**
     * Method gets called my the updateNeighbours method, via RMI, method updates neighbors of remote node
     * @param previous, Neighbor object
     * @param next, Neighbor object
     */
    public void updateNode(Neighbour previous, Neighbour next) {
        this.next = next;
        this.previous = previous;
    }

    public String toString() {
        return "Name: "+name+" IP: "+ip;
    }

    /**
     *
     * @param ip
     * sets the ip of the nameServer
     */
    public void setNameServerIp(String ip){
        namingServerIp = ip;
    }

    String getNameServerIp(){
        return namingServerIp;
    }

    /**
     * Method that gets invoked when a graceful shutdown has to be processed.
     * Sends your next 
     */
    public void shutDown(){
        //Execute the filemanager shutdown
        manager.shutdown(previous);
        try {
            NamingInterface namingStub = (NamingInterface) Naming.lookup("//"+namingServerIp+"/NamingServer");
            if(namingStub.getNumberOfNodes()==1){
                //only one node
                namingStub.removeNode(name);
            }else{
                NodeInterface nodeStub = (NodeInterface) Naming.lookup("//"+previous.getIp()+"/Node");

                //sends the neighbour of the next Node to the previous Node
                nodeStub.setNext(next);
                //sends the neighbour of the previous node to the next Node
                nodeStub = (NodeInterface) Naming.lookup("//"+next.getIp()+"/Node");
                nodeStub.setPrevious(previous);
                //Deletes itself in the naming server
                namingStub.removeNode(name);
                //waits for threads to finish
                checkRunningThreads();

            }
        } catch (NotBoundException e) {
            System.err.println("The stub is not bound "+e.getMessage());
        } catch (MalformedURLException e) {
            System.err.println("Malformed URL: "+e.getMessage());
        } catch (RemoteException e) {
            System.err.println("Problem with RMI connection: "+e.getMessage());
        }
    }

    /**
     * Method that waits for all the running tcp threads to finish
     */
    private void checkRunningThreads(){
        for(Thread thread: manager.getThreadList()){
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Calculates the hash of a given String
     * @param name, name to calulate hash from
     * @return hashvalue
     */
    public int calculateHash(String name) {
        return Math.abs(name.hashCode() % 32768);
    }


    /**
     * Method that gets executed when a node fails in a system
     * When the failure happends, the neighbours of the next and the previous of the failed node get updated
     * @param failedNode, Neighbour
     */
    public void failure(Neighbour failedNode) {
        //Start communication with the nameserver
        try {
            NamingInterface nameServer = (NamingInterface) Naming.lookup("//"+namingServerIp+"/NamingServer");
            int numberOfNodes = nameServer.getNumberOfNodes();
            //the procedure to handel failure is different depending on how many nodes there are in the network.
            //the min numberOfNodes is 2 ( in the case when there is only 1 node failure cannot be summoned)
            if(numberOfNodes == 2){
                //in the case where there are 2 nodes and one fails the remaining node
                //gets updated, the nodes previous and next is the node itself.
                setPrevious(new Neighbour(name,ip));
                setNext(new Neighbour(name,ip));
                nameServer.removeNode(failedNode.getName());
            } else if(numberOfNodes > 2) {
                //get the previous and next of a given node
                Neighbour previous = nameServer.findPreviousNode(failedNode.getName());
                Neighbour next = nameServer.findNextNode(failedNode.getName());

                //start communication with these nodes
                NodeInterface previousCom = (NodeInterface) Naming.lookup("//" + previous.getIp() + "/Node");
                NodeInterface nextCom = (NodeInterface) Naming.lookup("//" + next.getIp() + "/Node");

                //Update the previous node, next node address with the next node
                previousCom.setNext(next);
                //Update the next node, previous next node address with the previous node
                nextCom.setPrevious(previous);
                //Remove the node at the nameserver
                nameServer.removeNode(failedNode.getName());
            }
            //Send a multicast to notify all nodes a node failed
            this.sendMulticast("02;fail-detected;" + failedNode.getName() + ";" + failedNode.getIp());

            // Start a failureAgent
            agentHandler.startAgent(agentHandler.createNewFailureAgent(failedNode));


        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            System.err.println("Error 4595 solving a failed node: ");
        }
    }

    /**
     * This method is called to check if this node detected a failed node
     * If failedNode is true, the fileAgent needs to be stopped
     * @return true/false depending on the state of failedNode
     */
    public boolean checkFailedNode(){
        return failedNode;
    }

    /**
     * This method sends a multicast
     * @param message, string
     */
    public void sendMulticast(String message){
        //Send a multicast
        multicast.sendMulticast(message);
    }

    /**
     * Function that gets called by the name server through RMI when the node can't be added
     */
    public void failedToAddNode(){
        System.err.println("Failed to add the node to the name server.");
        if(!gui) {
            /*Scanner input = new Scanner(System.in);
            System.out.println("Hostname: ");
            this.name = input.nextLine();
            bootstrap();*/
            //Causes RMI problem
            System.exit(1);
        } else {
            guicontroller.closeWithError();
        }

    }

    /**
     * RMI function to remotely create a file entry on a node
     * @param owner, Neighbour
     * @param replicated, Neighbour
     * @param local, Neighbour
     * @param fileName, String
     * @param downloads, HashSet<Neighbour>
     */
    @Override
    public void createFileEntry(Neighbour owner, Neighbour replicated, Neighbour local, String fileName, HashSet<Neighbour> downloads) {
        manager.createFileEntry(owner,replicated,local,fileName,downloads);
    }

    @Override
    public void remoteSendFile(String ip,int destPort,String srcFilePath,String fileName,String destFolder,boolean notifDownloader){
        manager.sendFile(ip,destPort,srcFilePath,fileName,destFolder,notifDownloader);
    }

    @Override
    public void moveFile(String from,String to){
        new File(from).renameTo(new File(to));
    }

    @Override
    public void remoteCheckFileEntry(String filename,Neighbour leavingNode) {
        manager.checkFileEntry(filename, leavingNode);
    }

    @Override
    public void remoteRemoveFromDownload(String filename,Neighbour leavingNode){
        manager.removeFromDownload(filename,leavingNode);
    }

    public void removeFromDownload(String filename,Neighbour leavingNode){
        manager.removeFromDownload(filename,leavingNode);
    }

    /**
     * This function returns a Treemap with all files and file fiches from the replicated and local
     * @return  treemap of fileentries
     */
    public TreeMap<Integer,FileEntry> getFileFiches(String target){
        TreeMap<Integer,FileEntry> map = null;
        try {
            map = manager.getFilesMap(target);

        } catch (RemoteException e) {
            System.err.println("Error with RMI from filemanager");
        } catch (NotBoundException e) {
            System.err.println("RMI stub not bound");
        } catch (MalformedURLException e) {
            System.err.println("Malformed url");
        }
        return map;
    }

    public void replicate(File file){
        manager.replicate(file);
    }

    /**
     * Get file entry from node via RMI (if the entry exists)
     * @param fileName, name of file
     * @return a fileentry
     * @throws NullPointerException when fileEntry does not exist
     */
    @Override
    public FileEntry getFileEntry(String fileName) throws NullPointerException {
        return manager.getFileEntry(fileName);
    }

    public TreeMap<Integer,FileEntry> getFileEntries(){
        return manager.getMap();
    }

    /**
     * Function returns a list of names of all owned files.
     * @return an arraylist with all owned file(names)
     */
    public ArrayList<String> getOwnedFiles() {
        ArrayList<String> list = new ArrayList<>();
        TreeMap<Integer, FileEntry> map = (TreeMap<Integer, FileEntry>) manager.getMap().clone();
        for(Integer entry_key : map.keySet()) {
            FileEntry entry = map.get(entry_key);
            list.add(entry.getFileName());
        }
        return list;
    }

    /**
     * This method deletes a file from the network
     * @param filename
     */
    public void deleteFileOwner(String filename,boolean shuttingDown){
        try {
            Neighbour owner = new Neighbour(name,ip);
            if(!shuttingDown){
                NamingInterface nameServer = (NamingInterface) Naming.lookup("//"+namingServerIp+"/NamingServer");
                owner = nameServer.getOwner(filename);
            }
            if(calculateHash(owner.getName())==calculateHash(this.getName())){
                //You are the owner check the file entry and delete it at all nodes
                FileEntry fiche = this.getFileEntry(filename);

                //First remove file entry
                manager.removeFileEntry(filename);

                //Second remove the file from the fileAgent
                removeFileFromFileAgent(filename);
                Thread waitForFileAgent = new Thread(new WaitForFileAgent(this,filename));
                waitForFileAgent.start();
                waitForFileAgent.join();

                //Third the replicated
                Neighbour replicated = fiche.getReplicated();
                NodeInterface replicatedStub = (NodeInterface) Naming.lookup("//" + replicated.getIp() + "/Node");
                replicatedStub.deleteFile(rootPath+"replicated/"+ filename);

                //Fourth the local
                if(!shuttingDown){
                    Neighbour local = fiche.getLocal();
                    NodeInterface localStub = (NodeInterface) Naming.lookup("//" + local.getIp() + "/Node");
                    localStub.deleteFile(rootPath+"local/"+ filename);
                }

                //Lastly the downloads
                HashSet<Neighbour> downloads = fiche.getDownloads();
                for (Neighbour next : downloads) {
                    NodeInterface downloadStub = (NodeInterface) Naming.lookup("//" + next.getIp() + "/Node");
                    downloadStub.deleteFile(rootPath + "download/" + filename);
                }
            }else{
                //You are not the owner, pass the function to the owner node
                NodeInterface ownerStub = (NodeInterface) Naming.lookup("//" + owner.getIp() + "/Node");
                ownerStub.deleteFileOwner(filename,shuttingDown);
            }
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("FileAgent never received! Restarting delete procedure...");
            this.deleteFileOwner(filename,shuttingDown);
        }
    }

    public void deleteFile(String target){
        String filename = new File(target).getName();
        FilenameFilter filter  = (dir, name) -> {
            boolean result = false;
            if(name.equals(filename))
                result = true;
            return result;
        };
        boolean result = false;
        result |= deleteFileFromFolder(rootPath+"download/",filter);
        result |= deleteFileFromFolder(rootPath+"local/",filter);
        result |= deleteFileFromFolder(rootPath+"replicated/",filter);
        if(!result){
            System.err.println("Could not delete file: not found!");
        }
    }

    /**
     * Delete a specific file from a folder, it searches the folder for the file and removes it
     * @param folder, String
     * @param filter, FilenameFilter
     * @return, false if the file was not found/ not deleted, true if the file was deleted
     */
    private boolean deleteFileFromFolder(String folder,FilenameFilter filter){
        File folderFile = new File(folder);
        String[] files  = folderFile.list(filter);
        try{
            return new File(folder + files[0]).delete();
        }catch (Exception e){
            return false;
        }
    }

    public ArrayList<String> getRequests() {
        return requests;
    }

    private ArrayList<String> requests = new ArrayList<>();

    public ArrayList<String> getDownloaded() {
        return dowloaded;
    }

    private ArrayList<String> dowloaded = new ArrayList<>();


    /**
     * The goal of this function is to ask the fileAgent for a download, when the node can download the file the download function will be called
     * @param filename String
     */
    private void startDownload(String filename) {
        requests.add(filename);
    }

    public void downloadFile(String filename) {
        try {
            requests.remove(filename);
            NamingInterface nameServer = (NamingInterface) Naming.lookup("//"+namingServerIp+"/NamingServer");
            Neighbour owner = nameServer.getOwner(filename);

            NodeInterface nodeStub = (NodeInterface) Naming.lookup("//"+owner.getIp()+"/Node");
            Neighbour download_location = nodeStub.getDownloadLocation(filename, new Neighbour(name,ip));

            nodeStub = (NodeInterface) Naming.lookup("//"+download_location.getIp()+"/Node");
            nodeStub.remoteSendFile(ip,6000,"",filename,"download", true);
        } catch (NullPointerException e) {
            System.out.println("FileAgent error: this node did not want to download the file: "+filename);
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            System.out.println("Problem with RMI in node while asking for owner of a file.");
        }
    }

    public void fileDownloaded(String filename) {
        dowloaded.add(filename);
        openFile(filename);
    }

    /**
     * Get the download location from the owner via RMI.
     * @param filename String
     * @param want_download Neighbour that wants to download the file
     * @return a neighbour = the location to download from
     */
    public Neighbour getDownloadLocation(String filename, Neighbour want_download) {
        TreeMap<Integer, FileEntry> map = manager.getMap();
        int hash = calculateHash(filename);
        if(map.containsKey(hash)) {
            // This node is owner and has fileEntry, also update the entry (add download)
            FileEntry entry = map.get(hash);
            Neighbour compare;
            if(calculateHash(entry.getLocal().getName()) == calculateHash(name)) {
                // this node is local ask replicated node for current downloads
                compare = entry.getReplicated();
            } else {
                // this node is the replicated node, ask the local for current downloads
                compare = entry.getLocal();
            }
            int number_compare = 0;
            try {
                NodeInterface nodeStub = (NodeInterface) Naming.lookup("//" + compare.getIp() + "/Node");
                number_compare = nodeStub.getCurrentNumberDownloads();
            } catch (NotBoundException | MalformedURLException | RemoteException e) {
                System.out.println("Problem with RMI to replicated node in request for download location.");
            }
            // update fileEntry downloads
            entry.addNode(want_download);
            return (number_compare >= getCurrentNumberDownloads()) ?  new Neighbour(name,ip) : compare;
        }
        return null;
    }

    /**
     * functions that returns the amount of running threads of a given node.
     * This amount can be used to determine how many files are being downloaded from a node at the same time.
     * @return amount of running threads of the node
     */
    public int getCurrentNumberDownloads() {
        return manager.getNumberOfThreadsAlive();
    }

    public void openFile(String filename) {
        try {
            if (Desktop.isDesktopSupported() && files.containsKey(filename)) {
                File local = new File(rootPath+"local/"+ filename);
                if(local.exists()){
                    System.out.println("File found in local folder.");
                    Desktop.getDesktop().open(local);
                } else {
                    File replicated = new File(rootPath+"replicated/"+ filename);
                    if(replicated.exists()) {
                        System.out.println("File found in replicated folder.");
                        Desktop.getDesktop().open(replicated);
                    } else {
                        File download = new File(rootPath+"download/" + filename);
                        if(download.exists()){
                            System.out.println("File found in download folder.");
                            if(dowloaded.contains(filename)) {
                                dowloaded.remove(filename);
                            }
                            Desktop.getDesktop().open(download);
                        } else {
                            System.out.println("File not found on node. Downloading...");
                            startDownload(filename);
                        }
                    }
                }
            }
        } catch (IOException ioe) {
            System.err.println("could not open file");
        } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
            System.out.println("Please enter a filename as parameter.");
        }
    }

    public void locallyRemoveFile(String filename) throws FileLocationException, NullPointerException {
        File local = new File(rootPath+"local/"+filename);
        File replicated = new File(rootPath+"replicated/"+filename);
        if(local.exists() || replicated.exists()) throw new FileLocationException();
        File download = new File(rootPath+"download/"+filename);
        if(download.exists()) {
            if(!download.delete()) throw new NullPointerException();
        } else {
            throw new NullPointerException();
        }
        // update owner fileEntry
        try {
            NamingInterface nameServer = (NamingInterface) Naming.lookup("//" + namingServerIp + "/NamingServer");
            Neighbour owner = nameServer.getOwner(filename);

            NodeInterface nodeStub = (NodeInterface) Naming.lookup("//" + owner.getIp() + "/Node");
            nodeStub.remoteRemoveFromDownload(filename,new Neighbour(name,ip));
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            System.out.println("Problem with RMI in node while asking for owner of a file.");
        }
    }

    public ArrayList<String> getFilesToRemove() {
        return filesToRemove;
    }

    public void removeFileFromFileAgent(String filename) {
        filesToRemove.add(filename);
    }

    public Boolean fileInSystem(String filename) {
        return files.containsKey(filename);
    }
}
