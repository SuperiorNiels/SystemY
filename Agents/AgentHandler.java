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
            System.setProperty("sun.rmi.transport.tcp.responseTimeout","1000");
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
            if (next == null) { next = new Neighbour(rootNode.getName(),rootNode.getIp()); }
            if(!next.equals(new Neighbour(rootNode.getName(),rootNode.getIp()))) {
                try {
                    AgentHandlerInterface agentStub = (AgentHandlerInterface) Naming.lookup("//" + next.getIp() + "/AgentHandler");
                    agentStub.startAgent(agent);
                } catch (RemoteException | NotBoundException e) {
                    System.out.println("RMI to next node failed for fileAgent.");
                    System.out.println(e.getMessage());
                    System.out.println(e.getCause());
                    // run the agent again on this node
                    if(!e.getMessage().getClass().isInstance("SocketTimeoutException")) {
                        System.out.println("Starting new fileAgent.");
                        startAgent(agent);
                    }
                } catch (MalformedURLException e) {
                    System.err.println("Malformed url in RMI fileAgent");
                }
            } else {
                // run agent again on self (not using RMI to be sure)
                startAgent(agent);
            }
        } else if (agent.getType().equals(AgentType.FAILURE_AGENT)) {
            FailureAgent failureAgent = (FailureAgent) agent;
            // Prepare agent for rmi transport
            failureAgent.setNode(null);
            failureAgent.setHandler(null);

            // Run agent on previous node
            Neighbour previous = rootNode.getPrevious();
            if (previous == null) { previous = new Neighbour(rootNode.getName(),rootNode.getIp()); }
            if(!previous.equals(new Neighbour(rootNode.getName(),rootNode.getIp()))) {
                try {
                    AgentHandlerInterface agentStub = (AgentHandlerInterface) Naming.lookup("//" + previous.getIp() + "/AgentHandler");
                    agentStub.startAgent(agent);
                } catch (RemoteException | MalformedURLException | NotBoundException e) {
                    System.err.println("RMI failed in failureAgent");
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
        } else if (agent.getType().equals(AgentType.FAILURE_AGENT)) {
            FailureAgent failureAgent = (FailureAgent) agent;
            failureAgent.setNode(rootNode);
            failureAgent.setHandler(this);
            new Thread(agent).start();
        }
    }

    public FileAgent createNewFileAgent() {
        return new FileAgent();
    }

    public FailureAgent createNewFailureAgent(Neighbour failedNode) {
        return new FailureAgent(rootNode, failedNode);
    }
}