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

    public void monitorAndScaleDown() {
        new Thread(() -> {
            while (true) {
                try {
                    System.out.println("CHECKING FOR SCALE DOWN");
                    Thread.sleep(1000); // Check interval
                    synchronized (serverLoadMap) {
                        Iterator<Map.Entry<Integer, Integer>> iterator = serverLoadMap.entrySet().iterator();
                        while (iterator.hasNext()) {
                            Map.Entry<Integer, Integer> entry = iterator.next();
                            int port = entry.getKey();
                            int load = entry.getValue();

                            if (load == 0 && serverLoadMap.size() > 1) {
                                long currentTime = System.currentTimeMillis();
                                if (!idleServerTimestamps.containsKey(port)) {
                                    idleServerTimestamps.put(port, currentTime);
                                } else if (currentTime - idleServerTimestamps.get(port) >= SCALE_DOWN_DELAY) {
                                    System.out.println("Server on port " + port + " has been idle for 10 seconds. Scaling down...");
                                    killServer(port);
                                    iterator.remove();
                                    idleServerTimestamps.remove(port);
                                }
                            } else {
                                idleServerTimestamps.remove(port);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    System.err.println("Monitor thread interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
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
    public synchronized void removeClient(MessagingClient client) throws RemoteException {
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
    public synchronized void registerServer(String address, int load, int port) throws IOException, NotBoundException {

        String command = String.format("java MessagingServerImpl %d", port);
        Runtime.getRuntime().exec(command);

        System.out.println("SERVER CREATED USING COMMAND");


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
            coordinator.registerServer("localhost:1101", 0, 1101);

            coordinator.heartbeat();
            coordinator.monitorAndScaleDown();

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

            // Start a thread to listen for user input
            new Thread(() -> {
                Scanner scanner = new Scanner(System.in);
                while (true) {
                    System.out.println("Enter a command (e.g., 'load' to print server loads):");
                    String command = scanner.nextLine();

                    if ("load".equalsIgnoreCase(command)) {
                        try {
                            Map<Integer, Integer> serverLoads = coordinator.getServerLoads();
                            System.out.println("Current server loads:");
                            for (Map.Entry<Integer, Integer> entry : serverLoads.entrySet()) {
                                System.out.println("Port: " + entry.getKey() + ", Load: " + entry.getValue());
                            }
                        } catch (RemoteException e) {
                            System.err.println("Error fetching server loads: " + e.getMessage());
                        }
                    } else {
                        System.out.println("Unknown command. Try again.");
                    }
                }
            }).start();

            // Print all server loads
            Map<Integer, Integer> serverLoads = coordinator.getServerLoads();
            System.out.println("Server loads: " + serverLoads);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void heartbeat() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000); // Heartbeat interval (5 seconds)
                    synchronized (serverLoadMap) {
                        Iterator<Map.Entry<Integer, Integer>> iterator = serverLoadMap.entrySet().iterator();
                        while (iterator.hasNext()) {
                            Map.Entry<Integer, Integer> entry = iterator.next();
                            int port = entry.getKey();
    
                            // Check if the server is alive
                            try {
                                Registry registry = LocateRegistry.getRegistry(port);
                                MessagingServer server = (MessagingServer) registry.lookup("MessagingService");
                                server.ping(); // Assume `MessagingServer` has a ping method for heartbeat
                            } catch (Exception e) {
                                System.err.println("Server at port " + port + " is unresponsive. Removing it.");
                                iterator.remove();
    
                                // Reassign clients connected to this server
                                reassignClients(port);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    System.err.println("Heartbeat interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }
    
    private void reassignClients(int failedPort) {
        synchronized (clientMap) {
            Iterator<Map.Entry<MessagingClient, Integer>> iterator = clientMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<MessagingClient, Integer> entry = iterator.next();
                MessagingClient client = entry.getKey();
                int clientPort = entry.getValue();
    
                if (clientPort == failedPort) {
                    try {
                        int newPort = getLeastLoadedServer();
                        if (newPort != 0) { // Ensure a server is available
                            Registry registry = LocateRegistry.getRegistry(newPort);
                            MessagingServer newServer = (MessagingServer) registry.lookup("MessagingService");
                            // Update the client connection
                            newServer.registerClient(client.toString(), client); // Assuming toString is overridden for unique IDs
                            client.connectToServer(newPort);
                            clientMap.put(client, newPort);
                            System.out.println("Reassigned client to new server at port: " + newPort);
                        } else {
                            System.err.println("No available servers to reassign client.");
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to reassign client: " + e.getMessage());
                    }
                }
            }
        }
    }
}


