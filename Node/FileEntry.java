package Node;

import java.util.ArrayList;

public class FileEntry {
    private Neighbour owner;
    private Neighbour replicated;
    private Neighbour local;
    private ArrayList<Neighbour> downloads;

    public FileEntry(Neighbour owner, Neighbour replicated, Neighbour local) {
        this.owner = owner;
        this.replicated = replicated;
        this.local = local;
        downloads = new ArrayList<Neighbour>();
    }

    public ArrayList<Neighbour> getDownloads() {
        return downloads;
    }

    public void addNode(Neighbour node) {
        downloads.add(node);
    }

    public void removeNode(Neighbour node) {
        downloads.remove(node);
    }

    public void setOwner(Neighbour owner) {
        this.owner = owner;
    }

    public Neighbour getOwner() {
        return owner;
    }

    public Neighbour getReplicated() {
        return replicated;
    }

    public void setReplicated(Neighbour replicated) {
        this.replicated = replicated;
    }

    public Neighbour getLocal() {
        return local;
    }

    public void setLocal(Neighbour local) {
        this.local = local;
    }
}
