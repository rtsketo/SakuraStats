package eu.rtsketo.sakurastats.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.qwerjk.better_text.MagicTextView;

import eu.rtsketo.sakurastats.R;
import eu.rtsketo.sakurastats.control.DAObject;
import eu.rtsketo.sakurastats.control.DataRoom;
import eu.rtsketo.sakurastats.control.DialogView;
import eu.rtsketo.sakurastats.dbobjects.ClanStats;
import eu.rtsketo.sakurastats.main.Interface;
import eu.rtsketo.sakurastats.main.Service;

import static eu.rtsketo.sakurastats.control.ThreadPool.getCachePool;
import static eu.rtsketo.sakurastats.control.ViewDecor.bounce;
import static eu.rtsketo.sakurastats.control.ViewDecor.decorate;
import static eu.rtsketo.sakurastats.hashmaps.SDPMap.sdp2px;

public class AppSettings extends Fragment {
    public AppSettings() { /* Needed for FragmentManager */ }
    private DAObject db = DataRoom.getInstance().getDao();
    private MagicTextView[] clanName = new MagicTextView[5];
    private ImageView[] clanBadge = new ImageView[5];
    private ImageView[] clanEdit = new ImageView[5];
    private ImageView[] clanSele = new ImageView[5];
    private View.OnClickListener urlOpener;
    private Interface acti;

    @Override
    public void onAttach(Context context) {
        acti = (Interface) getActivity();
        super.onAttach(context);
    }


    @Override
    public View onCreateView(LayoutInflater inflater,
                                       ViewGroup container, final Bundle savedInstanceState) {
        View frag = inflater.inflate(R.layout.fragment_settings, container, false);
        MagicTextView clanLabel = frag.findViewById(R.id.settingsClanLabel);
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
        decorate(frag.findViewById(R.id.settingsLegend), "Legend", sdp2px(9));
        decorate(frag.findViewById(R.id.settingsSupport), "Support", sdp2px(9));
        decorate(frag.findViewById(R.id.settingsLegend1), "The current number of won battles.", size);
        decorate(frag.findViewById(R.id.settingsLegend2), "The estimated number of battles that might be won in this war, based on the statistics of each player.", size);
        decorate(frag.findViewById(R.id.settingsLegend3), "A score based on the current levels of the cards each player has. From zero to over 9000!", size);
        decorate(frag.findViewById(R.id.settingsLegend4), "Normalized win ratio of final battles of each player. Everyone starts at a 50% chance of winning a battle, it becomes more accurate with time.", size);
        decorate(frag.findViewById(R.id.settingsLegend5), "The chances of getting one or more wins than the estimated number.", size);
        decorate(frag.findViewById(R.id.settingsLegend6), "The number of stored wars the player has participated in.", size);
        decorate(frag.findViewById(R.id.settingsLegend7), "The number of stored final battles the player has missed.", size);
        decorate(frag.findViewById(R.id.settingsLegend8), "The average number of cards the player collects per war.", size);
        decorate(frag.findViewById(R.id.settingsLegend9), "The actual win ratio of player's stored battles.", size);
        decorate(frag.findViewById(R.id.settingsSupportAPI), "This app wouldn’t have been possible without RoyaleAPI, please consider donating and supporting them.", size);
        decorate(frag.findViewById(R.id.settingsSupportText), "Report any issues, or suggest any ideas to Reddit.", size);

        urlOpener = v -> {
            String url;
            if(v.getId() == R.id.settingsSupportReddit)
                url = "https://www.reddit.com/r/ClashRoyale/comments/aeleie/update_clan_management_app_for_android_sakura/";
            else url = "https://github.com/rtsketo/SakuraStats";

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i); };

        refreshStored();
        for (int c = 0; c < 5; c++) {
            final int finalC = c;
            clanSele[c].setOnClickListener(view -> {
                bounce(view, acti); selectClan(finalC); });

            clanEdit[c].setOnClickListener(view ->
                    new DialogView(DialogView.SakuraDialog.INPUT, finalC, acti));
        }

        frag.findViewById(R.id.settingsSupportGitHub).setOnClickListener(urlOpener);
        frag.findViewById(R.id.settingsSupportReddit).setOnClickListener(urlOpener);
        return frag;
    }

    public void selectClan(int c) {
        acti.runOnUiThread(() -> setSelect(false));
        String cTag = acti.getStoredClan(c);
        if (cTag != null) {
            Service.getThread().start(cTag, false, true);
            acti.setLastClan(cTag);
        }
    }

    private void setSelect(boolean sele) {
        for (int c = 0; c < 5; c++) {
            if (acti.getStoredClan(c) != null)
                clanSele[c].setEnabled(sele);
            if (sele) clanSele[c].setColorFilter(null);
            else clanSele[c].setColorFilter(Color.argb(
                        100, 200, 200, 200));
        }
    }

    public void refreshStored(final int c) { refreshStored(c, true); }
    public void refreshStored(final int c, final boolean button) {
        if(acti.getStoredClan(c) != null)
            getCachePool().execute(() -> {
                final int size = sdp2px(9);
                final ClanStats clan = db.getClanStats(
                        acti.getStoredClan(c));

                acti.runOnUiThread(() -> {
                    if (button) {
                        clanSele[c].setEnabled(true);
                        clanSele[c].setColorFilter(null); }

                    if (clan != null) {
                        decorate(clanName[c], clan.getName(), size);
                        clanBadge[c].setImageResource(
                                acti.getResources()
                                        .getIdentifier(clan.getBadge(),
                                                "drawable", acti
                                                        .getPackageName()));}
                    else {
                        decorate(clanName[c], "#" +
                                acti.getStoredClan(c), size, Color.LTGRAY);
                        clanBadge[c].setImageResource(R.drawable.no_clan);}});});
        else {
            acti.runOnUiThread(() -> {
                clanSele[c].setColorFilter(Color.argb(100,200,200,200));
                clanSele[c].setEnabled(false);
        if (c<2) decorate(clanName[c], "Edit to Add a Clan", sdp2px(8), Color.LTGRAY);
        else decorate(clanName[c], "Not yet Available!", sdp2px(6), Color.GRAY); });
        }
    }

    public void refreshStored() {
        for (int c = 0; c < 2; c++)
            refreshStored(c); }
}
