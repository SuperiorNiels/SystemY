package Agents;

import NameServer.NamingInterface;
import Node.Node;
import Node.FileEntry;
import Node.Neighbour;

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

    public FailureAgent(Node node,Neighbour failing) {
        super(AgentType.FAILURE_AGENT);
        this.node = node;
        this.failingNode = calculateHash(failing.getName());
    }

    public AgentHandler getHandler() {
        return handler;
    }

    public void setHandler(AgentHandler handler) {
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            TreeMap<Integer, FileEntry> files = node.getFileFiches();
            NamingInterface namingStub = (NamingInterface) Naming.lookup("//" + node.getNameServerIp() + "/NamingServer");
            for (Map.Entry<Integer, FileEntry> file : files.entrySet()) {
                FileEntry fiche = file.getValue();
                //Check if file is owned by the failing node
                if(file.getKey() == failingNode){
                    //Change the owner
                    //For this we assume the failed node already left the system and is not listed by the naming server anymore
                    Neighbour newOwner = namingStub.getOwner(fiche.getFileName());
                    //TODO send file + update file fiche
                }else{
                    //Check if the file is replicated by the failing node
                    if(calculateHash(fiche.getReplicated().getName()) == failingNode) {
                        //Relocate the file to owner or the prev of the owner
                        Neighbour newOwner = namingStub.getOwner(fiche.getFileName());

                    }
                }
            }
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            e.printStackTrace();
        }
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

