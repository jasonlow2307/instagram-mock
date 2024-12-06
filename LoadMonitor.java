public class LoadMonitor {
    private int connectedClients = 0;
    private long messagesSent = 0;

    public synchronized void clientConnected() {
        connectedClients++;
    }

    public synchronized void clientDisconnected() {
        connectedClients--;
    }

    public synchronized void messageSent() {
        messagesSent++;
    }

    public synchronized int getClientCount() {
        return connectedClients;
    }

    public synchronized long getMessagesSent() {
        return messagesSent;
    }
}
