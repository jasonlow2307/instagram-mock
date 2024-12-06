import java.rmi.RemoteException;

public class TestMessagingClient implements MessagingClient {
    private final String username;

    public TestMessagingClient(String username) {
        this.username = username;
    }

    @Override
    public void receiveMessage(String message) throws RemoteException {
        System.out.println("[" + username + "] Received message: " + message);
    }

    @Override
    public void receiveChatMessage(String roomName, String message) throws RemoteException {
        System.out.println("[" + username + "] [" + roomName + "] Chat message: " + message);
    }

    @Override
    public boolean connectToServer(int port) throws RemoteException {
        return true;
    }
}
