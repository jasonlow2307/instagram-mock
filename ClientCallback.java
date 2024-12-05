import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientCallback extends Remote {
    // Receive a general message from the server
    void receiveMessage(String message) throws RemoteException;

    // Receive a message within a chatroom
    void receiveChatMessage(String roomName, String message) throws RemoteException;
}
