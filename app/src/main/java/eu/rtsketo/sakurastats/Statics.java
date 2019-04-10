package eu.rtsketo.sakurastats;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.ImageView;

import com.qwerjk.better_text.MagicTextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import io.reactivex.subjects.PublishSubject;
import jcrapi.model.Member;

import static android.support.v4.content.ContextCompat.startActivity;
import static eu.rtsketo.sakurastats.APIControl.checkClan;
import static eu.rtsketo.sakurastats.APIControl.getMemberActivity;
import static eu.rtsketo.sakurastats.APIControl.getMembers;
import static eu.rtsketo.sakurastats.APIControl.getPlayerProfile;
import static eu.rtsketo.sakurastats.APIControl.getPlayerStats;
import static eu.rtsketo.sakurastats.APIControl.sleep;
import static eu.rtsketo.sakurastats.Interface.getDimensions;
import static eu.rtsketo.sakurastats.Interface.getLastClan;
import static eu.rtsketo.sakurastats.Interface.getLastUse;
import static eu.rtsketo.sakurastats.Interface.incUseCount;
import static eu.rtsketo.sakurastats.Interface.setLastClan;
import static eu.rtsketo.sakurastats.Interface.setStoredClan;
import static java.lang.Math.max;

public class Statics {
    private static Typeface tf;
    private static int clanSize;
    public static boolean checkInput;
    private static BackThread backThread;
    private static SparseIntArray sdpMap;
    private static AppCompatActivity activity;
    private static Pair<Integer, Integer> dims;
    private static ThreadPoolExecutor fixedPool;
    private static ThreadPoolExecutor cachePool;
    private static List<Pair<Integer, Integer>> leagueMap;
    private static ConcurrentMap<String, Pair<ClanPlayer, PlayerStats>> playerMap;
    private static PublishSubject<ClanPlayer> cpSub;
    private static PublishSubject<PlayerStats> psSub;

    public enum SakuraDialog {INPUT, INFO, CLANQUEST, RATEQUEST}
    public static void setActi(AppCompatActivity act) {
        activity = act;
    }
    public static void setTf(Typeface tf) {
        Statics.tf = tf;
    }
    public static final String agent = "Mozilla/5.0 " +
            "(Linux; Android 6.0; Nexus 5 Build/MRA58N) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/64.0.3282.186 Mobile Safari/537.36";

    public static int getClanSize() { return clanSize; }
    public static boolean hasPlayerMap() { return playerMap != null; }
    public static ConcurrentMap<String, Pair<ClanPlayer, PlayerStats>> getPlayerMap() {
        return playerMap; }

    public static void putPlayerMap(String tag, Object player) {
        if (playerMap == null) resetPlayerMap();
        Pair<ClanPlayer, PlayerStats> pair = playerMap.get(tag);

        if (pair == null)
            pair = new Pair<>(new ClanPlayer(), new PlayerStats());

        if (player instanceof ClanPlayer) {
            pair = new Pair<>((ClanPlayer) player, pair.second);
            cpSub.onNext((ClanPlayer) player);
        } else {
            pair = new Pair<>(pair.first, (PlayerStats) player);
            psSub.onNext((PlayerStats) player);
        }

        playerMap.put(tag, pair);
    }

    public static void resetPlayerMap() {
        if (hasPlayerMap()) playerMap.clear();
        else playerMap = new ConcurrentHashMap<>(clanSize);

        psSub = PublishSubject.create();
        cpSub = PublishSubject.create();
        cpSub.subscribe(getWarFrag().cpObserver());
        psSub.subscribe(getWarFrag().psObserver());
        cpSub.subscribe(getActiFrag().cpObserver());
        psSub.subscribe(getActiFrag().psObserver());
    }

    private static void completeSubs() {
        cpSub.onComplete();
        psSub.onComplete();
    }

    public static void updatePlayerMap(boolean force) {
        getCachePool().execute(() ->{
            GeneralDao db = DBControl.getDB().getDao();
            String cTag = getLastClan();

            List<Member> members;
            if (getLastUse(cTag) || force)
                 members = getMembers(cTag);
            else {
                members = new ArrayList<>();
                for (PlayerStats player : db.getClanPlayerStats(cTag)) {
                    Member member = new Member();
                    member.setName(player.getName());
                    member.setTag(player.getTag());
                    members.add(member);
                }
            }

            clanSize = members.size();
            resetPlayerMap();

            CountDownLatch psLatch = new CountDownLatch(members.size());
            List<PlayerStats> ps = new ArrayList<>();
            List<ClanPlayer> cp = new ArrayList<>();
            db.resetCurrentPlayers(cTag);

            for (Member member : members)
                getFixedPool().execute(()-> {
                    if (getBackThread().getApproval()) {
                        PlayerStats playerStats = getPlayerStats(cTag, member, force);
                        putPlayerMap(member.getTag(), playerStats);
                        ps.add(playerStats);
                    } psLatch.countDown();
                    updateLoading((int) (members.size() - psLatch.getCount()),
                            members.size()*3); });
            waitLatch(psLatch);

            Collections.reverse(ps);
            CountDownLatch cpLatch = new CountDownLatch(ps.size());
            for (final PlayerStats playerStats : ps)
                getFixedPool().execute(()->{
                    if (getBackThread().getApproval()) {
                        String pTag = playerStats.getTag();
                        ClanPlayer clanPlayer = getPlayerProfile(pTag, force);
                        putPlayerMap(pTag, clanPlayer);
                        cp.add(clanPlayer);
                    } cpLatch.countDown();
                    updateLoading((int) (ps.size()*2 - cpLatch.getCount()),
                            ps.size()*3); });
            waitLatch(cpLatch);

            CountDownLatch acLatch = new CountDownLatch(cp.size());
            for (final ClanPlayer tempPlayer : cp)
                getFixedPool().execute(()->{
                    if (getBackThread().getApproval()) {
                        String pTag = tempPlayer.getTag();
                        ClanPlayer clanPlayer = getMemberActivity(tempPlayer, force);
                        putPlayerMap(pTag, clanPlayer);
                    } acLatch.countDown();
                    updateLoading((int) (ps.size()*3 - acLatch.getCount()),
                            ps.size()*3); });
            waitLatch(acLatch);
            completeSubs();
            incUseCount();
        });
    }

    private static void updateLoading(int cur, int max) {
        getWarFrag().updateLoading(cur, max);
        getActiFrag().updateLoading(cur, max);
    }

    public static void waitLatch(CountDownLatch latch) {
        try { latch.await(); }
        catch (InterruptedException e) { e.printStackTrace(); }
    }

    private static synchronized Pair<Integer, Integer> getDims() {
        if (dims == null) {
            while (getDimensions() == null)
                sleep(500);
            dims = getDimensions();
        }
        return dims;
    }


    public static AppCompatActivity getActi() {
        while (activity == null)
            sleep(500);
        return activity;
    }

    public static ProgFrag getProgFrag() {
        while (getActi().getSupportFragmentManager().getFragments().size() < 1) sleep(500);
        return (ProgFrag) getActi().getSupportFragmentManager().getFragments().get(0);
    }

    public static WarFrag getWarFrag() {
        while (getActi().getSupportFragmentManager().getFragments().size() < 2) sleep(500);
        return (WarFrag) getActi().getSupportFragmentManager().getFragments().get(1);
    }

    public static ActiFrag getActiFrag() {
        while (getActi().getSupportFragmentManager().getFragments().size() < 3) sleep(500);
        return (ActiFrag) getActi().getSupportFragmentManager().getFragments().get(2);
    }

    public static SettiFrag getSettiFrag() {
        while (getActi().getSupportFragmentManager().getFragments().size() < 4) sleep(500);
        return (SettiFrag) getActi().getSupportFragmentManager().getFragments().get(3);
    }

    public static ThreadPoolExecutor getFixedPool() {
        if (fixedPool == null) fixedPool = (ThreadPoolExecutor)
                Executors.newFixedThreadPool( 8);
        return fixedPool;
    }

    public static ThreadPoolExecutor getCachePool() {
        if (cachePool == null) cachePool = (ThreadPoolExecutor)
                Executors.newCachedThreadPool();
        return cachePool;
    }

    public static void animateView(ImageView view, boolean animate) {
        Drawable anim = view.getDrawable();
        if (anim instanceof AnimationDrawable)
            if (animate) ((AnimationDrawable) anim).start();
            else ((AnimationDrawable) anim).stop();
    }

    public static void blinkView(ImageView view, boolean blink) {
        if (blink) {
            Animation animation = new AlphaAnimation(1, .5f);
            animation.setDuration(400);
            animation.setInterpolator(new LinearInterpolator());
            animation.setRepeatCount(Animation.INFINITE);
            animation.setRepeatMode(Animation.REVERSE);
            view.startAnimation(animation); }
        else view.clearAnimation(); }

    public static MagicTextView decorate(MagicTextView tv, String txt, float size) {
        return decorate(tv, txt, size, Color.WHITE); }

    public static MagicTextView decorate(MagicTextView tv, int txt, float size) {
        return decorate(tv, ""+txt, size, Color.WHITE); }

    public static MagicTextView decorate(MagicTextView tv, double txt, float size) {
        return decorate(tv, ""+txt, size, Color.WHITE); }

    public static MagicTextView decorate(MagicTextView tv, int txt, float size, int color) {
        return decorate(tv, ""+txt, size, color); }

    public static MagicTextView decorate(MagicTextView tv, double txt, float size, int color) {
        return decorate(tv, ""+txt, size, color); }

    public static EditText decorate(EditText et, int size) {
        et.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        et.setTextColor(Color.BLACK);
        et.setTypeface(tf);
        return et;
    }

    public static MagicTextView decorate(MagicTextView tv, String txt, float size, int color) {
        return decorate(tv, txt, size, color, -1); }

    public static MagicTextView decorate(MagicTextView tv, String txt,
                                         float size, int color, int maxWidth) {
        tv.clearOuterShadows();
        tv.setText(txt);
        tv.setTypeface(tf);
        tv.setTextColor(color);
        tv.setStroke(max(sdp2px(1)-2,.8f), Color.BLACK);
        tv.addOuterShadow(max(sdp2px(1)-2,.8f),
                max(sdp2px(1)-2,.8f),max(sdp2px(2)-2,.8f),Color.BLACK);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        tv.measure(0,0);

        if (maxWidth > 0) {
            float width = tv.getMeasuredWidth();
            if (width > sdp2px(maxWidth))
                return decorate(tv, txt,
                        (float) (size - .5), color, maxWidth);
        }

        return tv;
    }


    public static BackThread getBackThread() {
        if (backThread == null)
            backThread = new BackThread();
        return backThread; }

    public static void startChecking(final EditText input) {
        getCachePool().execute(new Runnable() {
            @Override public void run() {
                String clanTagInput = "";
                checkInput = true;
                while (checkInput) {
                    String tag = input.getText().toString();
                    if (!clanTagInput.equals(tag)) {
                        tag = "#"+tag.toUpperCase()
                                .replace("O", "0")
                                .replaceAll("[^0289CGJLPQRUVY]", "");
                        clanTagInput = tag;
                        final String finalTag = tag;
                        getActi().runOnUiThread(new Runnable() {
                            @Override public void run() {
                                input.setText(finalTag);
                                input.setSelection(finalTag.length()); }});
                    } sleep(1000); }}});
    }

    public static void bounce(View view) {
        view.startAnimation(AnimationUtils.loadAnimation(getActi(), R.anim.bounce)); }
    public static void rotate(View view) {
        view.startAnimation(AnimationUtils.loadAnimation(getActi(), R.anim.rotate)); }

    static void createDialog(SakuraDialog type, final Dialog prevDia) {
        createDialog(type, prevDia, -1); }
    static void createDialog(SakuraDialog type, int count) {
        createDialog(type, null, count);
    }
    static void createDialog(SakuraDialog type) { createDialog(type, null, -1); }
    private static void createDialog(SakuraDialog type, final Dialog prevDia, final int count) {
        final View inputView;
        switch (type) {
            case INPUT:
                inputView = LayoutInflater.from(getActi())
                        .inflate(R.layout.dialog_input, null);
                break;
            default:
                inputView = LayoutInflater.from(getActi())
                        .inflate(R.layout.dialog_quest, null); }

        final Dialog dialog = new Dialog(getActi());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawableResource(
                android.R.color.transparent);
        dialog.setContentView(inputView);
        dialog.setCancelable(false);

        final MagicTextView explain = dialog.findViewById(R.id.inputExplaination);
        final MagicTextView explain2 = dialog.findViewById(R.id.inputExplaination2);
        final ImageView confirm = dialog.findViewById(R.id.inputConfirmButton);
        final ImageView cancel = dialog.findViewById(R.id.inputCancelButton);
        final EditText input = dialog.findViewById(R.id.inputClanTag);

        int size[] = { sdp2px(9), sdp2px(8) };
        switch (type) {
            case INPUT:
                final ImageView loadingAnim = dialog.findViewById(R.id.loadingAnim);
                if (count > 0) decorate(explain,"Enter a", size[0]);
                else decorate(explain, "Enter your", size[0]);
                decorate(explain2,"clan's tag!", size[0]);
                decorate(input, size[0]);
                startChecking(input);

                confirm.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        bounce(view);
                        loadingAnim.setVisibility(View.VISIBLE);
                        String tag = input.getText().toString();
                        tag = tag.toUpperCase()
                                .replace("O", "0")
                                .replaceAll("[^0289CGJLPQRUVY]", "");
                        cancel.setEnabled(false);
                        confirm.setEnabled(false);
                        cancel.setColorFilter(Color
                                .argb(100,200,200,200));
                        confirm.setColorFilter(Color
                                .argb(100,200,200,200));
                        ((AnimationDrawable)loadingAnim.getDrawable()).start();
                        final String finalTag = tag;
                        getCachePool().execute(new Runnable() {
                            @Override public void run() {
                                if (checkClan(finalTag)) {
                                    if(count > -1) {
                                        setStoredClan(count, finalTag);
                                        getSettiFrag().selectClan(count);
                                        getSettiFrag().refreshStored(
                                                count,false);
                                    } else {
                                        setLastClan(finalTag);
                                        setStoredClan(0, finalTag);
                                        getBackThread().setClan(finalTag);
                                        getBackThread().startThread();
                                    }
                                    dialog.cancel();
                                    checkInput = false;
                                } else getActi().runOnUiThread(new Runnable() {
                                    @Override public void run() {
                                        ((AnimationDrawable)loadingAnim
                                                .getDrawable()).stop();
                                        createDialog(SakuraDialog.INFO);
                                        loadingAnim.setVisibility(View.INVISIBLE);
                                        confirm.setColorFilter(null);
                                        cancel.setColorFilter(null);
                                        confirm.setEnabled(true);
                                        cancel.setEnabled(true); }}); }}); }});

                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        bounce(view);
                        if (count > -1) dialog.cancel();
                        else createDialog(SakuraDialog.CLANQUEST, dialog); }});
                break;

            case CLANQUEST:
                decorate(explain, "Are you sure?", size[0]);
                decorate(explain2, "A random clan" +
                        "\nwill be chosen.", size[1]);

                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        bounce(view);
                        dialog.cancel(); }});

                confirm.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        bounce(view);
                        getBackThread().setRandom(true);
                        getBackThread().startThread();
                        dialog.cancel();
                        prevDia.cancel();
                        checkInput = false; }});
                break;

            case RATEQUEST:
                decorate(explain, "This appears only once!", size[0]);
                decorate(explain2, "\nSince you are using this" +
                        "\napp for sometime now," +
                        "\nplease consider rating it." +
                        "\n\n\nYour feedback is important!", size[1]);

                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        bounce(view);
                        dialog.cancel(); }});

                confirm.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        bounce(view);
                        Uri uri = Uri.parse("market://details?id=" + getActi().getPackageName());
                        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        try {
                            startActivity(getActi(), goToMarket, null);
                        } catch (ActivityNotFoundException e) {
                            startActivity(getActi(), new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("http://play.google.com/store/apps/details?id=" + getActi().getPackageName())), null);
                        }
                        dialog.cancel(); }});
                break;

            case INFO:
                dialog.setCancelable(true);
                cancel.setVisibility(View.GONE);
                decorate(explain, "Wrong tag!", size[0]);
                decorate(explain2, "Tag doesn't exist" +
                        "\nor bad connection.", size[1]);

                confirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        bounce(view);
                        dialog.cancel();
                    }
                });
        }
        dialog.show();
    }

    public static void badConnection() {
                getActi().runOnUiThread(new Runnable() {
                    @Override public void run() {
                        if (getWarFrag().getLoading())
                            getWarFrag().getWifi().setVisibility(View.VISIBLE);
                        if (getActiFrag().getLoading())
                            getActiFrag().getWifi().setVisibility(View.VISIBLE);
                        getProgFrag().getWifi().first.setVisibility(View.VISIBLE);
                        getProgFrag().getWifi().second.setVisibility(View.VISIBLE);
                    }
                });

                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override public void run() {
                        getActi().runOnUiThread(new Runnable() {
                            @Override public void run() {
                                getWarFrag().getWifi().setVisibility(View.INVISIBLE);
                                getActiFrag().getWifi().setVisibility(View.INVISIBLE);
                                getProgFrag().getWifi().first.setVisibility(View.INVISIBLE);
                                getProgFrag().getWifi().second.setVisibility(View.INVISIBLE);
                            }});}},1500); }

    public static int l2o(int league) {
        for (Pair<Integer, Integer> p : leagueMap)
            if (p.second == league) return p.first;
        return 1; }

    public static int o2l(int order) {
        for (Pair<Integer, Integer> p : leagueMap)
            if (p.first == order) return p.second;
        return 45; }

    static void initLeagueMap() {
        if (leagueMap == null) {
            leagueMap = new ArrayList<>();
            leagueMap.add(new Pair<>(1,45));
            leagueMap.add(new Pair<>(2,44));
            leagueMap.add(new Pair<>(3,43));
            leagueMap.add(new Pair<>(4,35));
            leagueMap.add(new Pair<>(5,34));
            leagueMap.add(new Pair<>(6,42));
            leagueMap.add(new Pair<>(7,33));
            leagueMap.add(new Pair<>(8,41));
            leagueMap.add(new Pair<>(9,25));
            leagueMap.add(new Pair<>(10,24));
            leagueMap.add(new Pair<>(11,32));
            leagueMap.add(new Pair<>(12,23));
            leagueMap.add(new Pair<>(13,31));
            leagueMap.add(new Pair<>(14,15));
            leagueMap.add(new Pair<>(15,14));
            leagueMap.add(new Pair<>(16,22));
            leagueMap.add(new Pair<>(17,13));
            leagueMap.add(new Pair<>(18,21));
            leagueMap.add(new Pair<>(19,12));
            leagueMap.add(new Pair<>(20,11));
        }
    }

    public static int sdp2px(int sdp) {
        if (sdp < 1) return -1;
        if (sdpMap == null) sdpMap = new SparseIntArray();
        int px = sdpMap.get(sdp, 1337);
        if (px == 1337) {
            String pack = getActi().getPackageName();
            Resources res = getActi().getResources();
            int id = res.getIdentifier("_" +
                    sdp + "sdp", "dimen", pack);
            px = res.getDimensionPixelSize(id);
            sdpMap.put(sdp, px); }
        return px; }

    public static int dp2px(float dp) {
        return Math.round(dp * Resources.getSystem().getDisplayMetrics().density); }

    public static float px2dp(float px) {
        return px / Resources.getSystem().getDisplayMetrics().density; }

    public static float sdp2dp(int sdp) {
        return px2dp(sdp2px(sdp)); }

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels; }

    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels; }
}

//    public static int dp2p(float dp) {
//        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
//                dp, Resources.getSystem().getDisplayMetrics());
//    }