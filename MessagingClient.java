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
        System.out.println("\n" + message);
    }

    @Override
    public void receiveChatMessage(String roomName, String message) throws RemoteException {
        System.out.println("[" + roomName + "] Chat Partner: " + message);
        System.out.print("You: ");
    }


    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("192.168.100.231", 1099);
            MessagingService server = (MessagingService) registry.lookup("MessagingService");

            MessagingClient client = new MessagingClient();
            server.registerClient(client);

            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.println("\n1. Send Message\n2. Targeted Chatroom\n3. Create Post\n4. View Feed\n5. Like Post\n6. Comment on Post\n7. Exit");
                System.out.print("Choose an option: ");
                int choice = -1;

                try {
                    choice = scanner.nextInt();
                    scanner.nextLine(); // Consume leftover newline
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
                        System.out.println("\n1. Create Chatroom\n2. Join Chatroom");
                        System.out.print("Choose an option: ");
                        int chatChoice = scanner.nextInt();
                        scanner.nextLine(); // Consume leftover newline

                        if (chatChoice == 1) {
                            System.out.print("Enter chatroom name: ");
                            String roomName = scanner.nextLine();
                            server.createChatroom(roomName);
                            System.out.println("Chatroom created: " + roomName);
                        } else if (chatChoice == 2) {
                            List<String> chatrooms = server.getChatrooms();
                            if (chatrooms.isEmpty()) {
                                System.out.println("No chatrooms available.");
                                break;
                            }
                            System.out.println("Available chatrooms:");
                            for (int i = 0; i < chatrooms.size(); i++) {
                                System.out.println((i + 1) + ". " + chatrooms.get(i));
                            }
                            System.out.print("Choose a chatroom to join: ");
                            int roomIndex = scanner.nextInt() - 1;
                            scanner.nextLine(); // Consume leftover newline
                            if (roomIndex >= 0 && roomIndex < chatrooms.size()) {
                                String roomName = chatrooms.get(roomIndex);
                                server.joinChatroom(roomName, client);
                                System.out.println("Joined chatroom: " + roomName);
                                System.out.println("Type 'quit' to exit the chatroom.");
                                while (true) {
                                    System.out.print("You: ");
                                    String chatMessage = scanner.nextLine();
                                    if (chatMessage.equalsIgnoreCase("quit")) {
                                        break;
                                    }
                                    server.sendMessageToChatroom(roomName, chatMessage, client);
                                }
                            } else {
                                System.out.println("Invalid choice.");
                            }
                        }
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
                        scanner.nextLine(); // Consume leftover newline
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
