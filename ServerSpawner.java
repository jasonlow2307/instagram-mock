import java.io.IOException;

public class ServerSpawner {
    public static void spawnNewServer(int port, boolean isPrimary, int backupPort) {
        try {
            String command = String.format("java MessagingServer %d %b %d", port, isPrimary, backupPort);
            Runtime.getRuntime().exec(command);
            System.out.println("Spawned new server on port " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    }

