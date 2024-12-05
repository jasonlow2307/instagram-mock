import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.InputMismatchException;
import java.util.List;
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
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            MessagingService server = (MessagingService) registry.lookup("MessagingService");

            MessagingClient client = new MessagingClient();
            server.registerClient(client);

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

                switch (choice) {
                    case 1:
                        System.out.print("Enter message: ");
                        String message = scanner.nextLine();
                        server.sendMessage(message);
                        break;
                    case 2:
                        List<String> clientList = server.getClientList();
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
                        server.sendMessageToClient(targetedMessage, clientIndex);
                        break;
                    case 3:
                        System.out.print("Enter post content: ");
                        String content = scanner.nextLine();
                        server.createPost("User", content);
                        break;
                    case 4:
                        displayFeed(server);
                        break;
                    case 5:
                        displayFeed(server);
                        System.out.print("Enter post ID to like: ");
                        int postIdToLike = scanner.nextInt();
                        server.likePost("User", postIdToLike);
                        break;
                    case 6:
                        displayFeed(server);
                        System.out.print("Enter post ID to comment on: ");
                        int postIdToComment = scanner.nextInt();
                        scanner.nextLine();
                        System.out.print("Enter comment: ");
                        String comment = scanner.nextLine();
                        server.commentOnPost("User", postIdToComment, comment);
                        break;
                    case 7:
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void displayFeed(MessagingService server) throws RemoteException {
        List<Post> feed = server.getFeed();
        System.out.println("\nFeed:");
        for (Post post : feed) {
            System.out.println(post.getId() + ". " + post.getUsername() + ": " + post.getContent());
            System.out.println("   Likes: " + post.getLikes());
            System.out.println("   Comments: " + post.getComments());
        }
    }
}
