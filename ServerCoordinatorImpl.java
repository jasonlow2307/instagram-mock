import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ServerCoordinatorImpl extends UnicastRemoteObject implements ServerCoordinator {
    private final Map<Integer, Integer> serverLoadMap;

    // Constructor
    protected ServerCoordinatorImpl() throws RemoteException {
        super();
        serverLoadMap = new HashMap<>();
    }

    @Override
    public synchronized void registerServer(String address, int load, int port) throws RemoteException, NotBoundException {

        System.setProperty("java.rmi.server.hostname", "localhost");
        LocateRegistry.createRegistry(port);
        MessagingServer server = new MessagingServer(port);
        Registry registry = LocateRegistry.getRegistry(port);
        registry.rebind("MessagingService", server);
        serverLoadMap.put(port, load);
        System.out.println("Registered server: " + address + " with load: " + load);
    }

    @Override
    public synchronized void updateLoad(int load, int port) throws RemoteException {
        if (serverLoadMap.containsKey(port)) {
            serverLoadMap.put(port, load);
            System.out.println("Updated server: " + port + " with load: " + load);
        } else {
            System.out.println("Server not registered: " + port);
        }
    }

    @Override
    public synchronized int getLeastLoadedServer() throws RemoteException {
        return serverLoadMap.entrySet()
                .stream()
                .min(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(0);
    }

    @Override
    public synchronized Map<Integer, Integer> getServerLoads() throws RemoteException {
        return new HashMap<>(serverLoadMap);
    }
}


