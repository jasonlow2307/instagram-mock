import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;

public class ServerCoordinatorClient {
    public static void main(String[] args) {
        try {
            // Locate the RMI registry
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);

            // Lookup the ServerCoordinator
            ServerCoordinator coordinator = (ServerCoordinator) registry.lookup("ServerCoordinator");

            // Register servers
            coordinator.registerServer("localhost:1099", 10, 1099);
            coordinator.registerServer("localhost:1100", 5, 1100);

            // Update server loads
            //coordinator.updateLoad("localhost:1099", 8);

            // Get least-loaded server
            String leastLoaded = coordinator.getLeastLoadedServer();
            System.out.println("Least-loaded server: " + leastLoaded);

            //String command = String.format("java MessagingServer %d", port);
            //Runtime.getRuntime().exec(command);

            // Print all server loads
            Map<String, Integer> serverLoads = coordinator.getServerLoads();
            System.out.println("Server loads: " + serverLoads);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
