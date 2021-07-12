package net.k1ra.kirino;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.arch.persistence.db.SupportSQLiteOpenHelper.Callback;
import android.arch.persistence.db.SupportSQLiteOpenHelper.Configuration;
import android.arch.persistence.room.DatabaseConfiguration;
import android.arch.persistence.room.InvalidationTracker;
import android.arch.persistence.room.RoomOpenHelper;
import android.arch.persistence.room.RoomOpenHelper.Delegate;
import android.arch.persistence.room.util.TableInfo;
import android.arch.persistence.room.util.TableInfo.Column;
import android.arch.persistence.room.util.TableInfo.ForeignKey;
import android.arch.persistence.room.util.TableInfo.Index;
import java.lang.IllegalStateException;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;
import java.util.HashSet;

@SuppressWarnings("unchecked")
public class Feed_DB_Impl extends Feed_DB {
  private volatile Feed_DaoAccess _feedDaoAccess;

  @Override
  protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration configuration) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(configuration, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("CREATE TABLE IF NOT EXISTS `FeedItem` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` INTEGER NOT NULL, `json` TEXT)");
        _db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        _db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, \"be5368c513a4d571748d90f6c2f7226a\")");
      }

      @Override
      public void dropAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("DROP TABLE IF EXISTS `FeedItem`");
      }

      @Override
      protected void onCreate(SupportSQLiteDatabase _db) {
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onCreate(_db);
          }
        }
      }

      @Override
      public void onOpen(SupportSQLiteDatabase _db) {
        mDatabase = _db;
        internalInitInvalidationTracker(_db);
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onOpen(_db);
          }
        }
      }

      @Override
      protected void validateMigration(SupportSQLiteDatabase _db) {
        final HashMap<String, TableInfo.Column> _columnsFeedItem = new HashMap<String, TableInfo.Column>(3);
        _columnsFeedItem.put("id", new TableInfo.Column("id", "INTEGER", true, 1));
        _columnsFeedItem.put("type", new TableInfo.Column("type", "INTEGER", true, 0));
        _columnsFeedItem.put("json", new TableInfo.Column("json", "TEXT", false, 0));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFeedItem = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFeedItem = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoFeedItem = new TableInfo("FeedItem", _columnsFeedItem, _foreignKeysFeedItem, _indicesFeedItem);
        final TableInfo _existingFeedItem = TableInfo.read(_db, "FeedItem");
        if (! _infoFeedItem.equals(_existingFeedItem)) {
          throw new IllegalStateException("Migration didn't properly handle FeedItem(net.k1ra.kirino.FeedItem).\n"
                  + " Expected:\n" + _infoFeedItem + "\n"
                  + " Found:\n" + _existingFeedItem);
        }
      }
    }, "be5368c513a4d571748d90f6c2f7226a", "f42c423446a22d14aba6539130aa824a");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
        .name(configuration.name)
        .callback(_openCallback)
        .build();
    final SupportSQLiteOpenHelper _helper = configuration.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  protected InvalidationTracker createInvalidationTracker() {
    return new InvalidationTracker(this, "FeedItem");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `FeedItem`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  public Feed_DaoAccess DAO() {
    if (_feedDaoAccess != null) {
      return _feedDaoAccess;
    } else {
      synchronized(this) {
        if(_feedDaoAccess == null) {
          _feedDaoAccess = new Feed_DaoAccess_Impl(this);
        }
        return _feedDaoAccess;
      }
    }
  }
}
