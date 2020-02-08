package eu.rtsketo.sakurastats.hashmaps

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Pair
import eu.rtsketo.sakurastats.dbobjects.ClanPlayer
import eu.rtsketo.sakurastats.dbobjects.PlayerStats
import eu.rtsketo.sakurastats.main.Interface
import io.reactivex.subjects.ReplaySubject

class PlayerMap private constructor(private val acti: Interface) {
    private var playerMap = mutableMapOf<String, Pair<ClanPlayer, PlayerStats>>()
    private var psSub: ReplaySubject<PlayerStats> = ReplaySubject.create()
    private var cpSub: ReplaySubject<ClanPlayer> = ReplaySubject.create()
    private val sync = Any()
    private val delay = 20
    private var time = 0L
    private var size = 0
    fun size(): Int {
        return size
    }

    fun put(tag: String, player: Any) {
        var pair = playerMap[tag]
        val handler = Handler(Looper.getMainLooper())
        if (pair == null) pair = Pair(ClanPlayer(), PlayerStats())

        synchronized(sync) {
            val now = SystemClock.uptimeMillis()
            if (time == 0L || now > time + delay) time = now
            time += delay.toLong()
        }

        if (player is ClanPlayer) {
            pair = Pair(player, pair.second)
            handler.postAtTime({ cpSub.onNext(player) }, time)
        } else {
            pair = Pair(pair.first, player as PlayerStats)
            handler.postAtTime({ psSub.onNext(player) }, time)
        }
        playerMap[tag] = pair
    }

    fun reset(size: Int) {
        synchronized(PlayerMap::class.java) {
            playerMap.clear()
            this.size = size
        }

        val wFrag = acti.getWarFrag()
        val aFrag = acti.getActiFrag()
        psSub = ReplaySubject.create()
        cpSub = ReplaySubject.create()
        cpSub.subscribe(wFrag.cpObserver())
        psSub.subscribe(wFrag.psObserver())
        cpSub.subscribe(aFrag.cpObserver())
        psSub.subscribe(aFrag.psObserver())
    }

    fun completeSubs() {
        val handler = Handler(Looper.getMainLooper())
        handler.postAtTime({
            cpSub.onComplete()
            psSub.onComplete()
        }, time)
    }

    val all: Map<String, Pair<ClanPlayer, PlayerStats>>
        get() = playerMap

    operator fun get(tag: String): Pair<ClanPlayer, PlayerStats>? {
        return playerMap[tag]
    }

    companion object {
        lateinit var instance: PlayerMap
            private set

        fun init(activity: Interface) {
            instance = PlayerMap(activity)
        }
    }

}