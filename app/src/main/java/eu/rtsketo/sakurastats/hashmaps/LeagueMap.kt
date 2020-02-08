package eu.rtsketo.sakurastats.hashmaps

import android.util.Pair

object LeagueMap {
    private var leagueMap = mutableListOf<Pair<Int, Int>>()
    fun l2o(league: Int): Int {
        for (p in leagueMap)
            if (p.second == league) return p.first
        return 1
    }

    fun o2l(order: Int): Int {
        for (p in leagueMap)
            if (p.first == order) return p.second
        return 45
    }

    init {
        leagueMap.add(Pair(1, 45))
        leagueMap.add(Pair(2, 44))
        leagueMap.add(Pair(3, 43))
        leagueMap.add(Pair(4, 35))
        leagueMap.add(Pair(5, 34))
        leagueMap.add(Pair(6, 42))
        leagueMap.add(Pair(7, 33))
        leagueMap.add(Pair(8, 41))
        leagueMap.add(Pair(9, 25))
        leagueMap.add(Pair(10, 24))
        leagueMap.add(Pair(11, 32))
        leagueMap.add(Pair(12, 23))
        leagueMap.add(Pair(13, 31))
        leagueMap.add(Pair(14, 15))
        leagueMap.add(Pair(15, 14))
        leagueMap.add(Pair(16, 22))
        leagueMap.add(Pair(17, 13))
        leagueMap.add(Pair(18, 21))
        leagueMap.add(Pair(19, 12))
        leagueMap.add(Pair(20, 11))
    }
}