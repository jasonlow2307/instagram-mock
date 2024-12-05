import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class MessagingServer extends UnicastRemoteObject implements MessagingService {
    private final List<ClientCallback> clients; // List of connected clients
    private final List<Post> posts;            // List of posts for the feed

    protected MessagingServer() throws RemoteException {
        super();
        clients = new ArrayList<>();
        posts = new ArrayList<>();
    }

    @Override
    public void sendMessage(String message) throws RemoteException {
        System.out.println("Broadcasting message: " + message);
        for (ClientCallback client : clients) {
            client.receiveMessage(message);
        }
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
    }

    @Override
    public void registerClient(ClientCallback client) throws RemoteException {
        clients.add(client);
        System.out.println("New client registered. Total clients: " + clients.size());
    }

    @Override
    public List<String> getClientList() throws RemoteException {
        List<String> clientDescriptions = new ArrayList<>();
        for (int i = 0; i < clients.size(); i++) {
            clientDescriptions.add("Client " + (i + 1)); // Return a descriptive name for each client
        }
        return clientDescriptions;
    }

    @Override
    public void createPost(String username, String content) throws RemoteException {
        Post post = new Post(username, content);
        posts.add(post);
        System.out.println("New post created by " + username + ": " + content);
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
                return;
            }
        }
        System.out.println("Post not found: " + postId);
    }

    public static void main(String[] args) {
        try {
            // Set the hostname to the server's IP address
            System.setProperty("java.rmi.server.hostname", "localhost");

            // Create and bind the RMI registry
            LocateRegistry.createRegistry(1099);
            MessagingServer server = new MessagingServer();
            Registry registry = LocateRegistry.getRegistry(1099);

            // Bind the MessagingService to the registry
            registry.rebind("MessagingService", server);

            System.out.println("Messaging server is running at: " + System.getProperty("java.rmi.server.hostname"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
