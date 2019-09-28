package eu.rtsketo.sakurastats.main;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import eu.rtsketo.sakurastats.control.DAObject;
import eu.rtsketo.sakurastats.control.DataFetch;
import eu.rtsketo.sakurastats.control.DataRoom;
import eu.rtsketo.sakurastats.dbobjects.ClanPlayer;
import eu.rtsketo.sakurastats.dbobjects.PlayerStats;
import eu.rtsketo.sakurastats.hashmaps.PlayerMap;
import jcrapi.model.Member;
import jcrapi.model.TopClan;

import static eu.rtsketo.sakurastats.control.ThreadPool.getCachePool;
import static eu.rtsketo.sakurastats.control.ThreadPool.getFixedPool;
import static eu.rtsketo.sakurastats.hashmaps.SiteMap.clearPages;
import static eu.rtsketo.sakurastats.main.Interface.TAG;


public class Service {
    private static Service bth;
    private Interface acti;
    private Thread thread;
    private DataFetch df;

    private boolean force;
    private boolean stop;

    private Service(Interface acti) { this.acti = acti; }

    public static Service getThread() {
        if (bth == null)
            throw new NullPointerException(
                    "Service not initialized properly.");
        return bth; }

    public static Service getThread(Interface acti) {
        if (bth == null || !getThread().acti.equals(acti))
            bth = new Service(acti);
        return bth; }

    public synchronized void start(String tag, boolean force, boolean tab) {
        getCachePool().execute(() -> {
            if (thread != null)
                try { stop = true;
                    thread.join();
                    stop = false; }
                catch (InterruptedException e) {
                    Log.e(TAG, "Join failed", e);
                    thread.interrupt(); }

            this.force = force;
            df = new DataFetch(acti);
            thread = new Thread(()-> {
                List<TopClan> tp = new ArrayList<>();
                TopClan sakura = new TopClan();
                sakura.setName("Sakura Frontier");
                sakura.setTag("2YCJRUC");
                tp.add(sakura);

                if (tag != null)
                    collectData(tag);
                else if (acti.getLastClan() != null)
                    collectData(acti.getLastClan());
                else {
                    Console.logln(" \nChecking top clans...");
                    tp.addAll(df.getTopClans());
                    for (TopClan clan : tp) {
                        String cTag = clan.getTag();
                        Console.logln("\t\t\t\t\t\t-\t\t"
                                + clan.getName());
                        if (df.getClanWar(cTag).getState().equals("warDay")) {
                            acti.setLastClan(cTag);
                            collectData(cTag);
                            break;
                        }}}});

            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        });
    }

    private void collectData(String cTag) {
        PlayerMap pm = PlayerMap.getInstance();
        DAObject db = DataRoom.getInstance().getDao();
        acti.runOnUiThread(()->
            acti.getProgFrag().getConsole()
                    .setVisibility(View.VISIBLE));
        acti.getProgFrag().removeViews();
        clearPages();

        List<Member> members;
        if (acti.getLastUse(cTag) || force)
            members = df.getMembers(cTag);
        else {
            members = new ArrayList<>();
            for (PlayerStats player : db.getClanPlayerStats(cTag)) {
                Member member = new Member();
                member.setName(player.getName());
                member.setTag(player.getTag());
                members.add(member);
            }
        }

        pm.reset(members.size());
        CountDownLatch psLatch = new CountDownLatch(members.size());
        List<PlayerStats> ps = Collections.synchronizedList(new ArrayList<>());
        List<ClanPlayer> cp = Collections.synchronizedList(new ArrayList<>());

        Console.logln(" \nFetching clan members...");
        for (Member member : members)
            getFixedPool().execute(()-> {
                if (!stop && member != null) {
                    PlayerStats playerStats = df.getPlayerStats(cTag, member, force);
                    pm.put(member.getTag(), playerStats);
                    ps.add(playerStats);
                } psLatch.countDown();
                updateLoading((int) (members.size() - psLatch.getCount()), members.size()); });
        waitLatch(psLatch);

        Collections.reverse(ps);
        Console.logln(" \nFetching member stats...");
        CountDownLatch cpLatch = new CountDownLatch(ps.size());
        for (final PlayerStats playerStats : ps)
            getFixedPool().execute(()->{
                if (!stop && playerStats != null) {
                    String pTag = playerStats.getTag();
                    ClanPlayer clanPlayer = df.getPlayerProfile(pTag, force);
                    Console.logln("\t\t" +
                            Console.convertRole((clanPlayer.getRole()))
                            + "\t\t" + playerStats.getName());
                    pm.put(pTag, clanPlayer);
                    cp.add(clanPlayer);
                } cpLatch.countDown();
                updateLoading((int) (ps.size()*2 - cpLatch.getCount()), ps.size());
            });
        waitLatch(cpLatch);

        acti.getWarFrag().setLoading(false);
        Console.logln(" \nFetching member activity...");
        CountDownLatch acLatch = new CountDownLatch(cp.size());

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm");
        for (final ClanPlayer tempPlayer : cp)
            getFixedPool().execute(()->{
                if (!stop && tempPlayer != null) {
                    String pTag = tempPlayer.getTag();
                    ClanPlayer clanPlayer = df.getMemberActivity(tempPlayer, force);
                    Console.logln("\t\t" +
                            sdf.format(new Date(clanPlayer.getLast()*1000))
                            + "\t\t" + clanPlayer.getTag());
                    pm.put(pTag, clanPlayer);
                } acLatch.countDown();
                updateLoading((int) (ps.size()*3 - acLatch.getCount()), ps.size());
            });
        waitLatch(acLatch);
        pm.completeSubs();

        acti.getProgFrag().refresh(false);
        acti.getSettiFrag().refreshStored();
        acti.incUseCount();
    }

    private void waitLatch(CountDownLatch latch) {
        try { latch.await(); }
        catch (InterruptedException e) {
            Log.e(TAG, "Latch failed", e);
            Thread.currentThread().interrupt(); }
    }

    private void updateLoading(int cur, int max) {
        int warMax = 2 * max;
        int actMax = 3 * max;

        if (cur < warMax)
            acti.getWarFrag().updateLoading(cur, warMax);
        acti.getActiFrag().updateLoading(cur, actMax);
    }
}
