package eu.rtsketo.sakurastats;

import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
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

import static eu.rtsketo.sakurastats.APIControl.getPlayerProfile;
import static eu.rtsketo.sakurastats.APIControl.getPlayerStats;
import static eu.rtsketo.sakurastats.APIControl.sleep;
import static eu.rtsketo.sakurastats.Interface.getLastClan;
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
import static eu.rtsketo.sakurastats.Statics.putPlayerMap;
import static eu.rtsketo.sakurastats.Statics.rotate;
import static eu.rtsketo.sakurastats.Statics.sdp2px;
import static java.lang.Math.min;

public class WarFrag extends Fragment {
    private View root; private ImageView wifi;
    private PlayerView[] playerView = new PlayerView[50];
    private MagicTextView loadView, info;
    private ConstraintLayout warBar;
    private LinearLayout lineage;
    private boolean loaded, loading;
    private ImageView loadingAnim,
            sortRatio, sortScore, sortTroph;
    private int size, loadingCur, loadingMax;
    public boolean isLoaded() { return loaded; }
    public WarFrag() { }

    public void refreshList(final int choice) { // Check nulls should be erased.
        getFixedPool().execute(new Runnable() {
            @Override public void run() {
                if (hasPlayerMap()) {
                    setLoading(true);
                    clearList();

                    if(choice != 3) {
                        List<ClanPlayer> tempCP = new ArrayList<>();
                        for (Map.Entry<String, Pair<ClanPlayer, PlayerStats>>
                                player : getPlayerMap().entrySet())
                            if (player.getValue().first != null)
                                tempCP.add(player.getValue().first);

                        if (choice == 1) Collections.sort(tempCP, new SortByTrophies());
                        if (choice == 2) Collections.sort(tempCP, new SortByScore());

                        for (int c = min(tempCP.size() - 1,49); c >= 0; c--) {
                            sleep(20);
                            String tag = tempCP.get(c).getTag();
                            Pair<ClanPlayer, PlayerStats> pair = getPlayerMap().get(tag);
                            addPlayer(pair.first, pair.second, c); }

                    } else {
                            List<PlayerStats> tempPS = new ArrayList<>();
                            for (Map.Entry<String, Pair<ClanPlayer, PlayerStats>>
                                    player : getPlayerMap().entrySet())
                                if (player.getValue().first != null)
                                    tempPS.add(player.getValue().second);
                            Collections.sort(tempPS, new SortByRatio());

                            for (int c = min(tempPS.size() - 1,49); c >= 0; c--) {
                                sleep(20);
                                String tag = tempPS.get(c).getTag();
                                Pair<ClanPlayer, PlayerStats> pair = getPlayerMap().get(tag);
                                addPlayer(pair.first, pair.second, c); }
                    }

                    setLoading(false); }}});
    }

    public boolean getLoading() { return loading; }
    public void setLoading(final boolean loading) {
        getActi().runOnUiThread(new Runnable() {
            @Override public void run() {
                if (!loading || getBackThread().getGUI())
                    blinkView(((Interface)getActi())
                        .getTab(1), loading);

                if (loading) {
                    sortRatio.setEnabled(false);
                    sortScore.setEnabled(false);
                    sortTroph.setEnabled(false);
                    loadingAnim.setEnabled(false);
                    loadingAnim.setColorFilter(null);
                    loadView.setVisibility(View.VISIBLE);
                    decorate(loadView, "Loading...", size);
                    loadingAnim.setImageResource(R.drawable.loading);
                    sortRatio.setColorFilter(Color.argb(100,200,200,200));
                    sortScore.setColorFilter(Color.argb(100,200,200,200));
                    sortTroph.setColorFilter(Color.argb(100,200,200,200));
                    animateView(loadingAnim, loading);
                } else {
                    sortRatio.setEnabled(true);
                    sortScore.setEnabled(true);
                    sortTroph.setEnabled(true);
                    info.setVisibility(View.GONE);
                    sortRatio.setColorFilter(null);
                    sortScore.setColorFilter(null);
                    sortTroph.setColorFilter(null);
                    animateView(loadingAnim, loading);
                    loadingAnim.setImageResource(R.drawable.refresh);
                    loadingAnim.setColorFilter(Color.argb(100,200,200,200));

                    final Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override public void run() {
                            final int refresh = 15 - getLastForce(1);
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
    public void refreshForce() { refresh(null, true, true); }
    public void refresh(final List<PlayerStats> finalPS, final boolean gui, final boolean force) {
        setLoading(true);
        getFixedPool().execute(new Runnable() {
            @Override public void run() {
                selectSort(0);
                if (gui) clearList();

                List<PlayerStats> ps = finalPS;
                if (ps == null) ps = getPlayerStats(getLastClan(), force);

                int c = ps.size()-1;
                resetLoading(ps.size());
                if (getBackThread().getApproval())
                    for (final PlayerStats playerStats : ps) {
                        final int finalC = c--;
                        if (finalC < 0)
                            System.out.println("Error: More than 50 players!");
                        else getFixedPool().execute(new Runnable() {
                            @Override public void run() {
                                if (getBackThread().getApproval()) {
                                    String pTag = playerStats.getTag();
                                    ClanPlayer clanPlayer = getPlayerProfile(pTag, force);
                                    if (gui) putPlayerMap(pTag, new Pair<>(clanPlayer, playerStats));
                                    if (gui) addPlayer(clanPlayer, playerStats, finalC);
                                    increaseLoading(); }}});
                        sleep(80); }
                incUseCount(); }});
    }

    public void clearList() {
        getActi().runOnUiThread(new Runnable() {
            @Override public void run() {
                for (PlayerView pv : playerView) {
                    View frame = pv.getFrame();
                    if (frame != null)
                        frame.setVisibility(View.GONE); }}});
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_warstats, container, false);
        lineage = root.findViewById(R.id.lineage);
        warBar = root.findViewById(R.id.warBar);

        loadingAnim = root.findViewById(R.id.loadingAnim);
        sortRatio = root.findViewById(R.id.sortRatio);
        sortScore = root.findViewById(R.id.sortScore);
        sortTroph = root.findViewById(R.id.sortTroph);
        loadView = root.findViewById(R.id.loading);
        info = new MagicTextView(getActi());
        info.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        sortTroph.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { bounce(view); selectSort(1); }});
        sortScore.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { bounce(view); selectSort(2); }});
        sortRatio.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { bounce(view); selectSort(3); }});

        initFrames();

        loaded = true;
        System.out.println("War, I'm loaded!");
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
                    " it might take a few minutes.\n", size+2);
            lineage.addView(info); }});

        for (int c = 0; c < playerView.length; c++) {
            final View frame;
//            if (smallWidth())
//                frame = LayoutInflater.from(getActi())
//                        .inflate(R.layout.frame_warstats_small, null);
//            else
                frame = LayoutInflater.from(getActi())
                    .inflate(R.layout.frame_warstats, null);

            playerView[c] = new PlayerView(frame);
            ImageView bgFrame = frame.findViewById(R.id.frameBG2);
            if (c%2==1) bgFrame.setImageResource(R.drawable.background_2);

            playerView[c].setTroph((MagicTextView) frame.findViewById(R.id.trophies));
            playerView[c].setMissed((MagicTextView) frame.findViewById(R.id.missed));
            playerView[c].setScore((MagicTextView) frame.findViewById(R.id.score));
            playerView[c].setNorma((MagicTextView) frame.findViewById(R.id.norma));
            playerView[c].setCards((MagicTextView) frame.findViewById(R.id.cards));
            playerView[c].setRatio((MagicTextView) frame.findViewById(R.id.ratio));
            playerView[c].setName((MagicTextView) frame.findViewById(R.id.name));
            playerView[c].setWars((MagicTextView) frame.findViewById(R.id.wars));
            playerView[c].setTag((MagicTextView) frame.findViewById(R.id.tag));
            frame.setVisibility(View.GONE);

            final int finalC = c;
            final MagicTextView rank = frame.findViewById(R.id.rank);
            getActi().runOnUiThread(new Runnable() {
                @Override public void run() {
                    decorate(rank, finalC+1+"",size+2);
                    lineage.addView(frame); }});
        }
        rotate(root.findViewById(R.id.warSelection));
        loadingAnim.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { refreshForce(); }});
        selectSort(0);
    }

    public synchronized void selectSort(final int choice) {
        final ConstraintSet conSet = new ConstraintSet();
        int posView = ConstraintSet.START, idView = 0,
                disView = 8, seleVis = View.VISIBLE;
        switch(choice) {
            case 1: idView = R.id.sortScore; break;
            case 2: idView = R.id.sortRatio; break;
            case 3: idView = R.id.warBar;
                posView = ConstraintSet.END;
                disView = 3; break;
            case 0: seleVis = View.INVISIBLE;
                idView = R.id.loadingAnim;
        }

        conSet.clone(warBar);
        conSet.connect(R.id.warSelection,
                ConstraintSet.END, idView, posView, sdp2px(disView));
        conSet.setVisibility(R.id.warSelection, seleVis);
        TransitionManager.beginDelayedTransition(warBar);
        getActi().runOnUiThread(new Runnable() {
            @Override public void run() {
                conSet.applyTo(warBar); }});
        if (choice > 0) refreshList(choice);
    }

    public void addPlayer(final ClanPlayer cp, final PlayerStats ps, int c) {
        final String normaText = (int)(ps.getNorma()*100)+"%";
        final String ratioText = (int)(ps.getRatio()*100)+"%";
        final String scoreText = cp.getScore()==9001?
                "max":String.valueOf(cp.getScore());
        final int cardsText = ps.getWars()==0?
                0 : ps.getCards()/ps.getWars();
        final String tagString = cp.getTag();
        final PlayerView pv = playerView[c];

        final int[] maxy = new int[2];
//        if (smallWidth()) {
            maxy[0] = 112;
            maxy[1] = 62;
//        } else {
//            maxy[0] = 120;
//            maxy[1] = -1; }

        final int[] size = { sdp2px(9), sdp2px(8) };
        getActi().runOnUiThread(new Runnable() {
            @Override public void run() {
                decorate(pv.getScore(), scoreText, size[0]);
                decorate(pv.getNorma(), normaText, size[0]);
                decorate(pv.getTroph(), cp.getTrophies(), size[1]);
                decorate(pv.getWars(), ps.getWars(), size[1]);
                decorate(pv.getMissed(), ps.getMissed(), size[1]);
                decorate(pv.getCards(), cardsText, size[1]);
                decorate(pv.getRatio(), ratioText,size[1]);
                decorate(pv.getName(), ps.getName(), size[0],
                        Color.WHITE, maxy[0]);
                decorate(pv.getTag(), "#"+tagString, size[1],
                        Color.WHITE, maxy[1]);
                pv.getFrame().setVisibility(View.VISIBLE); }});
    }

    public ImageView getWifi() {
        if (wifi == null)
            wifi = root.findViewById(R.id.wifi);
        return wifi;
    }

    public void increaseLoading() {
        if (++loadingCur == loadingMax) setLoading(false);
        else getActi().runOnUiThread(new Runnable() {
            @Override public void run() {
                decorate(loadView, "... " +
                        loadingCur + "/" + loadingMax, size); }});
    }

    public void resetLoading(int max) {
        loadingMax = max;
        loadingCur = 0;
    }

    private class SortByScore implements Comparator<ClanPlayer> {
        @Override public int compare(ClanPlayer a, ClanPlayer b) {
            return b.getScore() - a.getScore();
        }}

    private class SortByTrophies implements Comparator<ClanPlayer> {
        @Override public int compare(ClanPlayer a, ClanPlayer b) {
            return b.getTrophies() - a.getTrophies();
        }}

    private class SortByRatio implements Comparator<PlayerStats> {
        @Override public int compare(PlayerStats a, PlayerStats b) {
            return (int) (b.getNorma()*10000 - a.getNorma()*10000); }}

    private class PlayerView {
        private MagicTextView name;
        private MagicTextView score;
        private MagicTextView norma;
        private MagicTextView tag;
        private MagicTextView troph;
        private MagicTextView wars;
        private MagicTextView missed;
        private MagicTextView cards;
        private MagicTextView ratio;

        private View frame;
        public PlayerView(View frame) {
            this.frame = frame; }

        public View getFrame() { return frame; }
        public MagicTextView getTag() { return tag; }
        public MagicTextView getName() { return name; }
        public MagicTextView getWars() { return wars; }
        public MagicTextView getScore() { return score; }
        public MagicTextView getNorma() { return norma; }
        public MagicTextView getTroph() { return troph; }
        public MagicTextView getCards() { return cards; }
        public MagicTextView getRatio() { return ratio; }
        public MagicTextView getMissed() { return missed; }
        public void setTag(MagicTextView tag) { this.tag = tag; }
        public void setName(MagicTextView name) { this.name = name; }
        public void setWars(MagicTextView wars) { this.wars = wars; }
        public void setScore(MagicTextView score) { this.score = score; }
        public void setNorma(MagicTextView norma) { this.norma = norma; }
        public void setTroph(MagicTextView troph) { this.troph = troph; }
        public void setCards(MagicTextView cards) { this.cards = cards; }
        public void setRatio(MagicTextView ratio) { this.ratio = ratio; }
        public void setMissed(MagicTextView missed) { this.missed = missed; }
    }
}
