package Node;

import NameServer.NamingInterface;
import Network.MulticastObserverable;
import Network.MulticastService;

import java.io.IOException;
import java.net.MalformedURLException;
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
            ip = multicast.getIpAddress();
            multicast.addObserver(this);
            multicast.start();
            //startRMI();
            multicast.sendMulticast("00;" + name + ";" + ip);
            System.out.println("Node started.");
            Scanner input = new Scanner(System.in);
            while(running) {
                String command = input.nextLine();
                String parts[] = command.split(" ");
                if(parts[0].toLowerCase().equals("multicast")) {
                    if (parts.length != 1) {
                        multicast.sendMulticast(parts[1]);
                    } else {
                        System.out.println("Please enter a message to multicast.");
                    }
                } else {
                    System.out.println("Command not found.");
                }
            }
        }
        catch (IOException e) {
            System.out.println("IOException: multicast failed.");
        }

    }

    /**
     * Method when a multicast message is recieved
     * @param observable
     * @param o
     */
    @Override
    public void update(Observable observable, Object o) {
        String message = o.toString();
        System.out.println(message);
    }

    /*
    * Starts the RMI server
     */
    private void startRMI() {
        try {
            //Start the RMI-server
            Node node = this;
            NodeInterface stub = (NodeInterface) UnicastRemoteObject.exportObject(node,0);
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.bind("Node", stub);
            //System.out.println("Server ready!");
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
     * This method updates the nodes next en previous ip addresses
     * @param new_name, String name of the new node (recieved via multicast)
     * @param new_ip, String ip address of the new node
     */
    public void updateNeighbors(String new_name, String new_ip) throws NodeAlreadyExistsException {
        int my_hash = calculateHash(name);
        int new_hash = calculateHash(new_name);

        if(my_hash == new_hash) throw new NodeAlreadyExistsException();

        if(my_hash < new_hash && new_hash < calculateHash(next.getName())) {
            // Update new node neighbours previous = self and next = self next
            try {
                NodeInterface stub = (NodeInterface) Naming.lookup("//"+next.getIp()+"/Node");
                stub.updateNode(new Neighbour(name,ip), next);
            }
            catch (Exception e) {
                System.out.println("RMI to node failed.");
            }
            // update next with new node
            next = new Neighbour(new_ip, new_name);
        } else if(calculateHash(previous.getName()) < new_hash && new_hash < my_hash) {
            // update previous with new node
            previous = new Neighbour(new_ip, new_name);
        }
    }

    public void updateNode(Neighbour previous, Neighbour next) {
        if(numberOfNodesInNetwork < 1) {
            this.next = new Neighbour(name, ip);
            this.previous = new Neighbour(name, ip);
        } else {
            this.next = next;
            this.previous = previous;
        }
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
            //sends the neighbour of the next Node to the previous Node
            NodeInterface nodeStub = (NodeInterface) Naming.lookup("//"+previous.getIp()+"/Node");
            nodeStub.setNext(next);
            //sends the neighbour of the previous node to the next Node
            nodeStub = (NodeInterface) Naming.lookup("//"+next.getIp()+"/Node");
            nodeStub.setPrevious(previous);
            //Deletes itself by the naming server
            NamingInterface namingStub = (NamingInterface) Naming.lookup("//"+namingServerIp+"/NamingServer");
            namingStub.removeNode(name);
            //stops the SystemY process
            running = false;
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    private int calculateHash(String name) {
        return Math.abs(name.hashCode() % 32768);
    }

    /**
     * This methode executes when a node wants to communicate with annother node
     * and the communication cannot find place because their is a problem with the other node (failedNode)
     * @param failedNode
     */
    public void failure(Node failedNode) {
        //Start communication with the nameserver
        NamingInterface nameServer = null;
        try {
            nameServer = (NamingInterface) Naming.lookup("//"+namingServerIp+"/NamingServer");

            //ask the nameServer for the previous and next node from the failedNode

            //not sure if getName would work because failedNode cannot be accesed
            String nameFailed = failedNode.getName();

            Node previous = nameServer.findPreviousNode(nameFailed);
            Node next     = nameServer.findNextNode(nameFailed);
            //Update the previous node, next node address with the next node
            previous.setNext(new Neighbour(next.getName(),next.getIp()));
            //Update the next node, previous next node address with the previous node
            next.setPrevious(new Neighbour(previous.getName(),previous.getIp()));
            //Verwijder de node bij de nameserver.
            nameServer.removeNode(nameFailed);
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
