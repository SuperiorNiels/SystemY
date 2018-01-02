package Agents;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import Node.Node;
import Node.Neighbour;
import Node.NodeInterface;

public class FileAgent extends Agent {

    private TreeMap<String, FileRequest> files = new TreeMap<String, FileRequest>();
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
            // Add files from node to files map
            for (String filename : node.getOwnedFiles()) {
                if (!files.containsKey(filename)) {
                    // Add file to list, add set boolean to false = not locked
                    files.put(filename, new FileRequest());
                }
            }

            ArrayList<String> requests = node.getRequests();
            ArrayList<String> downloaded = node.getDownloaded();
            for (String name : files.keySet()) {
                // Go through map and check for download requests
                FileRequest request = files.get(name);
                if (requests.contains(name)) {
                    Neighbour current = new Neighbour(node.getName(),node.getIp());
                    if(!request.hasInQueue(current)) {request.addRequest(current); }
                }

                if(!request.getLocked()) {
                    // A node can download the file
                    // Always at least one neighbour in queue so don't need to check
                    Neighbour to_download = request.popRequest();
                    // Check if to_download is correct node else perform rmi call to let the node download the file
                    if(!(node.calculateHash(to_download.getName())== node.calculateHash(node.getName()))) {
                        // Perform RMI function
                        try {
                            NodeInterface nodeStub = (NodeInterface) Naming.lookup("//" + to_download.getIp() + "/Node");
                            nodeStub.downloadFile(name);
                        } catch (NotBoundException | MalformedURLException | RemoteException e) {
                            System.out.println("Problem with RMI to node in fileAgent.");
                        }
                    } else {
                        // The current can download the file
                        node.downloadFile(name);
                    }
                    request.lock();
                }

                // Check for the downloaded files in the node map (so we can clear the lock)
                if(downloaded.contains(name)) {
                    // the current node has downloaded the file, remove the lock
                    request.unlock();
                }
            }

            ArrayList<String> to_remove = node.getFilesToRemove(); // Check files to_remove from file
            ArrayList<String> remove_from_list = new ArrayList<>();
            for(Iterator<String> it = to_remove.iterator(); it.hasNext(); ) {
                String file = it.next();
                if(files.containsKey(file)) {
                    files.remove(file);
                    remove_from_list.add(file);
                }
            }

            to_remove.removeAll(remove_from_list);

            node.setFiles(files);
        }

        //check if a failed node was detected; if so don't start the next agent
        if(!node.checkFailedNode())
            handler.startNextAgent(this);
    }
}