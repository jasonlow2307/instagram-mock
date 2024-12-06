import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class MessagingServerImpl extends UnicastRemoteObject implements MessagingServer {
    private LoadBalancer coordinator;
    private int currentLoad = 0;
    private final List<MessagingClient> clients; // List of connected clients
    private final List<Post> posts;            // List of posts for the feed
    private final Map<String, List<MessagingClient>> chatrooms; // Chatrooms and their participants

    private final Map<String, Set<String>> followers; // Map to track followers for each user
    private final Map<MessagingClient, String> onlineUsers; // Map to track online users and their usernames

    private final int currentPort;

    protected MessagingServerImpl(int currentPort) throws RemoteException, NotBoundException {
        super();
        clients = new ArrayList<>();
        posts = new ArrayList<>();
        chatrooms = new HashMap<>();
        followers = new HashMap<>();
        onlineUsers = new HashMap<>();
        this.currentPort = currentPort;

        // Pull load balancer into server for updating load
        Registry registry = LocateRegistry.getRegistry(1099);
        this.coordinator = (LoadBalancer) registry.lookup("ServerCoordinator");

        System.out.println("Before calling monitorAndScale...");
        monitorAndScale();
        System.out.println("After calling monitorAndScale...");
    }

    @Override
    public void sendMessage(String message) throws RemoteException {
        System.out.println("Broadcasting message: " + message);
        for (MessagingClient client : clients) {
            client.receiveMessage(message);
        }
    }

    @Override
    public void sendMessageToClient(String message, int clientIndex) throws RemoteException {
        if (clientIndex < 0 || clientIndex >= clients.size()) {
            System.out.println("Invalid client index: " + clientIndex);
            return;
        }
        MessagingClient client = clients.get(clientIndex);
        client.receiveMessage(message);
        System.out.println("Message sent to Client " + (clientIndex + 1));
    }

    @Override
    public void registerClient(String username, MessagingClient client) throws RemoteException {
        clients.add(client);
        onlineUsers.put(client, username);

        if (!followers.containsKey(username)) {
            followers.put(username, new HashSet<>());
        }

        System.out.println("New client registered: " + username);
        System.out.println("Total clients: " + clients.size()); // Log client count
        notifyStateChange();

        currentLoad++;
        coordinator.updateLoad(currentLoad, currentPort);
    }

    public void followUser(String follower, String followee) throws RemoteException {
        if (!followers.containsKey(followee)) {
            System.out.println(followee + " does not exist.");
            return;
        }

        followers.get(followee).add(follower);
        System.out.println(follower + " is now following " + followee);
        notifyStateChange();
    }

    public void unfollowUser(String follower, String followee) throws RemoteException {
        if (!followers.containsKey(followee)) {
            System.out.println(followee + " does not exist.");
            return;
        }

        followers.get(followee).remove(follower);
        System.out.println(follower + " unfollowed " + followee);
        notifyStateChange();
    }

    @Override
    public Map<String, Set<String>> listOnlineUsers() throws RemoteException {
        Map<String, Set<String>> onlineUsersWithFollowers = new HashMap<>();
        for (Map.Entry<MessagingClient, String> entry : onlineUsers.entrySet()) {
            String username = entry.getValue();
            onlineUsersWithFollowers.put(username, followers.getOrDefault(username, new HashSet<>()));
        }
        return onlineUsersWithFollowers;
    }


    @Override
    public List<String> getClientList() throws RemoteException {
        List<String> clientDescriptions = new ArrayList<>();
        for (int i = 0; i < clients.size(); i++) {
            clientDescriptions.add("Client " + (i + 1));
        }
        return clientDescriptions;
    }

    @Override
    public void createChatroom(String roomName) throws RemoteException {
        if (!chatrooms.containsKey(roomName)) {
            chatrooms.put(roomName, new ArrayList<>());
            System.out.println("Chatroom created: " + roomName);
            notifyStateChange();
        } else {
            System.out.println("Chatroom already exists: " + roomName);
        }
    }

    @Override
    public List<String> getChatrooms() throws RemoteException {
        return new ArrayList<>(chatrooms.keySet());
    }

    @Override
    public void joinChatroom(String roomName, MessagingClient client) throws RemoteException {
        if (chatrooms.containsKey(roomName)) {
            chatrooms.get(roomName).add(client);
            System.out.println("Client joined chatroom: " + roomName);
            notifyStateChange();
        } else {
            System.out.println("Chatroom not found: " + roomName);
        }
    }

    @Override
    public void sendMessageToChatroom(String roomName, String message, MessagingClient sender) throws RemoteException {
        if (chatrooms.containsKey(roomName)) {
            for (MessagingClient client : chatrooms.get(roomName)) {
                if (!client.equals(sender)) {
                    // include the username of the sender in the message
                    client.receiveMessage(onlineUsers.get(sender) + ": " + message);
                    System.out.println("RECEIVEDMESSAGE");
                }
            }
        } else {
            System.out.println("Chatroom not found: " + roomName);
        }
    }

    @Override
    public void createPost(String username, String content) throws RemoteException {
        Post post = new Post(username, content);
        posts.add(post);
        System.out.println("New post created by " + username + ": " + content);
        notifyStateChange();
    }

    @Override
    public List<Post> getFeed() throws RemoteException {
        return posts;
    }

    @Override
    public void likePost(String username, int postId) throws RemoteException {
        for (Post post : posts) {
            if (post.getId() == postId) {
                post.addLike();
                System.out.println(username + " liked post " + postId);
                notifyStateChange();
                return;
            }
        }
        System.out.println("Post not found: " + postId);
    }

    @Override
    public void commentOnPost(String username, int postId, String comment) throws RemoteException {
        for (Post post : posts) {
            if (post.getId() == postId) {
                post.addComment(username + ": " + comment);
                System.out.println(username + " commented on post " + postId);
                notifyStateChange();
                return;
            }
        }
        System.out.println("Post not found: " + postId);
    }

    private static final int CLIENT_THRESHOLD = 20; // Adjust as necessary
    private LoadMonitor loadMonitor = new LoadMonitor();

    private void monitorAndScale() {
        System.out.println("Starting monitorAndScale thread...");
        new Thread(() -> {
            while (true) {
                System.out.println("Monitoring client load...");
                int currentClientCount = clients.size();
                System.out.println("Current client count: " + currentClientCount);
                if (currentClientCount > CLIENT_THRESHOLD) {
                    System.out.println("Client threshold exceeded. Spawning new server...");
                    try {
                        int newPort = findAvailablePort(); // Dynamically find an available port
                        ServerSpawner.spawnNewServer(newPort, false, currentPort); // Use currentPort
                        System.out.println("New server spawned at port: " + newPort);
                    } catch (Exception e) {
                        System.err.println("Failed to spawn a new server: " + e.getMessage());
                    }
                }
                try {
                    Thread.sleep(5000); // Check every 5 seconds
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }



    private int findAvailablePort() throws IOException {
        // Use a ServerSocket to find an available port dynamically
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort(); // Get an available port
        }
    }

    public void simulateClientLoad(int numberOfClients) {
        new Thread(() -> {
            try {
                for (int i = 0; i < numberOfClients; i++) {
                    String username = "TestUser" + i;
                    registerClient(username, new TestMessagingClient(username));
                    System.out.println("Simulated client: " + username);
                    Thread.sleep(50); // Small delay between client registrations
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void notifyStateChange() {
        try {
            Registry registry = LocateRegistry.getRegistry(1099);
            LoadBalancer coordinator = (LoadBalancer) registry.lookup("ServerCoordinator");

            // Create a state map to send
            Map<String, Object> state = new HashMap<>();
            state.put("clients", clients); // Send client details if needed
            state.put("posts", new ArrayList<>(posts));
            state.put("chatrooms", new HashMap<>(chatrooms));
            state.put("followers", new HashMap<>(followers));
            state.put("onlineUsers", new HashMap<>(onlineUsers));

            coordinator.syncServerState(currentPort, state);
            System.out.println("State change notified to coordinator.");
        } catch (Exception e) {
            System.err.println("Failed to notify state change: " + e);
            e.printStackTrace(); // Add stack trace for debugging
        }
    }

    @Override
    public synchronized void updateState(Map<String, Object> newState) throws RemoteException {
        try {
            System.out.println("Updating state with: " + newState);

            // Clear current state
            clients.clear();
            posts.clear();
            chatrooms.clear();
            followers.clear();
            onlineUsers.clear();

            // Apply the new state
            clients.addAll((List<MessagingClient>) newState.get("clients"));
            posts.addAll((List<Post>) newState.get("posts"));
            chatrooms.putAll((Map<String, List<MessagingClient>>) newState.get("chatrooms"));
            followers.putAll((Map<String, Set<String>>) newState.get("followers"));
            onlineUsers.putAll((Map<MessagingClient, String>) newState.get("onlineUsers"));
            System.out.println("State successfully updated.");

        } catch (ClassCastException e) {
            System.err.println("Failed to update state: Invalid state format - " + e.getMessage());
        }
    }


}