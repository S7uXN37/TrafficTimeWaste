package ch.kanti_baden.pu_marc_14b.traffictimewaste;

import java.io.Serializable;

public class Post implements Serializable {

    public int id;
    public String content;
    public String postedAt;
    public String ownerName;
    public int votesUp;
    public int votesDown;
    public String[] tags;

    public Post(int postId, String text, String timePosted, String username,
                int upvotes, int downvotes, String postTags) {
        id = postId;
        content = text;
        postedAt = timePosted;
        ownerName = username;
        votesUp = upvotes;
        votesDown = downvotes;
        tags = postTags.split(",");
    }

}
