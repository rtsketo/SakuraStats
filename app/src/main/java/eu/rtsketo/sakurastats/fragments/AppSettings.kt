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
import eu.rtsketo.sakurastats.hashmaps.SDPMap
import eu.rtsketo.sakurastats.main.Interface
import eu.rtsketo.sakurastats.main.Service

class AppSettings : Fragment() {
    private val db: DAObject = DataRoom.Companion.getInstance().getDao()
    private val clanName = arrayOfNulls<MagicTextView>(5)
    private val clanBadge = arrayOfNulls<ImageView>(5)
    private val clanEdit = arrayOfNulls<ImageView>(5)
    private val clanSele = arrayOfNulls<ImageView>(5)
    private var acti: Interface = null
    override fun onAttach(context: Context) {
        acti = activity as Interface
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup, savedInstanceState: Bundle): View {
        val frag = inflater.inflate(R.layout.fragment_settings, container, false)
        val clanLabel: MagicTextView = frag.findViewById(R.id.settingsClanLabel)
        clanName[0] = frag.findViewById(R.id.settingsClanName1)
        clanName[1] = frag.findViewById(R.id.settingsClanName2)
        clanName[2] = frag.findViewById(R.id.settingsClanName3)
        clanName[3] = frag.findViewById(R.id.settingsClanName4)
        clanName[4] = frag.findViewById(R.id.settingsClanName5)
        clanBadge[0] = frag.findViewById(R.id.settingsClanBadge1)
        clanBadge[1] = frag.findViewById(R.id.settingsClanBadge2)
        clanBadge[2] = frag.findViewById(R.id.settingsClanBadge3)
        clanBadge[3] = frag.findViewById(R.id.settingsClanBadge4)
        clanBadge[4] = frag.findViewById(R.id.settingsClanBadge5)
        clanEdit[0] = frag.findViewById(R.id.settingsClanEdit1)
        clanEdit[1] = frag.findViewById(R.id.settingsClanEdit2)
        clanEdit[2] = frag.findViewById(R.id.settingsClanEdit3)
        clanEdit[3] = frag.findViewById(R.id.settingsClanEdit4)
        clanEdit[4] = frag.findViewById(R.id.settingsClanEdit5)
        clanSele[0] = frag.findViewById(R.id.settingsClanSelect1)
        clanSele[1] = frag.findViewById(R.id.settingsClanSelect2)
        clanSele[2] = frag.findViewById(R.id.settingsClanSelect3)
        clanSele[3] = frag.findViewById(R.id.settingsClanSelect4)
        clanSele[4] = frag.findViewById(R.id.settingsClanSelect5)
        val size: Int = SDPMap.Companion.sdp2px(7)
        decorate(clanLabel, "Stored Clans", SDPMap.Companion.sdp2px(9).toFloat())
        decorate(frag.findViewById(R.id.settingsLegend), "Legend", SDPMap.Companion.sdp2px(9).toFloat())
        decorate(frag.findViewById(R.id.settingsSupport), "Support", SDPMap.Companion.sdp2px(9).toFloat())
        decorate(frag.findViewById(R.id.settingsLegend1), "The current number of won battles.", size.toFloat())
        decorate(frag.findViewById(R.id.settingsLegend2), "The estimated battles that might be won in this war, based on the statistics of each player.", size.toFloat())
        decorate(frag.findViewById(R.id.settingsLegend3), "A score based on the current levels of the cards each player has. From zero to over 9000!", size.toFloat())
        decorate(frag.findViewById(R.id.settingsLegend4), "Normalized win ratio of final battles of each player. Everyone starts at 50%.", size.toFloat())
        decorate(frag.findViewById(R.id.settingsLegend5), "The chances of the clan getting one or more wins than the estimated.", size.toFloat())
        decorate(frag.findViewById(R.id.settingsLegend6), "The number of wars the player has participated in.", size.toFloat())
        decorate(frag.findViewById(R.id.settingsLegend7), "The number of final battles the player has missed.", size.toFloat())
        decorate(frag.findViewById(R.id.settingsLegend8), "The best war chest a player has acquired in this season.", size.toFloat())
        decorate(frag.findViewById(R.id.settingsLegend9), "The actual win ratio of a player. It starts as 0%.", size.toFloat())
        decorate(frag.findViewById(R.id.settingsSupportText), "Suggest new ideas!\n\nRequest a feature or\nreport any issue to GitHub.", size.toFloat())
        refreshStored()
        for (c in 0..4) {
            clanSele[c].setOnClickListener(View.OnClickListener { view: View ->
                ViewDecor.bounce(view, acti)
                selectClan(c)
            })
            clanEdit[c].setOnClickListener(View.OnClickListener { view: View -> DialogView(SakuraDialog.INPUT, c, acti) })
        }
        frag.findViewById<View>(R.id.settingsSupportGitHub)
                .setOnClickListener { v: View -> reportIssue(v.context) }
        return frag
    }

    fun selectClan(c: Int) {
        acti.runOnUiThread { setSelect(false) }
        val cTag = acti.getStoredClan(c)
        if (cTag != null) {
            Service.Companion.getThread().start(cTag, false, true)
            acti.setLastClan(cTag)
        }
    }

    private fun setSelect(sele: Boolean) {
        for (c in 0..4) {
            if (acti.getStoredClan(c) != null) clanSele[c].isEnabled = sele
            if (sele) clanSele[c].colorFilter = null else clanSele[c].setColorFilter(Color.argb(
                    100, 200, 200, 200))
        }
    }

    @JvmOverloads
    fun refreshStored(c: Int, button: Boolean = true) {
        if (acti.getStoredClan(c) != null) ThreadPool.cachePool.execute {
            val size: Int = SDPMap.Companion.sdp2px(9)
            val clan = db.getClanStats(
                    acti.getStoredClan(c))
            acti.runOnUiThread {
                if (button) {
                    clanSele[c].isEnabled = true
                    clanSele[c].colorFilter = null
                }
                if (clan != null) {
                    decorate(clanName[c], clan.name, size.toFloat())
                    clanBadge[c].setImageResource(
                            acti.resources
                                    .getIdentifier(clan.badge,
                                            "drawable", acti
                                            .getPackageName()))
                } else {
                    decorate(clanName[c], "#" +
                            acti.getStoredClan(c), size.toFloat(), Color.LTGRAY)
                    clanBadge[c].setImageResource(R.drawable.no_clan)
                }
            }
        } else {
            acti.runOnUiThread {
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
                .putExtraInfo("Clan_Tag_1", acti.getStoredClan(0))
                .putExtraInfo("Clan_Tag_2", acti.getStoredClan(1))
                .putExtraInfo("Clan_Tag_3", acti.getStoredClan(2))
                .putExtraInfo("Clan_Tag_4", acti.getStoredClan(3))
                .putExtraInfo("Clan_Tag_5", acti.getStoredClan(4))
                .guestEmailRequired(true)
                .minDescriptionLength(20)
                .homeAsUpEnabled(true)
                .guestToken(APIDevKey.gitKey)
                .launch(context)
    }
}