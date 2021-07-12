package net.k1ra.kirino;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;


@Entity
public class FeedItem {
    @PrimaryKey(autoGenerate = true)
    protected int id;
    int type;
    String json;

    public FeedItem(int type, String json)
    {
        this.type = type;
        this.json = json;
    }
}
