import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class MessagingServer extends UnicastRemoteObject implements MessagingService {
    private final List<ClientCallback> clients;
    private final List<Post> posts;

    protected MessagingServer() throws RemoteException {
        super();
        clients = new ArrayList<>();
        posts = new ArrayList<>();
    }

    @Override
    public void sendMessage(String message) throws RemoteException {
        System.out.println("Received message: " + message);
        for (ClientCallback client : clients) {
            client.receiveMessage(message);
        }
    }

    @Override
    public void registerClient(ClientCallback client) throws RemoteException {
        clients.add(client);
        System.out.println("New client registered: " + client);
    }

    @Override
    public void createPost(String username, String content) throws RemoteException {
        Post post = new Post(username, content);
        posts.add(post);
        System.out.println("New post created: " + post);
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
            MessagingServer server = new MessagingServer();
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("MessagingService", server);
            System.out.println("Messaging server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
