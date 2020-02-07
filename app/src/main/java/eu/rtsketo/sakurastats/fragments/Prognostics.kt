package eu.rtsketo.sakurastats.fragments

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.qwerjk.better_text.MagicTextView
import eu.rtsketo.sakurastats.R
import eu.rtsketo.sakurastats.control.DataFetch
import eu.rtsketo.sakurastats.control.DataRoom
import eu.rtsketo.sakurastats.control.ThreadPool
import eu.rtsketo.sakurastats.control.ViewDecor
import eu.rtsketo.sakurastats.control.ViewDecor.decorate
import eu.rtsketo.sakurastats.dbobjects.ClanStats
import eu.rtsketo.sakurastats.hashmaps.SDPMap.Companion.sdp2px
import eu.rtsketo.sakurastats.main.Console
import eu.rtsketo.sakurastats.main.Interface
import kotlinx.android.synthetic.main.fragment_prognose.*
import kotlinx.android.synthetic.main.fragment_prognose.loadView
import kotlinx.android.synthetic.main.fragment_prognose.loadingAnim
import kotlinx.android.synthetic.main.fragment_warstats.*
import java.util.*
import kotlin.math.roundToInt

class Prognostics : Fragment() {
    private var loading = false
    private var acti: Interface? = null

    override fun onAttach(context: Context) {
        acti = activity as Interface
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (bgWaves?.drawable as AnimationDrawable).start()
        decorate(loadView, "Loading...", 10f)
        Console.create(acti as Activity, console)
        loadingAnim.setOnClickListener { refresh(true) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_prognose, container, false)
    }

    fun refresh(force: Boolean) {
        ThreadPool.cachePool.execute {
            removeViews()
            setLoading(true)
            val tag = acti?.lastClan ?: ""
            if (force || acti?.getLastUse(tag) == true) {
                acti?.apply {
                    DataFetch(this)
                            .getClanStats(tag)
                            ?.let { setStats(it)} }
                acti?.setLastForce(0)
            } else DataRoom.instance?.dao
                    ?.getClanStatsList(tag)
                    ?.let { setStats(it) }
        }
    }

    fun setLoading(loading: Boolean) {
        val size = sdp2px(10)
        if (loading && !this.loading)
            acti?.runOnUiThread {
                loadingAnim.isEnabled = false
                loadingAnim.colorFilter = null
                loadView.visibility = View.VISIBLE
                loadingOp.visibility = View.VISIBLE
                loadingAnim.visibility = View.VISIBLE
                decorate(loadView, "Loading...", size.toFloat())
                loadingAnim.setImageResource(R.drawable.loading)
                ViewDecor.animateView(loadingAnim, true)
                ViewDecor.blinkView(acti!!.getTab(0), true)
            }
        else if (!loading && this.loading) {
            acti?.runOnUiThread {
                ViewDecor.animateView(loadingAnim, false)
                ViewDecor.blinkView(acti!!.getTab(0), false)
                loadingAnim.setImageResource(R.drawable.refresh)
                loadingAnim.setColorFilter(Color.argb(100, 200, 200, 200))
            }

            val timer = Timer()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    val refresh = 15 - (acti?.getLastForce(0) ?: 0)
                    if (refresh < 0) acti?.runOnUiThread {
                        loadView.visibility = View.INVISIBLE
                        loadingOp.visibility = View.INVISIBLE
                        loadingAnim.colorFilter = null
                        loadingAnim.isEnabled = true
                        timer.cancel()
                    } else acti?.runOnUiThread {
                        loadView.visibility = View.VISIBLE
                        decorate(loadView, "     "
                                + refresh + "min", size.toFloat())
                    }
                }
            }, 0, 60000)
        }

        this.loading = loading
    }

    fun setStats(clans: List<ClanStats>) {
        acti?.runOnUiThread { console.visibility = View.GONE }
        if (clans.size == 5)
            clans.sortedWith(SortByPrediction())
                    .forEach { addClan(it) }
        else {
            val info = MagicTextView(acti)
            val more = MagicTextView(acti)
            val params = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            info.layoutParams = params; more.layoutParams = params
            decorate(info, "The clan isn't currently in War\n\n\n\n\n\n\n\n\n", sdp2px(10).toFloat())
            decorate(more, "Stats can't be refreshed in less than 15min", sdp2px(8).toFloat())
            info.textAlignment = View.TEXT_ALIGNMENT_CENTER
            more.textAlignment = View.TEXT_ALIGNMENT_CENTER
            acti?.runOnUiThread {
                lineage?.addView(info)
                lineage?.addView(more)
            }
        }

        setLoading(false)
    }

    fun removeViews() {
        acti?.runOnUiThread { lineage?.removeAllViews() }
    }

    private fun addClan(clan: ClanStats) {
        val predictWins = clan.estimatedWins
        val plusOne = (clan.extraWins.toFloat() * 100).roundToInt()
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
        decorate(name, clan.name, sdp2px(9).toFloat(), Color.WHITE, maxy[0])
        decorate(tag, "#" + clan.tag, sdp2px(6).toFloat(), Color.WHITE, maxy[1])
        decorate(wins, clan.actualWins, sdp2px(9).toFloat())
        decorate(pred, predictWins, sdp2px(9).toFloat())
        decorate(remain, clan.remaining, sdp2px(6).toFloat())
        decorate(crown, clan.crowns, sdp2px(6).toFloat())
        decorate(pone, "$plusOne%", sdp2px(6).toFloat())
        decorate(troph, clan.warTrophies, sdp2px(6).toFloat(),
                Color.rgb(240, 179, 255))
        acti?.resources?.getIdentifier(clan.badge,
                "drawable", acti?.packageName)
                ?.let { badge.setImageResource(it) }
        acti?.runOnUiThread { lineage?.addView(frame) }
    }

    internal inner class SortByPrediction : Comparator<ClanStats> {
        override fun compare(a: ClanStats, b: ClanStats): Int {
            return ((b.estimatedWins + b.extraWins) * 100
                    - (a.estimatedWins + a.extraWins) * 100).toInt()
        }
    }
}