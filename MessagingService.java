import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

// Remote interface for Instagram mock
public interface MessagingService extends Remote {
    // Messaging functionality
    void sendMessage(String message) throws RemoteException;
    void registerClient(ClientCallback client) throws RemoteException;

    // Posts functionality
    void createPost(String username, String content) throws RemoteException;
    List<Post> getFeed() throws RemoteException;
    void likePost(String username, int postId) throws RemoteException;
    void commentOnPost(String username, int postId, String comment) throws RemoteException;
}
