package eu.rtsketo.sakurastats.hashmaps;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Pair;

import java.util.HashMap;
import java.util.Map;

import eu.rtsketo.sakurastats.dbobjects.ClanPlayer;
import eu.rtsketo.sakurastats.dbobjects.PlayerStats;
import eu.rtsketo.sakurastats.fragments.PlayerActivity;
import eu.rtsketo.sakurastats.fragments.WarStatistics;
import eu.rtsketo.sakurastats.main.Interface;
import io.reactivex.subjects.ReplaySubject;

public class PlayerMap {
    private Map<String, Pair<ClanPlayer, PlayerStats>> playerMap;
    private final Object sync = new Object();
    private ReplaySubject<ClanPlayer> cpSub;
    private ReplaySubject<PlayerStats> psSub;
    private static PlayerMap instance;
    private final int DELAY = 20;
    private Interface acti;
    private long time;
    private int size;

    private PlayerMap(Interface activity) { acti = activity; }
    public static PlayerMap getInstance() { return instance; }
    public static void init(Interface activity) {
        instance = new PlayerMap(activity);
    }

    public int size() { return size; }
    public void put(String tag, Object player) {
        Pair<ClanPlayer, PlayerStats> pair = playerMap.get(tag);
        Handler handler = new Handler(Looper.getMainLooper());

        if (pair == null)
            pair = new Pair<>(new ClanPlayer(), new PlayerStats());

        synchronized (sync) {
            long now = SystemClock.uptimeMillis();
            if (time == 0 || now > time + DELAY)
                time = now;
            time += DELAY;
        }

        if (player instanceof ClanPlayer) {
            pair = new Pair<>((ClanPlayer) player, pair.second);
            handler.postAtTime(()->
                    cpSub.onNext((ClanPlayer) player), time);
        } else {
            pair = new Pair<>(pair.first, (PlayerStats) player);
            handler.postAtTime(()->
                    psSub.onNext((PlayerStats) player), time);
        }

        playerMap.put(tag, pair);
    }

    public void reset(int size) {
        synchronized (PlayerMap.class) {
            if (playerMap != null) playerMap.clear();
            else playerMap = new HashMap<>(size);
            this.size = size;
        }

        WarStatistics wFrag = acti.getWarFrag();
        PlayerActivity aFrag = acti.getActiFrag();

        psSub = ReplaySubject.create();
        cpSub = ReplaySubject.create();
        cpSub.subscribe(wFrag.cpObserver());
        psSub.subscribe(wFrag.psObserver());
        cpSub.subscribe(aFrag.cpObserver());
        psSub.subscribe(aFrag.psObserver());
    }

    public void completeSubs() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postAtTime(()-> {
            cpSub.onComplete();
            psSub.onComplete();
        }, time);
    }

    public Map<String, Pair<ClanPlayer, PlayerStats>> getAll() {
        return playerMap; }

    public Pair<ClanPlayer, PlayerStats> get(String tag) {
        return playerMap.get(tag); }
}
