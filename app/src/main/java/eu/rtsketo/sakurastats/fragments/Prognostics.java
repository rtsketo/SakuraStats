package eu.rtsketo.sakurastats.fragments;


import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.qwerjk.better_text.MagicTextView;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import eu.rtsketo.sakurastats.R;
import eu.rtsketo.sakurastats.control.DataFetch;
import eu.rtsketo.sakurastats.control.DataRoom;
import eu.rtsketo.sakurastats.dbobjects.ClanStats;
import eu.rtsketo.sakurastats.main.Interface;

import static eu.rtsketo.sakurastats.control.ThreadPool.getCachePool;
import static eu.rtsketo.sakurastats.control.ViewDecor.animateView;
import static eu.rtsketo.sakurastats.control.ViewDecor.blinkView;
import static eu.rtsketo.sakurastats.control.ViewDecor.decorate;
import static eu.rtsketo.sakurastats.hashmaps.SDPMap.sdp2px;


public class Prognostics extends Fragment {
    private ImageView loadingAnim,
            loadingOp, wifi, wifiOp;
    private MagicTextView loadView;
    private LinearLayout lineage;
    private boolean loading;
    private Interface acti;
    private View root;

    public Prognostics() { /* Needed for FragmentManager */ }

    @Override
    public void onAttach(Context context) {
        acti = (Interface) getActivity();
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_prognose, container, false);
        loadView = root.findViewById(R.id.loading);
        loadingOp = root.findViewById(R.id.loadingOp);
        lineage = root.findViewById(R.id.warClanList);
        loadingAnim = root.findViewById(R.id.loadingAnim);

        decorate(loadView, "Loading...", 10);
        ImageView bgWaves = root.findViewById(R.id.bgWaves);
        ((AnimationDrawable)bgWaves.getDrawable()).start();

        loadingAnim.setOnClickListener(v -> refresh(true));
        return root;
    }

    public void refresh(final boolean force) {
        getCachePool().execute(() -> {
            setLoading(true);
            String tag = acti.getLastClan();
            if (force || acti.getLastUse(tag)) {
                List<ClanStats> cs =
                        new DataFetch(acti)
                                .getClanStats(tag);
                setStats(cs);
                acti.setLastForce(0);
                if (cs.size() == 5)
                    acti.runOnUiThread(() ->
                        acti.changeTabTo(0)); }
            else setStats(DataRoom.getInstance().getDao()
                    .getClanStatsList(tag)); });
    }

    public void setLoading(final boolean loading) {
        final int size = sdp2px(10);
            if (loading && !this.loading)
                acti.runOnUiThread(() -> {
                    loadingAnim.setEnabled(false);
                    loadingAnim.setColorFilter(null);
                    loadView.setVisibility(View.VISIBLE);
                    loadingOp.setVisibility(View.VISIBLE);
                    loadingAnim.setVisibility(View.VISIBLE);
                    decorate(loadView, "Loading...", size);
                    loadingAnim.setImageResource(R.drawable.loading);
                    animateView(loadingAnim, true);
                    blinkView(acti.getTab(0), true); });

            else if (!loading && this.loading) {
                acti.runOnUiThread(() -> {
                    animateView(loadingAnim, false);
                    blinkView(acti.getTab(0), false);
                    loadingAnim.setImageResource(R.drawable.refresh);
                    loadingAnim.setColorFilter(Color.argb(100, 200, 200, 200));
                });

                final Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override public void run() {
                        final int refresh = 15 - acti.getLastForce(0);
                        if (refresh < 0) acti.runOnUiThread(() -> {
                            loadView.setVisibility(View.INVISIBLE);
                            loadingOp.setVisibility(View.INVISIBLE);
                            loadingAnim.setColorFilter(null);
                            loadingAnim.setEnabled(true);
                            timer.cancel(); });
                        else acti.runOnUiThread(() -> {
                            loadView.setVisibility(View.VISIBLE);
                            decorate(loadView, "     "+refresh+"min", size); });
                    }}, 0, 60000); }
        this.loading = loading;
    }


    public void setStats(List<ClanStats> clans) {
        removeViews();
        if (clans.size()==5) {
            int counter = 0;
            ClanStats[] stats = new ClanStats[5];
            for (ClanStats clan : clans)
                stats[counter++] = clan;

            Arrays.sort(stats, new SortByPrediction());
            for (ClanStats clan : stats)
                addClan(clan);

        } else {
            final MagicTextView info = new MagicTextView(acti);
            final MagicTextView more = new MagicTextView(acti);
            ViewGroup.LayoutParams params = new ViewGroup
                    .LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            info.setLayoutParams(params); more.setLayoutParams(params);
            decorate(info, "The clan isn't currently in War\n\n\n\n\n\n\n\n\n",sdp2px(10));
            decorate(more, "Stats can't be refreshed in less than 15min",sdp2px(8));
            info.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            more.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            acti.runOnUiThread(() -> {
                lineage.addView(info);
                lineage.addView(more); });
        }

        setLoading(false);
    }

    private void removeViews() {
        acti.runOnUiThread(() -> lineage.removeAllViews()); }

    private void addClan(ClanStats clan) {
        int predictWins = clan.getEstimatedWins();
        int plusOne = Math.round(((float)clan.getExtraWins() * 100));

        final View frame = LayoutInflater.from(acti)
                .inflate(R.layout.frame_prognose, null);
        MagicTextView name = frame.findViewById(R.id.clanName);
        MagicTextView wins = frame.findViewById(R.id.actualWins);
        MagicTextView pred = frame.findViewById(R.id.predictWins);
        MagicTextView tag = frame.findViewById(R.id.clanTag);
        MagicTextView remain = frame.findViewById(R.id.remaining);
        MagicTextView crown = frame.findViewById(R.id.clanCrown);
        MagicTextView pone = frame.findViewById(R.id.clanPlusOne);
        MagicTextView troph = frame.findViewById(R.id.clanTrophies);
        ImageView badge = frame.findViewById(R.id.clanBadge);

        int[] maxy = new int[] {104, 52};
        decorate(name, clan.getName(), sdp2px(9), Color.WHITE, maxy[0]);
        decorate(tag, "#"+clan.getTag(), sdp2px(6), Color.WHITE, maxy[1]);
        decorate(wins, clan.getActualWins(), sdp2px(9));
        decorate(pred, predictWins, sdp2px(9));
        decorate(remain, clan.getRemaining(), sdp2px(6));
        decorate(crown, clan.getCrowns(), sdp2px(6));
        decorate(pone, plusOne + "%", sdp2px(6));
        decorate(troph, clan.getWarTrophies(), sdp2px(6),
                Color.rgb(240, 179, 255));

        badge.setImageResource(acti.getResources().getIdentifier(
                clan.getBadge(),"drawable", acti.getPackageName()));

        acti.runOnUiThread(() -> lineage.addView(frame));
    }

    public Pair<ImageView, ImageView> getWifi() {
        if (wifi == null)
            wifi = root.findViewById(R.id.wifi);
        if (wifiOp == null)
            wifiOp = root.findViewById(R.id.wifiOp);
        return new Pair<>(wifi, wifiOp);
    }

    class SortByPrediction implements Comparator<ClanStats> {
        @Override public int compare(ClanStats a, ClanStats b) {
            return (int) ((b.getEstimatedWins()+b.getExtraWins())*100
                    - (a.getEstimatedWins()+a.getExtraWins())*100);
        }
    }
}
