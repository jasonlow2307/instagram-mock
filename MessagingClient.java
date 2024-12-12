import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MessagingClient extends Remote {
    // Receive a general message from the server
    void receiveMessage(String message) throws RemoteException;

    // Receive a message within a chatroom
    void receiveChatMessage(String roomName, String message) throws RemoteException;

    boolean connectToServer(int port) throws NotBoundException, RemoteException;
    void notify(String notification) throws RemoteException;

}