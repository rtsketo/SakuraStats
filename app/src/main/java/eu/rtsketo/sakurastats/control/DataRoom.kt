package eu.rtsketo.sakurastats.control

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import eu.rtsketo.sakurastats.dbobjects.ClanPlayer
import eu.rtsketo.sakurastats.dbobjects.ClanStats
import eu.rtsketo.sakurastats.dbobjects.PlayerStats
import eu.rtsketo.sakurastats.dbobjects.WarDay

@Database(entities = [WarDay::class, PlayerStats::class, ClanStats::class, ClanPlayer::class], exportSchema = false, version = 2)
abstract class DataRoom : RoomDatabase() {
    abstract val dao: DAObject

    companion object {
        var instance: DataRoom? = null
            private set

        fun init(context: Context) {
            instance = Room.databaseBuilder(context.applicationContext,
                    DataRoom::class.java, "sakuradb")
                    .fallbackToDestructiveMigration()
                    .build()
        }
    }
}