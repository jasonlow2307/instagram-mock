import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// Serializable class for posts
public class Post implements Serializable {
    private static int idCounter = 0;
    private final int id;
    private final String username;
    private final String content;
    private int likes;
    private final List<String> comments;

    public Post(String username, String content) {
        this.id = idCounter++;
        this.username = username;
        this.content = content;
        this.likes = 0;
        this.comments = new ArrayList<>();
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

    public void addLike() {
        likes++;
    }

    public void addComment(String comment) {
        comments.add(comment);
    }

    @Override
    public String toString() {
        return "Post{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", content='" + content + '\'' +
                ", likes=" + likes +
                ", comments=" + comments +
                '}';
    }
}
