package net.k1ra.kirino;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class Character {
    @PrimaryKey(autoGenerate = true)
    protected int id;
    String ALid;
    String image_URL;
    String name;
    String description;
    String media;

    public Character(String ALid, String image_URL, String name, String description)
    {
        this.ALid = ALid;
        this.image_URL = image_URL;
        this.name = name;
        this.description = description;
    }
}
