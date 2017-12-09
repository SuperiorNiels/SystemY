package Agents;

import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import Node.Node;

public class FileAgent extends Agent {

    private TreeMap<String, Semaphore> files = new TreeMap<String, Semaphore>();
    private Node node;

    private AgentHandler handler;

    public FileAgent() {
        super(AgentType.FILE_AGENT);
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public AgentHandler getHandler() {
        return handler;
    }

    public void setHandler(AgentHandler handler) {
        this.handler = handler;
    }

    @Override
    public void run() {
        if (node != null && handler != null) {
            for (String filename : node.getOwnedFiles()) {
                if (!files.containsKey(filename)) {
                    // Add file to list, add new semaphore with one slot and first-in first-out guarantee
                    files.put(filename, new Semaphore(1, true));
                }
            }
            node.setFiles(files);
            handler.startNextAgent(this);
        }
    }
}
