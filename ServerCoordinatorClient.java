import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;

public class ServerCoordinatorClient {
    public static void main(String[] args) {
        try {
            // Create and export the server coordinator
            ServerCoordinatorImpl coordinator = new ServerCoordinatorImpl();

            // Start the RMI registry (use a standard port)
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("ServerCoordinator", coordinator);

            System.out.println("ServerCoordinator is running on port 1099...");

            // Register servers
            coordinator.registerServer("localhost:1100", 10, 1100);
            coordinator.registerServer("localhost:1101", 5, 1101);

            // Update server loads
            //coordinator.updateLoad("localhost:1099", 8);

            // Get least-loaded server
            int leastLoaded = coordinator.getLeastLoadedServer();
            System.out.println("Least-loaded server: " + leastLoaded);

            //String command = String.format("java MessagingServer %d", port);
            //Runtime.getRuntime().exec(command);

            // Print all server loads
            Map<Integer, Integer> serverLoads = coordinator.getServerLoads();
            System.out.println("Server loads: " + serverLoads);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
