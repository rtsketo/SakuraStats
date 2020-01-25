package eu.rtsketo.sakurastats.hashmaps

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Pair
import eu.rtsketo.sakurastats.dbobjects.ClanPlayer
import eu.rtsketo.sakurastats.dbobjects.PlayerStats
import eu.rtsketo.sakurastats.main.Interface
import io.reactivex.subjects.ReplaySubject
import java.util.*

class PlayerMap private constructor(private val acti: Interface) {
    private var playerMap: MutableMap<String, Pair<ClanPlayer, PlayerStats?>>? = null
    private val sync = Any()
    private var cpSub: ReplaySubject<ClanPlayer?>? = null
    private var psSub: ReplaySubject<PlayerStats?>? = null
    private val DELAY = 20
    private var time: Long = 0
    private var size = 0
    fun size(): Int {
        return size
    }

    fun put(tag: String, player: Any?) {
        var pair = playerMap!![tag]
        val handler = Handler(Looper.getMainLooper())
        if (pair == null) pair = Pair(ClanPlayer(), PlayerStats())
        synchronized(sync) {
            val now = SystemClock.uptimeMillis()
            if (time == 0L || now > time + DELAY) time = now
            time += DELAY.toLong()
        }
        if (player is ClanPlayer) {
            pair = Pair(player, pair.second)
            handler.postAtTime({ cpSub!!.onNext((player as ClanPlayer?)!!) }, time)
        } else {
            pair = Pair(pair.first, player as PlayerStats?)
            handler.postAtTime({ psSub!!.onNext(player!!) }, time)
        }
        playerMap!![tag] = pair
    }

    fun reset(size: Int) {
        synchronized(PlayerMap::class.java) {
            if (playerMap != null) playerMap!!.clear() else playerMap = HashMap(size)
            this.size = size
        }
        val wFrag = acti.warFrag
        val aFrag = acti.actiFrag
        psSub = ReplaySubject.create()
        cpSub = ReplaySubject.create()
        cpSub.subscribe(wFrag!!.cpObserver())
        psSub.subscribe(wFrag!!.psObserver())
        cpSub.subscribe(aFrag!!.cpObserver())
        psSub.subscribe(aFrag!!.psObserver())
    }

    fun completeSubs() {
        val handler = Handler(Looper.getMainLooper())
        handler.postAtTime({
            cpSub!!.onComplete()
            psSub!!.onComplete()
        }, time)
    }

    val all: Map<String, Pair<ClanPlayer, PlayerStats?>>?
        get() = playerMap

    operator fun get(tag: String?): Pair<ClanPlayer, PlayerStats>? {
        return playerMap!!.get(tag)
    }

    companion object {
        var instance: PlayerMap? = null
            private set

        fun init(activity: Interface) {
            instance = PlayerMap(activity)
        }
    }

}