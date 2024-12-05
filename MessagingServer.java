import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class MessagingServer extends UnicastRemoteObject implements MessagingService {
    private final List<ClientCallback> clients; // List of connected clients
    private final List<Post> posts;            // List of posts for the feed
    private final Map<String, List<ClientCallback>> chatrooms; // Chatrooms and their participants

    private final Map<String, Set<String>> followers; // Map to track followers for each user
    private final Map<ClientCallback, String> onlineUsers; // Map to track online users and their usernames

    private boolean isPrimary;           // True if the server is primary
    private MessagingService backupServer;     // Reference to the backup server

    protected MessagingServer(boolean isPrimary) throws RemoteException {
        super();
        this.isPrimary = isPrimary;
        clients = new ArrayList<>();
        posts = new ArrayList<>();
        chatrooms = new HashMap<>();
        followers = new HashMap<>();
        onlineUsers = new HashMap<>();
    }

    @Override
    public void sendMessage(String message) throws RemoteException {
        System.out.println("Broadcasting message: " + message);
        for (ClientCallback client : clients) {
            client.receiveMessage(message);
        }
        syncBackup(() -> backupServer.sendMessage(message));
    }

    @Override
    public void sendMessageToClient(String message, int clientIndex) throws RemoteException {
        if (clientIndex < 0 || clientIndex >= clients.size()) {
            System.out.println("Invalid client index: " + clientIndex);
            return;
        }
        ClientCallback client = clients.get(clientIndex);
        client.receiveMessage(message);
        System.out.println("Message sent to Client " + (clientIndex + 1));

        syncBackup(() -> backupServer.sendMessageToClient(message, clientIndex));
    }

    @Override
    public void registerClient(String username, ClientCallback client) throws RemoteException {
        clients.add(client);
        onlineUsers.put(client, username);

        if (!followers.containsKey(username)) {
            followers.put(username, new HashSet<>());
        }

        System.out.println("New client registered: " + username);
        syncBackup(() -> backupServer.registerClient(username, client));
    }

    public void followUser(String follower, String followee) throws RemoteException {
        if (!followers.containsKey(followee)) {
            System.out.println(followee + " does not exist.");
            return;
        }

        followers.get(followee).add(follower);
        System.out.println(follower + " is now following " + followee);

        syncBackup(() -> backupServer.followUser(follower, followee));
    }

    public void unfollowUser(String follower, String followee) throws RemoteException {
        if (!followers.containsKey(followee)) {
            System.out.println(followee + " does not exist.");
            return;
        }

        followers.get(followee).remove(follower);
        System.out.println(follower + " unfollowed " + followee);

        syncBackup(() -> backupServer.unfollowUser(follower, followee));
    }

    @Override
    public Map<String, Set<String>> listOnlineUsers() throws RemoteException {
        Map<String, Set<String>> onlineUsersWithFollowers = new HashMap<>();
        for (Map.Entry<ClientCallback, String> entry : onlineUsers.entrySet()) {
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
        } else {
            System.out.println("Chatroom already exists: " + roomName);
        }
        syncBackup(() -> backupServer.createChatroom(roomName));
    }

    @Override
    public List<String> getChatrooms() throws RemoteException {
        return new ArrayList<>(chatrooms.keySet());
    }

    @Override
    public void joinChatroom(String roomName, ClientCallback client) throws RemoteException {
        if (chatrooms.containsKey(roomName)) {
            chatrooms.get(roomName).add(client);
            System.out.println("Client joined chatroom: " + roomName);
        } else {
            System.out.println("Chatroom not found: " + roomName);
        }
        syncBackup(() -> backupServer.joinChatroom(roomName, client));
    }

    @Override
    public void sendMessageToChatroom(String roomName, String message, ClientCallback sender) throws RemoteException {
        if (chatrooms.containsKey(roomName)) {
            for (ClientCallback client : chatrooms.get(roomName)) {
                if (!client.equals(sender)) {
                    client.receiveMessage("[" + roomName + "] " + message);
                }
            }
        } else {
            System.out.println("Chatroom not found: " + roomName);
        }
        syncBackup(() -> backupServer.sendMessageToChatroom(roomName, message, sender));
    }

    @Override
    public void createPost(String username, String content) throws RemoteException {
        Post post = new Post(username, content);
        posts.add(post);
        System.out.println("New post created by " + username + ": " + content);
        syncBackup(() -> backupServer.createPost(username, content));
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
                syncBackup(() -> backupServer.likePost(username, postId));
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
                syncBackup(() -> backupServer.commentOnPost(username, postId, comment));
                return;
            }
        }
        System.out.println("Post not found: " + postId);
    }

    @Override
    public void synchronizeState(List<ClientCallback> clients, List<Post> posts, Map<String, List<ClientCallback>> chatrooms) throws RemoteException {
        this.clients.clear();
        this.clients.addAll(clients);

        this.posts.clear();
        this.posts.addAll(posts);

        this.chatrooms.clear();
        this.chatrooms.putAll(chatrooms);

        System.out.println("Backup server state synchronized with primary.");
        System.out.println("Clients: " + clients.size() + ", Posts: " + posts.size() + ", Chatrooms: " + chatrooms.keySet());
    }

    private void syncBackup(SyncOperation operation) {
        if (isPrimary && backupServer != null) {
            try {
                // Execute the specific operation
                operation.execute();
                
                // Immediately synchronize the entire state
                backupServer.synchronizeState(
                    new ArrayList<>(clients), 
                    new ArrayList<>(posts), 
                    new HashMap<>(chatrooms)
                );
                System.out.println("Full state synchronized after operation");
            } catch (RemoteException e) {
                System.out.println("Failed to sync with backup server: " + e.getMessage());
            }
        }
    }

    @Override
    public void registerBackup(MessagingService backupServer) throws RemoteException {
        this.backupServer = backupServer;
        System.out.println("Backup server has been registered.");
    }

    private void connectToPrimaryServer(String primaryHost, int primaryPort) {
        new Thread(() -> {
            while (true) {
                try {
                    Registry primaryRegistry = LocateRegistry.getRegistry(primaryHost, primaryPort);
                    MessagingService primaryServer = (MessagingService) primaryRegistry.lookup("MessagingService");
                    
                    // Register backup server with the primary
                    primaryServer.registerBackup(this);
                    System.out.println("Backup server registered with the primary server.");
                    break; // Exit the loop once registered
                } catch (Exception e) {
                    System.out.println("Retrying connection to primary server: " + e.getMessage());
                    try {
                        Thread.sleep(5000); // Retry every 5 seconds
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }).start();
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java MessagingServer <port> <isPrimary(true/false)> <backupPort>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        boolean isPrimary = Boolean.parseBoolean(args[1]);
        int backupPort = Integer.parseInt(args[2]);
    
        try {
            System.setProperty("java.rmi.server.hostname", "localhost");
    
            LocateRegistry.createRegistry(port);
            MessagingServer server = new MessagingServer(isPrimary);
            Registry registry = LocateRegistry.getRegistry(port);
            registry.rebind("MessagingService", server);
    
            System.out.println((isPrimary ? "Primary" : "Backup") + " server running at port " + port);
    
            if (!isPrimary) {
                Registry backupRegistry = LocateRegistry.getRegistry("localhost", backupPort);
                server.backupServer = (MessagingService) backupRegistry.lookup("MessagingService");
                server.connectToPrimaryServer("localhost", backupPort);
                server.startHeartbeat();
            } 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void startHeartbeat() {
        new Thread(() -> {
            while (!isPrimary) {
                try {
                    // Heartbeat check
                    backupServer.getClientList(); 
                    Thread.sleep(5000); // Wait 5 seconds between checks
                } catch (Exception e) {
                    System.out.println("Primary server down. Promoting to primary...");
                    isPrimary = true;
                    backupServer = null;
                    break;
                }
            }
        }).start();
    }

    @FunctionalInterface
    private interface SyncOperation {
        void execute() throws RemoteException;
    }
}