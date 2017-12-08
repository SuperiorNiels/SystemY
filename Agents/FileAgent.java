package Agents;

import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import Node.Node;

public class FileAgent extends Agent {

    private TreeMap<String, Semaphore> files = new TreeMap<String, Semaphore>();
    private AgentType type = AgentType.FILE_AGENT;
    private Node node;

    public void setNode(Node node) {
        this.node = node;
    }

    @Override
    public void run() {
        for(String filename : node.getOwnedFiles()) {
            if(!files.containsKey(filename)) {
                // Add file to list, add new semaphore with one slot and first-in first-out guarantee
                files.put(filename, new Semaphore(1, true));
            }
        }
    }

    public TreeMap<String, Semaphore> getFiles() {
        return files;
    }
}
