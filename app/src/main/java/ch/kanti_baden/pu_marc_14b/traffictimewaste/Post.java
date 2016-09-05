package ch.kanti_baden.pu_marc_14b.traffictimewaste;

import java.io.Serializable;

/**
 * Created by marc on 9/5/16.
 */
public class Post implements Serializable {

    public String content;
    public String ownerName;
    public String[] tags;

    public Post(String text, String username, String[] postTags) {
        content = text;
        ownerName = username;
        tags = postTags;
    }

}
