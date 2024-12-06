import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface ServerCoordinator extends Remote {
    // Register a new server with its load
    void registerServer(String address, int load, int port) throws RemoteException;

    // Update the load of an existing server
    void updateLoad(String address, int load, int port) throws RemoteException;

    // Get the least-loaded server
    int getLeastLoadedServer() throws RemoteException;

    // Get all registered servers and their loads (optional for debugging)
    Map<Integer, Integer> getServerLoads() throws RemoteException;
}
