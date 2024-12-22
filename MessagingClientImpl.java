import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.Instant;
import java.util.*;

public class MessagingClientImpl extends UnicastRemoteObject implements MessagingClient {
    private String username;
    private MessagingServer server;

    protected MessagingClientImpl() throws RemoteException {
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

    @Override
    public boolean connectToServer(int port) throws NotBoundException, RemoteException {
        Registry registry = LocateRegistry.getRegistry("localhost", port);
        server = (MessagingServer) registry.lookup("MessagingService");
        System.out.println("Connected to server at port: " + port);
        return true;
    }

    @Override
    public void notify(String notification) throws RemoteException {
        System.out.println("[Notification] " + notification);
    }

    public static void main(String[] args) {
        try {
            MessagingClientImpl client = new MessagingClientImpl();
            Registry registry = LocateRegistry.getRegistry(1099);
            LoadBalancer coordinator = (LoadBalancer) registry.lookup("ServerCoordinator");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    System.out.println("\nShutting down...");
                    coordinator.removeClient(client);
                    client.server.removeOnlineUser(client.username);
                    client.server.decrementLoad();
                } catch (Exception e) {
                    System.err.println("Failed to notify load balancer on shutdown: " + e.getMessage());
                }
            }));

            int leastLoadedPort = coordinator.getLeastLoadedServer();
            System.out.println("Least-loaded server: " + leastLoadedPort);

            if (!client.connectToServer(leastLoadedPort)) {
                System.exit(1);
            }

            coordinator.addClient(client, leastLoadedPort);
            client.server.incrementLoad();

            Scanner scanner = new Scanner(System.in);
            boolean isLoggedIn = false;

            System.out.println("Welcome to Messaging Application!");

            while (!isLoggedIn) {
                System.out.println("\n0. Register");
                System.out.println("1. Login");
                System.out.print("Choose an option: ");
                int authChoice = -1;

                try {
                    authChoice = scanner.nextInt();
                    scanner.nextLine();
                } catch (InputMismatchException e) {
                    System.out.println("Invalid input. Please enter a valid number.");
                    scanner.nextLine();
                    continue;
                }

                switch (authChoice) {
                    case 0:
                        System.out.print("Enter username: ");
                        String newUsername = scanner.nextLine();
                        System.out.print("Enter password: ");
                        String newPassword = scanner.nextLine();

                        if (client.server.registerUser(newUsername, newPassword)) {
                            System.out.println("Registration successful.");
                        } else {
                            System.out.println("Registration failed. Username might already exist.");
                        }
                        break;

                    case 1:
                        System.out.print("Enter username: ");
                        String loginUsername = scanner.nextLine();
                        System.out.print("Enter password: ");
                        String loginPassword = scanner.nextLine();

                        if (client.server.loginUser(loginUsername, loginPassword)) {
                            System.out.println("Login successful.");
                            client.username = loginUsername;
                            isLoggedIn = true;

                            client.server.registerClient(client.username, client);
                        } else {
                            System.out.println("Invalid username or password.");
                        }
                        break;

                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }

            while (true) {
                System.out.println("\n1. Send Message");
                System.out.println("2. Targeted Chatroom");
                System.out.println("3. Create Content");
                System.out.println("4. View Feed");
                System.out.println("5. Like Post");
                System.out.println("6. Comment on Post");
                System.out.println("7. Follow User");
                System.out.println("8. Unfollow User");
                System.out.println("9. List Online Users");
                System.out.println("10. Delete Post");
                System.out.println("11. Share Content");
                System.out.println("12. Search Post");
                System.out.println("13. Exit");
                System.out.print("Choose an option: ");

                int choice = -1;

                try {
                    choice = scanner.nextInt();
                    scanner.nextLine();
                } catch (InputMismatchException e) {
                    System.out.println("Invalid input. Please enter a valid number.");
                    scanner.nextLine();
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
                            System.out.println("\n1. Create Chatroom\n2. Join Chatroom");
                            System.out.print("Choose an option: ");
                            int chatChoice = -1;

                            try {
                                chatChoice = scanner.nextInt();
                                scanner.nextLine();
                            } catch (InputMismatchException e) {
                                System.out.println("Invalid input. Please enter a valid number.");
                                scanner.nextLine();
                                break;
                            }

                            if (chatChoice == 1) {
                                System.out.print("Enter chatroom name: ");
                                String roomName = scanner.nextLine();
                                client.server.createChatroom(roomName);
                                System.out.println("Chatroom created: " + roomName);

                                client.server.joinChatroom(roomName, client);
                                System.out.println("Joined chatroom: " + roomName);
                                System.out.println("Type 'quit' to exit the chatroom.");
                                while (true) {
                                    System.out.print("You: ");
                                    String chatMessage = scanner.nextLine();
                                    if (chatMessage.equalsIgnoreCase("quit")) {
                                        break;
                                    }
                                    client.server.sendMessageToChatroom(roomName, chatMessage, client);
                                }
                            } else if (chatChoice == 2) {
                                List<String> chatrooms = client.server.getChatrooms();
                                if (chatrooms.isEmpty()) {
                                    System.out.println("No chatrooms available.");
                                    break;
                                }
                                System.out.println("Available chatrooms:");
                                for (int i = 0; i < chatrooms.size(); i++) {
                                    System.out.println((i + 1) + ". " + chatrooms.get(i));
                                }
                                System.out.print("Choose a chatroom to join: ");
                                int roomIndex = -1;

                                try {
                                    roomIndex = scanner.nextInt() - 1;
                                    scanner.nextLine();
                                } catch (InputMismatchException e) {
                                    System.out.println("Invalid input. Please enter a valid number.");
                                    scanner.nextLine();
                                    break;
                                }

                                if (roomIndex >= 0 && roomIndex < chatrooms.size()) {
                                    String roomName = chatrooms.get(roomIndex);
                                    client.server.joinChatroom(roomName, client);
                                    System.out.println("Joined chatroom: " + roomName);
                                    System.out.println("Type 'quit' to exit the chatroom.");
                                    while (true) {
                                        System.out.print("You: ");
                                        String chatMessage = scanner.nextLine();
                                        if (chatMessage.equalsIgnoreCase("quit")) {
                                            break;
                                        }
                                        client.server.sendMessageToChatroom(roomName, chatMessage, client);
                                    }
                                } else {
                                    System.out.println("Invalid choice.");
                                }
                            }
                            break;
                        case 3:
                            System.out.println("\n1. Regular Post\n2. Story");
                            System.out.print("Choose an option: ");
                            int contentChoice = -1;

                            try {
                                contentChoice = scanner.nextInt();
                                scanner.nextLine();
                            } catch (InputMismatchException e) {
                                System.out.println("Invalid input. Please enter a valid number.");
                                scanner.nextLine();
                                break;
                            }

                            if (contentChoice == 1) {
                                System.out.print("Enter post content: ");
                                String postContent = scanner.nextLine();
                                client.server.createPost(client.username, postContent);
                            } else if (contentChoice == 2) {
                                System.out.print("Enter story content: ");
                                String storyContent = scanner.nextLine();
                                client.server.createStory(client.username, storyContent, 86400); // Default to 24 hours
                            } else {
                                System.out.println("Invalid choice.");
                            }
                            break;
                        case 4:
                            client.displayFeed();
                            break;
                        case 5:
                            client.displayFeed();
                            System.out.print("Enter post ID to like: ");
                            int postIdToLike = -1;

                            try {
                                postIdToLike = scanner.nextInt();
                                scanner.nextLine();
                            } catch (InputMismatchException e) {
                                System.out.println("Invalid input. Please enter a valid number.");
                                scanner.nextLine();
                                break;
                            }

                            client.server.likePost(client.username, postIdToLike);
                            break;
                        case 6:
                            client.displayFeed();
                            System.out.print("Enter post ID to comment on: ");
                            int postIdToComment = -1;

                            try {
                                postIdToComment = scanner.nextInt();
                                scanner.nextLine();
                            } catch (InputMismatchException e) {
                                System.out.println("Invalid input. Please enter a valid number.");
                                scanner.nextLine();
                                break;
                            }

                            System.out.print("Enter comment: ");
                            String comment = scanner.nextLine();
                            client.server.commentOnPost(client.username, postIdToComment, comment);
                            break;
                        case 13:
                            System.exit(0);
                        default:
                            System.out.println("Invalid choice. Please try again.");
                    }
                } catch (RemoteException e) {
                    System.err.println("Server connection lost. Attempting to reconnect...");
                    if (!client.connectToServer(leastLoadedPort)) {
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
