import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class MessagingClient extends UnicastRemoteObject implements ClientCallback {
    private static final String[] SERVER_ADDRESSES = {
        "192.168.100.94:1099", // Primary server
        "192.168.100.94:1100"  // Secondary server
    };

    private MessagingService server;

    protected MessagingClient() throws RemoteException {
        super();
    }

    @Override
    public void receiveMessage(String message) throws RemoteException {
        System.out.println("New message: " + message);
    }

    private boolean connectToServer() {
        for (String address : SERVER_ADDRESSES) {
            try {
                String[] hostPort = address.split(":");
                String host = hostPort[0];
                int port = Integer.parseInt(hostPort[1]);
                Registry registry = LocateRegistry.getRegistry(host, port);
                server = (MessagingService) registry.lookup("MessagingService");
                System.out.println("Connected to server: " + address);
                return true;
            } catch (Exception e) {
                System.err.println("Failed to connect to server: " + address);
            }
        }
        System.err.println("All servers are unavailable.");
        return false;
    }

    public static void main(String[] args) {
        try {
            MessagingClient client = new MessagingClient();

            if (!client.connectToServer()) {
                System.exit(1);
            }

            client.server.registerClient(client);

            Scanner scanner = new Scanner(System.in);

            while (true) {
                int choice = -1;
                try {
                    System.out.println("\n1. Send Message\n2. Send Targeted Message\n3. Create Post\n4. View Feed\n5. Like Post\n6. Comment on Post\n7. Exit");
                    System.out.print("Choose an option: ");
                    choice = scanner.nextInt();
                    scanner.nextLine(); // Consume newline after number input
                } catch (InputMismatchException e) {
                    System.out.println("Invalid input. Please enter a number.");
                    scanner.nextLine(); // Clear invalid input
                    continue;
                }

                try {
                    switch (choice) {
                        case 1:
                            System.out.print("Enter message: ");
                            String message = scanner.nextLine();
                            client.server.sendMessage(message);
                            break;
                        case 2:
                            List<String> clientList = client.server.getClientList();
                            if (clientList.isEmpty()) {
                                System.out.println("No clients connected.");
                                break;
                            }
                            System.out.println("Connected clients:");
                            for (int i = 0; i < clientList.size(); i++) {
                                System.out.println((i + 1) + ". " + clientList.get(i));
                            }
                            System.out.print("Select client (number): ");
                            int clientIndex = scanner.nextInt() - 1;
                            scanner.nextLine(); // Consume newline
                            System.out.print("Enter message: ");
                            String targetedMessage = scanner.nextLine();
                            client.server.sendMessageToClient(targetedMessage, clientIndex);
                            break;
                        case 3:
                            System.out.print("Enter post content: ");
                            String content = scanner.nextLine();
                            client.server.createPost("User", content);
                            break;
                        case 4:
                            client.displayFeed();
                            break;
                        case 5:
                            client.displayFeed();
                            System.out.print("Enter post ID to like: ");
                            int postIdToLike = scanner.nextInt();
                            client.server.likePost("User", postIdToLike);
                            break;
                        case 6:
                            client.displayFeed();
                            System.out.print("Enter post ID to comment on: ");
                            int postIdToComment = scanner.nextInt();
                            scanner.nextLine();
                            System.out.print("Enter comment: ");
                            String comment = scanner.nextLine();
                            client.server.commentOnPost("User", postIdToComment, comment);
                            break;
                        case 7:
                            System.exit(0);
                            break;
                        default:
                            System.out.println("Invalid choice. Please try again.");
                    }
                } catch (RemoteException e) {
                    System.err.println("Server connection lost. Attempting to reconnect...");
                    if (!client.connectToServer()) {
                        System.out.println("Failed to reconnect. Exiting...");
                        System.exit(1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayFeed() throws RemoteException {
        List<Post> feed = server.getFeed();
        System.out.println("\nFeed:");
        for (Post post : feed) {
            System.out.println(post.getId() + ". " + post.getUsername() + ": " + post.getContent());
            System.out.println("   Likes: " + post.getLikes());
            System.out.println("   Comments: " + post.getComments());
        }
    }
}