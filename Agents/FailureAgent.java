package Agents;

import NameServer.NamingInterface;
import Node.Node;
import Node.FileEntry;
import Node.Neighbour;
import Node.NodeInterface;

import java.io.File;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

public class FailureAgent extends Agent {

    private AgentHandler handler;
    private int failingNode;
    private Node node;
    private final int startNode;

    public FailureAgent(Node node,Neighbour failing) {
        super(AgentType.FAILURE_AGENT);
        this.failingNode = calculateHash(failing.getName());
        this.startNode = calculateHash(node.getName());
    }

    public void setHandler(AgentHandler handler) {
        this.handler = handler;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    @Override
    public void run() {
        try {
            TreeMap<Integer, FileEntry> files = node.getFileFiches();
            NamingInterface namingStub = (NamingInterface) Naming.lookup("//" + node.getNameServerIp() + "/NamingServer");
            for (Map.Entry<Integer, FileEntry> file : files.entrySet()) {
                FileEntry fiche = file.getValue();
                if (fiche == null) {
                    //The file entry was not found ==> the failed node had it
                    //Create new file fiche maybe via replicte method?
                    Neighbour newOwner = namingStub.getOwner(fiche.getFileName());
                    NodeInterface nodeStub = (NodeInterface) Naming.lookup("//" + newOwner.getIp() + "/Node");
                   // nodeStub.createFileEntry(newOwner, , fiche.getLocal(), fiche.getFileName(), fiche.getDownloads());
                } else {
                    //the fiche does exist
                    //check if failed node replicated or has local
                    if (calculateHash(fiche.getReplicated().getName()) == failingNode) {
                        //Check if file is owned by the failing node

                    } else {
                        //Check if the file is replicated by the failing node
                        if (calculateHash(fiche.getReplicated().getName()) == failingNode) {
                            //Relocate the file to owner or the prev of the owner
                            Neighbour newOwner = namingStub.getOwner(fiche.getFileName());
                        }
                    }
                }
            }
        } catch (RemoteException e1) {
            e1.printStackTrace();
        } catch (NotBoundException e1) {
            e1.printStackTrace();
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
        handler.startNextAgent(this);
    }

    /**
     * Calculates the hash of a given String
     * @param name
     * @return
     */
    private int calculateHash(String name) {
        return Math.abs(name.hashCode() % 32768);
    }
}

