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
public class Character_DaoAccess_Impl implements Character_DaoAccess {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter __insertionAdapterOfCharacter;

  private final EntityDeletionOrUpdateAdapter __updateAdapterOfCharacter;

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  private final SharedSQLiteStatement __preparedStmtOfNuke;

  public Character_DaoAccess_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCharacter = new EntityInsertionAdapter<Character>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR ABORT INTO `Character`(`id`,`ALid`,`image_URL`,`name`,`description`,`media`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Character value) {
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
        if (value.description == null) {
          stmt.bindNull(5);
        } else {
          stmt.bindString(5, value.description);
        }
        if (value.media == null) {
          stmt.bindNull(6);
        } else {
          stmt.bindString(6, value.media);
        }
      }
    };
    this.__updateAdapterOfCharacter = new EntityDeletionOrUpdateAdapter<Character>(__db) {
      @Override
      public String createQuery() {
        return "UPDATE OR ABORT `Character` SET `id` = ?,`ALid` = ?,`image_URL` = ?,`name` = ?,`description` = ?,`media` = ? WHERE `id` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Character value) {
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
        if (value.description == null) {
          stmt.bindNull(5);
        } else {
          stmt.bindString(5, value.description);
        }
        if (value.media == null) {
          stmt.bindNull(6);
        } else {
          stmt.bindString(6, value.media);
        }
        stmt.bindLong(7, value.id);
      }
    };
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM Character WHERE ALid = ?";
        return _query;
      }
    };
    this.__preparedStmtOfNuke = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM Character";
        return _query;
      }
    };
  }

  @Override
  public void insert_single(Character character) {
    __db.beginTransaction();
    try {
      __insertionAdapterOfCharacter.insert(character);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insert_list(List<Character> character) {
    __db.beginTransaction();
    try {
      __insertionAdapterOfCharacter.insert(character);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(Character item) {
    __db.beginTransaction();
    try {
      __updateAdapterOfCharacter.handle(item);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(String m_id) {
    final SupportSQLiteStatement _stmt = __preparedStmtOfDelete.acquire();
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
      __preparedStmtOfDelete.release(_stmt);
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
  public Character fetch(int m_id) {
    final String _sql = "SELECT * FROM Character WHERE ALid = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, m_id);
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfId = _cursor.getColumnIndexOrThrow("id");
      final int _cursorIndexOfALid = _cursor.getColumnIndexOrThrow("ALid");
      final int _cursorIndexOfImageURL = _cursor.getColumnIndexOrThrow("image_URL");
      final int _cursorIndexOfName = _cursor.getColumnIndexOrThrow("name");
      final int _cursorIndexOfDescription = _cursor.getColumnIndexOrThrow("description");
      final int _cursorIndexOfMedia = _cursor.getColumnIndexOrThrow("media");
      final Character _result;
      if(_cursor.moveToFirst()) {
        final String _tmpALid;
        _tmpALid = _cursor.getString(_cursorIndexOfALid);
        final String _tmpImage_URL;
        _tmpImage_URL = _cursor.getString(_cursorIndexOfImageURL);
        final String _tmpName;
        _tmpName = _cursor.getString(_cursorIndexOfName);
        final String _tmpDescription;
        _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
        _result = new Character(_tmpALid,_tmpImage_URL,_tmpName,_tmpDescription);
        _result.id = _cursor.getInt(_cursorIndexOfId);
        _result.media = _cursor.getString(_cursorIndexOfMedia);
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
  public List<Character> fetch_list() {
    final String _sql = "SELECT * FROM Character";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfId = _cursor.getColumnIndexOrThrow("id");
      final int _cursorIndexOfALid = _cursor.getColumnIndexOrThrow("ALid");
      final int _cursorIndexOfImageURL = _cursor.getColumnIndexOrThrow("image_URL");
      final int _cursorIndexOfName = _cursor.getColumnIndexOrThrow("name");
      final int _cursorIndexOfDescription = _cursor.getColumnIndexOrThrow("description");
      final int _cursorIndexOfMedia = _cursor.getColumnIndexOrThrow("media");
      final List<Character> _result = new ArrayList<Character>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final Character _item;
        final String _tmpALid;
        _tmpALid = _cursor.getString(_cursorIndexOfALid);
        final String _tmpImage_URL;
        _tmpImage_URL = _cursor.getString(_cursorIndexOfImageURL);
        final String _tmpName;
        _tmpName = _cursor.getString(_cursorIndexOfName);
        final String _tmpDescription;
        _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
        _item = new Character(_tmpALid,_tmpImage_URL,_tmpName,_tmpDescription);
        _item.id = _cursor.getInt(_cursorIndexOfId);
        _item.media = _cursor.getString(_cursorIndexOfMedia);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }
}
