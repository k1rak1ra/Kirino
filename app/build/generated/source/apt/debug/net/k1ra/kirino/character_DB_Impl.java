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
public class character_DB_Impl extends character_DB {
  private volatile Character_DaoAccess _characterDaoAccess;

  @Override
  protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration configuration) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(configuration, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("CREATE TABLE IF NOT EXISTS `Character` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ALid` TEXT, `image_URL` TEXT, `name` TEXT, `description` TEXT, `media` TEXT)");
        _db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        _db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, \"c0c2d28f701f545b6cbe168a244c51ad\")");
      }

      @Override
      public void dropAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("DROP TABLE IF EXISTS `Character`");
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
        final HashMap<String, TableInfo.Column> _columnsCharacter = new HashMap<String, TableInfo.Column>(6);
        _columnsCharacter.put("id", new TableInfo.Column("id", "INTEGER", true, 1));
        _columnsCharacter.put("ALid", new TableInfo.Column("ALid", "TEXT", false, 0));
        _columnsCharacter.put("image_URL", new TableInfo.Column("image_URL", "TEXT", false, 0));
        _columnsCharacter.put("name", new TableInfo.Column("name", "TEXT", false, 0));
        _columnsCharacter.put("description", new TableInfo.Column("description", "TEXT", false, 0));
        _columnsCharacter.put("media", new TableInfo.Column("media", "TEXT", false, 0));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCharacter = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCharacter = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCharacter = new TableInfo("Character", _columnsCharacter, _foreignKeysCharacter, _indicesCharacter);
        final TableInfo _existingCharacter = TableInfo.read(_db, "Character");
        if (! _infoCharacter.equals(_existingCharacter)) {
          throw new IllegalStateException("Migration didn't properly handle Character(net.k1ra.kirino.Character).\n"
                  + " Expected:\n" + _infoCharacter + "\n"
                  + " Found:\n" + _existingCharacter);
        }
      }
    }, "c0c2d28f701f545b6cbe168a244c51ad", "7765b46fdf164f33c2132422a7336271");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
        .name(configuration.name)
        .callback(_openCallback)
        .build();
    final SupportSQLiteOpenHelper _helper = configuration.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  protected InvalidationTracker createInvalidationTracker() {
    return new InvalidationTracker(this, "Character");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `Character`");
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
  public Character_DaoAccess DAO() {
    if (_characterDaoAccess != null) {
      return _characterDaoAccess;
    } else {
      synchronized(this) {
        if(_characterDaoAccess == null) {
          _characterDaoAccess = new Character_DaoAccess_Impl(this);
        }
        return _characterDaoAccess;
      }
    }
  }
}
