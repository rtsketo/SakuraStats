package eu.rtsketo.sakurastats.fragments

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.qwerjk.better_text.MagicTextView
import eu.rtsketo.sakurastats.R
import eu.rtsketo.sakurastats.control.DataFetch
import eu.rtsketo.sakurastats.control.DataRoom
import eu.rtsketo.sakurastats.control.ThreadPool
import eu.rtsketo.sakurastats.control.ViewDecor
import eu.rtsketo.sakurastats.control.ViewDecor.decorate
import eu.rtsketo.sakurastats.dbobjects.ClanStats
import eu.rtsketo.sakurastats.hashmaps.SDPMap
import eu.rtsketo.sakurastats.main.Console
import eu.rtsketo.sakurastats.main.Interface
import kotlinx.android.synthetic.main.fragment_prognose.*
import java.util.*

class Prognostics : Fragment() {
    private var loadView: MagicTextView = null
    var console: MagicTextView = null
        private set
    private var lineage: LinearLayout = null
    private var loading = false
    private var acti: Interface = null
    private var root: View = null
    override fun onAttach(context: Context) {
        acti = activity as Interface
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup,
                              savedInstanceState: Bundle): View {
        root = inflater.inflate(R.layout.fragment_prognose, container, false)
        (bgWaves.drawable as AnimationDrawable).start()
        decorate(loadView, "Loading...", 10f)
        Console.create(acti, console)
        loadingAnim.setOnClickListener { refresh(true) }
        return root
    }

    fun refresh(force: Boolean) {
        ThreadPool.cachePool.execute {
            removeViews()
            setLoading(true)
            val tag = acti.getLastClan()
            if (force || acti.getLastUse(tag)) {
                val cs = DataFetch(acti)
                        .getClanStats(tag)
                setStats(cs)
                acti.setLastForce(0)
                //                if (cs.size() == 5)
//                    acti.runOnUiThread(() ->
//                        acti.changeTabTo(0));
            } else setStats(DataRoom.Companion.getInstance().getDao()
                    .getClanStatsList(tag))
        }
    }

    fun setLoading(loading: Boolean) {
        val size: Int = SDPMap.Companion.sdp2px(10)
        if (loading && !this.loading) acti.runOnUiThread {
            loadingAnim.isEnabled = false
            loadingAnim.colorFilter = null
            loadView.visibility = View.VISIBLE
            loadingOp.visibility = View.VISIBLE
            loadingAnim.visibility = View.VISIBLE
            decorate(loadView, "Loading...", size.toFloat())
            loadingAnim.setImageResource(R.drawable.loading)
            ViewDecor.animateView(loadingAnim, true)
            ViewDecor.blinkView(acti.getTab(0), true)
        } else if (!loading && this.loading) {
            acti.runOnUiThread {
                ViewDecor.animateView(loadingAnim, false)
                ViewDecor.blinkView(acti.getTab(0), false)
                loadingAnim.setImageResource(R.drawable.refresh)
                loadingAnim.setColorFilter(Color.argb(100, 200, 200, 200))
            }
            val timer = Timer()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    val refresh = 15 - acti.getLastForce(0)
                    if (refresh < 0) acti.runOnUiThread {
                        loadView.visibility = View.INVISIBLE
                        loadingOp.visibility = View.INVISIBLE
                        loadingAnim.colorFilter = null
                        loadingAnim.isEnabled = true
                        timer.cancel()
                    } else acti.runOnUiThread {
                        loadView.visibility = View.VISIBLE
                        decorate(loadView, "     " + refresh + "min", size.toFloat())
                    }
                }
            }, 0, 60000)
        }
        this.loading = loading
    }

    fun setStats(clans: List<ClanStats>) {
        acti.runOnUiThread { console.visibility = View.GONE }
        if (clans.size == 5) {
            var counter = 0
            val stats = arrayOfNulls<ClanStats>(5)
            for (clan in clans) stats[counter++] = clan
            Arrays.sort(stats, SortByPrediction())
            for (clan in stats) addClan(clan)
        } else {
            val info = MagicTextView(acti)
            val more = MagicTextView(acti)
            val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            info.layoutParams = params
            more.layoutParams = params
            decorate(info, "The clan isn't currently in War\n\n\n\n\n\n\n\n\n", SDPMap.Companion.sdp2px(10).toFloat())
            decorate(more, "Stats can't be refreshed in less than 15min", SDPMap.Companion.sdp2px(8).toFloat())
            info.textAlignment = View.TEXT_ALIGNMENT_CENTER
            more.textAlignment = View.TEXT_ALIGNMENT_CENTER
            acti.runOnUiThread {
                lineage.addView(info)
                lineage.addView(more)
            }
        }
        setLoading(false)
    }

    fun removeViews() {
        acti.runOnUiThread { lineage.removeAllViews() }
    }

    private fun addClan(clan: ClanStats) {
        val predictWins = clan.getEstimatedWins()
        val plusOne = Math.round(clan.getExtraWins() as Float * 100)
        val frame = LayoutInflater.from(acti)
                .inflate(R.layout.frame_prognose, null)
        val name: MagicTextView = frame.findViewById(R.id.clanName)
        val wins: MagicTextView = frame.findViewById(R.id.actualWins)
        val pred: MagicTextView = frame.findViewById(R.id.predictWins)
        val tag: MagicTextView = frame.findViewById(R.id.clanTag)
        val remain: MagicTextView = frame.findViewById(R.id.remaining)
        val crown: MagicTextView = frame.findViewById(R.id.clanCrown)
        val pone: MagicTextView = frame.findViewById(R.id.clanPlusOne)
        val troph: MagicTextView = frame.findViewById(R.id.clanTrophies)
        val badge = frame.findViewById<ImageView>(R.id.clanBadge)
        val maxy = intArrayOf(104, 52)
        ViewDecor.decorate(name, clan.getName(), SDPMap.Companion.sdp2px(9).toFloat(), Color.WHITE, maxy[0])
        ViewDecor.decorate(tag, "#" + clan.getTag(), SDPMap.Companion.sdp2px(6).toFloat(), Color.WHITE, maxy[1])
        ViewDecor.decorate(wins, clan.getActualWins(), SDPMap.Companion.sdp2px(9).toFloat())
        ViewDecor.decorate(pred, predictWins, SDPMap.Companion.sdp2px(9).toFloat())
        ViewDecor.decorate(remain, clan.getRemaining(), SDPMap.Companion.sdp2px(6).toFloat())
        ViewDecor.decorate(crown, clan.getCrowns(), SDPMap.Companion.sdp2px(6).toFloat())
        decorate(pone, "$plusOne%", SDPMap.Companion.sdp2px(6).toFloat())
        ViewDecor.decorate(troph, clan.getWarTrophies(), SDPMap.Companion.sdp2px(6).toFloat(),
                Color.rgb(240, 179, 255))
        badge.setImageResource(acti.resources.getIdentifier(
                clan.getBadge(), "drawable", acti.packageName))
        acti.runOnUiThread { lineage.addView(frame) }
    }

    internal inner class SortByPrediction : Comparator<ClanStats> {
        override fun compare(a: ClanStats, b: ClanStats): Int {
            return ((b.estimatedWins + b.extraWins) * 100
                    - (a.estimatedWins + a.extraWins) * 100) as Int
        }
    }

}