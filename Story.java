import java.time.Instant;

public class Story {
    private final String username;
    private final String content;
    private final Instant expiryTime;

    public Story(String username, String content, int durationInSeconds) {
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

    public boolean isExpired() {
        return Instant.now().isAfter(expiryTime);
    }
}
