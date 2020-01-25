package eu.rtsketo.sakurastats.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import com.qwerjk.better_text.MagicTextView
import eu.rtsketo.sakurastats.R
import eu.rtsketo.sakurastats.control.ThreadPool
import eu.rtsketo.sakurastats.control.ViewDecor
import eu.rtsketo.sakurastats.dbobjects.ClanPlayer
import eu.rtsketo.sakurastats.dbobjects.PlayerStats
import eu.rtsketo.sakurastats.hashmaps.PlayerMap
import eu.rtsketo.sakurastats.hashmaps.SDPMap
import eu.rtsketo.sakurastats.main.Console
import eu.rtsketo.sakurastats.main.Interface
import eu.rtsketo.sakurastats.main.Service
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.util.*

class WarStatistics : Fragment() {
    private var root: View? = null
    var wifi: ImageView? = null
        get() {
            if (field == null) field = root!!.findViewById(R.id.wifi)
            return field
        }
        private set
    private var loadView: MagicTextView? = null
    private var info: MagicTextView? = null
    private var warBar: ConstraintLayout? = null
    private var lineage: LinearLayout? = null
    private var loadingAnim: ImageView? = null
    private var sortRatio: ImageView? = null
    private var sortScore: ImageView? = null
    private var sortTroph: ImageView? = null
    private var playerMap: MutableMap<String, PlayerView?>? = null
    private val observers = BooleanArray(2)
    private val playerView = arrayOfNulls<PlayerView>(50)
    private val size = intArrayOf(SDPMap.Companion.sdp2px(9), SDPMap.Companion.sdp2px(8))
    var loading = false
        set(loading) {
            if (loading && !this.loading) acti!!.runOnUiThread {
                sortRatio!!.isEnabled = false
                sortScore!!.isEnabled = false
                sortTroph!!.isEnabled = false
                loadingAnim!!.isEnabled = false
                loadingAnim!!.colorFilter = null
                loadView!!.visibility = View.VISIBLE
                ViewDecor.animateView(loadingAnim, true)
                ViewDecor.blinkView(acti!!.getTab(1), true)
                decorate(loadView, "Loading...", size[0])
                loadingAnim!!.setImageResource(R.drawable.loading)
                sortRatio!!.setColorFilter(Color.argb(100, 200, 200, 200))
                sortScore!!.setColorFilter(Color.argb(100, 200, 200, 200))
                sortTroph!!.setColorFilter(Color.argb(100, 200, 200, 200))
            } else if (!loading && this.loading) {
                acti!!.runOnUiThread {
                    sortRatio!!.isEnabled = true
                    sortScore!!.isEnabled = true
                    sortTroph!!.isEnabled = true
                    info!!.visibility = View.GONE
                    sortRatio!!.colorFilter = null
                    sortScore!!.colorFilter = null
                    sortTroph!!.colorFilter = null
                    ViewDecor.animateView(loadingAnim, false)
                    ViewDecor.blinkView(acti!!.getTab(1), false)
                    loadingAnim!!.setImageResource(R.drawable.refresh)
                    loadingAnim!!.setColorFilter(Color.argb(100, 200, 200, 200))
                }
                val timer = Timer()
                timer.schedule(object : TimerTask() {
                    override fun run() {
                        val refresh = 60 - acti!!.getLastForce(1)
                        if (refresh < 0) acti!!.runOnUiThread {
                            loadView!!.visibility = View.INVISIBLE
                            loadingAnim!!.colorFilter = null
                            loadingAnim!!.isEnabled = true
                            timer.cancel()
                        } else acti!!.runOnUiThread {
                            loadView!!.visibility = View.VISIBLE
                            decorate(loadView, refresh.toString() + "min", size[0])
                        }
                    }
                }, 0, 60000)
            }
            field = loading
        }
    private var acti: Interface? = null
    fun psObserver(): Observer<PlayerStats> {
        return object : Observer<PlayerStats?> {
            override fun onSubscribe(d: Disposable) {
                subscribe(0)
            }

            override fun onNext(ps: PlayerStats?) {
                displayStats(ps)
            }

            override fun onComplete() {
                complete(0)
            }

            override fun onError(e: Throwable) {
                Log.e(Interface.Companion.TAG, "PS Observer failed", e)
            }
        }
    }

    fun cpObserver(): Observer<ClanPlayer> {
        return object : Observer<ClanPlayer> {
            override fun onSubscribe(d: Disposable) {
                subscribe(1)
            }

            override fun onNext(cp: ClanPlayer) {
                displayStats(cp)
            }

            override fun onComplete() {
                complete(1)
            }

            override fun onError(e: Throwable) {
                Log.e(Interface.Companion.TAG, "CP Observer failed", e)
            }
        }
    }

    private fun subscribe(obs: Int) {
        observers[obs] = true
        if (!observers[1 - obs]) {
            acti!!.runOnUiThread {
                acti!!.changeTabTo(1)
                info!!.visibility = View.VISIBLE
            }
            loading = true
            clearList()
        }
    }

    private fun complete(obs: Int) {
        observers[obs] = false
        if (!observers[1 - obs]) loading = false
    }

    private fun displayStats(cp: ClanPlayer) {
        displayStats(null, cp)
    }

    private fun displayStats(ps: PlayerStats?, cp: ClanPlayer? = null) {
        val tagString: String
        tagString = ps?.tag ?: cp.getTag()
        var pv = playerMap!![tagString]
        if (pv == null) pv = getPV(tagString)
        val finalPV = pv
        if (cp != null) {
            val scoreText = if (cp.score == 9001) "max" else cp.score.toString()
            val role: String = Console.Companion.convertRole(cp.role)
            acti!!.runOnUiThread {
                decorate(finalPV!!.score, scoreText, size[0])
                ViewDecor.decorate(finalPV!!.troph, cp.trophies, size[1])
                ViewDecor.decorate(finalPV!!.tag, role, size[1].toFloat(),
                        Color.WHITE, maxy[1])
                finalPV.frame.visibility = View.VISIBLE
            }
        }
        if (ps != null) {
            val normaText: String = (ps.norma * 100) as Int.toString()+"%"
            val ratioText: String = (ps.ratio * 100) as Int.toString()+"%"
            acti!!.runOnUiThread {
                decorate(finalPV!!.norma, normaText, size[0])
                ViewDecor.decorate(finalPV!!.wars, ps.wars, size[1])
                ViewDecor.decorate(finalPV!!.missed, ps.missed, size[1])
                decorate(finalPV!!.ratio, ratioText, size[1])
                ViewDecor.decorate(finalPV!!.name, ps.name, size[0].toFloat(),
                        Color.WHITE, maxy[0])
                finalPV.frame.visibility = View.VISIBLE
            }
        }
    }

    private fun getPV(tag: String): PlayerView? {
        var pvNum: Int = PlayerMap.Companion.getInstance()
                .size() - 1 - playerMap!!.size
        pvNum = Math.max(0, Math.min(49, pvNum))
        val pv = playerView[pvNum]
        playerMap!![tag] = pv
        return pv
    }

    fun refreshList(choice: Int) {
        val pm: PlayerMap = PlayerMap.Companion.getInstance()
        loading = true
        clearList()
        val tempList: List<*>
        tempList = if (choice == 3) {
            val tempPS: MutableList<PlayerStats?> = ArrayList()
            for ((_, value) in pm.all) if (value.first != null) tempPS.add(value.second)
            Collections.sort(tempPS, SortByRatio())
            tempPS
        } else {
            val tempCP: MutableList<ClanPlayer?> = ArrayList()
            for ((_, value) in pm.all) if (value.first != null) tempCP.add(value.first)
            if (choice == 1) Collections.sort(tempCP, SortByTrophies()) else if (choice == 2) Collections.sort(tempCP, SortByScore())
            tempCP
        }
        for (c in Math.min(tempList.size - 1, 49) downTo 0) {
            var tag: String
            SystemClock.sleep(20)
            tag = if (tempList[c] is PlayerStats) (tempList[c] as PlayerStats).tag else (tempList[c] as ClanPlayer).tag
            val pair = pm[tag]
            if (pair != null) displayStats(pair.second, pair.first)
        }
        loading = false
    }

    fun clearList() {
        playerMap!!.clear()
        acti!!.runOnUiThread {
            for (pv in playerView) {
                val frame = pv!!.frame
                if (frame != null) frame.visibility = View.GONE
            }
        }
    }

    override fun onAttach(context: Context) {
        acti = activity as Interface?
        playerMap = HashMap()
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        root = inflater.inflate(R.layout.fragment_warstats, container, false)
        lineage = root.findViewById(R.id.lineage)
        warBar = root.findViewById(R.id.warBar)
        loadingAnim = root.findViewById(R.id.loadingAnim)
        sortRatio = root.findViewById(R.id.sortRatio)
        sortScore = root.findViewById(R.id.sortScore)
        sortTroph = root.findViewById(R.id.sortTroph)
        loadView = root.findViewById(R.id.loading)
        info = MagicTextView(activity)
        info!!.textAlignment = View.TEXT_ALIGNMENT_CENTER
        sortTroph.setOnClickListener(View.OnClickListener { view: View ->
            ViewDecor.bounce(view, acti)
            selectSort(1)
        })
        sortScore.setOnClickListener(View.OnClickListener { view: View ->
            ViewDecor.bounce(view, acti)
            selectSort(2)
        })
        sortRatio.setOnClickListener(View.OnClickListener { view: View ->
            ViewDecor.bounce(view, acti)
            selectSort(3)
        })
        loadingAnim.setOnClickListener(View.OnClickListener { v: View ->
            ViewDecor.bounce(v, acti)
            Service.Companion.getThread()!!.start(acti.getLastClan(), true, false)
        })
        initFrames()
        return root
    }

    private fun initFrames() {
        val sort: MagicTextView = root!!.findViewById(R.id.sort)
        acti!!.runOnUiThread {
            decorate(sort, "Sort by:", size[0])
            decorate(loadView, "Loading...", size[0])
            decorate(info, "\nStats are being loaded," +
                    " please wait...\n", size[0] + 2.toFloat())
            lineage!!.addView(info)
        }
        for (c in playerView.indices) {
            val frame = LayoutInflater.from(acti)
                    .inflate(R.layout.frame_warstats, null)
            playerView[c] = PlayerView(frame)
            val bgFrame = frame.findViewById<ImageView>(R.id.frameBG2)
            if (c % 2 == 1) bgFrame.setImageResource(R.drawable.background_2)
            playerView[c]!!.troph = frame.findViewById(R.id.trophies)
            playerView[c]!!.missed = frame.findViewById(R.id.missed)
            playerView[c]!!.score = frame.findViewById(R.id.score)
            playerView[c]!!.norma = frame.findViewById(R.id.norma)
            playerView[c]!!.cards = frame.findViewById(R.id.cards)
            playerView[c]!!.ratio = frame.findViewById(R.id.ratio)
            playerView[c]!!.name = frame.findViewById(R.id.name)
            playerView[c]!!.wars = frame.findViewById(R.id.wars)
            playerView[c]!!.tag = frame.findViewById(R.id.tag)
            frame.visibility = View.GONE
            val rank: MagicTextView = frame.findViewById(R.id.rank)
            acti!!.runOnUiThread {
                decorate(rank, c + 1 + "", size[0] + 2.toFloat())
                decorate(playerView[c]!!.score, "???", size[0])
                decorate(playerView[c]!!.norma, "???", size[0])
                decorate(playerView[c]!!.troph, "???", size[1])
                decorate(playerView[c]!!.wars, "???", size[1])
                decorate(playerView[c]!!.missed, "???", size[1])
                decorate(playerView[c]!!.cards, "???", size[1])
                decorate(playerView[c]!!.ratio, "???", size[1])
                ViewDecor.decorate(playerView[c]!!.name, "???", size[0].toFloat(),
                        Color.WHITE, maxy[0])
                ViewDecor.decorate(playerView[c]!!.tag, "???         " +
                        "      ", size[1].toFloat(), Color.WHITE, maxy[1])
                lineage!!.addView(frame)
            }
        }
        ViewDecor.rotate(root!!.findViewById(R.id.warSelection), acti)
        selectSort(0)
    }

    @Synchronized
    fun selectSort(choice: Int) {
        ThreadPool.getCachePool().execute {
            val conSet = ConstraintSet()
            var posView = ConstraintSet.START
            val idView: Int
            var disView = 8
            var seleVis = View.VISIBLE
            when (choice) {
                1 -> idView = R.id.sortScore
                2 -> idView = R.id.sortRatio
                3 -> {
                    idView = R.id.warBar
                    posView = ConstraintSet.END
                    disView = 3
                }
                else -> {
                    seleVis = View.INVISIBLE
                    idView = R.id.loadingAnim
                }
            }
            conSet.clone(warBar)
            conSet.connect(R.id.warSelection,
                    ConstraintSet.END, idView, posView, SDPMap.Companion.sdp2px(disView))
            conSet.setVisibility(R.id.warSelection, seleVis)
            TransitionManager.beginDelayedTransition(warBar)
            acti!!.runOnUiThread { conSet.applyTo(warBar) }
            if (choice > 0) refreshList(choice)
        }
    }

    fun updateLoading(cur: Int, max: Int) {
        val percent = cur * 100 / max
        acti!!.runOnUiThread {
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
            return (b.norma * 10000 - a.norma * 10000) as Int
        }
    }

    private inner class PlayerView(val frame: View) {
        val name: MagicTextView? = null
        val score: MagicTextView? = null
        val norma: MagicTextView? = null
        val tag: MagicTextView? = null
        val troph: MagicTextView? = null
        val wars: MagicTextView? = null
        val missed: MagicTextView? = null
        val cards: MagicTextView? = null
        val ratio: MagicTextView? = null

    }

    companion object {
        private val maxy = intArrayOf(112, 62)
    }
}