package Node;


import NameServer.NamingInterface;
import com.sun.corba.se.impl.naming.pcosnaming.NameServer;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Node implements NodeInterface {
    private Neighbour previous = null;
    private Neighbour next = null;
    private String ip = null;
    private String name = null;
    public Node(String ip, String name) {
        this.ip = ip;
        this.name = name;
    }

    /*
    * Starts the RMI server
     */
    public void startRMI() {
        try {
            //Start the RMI-server
            Node node = this;
            NodeInterface stub = (NodeInterface) UnicastRemoteObject.exportObject(node,0);
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.bind(name, stub);
            System.out.println("Server ready!");
        } catch (RemoteException e) {
            System.err.println("Remote exception: "+e.getMessage());
        } catch (AlreadyBoundException e) {
            System.err.println("Port already bound");
        }
    }

    /**
     *
     * @param name
     * @param ip
     * @return
     */
    public Object startCommunication(String name, String ip){
        NodeInterface commNode = null;
        try {
            //Gets the bank object
            Registry registry = LocateRegistry.getRegistry(ip);
            //import the stub
            commNode= (NodeInterface) registry.lookup(name);
        }catch (RemoteException e) {
            System.out.println("Problem connecting to the RMI server: " + e.getMessage());
        }catch (NotBoundException e) {
            System.out.println("Problem binding a registry to a stub: " + e.getMessage());
        }
        return commNode;
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
    public void updateNodes(String new_name, String new_ip) throws NodeAlreadyExistsException {
        int my_hash = calculateHash(name);
        int new_hash = calculateHash(new_name);

        if(my_hash == new_hash) throw new NodeAlreadyExistsException();

        if(my_hash < new_hash && new_hash < calculateHash(next.getName())) {
            //next = new Node(new_ip, new_name);
            //update new node
        } else if(calculateHash(previous.getName()) < new_hash && new_hash < my_hash) {
            //previous = new Node(new_ip, new_name);
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
    public void failure(Node failedNode){
        /*
        //Start communication with the nameserver
        String ip = "10.0.0.1" ;
        Object nameserver = startCommunication("NameServer", ip);
        //ask the nameServer for the previous and next node from the failedNode

        //not sure if getName would work because failedNode cannot be accesed
        String name = failedNode.getName();
        
        Node previous = nameserver.findPreviousNode(name);
        Node next     = nameserver.findNextNode(name);
        //Update the previous node, next node address with the next node
        previous.setNext(next);
        //Update the next node, previous next node address with the previous node
        next.setPrevious(previous);
        //Verwijder de node bij de nameserver.
        nameserver.remove(failedNode);
        */
    }
}
