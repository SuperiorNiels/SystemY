package Node;

import NameServer.NamingInterface;
import Network.MulticastService;
import Network.SendTCP;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Observable;
import java.util.Observer;
import java.util.Scanner;

public class Node implements NodeInterface, Observer {
    private Neighbour previous = null;
    private Neighbour next = null;
    private String ip = null;
    private String name = null;
    private String namingServerIp = null;
    //Amout of nodes in the network, is only actual when the node is added to the network!
    private int numberOfNodesInNetwork = 0;
    private boolean running = true;
    public Node(String name) {
        this.name = name;
    }

    /**
     * Start the node, this method also starts a multicast service.
     */
    public void start() {
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
                    System.out.println("Previous: " + previous.toString());
                    System.out.println("Next: " + next.toString());
                    System.out.println("#nodes in network: " + numberOfNodesInNetwork);
                } else if (parts[0].toLowerCase().equals("shutdown")) {
                    System.out.println("shutting down.");
                    shutDown();
                    //closes the socket
                    multicast.terminate();
                    //stops SystemY process
                    System.exit(0);
                } else if(parts[0].toLowerCase().equals("hash")) {
                    System.out.println(calculateHash(name));
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
     * Method when a multicast message is received
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
            namingServerIp = parts[4];
            setNumberOfNodesInNetwork(Integer.parseInt(parts[1]));
            if(!name.equals(parts[2])) {
                try {
                    updateNeighbors(parts[2], parts[3]);
                } catch (NodeAlreadyExistsException e) {
                    System.err.println("The has of the new node is the same as mine!");
                    // Handle error?
                }
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

    public void setNumberOfNodesInNetwork(int number) {
        this.numberOfNodesInNetwork = number;
    }

    public int getNumberOfNodesInNetwork() {
        return numberOfNodesInNetwork;
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
     * and starts the method to update the files when this nodes becomes the previous.
     * @param new_name, String name of the new node (received via multicast)
     * @param new_ip, String ip address of the new node
     */
    public void updateNeighbors(String new_name, String new_ip) throws NodeAlreadyExistsException {
        //multiple nodes in the network
        if(numberOfNodesInNetwork > 1) {
            int my_hash = calculateHash(name);
            int new_hash = calculateHash(new_name);

            if(my_hash == new_hash) throw new NodeAlreadyExistsException();

            if(new_hash < calculateHash(next.getName()) && new_hash > my_hash) {
                //I'm the previous node
                //The new node becomes your next
                //The new node will have your next as next
                //The new node will have you as previous
                System.out.println("New node is my new next: RMI to "+new_ip);
                try {
                    NodeInterface stub = (NodeInterface) Naming.lookup("//"+new_ip+"/Node");
                    stub.updateNode(new Neighbour(name,ip), next);
                }
                catch (Exception e) {
                    System.err.println("RMI to node failed.");
                }
                //Update next with new node
                next = new Neighbour(new_name, new_ip);
                //after updating the neighbours update the files.
                //updateFilesNewNode(next);.
            } else if(calculateHash(previous.getName()) < new_hash && new_hash < my_hash) {
                //I'm the next node
                //The new node becomes your previous
                //The new node will have your previous as previous
                //The new node will have you as next
                // update previous with new node
                previous = new Neighbour(new_name, new_ip);
            } else if(calculateHash(previous.getName()) > my_hash && (calculateHash(previous.getName()) < new_hash || new_hash < my_hash)){
                //You are the lowest hash, a new higher node joins update your previous
                previous = new Neighbour(new_name, new_ip);
            } else if(calculateHash(next.getName()) < my_hash && (my_hash < new_hash || calculateHash(next.getName()) < my_hash)){
                //You are currently the highest hash, but a higher joins.
                //Update him to to have you as previous and the lowest as next ( the lowest is your current next)
                try {
                    NodeInterface stub = (NodeInterface) Naming.lookup("//"+new_ip+"/Node");
                    stub.updateNode(new Neighbour(name,ip), next);
                }
                catch (Exception e) {
                    System.err.println("RMI to node failed.");
                }
                //Update next with new node
                next = new Neighbour(new_name, new_ip);
            }
        } else {
            //Only 1 node in network, new node is next and previous.
            Neighbour new_neighbour = new Neighbour(new_name, new_ip);
            Neighbour self = new Neighbour(name, ip);
            updateNode(new_neighbour, new_neighbour);
            System.out.println("I am the only node, new node added: RMI to "+new_ip);
            try {
                NodeInterface stub = (NodeInterface) Naming.lookup("//" + new_ip + "/Node");
                stub.updateNode(self, self);
            } catch (Exception e) {
                System.err.println("RMI to node failed.");
                //e.printStackTrace();
            }
        }
    }

    /**
     * Method gets called my the updateNeighbors method, via RMI, method updates neighbors of remote node
     * @param previous, Neighbor object
     * @param next, Neighbor object
     */
    public void updateNode(Neighbour previous, Neighbour next) {
        this.next = next;
        this.previous = previous;
    }

    /**
     *
     * @param ip
     * sets the ip of the nameServer
     */
    public void setNameServerIp(String ip){
        namingServerIp = ip;
    }

    /**
     * Method that gets invoked when a graceful shutdown has to be processed.
     * Sends your next 
     */
    public void shutDown(){
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
            System.err.println("The stub is not bound");
        } catch (MalformedURLException e) {
            System.err.println("Malformed URL");
        } catch (RemoteException e) {
            System.err.println("Problem with RMI connection");
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
                //in the case where there are 2 nodes and one fails the remaning node
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
                NodeInterface nextcom = (NodeInterface) Naming.lookup("//" + namingServerIp + "/Node");

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
     * Method that gets all the files from a given directory and puts them into an array
     * After that the node checks who the owner is of each file, if another node is the owner, the file gets replicated
     * if this node is the owner, the file get replicated to the previous node.
     * This node will remain the owner in the latter case.
     *
     * @param folderPath = path of the file folder
     */
    private void replicate(String folderPath){
        int destPort = 8000;
        //first checks all the files that are in the folder
        File folder = new File(folderPath);
        File [] fileList = folder.listFiles();
        if(fileList == null){
            System.out.println("Something went wrong, most likely wrong path");
        }else if(fileList.length == 0){
            System.out.println("No files were found");
        }else{
            try {
                //start RMI
                NamingInterface namingStub = (NamingInterface) Naming.lookup("//"+namingServerIp+"/NamingServer");
                //get the owner of each file
                for(File file : fileList){
                    String ownerIp = namingStub.getOwner(file.getName());
                    if(ownerIp.equals(ip)){
                        //This node is the owner of the file = replicate it to the previous node
                        sendFile(previous.getIp(),destPort,folderPath,file.getName());

                    }else{
                        //replicate it to the owner of the file
                        sendFile(ownerIp,destPort,folderPath,file.getName());

                    }
                }
            } catch (NotBoundException e) {
                System.err.println("The stub is not bound");
            } catch (MalformedURLException e) {
                System.err.println("Malformed URL");
            } catch (RemoteException e) {
                System.err.println("Problem with RMI connection");
            }
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
    public void sendFile(String ip,int destPort, String filePath,String fileName){
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
     * node checks all files he owns (via hash)
     * compares hash with new node (next)
     * if hash(file) is closer to hash next
     * send file to next, update nameserver about owner
     * @param next
     */
    public void updateFilesNewNode(Neighbour next){

        int hashNext = calculateHash(next.getName());
        int destPort = 8000;
        String pathFilesReplication = "\\filesReplication";
        //get file of all files this node owns
        File folder = new File(pathFilesReplication);
        File[] listOfFiles = folder.listFiles();
        //for every file
            for (int i = 0; i < listOfFiles.length; i++) {
                //calculate hash
                int hashFile = calculateHash(listOfFiles[i].getName());
                //if hash file >= hash next
                if(hashFile >= hashNext ){
                    //sent via tcp to next
                    sendFile(next.getIp(),destPort,listOfFiles[i].getPath(),listOfFiles[i].getName());
                    //notify nameserver that next is now owner of file
                    //this node is now download location of file
                }
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
}
