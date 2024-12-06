import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class LoadBalancerImpl extends UnicastRemoteObject implements LoadBalancer {
    private final Map<Integer, Integer> serverLoadMap;
    // store all clients and their port
    private final Map<MessagingClient, Integer> clientMap;

    // Constructor
    protected LoadBalancerImpl() throws RemoteException {
        super();
        serverLoadMap = new HashMap<>();
        clientMap = new HashMap<>();
    }

    @Override   
    public void addClient(MessagingClient client, int port) {
        clientMap.put(client, port);
        checkLoad();
    }

    int LOAD_THRESHOLD = 1;
    private synchronized void checkLoad() {
        System.out.println("CHECKING LOAD");
        for (Map.Entry<Integer, Integer> entry : serverLoadMap.entrySet()) {
            int port = entry.getKey();
            int load = entry.getValue();

            if (load > LOAD_THRESHOLD) {
                System.out.println("Load on port " + port + " exceeds threshold. Spawning new server...");
                spawnNewServer();
                break;
            }
        }
    }

    private void spawnNewServer() {
        try {
            // Find an available port
            int newPort = findAvailablePort();

            // Spawn the new server (using an example command, adjust as needed)
            registerServer("localhost:newserver", 0, newPort);

            // Register the new server in the serverLoadMap with 0 clients initially
            serverLoadMap.put(newPort, 0);

            System.out.println("New server spawned on port " + newPort);
        } catch (IOException e) {
            System.err.println("Failed to spawn new server: " + e.getMessage());
        } catch (NotBoundException e) {
            throw new RuntimeException(e);
        }
    }

    private int findAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort(); // Dynamically find an available port
        } catch (IOException e) {
            throw new RuntimeException("Failed to find an available port", e);
        }
    }

    @Override
    public synchronized void registerServer(String address, int load, int port) throws RemoteException, NotBoundException {

        System.setProperty("java.rmi.server.hostname", "localhost");
        LocateRegistry.createRegistry(port);
        MessagingServerImpl server = new MessagingServerImpl(port);
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

    @Override
    public synchronized void syncServerState(int port, Map<String, Object> state) throws RemoteException {
        System.out.println("Syncing state from server at port " + port);

        // Propagate the updated state to all other servers
        for (Map.Entry<Integer, Integer> entry : serverLoadMap.entrySet()) {
            int otherPort = entry.getKey();
            if (otherPort != port) {
                try {
                    Registry registry = LocateRegistry.getRegistry(otherPort);
                    MessagingServer otherServer = (MessagingServer) registry.lookup("MessagingService");

                    otherServer.updateState(state);
                    System.out.println("State synchronized to server at port: " + otherPort);
                } catch (Exception e) {
                    System.err.println("Failed to sync state to server at port " + otherPort + ": " + e);
                    e.printStackTrace(); // Include stack trace for debugging
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            // Create and export the server coordinator
            LoadBalancerImpl coordinator = new LoadBalancerImpl();

            // Start the RMI registry (use a standard port)
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("ServerCoordinator", coordinator);

            System.out.println("ServerCoordinator is running on port 1099...");

            // Register servers
            coordinator.registerServer("localhost:1100", 0, 1100);

            // Print all server loads
            Map<Integer, Integer> serverLoads = coordinator.getServerLoads();
            System.out.println("Server loads: " + serverLoads);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


