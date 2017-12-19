package Agents;

import java.util.TreeMap;

import Node.Node;

public class FileAgent extends Agent {

    private TreeMap<String, Boolean> files = new TreeMap<String, Boolean>();
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
        if (node != null) {
            for (String filename : node.getOwnedFiles()) {
                if (!files.containsKey(filename)) {
                    // Add file to list, add set boolean to false = not locked
                    files.put(filename, false);

                }
            }
            node.setFiles(files);
        }
        handler.startNextAgent(this);
    }

}