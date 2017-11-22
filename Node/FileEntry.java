package Node;

import java.util.ArrayList;

public class FileEntry {
    private String saves_local; // Name of node that has the file locally saved
    private ArrayList<String> nodes; //Map of nodes (name) that have replicated the file

    public FileEntry(String owner) {
        this.saves_local = owner;
        nodes = new ArrayList<String>();
    }

    public void addNode(String name) {
        nodes.add(name);
    }

    public void removeNode(String name) {
        nodes.remove(name);
    }

    public void setOwner(String owner) {
        this.saves_local = owner;
    }

    public String getOwner() {
        return saves_local;
    }
}
