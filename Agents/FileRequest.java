package Agents;

import Node.Neighbour;

import java.io.Serializable;
import java.util.LinkedList;

public class FileRequest implements Serializable {
    private static final long serialVersionUID = 1;
    private LinkedList<Neighbour> requested_nodes = new LinkedList<>();
    private boolean locked = false;

    public FileRequest() { }

    public void addRequest(Neighbour node) {
        requested_nodes.add(node);
    }

    public Neighbour popRequest() {
        return requested_nodes.pop();
    }

    public void lock() {
       locked = true;
    }

    public void unlock() {
        locked = false;
    }

    public Boolean getLocked() {
        return locked;
    }

    public Boolean hasInQueue(Neighbour node) {
        return requested_nodes.contains(node);
    }

    public String toString() {
        String to_return = "\n\t\tLocked: "+locked+"\n\t\tNodes in queue:\n";
        for(Neighbour in_queue : requested_nodes) {
            to_return += "\t\t\t"+in_queue.toString();
        }
        return to_return;
    }
}
