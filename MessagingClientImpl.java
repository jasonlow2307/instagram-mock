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
                    client.server.decrementLoad();
                } catch (Exception e) {
                    System.err.println("Failed to notify load balancer on shutdown: " + e.getMessage());
                }
            }));


            int leastLoadedPort = coordinator.getLeastLoadedServer();
            System.out.println("Least-loaded server: " + leastLoadedPort);

            if (!client.connectToServer(leastLoadedPort)) {
                System.exit(1); // Exit if unable to connect to any server
            }

            coordinator.addClient(client, leastLoadedPort);
            client.server.incrementLoad();

            Scanner scanner = new Scanner(System.in);

            // Prompt for username
            System.out.print("Enter your username: ");
            client.username = scanner.nextLine();
            client.server.registerClient(client.username, client); // Pass username during registration

            while (true) {
                System.out.println("\n1. Send Message\n2. Targeted Chatroom\n3. Create Content\n4. View Feed\n5. Like Post\n6. Comment on Post");
                System.out.println("7. Follow User\n8. Unfollow User\n9. List Online Users\n10. Delete Post\n11. Share Post\n12. Search for Post\n13. Exit");
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
                            int chatChoice = scanner.nextInt();
                            scanner.nextLine(); // Consume leftover newline

                            if (chatChoice == 1) {
                                System.out.print("Enter chatroom name: ");
                                String roomName = scanner.nextLine();
                                client.server.createChatroom(roomName);
                                System.out.println("Chatroom created: " + roomName);

                                // Automatically join the chatroom after creation
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
                                int roomIndex = scanner.nextInt() - 1;
                                scanner.nextLine(); // Consume leftover newline
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
                        case 3: // Create Content
                            System.out.println("\n1. Regular Post\n2. Story");
                            System.out.print("Choose an option: ");
                            int contentChoice = scanner.nextInt();
                            scanner.nextLine(); // Consume leftover newline

                            if (contentChoice == 1) {
                                System.out.print("Enter post content: ");
                                String postContent = scanner.nextLine();
                                client.server.createPost(client.username, postContent);
                            } else if (contentChoice == 2) {
                                System.out.print("Enter story content: ");
                                String storyContent = scanner.nextLine();

                                // List time periods for story visibility
                                System.out.println("\nChoose story visibility duration:");
                                System.out.println("1. 30 seconds");
                                System.out.println("2. 1 minute");
                                System.out.println("3. 5 minutes");
                                System.out.println("4. 1 hour");
                                System.out.println("5. 8 hours");
                                System.out.println("6. 24 hours");
                                System.out.print("Choose an option: ");
                                int timeChoice = scanner.nextInt();
                                scanner.nextLine(); // Consume leftover newline

                                int durationInSeconds;
                                switch (timeChoice) {
                                    case 1: durationInSeconds = 30; break;
                                    case 2: durationInSeconds = 60; break;
                                    case 3: durationInSeconds = 300; break;
                                    case 4: durationInSeconds = 3600; break;
                                    case 5: durationInSeconds = 28800; break;
                                    case 6: durationInSeconds = 86400; break;
                                    default:
                                        System.out.println("Invalid choice. Defaulting to 24 hours.");
                                        durationInSeconds = 86400;
                                }

                                client.server.createStory(client.username, storyContent, durationInSeconds);
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
                            int postIdToLike = scanner.nextInt();
                            client.server.likePost(client.username, postIdToLike);
                            break;
                        case 6:
                            client.displayFeed();
                            System.out.print("Enter post ID to comment on: ");
                            int postIdToComment = scanner.nextInt();
                            scanner.nextLine();
                            System.out.print("Enter comment: ");
                            String comment = scanner.nextLine();
                            client.server.commentOnPost(client.username, postIdToComment, comment);
                            break;
                        case 7: // Follow a user
                            System.out.print("Enter username to follow: ");
                            String followee = scanner.nextLine();
                            System.out.println(client.username + "FOLLOW" + followee);
                            client.server.followUser(client.username, followee);
                            break;
                        case 8: // Unfollow a user
                            System.out.print("Enter username to unfollow: ");
                            String unfollowee = scanner.nextLine();
                            client.server.unfollowUser(client.username, unfollowee);
                            break;
                        case 9: // List online users
                            Map<String, Set<String>> onlineUsersWithFollowers = client.server.listOnlineUsers();
                            System.out.println("Online Users with Followers:");
                            for (Map.Entry<String, Set<String>> entry : onlineUsersWithFollowers.entrySet()) {
                                String user = entry.getKey();
                                Set<String> followers = entry.getValue();
                                System.out.println("- " + user + " (Followers: " + (followers.isEmpty() ? "None" : followers.size()) + ")");
                            }
                            break;
                        case 10: // Delete a post
                            client.displayFeed();
                            System.out.print("Enter post ID to delete: ");
                            int postIdToDelete = scanner.nextInt();
                            scanner.nextLine(); // Consume leftover newline
                            client.server.deletePost(postIdToDelete);
                            System.out.println("Post deleted successfully.");
                            break;
                        case 11: // Share a post
                            // Display the feed
                            client.displayFeed();
                            System.out.print("Enter post ID to share: ");
                            int postIdToShare = scanner.nextInt();
                            scanner.nextLine(); // Consume leftover newline

                            // Display online users
                            onlineUsersWithFollowers = client.server.listOnlineUsers();
                            if (onlineUsersWithFollowers.isEmpty()) {
                                System.out.println("No users are currently online to share the post.");
                                break;
                            }

                            System.out.println("Online Users:");
                            List<String> onlineUsernames = new ArrayList<>(onlineUsersWithFollowers.keySet());
                            for (int i = 0; i < onlineUsernames.size(); i++) {
                                System.out.println((i + 1) + ". " + onlineUsernames.get(i));
                            }

                            // Select recipient
                            System.out.print("Choose a user to share the post with: ");
                            int recipientIndex = scanner.nextInt() - 1;
                            scanner.nextLine(); // Consume leftover newline

                            if (recipientIndex >= 0 && recipientIndex < onlineUsernames.size()) {
                                String recipient = onlineUsernames.get(recipientIndex);
                                client.server.sharePost(postIdToShare, client.username, recipient);
                                System.out.println("Post shared successfully with " + recipient + ".");
                            } else {
                                System.out.println("Invalid user selection.");
                            }
                            break;
                        case 12: // Search for posts
                            System.out.println("\nSearch for Posts:");
                            System.out.print("Enter keyword (leave blank for no filter): ");
                            String keyword = scanner.nextLine().trim();

                            System.out.print("Enter username (leave blank for no filter): ");
                            String searchUsername = scanner.nextLine().trim();
                            if (searchUsername.isEmpty()) searchUsername = null;

                            System.out.println("Enter time range (leave blank for no filter):");
                            System.out.print("Start time (YYYY-MM-DD HH:mm:ss): ");
                            String startInput = scanner.nextLine().trim();
                            Instant startTime = startInput.isEmpty() ? null : Instant.parse(startInput + ":00Z");

                            System.out.print("End time (YYYY-MM-DD HH:mm:ss): ");
                            String endInput = scanner.nextLine().trim();
                            Instant endTime = endInput.isEmpty() ? null : Instant.parse(endInput + ":00Z");

                            List<Post> searchResults = client.server.searchPosts(
                                    keyword.isEmpty() ? null : keyword,
                                    searchUsername,
                                    startTime,
                                    endTime
                            );

                            System.out.println("\nSearch Results:");
                            for (Post post : searchResults) {
                                System.out.println(post.getId() + ". " + post.getUsername() + ": " + post.getContent());
                                System.out.println("   Likes: " + post.getLikes());
                                System.out.println("   Comments: " + post.getComments());
                            }
                            break;
                        case 13: // Exit
                            System.exit(0);
                            break;
                        default:
                            System.out.println("Invalid choice. Please try again.");
                    }
                } catch (RemoteException e) {
                    System.err.println("Server connection lost. Attempting to reconnect...");
                    if (!client.connectToServer(1009)) {
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

//    private boolean connectToLeastLoadedServer() {
//        try {
//            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
//            ServerCoordinator coordinator = (ServerCoordinator) registry.lookup("ServerCoordinator");
//            int serverPort = coordinator.getLeastLoadedServer();
//
//            if (serverAddress != null) {
//                String[] hostPort = serverAddress.split(":");
//                String host = hostPort[0];
//                int port = Integer.parseInt(hostPort[1]);
//                Registry serverRegistry = LocateRegistry.getRegistry(host, port);
//                server = (MessagingService) serverRegistry.lookup("MessagingService");
//                System.out.println("Connected to least-loaded server: " + serverAddress);
//                return true;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return false;
//    }

}
