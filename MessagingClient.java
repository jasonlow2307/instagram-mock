import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

public class MessagingClient extends UnicastRemoteObject implements ClientCallback {
    protected MessagingClient() throws RemoteException {
        super();
    }

    @Override
    public void receiveMessage(String message) throws RemoteException {
        System.out.println("New message: " + message);
    }

    public static void main(String[] args) {
        try {
            // Connect to the server
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            MessagingService server = (MessagingService) registry.lookup("MessagingService");

            // Register the client
            MessagingClient client = new MessagingClient();
            server.registerClient(client);

            // Send messages to the server
            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter messages to send (type 'exit' to quit):");
            while (true) {
                String message = scanner.nextLine();
                if ("exit".equalsIgnoreCase(message)) {
                    break;
                }
                server.sendMessage(message);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
