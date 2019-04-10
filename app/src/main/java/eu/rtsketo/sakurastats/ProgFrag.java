package eu.rtsketo.sakurastats;


import android.graphics.Color;
import android.graphics.Typeface;
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

import static eu.rtsketo.sakurastats.APIControl.getClanStats;
import static eu.rtsketo.sakurastats.APIControl.sleep;
import static eu.rtsketo.sakurastats.DBControl.getDB;
import static eu.rtsketo.sakurastats.Interface.getLastClan;
import static eu.rtsketo.sakurastats.Interface.getLastForce;
import static eu.rtsketo.sakurastats.Interface.getLastUse;
import static eu.rtsketo.sakurastats.Interface.setLastForce;
import static eu.rtsketo.sakurastats.Statics.animateView;
import static eu.rtsketo.sakurastats.Statics.blinkView;
import static eu.rtsketo.sakurastats.Statics.decorate;
import static eu.rtsketo.sakurastats.Statics.getActi;
import static eu.rtsketo.sakurastats.Statics.getBackThread;
import static eu.rtsketo.sakurastats.Statics.getFixedPool;
import static eu.rtsketo.sakurastats.Statics.sdp2px;

public class ProgFrag extends Fragment {
    private ImageView loadingAnim,
            loadingOp, wifi, wifiOp;
    private MagicTextView loadView;
    private LinearLayout lineage;
    private Typeface tf;
    private View root;
    private boolean loaded;

    public boolean isLoaded() { return loaded; }
    public ProgFrag() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_prognose, container, false);
        tf = Typeface.createFromAsset(getResources().getAssets(),
                "fonts/Supercell-Magic_5.ttf");
        loadView = root.findViewById(R.id.loading);
        loadingOp = root.findViewById(R.id.loadingOp);
        lineage = root.findViewById(R.id.warClanList);
        loadingAnim = root.findViewById(R.id.loadingAnim);

        decorate(loadView, "Loading...", 10);
        ImageView bgWaves = root.findViewById(R.id.bgWaves);
        ((AnimationDrawable)bgWaves.getDrawable()).start();

        loadingAnim.setOnClickListener(v -> refresh(true));

        loaded = true;
        System.out.println("Prog, I'm loaded!");
        return root;
    }

    public void refresh(final boolean force) {
        getFixedPool().execute(() -> {
            setLoading(true);
            String tag = getLastClan();
            if (force) {
                boolean refresh = getLastUse(tag);
                List<ClanStats> cs = getClanStats(tag);
                if (refresh) { setStats(cs); setLastForce(0); }}
            else setStats(getDB().getDao()
                    .getClanStatsList(tag));
            setLoading(false); });
    }

    public void setLoading(final boolean loading) {
        final int size = sdp2px(10);
        getActi().runOnUiThread(() -> {
            if (!loading || getBackThread().getGUI())
                blinkView(((Interface)getActi())
                    .getTab(0), loading);

            if (loading) {
                loadingAnim.setEnabled(false);
                loadingAnim.setColorFilter(null);
                loadView.setVisibility(View.VISIBLE);
                loadingOp.setVisibility(View.VISIBLE);
                loadingAnim.setVisibility(View.VISIBLE);
                decorate(loadView, "Loading...", size);
                loadingAnim.setImageResource(R.drawable.loading);
                animateView(loadingAnim, loading);
            } else {
                animateView(loadingAnim, loading);
                loadingAnim.setImageResource(R.drawable.refresh);
                loadingAnim.setColorFilter(Color.argb(100,200,200,200));

                final Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override public void run() {
                        final int refresh = 15 - getLastForce(0);
                        if (refresh < 0) getActi().runOnUiThread(new Runnable() {
                            @Override public void run() {
                                loadView.setVisibility(View.INVISIBLE);
                                loadingOp.setVisibility(View.INVISIBLE);
                                loadingAnim.setColorFilter(null);
                                loadingAnim.setEnabled(true);
                                timer.cancel(); }});
                        else getActi().runOnUiThread(new Runnable() {
                            @Override public void run() {
                                loadView.setVisibility(View.VISIBLE);
                                decorate(loadView, "     "+refresh+"min", size); }});
                    }}, 0, 60000); }});
    }

    public void setStats(List<ClanStats> clans) {
        removeViews();
        if (clans.size()==5) {
            int counter = 0;
            ClanStats[] stats = new ClanStats[5];
            for (ClanStats clan : clans) {
                if (counter == 5)
                    for (ClanStats clana : clans)
                        System.out.println(clana.getName() + ": " + clana.getTag());
                else stats[counter++] = clan;
            }
            Arrays.sort(stats, new SortByPrediction());
            for (ClanStats clan : stats)
                addClan(clan);
        } else {
            final MagicTextView info = new MagicTextView(getActi());
            final MagicTextView more = new MagicTextView(getActi());
            ViewGroup.LayoutParams params = new ViewGroup
                    .LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            info.setLayoutParams(params); more.setLayoutParams(params);
            decorate(info, "The clan isn't currently in War\n\n\n\n\n\n\n\n\n",sdp2px(10));
            decorate(more, "Stats can't be refreshed in less than 15min",sdp2px(8));
            info.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            more.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            getActi().runOnUiThread(() -> {
                lineage.addView(info);
                lineage.addView(more); });
        }
    }

    private void removeViews() {
        while (lineage == null) sleep(500);
        getActi().runOnUiThread(() -> lineage.removeAllViews()); }

    private void addClan(ClanStats clan) {
        int predictWins = clan.getEstimatedWins();
        int plusOne = Math.round(((float)clan.getExtraWins() * 100));

        final View frame = LayoutInflater.from(getActi())
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

        badge.setImageResource(getActi().getResources().getIdentifier(
                clan.getBadge(),"drawable", getActi().getPackageName()));

        getActi().runOnUiThread(() -> lineage.addView(frame));
    }

    public Pair<ImageView, ImageView> getWifi() {
        if (wifi == null)
            wifi = root.findViewById(R.id.wifi);
        if (wifiOp == null)
            wifiOp = root.findViewById(R.id.wifiOp);
        return new Pair<>(wifi, wifiOp);
    }

    class SortByPrediction implements Comparator<ClanStats> {
        @Override
        public int compare(ClanStats a, ClanStats b) {
            return (int) ((b.getEstimatedWins()+b.getExtraWins())*100
                    - (a.getEstimatedWins()+a.getExtraWins())*100);
        }
    }
}
