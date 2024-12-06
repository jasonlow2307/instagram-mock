import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerCoordinatorMain {
    public static void main(String[] args) {
        try {
            // Create and export the server coordinator
            ServerCoordinatorImpl coordinator = new ServerCoordinatorImpl();

            // Start the RMI registry (use a standard port)
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("ServerCoordinator", coordinator);

            System.out.println("ServerCoordinator is running on port 1099...");
        } catch (Exception e) {
            System.err.println("ServerCoordinator exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
