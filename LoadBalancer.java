import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface LoadBalancer extends Remote {
    // Register a new server with its load
    void registerServer(String address, int load, int port) throws IOException, NotBoundException;

    // Update the load of an existing server
    void updateLoad (int load, int port) throws RemoteException;

    // Get the least-loaded server
    int getLeastLoadedServer() throws RemoteException;

    // Get all registered servers and their loads (optional for debugging)
    Map<Integer, Integer> getServerLoads() throws RemoteException;

    void syncServerState(int port, Map<String, Object> state) throws RemoteException;

    void syncServerStateSequential (int port, Map<String, Object> state) throws RemoteException;

    void addClient(MessagingClient client, int port) throws RemoteException;

    void removeClient(MessagingClient client) throws RemoteException;

    void logMessage(String message) throws RemoteException;

}
