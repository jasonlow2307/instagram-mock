import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.Integer.parseInt;

public class MessagingServerImpl extends UnicastRemoteObject implements MessagingServer {
    private final LoadBalancer coordinator;
    private final DatabaseServer databaseServer;
    private int currentLoad = 0;

    private final int currentPort;

    protected MessagingServerImpl(int currentPort) throws RemoteException, NotBoundException {
        super();
        this.currentPort = currentPort;

        // Pull load balancer into server for updating load
        Registry registry = LocateRegistry.getRegistry(1099);
        this.coordinator = (LoadBalancer) registry.lookup("ServerCoordinator");

        registry = LocateRegistry.getRegistry(1098); // Database server port
        this.databaseServer = (DatabaseServer) registry.lookup("DatabaseServer");

        // Start story expiry checker
        
        List<Story> stories = databaseServer.getStories();
        ScheduledExecutorService storyExpiryExecutor = Executors.newScheduledThreadPool(1);
        storyExpiryExecutor.scheduleAtFixedRate(() -> {
            synchronized (stories) {
                stories.removeIf(Story::isExpired);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public void sendMessage(String message) throws RemoteException {
        forwardLogToLoadBalancer("Broadcasting message: " + message);
        List<MessagingClient> clients = databaseServer.getClients();
        for (MessagingClient client : clients) {
            client.receiveMessage(message);
        }
    }

    @Override
    public void sendMessageToClient(String message, int clientIndex) throws RemoteException {
        List<MessagingClient> clients = databaseServer.getClients();
        if (clientIndex < 0 || clientIndex >= clients.size()) {
            forwardLogToLoadBalancer("Invalid client index: " + clientIndex);
            return;
        }
        MessagingClient client = clients.get(clientIndex);
        client.receiveMessage(message);
        forwardLogToLoadBalancer("Message sent to Client " + (clientIndex + 1));
    }

    @Override
    public boolean registerUser(String username, String password) throws RemoteException, NoSuchAlgorithmException {
        // Hash the entered password
        String hashedPassword = hashPassword(password);

        return databaseServer.registerUser(username, hashedPassword);
    }

    @Override
    public boolean loginUser(String username, String password) throws RemoteException, NoSuchAlgorithmException {
        // Hash the password
        String hashedPassword = hashPassword(password);

        return databaseServer.loginUser(username, hashedPassword);
    }

    // Utility method to hash the password
    private String hashPassword(String password) throws NoSuchAlgorithmException {
        // Use SHA-256 hashing algorithm
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(password.getBytes());

        // Convert byte array into a Base64 encoded string for storage
        return Base64.getEncoder().encodeToString(hashBytes);
    }

    @Override
    public void registerClient(String username, MessagingClient client) throws RemoteException {
        List<MessagingClient> clients = databaseServer.getClients();
        clients.add(client);
        databaseServer.saveClients(clients);
        Map<MessagingClient, String> onlineUsers = databaseServer.getOnlineUsers();
        onlineUsers.put(client, username); // Add the client and username to the map
        databaseServer.saveOnlineUsers(onlineUsers);

        Map<String, Set<String>> followers = databaseServer.getFollowers();

        if (!followers.containsKey(username)) {
            followers.put(username, new HashSet<>());
            databaseServer.saveFollowers(followers);
        }

        forwardLogToLoadBalancer("New client registered: " + username);
        forwardLogToLoadBalancer("Total clients: " + onlineUsers.size()); // Log client count
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

        Map<String, Set<String>> followers = databaseServer.getFollowers();

        if (!followers.containsKey(followee) && !follower.equals(followee)) {
            forwardLogToLoadBalancer(followee + " does not exist.");
            return;
        }
        followers.get(followee).add(follower);
        databaseServer.saveFollowers(followers);
        forwardLogToLoadBalancer(follower + " is now following " + followee);

        // Notify the followed user
        MessagingClient followeeClient = getClientByUsername(followee);
        // Notify the followee if they are online
        Map<MessagingClient, String> onlineUsers = databaseServer.getOnlineUsers();
        if (followeeClient != null && onlineUsers.containsKey(followeeClient)) {
            followeeClient.notify(follower + " is now following you.");
        }
    }


    public void unfollowUser(String follower, String followee) throws RemoteException {
        Map<String, Set<String>> followers = databaseServer.getFollowers();
        if (!followers.containsKey(followee)) {
            forwardLogToLoadBalancer(followee + " does not exist.");
            return;
        }

        followers.get(followee).remove(follower);
        databaseServer.saveFollowers(followers);
        forwardLogToLoadBalancer(follower + " unfollowed " + followee);
    }

    @Override
    public Map<String, Set<String>> listOnlineUsers() throws RemoteException {
        Map<String, Set<String>> onlineUsersWithFollowers = new HashMap<>();
        Map<MessagingClient, String> onlineUsers = databaseServer.getOnlineUsers();
        Map<String, Set<String>> followers = databaseServer.getFollowers();
        for (Map.Entry<MessagingClient, String> entry : onlineUsers.entrySet()) {
            String username = entry.getValue();
            onlineUsersWithFollowers.put(username, followers.getOrDefault(username, new HashSet<>()));
        }
        return onlineUsersWithFollowers;
    }

    @Override 
    public void removeOnlineUser(String username) throws RemoteException {
        Map<MessagingClient, String> onlineUsers = databaseServer.getOnlineUsers();
        for (Map.Entry<MessagingClient, String> entry : onlineUsers.entrySet()) {
            if (entry.getValue().equals(username)) {
                onlineUsers.remove(entry.getKey());
                break;
            }
        }
        databaseServer.saveOnlineUsers(onlineUsers);
    }


    @Override
    public List<String> getClientList() throws RemoteException {
        List<String> clientDescriptions = new ArrayList<>();
        List<MessagingClient> clients = databaseServer.getClients();
        for (int i = 0; i < clients.size(); i++) {
            clientDescriptions.add("Client " + (i + 1));
        }
        return clientDescriptions;
    }

    @Override
    public void createChatroom(String roomName) throws RemoteException {
        Map<String, List<MessagingClient>> chatrooms = databaseServer.getChatrooms();
        if (!chatrooms.containsKey(roomName)) {
            chatrooms.put(roomName, new ArrayList<>());
            forwardLogToLoadBalancer("Chatroom created: " + roomName);
            databaseServer.saveChatrooms(chatrooms);
        } else {
            forwardLogToLoadBalancer("Chatroom already exists: " + roomName);
        }
    }

    @Override
    public List<String> getChatrooms() throws RemoteException {
        Map<String, List<MessagingClient>> chatrooms = databaseServer.getChatrooms();
        return new ArrayList<>(chatrooms.keySet());
    }

    @Override
    public void joinChatroom(String roomName, MessagingClient client) throws RemoteException {
        Map<String, List<MessagingClient>> chatrooms = databaseServer.getChatrooms();
        if (chatrooms.containsKey(roomName)) {
            chatrooms.get(roomName).add(client);
            forwardLogToLoadBalancer("Client joined chatroom: " + roomName);
            databaseServer.saveChatrooms(chatrooms);
        } else {
            forwardLogToLoadBalancer("Chatroom not found: " + roomName);
        }
    }

    @Override
    public void sendMessageToChatroom(String roomName, String message, MessagingClient sender) throws RemoteException {
        Map<String, List<MessagingClient>> chatrooms = databaseServer.getChatrooms();
        Map<MessagingClient, String> onlineUsers = databaseServer.getOnlineUsers();
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
        List<Post> posts = databaseServer.getPosts();
        Post post = new Post(username, content, databaseServer.getPostId() + 1);
        posts.add(post);
        forwardLogToLoadBalancer("New post created by " + username + ": " + content);
        databaseServer.savePosts(posts);
    }

    @Override
    public List<Post> getFeed() throws RemoteException {
        List<Post> posts = databaseServer.getPosts();
        List<Story> stories = databaseServer.getStories();

        // Add regular posts
        List<Post> combinedFeed = new ArrayList<>(posts);

        // Add non-expired stories
        Iterator<Story> iterator = stories.iterator();
        while (iterator.hasNext()) {
            Story story = iterator.next();
            if (story.isExpired()) {
                iterator.remove(); // Remove expired stories
                
            } else {
                Post pseudoPost = new Post(story.getUsername(), "[Story] " + story.getContent(), databaseServer.getPostId() + 1);
                combinedFeed.add(pseudoPost);
            }
        }

        return combinedFeed;
    }
    @Override
    public void likePost(String username, int postId) throws RemoteException {
        List<Post> posts = databaseServer.getPosts();
        for (Post post : posts) {
            if (post.getId() == postId) {
                post.addLike();
                forwardLogToLoadBalancer(username + " liked post " + postId);

                // Notify the post owner
                String postOwner = post.getUsername();
                MessagingClient ownerClient = getClientByUsername(postOwner);
                Map<MessagingClient, String> onlineUsers = databaseServer.getOnlineUsers();
                if (ownerClient != null && onlineUsers.containsKey(ownerClient)) {
                    ownerClient.notify(username + " liked your post: " + post.getContent());
                }

                databaseServer.savePosts(posts);
                return;
            }
        }
        forwardLogToLoadBalancer("Post not found: " + postId);
    }


    @Override
    public void commentOnPost(String username, int postId, String comment) throws RemoteException {
        List<Post> posts = databaseServer.getPosts();
        for (Post post : posts) {
            if (post.getId() == postId) {
                post.addComment(username + ": " + comment);
                forwardLogToLoadBalancer(username + " commented on post " + postId);

                // Notify the post owner
                String postOwner = post.getUsername();
                MessagingClient ownerClient = getClientByUsername(postOwner);
                Map<MessagingClient, String> onlineUsers = databaseServer.getOnlineUsers();
                if (ownerClient != null && onlineUsers.containsKey(ownerClient)) {
                    ownerClient.notify(username + " commented on your post: " + post.getContent());
                }

                databaseServer.savePosts(posts);
                return;
            }
        }
        forwardLogToLoadBalancer("Post not found: " + postId);
    }

    private MessagingClient getClientByUsername(String username) throws RemoteException {
        Map<MessagingClient, String> onlineUsers = databaseServer.getOnlineUsers();
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
    public void ping() throws RemoteException {
        // Simply return or perform no operation to confirm server is alive
    }

    @Override
    public void deletePost(int postId) throws RemoteException {
        List<Post> posts = databaseServer.getPosts();
        boolean removed = posts.removeIf(post -> post.getId() == postId);
        if (removed) {
            forwardLogToLoadBalancer("Post with ID " + postId + " deleted.");
        } else {
            forwardLogToLoadBalancer("Post with ID " + postId + " not found.");
        }
        databaseServer.savePosts(posts);
    }

    @Override
    public void shareContent(int contentId, String sharerUsername, String recipientUsername) throws RemoteException {
        Post sharedPost = null;
        Story sharedStory = null;
        List<Post> posts = databaseServer.getPosts();
        List<Story> stories = databaseServer.getStories();

        // Find the post to share
        for (Post post : posts) {
            if (post.getId() == contentId) {
                sharedPost = post;
                break;
            }
        }

        for (Story story : stories) {
            if (story.getId() == contentId) {
                sharedStory = story;
                break;
            }
        }

        if (sharedPost == null && sharedStory == null) {
            forwardLogToLoadBalancer("Post or Story with ID " + contentId + " not found.");
            return;
        }

        Map<MessagingClient, String> onlineUsers = databaseServer.getOnlineUsers();

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

        if (sharedPost != null) {
            // Notify the recipient
            String message = sharerUsername + " shared a post with you:\n" +
                    sharedPost.getId() + ". " + sharedPost.getUsername() + ": " + sharedPost.getContent() + "\n" +
                    "   Likes: " + sharedPost.getLikes() + "\n" +
                    "   Comments: " + sharedPost.getComments();
            recipientClient.notify(message);
            forwardLogToLoadBalancer(sharerUsername + " shared post ID " + contentId + " with " + recipientUsername);
        }
        if (sharedStory != null) {
            // Notify the recipient
            String message = sharerUsername + " shared a story with you:\n" +
                    sharedStory.getId() + ". " + sharedStory.getUsername() + ": " + sharedStory.getContent();
            recipientClient.notify(message);
            forwardLogToLoadBalancer(sharerUsername + " shared story ID " + contentId + " with " + recipientUsername);
        }



    }

    @Override
    public void createStory(String username, String content, int durationInSeconds) throws RemoteException {
        List<Story> stories = databaseServer.getStories();
        Story story = new Story(databaseServer.getPostId() + 1, username, content, durationInSeconds);
        stories.add(story);
        forwardLogToLoadBalancer("New story created by " + username + ": " + content);
        databaseServer.saveStories(stories);
    }

    @Override
    public List<Post> searchPosts(String keyword, String username, Instant startTime, Instant endTime) throws RemoteException {
        List<Post> results = new ArrayList<>();
        List<Post> posts = databaseServer.getPosts();
        for (Post post : posts) {
            boolean matchesKeyword = (keyword == null || post.getContent().toLowerCase().contains(keyword.toLowerCase()));
            boolean matchesUsername = (username == null || post.getUsername().equalsIgnoreCase(username));
            boolean matchesTimeRange = (startTime == null || !post.getTimestamp().isBefore(startTime)) &&
                    (endTime == null || !post.getTimestamp().isAfter(endTime));

            if (matchesKeyword && matchesUsername && matchesTimeRange) {
                results.add(post);
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