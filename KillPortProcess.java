import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class KillPortProcess {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java KillPortProcess <port_number>");
            return;
        }

        String port = args[0];
        try {
            // Find the PID using netstat
            Process netstatProcess = new ProcessBuilder("cmd.exe", "/c", "netstat -ano | findstr :" + port).start();
            BufferedReader netstatReader = new BufferedReader(new InputStreamReader(netstatProcess.getInputStream()));

            String line;
            String pid = null;
            while ((line = netstatReader.readLine()) != null) {
                System.out.println("Netstat Output: " + line);
                String[] parts = line.trim().split("\s+");
                if (parts.length >= 5 && parts[1].endsWith(":" + port)) {
                    pid = parts[4];
                    break;
                }
            }

            netstatReader.close();

            if (pid == null) {
                System.out.println("No process found listening on port " + port);
                return;
            }

            System.out.println("Found PID: " + pid);

            // Kill the process using the PID
            Process killProcess = new ProcessBuilder("cmd.exe", "/c", "taskkill /PID " + pid + " /F").start();
            BufferedReader killReader = new BufferedReader(new InputStreamReader(killProcess.getInputStream()));

            while ((line = killReader.readLine()) != null) {
                System.out.println(line);
            }

            killReader.close();
        } catch (IOException e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
