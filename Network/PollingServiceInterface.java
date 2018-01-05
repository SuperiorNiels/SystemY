package Network;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PollingServiceInterface extends Remote {
    Boolean pollNode() throws RemoteException;
}
