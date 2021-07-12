package net.k1ra.kirino;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;


import java.util.List;

@Dao
public interface Character_DaoAccess {

    @Insert
    void insert_single(Character character);
    @Insert
    void insert_list(List<Character> character);
    @Query("SELECT * FROM Character WHERE ALid = :m_id")
    Character fetch(int m_id);
    @Query("SELECT * FROM Character")
    List<Character> fetch_list();
    @Query("DELETE FROM Character WHERE ALid = :m_id")
    void delete(String m_id);
    @Query("DELETE FROM Character")
    void nuke();
    @Update
    void update(Character item);

}
