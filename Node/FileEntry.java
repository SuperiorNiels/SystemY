package Node;

import java.io.Serializable;
import java.util.HashSet;

public class FileEntry implements Serializable {
    private static final long serialVersionUID = 2;
    private Neighbour owner;
    private Neighbour replicated;
    private Neighbour local;
    private HashSet<Neighbour> downloads;
    private String fileName;

    public FileEntry(Neighbour owner, Neighbour replicated, Neighbour local,String fileName,HashSet<Neighbour> downloads) {
        this.owner = owner;
        this.replicated = replicated;
        this.local = local;
        if(downloads.size()!=0){
            this.downloads = downloads;
        }else{
            this.downloads = new HashSet<Neighbour>();
        }
        this.fileName = fileName;
    }

    public void addNode(Neighbour node) {
        downloads.add(node);
    }

    public HashSet<Neighbour> getDownloads() {
        return downloads;
    }

    public String getFileName(){
        return fileName;
    }

    public Neighbour getLocal() {
        return local;
    }

    public void setLocal(Neighbour local) {
        this.local = local;
    }

    public Neighbour getOwner() {
        return owner;
    }

    public void setOwner(Neighbour owner) {
        this.owner = owner;
    }

    public Neighbour getReplicated() {
        return replicated;
    }

    public void setReplicated(Neighbour replicated) {
        this.replicated = replicated;
    }

    public void removeNode(Neighbour node) {
        downloads.remove(node);
    }

}
