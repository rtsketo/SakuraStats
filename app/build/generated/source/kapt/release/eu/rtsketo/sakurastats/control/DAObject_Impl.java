package eu.rtsketo.sakurastats.control;

import android.database.Cursor;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.sqlite.db.SupportSQLiteStatement;
import eu.rtsketo.sakurastats.dbobjects.ClanPlayer;
import eu.rtsketo.sakurastats.dbobjects.ClanStats;
import eu.rtsketo.sakurastats.dbobjects.PlayerStats;
import eu.rtsketo.sakurastats.dbobjects.WarDay;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public final class DAObject_Impl implements DAObject {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter __insertionAdapterOfWarDay;

  private final EntityInsertionAdapter __insertionAdapterOfPlayerStats;

  private final EntityInsertionAdapter __insertionAdapterOfClanStats;

  private final EntityInsertionAdapter __insertionAdapterOfClanPlayer;

  private final SharedSQLiteStatement __preparedStmtOfResetCurrentPlayers;

  private final SharedSQLiteStatement __preparedStmtOfResetClanStats;

  public DAObject_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWarDay = new EntityInsertionAdapter<WarDay>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR IGNORE INTO `WarDay`(`warDay`,`tag`) VALUES (?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, WarDay value) {
        stmt.bindLong(1, value.getWarDay());
        if (value.getTag() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getTag());
        }
      }
    };
    this.__insertionAdapterOfPlayerStats = new EntityInsertionAdapter<PlayerStats>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR REPLACE INTO `PlayerStats`(`tag`,`name`,`clan`,`wins`,`played`,`curWins`,`curPlay`,`ratio`,`norma`,`current`,`missed`,`cards`,`wars`,`chest`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, PlayerStats value) {
        if (value.getTag() == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.getTag());
        }
        if (value.getName() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getName());
        }
        if (value.getClan() == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.getClan());
        }
        stmt.bindLong(4, value.getWins());
        stmt.bindLong(5, value.getPlayed());
        stmt.bindLong(6, value.getCurWins());
        stmt.bindLong(7, value.getCurPlay());
        stmt.bindDouble(8, value.getRatio());
        stmt.bindDouble(9, value.getNorma());
        final int _tmp;
        _tmp = value.getCurrent() ? 1 : 0;
        stmt.bindLong(10, _tmp);
        stmt.bindLong(11, value.getMissed());
        stmt.bindLong(12, value.getCards());
        stmt.bindLong(13, value.getWars());
        stmt.bindLong(14, value.getChest());
      }
    };
    this.__insertionAdapterOfClanStats = new EntityInsertionAdapter<ClanStats>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR REPLACE INTO `ClanStats`(`tag`,`name`,`state`,`badge`,`maxParticipants`,`estimatedWins`,`extraWins`,`warTrophies`,`actualWins`,`remaining`,`crowns`,`clan1`,`clan2`,`clan3`,`clan4`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, ClanStats value) {
        if (value.getTag() == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.getTag());
        }
        if (value.getName() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getName());
        }
        if (value.getState() == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.getState());
        }
        if (value.getBadge() == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.getBadge());
        }
        stmt.bindLong(5, value.getMaxParticipants());
        stmt.bindLong(6, value.getEstimatedWins());
        stmt.bindDouble(7, value.getExtraWins());
        stmt.bindLong(8, value.getWarTrophies());
        stmt.bindLong(9, value.getActualWins());
        stmt.bindLong(10, value.getRemaining());
        stmt.bindLong(11, value.getCrowns());
        if (value.getClan1() == null) {
          stmt.bindNull(12);
        } else {
          stmt.bindString(12, value.getClan1());
        }
        if (value.getClan2() == null) {
          stmt.bindNull(13);
        } else {
          stmt.bindString(13, value.getClan2());
        }
        if (value.getClan3() == null) {
          stmt.bindNull(14);
        } else {
          stmt.bindString(14, value.getClan3());
        }
        if (value.getClan4() == null) {
          stmt.bindNull(15);
        } else {
          stmt.bindString(15, value.getClan4());
        }
      }
    };
    this.__insertionAdapterOfClanPlayer = new EntityInsertionAdapter<ClanPlayer>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR REPLACE INTO `ClanPlayer`(`tag`,`clan`,`last`,`score`,`trophies`,`smc`,`legendary`,`magical`,`role`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, ClanPlayer value) {
        if (value.getTag() == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.getTag());
        }
        if (value.getClan() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getClan());
        }
        stmt.bindLong(3, value.getLast());
        stmt.bindLong(4, value.getScore());
        stmt.bindLong(5, value.getTrophies());
        stmt.bindLong(6, value.getSmc());
        stmt.bindLong(7, value.getLegendary());
        stmt.bindLong(8, value.getMagical());
        if (value.getRole() == null) {
          stmt.bindNull(9);
        } else {
          stmt.bindString(9, value.getRole());
        }
      }
    };
    this.__preparedStmtOfResetCurrentPlayers = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "UPDATE PlayerStats SET current = 0 WHERE clan = ?";
        return _query;
      }
    };
    this.__preparedStmtOfResetClanStats = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM ClanStats WHERE tag = ? OR Clan1 = ? OR Clan2 = ? OR Clan3 = ? OR Clan4 = ?";
        return _query;
      }
    };
  }

  @Override
  public void addWarDay(WarDay warday) {
    __db.beginTransaction();
    try {
      __insertionAdapterOfWarDay.insert(warday);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertPlayerStats(PlayerStats ps) {
    __db.beginTransaction();
    try {
      __insertionAdapterOfPlayerStats.insert(ps);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertClanStats(ClanStats clan) {
    __db.beginTransaction();
    try {
      __insertionAdapterOfClanStats.insert(clan);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertClanPlayer(ClanPlayer player) {
    __db.beginTransaction();
    try {
      __insertionAdapterOfClanPlayer.insert(player);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void resetCurrentPlayers(String tag) {
    final SupportSQLiteStatement _stmt = __preparedStmtOfResetCurrentPlayers.acquire();
    __db.beginTransaction();
    try {
      int _argIndex = 1;
      if (tag == null) {
        _stmt.bindNull(_argIndex);
      } else {
        _stmt.bindString(_argIndex, tag);
      }
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfResetCurrentPlayers.release(_stmt);
    }
  }

  @Override
  public void resetClanStats(String tag) {
    final SupportSQLiteStatement _stmt = __preparedStmtOfResetClanStats.acquire();
    __db.beginTransaction();
    try {
      int _argIndex = 1;
      if (tag == null) {
        _stmt.bindNull(_argIndex);
      } else {
        _stmt.bindString(_argIndex, tag);
      }
      _argIndex = 2;
      if (tag == null) {
        _stmt.bindNull(_argIndex);
      } else {
        _stmt.bindString(_argIndex, tag);
      }
      _argIndex = 3;
      if (tag == null) {
        _stmt.bindNull(_argIndex);
      } else {
        _stmt.bindString(_argIndex, tag);
      }
      _argIndex = 4;
      if (tag == null) {
        _stmt.bindNull(_argIndex);
      } else {
        _stmt.bindString(_argIndex, tag);
      }
      _argIndex = 5;
      if (tag == null) {
        _stmt.bindNull(_argIndex);
      } else {
        _stmt.bindString(_argIndex, tag);
      }
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfResetClanStats.release(_stmt);
    }
  }

  @Override
  public int countWarDays(String tag) {
    final String _sql = "SELECT COUNT(warDay) FROM WarDay WHERE tag = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (tag == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, tag);
    }
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _result;
      if(_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<WarDay> getWarDays() {
    final String _sql = "SELECT * FROM WarDay";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfWarDay = _cursor.getColumnIndexOrThrow("warDay");
      final int _cursorIndexOfTag = _cursor.getColumnIndexOrThrow("tag");
      final List<WarDay> _result = new ArrayList<WarDay>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final WarDay _item;
        final long _tmpWarDay;
        _tmpWarDay = _cursor.getLong(_cursorIndexOfWarDay);
        final String _tmpTag;
        _tmpTag = _cursor.getString(_cursorIndexOfTag);
        _item = new WarDay(_tmpWarDay,_tmpTag);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<PlayerStats> getClanPlayerStats(String clan) {
    final String _sql = "SELECT * FROM PlayerStats WHERE clan = ? AND current = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (clan == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, clan);
    }
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfTag = _cursor.getColumnIndexOrThrow("tag");
      final int _cursorIndexOfName = _cursor.getColumnIndexOrThrow("name");
      final int _cursorIndexOfClan = _cursor.getColumnIndexOrThrow("clan");
      final int _cursorIndexOfWins = _cursor.getColumnIndexOrThrow("wins");
      final int _cursorIndexOfPlayed = _cursor.getColumnIndexOrThrow("played");
      final int _cursorIndexOfCurWins = _cursor.getColumnIndexOrThrow("curWins");
      final int _cursorIndexOfCurPlay = _cursor.getColumnIndexOrThrow("curPlay");
      final int _cursorIndexOfRatio = _cursor.getColumnIndexOrThrow("ratio");
      final int _cursorIndexOfNorma = _cursor.getColumnIndexOrThrow("norma");
      final int _cursorIndexOfCurrent = _cursor.getColumnIndexOrThrow("current");
      final int _cursorIndexOfMissed = _cursor.getColumnIndexOrThrow("missed");
      final int _cursorIndexOfCards = _cursor.getColumnIndexOrThrow("cards");
      final int _cursorIndexOfWars = _cursor.getColumnIndexOrThrow("wars");
      final int _cursorIndexOfChest = _cursor.getColumnIndexOrThrow("chest");
      final List<PlayerStats> _result = new ArrayList<PlayerStats>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final PlayerStats _item;
        _item = new PlayerStats();
        final String _tmpTag;
        _tmpTag = _cursor.getString(_cursorIndexOfTag);
        _item.setTag(_tmpTag);
        final String _tmpName;
        _tmpName = _cursor.getString(_cursorIndexOfName);
        _item.setName(_tmpName);
        final String _tmpClan;
        _tmpClan = _cursor.getString(_cursorIndexOfClan);
        _item.setClan(_tmpClan);
        final int _tmpWins;
        _tmpWins = _cursor.getInt(_cursorIndexOfWins);
        _item.setWins(_tmpWins);
        final int _tmpPlayed;
        _tmpPlayed = _cursor.getInt(_cursorIndexOfPlayed);
        _item.setPlayed(_tmpPlayed);
        final int _tmpCurWins;
        _tmpCurWins = _cursor.getInt(_cursorIndexOfCurWins);
        _item.setCurWins(_tmpCurWins);
        final int _tmpCurPlay;
        _tmpCurPlay = _cursor.getInt(_cursorIndexOfCurPlay);
        _item.setCurPlay(_tmpCurPlay);
        final double _tmpRatio;
        _tmpRatio = _cursor.getDouble(_cursorIndexOfRatio);
        _item.setRatio(_tmpRatio);
        final double _tmpNorma;
        _tmpNorma = _cursor.getDouble(_cursorIndexOfNorma);
        _item.setNorma(_tmpNorma);
        final boolean _tmpCurrent;
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfCurrent);
        _tmpCurrent = _tmp != 0;
        _item.setCurrent(_tmpCurrent);
        final int _tmpMissed;
        _tmpMissed = _cursor.getInt(_cursorIndexOfMissed);
        _item.setMissed(_tmpMissed);
        final int _tmpCards;
        _tmpCards = _cursor.getInt(_cursorIndexOfCards);
        _item.setCards(_tmpCards);
        final int _tmpWars;
        _tmpWars = _cursor.getInt(_cursorIndexOfWars);
        _item.setWars(_tmpWars);
        final int _tmpChest;
        _tmpChest = _cursor.getInt(_cursorIndexOfChest);
        _item.setChest(_tmpChest);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public PlayerStats getPlayerStats(String tag) {
    final String _sql = "SELECT * FROM PlayerStats WHERE tag = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (tag == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, tag);
    }
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfTag = _cursor.getColumnIndexOrThrow("tag");
      final int _cursorIndexOfName = _cursor.getColumnIndexOrThrow("name");
      final int _cursorIndexOfClan = _cursor.getColumnIndexOrThrow("clan");
      final int _cursorIndexOfWins = _cursor.getColumnIndexOrThrow("wins");
      final int _cursorIndexOfPlayed = _cursor.getColumnIndexOrThrow("played");
      final int _cursorIndexOfCurWins = _cursor.getColumnIndexOrThrow("curWins");
      final int _cursorIndexOfCurPlay = _cursor.getColumnIndexOrThrow("curPlay");
      final int _cursorIndexOfRatio = _cursor.getColumnIndexOrThrow("ratio");
      final int _cursorIndexOfNorma = _cursor.getColumnIndexOrThrow("norma");
      final int _cursorIndexOfCurrent = _cursor.getColumnIndexOrThrow("current");
      final int _cursorIndexOfMissed = _cursor.getColumnIndexOrThrow("missed");
      final int _cursorIndexOfCards = _cursor.getColumnIndexOrThrow("cards");
      final int _cursorIndexOfWars = _cursor.getColumnIndexOrThrow("wars");
      final int _cursorIndexOfChest = _cursor.getColumnIndexOrThrow("chest");
      final PlayerStats _result;
      if(_cursor.moveToFirst()) {
        _result = new PlayerStats();
        final String _tmpTag;
        _tmpTag = _cursor.getString(_cursorIndexOfTag);
        _result.setTag(_tmpTag);
        final String _tmpName;
        _tmpName = _cursor.getString(_cursorIndexOfName);
        _result.setName(_tmpName);
        final String _tmpClan;
        _tmpClan = _cursor.getString(_cursorIndexOfClan);
        _result.setClan(_tmpClan);
        final int _tmpWins;
        _tmpWins = _cursor.getInt(_cursorIndexOfWins);
        _result.setWins(_tmpWins);
        final int _tmpPlayed;
        _tmpPlayed = _cursor.getInt(_cursorIndexOfPlayed);
        _result.setPlayed(_tmpPlayed);
        final int _tmpCurWins;
        _tmpCurWins = _cursor.getInt(_cursorIndexOfCurWins);
        _result.setCurWins(_tmpCurWins);
        final int _tmpCurPlay;
        _tmpCurPlay = _cursor.getInt(_cursorIndexOfCurPlay);
        _result.setCurPlay(_tmpCurPlay);
        final double _tmpRatio;
        _tmpRatio = _cursor.getDouble(_cursorIndexOfRatio);
        _result.setRatio(_tmpRatio);
        final double _tmpNorma;
        _tmpNorma = _cursor.getDouble(_cursorIndexOfNorma);
        _result.setNorma(_tmpNorma);
        final boolean _tmpCurrent;
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfCurrent);
        _tmpCurrent = _tmp != 0;
        _result.setCurrent(_tmpCurrent);
        final int _tmpMissed;
        _tmpMissed = _cursor.getInt(_cursorIndexOfMissed);
        _result.setMissed(_tmpMissed);
        final int _tmpCards;
        _tmpCards = _cursor.getInt(_cursorIndexOfCards);
        _result.setCards(_tmpCards);
        final int _tmpWars;
        _tmpWars = _cursor.getInt(_cursorIndexOfWars);
        _result.setWars(_tmpWars);
        final int _tmpChest;
        _tmpChest = _cursor.getInt(_cursorIndexOfChest);
        _result.setChest(_tmpChest);
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
  public List<ClanStats> getClanStatsList(String tag) {
    final String _sql = "SELECT * FROM ClanStats WHERE tag = ? OR Clan1 = ? OR Clan2 = ? OR Clan3 = ? OR Clan4 = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 5);
    int _argIndex = 1;
    if (tag == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, tag);
    }
    _argIndex = 2;
    if (tag == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, tag);
    }
    _argIndex = 3;
    if (tag == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, tag);
    }
    _argIndex = 4;
    if (tag == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, tag);
    }
    _argIndex = 5;
    if (tag == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, tag);
    }
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfTag = _cursor.getColumnIndexOrThrow("tag");
      final int _cursorIndexOfName = _cursor.getColumnIndexOrThrow("name");
      final int _cursorIndexOfState = _cursor.getColumnIndexOrThrow("state");
      final int _cursorIndexOfBadge = _cursor.getColumnIndexOrThrow("badge");
      final int _cursorIndexOfMaxParticipants = _cursor.getColumnIndexOrThrow("maxParticipants");
      final int _cursorIndexOfEstimatedWins = _cursor.getColumnIndexOrThrow("estimatedWins");
      final int _cursorIndexOfExtraWins = _cursor.getColumnIndexOrThrow("extraWins");
      final int _cursorIndexOfWarTrophies = _cursor.getColumnIndexOrThrow("warTrophies");
      final int _cursorIndexOfActualWins = _cursor.getColumnIndexOrThrow("actualWins");
      final int _cursorIndexOfRemaining = _cursor.getColumnIndexOrThrow("remaining");
      final int _cursorIndexOfCrowns = _cursor.getColumnIndexOrThrow("crowns");
      final int _cursorIndexOfClan1 = _cursor.getColumnIndexOrThrow("clan1");
      final int _cursorIndexOfClan2 = _cursor.getColumnIndexOrThrow("clan2");
      final int _cursorIndexOfClan3 = _cursor.getColumnIndexOrThrow("clan3");
      final int _cursorIndexOfClan4 = _cursor.getColumnIndexOrThrow("clan4");
      final List<ClanStats> _result = new ArrayList<ClanStats>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final ClanStats _item;
        _item = new ClanStats();
        final String _tmpTag;
        _tmpTag = _cursor.getString(_cursorIndexOfTag);
        _item.setTag(_tmpTag);
        final String _tmpName;
        _tmpName = _cursor.getString(_cursorIndexOfName);
        _item.setName(_tmpName);
        final String _tmpState;
        _tmpState = _cursor.getString(_cursorIndexOfState);
        _item.setState(_tmpState);
        final String _tmpBadge;
        _tmpBadge = _cursor.getString(_cursorIndexOfBadge);
        _item.setBadge(_tmpBadge);
        final int _tmpMaxParticipants;
        _tmpMaxParticipants = _cursor.getInt(_cursorIndexOfMaxParticipants);
        _item.setMaxParticipants(_tmpMaxParticipants);
        final int _tmpEstimatedWins;
        _tmpEstimatedWins = _cursor.getInt(_cursorIndexOfEstimatedWins);
        _item.setEstimatedWins(_tmpEstimatedWins);
        final double _tmpExtraWins;
        _tmpExtraWins = _cursor.getDouble(_cursorIndexOfExtraWins);
        _item.setExtraWins(_tmpExtraWins);
        final int _tmpWarTrophies;
        _tmpWarTrophies = _cursor.getInt(_cursorIndexOfWarTrophies);
        _item.setWarTrophies(_tmpWarTrophies);
        final int _tmpActualWins;
        _tmpActualWins = _cursor.getInt(_cursorIndexOfActualWins);
        _item.setActualWins(_tmpActualWins);
        final int _tmpRemaining;
        _tmpRemaining = _cursor.getInt(_cursorIndexOfRemaining);
        _item.setRemaining(_tmpRemaining);
        final int _tmpCrowns;
        _tmpCrowns = _cursor.getInt(_cursorIndexOfCrowns);
        _item.setCrowns(_tmpCrowns);
        final String _tmpClan1;
        _tmpClan1 = _cursor.getString(_cursorIndexOfClan1);
        _item.setClan1(_tmpClan1);
        final String _tmpClan2;
        _tmpClan2 = _cursor.getString(_cursorIndexOfClan2);
        _item.setClan2(_tmpClan2);
        final String _tmpClan3;
        _tmpClan3 = _cursor.getString(_cursorIndexOfClan3);
        _item.setClan3(_tmpClan3);
        final String _tmpClan4;
        _tmpClan4 = _cursor.getString(_cursorIndexOfClan4);
        _item.setClan4(_tmpClan4);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public ClanStats getClanStats(String tag) {
    final String _sql = "SELECT * FROM ClanStats WHERE tag = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (tag == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, tag);
    }
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfTag = _cursor.getColumnIndexOrThrow("tag");
      final int _cursorIndexOfName = _cursor.getColumnIndexOrThrow("name");
      final int _cursorIndexOfState = _cursor.getColumnIndexOrThrow("state");
      final int _cursorIndexOfBadge = _cursor.getColumnIndexOrThrow("badge");
      final int _cursorIndexOfMaxParticipants = _cursor.getColumnIndexOrThrow("maxParticipants");
      final int _cursorIndexOfEstimatedWins = _cursor.getColumnIndexOrThrow("estimatedWins");
      final int _cursorIndexOfExtraWins = _cursor.getColumnIndexOrThrow("extraWins");
      final int _cursorIndexOfWarTrophies = _cursor.getColumnIndexOrThrow("warTrophies");
      final int _cursorIndexOfActualWins = _cursor.getColumnIndexOrThrow("actualWins");
      final int _cursorIndexOfRemaining = _cursor.getColumnIndexOrThrow("remaining");
      final int _cursorIndexOfCrowns = _cursor.getColumnIndexOrThrow("crowns");
      final int _cursorIndexOfClan1 = _cursor.getColumnIndexOrThrow("clan1");
      final int _cursorIndexOfClan2 = _cursor.getColumnIndexOrThrow("clan2");
      final int _cursorIndexOfClan3 = _cursor.getColumnIndexOrThrow("clan3");
      final int _cursorIndexOfClan4 = _cursor.getColumnIndexOrThrow("clan4");
      final ClanStats _result;
      if(_cursor.moveToFirst()) {
        _result = new ClanStats();
        final String _tmpTag;
        _tmpTag = _cursor.getString(_cursorIndexOfTag);
        _result.setTag(_tmpTag);
        final String _tmpName;
        _tmpName = _cursor.getString(_cursorIndexOfName);
        _result.setName(_tmpName);
        final String _tmpState;
        _tmpState = _cursor.getString(_cursorIndexOfState);
        _result.setState(_tmpState);
        final String _tmpBadge;
        _tmpBadge = _cursor.getString(_cursorIndexOfBadge);
        _result.setBadge(_tmpBadge);
        final int _tmpMaxParticipants;
        _tmpMaxParticipants = _cursor.getInt(_cursorIndexOfMaxParticipants);
        _result.setMaxParticipants(_tmpMaxParticipants);
        final int _tmpEstimatedWins;
        _tmpEstimatedWins = _cursor.getInt(_cursorIndexOfEstimatedWins);
        _result.setEstimatedWins(_tmpEstimatedWins);
        final double _tmpExtraWins;
        _tmpExtraWins = _cursor.getDouble(_cursorIndexOfExtraWins);
        _result.setExtraWins(_tmpExtraWins);
        final int _tmpWarTrophies;
        _tmpWarTrophies = _cursor.getInt(_cursorIndexOfWarTrophies);
        _result.setWarTrophies(_tmpWarTrophies);
        final int _tmpActualWins;
        _tmpActualWins = _cursor.getInt(_cursorIndexOfActualWins);
        _result.setActualWins(_tmpActualWins);
        final int _tmpRemaining;
        _tmpRemaining = _cursor.getInt(_cursorIndexOfRemaining);
        _result.setRemaining(_tmpRemaining);
        final int _tmpCrowns;
        _tmpCrowns = _cursor.getInt(_cursorIndexOfCrowns);
        _result.setCrowns(_tmpCrowns);
        final String _tmpClan1;
        _tmpClan1 = _cursor.getString(_cursorIndexOfClan1);
        _result.setClan1(_tmpClan1);
        final String _tmpClan2;
        _tmpClan2 = _cursor.getString(_cursorIndexOfClan2);
        _result.setClan2(_tmpClan2);
        final String _tmpClan3;
        _tmpClan3 = _cursor.getString(_cursorIndexOfClan3);
        _result.setClan3(_tmpClan3);
        final String _tmpClan4;
        _tmpClan4 = _cursor.getString(_cursorIndexOfClan4);
        _result.setClan4(_tmpClan4);
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
  public ClanPlayer getClanPlayer(String tag) {
    final String _sql = "SELECT * FROM ClanPlayer WHERE tag = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (tag == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, tag);
    }
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfTag = _cursor.getColumnIndexOrThrow("tag");
      final int _cursorIndexOfClan = _cursor.getColumnIndexOrThrow("clan");
      final int _cursorIndexOfLast = _cursor.getColumnIndexOrThrow("last");
      final int _cursorIndexOfScore = _cursor.getColumnIndexOrThrow("score");
      final int _cursorIndexOfTrophies = _cursor.getColumnIndexOrThrow("trophies");
      final int _cursorIndexOfSmc = _cursor.getColumnIndexOrThrow("smc");
      final int _cursorIndexOfLegendary = _cursor.getColumnIndexOrThrow("legendary");
      final int _cursorIndexOfMagical = _cursor.getColumnIndexOrThrow("magical");
      final int _cursorIndexOfRole = _cursor.getColumnIndexOrThrow("role");
      final ClanPlayer _result;
      if(_cursor.moveToFirst()) {
        _result = new ClanPlayer();
        final String _tmpTag;
        _tmpTag = _cursor.getString(_cursorIndexOfTag);
        _result.setTag(_tmpTag);
        final String _tmpClan;
        _tmpClan = _cursor.getString(_cursorIndexOfClan);
        _result.setClan(_tmpClan);
        final long _tmpLast;
        _tmpLast = _cursor.getLong(_cursorIndexOfLast);
        _result.setLast(_tmpLast);
        final int _tmpScore;
        _tmpScore = _cursor.getInt(_cursorIndexOfScore);
        _result.setScore(_tmpScore);
        final int _tmpTrophies;
        _tmpTrophies = _cursor.getInt(_cursorIndexOfTrophies);
        _result.setTrophies(_tmpTrophies);
        final int _tmpSmc;
        _tmpSmc = _cursor.getInt(_cursorIndexOfSmc);
        _result.setSmc(_tmpSmc);
        final int _tmpLegendary;
        _tmpLegendary = _cursor.getInt(_cursorIndexOfLegendary);
        _result.setLegendary(_tmpLegendary);
        final int _tmpMagical;
        _tmpMagical = _cursor.getInt(_cursorIndexOfMagical);
        _result.setMagical(_tmpMagical);
        final String _tmpRole;
        _tmpRole = _cursor.getString(_cursorIndexOfRole);
        _result.setRole(_tmpRole);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }
}
