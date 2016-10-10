package ch.kanti_baden.pu_marc_14b.traffictimewaste;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class Post implements Serializable {

    public final int id;
    public final String content;
    public final String postedAt;
    public final long postedAtMillis;
    public final String ownerName;
    public final int votesUp;
    public final int votesDown;
    public final String[] tags;
    public boolean clickable = true;

    public Post(int postId, String text, String timePosted, String username,
                int upvotes, int downvotes, final String[] postTags) {
        id = postId;
        content = text;
        postedAt = timePosted;
        postedAtMillis = toMillis(timePosted);
        ownerName = username;
        votesUp = upvotes;
        votesDown = downvotes;
        tags = postTags.clone();
    }

    /**
    * Converts date of format YYYY-MM-DD HH:MM:SS to milliseconds since January 1, 1970 GMT
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
                ", tags=" + Arrays.toString(tags);
    }
}
