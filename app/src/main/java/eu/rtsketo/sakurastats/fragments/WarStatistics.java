package eu.rtsketo.sakurastats.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.app.Fragment;
import android.transition.TransitionManager;
import android.util.Log;
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

import eu.rtsketo.sakurastats.R;
import eu.rtsketo.sakurastats.dbobjects.ClanPlayer;
import eu.rtsketo.sakurastats.dbobjects.PlayerStats;
import eu.rtsketo.sakurastats.hashmaps.PlayerMap;
import eu.rtsketo.sakurastats.main.Interface;
import eu.rtsketo.sakurastats.main.Service;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import static eu.rtsketo.sakurastats.control.ThreadPool.getFixedPool;
import static eu.rtsketo.sakurastats.control.ViewDecor.animateView;
import static eu.rtsketo.sakurastats.control.ViewDecor.blinkView;
import static eu.rtsketo.sakurastats.control.ViewDecor.bounce;
import static eu.rtsketo.sakurastats.control.ViewDecor.decorate;
import static eu.rtsketo.sakurastats.control.ViewDecor.rotate;
import static eu.rtsketo.sakurastats.hashmaps.SDPMap.sdp2px;
import static eu.rtsketo.sakurastats.main.Interface.TAG;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class WarStatistics extends Fragment {
    private View root; private ImageView wifi;
        private MagicTextView loadView, info;
    private ConstraintLayout warBar;
    private LinearLayout lineage;
    private ImageView loadingAnim,
            sortRatio, sortScore, sortTroph;
    private Map<String, PlayerView> playerMap;
    private boolean[] observers = new boolean[2];
    private PlayerView[] playerView = new PlayerView[50];
    private final int[] size = { sdp2px(9), sdp2px(8) };
    private static final int[] maxy = { 112, 62 };
    private boolean loading;
    private Interface acti;

    public WarStatistics() { /* Needed for FragmentManager */ }

    public Observer<PlayerStats> psObserver() {
        return new Observer<PlayerStats>() {
            @Override public void onSubscribe(Disposable d) { subscribe(0); }
            @Override public void onNext(PlayerStats ps) { displayStats(ps); }
            @Override public void onComplete() { complete(0); }
            @Override public void onError(Throwable e) {
                Log.e(TAG, "PS Observer failed", e); }};
    }

    public Observer<ClanPlayer> cpObserver() {
        return new Observer<ClanPlayer>() {
            @Override public void onSubscribe(Disposable d) { subscribe(1); }
            @Override public void onNext(ClanPlayer cp) { displayStats(cp); }
            @Override public void onComplete() { complete(1); }
            @Override public void onError(Throwable e) {
                Log.e(TAG, "CP Observer failed", e); }};
    }

    private void subscribe(int obs) {
        observers[obs] = true;
        if (!observers[1 - obs]) {
            acti.runOnUiThread(() ->
                    acti.changeTabTo(1));
            setLoading(true);
            clearList(); }}

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

        PlayerView pv = playerMap.get(tagString);
        if (pv == null) pv = getPV(tagString);
        PlayerView finalPV = pv;

        if (cp != null) {
            final String scoreText = cp.getScore() == 9001 ?
                    "max" : String.valueOf(cp.getScore());

            acti.runOnUiThread(() -> {
                decorate(finalPV.score, scoreText, size[0]);
                decorate(finalPV.troph, cp.getTrophies(), size[1]);
                decorate(finalPV.tag, "#" + tagString, size[1],
                        Color.WHITE, maxy[1]);
                finalPV.frame.setVisibility(View.VISIBLE);
            });
        }

        if (ps != null) {
            final String normaText = (int) (ps.getNorma() * 100) + "%";
            final String ratioText = (int) (ps.getRatio() * 100) + "%";

            acti.runOnUiThread(() -> {
                decorate(finalPV.norma, normaText, size[0]);
                decorate(finalPV.wars, ps.getWars(), size[1]);
                decorate(finalPV.missed, ps.getMissed(), size[1]);
                decorate(finalPV.ratio, ratioText, size[1]);
                decorate(finalPV.name, ps.getName(), size[0],
                        Color.WHITE, maxy[0]);
                decorate(finalPV.tag, "#" + tagString, size[1],
                        Color.WHITE, maxy[1]);
                finalPV.frame.setVisibility(View.VISIBLE); });
        }
    }


    private PlayerView getPV(String tag) {
        int pvNum = PlayerMap.getInstance()
                .size() - 1 - playerMap.size();
        pvNum = max(0,min(49, pvNum));
        PlayerView pv =
                playerView[pvNum];
        playerMap.put(tag, pv);
        return pv;
    }

    public void refreshList(final int choice) {
        getFixedPool().execute(() -> {
            PlayerMap pm = PlayerMap.getInstance();
            setLoading(true);
            clearList();

            List tempList;
            if(choice == 3) {
                List<PlayerStats> tempPS = new ArrayList<>();
                for (Map.Entry<String, Pair<ClanPlayer, PlayerStats>>
                        player : pm.getAll().entrySet())
                    if (player.getValue().first != null)
                        tempPS.add(player.getValue().second);
                Collections.sort(tempPS, new SortByRatio());
                tempList = tempPS;
            } else {
                List<ClanPlayer> tempCP = new ArrayList<>();
                for (Map.Entry<String, Pair<ClanPlayer, PlayerStats>>
                        player : pm.getAll().entrySet())
                    if (player.getValue().first != null)
                        tempCP.add(player.getValue().first);

                if (choice == 1) Collections.sort(tempCP, new SortByTrophies());
                else if (choice == 2) Collections.sort(tempCP, new SortByScore());
                tempList = tempCP;
            }

            for (int c = min(tempList.size() - 1,49); c >= 0; c--) {
                String tag; SystemClock.sleep(20);
                if (tempList.get(c) instanceof PlayerStats)
                    tag = ((PlayerStats) tempList.get(c)).getTag();
                else tag = ((ClanPlayer) tempList.get(c)).getTag();
                Pair<ClanPlayer, PlayerStats> pair = pm.get(tag);
                displayStats(pair.second, pair.first); }
            setLoading(false); });
    }

    public boolean getLoading() { return loading; }
    public void setLoading(final boolean loading) {
        acti.runOnUiThread(() -> {
            if (loading && !this.loading) {
                sortRatio.setEnabled(false);
                sortScore.setEnabled(false);
                sortTroph.setEnabled(false);
                loadingAnim.setEnabled(false);
                loadingAnim.setColorFilter(null);
                loadView.setVisibility(View.VISIBLE);
                decorate(loadView, "Loading...", size[0]);
                loadingAnim.setImageResource(R.drawable.loading);
                sortRatio.setColorFilter(Color.argb(100,200,200,200));
                sortScore.setColorFilter(Color.argb(100,200,200,200));
                sortTroph.setColorFilter(Color.argb(100,200,200,200));
                animateView(loadingAnim, loading);
            } else if (!loading && this.loading){
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
                blinkView(acti.getTab(1), loading);

                final Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override public void run() {
                        final int refresh = 60 - acti.getLastForce(1);
                        if (refresh < 0) acti.runOnUiThread(() -> {
                            loadView.setVisibility(View.INVISIBLE);
                            loadingAnim.setColorFilter(null);
                            loadingAnim.setEnabled(true);
                            timer.cancel(); });
                        else acti.runOnUiThread(() -> {
                            loadView.setVisibility(View.VISIBLE);
                            decorate(loadView, refresh+"min", size[0]); });
                    }}, 0, 60000); }
        this.loading = loading; });
    }

    public void refreshInfo() { info.setVisibility(View.VISIBLE); }
    public void clearList() {
        playerMap.clear();
        acti.runOnUiThread(() -> {
            for (PlayerView pv : playerView) {
                View frame = pv.frame;
                if (frame != null)
                    frame.setVisibility(View.GONE); }});
    }

    @Override
    public void onAttach(Context context) {
        acti = (Interface) getActivity();
        playerMap = new HashMap<>();
        super.onAttach(context);
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
        info = new MagicTextView(getActivity());
        info.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        sortTroph.setOnClickListener(view -> { bounce(view, acti); selectSort(1); });
        sortScore.setOnClickListener(view -> { bounce(view, acti); selectSort(2); });
        sortRatio.setOnClickListener(view -> { bounce(view, acti); selectSort(3); });

        initFrames();
        return root;
    }

    private void initFrames() {
        final MagicTextView sort = root.findViewById(R.id.sort);

        acti.runOnUiThread(() -> {
        decorate(sort, "Sort by:", size[0]);
        decorate(loadView, "Loading...", size[0]);
        decorate(info, "\nStats are being loaded," +
                " please wait...\n", size[0]+2);
        lineage.addView(info); });

        for (int c = 0; c < playerView.length; c++) {
            final View frame = LayoutInflater.from(acti)
                    .inflate(R.layout.frame_warstats, null);

            playerView[c] = new PlayerView(frame);
            ImageView bgFrame = frame.findViewById(R.id.frameBG2);
            if (c%2==1) bgFrame.setImageResource(R.drawable.background_2);

            playerView[c].troph = frame.findViewById(R.id.trophies);
            playerView[c].missed = frame.findViewById(R.id.missed);
            playerView[c].score = frame.findViewById(R.id.score);
            playerView[c].norma = frame.findViewById(R.id.norma);
            playerView[c].cards = frame.findViewById(R.id.cards);
            playerView[c].ratio = frame.findViewById(R.id.ratio);
            playerView[c].name = frame.findViewById(R.id.name);
            playerView[c].wars = frame.findViewById(R.id.wars);
            playerView[c].tag = frame.findViewById(R.id.tag);
            frame.setVisibility(View.GONE);

            final int finalC = c;
            final MagicTextView rank = frame.findViewById(R.id.rank);
            acti.runOnUiThread(() -> {
                decorate(rank, finalC+1+"",size[0]+2);
                decorate(playerView[finalC].score, "???", size[0]);
                decorate(playerView[finalC].norma, "???", size[0]);
                decorate(playerView[finalC].troph, "???", size[1]);
                decorate(playerView[finalC].wars, "???", size[1]);
                decorate(playerView[finalC].missed, "???", size[1]);
                decorate(playerView[finalC].cards, "???", size[1]);
                decorate(playerView[finalC].ratio, "???",size[1]);
                decorate(playerView[finalC].name, "???", size[0],
                        Color.WHITE, maxy[0]);
                decorate(playerView[finalC].tag, "#???", size[1],
                        Color.WHITE, maxy[1]);
                lineage.addView(frame); });
        }

        rotate(root.findViewById(R.id.warSelection), acti);
        loadingAnim.setOnClickListener(v -> Service.getThread()
                .start(acti.getLastClan(),true, false));
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
        acti.runOnUiThread(() -> conSet.applyTo(warBar));
        if (choice > 0) refreshList(choice);
    }

    public ImageView getWifi() {
        if (wifi == null)
            wifi = root.findViewById(R.id.wifi);
        return wifi;
    }

    public void updateLoading(int cur, int max) {
        int percent = cur * 100 / max;
        acti.runOnUiThread(() ->
                decorate(loadView, "... " +
                percent + "%", size[0]));
    }

    private class SortByScore implements Comparator<ClanPlayer> {
        @Override public int compare(ClanPlayer a, ClanPlayer b) {
            return b.getScore() - a.getScore(); }}

    private class SortByTrophies implements Comparator<ClanPlayer> {
        @Override public int compare(ClanPlayer a, ClanPlayer b) {
            return b.getTrophies() - a.getTrophies(); }}

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

        private PlayerView(View frame) {
            this.frame = frame; }
    }
}
