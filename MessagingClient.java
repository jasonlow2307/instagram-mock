import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MessagingClient extends Remote {
    // Receive a general message from the server
    void receiveMessage(String message) throws RemoteException;

    // Receive a message within a chatroom
    void receiveChatMessage(String roomName, String message) throws RemoteException;

    boolean connectToServer(int port) throws NotBoundException, RemoteException;
    void notify(String notification) throws RemoteException;


    @Override
    public void receiveMessage(String message) throws RemoteException {
        System.out.println("\n" + message);
        System.out.print("You: ");
    }

    @Override
    public void receiveChatMessage(String roomName, String message) throws RemoteException {
        System.out.println("[" + roomName + "] Chat Partner: " + message);
        System.out.print("You: ");
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
                System.exit(1); // Exit if unable to connect to any server
            }

            Scanner scanner = new Scanner(System.in);

            // ANSI escape codes for colors
            String purple = "\033[35m"; // Purple
            String blue = "\033[34m";   // Blue
            String yellow = "\033[33m"; // Yellow
            String reset = "\033[0m";   // Reset color

            // Enhanced welcome message
            System.out.println(purple + "**********************************************" + reset);
            System.out.println(purple + "*                                            *" + reset);
            System.out.println(purple + "*   " + yellow + "WELCOME TO INSTAGRAM!!!" + purple + "                        *" + reset);
            System.out.println(purple + "*                                            *" + reset);
            System.out.println(purple + "*   " + blue + "Stay connected, share moments, and explore" + purple + "  *" + reset);
            System.out.println(purple + "*                                            *" + reset);
            System.out.println(purple + "*   " + yellow + "Let's make memories together ❤️" + purple + "            *" + reset);
            System.out.println(purple + "*                                            *" + reset);
            System.out.println(purple + "**********************************************" + reset + "\n\n");


            String asciiArt = """
                     ⬜⬜🟦🟦🟦🟦🟦🟦🟪🟪🟪🟪🟪🟪🟪🟪🟪⬜⬜
                     ⬜🟦🟦🟦🟦🟦🟦🟪🟪🟪🟪🟪🟪🟪🟪🟪🟪🟪⬜
                     🟦🟦🟦🟦🟦🟦🟦🟪🟪🟪🟪🟪🟪🟪🟪🟪🟪🟪🟪
                     🟪🟦🟦🟦⬜⬜⬜⬜⬜⬜⬜⬜⬜⬜⬜🟪🟪🟪🟪
                     🟪🟪🟪⬜🟪🟪🟪🟪🟪🟪🟪🟪🟪🟪🟪⬜🟪🟪🟪
                     🟪🟪🟪⬜🟪🟪🟪🟪🟪🟪🟪🟪⬜⬜🟪⬜🟪🟪🟪
                     🟪🟪🟪⬜🟪🟪🟪🟪🟪🟪🟪🟪⬜⬜🟪⬜🟪🟪🟪
                     🟪🟪🟪⬜🟪🟪🟪⬜⬜⬜⬜🟪🟪🟪🟪⬜🟪🟪🟪
                     🟪🟪🟪⬜🟥🟪⬜🟪🟪🟪🟪⬜🟪🟪🟪⬜🟪🟪🟪
                     🟪🟪🟥⬜🟥🟥⬜🟪🟥🟪🟪⬜🟪🟪🟪⬜🟪🟪🟪
                     🟥🟥🟥⬜🟥🟥⬜🟥🟥🟥🟥⬜🟪🟪🟪⬜🟪🟪🟪
                     🟥🟥🟧⬜🟧🟧⬜🟧🟧🟥🟥⬜🟥🟪🟪⬜🟪🟪🟪
                     🟧🟧🟧⬜🟨🟧🟧⬜⬜⬜⬜🟥🟥🟥🟥⬜🟪🟪🟪
                     🟧🟨🟨⬜🟨🟨🟧🟧🟧🟧🟥🟥🟥🟥🟥⬜🟪🟪🟪
                     🟧🟨🟨⬜🟨🟨🟧🟧🟧🟧🟥🟥🟥🟥🟥⬜🟪🟪🟪
                     🟨🟨🟨🟨⬜⬜⬜⬜⬜⬜⬜⬜⬜⬜⬜🟪🟪🟥🟥
                     🟨🟨🟨🟨🟨🟨🟨🟨🟧🟧🟧🟧🟧🟥🟥🟥🟥🟥🟥
                     ⬜🟨🟨🟨🟨🟨🟨🟨🟨🟧🟧🟧🟧🟥🟥🟥🟥🟥⬜
                     ⬜⬜🟨🟨🟨🟨🟨🟨🟨🟧🟧🟧🟧🟥🟥🟥🟥⬜⬜
                """;

            System.out.println(asciiArt);

            // Prompt for username
            System.out.print("Enter your username: ");
            client.username = scanner.nextLine();
            client.server.registerClient(client.username, client); // Pass username during registration

            while (true) {
                System.out.println("\n1. Send Message\n2. Targeted Chatroom\n3. Create Post\n4. View Feed\n5. Like Post\n6. Comment on Post");
                System.out.println("7. Follow User\n8. Unfollow User\n9. List Online Users\n10. Exit");
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
                        case 7: // Follow a user
                            System.out.print("Enter username to follow: ");
                            String followee = scanner.nextLine();
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
                                System.out.println("- " + user + " (Followers: " + (followers.isEmpty() ? "None" : String.join(", ", followers)) + ")");
                            }
                            break;
                        case 10: // Exit
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
