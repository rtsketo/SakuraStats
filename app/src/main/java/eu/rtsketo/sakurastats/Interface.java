package eu.rtsketo.sakurastats;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.qwerjk.better_text.MagicTextView;

import static eu.rtsketo.sakurastats.APIControl.initAPI;
import static eu.rtsketo.sakurastats.APIControl.sleep;
import static eu.rtsketo.sakurastats.DBControl.initDB;
import static eu.rtsketo.sakurastats.Statics.createDialog;
import static eu.rtsketo.sakurastats.Statics.decorate;
import static eu.rtsketo.sakurastats.Statics.getActi;
import static eu.rtsketo.sakurastats.Statics.getActiFrag;
import static eu.rtsketo.sakurastats.Statics.getBackThread;
import static eu.rtsketo.sakurastats.Statics.getCachePool;
import static eu.rtsketo.sakurastats.Statics.getFixedPool;
import static eu.rtsketo.sakurastats.Statics.getProgFrag;
import static eu.rtsketo.sakurastats.Statics.getScreenHeight;
import static eu.rtsketo.sakurastats.Statics.getScreenWidth;
import static eu.rtsketo.sakurastats.Statics.getWarFrag;
import static eu.rtsketo.sakurastats.Statics.initLeagueMap;
import static eu.rtsketo.sakurastats.Statics.sdp2px;
import static eu.rtsketo.sakurastats.Statics.setActi;
import static eu.rtsketo.sakurastats.Statics.setTf;
import static eu.rtsketo.sakurastats.Statics.updatePlayerMap;

public class Interface extends AppCompatActivity {
    private ImageView progTab, warTab, actiTab, settiTab;
    private MagicTextView[] tab = new MagicTextView[4];
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private static SharedPreferences preferences;
    private static Pair<Integer, Integer> dims;
    private ViewPager mViewPager;

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        public SectionsPagerAdapter(FragmentManager fm) { super(fm); }
        @Override public int getCount() { return 4; }
        @Override public Fragment getItem(int pos) {
            switch (pos) {
                case 0: return new ProgFrag();
                case 1: return new WarFrag();
                case 2: return new ActiFrag();
                case 3: return new SettiFrag();
                default: return null; }}}

    public void changeTabTo(int num) {
        mViewPager.setCurrentItem(num); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTf(Typeface.createFromAsset(getResources().getAssets(),
                "fonts/Supercell-Magic_5.ttf"));
        setContentView(R.layout.activity_interface);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();

        System.out.println(getScreenHeight()+" " +getScreenWidth());
        dims = new Pair<>(getScreenHeight(),getScreenWidth());

        initLeagueMap();
        setActi(this);
        initDB(this);
        initTabs();
        initAPI();

        mViewPager = findViewById(R.id.viewPager);
        mViewPager.addOnPageChangeListener(pageChangeListener);
        mViewPager.setOffscreenPageLimit(4);

        preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mSectionsPagerAdapter);
        super.onCreate(savedInstanceState);
        startApp();
    }

    private void startApp() {
        getCachePool().execute(new Runnable() {
            @Override public void run() {
                if (getLastClan()==null)
                    getActi().runOnUiThread(new Runnable() {
                        @Override public void run() {
                            createDialog(Statics.SakuraDialog.INPUT); }});
                else {
                    while(!getActiFrag().isLoaded() ||
                            !getWarFrag().isLoaded() ||
                            !getProgFrag().isLoaded())
                        sleep(50);
                    updatePlayerMap();
                    if (getBackThread().getApproval())
                        getProgFrag().refresh(false);
                    sleep(500);
                    if (getBackThread().getApproval())
                        getWarFrag().refreshList(0);
                    sleep(500);
                    if (getBackThread().getApproval())
                        getActiFrag().refreshList(0);
                    updateAll(); }}});
    }

    public ImageView getTab(int c) {
        ImageView view = null;
        switch (c) {
                case 0: view = progTab; break;
                case 1: view = warTab; break;
                case 2: view = actiTab; break;
                case 3: view = settiTab; break; }
        return view; }

    private void initTabs() {
        progTab = findViewById(R.id.progTab);
        warTab = findViewById(R.id.warTab);
        actiTab = findViewById(R.id.actiTab);
        settiTab = findViewById(R.id.settiTab);

        for (int c = 0; c < 4; c++) {
            final int finalC = c;
            getTab(c).setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    changeTabTo(finalC); }}); }

        tab[0] = findViewById(R.id.tab1);
        tab[1] = findViewById(R.id.tab2);
        tab[2] = findViewById(R.id.tab3);
        tab[3] = findViewById(R.id.tab4);

        int size = sdp2px(7);
        decorate(tab[0], "Forecast", size);
        decorate(tab[1], "Analytics", size);
        decorate(tab[2], "Activity", size);
        decorate(tab[3], "Settings", size);
    }

    private synchronized void updateAll() {
        String clan = getLastClan();
        for (int c = 0; c < 2; c++)
            if (getStoredClan(c) != null
                && !getStoredClan(c).equals(clan))
                updateClan(getStoredClan(c));
        updateClan(clan); }

    private void updateClan(String clan) {
        if (getLastUse(clan, "auto")) {
            getBackThread().setLock(true);
            while (getFixedPool().getActiveCount() > 0)
                sleep(500); sleep(3000);
            setLastUse(clan, "auto");
            getBackThread().setClan(clan);
            getBackThread().setGUI(false);
            getBackThread().setLock(false);
            getBackThread().startThread();

            while (getBackThread().getThread().isAlive())
                sleep(500); sleep(1000);
            if (getBackThread().getApproval()
                    && getLastClan().equals(clan)) {
                getActiFrag().refreshList(0);
                getWarFrag().refreshList(0); }
            getBackThread().setLock(false); }}

    public static Pair<Integer, Integer> getDimensions() { return dims; }
    public static void setLastUse(String tag) {
        setLastUse(tag, "");
    }

    public static boolean getLastUse(String tag) {
        return getLastUse(tag, "");
    }

    public static void setLastClan(String tag) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("ClanTag", tag);
        editor.apply();
    }

    public static String getLastClan() {
        return preferences.getString("ClanTag", getStoredClan(0));
    }

    public static void setLastUse(String tag, String mod) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(mod+tag, System.currentTimeMillis());
        editor.apply();
    }

    public static boolean getLastUse(String tag, String mod) {
        Long curr = System.currentTimeMillis();
        Long time = preferences.getLong(mod+tag, 0);
        Long diff = (curr - time);
        if (mod.equals("")) return diff > 30*60*1000;
        if (mod.startsWith("batt")) return diff > 24*60*60*1000;
        if (mod.startsWith("prof")) return diff > 6*60*60*1000;
        if (mod.startsWith("chest")) return diff > 12*60*60*1000;
        if (mod.startsWith("frag")) return diff > 15*60*1000;
        if (mod.startsWith("auto")) return diff > 48*60*60*1000;
        return (curr - time) > 1800000;
    }

    public static void setLastForce(int tab) {
        setLastUse(getTabName(tab), "frag");
    }

    public static int getLastForce(int tab) {
        Long curr = System.currentTimeMillis();
        Long time = preferences.getLong("frag"+getTabName(tab), 0);
        long diff = (curr - time) / 1000 / 60;
        return (int) diff;
    }

    public static int getUseCount() {
        int uc = preferences.getInt("useCount", 0);
        if (uc == 250 || uc == 3000 || uc == 10000)
            getActi().runOnUiThread(new Runnable() {
                @Override public void run() {
                    createDialog(Statics.SakuraDialog.RATEQUEST); }});
        return uc;
    }

    public static void incUseCount() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("useCount", getUseCount()+1);
        editor.apply();
    }

    private static String getTabName(int tab) {
        switch (tab) {
            case 0: return "prog";
            case 1: return "war";
            default: return "acti"; }
    }

    public static void setStoredClan(int index, String tag) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("StoredClan"+index, tag);
        editor.apply();
    }

    public static String getStoredClan(int index) {
        return preferences.getString("StoredClan"+index, null);
    }

    @Override
    public void onBackPressed() {
        this.moveTaskToBack(true);
    }

    private void changeTab(int num) {
        for (MagicTextView tabetto : tab)
            tabetto.setVisibility(TextView.GONE);
        tab[num].setVisibility(TextView.VISIBLE);
    }

    ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override public void onPageScrolled(int i, float v, int i1) { }
        @Override public void onPageSelected(int i) { changeTab(i); }
        @Override public void onPageScrollStateChanged(int i) { }
    };

}
