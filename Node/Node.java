package Node;

import Network.MulticastService;

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
    private int numberOfNodesInNetwork = 0;
    public Node(String ip, String name) {
        this.ip = ip;
        this.name = name;
    }

    public void start() {
        MulticastService multicast = new MulticastService("224.0.0.1",4446);
        multicast.setupService();
        multicast.sendMulticast("00;"+name+";"+ip);
        multicast.stopService();

        // Node loop
        while(true) {

        }
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
            registry.bind("Node", stub);
            System.out.println("Server ready!");
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
            next = new Neighbour(new_ip, new_name);
            try {
                Registry registry = LocateRegistry.getRegistry(next.getIp());
                NodeInterface stub = (NodeInterface) registry.lookup("Node");
                //stub.setNumberOfNodesInNetwork(map.size());
                stub = null;
                registry = null;
            }
            catch (Exception e) {
                System.out.println("RMI to node failed.");
            }
        } else if(calculateHash(previous.getName()) < new_hash && new_hash < my_hash) {
            previous = new Neighbour(new_ip, new_name);
        }
    }

    public void updateNode() {
        if(numberOfNodesInNetwork < 1) {
            next = new Neighbour(name, ip);
            previous = new Neighbour(name, ip);
        } else {

        }
    }


    private int calculateHash(String name) {
        return Math.abs(name.hashCode() % 32768);
    }
}
