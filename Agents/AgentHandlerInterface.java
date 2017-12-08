package Agents;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AgentHandlerInterface extends Remote {
    public void runAgent(Agent agent) throws RemoteException;

}
