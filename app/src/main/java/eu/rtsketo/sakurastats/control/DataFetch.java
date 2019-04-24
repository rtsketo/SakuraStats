package eu.rtsketo.sakurastats.control;

import android.annotation.SuppressLint;
import android.util.Log;
import android.util.Pair;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import eu.rtsketo.sakurastats.dbobjects.ClanPlayer;
import eu.rtsketo.sakurastats.dbobjects.ClanStats;
import eu.rtsketo.sakurastats.dbobjects.PlayerStats;
import eu.rtsketo.sakurastats.dbobjects.WarDay;
import eu.rtsketo.sakurastats.main.Interface;
import jcrapi.Api;
import jcrapi.model.Badge;
import jcrapi.model.Battle;
import jcrapi.model.Card;
import jcrapi.model.ChestCycle;
import jcrapi.model.Clan;
import jcrapi.model.ClanWar;
import jcrapi.model.ClanWarClan;
import jcrapi.model.ClanWarLog;
import jcrapi.model.ClanWarLogParticipant;
import jcrapi.model.ClanWarLogStanding;
import jcrapi.model.ClanWarParticipant;
import jcrapi.model.ClanWarStanding;
import jcrapi.model.Member;
import jcrapi.model.Profile;
import jcrapi.model.ProfileClan;
import jcrapi.model.TopClan;
import jcrapi.request.ClanRequest;
import jcrapi.request.ClanWarLogRequest;
import jcrapi.request.ClanWarRequest;
import jcrapi.request.PlayerBattlesRequest;
import jcrapi.request.PlayerChestsRequest;
import jcrapi.request.ProfileRequest;
import jcrapi.request.TopClansRequest;

import static eu.rtsketo.sakurastats.control.ThreadPool.getCachePool;
import static eu.rtsketo.sakurastats.control.ThreadPool.getFixedPool;
import static eu.rtsketo.sakurastats.hashmaps.LeagueMap.l2o;
import static eu.rtsketo.sakurastats.hashmaps.SiteMap.getAgent;
import static eu.rtsketo.sakurastats.hashmaps.SiteMap.getPage;
import static eu.rtsketo.sakurastats.main.Interface.TAG;

@SuppressWarnings("UnstableApiUsage")
public class DataFetch {
    private static Api api = new Api("http://api.royaleapi.com/",
            );

    @SuppressWarnings("FieldCanBeLocal")
    private int retries = 5;
    private int timeout = 10000;
    private Map<String, List<ClanWarLog>> clanWars = new HashMap<>();
    private DAObject db = DataRoom.getInstance().getDao();
    private List<ClanStats> clanList;
    private int maxParticipants;
    private String mainTag;
    private Interface acti;
    private int sleepTime;

    public DataFetch(Interface activity) { acti = activity; }

    private void timeout() {
        timeout = Math.max(5000, timeout - 500); }

    private void sleep() {
        sleepTime = sleepTime > 10000?
                0 : sleepTime + 150;
        sleep(sleepTime); }

    private void sleep(int time) {
        try { Thread.sleep(time); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Sleep failed", e); }
    }

    private void cought(Exception e) {
        Log.w(TAG, "Data fetching", e);
        acti.badConnection(); }

     boolean checkClan(final String tag) {
        try { TimeLimiter limiter = SimpleTimeLimiter.create(getCachePool());
            Clan clan = limiter.callWithTimeout(() ->
                    api.getClan(ClanRequest.builder(tag).build()),
                    timeout, TimeUnit.MILLISECONDS);
            if (clan.getTag() != null) return true; }
        catch (Exception ex) { cought(ex); }
        try { Document doc = getPage("https://spy.deckshop.pro/clan/"+tag);
            if (!doc.select(".text-muted").isEmpty() &&
                    doc.select(".text-muted").get(1)
                    .ownText().startsWith("#")) return true;
            doc = getPage("https://royaleapi.com/clan/"+tag+"/war");
            Element txt = doc.selectFirst(".horizontal .item");
            if (txt != null && txt.ownText().trim()
                    .startsWith("#")) return true; }
        catch (IOException ex) { cought(ex); }
        return false;
    }

    public List<Member> getMembers(final String tag) {
        List<Member> members = null;
        try { TimeLimiter limiter = SimpleTimeLimiter.create(getCachePool());
            members = limiter.callWithTimeout(() ->
                    api.getClan(ClanRequest.builder(tag).build())
                            .getMembers(), timeout, TimeUnit.MILLISECONDS); }
        catch (Exception ex) { cought(ex); timeout(); }
        if (members == null) try {
            List<Member> mems = new ArrayList<>();
            Document doc = getPage("https://spy.deckshop.pro/clan/"+tag);
            Elements small = doc.select(".text-muted");
            for (Element muted : small) {
                String clan = muted.text().replace("✓","").trim();
                if(clan.startsWith("#") && !clan.startsWith("# ")) {
                    String playerTag = clan.replace("#", "");
                    if (!playerTag.equals(tag)) {
                        Member member = new Member();
                        String selector = "a[href='/player/" + playerTag + "']";
                        Element ele = doc.selectFirst(selector);
                        String playerName = ele.ownText();
                        member.setName(playerName);
                        member.setTag(playerTag);
                        mems.add(member); }}}
                    members = mems;
        } catch (IOException ex) { cought(ex); sleep(); return getMembers(tag); }
        return members;
    }

    private PlayerStats getWarStats(final String tag) {
        Document doc = null;
        PlayerStats ps = null;
        for (int c = 0; c < retries || doc == null; c++) {
        try {
            doc = getPage("https://royaleapi.com/inc/player/cw_history?player_tag="+tag);
            double wins = doc.select(".won_all").size() - 1
                    + (doc.select(".won_one").size() - 1) * .5;
            int missed = doc.select(".missed").size() - 1;
            int wars = doc.select(".war_hit").size();

            double ratio = wars == 0? 0 :  wins / wars;
            double norma = (1.0 + wins)/(2.0 + wars);

            ps = new PlayerStats();
            ps.setWins((int)wins);
            ps.setMissed(missed);
            ps.setRatio(ratio);
            ps.setNorma(norma);
            ps.setWars(wars);
            ps.setTag(tag);
        } catch (IOException ex) { cought(ex); sleep(); }}
        return ps;
    }

    // Deprecated
    @SuppressWarnings("unused")
    private int lastWin(Document doc) {
        int league = 4;
        Element eleLeague = doc.select(".won_one .league img," +
                " .won_all .league img").first();

        if (eleLeague != null) {
            String lastLeague = eleLeague
                    .attr("data-cfsrc").toLowerCase();
            if (lastLeague.contains("silver")) league = 3;
            else if (lastLeague.contains("gold")) league = 2;
            else if (lastLeague.contains("legend")) league = 1;
        }

        return l2o(league*10);
    }

     public List<TopClan> getTopClans() {
        List<TopClan> topClans = null;
        try { TimeLimiter limiter = SimpleTimeLimiter.create(getCachePool());
            topClans = limiter.callWithTimeout(() ->
                    api.getTopClans(TopClansRequest.builder()
                            .build()), timeout, TimeUnit.MILLISECONDS); }
        catch (Exception ex) { cought(ex); timeout(); }
        if (topClans == null) try {
            List<TopClan> clans = new ArrayList<>();
            Document doc = getPage("https://spy.deckshop.pro/top/gr/clans");
            Elements small = doc.select(".text-muted");

            for (Element muted : small) {
                String clan = muted.text().trim();
                if(clan.startsWith("#") && !clan.startsWith("# ")) {
                    TopClan topClan = new TopClan();
                    topClan.setTag(clan.replace("#", ""));
                    clans.add(topClan);
                }
            }
            topClans = clans;
        } catch (IOException ex) { cought(ex); sleep(); return getTopClans(); }
        return topClans;
    }

    private ChestCycle getPlayerChests(final String tag) {
        ChestCycle chestCycle = null;
        try { TimeLimiter limiter = SimpleTimeLimiter.create(getCachePool());
            chestCycle = limiter.callWithTimeout(() ->
                    api.getPlayerChests(PlayerChestsRequest.builder(
                    Collections.singletonList(tag)).build()).get(0),
                    timeout, TimeUnit.MILLISECONDS); }
        catch (Exception ex) { cought(ex); timeout(); }
        if (chestCycle == null) try {
            ChestCycle cc = new ChestCycle();
            Document doc = getPage("https://spy.deckshop.pro/player/"+tag);
            Element ele = doc.selectFirst(".chest-magical ~ span");
            String chestNum = extractNum(ele);
            cc.setMagical(Integer.parseInt(chestNum));
            ele = doc.selectFirst(".chest-legendary ~ span");
            chestNum = extractNum(ele);
            cc.setLegendary(Integer.parseInt(chestNum));
            ele = doc.selectFirst(".chest-smc ~ span");
            chestNum = extractNum(ele);
            cc.setMegaLightning(Integer.parseInt(chestNum));
            chestCycle = cc;
            sleep(300);
        } catch (IOException ex) { cought(ex); sleep(); return getPlayerChests(tag); }
        return chestCycle;
    }

    private String extractNum(Element ele) {
        String chestNum;
        if (ele == null) chestNum = "0";
        else chestNum = ele.ownText().equals("")?"0":ele.ownText();
        return chestNum.replace("+","");
    }

     public ClanWar getClanWar(final String tag) {
        ClanWar clanWar = null;
        try { TimeLimiter limiter = SimpleTimeLimiter.create(getCachePool());
            clanWar = limiter.callWithTimeout(() ->
                    api.getClanWar(ClanWarRequest
                            .builder(tag).build()),
                    timeout, TimeUnit.MILLISECONDS);
            if (clanWar.getClan() == null) {
                ClanWarClan clanWarClan = new ClanWarClan();
                limiter = SimpleTimeLimiter.create(getCachePool());
                Clan clan = limiter.callWithTimeout(() ->
                        api.getClan(ClanRequest.builder(tag)
                                .build()), timeout, TimeUnit.MILLISECONDS);
                clanWarClan.setBadge(clan.getBadge());
                clanWarClan.setName(clan.getName());
                clanWarClan.setTag(tag);
                clanWar.setClan(clanWarClan);
            }
        }
        catch (Exception ex) { cought(ex); timeout(); }
        if (clanWar == null) try {
            ClanWar newWar = new ClanWar();
            ClanWarClan clan = new ClanWarClan();
            List<ClanWarStanding> newList = new ArrayList<>();
            List<ClanWarParticipant> newPars = new ArrayList<>();
            Document doc = getPage("https://royaleapi.com/clan/"+tag+"/war");
            Elements stats = doc.select(".clan_stats > .stats > .value");

            String warState = stats.get(0).ownText();
            if (warState.contains("War Day")) warState = "warDay";
            else if (warState.contains("Not")) warState = "notInWar";
            else warState = "collectionDay";
            newWar.setState(warState);

            String namae = doc.selectFirst(".p_head_item .header").ownText();
            String badge = doc.selectFirst(".attached .floated")
                    .attr("data-cfsrc").toLowerCase();
            badge = badge.split("/")[badge.split("/").length-1].split("\\.")[0];
            Badge clanBadge = new Badge();
            clanBadge.setName(badge);
            clan.setBadge(clanBadge);
            clan.setName(namae);
            clan.setTag(tag);

            if (stats.size() > 3) {
                int battlesPlayed = Integer.parseInt(stats.get(2).ownText());
                int participants = Integer.parseInt(stats.get(3).ownText());
                int wTrohpies = Integer.parseInt(stats.get(5).ownText());
                int crowns = Integer.parseInt(stats.get(4).ownText());

                clan.setBattlesPlayed(battlesPlayed);
                clan.setParticipants(participants);
                clan.setWarTrophies(wTrohpies);
                clan.setCrowns(crowns);

                Elements standings = doc.select(".standings tbody > tr");
                for (Element standing : standings) {
                    ClanWarStanding cws = new ClanWarStanding();
                    String clanTag = standing.selectFirst("td:nth-child(2) a")
                            .attr("href").replace("/clan/", "")
                            .replace("/war/", "")
                            .replace("/war", "");
                    String name = standing.selectFirst("td:nth-child(2) a").ownText();
                    int wins = Integer.parseInt(standing.selectFirst(".wins").ownText());

                    Document clanStand = getPage("https://royaleapi.com/clan/"+clanTag+"/war");
                    Elements clanStats = clanStand.select(".clan_stats > .stats > .value");

                    cws.setTag(clanTag);
                    cws.setName(name);
                    cws.setWins(wins);
                    cws.setBattlesPlayed(Integer.parseInt(clanStats.get(2).ownText()));
                    cws.setParticipants(Integer.parseInt(clanStats.get(3).ownText()));
                    cws.setWarTrophies(Integer.parseInt(clanStats.get(5).ownText()));
                    cws.setCrowns(Integer.parseInt(clanStats.get(4).ownText()));

                    if (clanTag.equals(tag)) {
                        clan.setTag(clanTag);
                        clan.setName(name);
                        clan.setWins(wins);
                    }
                    newList.add(cws);
                }

                Elements parties = doc.select(".roster > tbody tr");
                for (Element part : parties) {
                    ClanWarParticipant cwp = new ClanWarParticipant();
                    String name = part.selectFirst("td:nth-child(2) a").ownText();
                    String pTag = part.selectFirst("td:nth-child(2) a").attr("href")
                            .replace("/player/", "");

                    int batt = 0, wins = 0;
                    int sort = Integer.parseInt(part
                            .selectFirst("td:nth-child(6)")
                            .attr("data-sort-value"));
                    int cards = Integer.parseInt(part
                            .selectFirst("td:nth-child(5)").ownText());

                    switch (sort) {
                        case 22: wins = 2;
                        case 2: batt = 2; break;
                        case 11: wins = 1;
                        case 1: batt = 1; break;
                    }

                    cwp.setBattlesPlayed(batt);
                    cwp.setCardsEarned(cards);
                    cwp.setName(name);
                    cwp.setTag(pTag);
                    cwp.setWins(wins);

                    newPars.add(cwp);
                }

                newWar.setParticipants(newPars);
                newWar.setStandings(newList);
            }

            newWar.setClan(clan);
            clanWar = newWar;
        }
        catch (Exception ex) { cought(ex); sleep(); clanWar = getClanWar(tag); }
        return clanWar;
    }

    private List<ClanWarLog> getClanWarLog(final String tag) {
        if (clanWars.containsKey(tag)) return clanWars.get(tag);
        List<ClanWarLog> clanWarLogs = null;
        try { TimeLimiter limiter = SimpleTimeLimiter.create(getCachePool());
            clanWarLogs = limiter.callWithTimeout(() ->
                    api.getClanWarLog(ClanWarLogRequest
                            .builder(tag).build()),
                    timeout, TimeUnit.MILLISECONDS); }
        catch (Exception ex) { cought(ex); timeout(); }
        if (clanWarLogs == null) try {
            Document doc = getPage("https://royaleapi.com/clan/"+tag+"/war/log");
            String page = "https://royaleapi.com/clan/"+tag+"/war/analytics/csv";
            URLConnection clanURL = new URL(page).openConnection();
            clanURL.setRequestProperty("User-Agent", getAgent());
            clanURL.connect();

            InputStream input = clanURL.getInputStream();
            InputStreamReader reader = new InputStreamReader(input);
            List<CSVRecord> records = CSVFormat.DEFAULT.parse(reader).getRecords();

            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            Map<String, List<ClanWarLogParticipant>> dayMap = new HashMap<>();
            List<ClanWarLog> logDays = new ArrayList<>();

            for(CSVRecord rec : records)
                if (!rec.get(0).equals("name"))
                    for (int w = 0; w < (rec.size() - 5) / 4; w++)
                        if (!rec.get(4 * w + 5).equals("") &&
                                !dayMap.containsKey(rec.get(4 * w + 5)))
                                dayMap.put(rec.get(4 * w + 5), new ArrayList<>());

            for(CSVRecord rec : records)
                if (!rec.get(0).equals("name"))
                    for (int w = 0; w < (rec.size() - 5) / 4; w++)
                        if (!rec.get(4 * w + 5).equals("")) {
                            ClanWarLogParticipant player = new ClanWarLogParticipant();
                            player.setName(rec.get(0));
                            player.setTag(rec.get(1));

                            int day = 4 * w + 5;
                            player.setCardsEarned(Integer.parseInt(rec.get(day+1)));
                            player.setBattlesPlayed(Integer.parseInt(rec.get(day+2)));
                            player.setWins(Integer.parseInt(rec.get(day+3)));
                            dayMap.get(rec.get(day)).add(player);
                        }

            int[] season = new int[10];
            List<ClanWarLogStanding>[] standList = new List[10];
            Elements eles = doc.select(".ui .inverted .header");

            int counter = 0;
            for (Element ele : eles)
                if (ele.ownText().startsWith("Season"))
                    season[counter++] = Integer.valueOf(
                            ele.ownText().replace("Season ",""));

            counter = 0;
            eles = doc.select(".standings .unstackable");
            for (Element ele : eles) {
                Elements stands = ele.select("tbody tr");
                List<ClanWarLogStanding> cwlsList = new ArrayList<>();

                for (Element stand : stands) {
                    String clanTag = stand.selectFirst("td a")
                            .attr("href")
                            .replace("/clan/","")
                            .replace("/war/log","");
                    int warTrophies = Integer.parseInt(
                            stand.selectFirst("td.trophy").ownText());

                    ClanWarLogStanding cwls = new ClanWarLogStanding();
                    cwls.setWarTrophies(warTrophies);
                    cwls.setTag(clanTag);
                    cwlsList.add(cwls); }

                standList[counter++] = cwlsList;
            }

            counter = 0;
            for (Map.Entry<String, List<ClanWarLogParticipant>>
                    day : dayMap.entrySet()) {
                ClanWarLog newLog = new ClanWarLog();
                String createDate = day.getKey().replace(".000Z", "");
                Date date = sdf.parse(createDate);
                long time = date.getTime()/1000; // + 28800;

                newLog.setCreatedDate(time);
                newLog.setSeasonNumber(season[counter]);
                newLog.setParticipants(day.getValue());
                newLog.setStandings(standList[counter++]);

                logDays.add(newLog);
            }

            input.close();
            reader.close();
            clanWarLogs = logDays;
        } catch (Exception ex) { cought(ex); sleep(); clanWarLogs = getClanWarLog(tag); }
        clanWars.put(tag, clanWarLogs);
        return clanWarLogs;
    }

    private Profile getProfile(final String tag) {
        Profile profile = null;
        try { TimeLimiter limiter = SimpleTimeLimiter.create(getCachePool());
            profile = limiter.callWithTimeout(() ->
                    api.getProfile(ProfileRequest.builder(tag)
                            .build()), timeout, TimeUnit.MILLISECONDS); }
        catch (Exception ex) { cought(ex); timeout(); }
        if (profile == null) try {
            Profile player = new Profile();
            List<Card> cardList = new ArrayList<>();

            Document doc = getPage("https://spy.deckshop.pro/player/"+tag+"/cards");
            Elements cards = doc.select(".card_tag");
            for (Element element : cards) {
                Card card = new Card();
                card.setMaxLevel(13);
                card.setDisplayLevel((Integer.parseInt(
                        element.ownText().replace("Level ",""))));
                cardList.add(card);
            } player.setCards(cardList);

            doc = getPage("https://spy.deckshop.pro/player/"+tag);
            Element roleElement = doc.selectFirst(".clearfix > .text-muted");

            String role = "Member";
            if (roleElement != null)
                role = roleElement.ownText();

            switch (role) {
                case "Member": role = "member"; break;
                case "Co-leader": role = "coLeader"; break;
                case "Leader": role = "leader"; break;
                case "Elder": role = "elder"; break;
            }

            ProfileClan pClan = new ProfileClan();
            pClan.setRole(role);

            player.setClan(pClan);
            player.setTrophies(Integer.parseInt(doc.select(".media-body .text-warning")
                    .text().replace(" ","")));

            profile = player;
        } catch (IOException ex) { cought(ex); sleep(); return getProfile(tag); }
        return profile;
    }


    private List<Battle> getBattles(final String tag) {
        List<Battle> battles = null;
        try { TimeLimiter limiter = SimpleTimeLimiter.create(getCachePool());
            battles = limiter.callWithTimeout(() ->
                    api.getPlayerBattles(PlayerBattlesRequest.builder(
                    Collections.singletonList(tag)).build())
                            .get(0), timeout, TimeUnit.MILLISECONDS); }
        catch (Exception ex) { cought(ex); timeout(); }
        if (battles == null) try {
            List<Battle> batts = new ArrayList<>();
            Document doc = getPage("https://spy.deckshop.pro/player/"+tag+"/battles");
            Elements eles = doc.select(".timestamp");
            if (eles == null) {
                Battle battle = new Battle();
                battle.setUtcTime(0);
                batts.add(battle);
            } else for (Element ele : eles) {
                Battle battle = new Battle();
                String textTime = ele.attributes().get("data-timestamp");
                long time = Long.parseLong(textTime);
                battle.setUtcTime(time);
                batts.add(battle);
            }
            battles = batts;
        } catch (IOException ex) { cought(ex); sleep(); return getBattles(tag); }
        return battles;
    }

    private  ClanStats getClan(String tag) { return getClan(tag, false); }
    private  ClanStats getClan(String tag, boolean target) {
        ClanWar clanWar = getClanWar(tag);
        ClanWarClan clan = clanWar.getClan();
        ClanStats clanStats = new ClanStats();
        clanStats.setState(clanWar.getState());
        clanStats.setBadge(clan.getBadge()
                .getName().toLowerCase());
        clanStats.setName("???");
        clanStats.setTag(tag);

        clanStats.setCrowns(0);
        clanStats.setRemaining(0);
        clanStats.setActualWins(0);
        clanStats.setEstimatedWins(0);
        clanStats.setMaxParticipants(0);

        clanStats.setClan1("???");
        clanStats.setClan2("???");
        clanStats.setClan3("???");
        clanStats.setClan4("???");

            clanStats.setWarTrophies(clan.getWarTrophies());
            clanStats.setName(clan.getName());

            if (clanWar.getState().equals("warDay")
                    && clanWar.getClan().getWarTrophies() > 200) {
                if (target) clanStats.setRemaining(mainClan(clanWar));
                else clanStats.setRemaining(maxParticipants - clan.getBattlesPlayed());

                Pair<Integer, Double> eew = prognoseWins(clanWar);
                clanStats.setMaxParticipants(maxParticipants);
                clanStats.setActualWins(clan.getWins());
                clanStats.setCrowns(clan.getCrowns());
                clanStats.setEstimatedWins(eew.first);
                clanStats.setExtraWins(eew.second);

                int counter = 0;
                String[] opponentClan = new String[4];
                for (ClanWarStanding clanParticipant : clanWar.getStandings())
                    if (!clanParticipant.getTag().equals(tag))
                        opponentClan[counter++] = clanParticipant.getTag();

                clanStats.setClan1(opponentClan[0]);
                clanStats.setClan2(opponentClan[1]);
                clanStats.setClan3(opponentClan[2]);
                clanStats.setClan4(opponentClan[3]);
            }
        db.insertClanStats(clanStats);
        return clanStats;
    }

    private int mainClan(ClanWar mainClan) {
        List<ClanWarStanding> standings = mainClan.getStandings();
        mainTag = mainClan.getClan().getTag();
        maxParticipants = 0;

        for (ClanWarStanding clan : standings)
            if(maxParticipants < clan.getParticipants())
                maxParticipants = clan.getParticipants();

        for (final ClanWarStanding clan : standings)
            if (!clan.getTag().equals(mainClan.getClan().getTag()))
                getFixedPool().execute(() -> clanList.add(getClan(clan.getTag())));
            while (clanList.size() < 4) sleep(500);

        return maxParticipants - mainClan.getClan().getBattlesPlayed();
    }

    private Pair<Integer, Double> prognoseWins(ClanWar clan) {
        List<PlayerStats> playerList = getPlayerStats(clan, false);
        int remainingBattles = maxParticipants;
        PolynomialFunction poly = null;
        int alreadyWon = 0;

        for (PlayerStats player : playerList) {
                if (player.getCurPlay()>0) {
                    remainingBattles -= player.getCurPlay();
                    alreadyWon += player.getCurWins();
                } else {
                    remainingBattles--;
                    double winChance = player.getPlayed()==0? .5 : player.getNorma();
                    poly = polyMulti(poly,1-winChance, winChance);
                }
        }

        for (int r = 0; r < remainingBattles; r++)
            poly = polyMulti(poly, .5, .5);

        int probWin = 0;
        double maxCoef = 0;
        double extraWin = 0;
        double winProbability = 0;

        if (poly!=null) {
            double[] coef = poly.getCoefficients();
            for (int c = 0; c < coef.length; c++)
                if (maxCoef < coef[c]) {
                    maxCoef = coef[c];
                    probWin = c;
                }

            for (double c : coef) {
                winProbability += c;
                if (winProbability > .8)
                    break;
            }

            for (int c = probWin+1; c < coef.length; c++)
                extraWin += coef[c];

        }
        return new Pair<>(probWin + alreadyWon, extraWin);
    }

    private PolynomialFunction polyMulti(PolynomialFunction poly, double coef1, double coef2) {
        PolynomialFunction tempPoly = new PolynomialFunction(new double[]{coef1, coef2});
        if (poly==null) poly = tempPoly;
        else poly = poly.multiply(tempPoly);
        return poly;
    }


    public List<ClanStats> getClanStats(String tag) {
            if (acti.getLastUse(tag)) {
                db.resetClanStats(tag);
                clanList = new ArrayList<>();
                clanList.add(getClan(tag, true));
                acti.setLastUse(tag);
                return clanList;
            }

            return db.getClanStatsList(tag);
    }

    private List<PlayerStats> getPlayerStats(ClanWar clanWar, boolean force) {
        List<PlayerStats> playerStats =
                getPlayerStats(clanWar.getClan().getTag(), force);
        List<PlayerStats> currentStats = new ArrayList<>();

        for(PlayerStats player : playerStats)
            for(ClanWarParticipant warPlayer : clanWar.getParticipants())
                if (warPlayer.getTag().equals(player.getTag())) {
                    player.setCurPlay(warPlayer.getBattlesPlayed());
                    player.setCurWins(warPlayer.getWins());
                    currentStats.add(player);
                }

        return currentStats;
    }

    public PlayerStats getPlayerStats(String clanTag, Member member, boolean force) {
        PlayerStats ps;

        if (acti.getLastUse(member.getTag(), "wstat") || force) {
            ps = getWarStats(member.getTag());
            ps.setName(member.getName());
            ps.setTag(member.getTag());
            ps.setClan(clanTag);

            ps.setChest(findLastWarWin(ps));
            acti.setLastUse(member.getTag(), "wstat");
        } else ps = db.getPlayerStats(member.getTag());

        ps.setCurrent(true);
        db.insertPlayerStats(ps);
        return ps;
    }

     private List<PlayerStats> getPlayerStats(String tag) {
        return getPlayerStats(tag, false); }
     private List<PlayerStats> getPlayerStats(String tag, boolean force) {
        if (acti.getLastUse(tag) || force) {
            List<Member> members = getMembers(tag);
            List<PlayerStats> players = new ArrayList<>();
            db.resetCurrentPlayers(tag);

            for (Member member : members) {
                PlayerStats player = getPlayerStats(tag, member, force);
                player.setCurrent(true);
                players.add(player);
            }

            return players;
        }

        return db.getClanPlayerStats(tag);
    }


    private int findLastWarWin(PlayerStats player) {
        int chest = 0;
        List<ClanWarLog> warLog = getClanWarLog(player.getClan());
        for (ClanWarLog dayLog : warLog)
            for (ClanWarLogParticipant dayPlayer : dayLog.getParticipants())
                if (dayPlayer.getTag().equals(player.getTag())) {
                    int warTroph = dayLog.getStandings()
                            .get(0).getWarTrophies();

                    int league = 4;
                    if (warTroph > 2999) league = 1;
                    else if (warTroph > 1499) league = 2;
                    else if (warTroph > 599) league = 3;

                    int position = 0;
                    for (ClanWarLogStanding cwls : dayLog.getStandings()) {
                        position++;
                        if (cwls.getTag().equals(player.getClan()))
                            break;
                    }

                    int order = l2o(league * 10 + position);
                    int season = warLog.get(0).getSeasonNumber()
                            != dayLog.getSeasonNumber() ?
                            0 : dayLog.getSeasonNumber();
                    int newChest = order + season * 100;
                    if (chest < newChest) chest = newChest;
                }
        return chest;
    }



    // Deprecated
    @SuppressWarnings("unused")
    private PlayerStats getPlayer(String clanTag,
                                  Member member, List<ClanWarLog> warLog) {
        PlayerStats player = new PlayerStats();
        player.setName(member.getName());
        player.setTag(member.getTag());
        player.setClan(clanTag);
        player.setCurrent(true);

        PlayerStats playerDB = db.getPlayerStats(member.getTag());
        int missed = playerDB == null? 0 : playerDB.getMissed();
        int played = playerDB == null? 0 : playerDB.getPlayed();
        int chest = playerDB == null? 0 : playerDB.getChest();
        int cards = playerDB == null? 0 : playerDB.getCards();
        int wins = playerDB == null? 0 : playerDB.getWins();
        int wars = playerDB == null? 0 : playerDB.getWars();

        for (ClanWarLog dayLog : warLog) {
            boolean found = false;
            for (WarDay dayDB : db.getWarDays())
                if (dayDB.getWarDay() == dayLog.getCreatedDate()) found = true;
            if (!found)
                for (ClanWarLogParticipant dayPlayer : dayLog.getParticipants())
                    if (dayPlayer.getTag().equals(player.getTag())) {
                        int warTroph = dayLog.getStandings().get(0).getWarTrophies();
                        if (dayPlayer.getBattlesPlayed()==0) missed++;
                        played += dayPlayer.getBattlesPlayed();
                        cards += dayPlayer.getCardsEarned();
                        wins += dayPlayer.getWins();
                        wars++;

                        int league = 4;
                        if (warTroph > 2999) league = 1;
                        else if (warTroph > 1499) league = 2;
                        else if (warTroph > 599) league = 3;

                        int position = 0;
                        for (ClanWarLogStanding cwls : dayLog.getStandings()) {
                            position++;
                            if (cwls.getTag().equals(player.getClan())) break; }

                        int order = l2o(league*10+position);
                        int season = warLog.get(0).getSeasonNumber()
                                != dayLog.getSeasonNumber() ?
                                0 : dayLog.getSeasonNumber();
                        int newChest = order + season * 100;
                        if (chest < newChest) chest = newChest;

                    }
        }

        double ratio = played == 0 ? 0 : (double) wins / played;
        double norma = (1.0 + wins)/(2.0 + played);

        player.setMissed(missed);
        player.setPlayed(played);
        player.setCards(cards);
        player.setNorma(norma);
        player.setRatio(ratio);
        player.setChest(chest);
        player.setWars(wars);
        player.setWins(wins);

        db.insertPlayerStats(player);
        return player;
    }

     public ClanPlayer getPlayerProfile(String tag, boolean force) {
        if (acti.getLastUse(tag, "prof") || force) {
            ClanPlayer player = db.getClanPlayer(tag);
            if (player == null) player = new ClanPlayer();

            Profile profile = getProfile(tag);
            player.setClan(mainTag);
            player.setTag(tag);

            double cardCount = 0;
            for (Card card : profile.getCards()) {
                int max = card.getMaxLevel();
                int lvl = card.getDisplayLevel();
                int dif = max - lvl;

                if (dif < 1) cardCount += 1.1;
                else cardCount += 1.0 / dif;
            }

            int score = (int) ((cardCount / profile
                    .getCards().size()) * 8182.73);

            String role = profile.getClan()==null?"N/A":
                    profile.getClan().getRole();

            player.setRole(role);
            player.setTrophies(profile.getTrophies());
            player.setScore(score);

            db.insertClanPlayer(player);
            acti.setLastUse(tag, "prof");
            acti.setLastForce(1);
            return player;
        }
        return db.getClanPlayer(tag);
    }

     public ClanPlayer getMemberActivity(ClanPlayer player, boolean force) {
        String tag = player.getTag();
        boolean changed = false;

        if(acti.getLastUse(tag, "chest") || force) {
            ChestCycle cc = getPlayerChests(tag);
            player.setSmc(cc.getMegaLightning());
            player.setLegendary(cc.getLegendary());
            player.setMagical(cc.getMagical());
            acti.setLastUse(tag, "chest");
            changed = true;
        }

        if (acti.getLastUse(tag, "batt") || force) {
            List<Battle> battles = getBattles(tag);
            long last = 0;
            for (Battle battle : battles)
                if (last < battle.getUtcTime())
                    last = battle.getUtcTime();
            player.setLast(last);
            acti.setLastUse(tag, "batt");
            changed = true;
        }

        if (changed) {
            db.insertClanPlayer(player);
            acti.setLastForce(2);
        }
        return player;
    }

    // Deprecated
    @SuppressWarnings("unused")
    public  PlayerStats getMissingPlayer(Member member, String clanTag) {
        PlayerStats newPlayer = new PlayerStats();
        newPlayer.setTag(member.getTag());
        newPlayer.setName(member.getName());
        newPlayer.setClan(clanTag);
        newPlayer.setCards(0);
        newPlayer.setWars(0);
        newPlayer.setCurPlay(0);
        newPlayer.setCurWins(0);
        newPlayer.setCurrent(false);
        newPlayer.setRatio(0);
        newPlayer.setNorma(.5);
        newPlayer.setMissed(0);
        newPlayer.setPlayed(0);
        newPlayer.setWins(0);

        db.insertPlayerStats(newPlayer);
        return newPlayer;
    }

}