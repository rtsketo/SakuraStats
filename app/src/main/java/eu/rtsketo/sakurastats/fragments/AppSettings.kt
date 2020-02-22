package eu.rtsketo.sakurastats.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.heinrichreimersoftware.androidissuereporter.IssueReporterLauncher
import com.qwerjk.better_text.MagicTextView
import eu.rtsketo.sakurastats.R
import eu.rtsketo.sakurastats.control.*
import eu.rtsketo.sakurastats.control.DialogView.SakuraDialog
import eu.rtsketo.sakurastats.control.ViewDecor.decorate
import eu.rtsketo.sakurastats.hashmaps.SDPMap
import eu.rtsketo.sakurastats.main.Interface
import eu.rtsketo.sakurastats.main.Service
import kotlinx.android.synthetic.main.fragment_settings.view.*

class AppSettings : Fragment() {
    private val db= DataRoom.instance?.dao
    private val clanName = arrayListOf<MagicTextView>()
    private val clanBadge = arrayListOf<ImageView>()
    private val clanEdit = arrayListOf<ImageView>()
    private val clanSele = arrayListOf<ImageView>()
    private var acti: Interface? = null
    override fun onAttach(context: Context) {
        acti = activity as Interface
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val frag = inflater.inflate(R.layout.fragment_settings, container, false)
        frag.settingsClanName1?.let { clanName.add(it) }
        frag.settingsClanName2?.let { clanName.add(it) }
        frag.settingsClanName3?.let { clanName.add(it) }
        frag.settingsClanName4?.let { clanName.add(it) }
        frag.settingsClanName5?.let { clanName.add(it) }
        frag.settingsClanBadge1?.let { clanBadge.add(it) }
        frag.settingsClanBadge2?.let { clanBadge.add(it) }
        frag.settingsClanBadge3?.let { clanBadge.add(it) }
        frag.settingsClanBadge4?.let { clanBadge.add(it) }
        frag.settingsClanBadge5?.let { clanBadge.add(it) }
        frag.settingsClanEdit1?.let { clanEdit.add(it) }
        frag.settingsClanEdit2?.let { clanEdit.add(it) }
        frag.settingsClanEdit3?.let { clanEdit.add(it) }
        frag.settingsClanEdit4?.let { clanEdit.add(it) }
        frag.settingsClanEdit5?.let { clanEdit.add(it) }
        frag.settingsClanSelect1?.let { clanSele.add(it) }
        frag.settingsClanSelect2?.let { clanSele.add(it) }
        frag.settingsClanSelect3?.let { clanSele.add(it) }
        frag.settingsClanSelect4?.let { clanSele.add(it) }
        frag.settingsClanSelect5?.let { clanSele.add(it) }

        val size: Int = SDPMap.sdp2px(7)
        frag.settingsClanLabel?.let { decorate(it, "Stored Clans", SDPMap.Companion.sdp2px(9).toFloat()) }
        frag.settingsLegend?.let { decorate(it, "Legend", SDPMap.Companion.sdp2px(9).toFloat()) }
        frag.settingsSupport?.let { decorate(it, "Support", SDPMap.Companion.sdp2px(9).toFloat()) }
        frag.settingsLegend1?.let { decorate(it, "The current number of won battles.", size.toFloat()) }
        frag.settingsLegend2?.let { decorate(it, "The estimated battles that might be won in this war, based on the statistics of each player.", size.toFloat()) }
        frag.settingsLegend3?.let { decorate(it, "A score based on the current levels of the cards each player has. From zero to over 9000!", size.toFloat()) }
        frag.settingsLegend4?.let { decorate(it, "Normalized win ratio of final battles of each player. Everyone starts at 50%.", size.toFloat()) }
        frag.settingsLegend5?.let { decorate(it, "The chances of the clan getting one or more wins than the estimated.", size.toFloat()) }
        frag.settingsLegend6?.let { decorate(it, "The number of wars the player has participated in.", size.toFloat()) }
        frag.settingsLegend7?.let { decorate(it, "The number of final battles the player has missed.", size.toFloat()) }
        frag.settingsLegend8?.let { decorate(it, "The best war chest a player has acquired in this season.", size.toFloat()) }
        frag.settingsLegend9?.let { decorate(it, "The actual win ratio of a player. It starts as 0%.", size.toFloat()) }
        frag.settingsSupportText?.let { decorate(it, "Suggest new ideas!\n\nRequest a feature or\nreport any issue to GitHub.", size.toFloat()) }
        refreshStored()
        for (c in 0..4) {
            clanSele[c].setOnClickListener { view ->
                acti?.apply {
                    ViewDecor.bounce(view, acti!!) }
                selectClan(c)
            }
            clanEdit[c].setOnClickListener {
                DialogView(SakuraDialog.INPUT, c, acti as Interface) }
        }

        frag.settingsSupportGitHub
                ?.setOnClickListener { v: View -> reportIssue(v.context) }
        return frag
    }

    fun selectClan(c: Int) {
        acti?.runOnUiThread { setSelect(false) }
        val cTag = acti?.getStoredClan(c) ?: ""
        if (cTag.isNotEmpty()) {
            Service.getThread().start(cTag, force = false, tab = true)
            acti?.lastClan = cTag
        }
    }

    private fun setSelect(sele: Boolean) {
        for (c in 0..4) {
            if (acti?.getStoredClan(c) != null) clanSele[c].isEnabled = sele
            if (sele) clanSele[c].colorFilter = null else clanSele[c].setColorFilter(Color.argb(
                    100, 200, 200, 200))
        }
    }

    @JvmOverloads
    fun refreshStored(c: Int, button: Boolean = true) {
        if (acti?.getStoredClan(c) != null)
            ThreadPool.cachePool.execute {
                val size: Int = SDPMap.Companion.sdp2px(9)
                val clan = db?.getClanStats(
                        acti?.getStoredClan(c) ?: "")

                acti?.runOnUiThread {
                if (button) {
                    clanSele[c].isEnabled = true
                    clanSele[c].colorFilter = null
                }
                if (clan != null) {
                    decorate(clanName[c], clan.name, size.toFloat())
                    acti?.resources?.getIdentifier(clan.badge,
                                    "drawable", acti?.packageName)
                            ?.let { clanBadge[c].setImageResource(it) }
                } else {
                    decorate(clanName[c], "#" +
                            acti?.getStoredClan(c), size.toFloat(), Color.LTGRAY)
                    clanBadge[c].setImageResource(R.drawable.no_clan)
                }
            }
        } else {
            acti?.runOnUiThread {
                clanSele[c].setColorFilter(Color.argb(100, 200, 200, 200))
                clanSele[c].isEnabled = false
                if (c < 5) decorate(clanName[c], "Edit to Add a Clan", SDPMap.Companion.sdp2px(8).toFloat(), Color.LTGRAY) else decorate(clanName[c], "Not yet Available!", SDPMap.Companion.sdp2px(6).toFloat(), Color.GRAY)
            }
        }
    }

    fun refreshStored() {
        for (c in 0..4) refreshStored(c)
    }

    fun reportIssue(context: Context) {
        IssueReporterLauncher.forTarget("rtsketo", "SakuraStats")
                .putExtraInfo("Clan_Tag_1", acti?.getStoredClan(0))
                .putExtraInfo("Clan_Tag_2", acti?.getStoredClan(1))
                .putExtraInfo("Clan_Tag_3", acti?.getStoredClan(2))
                .putExtraInfo("Clan_Tag_4", acti?.getStoredClan(3))
                .putExtraInfo("Clan_Tag_5", acti?.getStoredClan(4))
                .guestEmailRequired(true)
                .minDescriptionLength(20)
                .homeAsUpEnabled(true)
                .guestToken(APIDevKey.gitKey)
                .launch(context)
    }

    companion object {
        val instance = AppSettings()
    }
}