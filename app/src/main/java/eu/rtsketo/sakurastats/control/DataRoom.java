package eu.rtsketo.sakurastats.control;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import eu.rtsketo.sakurastats.dbobjects.ClanPlayer;
import eu.rtsketo.sakurastats.dbobjects.ClanStats;
import eu.rtsketo.sakurastats.dbobjects.PlayerStats;
import eu.rtsketo.sakurastats.dbobjects.WarDay;

@SuppressWarnings("SingletonPattern")
@Database(entities = {WarDay.class, PlayerStats.class,
        ClanStats.class, ClanPlayer.class}, version = 1)
public abstract class DataRoom extends RoomDatabase {
    public abstract DAObject getDao();
    private static DataRoom instance;

    public static void init(final Context context) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            DataRoom.class, "sakuradb").build();
    }

    public static DataRoom getInstance() { return instance; }
}


