package eu.rtsketo.sakurastats;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

@Database(entities = {WarDay.class, PlayerStats.class,
        ClanStats.class, ClanPlayer.class}, version = 1)
public abstract class DBControl extends RoomDatabase {

    public abstract GeneralDao getDao();
    private static DBControl db;

    public static DBControl initDB(final Context context) {
        if (db == null)
            synchronized (DBControl.class) {
                if (db == null)
                    db = Room.databaseBuilder(context.getApplicationContext(),
                            DBControl.class, "sakuradb").build();
            }
         return db;
    }

    public static DBControl getDB() { return db; }
}


