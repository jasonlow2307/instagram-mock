import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Post implements Serializable {
    private static int nextId = 1; // Static ID generator
    private final int id;         // Unique ID of the post
    private final String username;
    private final String content;
    private int likes;
    private final List<String> comments;
    private final Instant timestamp;

    public Post(String username, String content, int postId) {
        this.id = postId;
        this.username = username;
        this.content = content;
        this.likes = 0;
        this.comments = new ArrayList<>();
        this.timestamp = Instant.now();
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getContent() {
        return content;
    }

    public int getLikes() {
        return likes;
    }

    public List<String> getComments() {
        return comments;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void addLike() {
        likes++;
    }

    public void addComment(String comment) {
        comments.add(comment);
    }

    @Override
    public String toString() {
        return "Post{id=" + id + ", username='" + username + "', content='" + content +
                "', likes=" + likes + ", comments=" + comments + '}';
    }
}
