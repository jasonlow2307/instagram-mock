import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface ServerCoordinator extends Remote {
    // Register a new server with its load
    void registerServer(String address, int load) throws RemoteException;

    // Update the load of an existing server
    void updateLoad(String address, int load) throws RemoteException;

    // Get the least-loaded server
    String getLeastLoadedServer() throws RemoteException;

    // Get all registered servers and their loads (optional for debugging)
    Map<String, Integer> getServerLoads() throws RemoteException;
}
