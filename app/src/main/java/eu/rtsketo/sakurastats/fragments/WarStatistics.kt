package eu.rtsketo.sakurastats.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import com.qwerjk.better_text.MagicTextView
import eu.rtsketo.sakurastats.R
import eu.rtsketo.sakurastats.control.ThreadPool
import eu.rtsketo.sakurastats.control.ViewDecor
import eu.rtsketo.sakurastats.control.ViewDecor.bounce
import eu.rtsketo.sakurastats.control.ViewDecor.decorate
import eu.rtsketo.sakurastats.dbobjects.ClanPlayer
import eu.rtsketo.sakurastats.dbobjects.PlayerStats
import eu.rtsketo.sakurastats.hashmaps.PlayerMap
import eu.rtsketo.sakurastats.hashmaps.SDPMap.Companion.sdp2px
import eu.rtsketo.sakurastats.main.Console
import eu.rtsketo.sakurastats.main.Interface
import eu.rtsketo.sakurastats.main.Interface.Companion.TAG
import eu.rtsketo.sakurastats.main.Service
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_warstats.*
import kotlinx.android.synthetic.main.fragment_warstats.view.*
import java.util.*

class WarStatistics: Fragment() {
    private val size = intArrayOf(sdp2px(9), sdp2px(8))
    private var playerMap = mutableMapOf<String, PlayerView>()
    private val playerView = arrayListOf<PlayerView>()
    private val observers = BooleanArray(2)
    private lateinit var info: MagicTextView
    private val maxy = intArrayOf(112, 62)
    private var acti: Interface? = null

    var loading = false
        set(loading) {
            if (loading && !this.loading)
                acti?.runOnUiThread {
                    sortRatio.isEnabled = false
                    sortScore.isEnabled = false
                    sortTroph.isEnabled = false
                    loadingAnim.isEnabled = false
                    loadingAnim.colorFilter = null
                    loadView.visibility = VISIBLE
                    ViewDecor.animateView(loadingAnim, true)
                    ViewDecor.blinkView(acti!!.getTab(1), true)
                    decorate(loadView, "Loading...", size[0])
                    loadingAnim.setImageResource(R.drawable.loading)
                    sortRatio.setColorFilter(Color.argb(100, 200, 200, 200))
                    sortScore.setColorFilter(Color.argb(100, 200, 200, 200))
                    sortTroph.setColorFilter(Color.argb(100, 200, 200, 200))
                }
            else if (!loading && this.loading) {
                acti?.runOnUiThread {
                    info.visibility = GONE
                    sortRatio.isEnabled = true
                    sortScore.isEnabled = true
                    sortTroph.isEnabled = true
                    sortRatio.colorFilter = null
                    sortScore.colorFilter = null
                    sortTroph.colorFilter = null
                    ViewDecor.animateView(loadingAnim, false)
                    ViewDecor.blinkView(acti!!.getTab(1), false)
                    loadingAnim.setImageResource(R.drawable.refresh)
                    loadingAnim.setColorFilter(Color.argb(100, 200, 200, 200))
                }

                val timer = Timer()
                timer.schedule(object : TimerTask() {
                    override fun run() {
                        val refresh = 60 - (acti?.getLastForce(1) ?: 0)
                        if (refresh < 0) acti?.runOnUiThread {
                            loadView.visibility = INVISIBLE
                            loadingAnim.colorFilter = null
                            loadingAnim.isEnabled = true
                            timer.cancel()
                        } else acti?.runOnUiThread {
                            loadView.visibility = VISIBLE
                            decorate(loadView, refresh.toString() + "min", size[0])
                        }
                    }
                }, 0, 60000)
            }
            field = loading
        }

    fun psObserver(): Observer<PlayerStats> {
        return object : Observer<PlayerStats> {
            override fun onSubscribe(d: Disposable) { subscribe(0) }
            override fun onNext(ps: PlayerStats) { displayStats(ps) }
            override fun onComplete() { complete(0) }
            override fun onError(e: Throwable) {
                Log.e(TAG, "PS Observer failed", e)
            }
        }
    }

    fun cpObserver(): Observer<ClanPlayer> {
        return object : Observer<ClanPlayer> {
            override fun onSubscribe(d: Disposable) { subscribe(1) }
            override fun onNext(cp: ClanPlayer) { displayStats(cp) }
            override fun onComplete() { complete(1) }
            override fun onError(e: Throwable) {
                Log.e(TAG, "CP Observer failed", e)
            }
        }
    }

    private fun subscribe(obs: Int) {
        observers[obs] = true
        if (!observers[1 - obs]) {
            acti?.runOnUiThread {
//                acti!!.changeTabTo(1)
                info.visibility = VISIBLE
            }
            loading = true
            clearList()
        }
    }

    private fun complete(obs: Int) {
        observers[obs] = false
        if (!observers[1 - obs]) loading = false
    }

    private fun displayStats(cp: ClanPlayer) { displayStats(null, cp) }
    private fun displayStats(ps: PlayerStats) { displayStats(ps, null) }
    private fun displayStats(ps: PlayerStats?, cp: ClanPlayer?) {
        val tagString = ps?.tag ?: cp?.tag ?: ""
        var pv = playerMap[tagString]
        if (pv == null) pv = getPV(tagString)
        val finalPV = pv
        if (cp != null) {
            val scoreText = if (cp.score == 9001) "max" else cp.score.toString()
            val role: String = Console.Companion.convertRole(cp.role)
            acti?.runOnUiThread {
                decorate(finalPV.score, scoreText, size[0])
                decorate(finalPV.troph, cp.trophies, size[1])
                decorate(finalPV.tag, role, size[1].toFloat(),
                        Color.WHITE, maxy[1])
                finalPV.frame.visibility = VISIBLE
            }
        }
        if (ps != null) {
            val normaText = (ps.norma * 100).toInt().toString()+"%"
            val ratioText = (ps.ratio * 100).toInt().toString()+"%"
            acti?.runOnUiThread {
                decorate(finalPV.norma, normaText, size[0])
                decorate(finalPV.wars, ps.wars, size[1])
                decorate(finalPV.missed, ps.missed, size[1])
                decorate(finalPV.ratio, ratioText, size[1])
                decorate(finalPV.name, ps.name, size[0].toFloat(),
                        Color.WHITE, maxy[0])
                finalPV.frame.visibility = VISIBLE
            }
        }
    }

    private fun getPV(tag: String): PlayerView {
        var pvNum: Int = PlayerMap.instance
                .size() - 1 - playerMap.size
        pvNum = 0.coerceAtLeast(49.coerceAtMost(pvNum))
        val pv = playerView[pvNum]
        playerMap[tag] = pv
        return pv
    }

    fun refreshList(choice: Int) {
        val pm: PlayerMap = PlayerMap.instance
        loading = true
        clearList()
        val tempList: List<*>
        tempList = if (choice == 3) {
            val tempPS: MutableList<PlayerStats> = ArrayList()
            for ((_, value) in pm.all) 
                if (value.first != null) tempPS.add(value.second)
            Collections.sort(tempPS, SortByRatio())
            tempPS
        } else {
            val tempCP: MutableList<ClanPlayer> = ArrayList()
            for ((_, value) in pm.all) 
                if (value.first != null) tempCP.add(value.first)
            if (choice == 1) Collections.sort(tempCP, SortByTrophies()) 
            else if (choice == 2) Collections.sort(tempCP, SortByScore())
            tempCP
        }
        for (c in (tempList.size - 1).coerceAtMost(49) downTo 0) {
            SystemClock.sleep(20)
            val tag: String =
                    if (tempList[c] is PlayerStats)
                        (tempList[c] as PlayerStats).tag
                    else (tempList[c] as ClanPlayer).tag
            val pair = pm[tag]
            if (pair != null) displayStats(pair.second, pair.first)
        }
        loading = false
    }

    fun clearList() {
        playerMap.clear()
        acti?.runOnUiThread {
            playerView.forEach {
                it.frame.visibility = GONE
            }
        }
    }

    override fun onAttach(context: Context) {
        acti = activity as Interface
        playerMap = HashMap()
        super.onAttach(context)
    }

    private fun bounceButton(v: View) {
        acti?.apply { bounce(v, this) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_warstats, container, false)
        info = MagicTextView(context)
        info.textAlignment = TEXT_ALIGNMENT_CENTER
        view.sortTroph.setOnClickListener { v -> bounceButton(v); selectSort(1) }
        view.sortScore.setOnClickListener { v -> bounceButton(v); selectSort(2) }
        view.sortRatio.setOnClickListener { v -> bounceButton(v); selectSort(3) }
        view.loadingAnim.setOnClickListener { v ->
            acti?.lastClan?.let { Service.getThread()
                    .start(it, force = true, tab = false) }
            bounceButton(v)
        }

        initFrames(view)
        return view
    }

    private fun initFrames(view: View) {
        acti?.runOnUiThread {
            decorate(view.sort, "Sort by:", size[0])
            decorate(view.loadView, "Loading...", size[0])
            decorate(info, "\nStats are being loaded," +
                    " please wait...\n", size[0] + 2.toFloat())
            view.lineage.addView(info)
        }

        for(c in 0..50) {
            val frame = LayoutInflater.from(acti)
                    .inflate(R.layout.frame_warstats, null)
            playerView.add(PlayerView(frame))
            val bgFrame = frame.findViewById<ImageView>(R.id.frameBG2)
            if (c % 2 == 1) bgFrame.setImageResource(R.drawable.background_2)
            playerView[c].troph = frame.findViewById(R.id.trophies)
            playerView[c].missed = frame.findViewById(R.id.missed)
            playerView[c].score = frame.findViewById(R.id.score)
            playerView[c].norma = frame.findViewById(R.id.norma)
            playerView[c].cards = frame.findViewById(R.id.cards)
            playerView[c].ratio = frame.findViewById(R.id.ratio)
            playerView[c].name = frame.findViewById(R.id.name)
            playerView[c].wars = frame.findViewById(R.id.wars)
            playerView[c].tag = frame.findViewById(R.id.tag)
            frame.visibility = GONE

            val rank: MagicTextView = frame.findViewById(R.id.rank)
            acti?.runOnUiThread {
                decorate(rank, "${c + 1}", size[0] + 2.toFloat())
                decorate(playerView[c].score, "", size[0])
                decorate(playerView[c].norma, "", size[0])
                decorate(playerView[c].troph, "", size[1])
                decorate(playerView[c].wars, "", size[1])
                decorate(playerView[c].missed, "", size[1])
                decorate(playerView[c].cards, "", size[1])
                decorate(playerView[c].ratio, "", size[1])
                decorate(playerView[c].name, "", size[0].toFloat(),
                        Color.WHITE, maxy[0])
                decorate(playerView[c].tag, "         " +
                        "      ", size[1].toFloat(), Color.WHITE, maxy[1])
                view.lineage.addView(frame)
            }
        }

        acti?.let { ViewDecor.rotate(view.warSelection, it) }
        selectSort(0)
    }

    @Synchronized
    fun selectSort(choice: Int) {
        ThreadPool.cachePool.execute {
            val conSet = ConstraintSet()
            var posView = ConstraintSet.START
            val idView: Int
            var disView = 8
            var seleVis = VISIBLE
            when (choice) {
                1 -> idView = R.id.sortScore
                2 -> idView = R.id.sortRatio
                3 -> {
                    idView = R.id.warBar
                    posView = ConstraintSet.END
                    disView = 3
                }
                else -> {
                    seleVis = INVISIBLE
                    idView = R.id.loadingAnim
                }
            }
            conSet.clone(warBar)
            conSet.connect(R.id.warSelection,
                    ConstraintSet.END, idView, posView, sdp2px(disView))
            conSet.setVisibility(R.id.warSelection, seleVis)
            TransitionManager.beginDelayedTransition(warBar)
            acti?.runOnUiThread { conSet.applyTo(warBar) }
            if (choice > 0) refreshList(choice)
        }
    }

    fun updateLoading(cur: Int, max: Int) {
        val percent = cur * 100 / max
        acti?.runOnUiThread {
            decorate(loadView, "... " +
                    percent + "%", size[0])
        }
    }

    private inner class SortByScore : Comparator<ClanPlayer> {
        override fun compare(a: ClanPlayer, b: ClanPlayer): Int {
            return b.score - a.score
        }
    }

    private inner class SortByTrophies : Comparator<ClanPlayer> {
        override fun compare(a: ClanPlayer, b: ClanPlayer): Int {
            return b.trophies - a.trophies
        }
    }

    private inner class SortByRatio : Comparator<PlayerStats> {
        override fun compare(a: PlayerStats, b: PlayerStats): Int {
            return (b.norma * 10000 - a.norma * 10000).toInt()
        }
    }

    inner class PlayerView(val frame: View) {
        var name = MagicTextView(context)
        var score = MagicTextView(context)
        var norma = MagicTextView(context)
        var tag = MagicTextView(context)
        var troph = MagicTextView(context)
        var wars = MagicTextView(context)
        var missed = MagicTextView(context)
        var cards = MagicTextView(context)
        var ratio = MagicTextView(context)
    }

    companion object {
        val instance = WarStatistics()
    }
}