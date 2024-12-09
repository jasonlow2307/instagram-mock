import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LoadBalancerImpl extends UnicastRemoteObject implements LoadBalancer {
    private final ConcurrentHashMap<Integer, Integer> serverLoadMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> idleServerTimestamps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<MessagingClient, Integer> clientMap = new ConcurrentHashMap<>();
    private static final int SCALE_DOWN_DELAY = 10000;

    private final ReentrantLock serverLoadMapLock = new ReentrantLock();
    private final ReentrantLock clientMapLock = new ReentrantLock();

    // Constructor
    protected LoadBalancerImpl() throws RemoteException {
        super();
    }

    public void monitorAndManage() {
        new Thread(() -> {
            while (true) {
                try {
                    // Sleep interval between monitoring cycles
                    Thread.sleep(5000); // 5 seconds monitoring interval
    
                    // Attempt to acquire locks with timeout
                    if (serverLoadMapLock.tryLock(1, TimeUnit.SECONDS) && 
                        clientMapLock.tryLock(1, TimeUnit.SECONDS)) {
                        try {
                            // Print current server loads
                            System.out.println("Server loads: " + serverLoadMap);
    
                            // Use an iterator to safely modify the map during iteration
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
                                    
                                    // Track idle server timestamp
                                    idleServerTimestamps.compute(port, (k, existingTimestamp) -> {
                                        if (existingTimestamp == null) {
                                            return currentTime;
                                        }
                                        
                                        // Check if idle time exceeds threshold
                                        if (currentTime - existingTimestamp >= SCALE_DOWN_DELAY) {
                                            System.out.println("Server on port " + port + " has been idle for too long. Scaling down...");
                                            killServer(port);
                                            iterator.remove();
                                            return null; // Remove timestamp
                                        }
                                        
                                        return existingTimestamp;
                                    });
                                } else {
                                    // Remove idle timestamp if server becomes active
                                    idleServerTimestamps.remove(port);
                                }
                            }
                        } finally {
                            // Always release locks in the correct order
                            clientMapLock.unlock();
                            serverLoadMapLock.unlock();
                        }
                    } else {
                        // Log lock acquisition failure
                        System.err.println("Could not acquire locks for monitoring");
                    }
                } catch (InterruptedException e) {
                    System.err.println("Monitor thread interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
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

        for (MessagingClient client : clientsToReassign) {
            try {
                int newPort = getLeastLoadedServer();
                Registry registry = LocateRegistry.getRegistry(newPort);
                MessagingServer newServer = (MessagingServer) registry.lookup("MessagingService");
                client.connectToServer(newPort);

                // Update mappings
                clientMap.put(client, newPort);
                newServer.incrementLoad();

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
            server.notifyStateChange(true);
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
        synchronized (serverLoadMap){
            return new HashMap<>(serverLoadMap);
        }
    }

    @Override
    public void syncServerState(int port, Map<String, Object> state) throws RemoteException {
        try {
            // Attempt to acquire server load map lock with timeout
            if (serverLoadMapLock.tryLock(5, TimeUnit.SECONDS)) {
                try {
                    // Create a thread-safe copy of server load map
                    Map<Integer, Integer> localServerLoadMap = new HashMap<>(serverLoadMap);

                    // Use ExecutorService for parallel, controlled synchronization
                    ExecutorService executorService = Executors.newFixedThreadPool(
                        Math.min(localServerLoadMap.size(), Runtime.getRuntime().availableProcessors())
                    );

                    // Collect futures to handle potential sync failures
                    List<Future<?>> syncFutures = new ArrayList<>();

                    for (Map.Entry<Integer, Integer> entry : localServerLoadMap.entrySet()) {
                        int otherPort = entry.getKey();
                        if (otherPort != port) {
                            Future<?> future = executorService.submit(() -> {
                                try {
                                    // Locate RMI registry for the server
                                    Registry registry = LocateRegistry.getRegistry(otherPort);
                                    MessagingServer otherServer = (MessagingServer) registry.lookup("MessagingService");

                                    // Implement a timeout mechanism for state update
                                    CompletableFuture<Void> syncTask = CompletableFuture.runAsync(() -> {
                                        try {
                                            otherServer.updateState(state);
                                            System.out.println("[Concurrent] State synchronized to server at port: " + otherPort);
                                        } catch (RemoteException e) {
                                            throw new CompletionException(e);
                                        }
                                    }).orTimeout(5, TimeUnit.SECONDS);

                                    // Wait for the task to complete or timeout
                                    syncTask.join();
                                } catch (Exception e) {
                                    System.err.println("Failed to sync state to server at port " + otherPort + ": " + e);
                                    // Log the error but continue with other servers
                                }
                            });
                            syncFutures.add(future);
                        }
                    }

                    // Wait for all sync tasks to complete
                    for (Future<?> future : syncFutures) {
                        try {
                            future.get(5, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            System.err.println("Sync task failed: " + e);
                        }
                    }

                    // Shutdown the executor service
                    executorService.shutdown();
                } finally {
                    // Always release the lock
                    serverLoadMapLock.unlock();
                }
            } else {
                System.err.println("Could not acquire lock for state synchronization");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RemoteException("Synchronization interrupted", e);
        }
    }

    @Override
    public void syncServerStateSequential (int port, Map<String, Object> state) throws RemoteException {
        // Iterate through serverLoadMap sequentially
        for (Map.Entry<Integer, Integer> entry : serverLoadMap.entrySet()) {
            int otherPort = entry.getKey();
            if (otherPort != port) {
                try {
                    // Locate RMI registry for the target server
                    Registry registry = LocateRegistry.getRegistry(otherPort);
                    MessagingServer otherServer = (MessagingServer) registry.lookup("MessagingService");
    
                    // Synchronize state with the target server
                    otherServer.updateState(state);
                    System.out.println("[Sequential] State synchronized to server at port: " + otherPort);
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

    @Override
    public void logMessage(String message) throws RemoteException {
        System.out.println("[Server Log] " + message);
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


