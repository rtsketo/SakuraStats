package eu.rtsketo.sakurastats.main

import android.content.SharedPreferences
import android.os.Bundle
import android.os.SystemClock
import android.preference.PreferenceManager
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.qwerjk.better_text.MagicTextView
import eu.rtsketo.sakurastats.R
import eu.rtsketo.sakurastats.control.DataRoom
import eu.rtsketo.sakurastats.control.DialogView
import eu.rtsketo.sakurastats.control.DialogView.SakuraDialog
import eu.rtsketo.sakurastats.control.ThreadPool
import eu.rtsketo.sakurastats.control.ViewDecor
import eu.rtsketo.sakurastats.fragments.AppSettings
import eu.rtsketo.sakurastats.fragments.PlayerActivity
import eu.rtsketo.sakurastats.fragments.Prognostics
import eu.rtsketo.sakurastats.fragments.WarStatistics
import eu.rtsketo.sakurastats.hashmaps.PlayerMap
import eu.rtsketo.sakurastats.hashmaps.SDPMap
import java.util.*

class Interface : AppCompatActivity() {
    private var progTab: ImageView? = null
    private var warTab: ImageView? = null
    private var actiTab: ImageView? = null
    private var settiTab: ImageView? = null
    private val tab = arrayOfNulls<MagicTextView>(4)
    private var preferences: SharedPreferences? = null
    private var actiFrag: PlayerActivity? = null
    private var warFrag: WarStatistics? = null
    private var settiFrag: AppSettings? = null
    private var progFrag: Prognostics? = null
    private var mViewPager: ViewPager? = null

    inner class SectionsPagerAdapter internal constructor(fm: FragmentManager?) : FragmentPagerAdapter(fm) {
        override fun getCount(): Int {
            return 4
        }

        override fun getItem(pos: Int): Fragment {
            return when (pos) {
                0 -> Prognostics()
                1 -> WarStatistics()
                2 -> PlayerActivity()
                3 -> AppSettings()
                else -> null
            }
        }
    }

    fun changeTabTo(num: Int) {
        mViewPager!!.currentItem = num
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        preferences = PreferenceManager.getDefaultSharedPreferences(baseContext)
        setContentView(R.layout.activity_interface)
        mViewPager = findViewById(R.id.viewPager)
        mViewPager.setAdapter(mSectionsPagerAdapter)
        mViewPager.addOnPageChangeListener(pageChangeListener)
        mViewPager.setOffscreenPageLimit(4)
        val actionBar = supportActionBar
        actionBar?.hide()
        ViewDecor.init(resources)
        SDPMap.Companion.init(resources)
        PlayerMap.Companion.init(this)
        DataRoom.Companion.init(this)
        initTabs()
        startApp()
        super.onCreate(savedInstanceState)
    }

    override fun onAttachFragment(frag: Fragment) {
        if (frag is PlayerActivity) actiFrag = frag else if (frag is WarStatistics) warFrag = frag else if (frag is AppSettings) settiFrag = frag else if (frag is Prognostics) progFrag = frag
        super.onAttachFragment(frag)
    }

    private fun startApp() {
        ThreadPool.getCachePool().execute {
            if (lastClan == null) runOnUiThread {
                DialogView(
                        SakuraDialog.INPUT, this)
            } else Service.Companion.getThread(this)!!.start(
                    lastClan, false, true)
        }
    }

    fun getTab(c: Int): ImageView? {
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
        progTab = findViewById(R.id.progTab)
        warTab = findViewById(R.id.warTab)
        actiTab = findViewById(R.id.actiTab)
        settiTab = findViewById(R.id.settiTab)
        for (c in 0..3) {
            getTab(c)!!.setOnClickListener { view: View? -> changeTabTo(c) }
        }
        tab[0] = findViewById(R.id.tab1)
        tab[1] = findViewById(R.id.tab2)
        tab[2] = findViewById(R.id.tab3)
        tab[3] = findViewById(R.id.tab4)
        val size: Int = SDPMap.Companion.sdp2px(7)
        decorate(tab[0], "Forecast", size.toFloat())
        decorate(tab[1], "Analytics", size.toFloat())
        decorate(tab[2], "Activity", size.toFloat())
        decorate(tab[3], "Settings", size.toFloat())
    }

    fun setLastUse(tag: String?) {
        setLastUse(tag, "")
    }

    fun getLastUse(tag: String?): Boolean {
        return getLastUse(tag, "")
    }

    fun setLastForce(tab: Int) {
        setLastUse("tab", getTabName(tab))
    }

    var lastClan: String?
        get() = preferences!!.getString("ClanTag", getStoredClan(0))
        set(tag) {
            val editor = preferences!!.edit()
            editor.putString("ClanTag", tag)
            editor.apply()
        }

    fun setLastUse(tag: String?, mod: String) {
        val editor = preferences!!.edit()
        editor.putLong(mod + tag, System.currentTimeMillis())
        editor.apply()
    }

    fun getLastUse(tag: String?, mod: String): Boolean {
        val curr = System.currentTimeMillis()
        val time = preferences!!.getLong(mod + tag, 0)
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
        val time = preferences!!.getLong(getTabName(tab) + "tab", 0)
        val diff = (curr - time) / 1000 / 60
        return diff.toInt()
    }

    val useCount: Int
        get() {
            val uc = preferences!!.getInt("useCount", 0)
            if (uc == 20 || uc == 150 || uc == 500) runOnUiThread {
                DialogView(
                        SakuraDialog.RATEQUEST, this)
            }
            return uc
        }

    fun incUseCount() {
        val editor = preferences!!.edit()
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

    fun setStoredClan(index: Int, tag: String?) {
        val editor = preferences!!.edit()
        editor.putString("StoredClan$index", tag)
        editor.apply()
    }

    fun getStoredClan(index: Int): String? {
        return preferences!!.getString("StoredClan$index", null)
    }

    private fun changeTab(num: Int) {
        for (tabetto in tab) tabetto!!.visibility = TextView.GONE
        tab[num]!!.visibility = TextView.VISIBLE
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
                if (warFrag.getLoading()) warFrag.getWifi().visibility = View.VISIBLE
                if (actiFrag.getLoading()) actiFrag.getWifi().visibility = View.VISIBLE
                progFrag.getWifi().first.visibility = View.VISIBLE
                progFrag.getWifi().second.visibility = View.VISIBLE
            }
            val timer = Timer()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    runOnUiThread {
                        warFrag.getWifi().visibility = View.INVISIBLE
                        actiFrag.getWifi().visibility = View.INVISIBLE
                        progFrag.getWifi().first.visibility = View.INVISIBLE
                        progFrag.getWifi().second.visibility = View.INVISIBLE
                    }
                }
            }, 1500)
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    fun getWarFrag(): WarStatistics {
        while (warFrag == null) SystemClock.sleep(500)
        waitForView(warFrag!!)
        return warFrag
    }

    fun getActiFrag(): PlayerActivity {
        while (actiFrag == null) SystemClock.sleep(500)
        waitForView(actiFrag!!)
        return actiFrag
    }

    fun getProgFrag(): Prognostics {
        while (progFrag == null) SystemClock.sleep(500)
        waitForView(progFrag!!)
        return progFrag
    }

    fun getSettiFrag(): AppSettings {
        while (settiFrag == null) SystemClock.sleep(500)
        waitForView(settiFrag!!)
        return settiFrag
    }

    private fun waitForView(frag: Fragment) {
        while (frag.view == null) SystemClock.sleep(500)
    }

    companion object {
        const val TAG = "eu.rtsketo.sakurastats"
        private const val SECS = 1000
        private const val MINS = 60 * SECS
        private const val HRS = 60 * MINS
    }
}