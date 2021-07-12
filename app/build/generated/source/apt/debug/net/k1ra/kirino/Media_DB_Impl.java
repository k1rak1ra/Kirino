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
public class Media_DB_Impl extends Media_DB {
  private volatile Media_DaoAccess _mediaDaoAccess;

  @Override
  protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration configuration) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(configuration, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("CREATE TABLE IF NOT EXISTS `Media` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ALid` TEXT, `image_URL` TEXT, `name` TEXT, `format` TEXT, `type` TEXT, `score` TEXT, `numEpisodes` TEXT, `description` TEXT, `AirDate` TEXT, `current` INTEGER NOT NULL, `has_started_airing` INTEGER NOT NULL, `new_ep_day` INTEGER NOT NULL, `streamType` INTEGER NOT NULL, `p_score` REAL NOT NULL, `progress` INTEGER NOT NULL, `progress_volumes` INTEGER NOT NULL, `repeat` INTEGER NOT NULL, `started_year` INTEGER NOT NULL, `started_month` INTEGER NOT NULL, `started_day` INTEGER NOT NULL, `ended_year` INTEGER NOT NULL, `ended_month` INTEGER NOT NULL, `ended_day` INTEGER NOT NULL)");
        _db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        _db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, \"f61d5d2298ebab97f0e826c93b62a06b\")");
      }

      @Override
      public void dropAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("DROP TABLE IF EXISTS `Media`");
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
        final HashMap<String, TableInfo.Column> _columnsMedia = new HashMap<String, TableInfo.Column>(24);
        _columnsMedia.put("id", new TableInfo.Column("id", "INTEGER", true, 1));
        _columnsMedia.put("ALid", new TableInfo.Column("ALid", "TEXT", false, 0));
        _columnsMedia.put("image_URL", new TableInfo.Column("image_URL", "TEXT", false, 0));
        _columnsMedia.put("name", new TableInfo.Column("name", "TEXT", false, 0));
        _columnsMedia.put("format", new TableInfo.Column("format", "TEXT", false, 0));
        _columnsMedia.put("type", new TableInfo.Column("type", "TEXT", false, 0));
        _columnsMedia.put("score", new TableInfo.Column("score", "TEXT", false, 0));
        _columnsMedia.put("numEpisodes", new TableInfo.Column("numEpisodes", "TEXT", false, 0));
        _columnsMedia.put("description", new TableInfo.Column("description", "TEXT", false, 0));
        _columnsMedia.put("AirDate", new TableInfo.Column("AirDate", "TEXT", false, 0));
        _columnsMedia.put("current", new TableInfo.Column("current", "INTEGER", true, 0));
        _columnsMedia.put("has_started_airing", new TableInfo.Column("has_started_airing", "INTEGER", true, 0));
        _columnsMedia.put("new_ep_day", new TableInfo.Column("new_ep_day", "INTEGER", true, 0));
        _columnsMedia.put("streamType", new TableInfo.Column("streamType", "INTEGER", true, 0));
        _columnsMedia.put("p_score", new TableInfo.Column("p_score", "REAL", true, 0));
        _columnsMedia.put("progress", new TableInfo.Column("progress", "INTEGER", true, 0));
        _columnsMedia.put("progress_volumes", new TableInfo.Column("progress_volumes", "INTEGER", true, 0));
        _columnsMedia.put("repeat", new TableInfo.Column("repeat", "INTEGER", true, 0));
        _columnsMedia.put("started_year", new TableInfo.Column("started_year", "INTEGER", true, 0));
        _columnsMedia.put("started_month", new TableInfo.Column("started_month", "INTEGER", true, 0));
        _columnsMedia.put("started_day", new TableInfo.Column("started_day", "INTEGER", true, 0));
        _columnsMedia.put("ended_year", new TableInfo.Column("ended_year", "INTEGER", true, 0));
        _columnsMedia.put("ended_month", new TableInfo.Column("ended_month", "INTEGER", true, 0));
        _columnsMedia.put("ended_day", new TableInfo.Column("ended_day", "INTEGER", true, 0));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMedia = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMedia = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMedia = new TableInfo("Media", _columnsMedia, _foreignKeysMedia, _indicesMedia);
        final TableInfo _existingMedia = TableInfo.read(_db, "Media");
        if (! _infoMedia.equals(_existingMedia)) {
          throw new IllegalStateException("Migration didn't properly handle Media(net.k1ra.kirino.Media).\n"
                  + " Expected:\n" + _infoMedia + "\n"
                  + " Found:\n" + _existingMedia);
        }
      }
    }, "f61d5d2298ebab97f0e826c93b62a06b", "e1c29bbf11dc0c709ce5e67f6d721b6d");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
        .name(configuration.name)
        .callback(_openCallback)
        .build();
    final SupportSQLiteOpenHelper _helper = configuration.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  protected InvalidationTracker createInvalidationTracker() {
    return new InvalidationTracker(this, "Media");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `Media`");
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
  public Media_DaoAccess DAO() {
    if (_mediaDaoAccess != null) {
      return _mediaDaoAccess;
    } else {
      synchronized(this) {
        if(_mediaDaoAccess == null) {
          _mediaDaoAccess = new Media_DaoAccess_Impl(this);
        }
        return _mediaDaoAccess;
      }
    }
  }
}
