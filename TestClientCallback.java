import java.rmi.RemoteException;

public class TestClientCallback implements ClientCallback {
    private final String username;

    public TestClientCallback(String username) {
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
}
