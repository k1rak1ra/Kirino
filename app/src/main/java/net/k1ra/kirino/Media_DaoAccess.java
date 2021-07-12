package net.k1ra.kirino;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface Media_DaoAccess {

    @Insert
    void insert(Media item);
    @Insert
    void insert_list(List<Media> items);
    @Query("SELECT * FROM Media WHERE ALid = :m_id")
    Media fetch(int m_id);
    @Query("SELECT * FROM Media")
    List<Media> fetch_all();
    @Query("DELETE FROM MEDIA")
    void  nuke();
    @Query("DELETE FROM MEDIA WHERE ALid = :m_id")
    void  delet_one(String m_id);
    @Update
    void update(Media item);

}
