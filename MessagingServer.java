import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MessagingServer extends Remote {
    void sendMessage(String message) throws RemoteException;
    void sendMessageToClient(String message, int clientIndex) throws RemoteException;
    List<String> getClientList() throws RemoteException;
    void createChatroom(String roomName) throws RemoteException;
    List<String> getChatrooms() throws RemoteException;
    void joinChatroom(String roomName, MessagingClient client) throws RemoteException;
    void sendMessageToChatroom(String roomName, String message, MessagingClient sender) throws RemoteException;
    void createPost(String username, String content) throws RemoteException;
    List<Post> getFeed() throws RemoteException;
    void likePost(String username, int postId) throws RemoteException;
    void commentOnPost(String username, int postId, String comment) throws RemoteException;
    void followUser(String follower, String followee) throws RemoteException; // New
    void unfollowUser(String follower, String followee) throws RemoteException; // New
    void registerClient(String username, MessagingClient client) throws RemoteException;
    Map<String, Set<String>> listOnlineUsers() throws RemoteException; // Updated
    void updateState(Map<String, Object> newState) throws RemoteException;
    void ping() throws RemoteException;
    void decrementLoad() throws RemoteException;
    void incrementLoad() throws RemoteException;
    void notifyStateChange() throws RemoteException;
}