import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagingServer extends UnicastRemoteObject implements MessagingService {
    private final List<ClientCallback> clients; // List of connected clients
    private final List<Post> posts;            // List of posts for the feed
    private final Map<String, List<ClientCallback>> chatrooms; // Chatrooms and their participants

    protected MessagingServer() throws RemoteException {
        super();
        clients = new ArrayList<>();
        posts = new ArrayList<>();
        chatrooms = new HashMap<>();
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
        if (args.length < 1) {
            System.out.println("Usage: java MessagingServer <port>");
            return;
        }
        int port = Integer.parseInt(args[0]);

        try {
            System.setProperty("java.rmi.server.hostname", "localhost"); // Update this IP to match your server's IP

            LocateRegistry.createRegistry(port);
            MessagingServer server = new MessagingServer();
            Registry registry = LocateRegistry.getRegistry(port);
            registry.rebind("MessagingService", server);

            System.out.println("Messaging server is running at: " + System.getProperty("java.rmi.server.hostname") + ":" + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
