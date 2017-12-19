package Network;

import Agents.AgentHandlerInterface;
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
import java.util.Timer;
import java.util.TimerTask;

public class PollingService extends Thread implements PollingServiceInterface {

    private Node rootNode;

    public PollingService(Node rootnode) {
        this.rootNode = rootnode;
        startRMI();
    }

    /**
     * Start RMI
     */
    private void startRMI() {
        try {
            System.setProperty("java.rmi.server.hostname",rootNode.getIp());
            System.setProperty("sun.rmi.transport.tcp.responseTimeout","1000");
            PollingServiceInterface stub = (PollingServiceInterface) UnicastRemoteObject.exportObject(this,0);
            Registry registry = LocateRegistry.getRegistry(1099);
            registry.bind("Polling", stub);
        } catch (RemoteException e) {
            System.err.println("Remote exception: "+e.getMessage());
        } catch (AlreadyBoundException e) {
            System.err.println("Port already bound");
        }
    }

    @Override
    public void run() {
        // TODO: Add timer to this loop
        while (rootNode.isRunning()){
            Neighbour next = rootNode.getNext();
            if (!next.equals(new Neighbour(rootNode.getName(), rootNode.getIp()))) {
                try {
                    PollingServiceInterface pollingStub = (PollingServiceInterface) Naming.lookup("//" + next.getIp() + "/Polling");
                    pollingStub.pollNode();
                } catch (RemoteException | NotBoundException e) {
                    rootNode.failure(rootNode.getNext());
                } catch (MalformedURLException e) {
                    System.err.println("Malformed url in RMI fileAgent");
                }
            }
            try {
                sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Boolean pollNode() throws RemoteException {
        System.out.println("polled by previous");
        return true;
    }
}
