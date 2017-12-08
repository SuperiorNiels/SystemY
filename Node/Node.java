package Node;

import Agents.AgentHandler;
import NameServer.NamingInterface;
import Network.MulticastService;

import java.io.File;
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
import java.util.concurrent.Semaphore;

public class Node implements NodeInterface, Observer {
    private Neighbour previous = null;
    private Neighbour next = null;
    private String ip = null;
    private String name = null;
    private String rootPath = "./files/";
    private String namingServerIp = null;
    private FileManager manager = new FileManager(rootPath,this);
    private boolean running = true;

    // AgentHandler, handler for fileAgent and failureAgent
    private AgentHandler agentHandler;
    // Files map updates by file agent
    TreeMap<String, Semaphore> files = new TreeMap<>();

    public Node() {
        Scanner input = new Scanner(System.in);
        System.out.println("Hostname: ");
        String name = input.nextLine();
        this.name = name;
    }

    public TreeMap<String, Semaphore> getFiles() {
        return files;
    }

    public void setFiles(TreeMap<String, Semaphore> files) {
        this.files = files;
    }

    public boolean isRunning() {
        return running;
    }

    public void printFiles() {
        if(files.size() != 0) {
            for (String filename : files.keySet()) {
                System.out.println(filename + " Locked: " + files.get(filename).toString());
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
            MulticastService multicast = new MulticastService("224.0.0.1", 4446);
            // update ip
            ip = multicast.getIpAddress();
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
            Scanner input = new Scanner(System.in);
            /*
            * This part is used to test and debug
             */
            while(running) {
                String command = input.nextLine();
                String parts[] = command.split(" ");
                if (parts[0].toLowerCase().equals("multicast")) {
                    if (parts.length != 1) {
                        multicast.sendMulticast(parts[1]);
                    } else {
                        System.err.println("Please enter a message to multicast.");
                    }
                } else if (parts[0].toLowerCase().equals("print")) {
                    if(parts[1].toLowerCase().equals("nodes")) {
                        System.out.println("Previous: " + previous.toString());
                        System.out.println("Next: " + next.toString());
                        System.out.println("#nodes in network: " + getNumberOfNodesInNetwork());
                    } else if(parts[1].toLowerCase().equals("hash")) {
                        System.out.println(calculateHash(name));
                    } else if(parts[1].toLowerCase().equals("entries")) {
                        manager.printMap();
                    } else if(parts[1].toLowerCase().equals("files")) {
                        printFiles();
                    } else {
                        System.out.println("Enter parameter for what to print.");
                    }
                } else if (parts[0].toLowerCase().equals("shutdown")) {
                    System.out.println("shutting down.");
                    shutDown();
                    //closes the socket
                    multicast.terminate();
                    //stops SystemY process
                    System.exit(0);
                } else if(parts[0].toLowerCase().equals("fail")) {
                    failure(previous);
                } else {
                    System.err.println("Command not found.");
                }
            }
        }
        catch (IOException e) {
            System.err.println("IOException: multicast failed.");
        }
    }

    /**
     * Method that gets executed when a multicast message is received
     * @param observable
     * @param o
     */
    @Override
    public void update(Observable observable, Object o) {
        String message = o.toString();
        String parts[] = message.split(";");
        if(parts[0].equals("00")) {
            System.out.println("New node detected.");
            System.out.println("Name: "+parts[1]+" IP: "+parts[2]);
        } else if(parts[0].equals("01")) {
            System.out.println("Nameserver message received. #hosts: "+parts[1]);
            // fills in the ip of the nameserver
            namingServerIp = parts[4];
            // checks if you are the new node that just joined
            if(!name.equals(parts[2])) {
                try {
                    updateNeighbours(parts[2], parts[3]);
                } catch (NodeAlreadyExistsException e) {
                    System.err.println("The hash of the new node is the same as mine!");
                    // Handle error?
                }
            } else {
                // you are the new node that just joined
                Neighbour self = new Neighbour(name, ip);
                char p = ' ';
                while(getNumberOfNodesInNetwork()!=0 &&(previous.equals(self) || next.equals(self))){
                    // wait till your neighbours are set
                    System.out.print("Waiting for neighbors to change... \r");
                }
                // initialize your filemanager
                manager.initialize();
            }
        }
    }

    /**
     * Starts the RMI server
     */
    private void startRMI() {
        try {
            System.setProperty("java.rmi.server.hostname",ip);
            //Start the RMI-server
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

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * gets the number of nodes
     * @return
     */
    public int getNumberOfNodesInNetwork() {
        int Nodes = 0;
        try {
            NamingInterface namingStub = (NamingInterface) Naming.lookup("//"+namingServerIp+"/NamingServer");
                Nodes = namingStub.getNumberOfNodes();
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return Nodes-1;

    }

    /**
     * Function compares 2 nodes and returns Boolean if either name or ip are same
     * @param node the node to compare to
     * @return error, true when ip or name are equal
     */
    public Boolean equals(Node node) {
        Boolean error = false;
        if(this.ip.equals(node.ip)) {
            //System.out.println("IP address already in use.");
            error = true;
        }
        if(this.name.equals(node.name)) {
            //System.out.println("Name already in use.");
            error = true;
        }
        return error;
    }

    /**
     * This method updates the nodes next and previous neighbours
     * and starts the method to update the replicated files when this nodes becomes the previous.
     * @param new_name, String name of the new node (received via multicast)
     * @param new_ip, String ip address of the new node
     */
    public void updateNeighbours(String new_name, String new_ip) throws NodeAlreadyExistsException {
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
                manager.updateFilesNewNode();
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
            Neighbour new_neighbour = new Neighbour(new_name, new_ip);
            Neighbour self = new Neighbour(name, ip);
            updateNode(new_neighbour, new_neighbour);
            manager.updateFilesNewNode();
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
            agentHandler.runAgent(agentHandler.createNewFileAgent());
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

    public String getNameServerIp(){
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
     * Calculates the hash of a given String
     * @param name
     * @return
     */
    private int calculateHash(String name) {
        return Math.abs(name.hashCode() % 32768);
    }

    /**
     * This methode executes when a node wants to communicate with annother node
     * and the communication cannot find place because their is a problem with the other node (failedNode)
     * @param failedNode
     */
    public void failure(Neighbour failedNode) {
        //Start communication with the nameserver
        NamingInterface nameServer = null;
        try {
            nameServer = (NamingInterface) Naming.lookup("//"+namingServerIp+"/NamingServer");
            int numberOfNodes = nameServer.getNumberOfNodes();

            /*
            the procedure to handel failure is different depending on how many nodes there are in the network.
            the min numberOfNodes is 2 ( in the case when there is only 1 node failure cannot be summoned)
            */
            if(numberOfNodes == 2){
                //in the case where there are 2 nodes and one fails the remaining node
                //gets updated, the nodes previous and next is the node itself.
                setPrevious(new Neighbour(name,ip));
                setNext(new Neighbour(name,ip));

                //not sure if getName would work because failedNode cannot be accesed
                String nameFailed = failedNode.getName();
                //Remove the node at the nameserver
                nameServer.removeNode(nameFailed);
            }
            else if(numberOfNodes > 2) {
                //ask the nameServer for the previous and next node from the failedNode

                //not sure if getName would work because failedNode cannot be accesed
                String nameFailed = failedNode.getName();

                Neighbour previous = nameServer.findPreviousNode(nameFailed);
                Neighbour next = nameServer.findNextNode(nameFailed);

                //make communication with these nodes
                NodeInterface previouscom = (NodeInterface) Naming.lookup("//" + previous.getIp() + "/Node");
                NodeInterface nextcom = (NodeInterface) Naming.lookup("//" + next.getIp() + "/Node");

                //Update the previous node, next node address with the next node
                previouscom.setNext(new Neighbour(next.getName(), next.getIp()));
                //Update the next node, previous next node address with the previous node
                nextcom.setPrevious(new Neighbour(previous.getName(), previous.getIp()));
                //Remove the node at the nameserver
                nameServer.removeNode(nameFailed);
            }
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Function that gets called by the name server through RMI when the node can't be added
     */
    public void failedToAddNode(Exception e){
        System.err.println("Failed to add the node to the Nameserver");
        System.err.println("Error is caused by the following exception: "+e.getMessage());
        System.exit(1);
    }

    /**
     * RMI function to remotely create a file entry on a node
     * @param owner
     * @param replicated
     * @param local
     * @param fileName
     * @param downloads
     */
    @Override
    public void createFileEntry(Neighbour owner, Neighbour replicated, Neighbour local, String fileName, HashSet<Neighbour> downloads) {
        manager.createFileEntry(owner,replicated,local,fileName,downloads);
    }

    @Override
    public void remoteSendFile(String ip,int destPort,String srcFilePath,String fileName,String destFolder){
        manager.sendFile(ip,destPort,srcFilePath,fileName,destFolder);
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

    /**
     * Get file entry from node via RMI (if the entry exists)
     * @param fileName
     * @return
     * @throws NullPointerException when fileEntry does not exist
     */
    @Override
    public FileEntry getFileEntry(String fileName) throws NullPointerException {
        return manager.getFileEntry(fileName);
    }

    /**
     * Function returs a list of names of all owned files.
     * @return
     */
    public ArrayList<String> getOwnedFiles() {
        ArrayList<String> list = new ArrayList<>();
        TreeMap<Integer, FileEntry> map = manager.getMap();
        for(Integer entry_key : map.keySet()) {
            FileEntry entry = map.get(entry_key);
            list.add(entry.getFileName());
        }
        return list;
    }
}
