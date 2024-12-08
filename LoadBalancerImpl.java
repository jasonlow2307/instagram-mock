import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class LoadBalancerImpl extends UnicastRemoteObject implements LoadBalancer {
    private final Map<Integer, Integer> serverLoadMap;
    // store all clients and their port
    private final Map<MessagingClient, Integer> clientMap;
    private final Map<Integer, Long> idleServerTimestamps = new HashMap<>();
    private static final int SCALE_DOWN_DELAY = 10000;

    // Constructor
    protected LoadBalancerImpl() throws RemoteException {
        super();
        serverLoadMap = new HashMap<>();
        clientMap = new HashMap<>();
    }

    public void monitorAndManage() throws RemoteException {

        // iterate through all servers and print their loads
        System.out.println("Server loads: " + serverLoadMap);
        Iterator<Map.Entry<Integer, Integer>> iterator = serverLoadMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterator.next();
            int port = entry.getKey();
            int load = entry.getValue();

            // Heartbeat: Check if the server is alive
            if (!isServerAlive(port)) {
                System.err.println("Server at port " + port + " is unresponsive. Removing it.");
                iterator.remove();
                reassignClients(port);
                continue; // Skip further checks for this server
            }

            // Scale-up: Check if the server is overloaded
            if (load > LOAD_THRESHOLD) {
                System.out.println("Load on port " + port + " exceeds threshold. Scaling up...");
                int newPort = spawnNewServer();
                redistributeClients(port, newPort);
                break; // Avoid excessive scaling
            }

            // Scale-down: Check if the server is idle
            if (load == 0 && serverLoadMap.size() > 1) {
                long currentTime = System.currentTimeMillis();
                if (!idleServerTimestamps.containsKey(port)) {
                    idleServerTimestamps.put(port, currentTime);
                } else if (currentTime - idleServerTimestamps.get(port) >= SCALE_DOWN_DELAY) {
                    System.out.println("Server on port " + port + " has been idle for too long. Scaling down...");
                    killServer(port);
                    iterator.remove();
                    idleServerTimestamps.remove(port);
                }
            } else {
                idleServerTimestamps.remove(port);
            }
        }
    }
    
    // Heartbeat logic for server check
    private boolean isServerAlive(int port) {
        try {
            Registry registry = LocateRegistry.getRegistry(port);
            MessagingServer server = (MessagingServer) registry.lookup("MessagingService");
            server.ping(); // Assume `MessagingServer` has a `ping` method
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void reassignClients(int failedPort) {
        List<MessagingClient> clientsToReassign = new ArrayList<>();
        for (Map.Entry<MessagingClient, Integer> entry : clientMap.entrySet()) {
            if (entry.getValue() == failedPort) {
                clientsToReassign.add(entry.getKey());
            }
        }

        System.out.println("Reassigning clients: " + clientsToReassign);

        for (MessagingClient client : clientsToReassign) {
            try {
                int newPort = getLeastLoadedServer();
                Registry registry = LocateRegistry.getRegistry(newPort);
                MessagingServer newServer = (MessagingServer) registry.lookup("MessagingService");
                newServer.registerClient(client.toString(), client);
                client.connectToServer(newPort);

                // Update mappings
                clientMap.put(client, newPort);
                newServer.incrementLoad();
                serverLoadMap.remove(failedPort);

                System.out.println("Reassigned client to new server on port: " + newPort);
            } catch (Exception e) {
                System.err.println("Failed to reassign client: " + e.getMessage());
            }
        }
    }

    private void killServer(int port) {
        try {
            String command = String.format("java KillPortProcess %d", port);
            Runtime.getRuntime().exec(command);
            System.out.println("Executed scale-down command for server on port " + port);
        } catch (IOException e) {
            System.err.println("Failed to scale down server on port " + port + ": " + e.getMessage());
        }
    }

    @Override   
    public void addClient(MessagingClient client, int port) {
        clientMap.put(client, port);
        checkLoad();
    }

    @Override
    public void removeClient(MessagingClient client) throws RemoteException {
        System.out.println("Removed Client: " + client);
        if (clientMap.containsKey(client)) {
            int port = clientMap.get(client);
            clientMap.remove(client);

            // Decrement the load for the respective port
            serverLoadMap.put(port, serverLoadMap.get(port) - 1);

            System.out.println("Client removed. Updated load for port " + port + ": " + serverLoadMap.get(port));
        } else {
            System.out.println("Client not found in load balancer.");
        }
    }

    int LOAD_THRESHOLD = 1;
    private void checkLoad() {
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

    private int spawnNewServer() {
        try {
            // Find an available port
            int newPort = findAvailablePort();

            // get any server that is running, and sync the state to the new server
            int oldPort = serverLoadMap.keySet().iterator().next();

            // Spawn the new server (using an example command, adjust as needed)
            registerServer("localhost:newserver", 0, newPort);

            // Register the new server in the serverLoadMap with 0 clients initially
            serverLoadMap.put(newPort, 0);

            System.out.println("New server spawned on port " + newPort);

            
            MessagingServer server = (MessagingServer) LocateRegistry.getRegistry(oldPort).lookup("MessagingService");
            // server.notifyStateChange();
            System.out.println("Notified server at port " + oldPort + " to sync state to new server on port " + newPort);

            return newPort;
        } catch (IOException | NotBoundException e) {
            System.err.println("Failed to spawn new server: " + e.getMessage());
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
    public void registerServer(String address, int load, int port) throws IOException, NotBoundException {

        String command = String.format("java MessagingServerImpl %d", port);
        Runtime.getRuntime().exec(command);

        System.out.println("SERVER CREATED USING COMMAND");


        serverLoadMap.put(port, load);
        System.out.println("Registered server: " + address + " with load: " + load);
    }

    @Override
    public void updateLoad(int load, int port) throws RemoteException {
        if (serverLoadMap.containsKey(port)) {
            serverLoadMap.put(port, load);
            System.out.println("Updated server: " + port + " with load: " + load);
        } else {
            System.out.println("Server not registered: " + port);
        }
    }

    @Override
    public int getLeastLoadedServer() throws RemoteException {
        return serverLoadMap.entrySet()
                .stream()
                .min(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(0);
    }

    @Override
    public Map<Integer, Integer> getServerLoads() throws RemoteException {
        return new HashMap<>(serverLoadMap);
    }

    @Override
    public void syncServerState(int port, Map<String, Object> state) throws RemoteException {
        System.out.println("Syncing state from server at port " + port);

        // Propagate the updated state to all other servers
        for (Map.Entry<Integer, Integer> entry : serverLoadMap.entrySet()) {
            int otherPort = entry.getKey();
            if (otherPort != port) {
                try {
                    Registry registry = LocateRegistry.getRegistry(otherPort);
                    MessagingServer otherServer = (MessagingServer) registry.lookup("MessagingService");

                    otherServer.updateState(state);
                    Thread.sleep(500); // Simulate network latency
                    System.out.println("State synchronized to server at port: " + otherPort);
                } catch (Exception e) {
                    System.err.println("Failed to sync state to server at port " + otherPort + ": " + e);
                    e.printStackTrace(); // Include stack trace for debugging
                }
            }
        }
        
    }

    private void redistributeClients(int overloadedPort, int newPort) {
        System.out.println("Redistributing clients from port " + overloadedPort + " to port " + newPort);
            List<MessagingClient> clientsToMove = new ArrayList<>();

        // Find clients connected to the overloaded port
        for (Map.Entry<MessagingClient, Integer> entry : clientMap.entrySet()) {
            if (entry.getValue() == overloadedPort) {
                clientsToMove.add(entry.getKey());
                if (clientsToMove.size() >= LOAD_THRESHOLD / 2) {
                    // Move half of the load to the new server
                    break;
                }
            }
        }

        for (MessagingClient client : clientsToMove) {
            try {
                // Reassign the client to the new server
                Registry registry = LocateRegistry.getRegistry(newPort);
                MessagingServer newServer = (MessagingServer) registry.lookup("MessagingService");
                newServer.registerClient(client.toString(), client); // Update the client connection
                client.connectToServer(newPort);

                // Update load maps
                clientMap.put(client, newPort);
                newServer.incrementLoad();
                // get the old server
                MessagingServer oldServer = (MessagingServer) LocateRegistry.getRegistry(overloadedPort).lookup("MessagingService");
                oldServer.decrementLoad();
                
                System.out.println("Moved client to new server on port: " + newPort);
            } catch (Exception e) {
                System.err.println("Failed to move client to new server: " + e.getMessage());
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
            coordinator.registerServer("localhost:1101", 0, 1101);

            coordinator.monitorAndManage();

            // Update server loads
            //coordinator.updateLoad("localhost:1099", 8);

            // Get least-loaded server
//            int leastLoaded = coordinator.getLeastLoadedServer();
//            System.out.println("Least-loaded server: " + leastLoaded);

            //String command = String.format("java MessagingServer %d", port);
            //Runtime.getRuntime().exec(command);

            // Add a shutdown hook to clean up resources
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutdown initiated. Cleaning up resources...");
                synchronized (coordinator.serverLoadMap) {
                    for (int port : coordinator.serverLoadMap.keySet()) {
                        try {
                            String command = String.format("java KillPortProcess %d", port);
                            Runtime.getRuntime().exec(command);
                            System.out.println("Killed server on port: " + port);
                        } catch (IOException e) {
                            System.err.println("Failed to kill server on port " + port + ": " + e.getMessage());
                        }
                    }
                }
            }));
            // Print all server loads
            Map<Integer, Integer> serverLoads = coordinator.getServerLoads();
            System.out.println("Server loads: " + serverLoads);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
}


