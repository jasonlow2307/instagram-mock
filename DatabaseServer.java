import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.*;

public interface DatabaseServer extends Remote {
    // Write methods to update the state in the database
    boolean registerUser(String username, String password) throws RemoteException;

    boolean loginUser(String username, String password) throws RemoteException;

    void saveClients(List<MessagingClient> clients) throws RemoteException;

    void savePosts(List<Post> posts) throws RemoteException;

    void saveChatrooms(Map<String, List<MessagingClient>> chatrooms) throws RemoteException;

    void saveFollowers(Map<String, Set<String>> followers) throws RemoteException;

    void saveStories(List<Story> stories) throws RemoteException;

    void saveOnlineUsers(Map<MessagingClient, String> onlineUsers) throws RemoteException;

    // Read methods to retrieve the current state
    List<MessagingClient> getClients() throws RemoteException;

    List<Post> getPosts() throws RemoteException;

    Map<String, List<MessagingClient>> getChatrooms() throws RemoteException;

    Map<String, Set<String>> getFollowers() throws RemoteException;

    List<Story> getStories() throws RemoteException;

    Map<MessagingClient, String> getOnlineUsers() throws RemoteException;
}