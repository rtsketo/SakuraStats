package eu.rtsketo.sakurastats.hashmaps;

import android.os.SystemClock;
import android.util.Pair;

import java.util.HashMap;
import java.util.Map;

import eu.rtsketo.sakurastats.dbobjects.ClanPlayer;
import eu.rtsketo.sakurastats.dbobjects.PlayerStats;
import eu.rtsketo.sakurastats.main.Interface;
import io.reactivex.subjects.PublishSubject;

public class PlayerMap {
    private Map<String, Pair<ClanPlayer, PlayerStats>> playerMap;
    private PublishSubject<ClanPlayer> cpSub;
    private PublishSubject<PlayerStats> psSub;
    private static PlayerMap instance;
    private Interface acti;
    private int size;

    private PlayerMap(Interface activity) { acti = activity; }
    public static PlayerMap getInstance() { return instance; }
    public static void init(Interface activity) {
        instance = new PlayerMap(activity);
    }

    public int size() { return size; }
    public void put(String tag, Object player) {
        if (playerMap == null) reset(0);
        Pair<ClanPlayer, PlayerStats> pair = playerMap.get(tag);

        if (pair == null)
            pair = new Pair<>(new ClanPlayer(), new PlayerStats());

        if (player instanceof ClanPlayer) {
            pair = new Pair<>((ClanPlayer) player, pair.second);
            cpSub.onNext((ClanPlayer) player);
        } else {
            pair = new Pair<>(pair.first, (PlayerStats) player);
            psSub.onNext((PlayerStats) player);
        }

        playerMap.put(tag, pair);
    }

    public void reset(int size) {
        synchronized (PlayerMap.class) {
            if (playerMap != null) playerMap.clear();
            else playerMap = new HashMap<>(size);
            this.size = size;
        }

        while (acti.getWarFrag() == null ||
                acti.getActiFrag() == null )
            SystemClock.sleep(50);

        psSub = PublishSubject.create();
        cpSub = PublishSubject.create();
        cpSub.subscribe(acti.getWarFrag().cpObserver());
        psSub.subscribe(acti.getWarFrag().psObserver());
        cpSub.subscribe(acti.getActiFrag().cpObserver());
        psSub.subscribe(acti.getActiFrag().psObserver());
    }

    public void completeSubs() {
        cpSub.onComplete();
        psSub.onComplete();
    }

    public Map<String, Pair<ClanPlayer, PlayerStats>> getAll() {
        return playerMap; }

    public Pair<ClanPlayer, PlayerStats> get(String tag) {
        return playerMap.get(tag); }
}
