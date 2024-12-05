import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MessagingService extends Remote {
    void sendMessage(String message) throws RemoteException;
    void sendMessageToClient(String message, int clientIndex) throws RemoteException;
    List<String> getClientList() throws RemoteException;
    void createChatroom(String roomName) throws RemoteException;
    List<String> getChatrooms() throws RemoteException;
    void joinChatroom(String roomName, ClientCallback client) throws RemoteException;
    void sendMessageToChatroom(String roomName, String message, ClientCallback sender) throws RemoteException;
    void createPost(String username, String content) throws RemoteException;
    List<Post> getFeed() throws RemoteException;
    void likePost(String username, int postId) throws RemoteException;
    void commentOnPost(String username, int postId, String comment) throws RemoteException;
    void synchronizeState(List<ClientCallback> clients, List<Post> posts, Map<String, List<ClientCallback>> chatrooms) throws RemoteException;
    void registerBackup(MessagingService backupServer) throws RemoteException; // New method


    void followUser(String follower, String followee) throws RemoteException; // New
    void unfollowUser(String follower, String followee) throws RemoteException; // New
    void registerClient(String username, ClientCallback client) throws RemoteException;
    Map<String, Set<String>> listOnlineUsers() throws RemoteException; // Updated
    // Other methods...
}