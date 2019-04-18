package eu.rtsketo.sakurastats.main;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.qwerjk.better_text.MagicTextView;

import java.util.Timer;
import java.util.TimerTask;

import eu.rtsketo.sakurastats.R;
import eu.rtsketo.sakurastats.control.DataRoom;
import eu.rtsketo.sakurastats.control.DialogView;
import eu.rtsketo.sakurastats.control.ViewDecor;
import eu.rtsketo.sakurastats.fragments.AppSettings;
import eu.rtsketo.sakurastats.fragments.PlayerActivity;
import eu.rtsketo.sakurastats.fragments.Prognostics;
import eu.rtsketo.sakurastats.fragments.WarStatistics;
import eu.rtsketo.sakurastats.hashmaps.PlayerMap;
import eu.rtsketo.sakurastats.hashmaps.SDPMap;

import static eu.rtsketo.sakurastats.control.ThreadPool.getCachePool;
import static eu.rtsketo.sakurastats.control.ViewDecor.decorate;
import static eu.rtsketo.sakurastats.hashmaps.SDPMap.sdp2px;

public class Interface extends AppCompatActivity {
    public static final String TAG = "eu.rtsketo.sakurastats";
    private ImageView progTab, warTab, actiTab, settiTab;
    private MagicTextView[] tab = new MagicTextView[4];
    private SharedPreferences preferences;
    private ViewPager mViewPager;
    private PlayerActivity actiFrag;
    private WarStatistics warFrag;
    private Prognostics progFrag;
    private AppSettings settiFrag;

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        SectionsPagerAdapter(FragmentManager fm) { super(fm); }
        @Override public int getCount() { return 4; }
        @Override public Fragment getItem(int pos) {
            switch (pos) {
                case 0: return new Prognostics();
                case 1: return new WarStatistics();
                case 2: return new PlayerActivity();
                case 3: return new AppSettings();
                default: return null; }}}

    public void changeTabTo(int num) {
        mViewPager.setCurrentItem(num); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        setContentView(R.layout.activity_interface);

        mViewPager = findViewById(R.id.viewPager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(pageChangeListener);
        mViewPager.setOffscreenPageLimit(4);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();
        ViewDecor.init(getResources());
        SDPMap.init(getResources());
        PlayerMap.init(this);
        DataRoom.init(this);
        initTabs();
        startApp();

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttachFragment(Fragment frag) {
        if (frag instanceof PlayerActivity)
            actiFrag = (PlayerActivity) frag;
        else if (frag instanceof WarStatistics)
            warFrag = (WarStatistics) frag;
        else if (frag instanceof AppSettings)
            settiFrag = (AppSettings) frag;
        else if (frag instanceof Prognostics)
            progFrag = (Prognostics) frag;
        super.onAttachFragment(frag);
    }

    private void startApp() {
        getCachePool().execute(() -> {
            if (getLastClan() == null)
                runOnUiThread(() -> new DialogView(
                        DialogView.SakuraDialog.INPUT, this));
            else Service.getThread(this).start(getLastClan());
        }); }

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
            getTab(c).setOnClickListener(
                    view -> changeTabTo(finalC)); }

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


    public void setLastUse(String tag) { setLastUse(tag, ""); }
    public boolean getLastUse(String tag) { return getLastUse(tag, ""); }
    public void setLastForce(int tab) { setLastUse(getTabName(tab), "frag"); }
    public void setLastClan(String tag) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("ClanTag", tag);
        editor.apply(); }

    public String getLastClan() {
        return preferences.getString("ClanTag", getStoredClan(0)); }

    public void setLastUse(String tag, String mod) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(mod+tag, System.currentTimeMillis());
        editor.apply(); }

    public boolean getLastUse(String tag, String mod) {
        Long curr = System.currentTimeMillis();
        Long time = preferences.getLong(mod+tag, 0);
        Long diff = (curr - time);
        if (mod.equals("")) return diff > 30*60*1000;
        if (mod.startsWith("batt")) return diff > 24*60*60*1000;
        if (mod.startsWith("prof")) return diff > 6*60*60*1000;
        if (mod.startsWith("chest")) return diff > 12*60*60*1000;
        if (mod.startsWith("frag")) return diff > 15*60*1000;
        if (mod.startsWith("auto")) return diff > 48*60*60*1000;
        return (curr - time) > 1800000; }


    public int getLastForce(int tab) {
        Long curr = System.currentTimeMillis();
        Long time = preferences.getLong("frag"+getTabName(tab), 0);
        long diff = (curr - time) / 1000 / 60;
        return (int) diff; }

    public int getUseCount() {
        int uc = preferences.getInt("useCount", 0);
        if (uc == 250 || uc == 3000 || uc == 10000)
            runOnUiThread(() -> new DialogView(
                    DialogView.SakuraDialog.RATEQUEST, this));
        return uc; }

    public void incUseCount() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("useCount", getUseCount()+1);
        editor.apply(); }

    private String getTabName(int tab) {
        switch (tab) {
            case 0: return "prog";
            case 1: return "war";
            default: return "acti"; } }

    public void setStoredClan(int index, String tag) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("StoredClan"+index, tag);
        editor.apply(); }

    public String getStoredClan(int index) {
        return preferences.getString("StoredClan"+index, null); }


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

    public void badConnection() {
        runOnUiThread(() -> {
            if (warFrag.getLoading())
                warFrag.getWifi().setVisibility(View.VISIBLE);
            if (actiFrag.getLoading())
                actiFrag.getWifi().setVisibility(View.VISIBLE);
            progFrag.getWifi().first.setVisibility(View.VISIBLE);
            progFrag.getWifi().second.setVisibility(View.VISIBLE);
        });

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override public void run() {
                runOnUiThread(() -> {
                    warFrag.getWifi().setVisibility(View.INVISIBLE);
                    actiFrag.getWifi().setVisibility(View.INVISIBLE);
                    progFrag.getWifi().first.setVisibility(View.INVISIBLE);
                    progFrag.getWifi().second.setVisibility(View.INVISIBLE);
                });}},1500); }

    @Override
    public void onBackPressed() {
        this.moveTaskToBack(true);
    }

    public WarStatistics getWarFrag() { waitForFrag(warFrag); return warFrag; }
    public PlayerActivity getActiFrag() { waitForFrag(actiFrag); return actiFrag; }
    public Prognostics getProgFrag() { waitForFrag(progFrag); return progFrag; }
    public AppSettings getSettiFrag() { waitForFrag(settiFrag); return settiFrag; }
    private void waitForFrag(Fragment frag) {
        while (frag.getView() == null)
            SystemClock.sleep(50);
    }
}
