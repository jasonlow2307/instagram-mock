import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ServerCoordinatorImpl extends UnicastRemoteObject implements ServerCoordinator {
    private final Map<String, Integer> serverLoadMap;

    // Constructor
    protected ServerCoordinatorImpl() throws RemoteException {
        super();
        serverLoadMap = new HashMap<>();
    }

    @Override
    public synchronized void registerServer(String address, int load, int port) throws RemoteException {
        MessagingServer server = new MessagingServer(port);
        Registry registry = LocateRegistry.getRegistry(port);
        registry.rebind("MessagingService", server);
        serverLoadMap.put(address, load);
        System.out.println("Registered server: " + address + " with load: " + load);
    }

    @Override
    public synchronized void updateLoad(String address, int load) throws RemoteException {
        if (serverLoadMap.containsKey(address)) {
            serverLoadMap.put(address, load);
            System.out.println("Updated server: " + address + " with load: " + load);
        } else {
            System.out.println("Server not registered: " + address);
        }
    }

    @Override
    public synchronized String getLeastLoadedServer() throws RemoteException {
        return serverLoadMap.entrySet()
                .stream()
                .min(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    @Override
    public synchronized Map<String, Integer> getServerLoads() throws RemoteException {
        return new HashMap<>(serverLoadMap);
    }
}


