package Agents;

import Node.Neighbour;

import java.io.Serializable;
import java.util.LinkedList;

public class FileRequest implements Serializable {
    private static final long serialVersionUID = 1;
    private LinkedList<Neighbour> requested_nodes = new LinkedList<>();
    private Boolean locked = false;

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
}
