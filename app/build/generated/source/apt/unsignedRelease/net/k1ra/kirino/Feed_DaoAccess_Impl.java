package net.k1ra.kirino;

import android.arch.persistence.db.SupportSQLiteStatement;
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
public class Feed_DaoAccess_Impl implements Feed_DaoAccess {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter __insertionAdapterOfFeedItem;

  private final SharedSQLiteStatement __preparedStmtOfDelete_old;

  private final SharedSQLiteStatement __preparedStmtOfDelete_one;

  public Feed_DaoAccess_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfFeedItem = new EntityInsertionAdapter<FeedItem>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR ABORT INTO `FeedItem`(`id`,`type`,`json`) VALUES (nullif(?, 0),?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, FeedItem value) {
        stmt.bindLong(1, value.id);
        stmt.bindLong(2, value.type);
        if (value.json == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.json);
        }
      }
    };
    this.__preparedStmtOfDelete_old = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM FeedItem WHERE id < ?";
        return _query;
      }
    };
    this.__preparedStmtOfDelete_one = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM FeedItem WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public void insert(FeedItem item) {
    __db.beginTransaction();
    try {
      __insertionAdapterOfFeedItem.insert(item);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insert_list(List<FeedItem> items) {
    __db.beginTransaction();
    try {
      __insertionAdapterOfFeedItem.insert(items);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete_old(int m_id) {
    final SupportSQLiteStatement _stmt = __preparedStmtOfDelete_old.acquire();
    __db.beginTransaction();
    try {
      int _argIndex = 1;
      _stmt.bindLong(_argIndex, m_id);
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDelete_old.release(_stmt);
    }
  }

  @Override
  public void delete_one(int m_id) {
    final SupportSQLiteStatement _stmt = __preparedStmtOfDelete_one.acquire();
    __db.beginTransaction();
    try {
      int _argIndex = 1;
      _stmt.bindLong(_argIndex, m_id);
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDelete_one.release(_stmt);
    }
  }

  @Override
  public FeedItem fetch(int m_id) {
    final String _sql = "SELECT * FROM FeedItem WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, m_id);
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfId = _cursor.getColumnIndexOrThrow("id");
      final int _cursorIndexOfType = _cursor.getColumnIndexOrThrow("type");
      final int _cursorIndexOfJson = _cursor.getColumnIndexOrThrow("json");
      final FeedItem _result;
      if(_cursor.moveToFirst()) {
        final int _tmpType;
        _tmpType = _cursor.getInt(_cursorIndexOfType);
        final String _tmpJson;
        _tmpJson = _cursor.getString(_cursorIndexOfJson);
        _result = new FeedItem(_tmpType,_tmpJson);
        _result.id = _cursor.getInt(_cursorIndexOfId);
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
  public List<FeedItem> fetch_all() {
    final String _sql = "SELECT * FROM (SELECT * FROM FeedItem ORDER BY id DESC LIMIT 300) ORDER BY id ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfId = _cursor.getColumnIndexOrThrow("id");
      final int _cursorIndexOfType = _cursor.getColumnIndexOrThrow("type");
      final int _cursorIndexOfJson = _cursor.getColumnIndexOrThrow("json");
      final List<FeedItem> _result = new ArrayList<FeedItem>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final FeedItem _item;
        final int _tmpType;
        _tmpType = _cursor.getInt(_cursorIndexOfType);
        final String _tmpJson;
        _tmpJson = _cursor.getString(_cursorIndexOfJson);
        _item = new FeedItem(_tmpType,_tmpJson);
        _item.id = _cursor.getInt(_cursorIndexOfId);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<FeedItem> fetch_all_no_limit() {
    final String _sql = "SELECT * FROM FeedItem";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfId = _cursor.getColumnIndexOrThrow("id");
      final int _cursorIndexOfType = _cursor.getColumnIndexOrThrow("type");
      final int _cursorIndexOfJson = _cursor.getColumnIndexOrThrow("json");
      final List<FeedItem> _result = new ArrayList<FeedItem>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final FeedItem _item;
        final int _tmpType;
        _tmpType = _cursor.getInt(_cursorIndexOfType);
        final String _tmpJson;
        _tmpJson = _cursor.getString(_cursorIndexOfJson);
        _item = new FeedItem(_tmpType,_tmpJson);
        _item.id = _cursor.getInt(_cursorIndexOfId);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }
}
