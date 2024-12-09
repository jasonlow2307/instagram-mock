import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.Integer.parseInt;

public class MessagingServerImpl extends UnicastRemoteObject implements MessagingServer {
    private LoadBalancer coordinator;
    private int currentLoad = 0;
    private final List<MessagingClient> clients; // List of connected clients
    private final List<Post> posts;            // List of posts for the feed
    private final Map<String, List<MessagingClient>> chatrooms; // Chatrooms and their participants

    private final Map<String, Set<String>> followers; // Map to track followers for each user
    private final Map<MessagingClient, String> onlineUsers; // Map to track online users and their usernames

    private final int currentPort;

    private final List<Story> stories = new ArrayList<>();

    private final ScheduledExecutorService storyExpiryExecutor = Executors.newScheduledThreadPool(1);

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

        // Start story expiry checker
        storyExpiryExecutor.scheduleAtFixedRate(() -> {
            synchronized (stories) {
                stories.removeIf(Story::isExpired);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public void sendMessage(String message) throws RemoteException {
        forwardLogToLoadBalancer("Broadcasting message: " + message);
        for (MessagingClient client : clients) {
            client.receiveMessage(message);
        }
    }

    @Override
    public void sendMessageToClient(String message, int clientIndex) throws RemoteException {
        if (clientIndex < 0 || clientIndex >= clients.size()) {
            forwardLogToLoadBalancer("Invalid client index: " + clientIndex);
            return;
        }
        MessagingClient client = clients.get(clientIndex);
        client.receiveMessage(message);
        forwardLogToLoadBalancer("Message sent to Client " + (clientIndex + 1));
    }

    @Override
    public void registerClient(String username, MessagingClient client) throws RemoteException {
        clients.add(client);
        onlineUsers.put(client, username);

        if (!followers.containsKey(username)) {
            followers.put(username, new HashSet<>());
        }

        forwardLogToLoadBalancer("New client registered: " + username);
        forwardLogToLoadBalancer("Total clients: " + clients.size()); // Log client count
        notifyStateChange(false);
    }

    @Override
    public void decrementLoad() throws RemoteException {
        currentLoad--;
        coordinator.updateLoad(currentLoad, currentPort);
    }

    @Override
    public void incrementLoad() throws RemoteException {
        currentLoad++;
        coordinator.updateLoad(currentLoad, currentPort);
    }

    @Override
    public void followUser(String follower, String followee) throws RemoteException {
        forwardLogToLoadBalancer(follower + " is trying to follow " + followee);

        if (!followers.containsKey(followee) && !follower.equals(followee)) {
            forwardLogToLoadBalancer(followee + " does not exist.");
            return;
        }

        followers.get(followee).add(follower);
        forwardLogToLoadBalancer(follower + " is now following " + followee);

        // Notify the followed user
        MessagingClient followeeClient = getClientByUsername(followee);
        if (followeeClient != null) {
            followeeClient.notify(follower + " started following you.");
        }

        notifyStateChange(false);
    }


    public void unfollowUser(String follower, String followee) throws RemoteException {
        if (!followers.containsKey(followee)) {
            forwardLogToLoadBalancer(followee + " does not exist.");
            return;
        }

        followers.get(followee).remove(follower);
        forwardLogToLoadBalancer(follower + " unfollowed " + followee);
        notifyStateChange(false);
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
            forwardLogToLoadBalancer("Chatroom created: " + roomName);
            notifyStateChange(false);
        } else {
            forwardLogToLoadBalancer("Chatroom already exists: " + roomName);
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
            forwardLogToLoadBalancer("Client joined chatroom: " + roomName);
            notifyStateChange(false);
        } else {
            forwardLogToLoadBalancer("Chatroom not found: " + roomName);
        }
    }

    @Override
    public void sendMessageToChatroom(String roomName, String message, MessagingClient sender) throws RemoteException {
        if (chatrooms.containsKey(roomName)) {
            for (MessagingClient client : chatrooms.get(roomName)) {
                if (!client.equals(sender)) {
                    // include the username of the sender in the message
                    client.receiveMessage(onlineUsers.get(sender) + ": " + message);
                }
            }
        } else {
            forwardLogToLoadBalancer("Chatroom not found: " + roomName);
        }
    }

    @Override
    public void createPost(String username, String content) throws RemoteException {
        Post post = new Post(username, content);
        posts.add(post);
        forwardLogToLoadBalancer("New post created by " + username + ": " + content);
        notifyStateChange(false);
    }

    @Override
    public List<Post> getFeed() throws RemoteException {
        List<Post> combinedFeed = new ArrayList<>();

        // Add regular posts
        synchronized (posts) {
            combinedFeed.addAll(posts);
        }

        // Add non-expired stories
        synchronized (stories) {
            Iterator<Story> iterator = stories.iterator();
            while (iterator.hasNext()) {
                Story story = iterator.next();
                if (story.isExpired()) {
                    iterator.remove(); // Remove expired stories
                } else {
                    Post pseudoPost = new Post(story.getUsername(), "[Story] " + story.getContent());
                    combinedFeed.add(pseudoPost);
                }
            }
        }

        return combinedFeed;
    }
    @Override
    public void likePost(String username, int postId) throws RemoteException {
        synchronized (posts) {
            for (Post post : posts) {
                if (post.getId() == postId) {
                    post.addLike();
                    forwardLogToLoadBalancer(username + " liked post " + postId);

                    // Notify the post owner
                    String postOwner = post.getUsername();
                    MessagingClient ownerClient = getClientByUsername(postOwner);
                    if (ownerClient != null) {
                        ownerClient.notify(username + " liked your post: " + post.getContent());
                    }

                    notifyStateChange(false);
                    return;
                }
            }
        }
        forwardLogToLoadBalancer("Post not found: " + postId);
    }


    @Override
    public void commentOnPost(String username, int postId, String comment) throws RemoteException {
        synchronized (posts) {
            for (Post post : posts) {
                if (post.getId() == postId) {
                    post.addComment(username + ": " + comment);
                    forwardLogToLoadBalancer(username + " commented on post " + postId);

                    // Notify the post owner
                    String postOwner = post.getUsername();
                    MessagingClient ownerClient = getClientByUsername(postOwner);
                    if (ownerClient != null) {
                        ownerClient.notify(username + " commented on your post: " + post.getContent());
                    }

                    notifyStateChange(false);
                    return;
                }
            }
        }
        forwardLogToLoadBalancer("Post not found: " + postId);
    }

    private MessagingClient getClientByUsername(String username) {
        for (Map.Entry<MessagingClient, String> entry : onlineUsers.entrySet()) {
            forwardLogToLoadBalancer(entry.getValue());
            forwardLogToLoadBalancer(username);
            if (entry.getValue().equals(username)) {
                return entry.getKey();
            }
        }
        return null; // User not found or offline
    }

    private void forwardLogToLoadBalancer(String logMessage) {
        try {
            Registry registry = LocateRegistry.getRegistry(1099); // Load balancer's registry
            LoadBalancer loadBalancer = (LoadBalancer) registry.lookup("ServerCoordinator");
            loadBalancer.logMessage(logMessage);
        } catch (Exception e) {
            forwardLogToLoadBalancer("Failed to send log to load balancer: " + e.getMessage());
        }
    }



    @Override
    public void notifyStateChange(boolean sequential) throws RemoteException {
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

            if (sequential) {
                coordinator.syncServerStateSequential(currentPort, state);
                forwardLogToLoadBalancer("Sequential State change notification sent to coordinator.");
            } else {
                coordinator.syncServerState(currentPort, state);
                forwardLogToLoadBalancer("Concurrent State change notification sent to coordinator.");
            }
        } catch (Exception e) {
            forwardLogToLoadBalancer("Failed to notify state change: " + e);
            e.printStackTrace(); // Add stack trace for debugging
        }
    }

    @Override
    public void updateState(Map<String, Object> newState) throws RemoteException {
        try {
            forwardLogToLoadBalancer("Updating state with: " + newState);

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
            forwardLogToLoadBalancer("State successfully updated.");

        } catch (ClassCastException e) {
            System.err.println("Failed to update state: Invalid state format - " + e.getMessage());
        }
    }

    @Override
    public void ping() throws RemoteException {
        // Simply return or perform no operation to confirm server is alive
    }

    @Override
    public void deletePost(int postId) throws RemoteException {
        synchronized (posts) { // Synchronize to handle concurrent deletions
            boolean removed = posts.removeIf(post -> post.getId() == postId);
            if (removed) {
                forwardLogToLoadBalancer("Post with ID " + postId + " deleted.");
            } else {
                forwardLogToLoadBalancer("Post with ID " + postId + " not found.");
            }
        }
        notifyStateChange(false); // Notify the load balancer of the state change if needed
    }

    @Override
    public void sharePost(int postId, String sharerUsername, String recipientUsername) throws RemoteException {
        Post sharedPost = null;

        // Find the post to share
        synchronized (posts) {
            for (Post post : posts) {
                if (post.getId() == postId) {
                    sharedPost = post;
                    break;
                }
            }
        }

        if (sharedPost == null) {
            forwardLogToLoadBalancer("Post with ID " + postId + " not found.");
            return;
        }

        // Find the recipient client
        MessagingClient recipientClient = null;
        for (Map.Entry<MessagingClient, String> entry : onlineUsers.entrySet()) {
            if (entry.getValue().equals(recipientUsername)) {
                recipientClient = entry.getKey();
                break;
            }
        }

        if (recipientClient == null) {
            forwardLogToLoadBalancer("Recipient user " + recipientUsername + " is not online.");
            return;
        }

        // Notify the recipient
        String message = sharerUsername + " shared a post with you:\n" +
                sharedPost.getId() + ". " + sharedPost.getUsername() + ": " + sharedPost.getContent() + "\n" +
                "   Likes: " + sharedPost.getLikes() + "\n" +
                "   Comments: " + sharedPost.getComments();
        recipientClient.notify(message);

        forwardLogToLoadBalancer(sharerUsername + " shared post ID " + postId + " with " + recipientUsername);
    }

    @Override
    public void createStory(String username, String content, int durationInSeconds) throws RemoteException {
        Story story = new Story(username, content, durationInSeconds);
        synchronized (stories) {
            stories.add(story);
        }
        forwardLogToLoadBalancer("New story created by " + username + ": " + content);
        notifyStateChange(false);
    }

    @Override
    public List<Post> searchPosts(String keyword, String username, Instant startTime, Instant endTime) throws RemoteException {
        List<Post> results = new ArrayList<>();
        synchronized (posts) {
            for (Post post : posts) {
                boolean matchesKeyword = (keyword == null || post.getContent().toLowerCase().contains(keyword.toLowerCase()));
                boolean matchesUsername = (username == null || post.getUsername().equalsIgnoreCase(username));
                boolean matchesTimeRange = (startTime == null || !post.getTimestamp().isBefore(startTime)) &&
                        (endTime == null || !post.getTimestamp().isAfter(endTime));

                if (matchesKeyword && matchesUsername && matchesTimeRange) {
                    results.add(post);
                }
            }
        }
        return results;
    }



    public static void main(String[] args) throws RemoteException, NotBoundException {
        int port = parseInt(args[0]);
       System.setProperty("java.rmi.server.hostname", "localhost");
       LocateRegistry.createRegistry(port);
       MessagingServerImpl server = new MessagingServerImpl(port);
       Registry registry = LocateRegistry.getRegistry(port);
       registry.rebind("MessagingService", server);
   }

}