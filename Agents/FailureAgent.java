package Agents;

import NameServer.NamingInterface;
import Node.Node;
import Node.FileEntry;
import Node.Neighbour;
import Node.NodeInterface;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

public class FailureAgent extends Agent {
    private AgentHandler handler;
    private Neighbour failingNode;
    private Node node;
    private final int startNode;
    private Boolean started = false;

    public FailureAgent(Node node,Neighbour failing) {
        super(AgentType.FAILURE_AGENT);
        this.failingNode = failing;
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
        //Stop the failure agent when you are the node that started
        if(startNode == calculateHash(node.getName()) && started) {
            Thread.currentThread().interrupt();
            return;
        }
        else{
            try {
                //first check your replicated files
                TreeMap<Integer, FileEntry> replicated = node.getFileFiches("replicated");
                for (Map.Entry<Integer, FileEntry> file : replicated.entrySet()) {
                    FileEntry fiche = file.getValue();
                    int fileHash = file.getKey();
                    //if fiche null ==> fiche was owned by the failing node
                    if (fiche == null) {
                        //You are the only one with the file left ==> move to local and filemanager will take care of the replication
                        String filename = null;
                        for (Map.Entry<String, Boolean> f : node.getFiles().entrySet()) {
                            if(fileHash == calculateHash(f.getKey()))
                                filename = f.getKey();
                        }
                        if(filename!=null){
                            //Replicate the file again to make sure the copy remains in the system
                            node.replicate(new File("./files/replicated/"+filename));
                        }
                    }else if(calculateHash(fiche.getLocal().getName()) == calculateHash(failingNode.getName())) {
                        //The local node is the failed node ==> check for downloads and maybe remove the file from the system
                        //NodeInterface ownerStub = (NodeInterface) Naming.lookup("//" + fiche.getOwner().getIp() + "/Node");
                        //ownerStub.remoteCheckFileEntry(fiche.getFileName(),failingNode);
                        node.remoteCheckFileEntry(fiche.getFileName(),failingNode);
                    }
                }

                //Second check your local files
                TreeMap<Integer, FileEntry> local = node.getFileFiches("local");
                for (Map.Entry<Integer, FileEntry> file : local.entrySet()) {
                    FileEntry fiche = file.getValue();
                    int fileHash = file.getKey();
                    //if fiche null ==> fiche was owned by the failing node
                    if (fiche == null) {
                        //First find the filename of the file then replicate it
                        File replicateFile = null;
                        for (Map.Entry<String, Boolean> f : node.getFiles().entrySet()) {
                            if(fileHash == calculateHash(f.getKey()))
                                replicateFile = new File("./files/local/"+f.getKey());
                        }
                        node.replicate(replicateFile);
                    } else if (calculateHash(fiche.getReplicated().getName()) == calculateHash(failingNode.getName())) {
                        //This node is the owner, the failed node replicated because he is your previous
                        //Send file to your new previous node
                        Neighbour newReplicated = node.getPrevious();
                        NodeInterface ownerStub = (NodeInterface) Naming.lookup("//" + fiche.getOwner().getIp() + "/Node");
                        ownerStub.createFileEntry(fiche.getOwner(),newReplicated,fiche.getLocal(),fiche.getFileName(),fiche.getDownloads());
                        node.remoteSendFile(newReplicated.getIp(),6000,"./files/local",fiche.getFileName(),"replicated",false);
                    }
                }
            } catch (RemoteException e1) {
                e1.printStackTrace();
            } catch (NotBoundException e1) {
                e1.printStackTrace();
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
                System.err.println("This could be because there were only 2 nodes in the network");
            }
            //Set started true to make sure it keeps running and is stopped when it reaches the
            started = true;
            handler.startNextAgent(this);
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