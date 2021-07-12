package net.k1ra.kirino;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class Media {
    @PrimaryKey(autoGenerate = true)
    protected int id;
    String ALid;
    String image_URL;
    String name;
    String format;
    String type;
    String score;
    String numEpisodes;
    String description;
    String AirDate;
    boolean current = false;
    boolean has_started_airing = true;
    long new_ep_day = 0;
    int streamType = 0;
    //Stream types:
    //0 = pirate (masterani.me) only
    //1 = Crunchyroll
    //Not a boolean because more stream types could be added in the future (like funimation)

    float p_score = 0;
    int progress = 0;
    int progress_volumes = 0;
    int repeat = 0;

    int started_year = 0;
    int started_month = 0;
    int started_day = 0;

    int ended_year = 0;
    int ended_month = 0;
    int ended_day = 0;

    public Media(String ALid, String image_URL, String name, String type ,String format,  String score, String numEpisodes, String description, String AirDate)
    {
        this.ALid = ALid;
        this.image_URL = image_URL;
        this.name = name;
        this.format = format;
        this.type = type;
        this.score = score;
        this.numEpisodes = numEpisodes;
        this.description = description;
        this.AirDate = AirDate;
    }
}
