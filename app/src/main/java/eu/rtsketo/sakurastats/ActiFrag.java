package eu.rtsketo.sakurastats;


import android.graphics.Color;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.app.Fragment;
import android.transition.TransitionManager;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.qwerjk.better_text.MagicTextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static eu.rtsketo.sakurastats.APIControl.getMemberActivity;
import static eu.rtsketo.sakurastats.APIControl.sleep;
import static eu.rtsketo.sakurastats.Interface.getLastForce;
import static eu.rtsketo.sakurastats.Interface.incUseCount;
import static eu.rtsketo.sakurastats.Statics.animateView;
import static eu.rtsketo.sakurastats.Statics.blinkView;
import static eu.rtsketo.sakurastats.Statics.bounce;
import static eu.rtsketo.sakurastats.Statics.decorate;
import static eu.rtsketo.sakurastats.Statics.getActi;
import static eu.rtsketo.sakurastats.Statics.getBackThread;
import static eu.rtsketo.sakurastats.Statics.getFixedPool;
import static eu.rtsketo.sakurastats.Statics.getPlayerMap;
import static eu.rtsketo.sakurastats.Statics.hasPlayerMap;
import static eu.rtsketo.sakurastats.Statics.o2l;
import static eu.rtsketo.sakurastats.Statics.rotate;
import static eu.rtsketo.sakurastats.Statics.sdp2px;
import static java.lang.Math.min;


public class ActiFrag extends Fragment {
    private MagicTextView loadView, info;
    private ConstraintLayout actiBar;
    private LinearLayout lineage;
    private boolean loading;
    private boolean loaded;
    private ImageView wifi;
    private int maxChest;
    private View root;

    private PlayerView[] playerView = new PlayerView[50];
    private ImageView loadingAnim, sortTime,
            sortSMC, sortMagi, sortLege;
    private int size, loadingCur, loadingMax;

    public boolean isLoaded() { return loaded; }
    public ActiFrag() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_activity,
                container, false);

        loadingAnim = root.findViewById(R.id.loadingAnim);
        sortTime = root.findViewById(R.id.sortTime);
        sortMagi = root.findViewById(R.id.sortMagi);
        sortLege = root.findViewById(R.id.sortLege);
        loadView = root.findViewById(R.id.loading);
        sortSMC = root.findViewById(R.id.sortSMC);
        actiBar = root.findViewById(R.id.actiBar);
        lineage = root.findViewById(R.id.lila);
        info = new MagicTextView(getActi());
        info.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        sortTime.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { bounce(view); selectSort(1); }});
        sortSMC.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { bounce(view); selectSort(2); }});
        sortLege.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { bounce(view); selectSort( 3); }});
        sortMagi.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { bounce(view); selectSort( 4); }});

        initFrames();

        loaded = true;
        System.out.println("Acti, I'm loaded!");
        return root;
    }

    private void initFrames() {
        final MagicTextView sort = root.findViewById(R.id.sort);
        size = sdp2px(9);

        getActi().runOnUiThread(new Runnable() {
            @Override public void run() {
            decorate(sort, "Sort by:", size);
            decorate(loadView, "Loading...", size);
            decorate(info, "\nStats are being loaded," +
                    " it might take a few minutes.\n", size + sdp2px(2));
            lineage.addView(info); }});


        for (int c = 0; c < playerView.length; c++) {
            final View frame = LayoutInflater.from(getActi())
                    .inflate(R.layout.frame_activity, null);

            playerView[c] = new PlayerView(frame);
            ImageView bgFrame = frame.findViewById(R.id.frameBG3);
            if (c%2==1) bgFrame.setImageResource(R.drawable.background_2);
            playerView[c].setWarChest((ImageView) frame.findViewById(R.id.magicalImage));
            playerView[c].setLast((MagicTextView) frame.findViewById(R.id.lastBattle));
            playerView[c].setMagi((MagicTextView) frame.findViewById(R.id.magical));
            playerView[c].setLege((MagicTextView) frame.findViewById(R.id.legend));
            playerView[c].setName((MagicTextView) frame.findViewById(R.id.namae));
            playerView[c].setRole((MagicTextView) frame.findViewById(R.id.role));
            playerView[c].setTime((MagicTextView) frame.findViewById(R.id.time));
            playerView[c].setSMC((MagicTextView) frame.findViewById(R.id.smc));
            frame.setVisibility(View.GONE);

            getActi().runOnUiThread(new Runnable() {
                @Override public void run() {
                    lineage.addView(frame);
                }});
        }

        rotate(root.findViewById(R.id.actiSelection));
        loadingAnim.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { refreshForce(); }});
        selectSort(0);
    }

    public synchronized void selectSort(final int choice) {
        final ConstraintSet conSet = new ConstraintSet();
        int posView = ConstraintSet.START, idView = 0,
                seleVis = View.VISIBLE;

        switch(choice) {
            case 2: idView = R.id.sortLege; break;
            case 3: idView = R.id.sortMagi; break;
            case 4: idView = R.id.sortTime; break;
            case 1: idView = R.id.actiBar;
                posView = ConstraintSet.END; break;
            case 0: seleVis = View.INVISIBLE;
                idView = R.id.actiBar;
//                idView = R.id.loadingAnim;
        }

        conSet.clone(actiBar);
        conSet.connect(R.id.actiSelection,
                ConstraintSet.END, idView, posView, sdp2px(2));
        conSet.setVisibility(R.id.actiSelection, seleVis);
        TransitionManager.beginDelayedTransition(actiBar);
        getActi().runOnUiThread(new Runnable() {
            @Override public void run() {
                conSet.applyTo(actiBar); }});
        if (choice > 0) refreshList(choice);
    }

    public boolean getLoading() { return loading; }
    public void setLoading(final boolean loading) {
        getActi().runOnUiThread(new Runnable() {
            @Override public void run() {
                if (!loading || getBackThread().getGUI())
                    blinkView(((Interface)getActi())
                        .getTab(2), loading);

                if (loading) {
                    sortSMC.setEnabled(false);
                    sortTime.setEnabled(false);
                    sortMagi.setEnabled(false);
                    sortLege.setEnabled(false);
                    loadingAnim.setEnabled(false);
                    loadingAnim.setColorFilter(null);
                    loadView.setVisibility(View.VISIBLE);
                    decorate(loadView, "Loading...", size);
                    loadingAnim.setImageResource(R.drawable.loading);
                    sortSMC.setColorFilter(Color.argb(100,200,200,200));
                    sortMagi.setColorFilter(Color.argb(100,200,200,200));
                    sortLege.setColorFilter(Color.argb(100,200,200,200));
                    sortTime.setColorFilter(Color.argb(100,200,200,200));
                    animateView(loadingAnim, loading);
                } else {
                    sortSMC.setEnabled(true);
                    sortTime.setEnabled(true);
                    sortMagi.setEnabled(true);
                    sortLege.setEnabled(true);
                    sortSMC.setColorFilter(null);
                    sortMagi.setColorFilter(null);
                    sortLege.setColorFilter(null);
                    sortTime.setColorFilter(null);
                    info.setVisibility(View.GONE);
                    animateView(loadingAnim, loading);
                    loadingAnim.setImageResource(R.drawable.refresh);
                    loadingAnim.setColorFilter(Color.argb(100,200,200,200));

                    final Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override public void run() {
                            final int refresh = 15 - getLastForce(2);
                            if (refresh < 0) getActi().runOnUiThread(new Runnable() {
                                @Override public void run() {
                                    loadView.setVisibility(View.INVISIBLE);
                                    loadingAnim.setColorFilter(null);
                                    loadingAnim.setEnabled(true);
                                    timer.cancel(); }});
                            else getActi().runOnUiThread(new Runnable() {
                                @Override public void run() {
                                    loadView.setVisibility(View.VISIBLE);
                                    decorate(loadView, refresh+"min", size); }});
                        }}, 0, 60000); }}});
        this.loading = loading;
    }

    public void refreshInfo() { info.setVisibility(View.VISIBLE); }
    public void refreshForce() { refresh(true, true); }
    public void refresh(final boolean gui, final boolean force) {
        setLoading(true);
        getFixedPool().execute(new Runnable() {
            @Override public void run() {
                selectSort(0);
                if (gui) clearList();

                int c = getPlayerMap().size() - 1;
                resetLoading(getPlayerMap().size());
                if (getBackThread().getApproval())
                    for (final Map.Entry<String, Pair<ClanPlayer,
                            PlayerStats>> player : getPlayerMap().entrySet()) {
                        final int finalC = c--;
                        if (finalC < 0)
                            System.out.println("Error: More than 50 players!");
                        else getFixedPool().execute(new Runnable() {
                            @Override
                            public void run() {
                                if (getBackThread().getApproval()) {
                                    Pair<ClanPlayer, PlayerStats>
                                            pair = player.getValue();
                                    getMemberActivity(pair.first, force);
                                    if (gui) addPlayer(pair.first, pair.second, finalC);
                                    increaseLoading(); }}
                        });
                        sleep(160); }
                incUseCount(); }}); }

    public void refreshList(final int choice) {
        getFixedPool().execute(new Runnable() {
            @Override public void run() {
                if (hasPlayerMap()) {
                    setLoading(true);
                    clearList();

                    List<PlayerStats> tempPS = new ArrayList<>();
                    if (choice == 0 || choice == 4) {
                        for (Map.Entry<String, Pair<ClanPlayer, PlayerStats>>
                                player : getPlayerMap().entrySet())
                            if (player.getValue().first != null)
                                tempPS.add(player.getValue().second);
                        Collections.sort(tempPS, new SortByWar());

                        if (tempPS.size() > 0)
                            maxChest = tempPS.get(0).getChest(); }

                    if (choice != 4) {
                        List<ClanPlayer> tempCP = new ArrayList<>();
                        for (Map.Entry<String, Pair<ClanPlayer, PlayerStats>>
                                player : getPlayerMap().entrySet())
                            if (player.getValue().first != null)
                                tempCP.add(player.getValue().first);

                        switch (choice) {
                            case 1: Collections.sort(tempCP, new SortByTime()); break;
                            case 2: Collections.sort(tempCP, new SortBySMC()); break;
                            case 3: Collections.sort(tempCP, new SortByLege()); break;
                            case 0: break; }

                        for (int c = min(tempCP.size()-1,49); c >= 0 ; c--) {
                            sleep(20);
                            String tag = tempCP.get(c).getTag();
                            Pair<ClanPlayer, PlayerStats> pair = getPlayerMap().get(tag);
                            addPlayer(pair.first, pair.second, c); }

                    } else for (int c = min(tempPS.size() - 1,49); c >= 0; c--) {
                            sleep(20);
                            String tag = tempPS.get(c).getTag();
                            Pair<ClanPlayer, PlayerStats> pair = getPlayerMap().get(tag);
                            addPlayer(pair.first, pair.second, c); }
                    }

                    setLoading(false); }});
}

    public void clearList() {
        getActi().runOnUiThread(new Runnable() {
            @Override public void run() {
                for (PlayerView pv : playerView) {
                    View frame = pv.getFrame();
                    if (frame != null)
                        frame.setVisibility(View.GONE); }}});
    }

    public void increaseLoading() {
        loadingCur++;
        if (loadingCur == loadingMax)
            setLoading(false);

        getActi().runOnUiThread(new Runnable() {
            @Override public void run() {
                decorate(loadView, "... " +
                        loadingCur + "/" + loadingMax, size);
            }});
    }

    public void resetLoading(int max) {
        loadingMax = max;
        loadingCur = 0;
    }


    public void addPlayer(final ClanPlayer cp, final PlayerStats ps, int c) {
        final PlayerView pv = playerView[c]; String lastBattle = "";
        double diff  = System.currentTimeMillis() / 1000d - cp.getLast();
        int temp = (int) (diff / 60 / 60 / 24 / 30);
        if (temp > 500) lastBattle = "N/A";
        else {
            if (temp > 0) lastBattle = temp + " month";
            else {
                temp = (int) (diff / 60 / 60 / 24);
                if (temp > 0) lastBattle = temp + " day";
                else lastBattle = "few hours"; }
            if (temp > 1) lastBattle += "s"; }

        final int maxy = 140;
        String roleString = "";
        switch (cp.getRole()) {
            case "coLeader": roleString = "Co-Leader"; break;
            case "leader": roleString = "Leader"; break;
            case "member": roleString = "Member"; break;
            case "elder": roleString = "Elder"; break; }

        int leagueEmblem = 0;
        String chestString = "--";
        int chest = ps.getChest();
        if (chest/100 > 1 && maxChest-chest < 25) {
            int league = o2l(chest - (chest/100)*100);
            chestString = "#" + (league - (league/10)*10);
            league = league / 10;

            switch (league) {
                case 4: leagueEmblem = R.drawable.clanwars_league_bronze; break;
                case 3: leagueEmblem = R.drawable.clanwars_league_silver; break;
                case 2: leagueEmblem = R.drawable.clanwars_league_gold; break;
                case 1: leagueEmblem = R.drawable.clanwars_league_legendary; break; }}
        else leagueEmblem = R.drawable.no_clan;

        final String finalRoleString = roleString;
        final String finalLastBattle = lastBattle;
        final int finalLeagueEmblem = leagueEmblem;
        final String finalChestString = chestString;
        final int[] size = { sdp2px(9), sdp2px(8), sdp2px(6) };
        getActi().runOnUiThread(new Runnable() {
            @Override public void run() {
            pv.getWarChest().setImageResource(finalLeagueEmblem);
            decorate(pv.getRole(), finalRoleString, size[1]);
            decorate(pv.getLast(), "Last battle:", size[2]);
            decorate(pv.getTime(), finalLastBattle, size[1]);
            decorate(pv.getMagi(), finalChestString, size[1]);
            decorate(pv.getLege(), cp.getLegendary(), size[1]);
            decorate(pv.getSMC(), cp.getSmc(), size[1]);
            decorate(pv.getName(), ps.getName(), size[0],
                    Color.WHITE, maxy);
            pv.getFrame().setVisibility(View.VISIBLE); }});
    }

    public ImageView getWifi() {
        if (wifi == null)
            wifi = root.findViewById(R.id.wifi);
        return wifi;
    }

    private class SortByTime implements Comparator<ClanPlayer> {
        @Override public int compare(ClanPlayer a, ClanPlayer b) {
            return (int) (a.getLast() - b.getLast()); }}

    private class SortBySMC implements Comparator<ClanPlayer> {
        @Override public int compare(ClanPlayer a, ClanPlayer b) {
            return (a.getSmc() - b.getSmc()); }}

    private class SortByLege implements Comparator<ClanPlayer> {
        @Override public int compare(ClanPlayer a, ClanPlayer b) {
            return (a.getLegendary() - b.getLegendary()); }}

    private class SortByMagi implements Comparator<ClanPlayer> {
        @Override public int compare(ClanPlayer a, ClanPlayer b) {
            return (a.getMagical() - b.getMagical()); }}

    private class SortByWar implements Comparator<PlayerStats> {
        @Override public int compare(PlayerStats a, PlayerStats b) {
            return (b.getChest() - a.getChest()); }}

    private class PlayerView {
        private MagicTextView name;
        private MagicTextView role;
        private MagicTextView last;
        private MagicTextView time;
        private MagicTextView magi;
        private MagicTextView lege;
        private MagicTextView smc;
        private ImageView warChest;

        public void setWarChest(ImageView warChest) { this.warChest = warChest; }
        public void setName(MagicTextView name) { this.name = name; }
        public void setRole(MagicTextView role) { this.role = role; }
        public void setLast(MagicTextView last) { this.last = last; }
        public void setTime(MagicTextView time) { this.time = time; }
        public void setMagi(MagicTextView magi) { this.magi = magi; }
        public void setLege(MagicTextView lege) { this.lege = lege; }
        public void setSMC(MagicTextView smc) { this.smc = smc; }
        public ImageView getWarChest() { return warChest; }
        public MagicTextView getTime() { return time; }
        public MagicTextView getRole() { return role; }
        public MagicTextView getLast() { return last; }
        public MagicTextView getLege() { return lege; }
        public MagicTextView getMagi() { return magi; }
        public MagicTextView getName() { return name; }
        public MagicTextView getSMC() { return smc; }
        public View getFrame() { return frame; }

        private View frame;
        public PlayerView(View frame) {
            this.frame = frame; }
    }

}
