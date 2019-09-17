package eu.rtsketo.sakurastats.control;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import eu.rtsketo.sakurastats.dbobjects.ClanPlayer;
import eu.rtsketo.sakurastats.dbobjects.ClanStats;
import eu.rtsketo.sakurastats.dbobjects.PlayerStats;
import eu.rtsketo.sakurastats.dbobjects.WarDay;

@SuppressWarnings("SingletonPattern")
@Database(entities = {WarDay.class, PlayerStats.class, ClanStats.class,
        ClanPlayer.class}, exportSchema = false, version = 1)
public abstract class DataRoom extends RoomDatabase {
    public abstract DAObject getDao();
    private static DataRoom instance;

    public static void init(final Context context) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            DataRoom.class, "sakuradb").build();
    }

    public static DataRoom getInstance() { return instance; }
}


