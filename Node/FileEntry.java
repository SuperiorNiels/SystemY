package Node;

import java.util.ArrayList;

public class FileEntry {
    private Neighbour owner;
    private ArrayList<Neighbour> nodes; 

    public FileEntry(Neighbour owner) {
        this.owner = owner;
        nodes = new ArrayList<Neighbour>();
    }

    public ArrayList<Neighbour> getReplicatedNodes() {
        return nodes;
    }

    public void addNode(Neighbour node) {
        nodes.add(node);
    }

    public void removeNode(Node node) {
        nodes.remove(node);
    }

    public void setOwner(Neighbour owner) {
        this.owner = owner;
    }

    public void equals() {
        System.out.println("test");
    }

    public Neighbour getOwner() {
        return owner;
    }
}
