package eu.rtsketo.sakurastats.main

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.os.SystemClock.sleep
import android.view.View.*
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.qwerjk.better_text.MagicTextView
import eu.rtsketo.sakurastats.R
import eu.rtsketo.sakurastats.control.DataRoom
import eu.rtsketo.sakurastats.control.DialogView
import eu.rtsketo.sakurastats.control.DialogView.SakuraDialog
import eu.rtsketo.sakurastats.control.ThreadPool
import eu.rtsketo.sakurastats.control.ViewDecor
import eu.rtsketo.sakurastats.control.ViewDecor.decorate
import eu.rtsketo.sakurastats.fragments.AppSettings
import eu.rtsketo.sakurastats.fragments.PlayerActivity
import eu.rtsketo.sakurastats.fragments.Prognostics
import eu.rtsketo.sakurastats.fragments.WarStatistics
import eu.rtsketo.sakurastats.hashmaps.PlayerMap
import eu.rtsketo.sakurastats.hashmaps.SDPMap
import eu.rtsketo.sakurastats.hashmaps.SDPMap.Companion.sdp2px
import kotlinx.android.synthetic.main.activity_interface.*
import kotlinx.android.synthetic.main.fragment_prognose.*
import java.util.*

class Interface : AppCompatActivity() {
    private lateinit var preferences: SharedPreferences
    private val tab = arrayListOf<MagicTextView>()
    private var actiFrag: PlayerActivity? = null
    private var warFrag: WarStatistics? = null
    private var settiFrag: AppSettings? = null
    private var progFrag: Prognostics? = null

    inner class SectionsPagerAdapter internal constructor(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        override fun getCount() = 4
        override fun getItem(pos: Int): Fragment {
            return when (pos) {
                0 -> Prognostics()
                1 -> WarStatistics()
                2 -> PlayerActivity()
                else -> AppSettings()
            }
        }
    }

    fun changeTabTo(num: Int) {
        viewPager.currentItem = num
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        preferences = getPreferences(Activity.MODE_PRIVATE)
        setContentView(R.layout.activity_interface)
        viewPager.adapter = mSectionsPagerAdapter
        viewPager.addOnPageChangeListener(pageChangeListener)
        viewPager.offscreenPageLimit = 4
        supportActionBar?.hide()
        ViewDecor.init(resources)
        SDPMap.init(resources)
        PlayerMap.init(this)
        DataRoom.init(this)
        initTabs()
        startApp()
        super.onCreate(savedInstanceState)
    }

    override fun onAttachFragment(frag: Fragment) {
        when (frag) {
            is PlayerActivity -> actiFrag = frag
            is WarStatistics -> warFrag = frag
            is AppSettings -> settiFrag = frag
            is Prognostics -> progFrag = frag
        }
        super.onAttachFragment(frag)
    }

    private fun startApp() {
        ThreadPool.cachePool.execute {
            if (lastClan.isEmpty())
                runOnUiThread {
                    DialogView(SakuraDialog.INPUT, this)
            } else Service.getThread(this)
                    .start(lastClan, force = false, tab = true)
        }
    }

    fun getTab(c: Int): ImageView {
        return when (c) {
            0 -> progTab
            1 -> warTab
            2 -> actiTab
            3 -> settiTab
            else -> throw NullPointerException(
                    "Invalid number passed in getTab().")
        }
    }

    private fun initTabs() {
        for (c in 0..3) getTab(c)
                .setOnClickListener { changeTabTo(c) }
        tab.add(tab1); tab.add(tab2)
        tab.add(tab3); tab.add(tab4)

        val size = sdp2px(7)
        decorate(tab[0], "Forecast", size.toFloat())
        decorate(tab[1], "Analytics", size.toFloat())
        decorate(tab[2], "Activity", size.toFloat())
        decorate(tab[3], "Settings", size.toFloat())
    }

    fun setLastUse(tag: String) {
        setLastUse(tag, "")
    }

    fun getLastUse(tag: String): Boolean {
        return getLastUse(tag, "")
    }

    fun setLastForce(tab: Int) {
        setLastUse("tab", getTabName(tab))
    }

    var lastClan: String
        get() = preferences.getString(
                "ClanTag",
                getStoredClan(0)) ?: ""
        set(tag) {
            val editor
                    = preferences.edit()
            editor.putString("ClanTag", tag)
            editor.apply()
        }

    fun setLastUse(tag: String, mod: String) {
        val editor = preferences.edit()
        editor.putLong(mod + tag, System.currentTimeMillis())
        editor.apply()
    }

    fun getLastUse(tag: String, mod: String): Boolean {
        val curr = System.currentTimeMillis()
        val time = preferences
                .getLong(mod + tag, 0)
        val diff = curr - time
        return when (mod) {
            "batt", "chest" -> diff > 24 * HRS
            "prof" -> diff > 72 * HRS
            "auto", "wstat" -> diff > 48 * HRS
            "prog" -> diff > 15 * MINS
            "acti", "war" -> diff > 60 * MINS
            else -> diff > 15 * MINS
        }
    }

    fun getLastForce(tab: Int): Int {
        val curr = System.currentTimeMillis()
        val time = preferences.getLong(
                getTabName(tab) + "tab", 0)
        val diff = (curr - time) / 1000 / 60
        return diff.toInt()
    }

    private val useCount: Int
        get() {
            val uc = preferences.getInt("useCount", 0)
            if (uc == 20 || uc == 150 || uc == 500) runOnUiThread {
                DialogView(SakuraDialog.RATEQUEST, this) }
            return uc
        }

    fun incUseCount() {
        val editor
                = preferences.edit()
        editor.putInt("useCount", useCount + 1)
        editor.apply()
    }

    private fun getTabName(tab: Int): String {
        return when (tab) {
            0 -> "prog"
            1 -> "war"
            else -> "acti"
        }
    }

    fun setStoredClan(index: Int, tag: String) {
        val editor
                = preferences.edit()
        editor.putString("StoredClan$index", tag)
        editor.apply()
    }

    fun getStoredClan(index: Int): String {
        return preferences.getString(
                "StoredClan$index",
                "") ?: ""
    }

    private fun changeTab(num: Int) {
        for (tabetto in tab)
            tabetto.visibility = GONE
        tab[num].visibility = VISIBLE
    }

    var pageChangeListener: OnPageChangeListener = object : OnPageChangeListener {
        override fun onPageScrolled(i: Int, v: Float, i1: Int) {}
        override fun onPageSelected(i: Int) {
            changeTab(i)
        }

        override fun onPageScrollStateChanged(i: Int) {}
    }

    fun badConnection() {
        if (warFrag != null && actiFrag != null && progFrag != null) {
            runOnUiThread {
                if (warFrag?.loading == true)
                    warFrag?.wifi?.visibility = VISIBLE
                if (actiFrag?.loading == true)
                    actiFrag?.wifi?.visibility = VISIBLE
                progFrag?.wifi?.visibility = VISIBLE
                progFrag?.wifiOp?.visibility = VISIBLE
            }
            val timer = Timer()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    runOnUiThread {
                        warFrag?.wifi?.visibility = INVISIBLE
                        actiFrag?.wifi?.visibility = INVISIBLE
                        progFrag?.wifi?.visibility = INVISIBLE
                        progFrag?.wifiOp?.visibility = INVISIBLE
                    }
                }
            }, 1500)
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    fun getWarFrag(): WarStatistics {
        while (warFrag == null) sleep(50)
        waitForView(warFrag!!)
        return warFrag as WarStatistics
    }

    fun getActiFrag(): PlayerActivity {
        while (actiFrag == null) sleep(50)
        waitForView(actiFrag!!)
        return actiFrag as PlayerActivity
    }

    fun getProgFrag(): Prognostics {
        while (progFrag == null) sleep(50)
        waitForView(progFrag!!)
        return progFrag as Prognostics
    }

    fun getSettiFrag(): AppSettings {
        while (settiFrag == null) sleep(500)
        waitForView(settiFrag!!)
        return settiFrag as AppSettings
    }

    private fun waitForView(frag: Fragment) {
        while (frag.view == null) sleep(500)
    }

    companion object {
        const val TAG = "eu.rtsketo.sakurastats"
        private const val SECS = 1000
        private const val MINS = 60 * SECS
        private const val HRS = 60 * MINS
    }
}