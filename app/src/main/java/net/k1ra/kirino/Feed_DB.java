package net.k1ra.kirino;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {FeedItem.class}, version = 1, exportSchema = false)
public abstract class Feed_DB extends RoomDatabase {
    public abstract Feed_DaoAccess DAO() ;
}
