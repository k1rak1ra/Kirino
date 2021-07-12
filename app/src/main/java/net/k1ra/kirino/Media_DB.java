package net.k1ra.kirino;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {Media.class}, version = 3, exportSchema = false)
public abstract class Media_DB extends RoomDatabase {
    public abstract Media_DaoAccess DAO() ;
}
