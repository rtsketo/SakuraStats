package eu.rtsketo.sakurastats;

import java.util.ArrayList;
import java.util.List;

import jcrapi.model.TopClan;

import static eu.rtsketo.sakurastats.APIControl.clearPages;
import static eu.rtsketo.sakurastats.APIControl.getClanStats;
import static eu.rtsketo.sakurastats.APIControl.getClanWar;
import static eu.rtsketo.sakurastats.APIControl.getTopClans;
import static eu.rtsketo.sakurastats.APIControl.sleep;
import static eu.rtsketo.sakurastats.Interface.getLastClan;
import static eu.rtsketo.sakurastats.Interface.getLastUse;
import static eu.rtsketo.sakurastats.Interface.setLastClan;
import static eu.rtsketo.sakurastats.Interface.setLastForce;
import static eu.rtsketo.sakurastats.Statics.getActi;
import static eu.rtsketo.sakurastats.Statics.getActiFrag;
import static eu.rtsketo.sakurastats.Statics.getFixedPool;
import static eu.rtsketo.sakurastats.Statics.getProgFrag;
import static eu.rtsketo.sakurastats.Statics.getSettiFrag;
import static eu.rtsketo.sakurastats.Statics.getWarFrag;
import static eu.rtsketo.sakurastats.Statics.resetPlayerMap;
import static eu.rtsketo.sakurastats.Statics.updatePlayerMap;

public class BackThread implements Runnable {
    private GeneralDao db = DBControl.getDB().getDao();
    private final List<TopClan> tp = new ArrayList<>();
    private boolean rand = false;
    private boolean stop = false;
    private boolean lock = false;
    private boolean gui = true;
    private Thread clanThread;
    private Thread thread;
    private String tag;

    @Override
    public void run() {
        TopClan sakura = new TopClan();
        sakura.setTag("2YCJRUC");
        tp.add(sakura);
        stop = false;
        lock = true;

        if (tag == null) tag = getLastClan();
                getProgFrag().setLoading(true);
                getActiFrag().setLoading(true);
                getWarFrag().setLoading(true);

        if (rand) {
            if (tp.size() < 5)
                tp.addAll(getTopClans());
            startClanThread();
        } else readData();
        gui = true;
    }

    public boolean getGUI() { return gui; }
    public void setGUI(boolean gui) { this.gui = gui; }
    public void setClan(String tag) { rand = false; this.tag = tag; }
    public void startThread() { killThread(); getThread().start(); }
    public void setRandom(boolean rand) { this.rand = rand; }
    public void setLock(boolean lock) { this.lock = lock; }
    public boolean getApproval() { return !stop; }

    private void startClanThread() {
        lock = false;
        killThread();
        while (clanThread.isAlive())
            sleep(500);
        stop = false;
        if (clanThread != null)
            clanThread = null;
        getClanThread().start();
        lock = true;
    }

    private Thread getClanThread() {
        if (clanThread == null)
            clanThread = new Thread(() -> {
                for (TopClan clan : tp)
                    if(getApproval()) {
                        tag = clan.getTag();
                        if (getClanWar(tag).getState().equals("warDay")) {
                            setLastClan(tag); readData(); break; }}});
        return clanThread; }

    private void killThread() {
        stop = true;
        while (getClanThread().isAlive() || lock ||
                getFixedPool().getActiveCount() > 0)
            sleep(500);
        thread = null; }

    Thread getThread() {
        if (thread == null)
            thread = new Thread(this);
        return thread;
    }

    private void readData() {
        clearPages();
        resetPlayerMap();
        updatePlayerMap(false);
        getActi().runOnUiThread(() -> {
            ((Interface)getActi()).changeTabTo(1); });

        boolean refresh = getLastUse(tag);
        List<ClanStats> cs = null;
        if (getApproval()) cs = getClanStats(tag);
        final int csSize = cs.size();

        sleep(1500);
        if (refresh && gui) {
            getProgFrag().setStats(cs);
            getActi().runOnUiThread(() -> {
                if (csSize == 5) ((Interface)getActi())
                        .changeTabTo(0); });
            setLastForce(0); }
        getProgFrag().setLoading(false);

        while (getFixedPool().getActiveCount() > 0)
            sleep(500);
        getSettiFrag().refreshStored();
        lock = false;
    }
}
