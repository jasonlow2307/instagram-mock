import java.rmi.Remote;
import java.rmi.RemoteException;

// Remote interface for messaging
public interface MessagingService extends Remote {
    void sendMessage(String message) throws RemoteException;
    void registerClient(ClientCallback client) throws RemoteException;
}
