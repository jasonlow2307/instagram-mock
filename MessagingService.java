import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface MessagingService extends Remote {
    void sendMessage(String message) throws RemoteException;
    void registerClient(ClientCallback client) throws RemoteException;
    List<String> getClientList() throws RemoteException; // New method
    void sendMessageToClient(String message, int clientIndex) throws RemoteException; // New method for targeted message
    // Existing methods
    void createPost(String username, String content) throws RemoteException;
    List<Post> getFeed() throws RemoteException;
    void likePost(String username, int postId) throws RemoteException;
    void commentOnPost(String username, int postId, String comment) throws RemoteException;
}
