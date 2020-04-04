package eu.rtsketo.sakurastats.control;

import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomOpenHelper;
import androidx.room.RoomOpenHelper.Delegate;
import androidx.room.util.TableInfo;
import androidx.room.util.TableInfo.Column;
import androidx.room.util.TableInfo.ForeignKey;
import androidx.room.util.TableInfo.Index;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteOpenHelper.Callback;
import androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration;
import java.lang.IllegalStateException;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;
import java.util.HashSet;

@SuppressWarnings("unchecked")
public final class DataRoom_Impl extends DataRoom {
  private volatile DAObject _dAObject;

  @Override
  protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration configuration) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(configuration, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("CREATE TABLE IF NOT EXISTS `WarDay` (`warDay` INTEGER NOT NULL, `tag` TEXT NOT NULL, PRIMARY KEY(`warDay`))");
        _db.execSQL("CREATE TABLE IF NOT EXISTS `PlayerStats` (`tag` TEXT NOT NULL, `name` TEXT NOT NULL, `clan` TEXT NOT NULL, `wins` INTEGER NOT NULL, `played` INTEGER NOT NULL, `curWins` INTEGER NOT NULL, `curPlay` INTEGER NOT NULL, `ratio` REAL NOT NULL, `norma` REAL NOT NULL, `current` INTEGER NOT NULL, `missed` INTEGER NOT NULL, `cards` INTEGER NOT NULL, `wars` INTEGER NOT NULL, `chest` INTEGER NOT NULL, PRIMARY KEY(`tag`))");
        _db.execSQL("CREATE TABLE IF NOT EXISTS `ClanStats` (`tag` TEXT NOT NULL, `name` TEXT NOT NULL, `state` TEXT NOT NULL, `badge` TEXT NOT NULL, `maxParticipants` INTEGER NOT NULL, `estimatedWins` INTEGER NOT NULL, `extraWins` REAL NOT NULL, `warTrophies` INTEGER NOT NULL, `actualWins` INTEGER NOT NULL, `remaining` INTEGER NOT NULL, `crowns` INTEGER NOT NULL, `clan1` TEXT NOT NULL, `clan2` TEXT NOT NULL, `clan3` TEXT NOT NULL, `clan4` TEXT NOT NULL, PRIMARY KEY(`tag`))");
        _db.execSQL("CREATE TABLE IF NOT EXISTS `ClanPlayer` (`tag` TEXT NOT NULL, `clan` TEXT NOT NULL, `last` INTEGER NOT NULL, `score` INTEGER NOT NULL, `trophies` INTEGER NOT NULL, `smc` INTEGER NOT NULL, `legendary` INTEGER NOT NULL, `magical` INTEGER NOT NULL, `role` TEXT NOT NULL, PRIMARY KEY(`tag`))");
        _db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        _db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, \"882a00bd1f0c24f8ff660b8e600f1550\")");
      }

      @Override
      public void dropAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("DROP TABLE IF EXISTS `WarDay`");
        _db.execSQL("DROP TABLE IF EXISTS `PlayerStats`");
        _db.execSQL("DROP TABLE IF EXISTS `ClanStats`");
        _db.execSQL("DROP TABLE IF EXISTS `ClanPlayer`");
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
        final HashMap<String, TableInfo.Column> _columnsWarDay = new HashMap<String, TableInfo.Column>(2);
        _columnsWarDay.put("warDay", new TableInfo.Column("warDay", "INTEGER", true, 1));
        _columnsWarDay.put("tag", new TableInfo.Column("tag", "TEXT", true, 0));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWarDay = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWarDay = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoWarDay = new TableInfo("WarDay", _columnsWarDay, _foreignKeysWarDay, _indicesWarDay);
        final TableInfo _existingWarDay = TableInfo.read(_db, "WarDay");
        if (! _infoWarDay.equals(_existingWarDay)) {
          throw new IllegalStateException("Migration didn't properly handle WarDay(eu.rtsketo.sakurastats.dbobjects.WarDay).\n"
                  + " Expected:\n" + _infoWarDay + "\n"
                  + " Found:\n" + _existingWarDay);
        }
        final HashMap<String, TableInfo.Column> _columnsPlayerStats = new HashMap<String, TableInfo.Column>(14);
        _columnsPlayerStats.put("tag", new TableInfo.Column("tag", "TEXT", true, 1));
        _columnsPlayerStats.put("name", new TableInfo.Column("name", "TEXT", true, 0));
        _columnsPlayerStats.put("clan", new TableInfo.Column("clan", "TEXT", true, 0));
        _columnsPlayerStats.put("wins", new TableInfo.Column("wins", "INTEGER", true, 0));
        _columnsPlayerStats.put("played", new TableInfo.Column("played", "INTEGER", true, 0));
        _columnsPlayerStats.put("curWins", new TableInfo.Column("curWins", "INTEGER", true, 0));
        _columnsPlayerStats.put("curPlay", new TableInfo.Column("curPlay", "INTEGER", true, 0));
        _columnsPlayerStats.put("ratio", new TableInfo.Column("ratio", "REAL", true, 0));
        _columnsPlayerStats.put("norma", new TableInfo.Column("norma", "REAL", true, 0));
        _columnsPlayerStats.put("current", new TableInfo.Column("current", "INTEGER", true, 0));
        _columnsPlayerStats.put("missed", new TableInfo.Column("missed", "INTEGER", true, 0));
        _columnsPlayerStats.put("cards", new TableInfo.Column("cards", "INTEGER", true, 0));
        _columnsPlayerStats.put("wars", new TableInfo.Column("wars", "INTEGER", true, 0));
        _columnsPlayerStats.put("chest", new TableInfo.Column("chest", "INTEGER", true, 0));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPlayerStats = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPlayerStats = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPlayerStats = new TableInfo("PlayerStats", _columnsPlayerStats, _foreignKeysPlayerStats, _indicesPlayerStats);
        final TableInfo _existingPlayerStats = TableInfo.read(_db, "PlayerStats");
        if (! _infoPlayerStats.equals(_existingPlayerStats)) {
          throw new IllegalStateException("Migration didn't properly handle PlayerStats(eu.rtsketo.sakurastats.dbobjects.PlayerStats).\n"
                  + " Expected:\n" + _infoPlayerStats + "\n"
                  + " Found:\n" + _existingPlayerStats);
        }
        final HashMap<String, TableInfo.Column> _columnsClanStats = new HashMap<String, TableInfo.Column>(15);
        _columnsClanStats.put("tag", new TableInfo.Column("tag", "TEXT", true, 1));
        _columnsClanStats.put("name", new TableInfo.Column("name", "TEXT", true, 0));
        _columnsClanStats.put("state", new TableInfo.Column("state", "TEXT", true, 0));
        _columnsClanStats.put("badge", new TableInfo.Column("badge", "TEXT", true, 0));
        _columnsClanStats.put("maxParticipants", new TableInfo.Column("maxParticipants", "INTEGER", true, 0));
        _columnsClanStats.put("estimatedWins", new TableInfo.Column("estimatedWins", "INTEGER", true, 0));
        _columnsClanStats.put("extraWins", new TableInfo.Column("extraWins", "REAL", true, 0));
        _columnsClanStats.put("warTrophies", new TableInfo.Column("warTrophies", "INTEGER", true, 0));
        _columnsClanStats.put("actualWins", new TableInfo.Column("actualWins", "INTEGER", true, 0));
        _columnsClanStats.put("remaining", new TableInfo.Column("remaining", "INTEGER", true, 0));
        _columnsClanStats.put("crowns", new TableInfo.Column("crowns", "INTEGER", true, 0));
        _columnsClanStats.put("clan1", new TableInfo.Column("clan1", "TEXT", true, 0));
        _columnsClanStats.put("clan2", new TableInfo.Column("clan2", "TEXT", true, 0));
        _columnsClanStats.put("clan3", new TableInfo.Column("clan3", "TEXT", true, 0));
        _columnsClanStats.put("clan4", new TableInfo.Column("clan4", "TEXT", true, 0));
        final HashSet<TableInfo.ForeignKey> _foreignKeysClanStats = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesClanStats = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoClanStats = new TableInfo("ClanStats", _columnsClanStats, _foreignKeysClanStats, _indicesClanStats);
        final TableInfo _existingClanStats = TableInfo.read(_db, "ClanStats");
        if (! _infoClanStats.equals(_existingClanStats)) {
          throw new IllegalStateException("Migration didn't properly handle ClanStats(eu.rtsketo.sakurastats.dbobjects.ClanStats).\n"
                  + " Expected:\n" + _infoClanStats + "\n"
                  + " Found:\n" + _existingClanStats);
        }
        final HashMap<String, TableInfo.Column> _columnsClanPlayer = new HashMap<String, TableInfo.Column>(9);
        _columnsClanPlayer.put("tag", new TableInfo.Column("tag", "TEXT", true, 1));
        _columnsClanPlayer.put("clan", new TableInfo.Column("clan", "TEXT", true, 0));
        _columnsClanPlayer.put("last", new TableInfo.Column("last", "INTEGER", true, 0));
        _columnsClanPlayer.put("score", new TableInfo.Column("score", "INTEGER", true, 0));
        _columnsClanPlayer.put("trophies", new TableInfo.Column("trophies", "INTEGER", true, 0));
        _columnsClanPlayer.put("smc", new TableInfo.Column("smc", "INTEGER", true, 0));
        _columnsClanPlayer.put("legendary", new TableInfo.Column("legendary", "INTEGER", true, 0));
        _columnsClanPlayer.put("magical", new TableInfo.Column("magical", "INTEGER", true, 0));
        _columnsClanPlayer.put("role", new TableInfo.Column("role", "TEXT", true, 0));
        final HashSet<TableInfo.ForeignKey> _foreignKeysClanPlayer = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesClanPlayer = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoClanPlayer = new TableInfo("ClanPlayer", _columnsClanPlayer, _foreignKeysClanPlayer, _indicesClanPlayer);
        final TableInfo _existingClanPlayer = TableInfo.read(_db, "ClanPlayer");
        if (! _infoClanPlayer.equals(_existingClanPlayer)) {
          throw new IllegalStateException("Migration didn't properly handle ClanPlayer(eu.rtsketo.sakurastats.dbobjects.ClanPlayer).\n"
                  + " Expected:\n" + _infoClanPlayer + "\n"
                  + " Found:\n" + _existingClanPlayer);
        }
      }
    }, "882a00bd1f0c24f8ff660b8e600f1550", "309a99c4d220e66c65c48d038c275488");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
        .name(configuration.name)
        .callback(_openCallback)
        .build();
    final SupportSQLiteOpenHelper _helper = configuration.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  protected InvalidationTracker createInvalidationTracker() {
    return new InvalidationTracker(this, "WarDay","PlayerStats","ClanStats","ClanPlayer");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `WarDay`");
      _db.execSQL("DELETE FROM `PlayerStats`");
      _db.execSQL("DELETE FROM `ClanStats`");
      _db.execSQL("DELETE FROM `ClanPlayer`");
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
  public DAObject getDao() {
    if (_dAObject != null) {
      return _dAObject;
    } else {
      synchronized(this) {
        if(_dAObject == null) {
          _dAObject = new DAObject_Impl(this);
        }
        return _dAObject;
      }
    }
  }
}
