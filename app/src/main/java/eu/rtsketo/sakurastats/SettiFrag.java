package eu.rtsketo.sakurastats;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.qwerjk.better_text.MagicTextView;

import static eu.rtsketo.sakurastats.DBControl.getDB;
import static eu.rtsketo.sakurastats.Interface.getStoredClan;
import static eu.rtsketo.sakurastats.Interface.incUseCount;
import static eu.rtsketo.sakurastats.Interface.setLastClan;
import static eu.rtsketo.sakurastats.Statics.bounce;
import static eu.rtsketo.sakurastats.Statics.createDialog;
import static eu.rtsketo.sakurastats.Statics.decorate;
import static eu.rtsketo.sakurastats.Statics.getActi;
import static eu.rtsketo.sakurastats.Statics.getBackThread;
import static eu.rtsketo.sakurastats.Statics.getCachePool;
import static eu.rtsketo.sakurastats.Statics.sdp2px;

public class SettiFrag extends Fragment {
    public SettiFrag() {}
    private MagicTextView clanLabel;
    private MagicTextView clanName[] = new MagicTextView[5];
    private ImageView clanBadge[] = new ImageView[5];
    private ImageView clanEdit[] = new ImageView[5];
    private ImageView clanSele[] = new ImageView[5];
    private GeneralDao db = getDB().getDao();
    private Thread thread;

    @Override public View onCreateView(LayoutInflater inflater,
                                       ViewGroup container, final Bundle savedInstanceState) {
        View frag = inflater.inflate(R.layout.fragment_settings, container, false);
        clanLabel = frag.findViewById(R.id.settingsClanLabel);
        clanName[0] = frag.findViewById(R.id.settingsClanName1);
        clanName[1] = frag.findViewById(R.id.settingsClanName2);
        clanName[2] = frag.findViewById(R.id.settingsClanName3);
        clanName[3] = frag.findViewById(R.id.settingsClanName4);
        clanName[4] = frag.findViewById(R.id.settingsClanName5);
        clanBadge[0] = frag.findViewById(R.id.settingsClanBadge1);
        clanBadge[1] = frag.findViewById(R.id.settingsClanBadge2);
        clanBadge[2] = frag.findViewById(R.id.settingsClanBadge3);
        clanBadge[3] = frag.findViewById(R.id.settingsClanBadge4);
        clanBadge[4] = frag.findViewById(R.id.settingsClanBadge5);
        clanEdit[0] = frag.findViewById(R.id.settingsClanEdit1);
        clanEdit[1] = frag.findViewById(R.id.settingsClanEdit2);
        clanEdit[2] = frag.findViewById(R.id.settingsClanEdit3);
        clanEdit[3] = frag.findViewById(R.id.settingsClanEdit4);
        clanEdit[4] = frag.findViewById(R.id.settingsClanEdit5);
        clanSele[0] = frag.findViewById(R.id.settingsClanSelect1);
        clanSele[1] = frag.findViewById(R.id.settingsClanSelect2);
        clanSele[2] = frag.findViewById(R.id.settingsClanSelect3);
        clanSele[3] = frag.findViewById(R.id.settingsClanSelect4);
        clanSele[4] = frag.findViewById(R.id.settingsClanSelect5);

        int size = sdp2px(7);
        decorate(clanLabel, "Stored Clans", sdp2px(9));
        decorate((MagicTextView) frag.findViewById(R.id.settingsLegend), "Legend", sdp2px(9));
        decorate((MagicTextView) frag.findViewById(R.id.settingsLegend1), "The current number of won battles.", size);
        decorate((MagicTextView) frag.findViewById(R.id.settingsLegend2), "The estimated number of battles that might be won in this war, based on the statistics of each player.", size);
        decorate((MagicTextView) frag.findViewById(R.id.settingsLegend3), "A score based on the current levels of the cards each player has. From zero to over 9000!", size);
        decorate((MagicTextView) frag.findViewById(R.id.settingsLegend4), "Normalized win ratio of final battles of each player. Everyone starts at a 50% chance of winning a battle, it becomes more accurate with time.", size);
        decorate((MagicTextView) frag.findViewById(R.id.settingsLegend5), "The chances of getting one or more wins than the estimated number.", size);
        decorate((MagicTextView) frag.findViewById(R.id.settingsLegend6), "The number of stored wars the player has participated in.", size);
        decorate((MagicTextView) frag.findViewById(R.id.settingsLegend7), "The number of stored final battles the player has missed.", size);
        decorate((MagicTextView) frag.findViewById(R.id.settingsLegend8), "The average number of cards the player collects per war.", size);
        decorate((MagicTextView) frag.findViewById(R.id.settingsLegend9), "The actual win ratio of player's stored battles.", size);

        refreshStored();
        for (int c = 0; c < 5; c++) {
            final int finalC = c;
            clanSele[c].setOnClickListener(new View.OnClickListener(){
                @Override public void onClick(View view) {
                    bounce(view); selectClan(finalC); }});

            clanEdit[c].setOnClickListener(new View.OnClickListener(){
                @Override public void onClick(View view) {
                    createDialog(Statics.SakuraDialog.INPUT, finalC); }});}
        return frag;
    }

    public void selectClan(final int finalC) {
        getActi().runOnUiThread(new Runnable() {
            @Override public void run() {
                setSelect(false);
                if (getStoredClan(finalC) != null)
                    if (thread == null) {
                        thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                getBackThread().setClan(getStoredClan(finalC));
                                setLastClan(getStoredClan(finalC));
                                getBackThread().startThread();
                                incUseCount();
                                thread = null; }
                        });
                        thread.start(); }}});
    }


    private void setSelect(boolean sele) {
        for (int c = 0; c < 5; c++) {
            if (sele && getStoredClan(c) != null) {
                clanSele[c].setEnabled(sele);
                clanSele[c].setColorFilter(null);
            } else {
                clanSele[c].setEnabled(false);
                clanSele[c].setColorFilter(Color.argb(
                        100, 200, 200, 200));
            }
        }
    }


    public void refreshStored(final int c) { refreshStored(c, true); }
    public void refreshStored(final int c, final boolean button) {
        if(getStoredClan(c) != null)
            getCachePool().execute(new Runnable() {
                @Override public void run() {
                    final int size = sdp2px(9);
                    final ClanStats clan = db.getClanStats(getStoredClan(c));
                    getActi().runOnUiThread(new Runnable() {
                        @Override public void run() {
                            if (button) {
                                clanSele[c].setEnabled(true);
                                clanSele[c].setColorFilter(null); }

                            if (clan != null) {
                                decorate(clanName[c], clan.getName(), size);
                                clanBadge[c].setImageResource(
                                        getActi().getResources()
                                                .getIdentifier(clan.getBadge(),
                                                        "drawable", getActi()
                                                                .getPackageName()));}
                            else {
                                decorate(clanName[c], "#" +
                                        getStoredClan(c), size, Color.LTGRAY);
                                clanBadge[c].setImageResource(R.drawable.no_clan);}}});}});
        else {
            getActi().runOnUiThread(new Runnable() {
                @Override public void run() {
                    clanSele[c].setColorFilter(Color.argb(100,200,200,200));
                    clanSele[c].setEnabled(false);
            if (c<2) decorate(clanName[c], "Edit to Add a Clan", sdp2px(8), Color.LTGRAY);
            else decorate(clanName[c], "Not yet Available!", sdp2px(6), Color.GRAY); }});
        }
//        "Available in Donation Version!"
    }

    public void refreshStored() {
        for (int c = 0; c < 2; c++)
            refreshStored(c); }
}
