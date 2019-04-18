package eu.rtsketo.sakurastats.hashmaps;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class LeagueMap {
    private static List<Pair<Integer, Integer>> leagueMap;

    public static int l2o(int league) {
        initLeagueMap();
        for (Pair<Integer, Integer> p : leagueMap)
            if (p.second == league) return p.first;
        return 1; }

    public static int o2l(int order) {
        initLeagueMap();
        for (Pair<Integer, Integer> p : leagueMap)
            if (p.first == order) return p.second;
        return 45; }

    private static synchronized void initLeagueMap() {
        if (leagueMap == null) {
            leagueMap = new ArrayList<>();
            leagueMap.add(new Pair<>(1,45));
            leagueMap.add(new Pair<>(2,44));
            leagueMap.add(new Pair<>(3,43));
            leagueMap.add(new Pair<>(4,35));
            leagueMap.add(new Pair<>(5,34));
            leagueMap.add(new Pair<>(6,42));
            leagueMap.add(new Pair<>(7,33));
            leagueMap.add(new Pair<>(8,41));
            leagueMap.add(new Pair<>(9,25));
            leagueMap.add(new Pair<>(10,24));
            leagueMap.add(new Pair<>(11,32));
            leagueMap.add(new Pair<>(12,23));
            leagueMap.add(new Pair<>(13,31));
            leagueMap.add(new Pair<>(14,15));
            leagueMap.add(new Pair<>(15,14));
            leagueMap.add(new Pair<>(16,22));
            leagueMap.add(new Pair<>(17,13));
            leagueMap.add(new Pair<>(18,21));
            leagueMap.add(new Pair<>(19,12));
            leagueMap.add(new Pair<>(20,11));
        }
    }
}
