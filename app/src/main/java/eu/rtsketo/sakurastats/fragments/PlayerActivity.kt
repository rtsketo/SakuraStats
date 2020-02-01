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
import eu.rtsketo.sakurastats.hashmaps.LeagueMap
import eu.rtsketo.sakurastats.hashmaps.PlayerMap
import eu.rtsketo.sakurastats.hashmaps.SDPMap
import eu.rtsketo.sakurastats.main.Interface
import eu.rtsketo.sakurastats.main.Service
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.util.*

class PlayerActivity : Fragment() {
    private var loadView: MagicTextView = null
    private var actiBar: ConstraintLayout = null
    private var lineage: LinearLayout = null
    private var info: MagicTextView = null
    var loading = false
        set(loading) {
            if (loading && !this.loading) acti.runOnUiThread {
                sortSMC.isEnabled = false
                sortTime.isEnabled = false
                sortMagi.isEnabled = false
                sortLege.isEnabled = false
                loadingAnim.isEnabled = false
                loadingAnim.colorFilter = null
                loadView.visibility = View.VISIBLE
                ViewDecor.animateView(loadingAnim, true)
                ViewDecor.blinkView(acti.getTab(2), true)
                decorate(loadView, "Loading...", size[1])
                loadingAnim.setImageResource(R.drawable.loading)
                sortSMC.setColorFilter(Color.argb(100, 200, 200, 200))
                sortMagi.setColorFilter(Color.argb(100, 200, 200, 200))
                sortLege.setColorFilter(Color.argb(100, 200, 200, 200))
                sortTime.setColorFilter(Color.argb(100, 200, 200, 200))
            } else if (!loading && this.loading) {
                acti.runOnUiThread {
                    sortSMC.isEnabled = true
                    sortTime.isEnabled = true
                    sortMagi.isEnabled = true
                    sortLege.isEnabled = true
                    sortSMC.colorFilter = null
                    sortMagi.colorFilter = null
                    sortLege.colorFilter = null
                    sortTime.colorFilter = null
                    info.visibility = View.GONE
                    ViewDecor.animateView(loadingAnim, false)
                    ViewDecor.blinkView(acti.getTab(2), false)
                    loadingAnim.setImageResource(R.drawable.refresh)
                    loadingAnim.setColorFilter(Color.argb(100, 200, 200, 200))
                }
                val timer = Timer()
                timer.schedule(object : TimerTask() {
                    override fun run() {
                        val refresh = 60 - acti.getLastForce(2)
                        if (refresh < 0) acti.runOnUiThread {
                            loadView.visibility = View.INVISIBLE
                            loadingAnim.colorFilter = null
                            loadingAnim.isEnabled = true
                            timer.cancel()
                        } else acti.runOnUiThread {
                            loadView.visibility = View.VISIBLE
                            decorate(loadView, refresh.toString() + "min", size[1])
                        }
                    }
                }, 0, 60000)
            }
            field = loading
        }
    var wifi: ImageView = null
        get() {
            if (field == null) field = root.findViewById(R.id.wifi)
            return field
        }
        private set
    private var acti: Interface = null
    private var maxChest = 0
    private var root: View = null
    private var playerMap: MutableMap<String, PlayerView> = null
    private val observers = BooleanArray(2)
    private val playerView = arrayOfNulls<PlayerView>(50)
    private var loadingAnim: ImageView = null
    private var sortTime: ImageView = null
    private var sortMagi: ImageView = null
    private var sortLege: ImageView = null
    private var sortSMC: ImageView = null
    private var size: IntArray
    fun psObserver(): Observer<PlayerStats> {
        return object : Observer<PlayerStats> {
            override fun onSubscribe(d: Disposable) {
                subscribe(0)
            }

            override fun onNext(ps: PlayerStats) {
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
            loading = true
            acti.runOnUiThread { info.visibility = View.VISIBLE }
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

    private fun displayStats(ps: PlayerStats, cp: ClanPlayer = null) {
        val tagString: String
        tagString = ps.tag : cp.getTag()
        val maxy = 140
        val size = intArrayOf(SDPMap.Companion.sdp2px(9), SDPMap.Companion.sdp2px(8))
        val pv = if (playerMap[tagString] == null) getPV(tagString) else playerMap[tagString]
        if (cp != null) {
            val lastBattle = lastBattleText(cp.last)
            acti.runOnUiThread {
                decorate(pv.role, "#" + cp.tag, size[1])
                ViewDecor.decorate(pv.lege, cp.legendary, size[1])
                ViewDecor.decorate(pv.smc, cp.smc, size[1])
                decorate(pv.time, lastBattle, size[1])
                pv.frame.visibility = View.VISIBLE
            }
        }
        if (ps != null) {
            var leagueEmblem = 0
            var chestString = "--"
            val chest = ps.chest
            if (chest / 100 > 1 && maxChest - chest < 25) {
                var league = LeagueMap.o2l(chest - chest / 100 * 100)
                chestString = "#" + (league - league / 10 * 10)
                league = league / 10
                leagueEmblem = when (league) {
                    3 -> R.drawable.clanwars_league_silver
                    2 -> R.drawable.clanwars_league_gold
                    1 -> R.drawable.clanwars_league_legendary
                    else -> R.drawable.clanwars_league_bronze
                }
            } else leagueEmblem = R.drawable.no_clan
            val finalLeagueEmblem = leagueEmblem
            val finalChestString = chestString
            acti.runOnUiThread {
                pv.warChest.setImageResource(finalLeagueEmblem)
                decorate(pv.magi, finalChestString, size[1])
                ViewDecor.decorate(pv.name, ps.name, size[0].toFloat(), Color.WHITE, maxy)
                pv.frame.visibility = View.VISIBLE
            }
        }
    }

    private fun lastBattleText(lb: Long): String {
        val lastBattle: StringBuilder
        val diff = System.currentTimeMillis() / 1000.0 - lb
        var temp = (diff / 60 / 60 / 24 / 30).toInt()
        if (temp > 500) lastBattle = StringBuilder("N/A") else {
            if (temp > 0) lastBattle = StringBuilder("$temp month") else {
                temp = (diff / 60 / 60 / 24).toInt()
                lastBattle = if (temp > 0) StringBuilder("$temp day") else StringBuilder("few hours")
            }
            if (temp > 1) lastBattle.append("s")
        }
        return lastBattle.toString()
    }

    private fun getPV(tag: String): PlayerView {
        var pvNum: Int = PlayerMap.Companion.getInstance()
                .size() - 1 - playerMap.size
        pvNum = Math.max(0, Math.min(49, pvNum))
        val pv = playerView[pvNum]
        playerMap[tag] = pv
        return pv
    }

    override fun onAttach(context: Context) {
        acti = activity as Interface
        playerMap = HashMap()
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup,
                              savedInstanceState: Bundle): View {
        root = inflater.inflate(R.layout.fragment_activity,
                container, false)
        loadingAnim = root.findViewById(R.id.loadingAnim)
        sortTime = root.findViewById(R.id.sortTime)
        sortMagi = root.findViewById(R.id.sortMagi)
        sortLege = root.findViewById(R.id.sortLege)
        loadView = root.findViewById(R.id.loading)
        sortSMC = root.findViewById(R.id.sortSMC)
        actiBar = root.findViewById(R.id.actiBar)
        lineage = root.findViewById(R.id.lila)
        info = MagicTextView(acti)
        info.textAlignment = View.TEXT_ALIGNMENT_CENTER
        sortTime.setOnClickListener(View.OnClickListener { view: View ->
            ViewDecor.bounce(view, acti)
            selectSort(1)
        })
        sortSMC.setOnClickListener(View.OnClickListener { view: View ->
            ViewDecor.bounce(view, acti)
            selectSort(2)
        })
        sortLege.setOnClickListener(View.OnClickListener { view: View ->
            ViewDecor.bounce(view, acti)
            selectSort(3)
        })
        sortMagi.setOnClickListener(View.OnClickListener { view: View ->
            ViewDecor.bounce(view, acti)
            selectSort(4)
        })
        loadingAnim.setOnClickListener(View.OnClickListener { v: View ->
            ViewDecor.bounce(v, acti)
            Service.Companion.getThread().start(acti.getLastClan(), true, false)
        })
        initFrames()
        return root
    }

    private fun initFrames() {
        val sort: MagicTextView = root.findViewById(R.id.sort)
        size = intArrayOf(SDPMap.Companion.sdp2px(6), SDPMap.Companion.sdp2px(9))
        acti.runOnUiThread {
            decorate(sort, "Sort by:", size[1])
            decorate(loadView, "Loading...", size[1])
            decorate(info, "\nStats are being loaded," +
                    " please wait...\n", size[1] + 2.toFloat())
            lineage.addView(info)
        }
        for (c in playerView.indices) {
            val frame = LayoutInflater.from(acti)
                    .inflate(R.layout.frame_activity, null)
            playerView[c] = PlayerView(frame)
            val bgFrame = frame.findViewById<ImageView>(R.id.frameBG3)
            if (c % 2 == 1) bgFrame.setImageResource(R.drawable.background_2)
            playerView[c].warChest = frame.findViewById(R.id.magicalImage)
            playerView[c].magi = frame.findViewById(R.id.magical)
            playerView[c].lege = frame.findViewById(R.id.legend)
            playerView[c].name = frame.findViewById(R.id.namae)
            playerView[c].role = frame.findViewById(R.id.role)
            playerView[c].time = frame.findViewById(R.id.time)
            playerView[c].smc = frame.findViewById(R.id.smc)
            frame.visibility = View.GONE
            val finalPV = playerView[c]
            acti.runOnUiThread {
                decorate(frame.findViewById(R.id.lastBattle), "Last battle:", size[0])
                decorate(finalPV.role, "#", size[1])
                decorate(finalPV.time, "", size[1])
                decorate(finalPV.lege, "", size[1])
                decorate(finalPV.smc, "", size[1])
                decorate(finalPV.magi, "", size[1])
                decorate(finalPV.name, "", size[0])
                lineage.addView(frame)
            }
        }
        ViewDecor.rotate(root.findViewById(R.id.actiSelection), acti)
        selectSort(0)
    }

    @Synchronized
    fun selectSort(choice: Int) {
        ThreadPool.cachePool.execute {
            val conSet = ConstraintSet()
            var posView = ConstraintSet.START
            val idView: Int
            var seleVis = View.VISIBLE
            when (choice) {
                2 -> idView = R.id.sortLege
                3 -> idView = R.id.sortMagi
                4 -> idView = R.id.sortTime
                1 -> {
                    idView = R.id.actiBar
                    posView = ConstraintSet.END
                }
                else -> {
                    seleVis = View.INVISIBLE
                    idView = R.id.actiBar
                }
            }
            conSet.clone(actiBar)
            conSet.connect(R.id.actiSelection,
                    ConstraintSet.END, idView, posView, SDPMap.Companion.sdp2px(2))
            conSet.setVisibility(R.id.actiSelection, seleVis)
            TransitionManager.beginDelayedTransition(actiBar)
            acti.runOnUiThread { conSet.applyTo(actiBar) }
            if (choice > 0) refreshList(choice)
        }
    }

    fun refreshList(choice: Int) {
        val pm: PlayerMap = PlayerMap.Companion.getInstance()
        loading = true
        clearList()
        val tempPS: MutableList<PlayerStats> = ArrayList()
        if (choice == 0 || choice == 4) {
            for ((_, value) in pm.all) if (value.first != null) tempPS.add(value.second)
            Collections.sort(tempPS, SortByWar())
            if (!tempPS.isEmpty()) maxChest = tempPS[0].getChest()
        }
        if (choice != 4) {
            val tempCP: MutableList<ClanPlayer> = ArrayList()
            for ((_, value) in pm.all) if (value.first != null) tempCP.add(value.first)
            when (choice) {
                1 -> Collections.sort(tempCP, SortByTime())
                2 -> Collections.sort(tempCP, SortBySMC())
                3 -> Collections.sort(tempCP, SortByLege())
                else -> {
                }
            }
            for (c in Math.min(tempCP.size - 1, 49) downTo 0) {
                SystemClock.sleep(20)
                val tag = tempCP[c].getTag()
                val pair = pm[tag]
                if (pair != null) displayStats(pair.second, pair.first)
            }
        } else for (c in Math.min(tempPS.size - 1, 49) downTo 0) {
            SystemClock.sleep(20)
            val tag = tempPS[c].getTag()
            val pair = pm[tag]
            if (pair != null) displayStats(pair.second, pair.first)
        }
        loading = false
    }

    fun clearList() {
        playerMap.clear()
        acti.runOnUiThread {
            for (pv in playerView) {
                val frame = pv.frame
                if (frame != null) frame.visibility = View.GONE
            }
        }
    }

    fun updateLoading(cur: Int, max: Int) {
        val percent = cur * 100 / max
        acti.runOnUiThread {
            decorate(loadView, "... " +
                    percent + "%", size[1])
        }
    }

    private inner class SortByTime : Comparator<ClanPlayer> {
        override fun compare(a: ClanPlayer, b: ClanPlayer): Int {
            return (a.last - b.last) as Int
        }
    }

    private inner class SortBySMC : Comparator<ClanPlayer> {
        override fun compare(a: ClanPlayer, b: ClanPlayer): Int {
            return a.smc - b.smc
        }
    }

    private inner class SortByLege : Comparator<ClanPlayer> {
        override fun compare(a: ClanPlayer, b: ClanPlayer): Int {
            return a.legendary - b.legendary
        }
    }

    private inner class SortByMagi : Comparator<ClanPlayer> {
        override fun compare(a: ClanPlayer, b: ClanPlayer): Int {
            return a.magical - b.magical
        }
    }

    private inner class SortByWar : Comparator<PlayerStats> {
        override fun compare(a: PlayerStats, b: PlayerStats): Int {
            return b.chest - a.chest
        }
    }

    private inner class PlayerView(val frame: View) {
        val name: MagicTextView = null
        val role: MagicTextView = null
        val time: MagicTextView = null
        val magi: MagicTextView = null
        val lege: MagicTextView = null
        val smc: MagicTextView = null
        val warChest: ImageView = null

    }
}