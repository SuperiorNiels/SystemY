package Node;

import NameServer.NamingInterface;
import NameServer.NamingServer;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Node implements NodeInterface {
    private Node previous = null;
    private Node next = null;
    private String ip = null;
    private String name = null;

    public Node(String ip, String name) {
        this.ip = ip;
        this.name = name;


    }
    /*
    * Starts the RMI server
     */
    public void startRMI(){
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

    public void setNext(Node nextNode) {
        next = nextNode;
    }

    public void setPrevious(Node previousNode) {
        previous = previousNode;
    }

    public Node getNext() {
        return next;
    }

    public Node getPrevious() {
        return previous;
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
        if(this.ip == node.ip) {
            //System.out.println("IP address already in use.");
            error = true;
        }
        if(this.name == node.name) {
            //System.out.println("Name already in use.");
            error = true;
        }
        return error;
    }

    /**
     *
     * @param new_hash
     */
    public void updateNodes(Integer new_hash) {
        int my_hash = Math.abs(this.name.hashCode() % 32768);

    }

}
