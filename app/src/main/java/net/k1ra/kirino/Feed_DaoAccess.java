package net.k1ra.kirino;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface Feed_DaoAccess {

    @Insert
    void insert(FeedItem item);
    @Insert
    void insert_list(List<FeedItem> items);
    @Query("SELECT * FROM FeedItem WHERE id = :m_id")
    FeedItem fetch(int m_id);
    @Query("SELECT * FROM (SELECT * FROM FeedItem ORDER BY id DESC LIMIT 300) ORDER BY id ASC") //limit to 300 for performance reasons on crappy devices
    List<FeedItem> fetch_all();
    @Query("SELECT * FROM FeedItem")
    List<FeedItem> fetch_all_no_limit();
    @Query("DELETE FROM FeedItem WHERE id < :m_id")
    void delete_old(int m_id);
    @Query("DELETE FROM FeedItem WHERE id = :m_id")
    void delete_one(int m_id);
}
