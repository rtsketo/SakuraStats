package eu.rtsketo.sakurastats;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
interface GeneralDao {
    @Query("SELECT COUNT(warDay) FROM WarDay WHERE tag = :tag")
    int countWarDays(String tag);

    @Query("SELECT * FROM WarDay")
    List<WarDay> getWarDays();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addWarDay(WarDay warday);

    @Query("SELECT * FROM PlayerStats WHERE clan = :clan AND current = 1")
    List<PlayerStats> getClanPlayerStats(String clan);

    @Query("SELECT * FROM PlayerStats WHERE tag = :tag")
    PlayerStats getPlayerStats(String tag);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPlayerStats(PlayerStats ps);

    @Query("SELECT * FROM ClanStats WHERE tag = :tag OR Clan1 = " +
            ":tag OR Clan2 = :tag OR Clan3 = :tag OR Clan4 = :tag")
    List<ClanStats> getClanStatsList(String tag);

    @Query("SELECT * FROM ClanStats WHERE tag = :tag")
    ClanStats getClanStats(String tag);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertClanStats(ClanStats clan);

    @Query("UPDATE PlayerStats SET current = 0 WHERE clan = :tag")
    void resetCurrentPlayers(String tag);

    @Query("DELETE FROM ClanStats WHERE tag = :tag OR Clan1 = " +
            ":tag OR Clan2 = :tag OR Clan3 = :tag OR Clan4 = :tag")
    void resetClanStats(String tag);

    @Query("SELECT * FROM ClanPlayer WHERE tag = :tag")
    ClanPlayer getClanPlayer(String tag);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertClanPlayer(ClanPlayer player);
}