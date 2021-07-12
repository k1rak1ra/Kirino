package net.k1ra.kirino;

import android.arch.persistence.db.SupportSQLiteStatement;
import android.arch.persistence.room.EntityDeletionOrUpdateAdapter;
import android.arch.persistence.room.EntityInsertionAdapter;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.RoomSQLiteQuery;
import android.arch.persistence.room.SharedSQLiteStatement;
import android.database.Cursor;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class Media_DaoAccess_Impl implements Media_DaoAccess {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter __insertionAdapterOfMedia;

  private final EntityDeletionOrUpdateAdapter __updateAdapterOfMedia;

  private final SharedSQLiteStatement __preparedStmtOfNuke;

  private final SharedSQLiteStatement __preparedStmtOfDelet_one;

  public Media_DaoAccess_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMedia = new EntityInsertionAdapter<Media>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR ABORT INTO `Media`(`id`,`ALid`,`image_URL`,`name`,`format`,`type`,`score`,`numEpisodes`,`description`,`AirDate`,`current`,`has_started_airing`,`new_ep_day`,`streamType`,`p_score`,`progress`,`progress_volumes`,`repeat`,`started_year`,`started_month`,`started_day`,`ended_year`,`ended_month`,`ended_day`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Media value) {
        stmt.bindLong(1, value.id);
        if (value.ALid == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.ALid);
        }
        if (value.image_URL == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.image_URL);
        }
        if (value.name == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.name);
        }
        if (value.format == null) {
          stmt.bindNull(5);
        } else {
          stmt.bindString(5, value.format);
        }
        if (value.type == null) {
          stmt.bindNull(6);
        } else {
          stmt.bindString(6, value.type);
        }
        if (value.score == null) {
          stmt.bindNull(7);
        } else {
          stmt.bindString(7, value.score);
        }
        if (value.numEpisodes == null) {
          stmt.bindNull(8);
        } else {
          stmt.bindString(8, value.numEpisodes);
        }
        if (value.description == null) {
          stmt.bindNull(9);
        } else {
          stmt.bindString(9, value.description);
        }
        if (value.AirDate == null) {
          stmt.bindNull(10);
        } else {
          stmt.bindString(10, value.AirDate);
        }
        final int _tmp;
        _tmp = value.current ? 1 : 0;
        stmt.bindLong(11, _tmp);
        final int _tmp_1;
        _tmp_1 = value.has_started_airing ? 1 : 0;
        stmt.bindLong(12, _tmp_1);
        stmt.bindLong(13, value.new_ep_day);
        stmt.bindLong(14, value.streamType);
        stmt.bindDouble(15, value.p_score);
        stmt.bindLong(16, value.progress);
        stmt.bindLong(17, value.progress_volumes);
        stmt.bindLong(18, value.repeat);
        stmt.bindLong(19, value.started_year);
        stmt.bindLong(20, value.started_month);
        stmt.bindLong(21, value.started_day);
        stmt.bindLong(22, value.ended_year);
        stmt.bindLong(23, value.ended_month);
        stmt.bindLong(24, value.ended_day);
      }
    };
    this.__updateAdapterOfMedia = new EntityDeletionOrUpdateAdapter<Media>(__db) {
      @Override
      public String createQuery() {
        return "UPDATE OR ABORT `Media` SET `id` = ?,`ALid` = ?,`image_URL` = ?,`name` = ?,`format` = ?,`type` = ?,`score` = ?,`numEpisodes` = ?,`description` = ?,`AirDate` = ?,`current` = ?,`has_started_airing` = ?,`new_ep_day` = ?,`streamType` = ?,`p_score` = ?,`progress` = ?,`progress_volumes` = ?,`repeat` = ?,`started_year` = ?,`started_month` = ?,`started_day` = ?,`ended_year` = ?,`ended_month` = ?,`ended_day` = ? WHERE `id` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Media value) {
        stmt.bindLong(1, value.id);
        if (value.ALid == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.ALid);
        }
        if (value.image_URL == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.image_URL);
        }
        if (value.name == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.name);
        }
        if (value.format == null) {
          stmt.bindNull(5);
        } else {
          stmt.bindString(5, value.format);
        }
        if (value.type == null) {
          stmt.bindNull(6);
        } else {
          stmt.bindString(6, value.type);
        }
        if (value.score == null) {
          stmt.bindNull(7);
        } else {
          stmt.bindString(7, value.score);
        }
        if (value.numEpisodes == null) {
          stmt.bindNull(8);
        } else {
          stmt.bindString(8, value.numEpisodes);
        }
        if (value.description == null) {
          stmt.bindNull(9);
        } else {
          stmt.bindString(9, value.description);
        }
        if (value.AirDate == null) {
          stmt.bindNull(10);
        } else {
          stmt.bindString(10, value.AirDate);
        }
        final int _tmp;
        _tmp = value.current ? 1 : 0;
        stmt.bindLong(11, _tmp);
        final int _tmp_1;
        _tmp_1 = value.has_started_airing ? 1 : 0;
        stmt.bindLong(12, _tmp_1);
        stmt.bindLong(13, value.new_ep_day);
        stmt.bindLong(14, value.streamType);
        stmt.bindDouble(15, value.p_score);
        stmt.bindLong(16, value.progress);
        stmt.bindLong(17, value.progress_volumes);
        stmt.bindLong(18, value.repeat);
        stmt.bindLong(19, value.started_year);
        stmt.bindLong(20, value.started_month);
        stmt.bindLong(21, value.started_day);
        stmt.bindLong(22, value.ended_year);
        stmt.bindLong(23, value.ended_month);
        stmt.bindLong(24, value.ended_day);
        stmt.bindLong(25, value.id);
      }
    };
    this.__preparedStmtOfNuke = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM MEDIA";
        return _query;
      }
    };
    this.__preparedStmtOfDelet_one = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM MEDIA WHERE ALid = ?";
        return _query;
      }
    };
  }

  @Override
  public void insert(Media item) {
    __db.beginTransaction();
    try {
      __insertionAdapterOfMedia.insert(item);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insert_list(List<Media> items) {
    __db.beginTransaction();
    try {
      __insertionAdapterOfMedia.insert(items);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(Media item) {
    __db.beginTransaction();
    try {
      __updateAdapterOfMedia.handle(item);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void nuke() {
    final SupportSQLiteStatement _stmt = __preparedStmtOfNuke.acquire();
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfNuke.release(_stmt);
    }
  }

  @Override
  public void delet_one(String m_id) {
    final SupportSQLiteStatement _stmt = __preparedStmtOfDelet_one.acquire();
    __db.beginTransaction();
    try {
      int _argIndex = 1;
      if (m_id == null) {
        _stmt.bindNull(_argIndex);
      } else {
        _stmt.bindString(_argIndex, m_id);
      }
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDelet_one.release(_stmt);
    }
  }

  @Override
  public Media fetch(int m_id) {
    final String _sql = "SELECT * FROM Media WHERE ALid = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, m_id);
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfId = _cursor.getColumnIndexOrThrow("id");
      final int _cursorIndexOfALid = _cursor.getColumnIndexOrThrow("ALid");
      final int _cursorIndexOfImageURL = _cursor.getColumnIndexOrThrow("image_URL");
      final int _cursorIndexOfName = _cursor.getColumnIndexOrThrow("name");
      final int _cursorIndexOfFormat = _cursor.getColumnIndexOrThrow("format");
      final int _cursorIndexOfType = _cursor.getColumnIndexOrThrow("type");
      final int _cursorIndexOfScore = _cursor.getColumnIndexOrThrow("score");
      final int _cursorIndexOfNumEpisodes = _cursor.getColumnIndexOrThrow("numEpisodes");
      final int _cursorIndexOfDescription = _cursor.getColumnIndexOrThrow("description");
      final int _cursorIndexOfAirDate = _cursor.getColumnIndexOrThrow("AirDate");
      final int _cursorIndexOfCurrent = _cursor.getColumnIndexOrThrow("current");
      final int _cursorIndexOfHasStartedAiring = _cursor.getColumnIndexOrThrow("has_started_airing");
      final int _cursorIndexOfNewEpDay = _cursor.getColumnIndexOrThrow("new_ep_day");
      final int _cursorIndexOfStreamType = _cursor.getColumnIndexOrThrow("streamType");
      final int _cursorIndexOfPScore = _cursor.getColumnIndexOrThrow("p_score");
      final int _cursorIndexOfProgress = _cursor.getColumnIndexOrThrow("progress");
      final int _cursorIndexOfProgressVolumes = _cursor.getColumnIndexOrThrow("progress_volumes");
      final int _cursorIndexOfRepeat = _cursor.getColumnIndexOrThrow("repeat");
      final int _cursorIndexOfStartedYear = _cursor.getColumnIndexOrThrow("started_year");
      final int _cursorIndexOfStartedMonth = _cursor.getColumnIndexOrThrow("started_month");
      final int _cursorIndexOfStartedDay = _cursor.getColumnIndexOrThrow("started_day");
      final int _cursorIndexOfEndedYear = _cursor.getColumnIndexOrThrow("ended_year");
      final int _cursorIndexOfEndedMonth = _cursor.getColumnIndexOrThrow("ended_month");
      final int _cursorIndexOfEndedDay = _cursor.getColumnIndexOrThrow("ended_day");
      final Media _result;
      if(_cursor.moveToFirst()) {
        final String _tmpALid;
        _tmpALid = _cursor.getString(_cursorIndexOfALid);
        final String _tmpImage_URL;
        _tmpImage_URL = _cursor.getString(_cursorIndexOfImageURL);
        final String _tmpName;
        _tmpName = _cursor.getString(_cursorIndexOfName);
        final String _tmpFormat;
        _tmpFormat = _cursor.getString(_cursorIndexOfFormat);
        final String _tmpType;
        _tmpType = _cursor.getString(_cursorIndexOfType);
        final String _tmpScore;
        _tmpScore = _cursor.getString(_cursorIndexOfScore);
        final String _tmpNumEpisodes;
        _tmpNumEpisodes = _cursor.getString(_cursorIndexOfNumEpisodes);
        final String _tmpDescription;
        _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
        final String _tmpAirDate;
        _tmpAirDate = _cursor.getString(_cursorIndexOfAirDate);
        _result = new Media(_tmpALid,_tmpImage_URL,_tmpName,_tmpType,_tmpFormat,_tmpScore,_tmpNumEpisodes,_tmpDescription,_tmpAirDate);
        _result.id = _cursor.getInt(_cursorIndexOfId);
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfCurrent);
        _result.current = _tmp != 0;
        final int _tmp_1;
        _tmp_1 = _cursor.getInt(_cursorIndexOfHasStartedAiring);
        _result.has_started_airing = _tmp_1 != 0;
        _result.new_ep_day = _cursor.getLong(_cursorIndexOfNewEpDay);
        _result.streamType = _cursor.getInt(_cursorIndexOfStreamType);
        _result.p_score = _cursor.getFloat(_cursorIndexOfPScore);
        _result.progress = _cursor.getInt(_cursorIndexOfProgress);
        _result.progress_volumes = _cursor.getInt(_cursorIndexOfProgressVolumes);
        _result.repeat = _cursor.getInt(_cursorIndexOfRepeat);
        _result.started_year = _cursor.getInt(_cursorIndexOfStartedYear);
        _result.started_month = _cursor.getInt(_cursorIndexOfStartedMonth);
        _result.started_day = _cursor.getInt(_cursorIndexOfStartedDay);
        _result.ended_year = _cursor.getInt(_cursorIndexOfEndedYear);
        _result.ended_month = _cursor.getInt(_cursorIndexOfEndedMonth);
        _result.ended_day = _cursor.getInt(_cursorIndexOfEndedDay);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<Media> fetch_all() {
    final String _sql = "SELECT * FROM Media";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfId = _cursor.getColumnIndexOrThrow("id");
      final int _cursorIndexOfALid = _cursor.getColumnIndexOrThrow("ALid");
      final int _cursorIndexOfImageURL = _cursor.getColumnIndexOrThrow("image_URL");
      final int _cursorIndexOfName = _cursor.getColumnIndexOrThrow("name");
      final int _cursorIndexOfFormat = _cursor.getColumnIndexOrThrow("format");
      final int _cursorIndexOfType = _cursor.getColumnIndexOrThrow("type");
      final int _cursorIndexOfScore = _cursor.getColumnIndexOrThrow("score");
      final int _cursorIndexOfNumEpisodes = _cursor.getColumnIndexOrThrow("numEpisodes");
      final int _cursorIndexOfDescription = _cursor.getColumnIndexOrThrow("description");
      final int _cursorIndexOfAirDate = _cursor.getColumnIndexOrThrow("AirDate");
      final int _cursorIndexOfCurrent = _cursor.getColumnIndexOrThrow("current");
      final int _cursorIndexOfHasStartedAiring = _cursor.getColumnIndexOrThrow("has_started_airing");
      final int _cursorIndexOfNewEpDay = _cursor.getColumnIndexOrThrow("new_ep_day");
      final int _cursorIndexOfStreamType = _cursor.getColumnIndexOrThrow("streamType");
      final int _cursorIndexOfPScore = _cursor.getColumnIndexOrThrow("p_score");
      final int _cursorIndexOfProgress = _cursor.getColumnIndexOrThrow("progress");
      final int _cursorIndexOfProgressVolumes = _cursor.getColumnIndexOrThrow("progress_volumes");
      final int _cursorIndexOfRepeat = _cursor.getColumnIndexOrThrow("repeat");
      final int _cursorIndexOfStartedYear = _cursor.getColumnIndexOrThrow("started_year");
      final int _cursorIndexOfStartedMonth = _cursor.getColumnIndexOrThrow("started_month");
      final int _cursorIndexOfStartedDay = _cursor.getColumnIndexOrThrow("started_day");
      final int _cursorIndexOfEndedYear = _cursor.getColumnIndexOrThrow("ended_year");
      final int _cursorIndexOfEndedMonth = _cursor.getColumnIndexOrThrow("ended_month");
      final int _cursorIndexOfEndedDay = _cursor.getColumnIndexOrThrow("ended_day");
      final List<Media> _result = new ArrayList<Media>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final Media _item;
        final String _tmpALid;
        _tmpALid = _cursor.getString(_cursorIndexOfALid);
        final String _tmpImage_URL;
        _tmpImage_URL = _cursor.getString(_cursorIndexOfImageURL);
        final String _tmpName;
        _tmpName = _cursor.getString(_cursorIndexOfName);
        final String _tmpFormat;
        _tmpFormat = _cursor.getString(_cursorIndexOfFormat);
        final String _tmpType;
        _tmpType = _cursor.getString(_cursorIndexOfType);
        final String _tmpScore;
        _tmpScore = _cursor.getString(_cursorIndexOfScore);
        final String _tmpNumEpisodes;
        _tmpNumEpisodes = _cursor.getString(_cursorIndexOfNumEpisodes);
        final String _tmpDescription;
        _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
        final String _tmpAirDate;
        _tmpAirDate = _cursor.getString(_cursorIndexOfAirDate);
        _item = new Media(_tmpALid,_tmpImage_URL,_tmpName,_tmpType,_tmpFormat,_tmpScore,_tmpNumEpisodes,_tmpDescription,_tmpAirDate);
        _item.id = _cursor.getInt(_cursorIndexOfId);
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfCurrent);
        _item.current = _tmp != 0;
        final int _tmp_1;
        _tmp_1 = _cursor.getInt(_cursorIndexOfHasStartedAiring);
        _item.has_started_airing = _tmp_1 != 0;
        _item.new_ep_day = _cursor.getLong(_cursorIndexOfNewEpDay);
        _item.streamType = _cursor.getInt(_cursorIndexOfStreamType);
        _item.p_score = _cursor.getFloat(_cursorIndexOfPScore);
        _item.progress = _cursor.getInt(_cursorIndexOfProgress);
        _item.progress_volumes = _cursor.getInt(_cursorIndexOfProgressVolumes);
        _item.repeat = _cursor.getInt(_cursorIndexOfRepeat);
        _item.started_year = _cursor.getInt(_cursorIndexOfStartedYear);
        _item.started_month = _cursor.getInt(_cursorIndexOfStartedMonth);
        _item.started_day = _cursor.getInt(_cursorIndexOfStartedDay);
        _item.ended_year = _cursor.getInt(_cursorIndexOfEndedYear);
        _item.ended_month = _cursor.getInt(_cursorIndexOfEndedMonth);
        _item.ended_day = _cursor.getInt(_cursorIndexOfEndedDay);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }
}
