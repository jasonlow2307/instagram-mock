import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface MessagingService extends Remote {
    // Broadcast a message to all connected clients
    void sendMessage(String message) throws RemoteException;

    // Send a targeted message to a specific client
    void sendMessageToClient(String message, int clientIndex) throws RemoteException;

    // Register a client with the server
    void registerClient(ClientCallback client) throws RemoteException;

    // Retrieve a list of connected clients
    List<String> getClientList() throws RemoteException;

    // Create a chatroom
    void createChatroom(String roomName) throws RemoteException;

    // Get the list of chatrooms
    List<String> getChatrooms() throws RemoteException;

    // Join a chatroom
    void joinChatroom(String roomName, ClientCallback client) throws RemoteException;

    // Send a message to a specific chatroom
    void sendMessageToChatroom(String roomName, String message, ClientCallback sender) throws RemoteException;

    // Create a new post
    void createPost(String username, String content) throws RemoteException;

    // Get the list of all posts (feed)
    List<Post> getFeed() throws RemoteException;

    // Like a specific post
    void likePost(String username, int postId) throws RemoteException;

    // Add a comment to a specific post
    void commentOnPost(String username, int postId, String comment) throws RemoteException;
}
