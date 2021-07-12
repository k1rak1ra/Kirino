package net.k1ra.kirino;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;


@Database(entities = {Character.class}, version = 1, exportSchema = false)
public abstract class character_DB extends RoomDatabase {
    public abstract Character_DaoAccess DAO() ;
}
