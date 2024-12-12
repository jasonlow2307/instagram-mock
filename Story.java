import java.time.Instant;
import java.io.Serializable;

public class Story implements Serializable {
    private final String username;
    private final String content;
    private final Instant expiryTime;
    private final int id;

    public Story(int id, String username, String content, int durationInSeconds) {
        this.id = id;
        this.username = username;
        this.content = content;
        this.expiryTime = Instant.now().plusSeconds(durationInSeconds);
    }

    public String getUsername() {
        return username;
    }

    public String getContent() {
        return content;
    }

    public int getId() {return id;}

    public boolean isExpired() {
        return Instant.now().isAfter(expiryTime);
    }
}
