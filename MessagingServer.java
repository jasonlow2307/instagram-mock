import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class MessagingServer extends UnicastRemoteObject implements MessagingService {
    private final List<ClientCallback> clients;

    protected MessagingServer() throws RemoteException {
        super();
        clients = new ArrayList<>();
    }

    @Override
    public void sendMessage(String message) throws RemoteException {
        System.out.println("Received message: " + message);
        // Broadcast the message to all registered clients
        for (ClientCallback client : clients) {
            client.receiveMessage(message);
        }
    }

    @Override
    public void registerClient(ClientCallback client) throws RemoteException {
        clients.add(client);
        System.out.println("New client registered: " + client);
    }

    public static void main(String[] args) {
        try {
            // Create and export the server
            MessagingServer server = new MessagingServer();
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("MessagingService", server);
            System.out.println("Messaging server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
