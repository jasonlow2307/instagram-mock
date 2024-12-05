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
            Registry registry = LocateRegistry.getRegistry("18.141.204.33", 1099);
            MessagingService server = (MessagingService) registry.lookup("MessagingService");

            MessagingClient client = new MessagingClient();
            server.registerClient(client);

            Scanner scanner = new Scanner(System.in);

            while (true) {
                int choice = -1; // Default invalid value
                try {
                    System.out.println("\n1. Send Message\n2. Create Post\n3. View Feed\n4. Like Post\n5. Comment on Post\n6. Exit");
                    System.out.print("Choose an option: ");
                    choice = scanner.nextInt();
                    scanner.nextLine(); // Consume newline after number input
                } catch (InputMismatchException e) {
                    System.out.println("Invalid input. Please enter a number between 1 and 6.");
                    scanner.nextLine(); // Clear the invalid input
                    continue; // Retry the loop
                }

                switch (choice) {
                    case 1:
                        System.out.print("Enter message: ");
                        String message = scanner.nextLine();
                        server.sendMessage(message);
                        break;
                    case 2:
                        System.out.print("Enter post content: ");
                        String content = scanner.nextLine();
                        server.createPost("User", content);
                        break;
                    case 3:
                        displayFeed(server);
                        break;
                    case 4:
                        displayFeed(server);
                        System.out.print("Enter post ID to like: ");
                        int postIdToLike = scanner.nextInt();
                        server.likePost("User", postIdToLike);
                        break;
                    case 5:
                        displayFeed(server);
                        System.out.print("Enter post ID to comment on: ");
                        int postIdToComment = scanner.nextInt();
                        scanner.nextLine(); // Consume newline
                        System.out.print("Enter comment: ");
                        String comment = scanner.nextLine();
                        server.commentOnPost("User", postIdToComment, comment);
                        break;
                    case 6:
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Invalid choice. Please enter a number between 1 and 6.");
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
