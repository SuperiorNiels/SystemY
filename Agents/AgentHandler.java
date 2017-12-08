package Agents;

import Node.Node;
import Node.Neighbour;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class AgentHandler implements AgentHandlerInterface {

    private Node rootNode;

    public AgentHandler(Node rootNode) {
        this.rootNode = rootNode;
        startRMI();
    }

    /**
     * Start RMI
     */
    private void startRMI() {
        try {
            System.setProperty("java.rmi.server.hostname",rootNode.getIp());
            AgentHandlerInterface stub = (AgentHandlerInterface) UnicastRemoteObject.exportObject(this,0);
            Registry registry = LocateRegistry.getRegistry(1099);
            registry.bind("AgentHandler", stub);
        } catch (RemoteException e) {
            System.err.println("Remote exception: "+e.getMessage());
        } catch (AlreadyBoundException e) {
            System.err.println("Port already bound");
        }
    }

    public void runAgent(Agent agent) {

        if(agent.getType() == AgentType.FILE_AGENT) {
            FileAgent fileAgent = (FileAgent) agent;
            fileAgent.setNode(rootNode);
            // Not start() so we return here when run() is finished
            agent.run();
            rootNode.setFiles(fileAgent.getFiles());

            // Run agent on next node
            Neighbour next = rootNode.getNext();
            try {
                AgentHandlerInterface agentStub = (AgentHandlerInterface) Naming.lookup("//" + next.getIp() + "/AgentHandler");
                agentStub.runAgent(agent);

            } catch (NotBoundException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }

    public FileAgent createNewFileAgent() {
        return new FileAgent();
    }
}