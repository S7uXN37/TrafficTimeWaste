package ch.kanti_baden.pu_marc_14b.traffictimewaste;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Post implements Serializable {

    public int id;
    public String content;
    public String postedAt;
    public long postedAtMillis;
    public String ownerName;
    public int votesUp;
    public int votesDown;
    public String[] tags;

    public Post(int postId, String text, String timePosted, String username,
                int upvotes, int downvotes, String[] postTags) {
        id = postId;
        content = text;
        postedAt = timePosted;
        postedAtMillis = toMillis(timePosted);
        ownerName = username;
        votesUp = upvotes;
        votesDown = downvotes;
        tags = postTags;
    }

    /**
    * Converts date of format YYYY-MM-DD HH:MM:SS to milliseconds from 1.1.1977
     */
    private static long toMillis(String str) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        try {
            Date date = sdf.parse(str);
            return date.getTime();
        } catch (ParseException e) {
            return -1;
        }
    }

    @Override
    public String toString() {
        return "id=" + id +
                ", content=" + content +
                ", posted at=" + postedAt + " / " + postedAtMillis +
                ", owner=" + ownerName +
                ", votes: (" + votesUp + ", " + votesDown + ")" +
                ", tags=" + tags;
    }
}
