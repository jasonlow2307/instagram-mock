import java.rmi.Remote;
import java.rmi.RemoteException;

// Remote interface for client callbacks
public interface ClientCallback extends Remote {
    void receiveMessage(String message) throws RemoteException;
}
