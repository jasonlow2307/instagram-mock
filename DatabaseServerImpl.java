import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class DatabaseServerImpl extends UnicastRemoteObject implements DatabaseServer {

    private final List <Account> accounts = new ArrayList<>();
    private final List<MessagingClient> clients = new ArrayList<>();
    private final List<Post> posts = new ArrayList<>();
    private final Map<String, List<MessagingClient>> chatrooms = new HashMap<>();
    private final Map<String, Set<String>> followers = new HashMap<>();
    private final List<Story> stories = new ArrayList<>();
    private final Map<MessagingClient, String> onlineUsers = new HashMap<>();

    protected DatabaseServerImpl() throws RemoteException {
        super();
    }

    // Write methods to update the state in the database
    @Override
    public synchronized boolean registerUser(String username, String password) throws RemoteException {
        try {
            // Store the account with the hashed password
            this.accounts.add(new Account(username, password));
            System.out.println("Registered new account: " + username);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public synchronized boolean loginUser(String username, String password) throws RemoteException {
        try {
            // Search for an account with the matching username and hashed password
            for (Account account : this.accounts) {
                if (account.getUsername().equals(username) && account.getPassword().equals(password)) {
                    System.out.println("User logged in successfully: " + username);
                    return true; // User found, login successful
                }
            }
            System.out.println("Login failed for user: " + username);
            return false; // No matching user found
        } catch (Exception e) {
            e.printStackTrace();
            return false; // Handle any unexpected errors
        }
    }

    @Override
    public synchronized void saveClients(List<MessagingClient> clients) throws RemoteException {
        this.clients.clear();
        this.clients.addAll(clients);
        System.out.println("Updated clients: " + this.clients);
    }

    @Override
    public synchronized void savePosts(List<Post> posts) throws RemoteException {
        this.posts.clear();
        this.posts.addAll(posts);
        System.out.println("Updated posts: " + this.posts);
    }

    @Override
    public synchronized void saveChatrooms(Map<String, List<MessagingClient>> chatrooms) throws RemoteException {
        this.chatrooms.clear();
        this.chatrooms.putAll(chatrooms);
        System.out.println("Updated chatrooms: " + this.chatrooms);
    }

    @Override
    public synchronized void saveFollowers(Map<String, Set<String>> followers) throws RemoteException {
        this.followers.clear();
        this.followers.putAll(followers);
        System.out.println("Updated followers: " + this.followers);
    }

    @Override
    public synchronized void saveStories(List<Story> stories) throws RemoteException {
        this.stories.clear();
        this.stories.addAll(stories);
        System.out.println("Updated stories: " + this.stories);
    }

    @Override
    public synchronized void saveOnlineUsers(Map<MessagingClient, String> onlineUsers) throws RemoteException {
        this.onlineUsers.clear();
        this.onlineUsers.putAll(onlineUsers);
        System.out.println("Updated online users: " + this.onlineUsers);
    }

    // Read methods to retrieve the current state
    @Override
    public synchronized List<MessagingClient> getClients() throws RemoteException {
        return new ArrayList<>(clients); // Return a copy to avoid modification of the original list
    }

    @Override
    public synchronized List<Post> getPosts() throws RemoteException {
        return new ArrayList<>(posts); // Return a copy to avoid modification of the original list
    }

    @Override
    public synchronized Map<String, List<MessagingClient>> getChatrooms() throws RemoteException {
        return new HashMap<>(chatrooms); // Return a copy of the chatrooms map
    }

    @Override
    public synchronized Map<String, Set<String>> getFollowers() throws RemoteException {
        return new HashMap<>(followers); // Return a copy of the followers map
    }

    @Override
    public synchronized List<Story> getStories() throws RemoteException {
        return new ArrayList<>(stories); // Return a copy to avoid modification of the original list
    }

    @Override
    public synchronized Map<MessagingClient, String> getOnlineUsers() throws RemoteException {
        return new HashMap<>(onlineUsers); // Return a copy of the online users map
    }

    // Main method to run the DatabaseServer
    public static void main(String[] args) {
        try {
            DatabaseServerImpl databaseServer = new DatabaseServerImpl();
            Registry registry = LocateRegistry.createRegistry(1098); // Use a different port for the database server
            registry.rebind("DatabaseServer", databaseServer);
            System.out.println("Database server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}