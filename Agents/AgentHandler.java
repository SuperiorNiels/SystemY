package Agents;

import Node.Node;
import Node.Neighbour;

import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
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

    public void startNextAgent(Agent agent) {
        if (agent.getType().equals(AgentType.FILE_AGENT)) {
            FileAgent fileAgent = (FileAgent) agent;
            // Prepare agent for rmi transport
            fileAgent.setNode(null);
            fileAgent.setHandler(null);

            // Run agent on next node
            Neighbour next = rootNode.getNext();
            if(!next.equals(new Neighbour(rootNode.getName(),rootNode.getIp()))) {
                try {
                    AgentHandlerInterface agentStub = (AgentHandlerInterface) Naming.lookup("//" + next.getIp() + "/AgentHandler");
                    agentStub.startAgent(agent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        //System.out.println("Ended.");
    }

    public void startAgent(Agent agent) {
        if (agent.getType().equals(AgentType.FILE_AGENT)) {
            FileAgent fileAgent = (FileAgent) agent;
            fileAgent.setNode(rootNode);
            fileAgent.setHandler(this);
            new Thread(agent).start();
        }
    }

    public FileAgent createNewFileAgent() {
        return new FileAgent();
    }

    public FailureAgent createNewFailureAgent(Node node,int failingNode) {
        return new FailureAgent(node,failingNode);
    }
}