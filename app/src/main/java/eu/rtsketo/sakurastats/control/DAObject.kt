package eu.rtsketo.sakurastats.control

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import eu.rtsketo.sakurastats.dbobjects.ClanPlayer
import eu.rtsketo.sakurastats.dbobjects.ClanStats
import eu.rtsketo.sakurastats.dbobjects.PlayerStats
import eu.rtsketo.sakurastats.dbobjects.WarDay

@Dao
interface DAObject {
    @Query("SELECT COUNT(warDay) FROM WarDay WHERE tag = :tag")
    fun countWarDays(tag: String): Int

    @get:Query("SELECT * FROM WarDay")
    val warDays: List<WarDay>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addWarDay(warday: WarDay)

    @Query("SELECT * FROM PlayerStats WHERE clan = :clan AND current = 1")
    fun getClanPlayerStats(clan: String): List<PlayerStats>

    @Query("SELECT * FROM PlayerStats WHERE tag = :tag")
    fun getPlayerStats(tag: String): PlayerStats

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlayerStats(ps: PlayerStats)

    @Query("SELECT * FROM ClanStats WHERE tag = :tag OR Clan1 = " +
            ":tag OR Clan2 = :tag OR Clan3 = :tag OR Clan4 = :tag")
    fun getClanStatsList(tag: String): List<ClanStats>

    @Query("SELECT * FROM ClanStats WHERE tag = :tag")
    fun getClanStats(tag: String): ClanStats

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertClanStats(clan: ClanStats)

    @Query("UPDATE PlayerStats SET current = 0 WHERE clan = :tag")
    fun resetCurrentPlayers(tag: String)

    @Query("DELETE FROM ClanStats WHERE tag = :tag OR Clan1 = " +
            ":tag OR Clan2 = :tag OR Clan3 = :tag OR Clan4 = :tag")
    fun resetClanStats(tag: String)

    @Query("SELECT * FROM ClanPlayer WHERE tag = :tag")
    fun getClanPlayer(tag: String): ClanPlayer

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertClanPlayer(player: ClanPlayer)
}