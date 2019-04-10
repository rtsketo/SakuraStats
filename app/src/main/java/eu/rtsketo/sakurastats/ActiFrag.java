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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import static eu.rtsketo.sakurastats.APIControl.sleep;
import static eu.rtsketo.sakurastats.Interface.getLastForce;
import static eu.rtsketo.sakurastats.Statics.animateView;
import static eu.rtsketo.sakurastats.Statics.blinkView;
import static eu.rtsketo.sakurastats.Statics.bounce;
import static eu.rtsketo.sakurastats.Statics.decorate;
import static eu.rtsketo.sakurastats.Statics.getActi;
import static eu.rtsketo.sakurastats.Statics.getBackThread;
import static eu.rtsketo.sakurastats.Statics.getCachePool;
import static eu.rtsketo.sakurastats.Statics.getClanSize;
import static eu.rtsketo.sakurastats.Statics.getPlayerMap;
import static eu.rtsketo.sakurastats.Statics.hasPlayerMap;
import static eu.rtsketo.sakurastats.Statics.o2l;
import static eu.rtsketo.sakurastats.Statics.rotate;
import static eu.rtsketo.sakurastats.Statics.sdp2px;
import static eu.rtsketo.sakurastats.Statics.updatePlayerMap;
import static java.lang.Math.min;


public class ActiFrag extends Fragment {
    private MagicTextView loadView;
    private ConstraintLayout actiBar;
    private LinearLayout lineage;
    private MagicTextView info;
    private boolean loading;
    private boolean loaded;
    private ImageView wifi;
    private int maxChest;
    private View root;

    private Map<String, PlayerView> playerMap;
    private boolean[] observers = new boolean[2];
    private PlayerView[] playerView = new PlayerView[50];
    private ImageView loadingAnim;
    private ImageView sortTime;
    private ImageView sortMagi;
    private ImageView sortLege;
    private ImageView sortSMC;
    private int[] size;

    public boolean isLoaded() { return loaded; }
    public ActiFrag() { /* Needed for FragmentManager */ }

    public Observer<PlayerStats> psObserver() {
        return new Observer<PlayerStats>() {
            @Override public void onSubscribe(Disposable d) { subscribe(0); }
            @Override public void onNext(PlayerStats ps) { displayStats(ps); }
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onComplete() { complete(0); }};
    }

    public Observer<ClanPlayer> cpObserver() {
        return new Observer<ClanPlayer>() {
            @Override public void onSubscribe(Disposable d) { subscribe(1); }
            @Override public void onNext(ClanPlayer cp) { displayStats(cp); }
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onComplete() { complete(1); }};
    }

    private void subscribe(int obs) {
        observers[obs] = true;
        if (!observers[1 - obs])
            setLoading(true); }

    private void complete(int obs) {
        observers[obs] = false;
        if (!observers[1 - obs])
            setLoading(false); }

    private void displayStats(ClanPlayer cp) { displayStats(null, cp); }
    private void displayStats(PlayerStats ps) { displayStats(ps, null); }
    private void displayStats(PlayerStats ps, ClanPlayer cp) {
        final String tagString;
        if (ps == null) tagString = cp.getTag();
        else tagString = ps.getTag();

        final int maxy = 140;
        final int[] size = { sdp2px(9), sdp2px(8) };
        PlayerView pv = playerMap.get(tagString) == null?
                getPV(tagString) : playerMap.get(tagString);

        if (cp != null) {
            String lastBattle =
                    lastBattleText(cp.getLast());

            String roleString;
            switch (cp.getRole()) {
                case "coLeader": roleString = "Co-Leader"; break;
                case "leader": roleString = "Leader"; break;
                case "elder": roleString = "Elder"; break;
                default: roleString = "Member";
            }

            final String finalRoleString = roleString;
            final String finalLastBattle = lastBattle;

            getActi().runOnUiThread(() -> {
                decorate(pv.role, finalRoleString, size[1]);
                decorate(pv.time, finalLastBattle, size[1]);
                decorate(pv.lege, cp.getLegendary(), size[1]);
                decorate(pv.smc, cp.getSmc(), size[1]);
                pv.frame.setVisibility(View.VISIBLE);
            });
        }

        if (ps != null) {
            int leagueEmblem = 0;
            String chestString = "--";
            int chest = ps.getChest();
            if (chest / 100 > 1 && maxChest - chest < 25) {
                int league = o2l(chest - (chest / 100) * 100);
                chestString = "#" + (league - (league / 10) * 10);
                league = league / 10;

                switch (league) {
                    case 3: leagueEmblem = R.drawable.clanwars_league_silver; break;
                    case 2: leagueEmblem = R.drawable.clanwars_league_gold; break;
                    case 1: leagueEmblem = R.drawable.clanwars_league_legendary; break;
                    default: leagueEmblem = R.drawable.clanwars_league_bronze;
                }

            } else leagueEmblem = R.drawable.no_clan;

            final int finalLeagueEmblem = leagueEmblem;
            final String finalChestString = chestString;
            getActi().runOnUiThread(() -> {
                pv.warChest.setImageResource(finalLeagueEmblem);
                decorate(pv.magi, finalChestString, size[1]);
                decorate(pv.name, ps.getName(), size[0], Color.WHITE, maxy);
                pv.frame.setVisibility(View.VISIBLE);
            });

         }
    }

    private String lastBattleText(long lb) {
        StringBuilder lastBattle;
        double diff = System.currentTimeMillis() / 1000d - lb;
        int temp = (int) (diff / 60 / 60 / 24 / 30);
        if (temp > 500) lastBattle = new StringBuilder("N/A");
        else {
            if (temp > 0) lastBattle = new StringBuilder(temp + " month");
            else {
                temp = (int) (diff / 60 / 60 / 24);
                if (temp > 0) lastBattle = new StringBuilder(temp + " day");
                else lastBattle = new StringBuilder("few hours");
            }
            if (temp > 1) lastBattle.append("s");
        }
        return lastBattle.toString();
    }

    private PlayerView getPV(String tag) {
        int pvNum = getClanSize() - 1
                - playerMap.size();
        PlayerView pv =
                playerView[pvNum];
        playerMap.put(tag, pv);
        return pv; }

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

        sortTime.setOnClickListener(view -> { bounce(view); selectSort(1); });
        sortSMC.setOnClickListener(view -> { bounce(view); selectSort(2); });
        sortLege.setOnClickListener(view -> { bounce(view); selectSort( 3); });
        sortMagi.setOnClickListener(view -> { bounce(view); selectSort( 4); });

        initFrames();

        playerMap = new HashMap<>();
        loaded = true;
        return root;
    }

    private void initFrames() {
        final MagicTextView sort = root.findViewById(R.id.sort);
        size = new int[]{ sdp2px(6), sdp2px(9) } ;

        getActi().runOnUiThread(() -> {
            decorate(sort, "Sort by:", size[1]);
            decorate(loadView, "Loading...", size[1]);
            decorate(info, "\nStats are being loaded," +
                    " please wait...\n", size[1]+2);
            lineage.addView(info); });

        for (int c = 0; c < playerView.length; c++) {
            final View frame = LayoutInflater.from(getActi())
                    .inflate(R.layout.frame_activity, null);

            playerView[c] = new PlayerView(frame);
            ImageView bgFrame = frame.findViewById(R.id.frameBG3);
            if (c%2==1) bgFrame.setImageResource(R.drawable.background_2);
            playerView[c].warChest = frame.findViewById(R.id.magicalImage);
            playerView[c].magi = frame.findViewById(R.id.magical);
            playerView[c].lege = frame.findViewById(R.id.legend);
            playerView[c].name = frame.findViewById(R.id.namae);
            playerView[c].role = frame.findViewById(R.id.role);
            playerView[c].time = frame.findViewById(R.id.time);
            playerView[c].smc = frame.findViewById(R.id.smc);
            frame.setVisibility(View.GONE);

            PlayerView finalPV = playerView[c];
            getActi().runOnUiThread(() -> {
                decorate(frame.findViewById(R.id.lastBattle), "Last battle:", size[0]);
                    decorate(finalPV.role, "???", size[1]);
                    decorate(finalPV.time, "???", size[1]);
                    decorate(finalPV.lege, "???", size[1]);
                    decorate(finalPV.smc, "???", size[1]);
                    decorate(finalPV.magi, "???", size[1]);
                    decorate(finalPV.name, "???", size[0]);
                lineage.addView(frame); });
        }

        rotate(root.findViewById(R.id.actiSelection));
        loadingAnim.setOnClickListener(v -> {
            setLoading(true); clearList();
            updatePlayerMap(true); });
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
        }

        conSet.clone(actiBar);
        conSet.connect(R.id.actiSelection,
                ConstraintSet.END, idView, posView, sdp2px(2));
        conSet.setVisibility(R.id.actiSelection, seleVis);
        TransitionManager.beginDelayedTransition(actiBar);
        getActi().runOnUiThread(() -> conSet.applyTo(actiBar));
        if (choice > 0) refreshList(choice);
    }

    public boolean getLoading() { return loading; }
    public void setLoading(final boolean loading) {
        getActi().runOnUiThread(() -> {
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
                decorate(loadView, "Loading...", size[1]);
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
                        if (refresh < 0) getActi().runOnUiThread(() -> {
                            loadView.setVisibility(View.INVISIBLE);
                            loadingAnim.setColorFilter(null);
                            loadingAnim.setEnabled(true);
                            timer.cancel(); });
                        else getActi().runOnUiThread(() -> {
                            loadView.setVisibility(View.VISIBLE);
                            decorate(loadView, refresh+"min", size[1]); });
                    }}, 0, 60000); }});
        this.loading = loading;
    }

    public void refreshInfo() { info.setVisibility(View.VISIBLE); }
    public void refreshList(final int choice) {
        getCachePool().execute(() -> {
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
                        displayStats(pair.second, pair.first); }

                } else for (int c = min(tempPS.size() - 1,49); c >= 0; c--) {
                        sleep(20);
                        String tag = tempPS.get(c).getTag();
                        Pair<ClanPlayer, PlayerStats> pair = getPlayerMap().get(tag);
                        displayStats(pair.second, pair.first); }
                }

                setLoading(false); });
    }

    public void clearList() {
        playerMap.clear();
        getActi().runOnUiThread(() -> {
            for (PlayerView pv : playerView) {
                View frame = pv.frame;
                if (frame != null)
                    frame.setVisibility(View.GONE); }});
    }

    public void updateLoading(int cur, int max) {
        int percent = cur * 100 / max;
        getActi().runOnUiThread(() ->
                decorate(loadView, "... " +
                        percent + "%", size[1]));
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
        private MagicTextView time;
        private MagicTextView magi;
        private MagicTextView lege;
        private MagicTextView smc;
        private ImageView warChest;
        private View frame;

        private PlayerView(View frame) {
            this.frame = frame; }
    }
}
