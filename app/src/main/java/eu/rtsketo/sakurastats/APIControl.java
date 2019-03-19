package eu.rtsketo.sakurastats;

import android.util.Pair;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.jsoup.Jsoup;
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
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

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

import static eu.rtsketo.sakurastats.Interface.getLastUse;
import static eu.rtsketo.sakurastats.Interface.setLastForce;
import static eu.rtsketo.sakurastats.Interface.setLastUse;
import static eu.rtsketo.sakurastats.Statics.agent;
import static eu.rtsketo.sakurastats.Statics.badConnection;
import static eu.rtsketo.sakurastats.Statics.getCachePool;
import static eu.rtsketo.sakurastats.Statics.getFixedPool;
import static eu.rtsketo.sakurastats.Statics.l2o;

public class APIControl {
    private static Api api;
    private static int timeout;
    private static Map<String, Document> pageMap = new HashMap<>();
    private static GeneralDao db = DBControl.getDB().getDao();
    private static List<ClanStats> clanList;
//    private static List<ClanWar> warClans;
    private static int maxParticipants;
    private static String mainTag;
    private static int sleepTime;

    static void initAPI() {
        api = null;
        api = new Api("http://api.royaleapi.com/",
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
                ".eyJpZCI6OTg2LCJpZGVuIjoiMTc3NDg1Nzg0OTIy" +
                "ODQ5MjgxIiwibWQiOnsidXNlcm5hbWUiOiJEZWFkbH" +
                "lBbGl2ZSIsImtleVZlcnNpb24iOjMsImRpc2NyaW1pb" +
                "mF0b3IiOiI3MjI5In0sInRzIjoxNTQxNDQzNzAxNjcyf" +
                "Q.uLGmc7wvzsmkt2ughm1QU-pUjrirWYwj3lQON7acF8k");
        timeout = 30000;
    }

    public static Api getApi() {
        if (api == null) initAPI();
        return api;
    }

    private static void timeout() {
        timeout = timeout < 5000?
                30000 : timeout - 450; }

    private static void sleep() {
        sleepTime = sleepTime > 10000?
                0 : sleepTime + 150;
        sleep(sleepTime); }

    static void sleep(int time) {
        try { Thread.sleep(time); }
        catch (InterruptedException e) { e.printStackTrace(); }
    }

    private static void cought(Exception ex) {
//        Log.d("Cought", Log.getStackTraceString(ex.getCause().getCause()));
        badConnection();
    }

    static boolean checkClan(final String tag) {
        try { TimeLimiter limiter = SimpleTimeLimiter.create(getCachePool());
            Clan clan = limiter.callWithTimeout(new Callable<Clan>() {
                public Clan call() {
                    return api.getClan(ClanRequest.builder(tag).build());
                }}, timeout, TimeUnit.MILLISECONDS);
            if (clan.getTag() != null) return true; }
        catch (Exception ex) { cought(ex); ex.printStackTrace(); }
        try { Document doc = getPage("https://spy.deckshop.pro/clan/"+tag);
            if (doc.select(".text-muted").size() > 0 &&
                    doc.select(".text-muted").get(1)
                    .ownText().startsWith("#")) return true;
            doc = getPage("https://royaleapi.com/clan/"+tag+"/war");
            Element txt = doc.selectFirst(".horizontal .item");
            if (txt != null && txt.ownText().trim()
                    .startsWith("#")) return true; }
        catch (IOException ex) { cought(ex); ex.printStackTrace(); }
        return false;
    }

    private static List<Member> getMembers(final String tag) {
        List<Member> members = null;
        try { TimeLimiter limiter = SimpleTimeLimiter.create(getCachePool());
            members = limiter.callWithTimeout(new Callable<List<Member>>() {
                public List<Member> call() {
                    return api.getClan(ClanRequest.builder(tag).build()).getMembers();
                }}, timeout, TimeUnit.MILLISECONDS); }
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

    static List<TopClan> getTopClans() {
        List<TopClan> topClans = null;
        try { TimeLimiter limiter = SimpleTimeLimiter.create(getCachePool());
            topClans = limiter.callWithTimeout(new Callable<List<TopClan>>() {
                public List<TopClan> call() {
                    return api.getTopClans(TopClansRequest.builder().build());
                }}, timeout, TimeUnit.MILLISECONDS); }
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

    private static ChestCycle getPlayerChests(final String tag) {
        ChestCycle chestCycle = null;
        try { TimeLimiter limiter = SimpleTimeLimiter.create(getCachePool());
            chestCycle = limiter.callWithTimeout(new Callable<ChestCycle>() {
                public ChestCycle call() {
                    return api.getPlayerChests(PlayerChestsRequest.builder(
                            Collections.singletonList(tag)).build()).get(0);
                }}, timeout, TimeUnit.MILLISECONDS); }
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

    private static String extractNum(Element ele) {
        String chestNum;
        if (ele == null) chestNum = "0";
        else chestNum = ele.ownText().equals("")?"0":ele.ownText();
        return chestNum.replace("+","");
    }

    static ClanWar getClanWar(final String tag) {
        ClanWar clanWar = null;
        try { TimeLimiter limiter = SimpleTimeLimiter.create(getCachePool());
            clanWar = limiter.callWithTimeout(new Callable<ClanWar>() {
                public ClanWar call() {
                    return api.getClanWar(ClanWarRequest.builder(tag).build());
                    }}, timeout, TimeUnit.MILLISECONDS);
            if (clanWar.getClan() == null) {
                ClanWarClan clanWarClan = new ClanWarClan();
                limiter = SimpleTimeLimiter.create(getCachePool());
                Clan clan = limiter.callWithTimeout(new Callable<Clan>() {
                    public Clan call() {
                        return api.getClan(ClanRequest.builder(tag).build());
                    }}, timeout, TimeUnit.MILLISECONDS);
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
            System.out.println("Badge: " + badge);
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

    private static List<ClanWarLog> getClanWarLog(final String tag) {
        List<ClanWarLog> clanWarLogs = null;
        try { TimeLimiter limiter = SimpleTimeLimiter.create(getCachePool());
            clanWarLogs = limiter.callWithTimeout(new Callable<List<ClanWarLog>>() {
                public List<ClanWarLog> call() {
                    return api.getClanWarLog(ClanWarLogRequest.builder(tag).build());
                }}, timeout, TimeUnit.MILLISECONDS);
            System.out.print("\nCRAPI "+tag+": ");
            for (ClanWarLog cwl : clanWarLogs)
                System.out.print(cwl.getCreatedDate()+" "); }
        catch (Exception ex) { cought(ex); timeout(); }
        if (clanWarLogs == null) try {
            Document doc = getPage("https://royaleapi.com/clan/"+tag+"/war/log");
            String page = "https://royaleapi.com/clan/"+tag+"/war/analytics/csv";
            URLConnection clanURL = new URL(page).openConnection();
            clanURL.setRequestProperty("User-Agent", agent);
            clanURL.connect();

            InputStream input = clanURL.getInputStream();
            InputStreamReader reader = new InputStreamReader(input);
            List<CSVRecord> records = CSVFormat.DEFAULT.parse(reader).getRecords();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            Map<String, List<ClanWarLogParticipant>> dayMap = new HashMap<>();
            List<ClanWarLog> logDays = new ArrayList<>();

            System.out.print("\nWEB "+tag+": ");
            for(CSVRecord rec : records)
                if (!rec.get(0).equals("name"))
                    for (int w = 0; w < (rec.size() - 5) / 4; w++)
                        if (!rec.get(4 * w + 5).equals(""))
                            if(!dayMap.containsKey(rec.get(4 * w + 5)))
                                dayMap.put(rec.get(4 * w + 5),
                                        new ArrayList<ClanWarLogParticipant>());

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
                    int warTrophies = Integer.valueOf(
                            stand.selectFirst("td.trophy").ownText());

                    ClanWarLogStanding cwls = new ClanWarLogStanding();
                    cwls.setWarTrophies(warTrophies);
                    cwls.setTag(clanTag);
                    cwlsList.add(cwls); }

                standList[counter++] = cwlsList;
            }

            counter = 0;
            for (String day : dayMap.keySet()) {
                ClanWarLog newLog = new ClanWarLog();
                String createDate = day.replace(".000Z", "");
                Date date = sdf.parse(createDate);
                Long time = date.getTime()/1000; // + 28800;

                newLog.setCreatedDate(time);
                newLog.setSeasonNumber(season[counter]);
                newLog.setParticipants(dayMap.get(day));
                newLog.setStandings(standList[counter++]);

                logDays.add(newLog);
                System.out.print(time +" ");
            }

            input.close();
            reader.close();
            clanWarLogs = logDays;
        } catch (Exception ex) { cought(ex); sleep(); clanWarLogs = getClanWarLog(tag); }
        return clanWarLogs;
    }

    private static Profile getProfile(final String tag) {
        Profile profile = null;
        try { TimeLimiter limiter = SimpleTimeLimiter.create(getCachePool());
            profile = limiter.callWithTimeout(new Callable<Profile>() {
                public Profile call() {
                    return api.getProfile(ProfileRequest.builder(tag).build());
                }}, timeout, TimeUnit.MILLISECONDS); }
        catch (Exception ex) { cought(ex); timeout(); }
        if (profile == null) try {
            Profile player = new Profile();
            List<Card> cardList = new ArrayList<>();

            Document doc = getPage("https://spy.deckshop.pro/player/"+tag+"/cards");
            Elements cards = doc.select(".card_tag");
            for (Element element : cards) {
                Card card = new Card();
                card.setMaxLevel(13);
                card.setLevel(Integer.parseInt(
                        element.ownText().replace("Level ","")));
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


    private static List<Battle> getBattles(final String tag) {
        List<Battle> battles = null;
        try { TimeLimiter limiter = SimpleTimeLimiter.create(getCachePool());
            battles = limiter.callWithTimeout(new Callable<List<Battle>>() {
                public List<Battle> call() {
                    return api.getPlayerBattles(PlayerBattlesRequest.builder(
                            Collections.singletonList(tag)).build()).get(0);
                }}, timeout, TimeUnit.MILLISECONDS); }
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

    private static ClanStats getClan(String tag) { return getClan(tag, false); }
    private static ClanStats getClan(String tag, boolean target) {
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

//        if (!clanWar.getState().equals("notInWar")) {
            clanStats.setWarTrophies(clan.getWarTrophies());
            clanStats.setName(clan.getName());
//            warClans.add(clanWar);

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
//        }

        db.insertClanStats(clanStats);
        return clanStats;
    }

    private static int mainClan(ClanWar mainClan) {
        List<ClanWarStanding> standings = mainClan.getStandings();
        mainTag = mainClan.getClan().getTag();
        maxParticipants = 0;

        for (ClanWarStanding clan : standings)
            if(maxParticipants < clan.getParticipants())
                maxParticipants = clan.getParticipants();

        for (final ClanWarStanding clan : standings)
            if (!clan.getTag().equals(mainClan.getClan().getTag()))
                getFixedPool().execute(new Runnable() {
                    @Override public void run() {
                        clanList.add(getClan(clan.getTag())); }});
            while (clanList.size() < 4) sleep(500);

        return maxParticipants - mainClan.getClan().getBattlesPlayed();
    }

    private static Pair<Integer, Double> prognoseWins(ClanWar clan) {
        List<PlayerStats> playerList = getPlayerStats(clan, false);
        int remainingBattles = maxParticipants;
        PolynomialFunction poly = null;
        int alreadyWon = 0;

        for (PlayerStats player : playerList) {
//            if (player.isCurrent())
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
        double meanValue = 0;
        double winProbability = 0;

        if (poly!=null) {
            double coef[] = poly.getCoefficients();
            for (int c = 0; c < coef.length; c++)
                if (maxCoef < coef[c]) {
                    maxCoef = coef[c];
                    probWin = c;
                }

            for (int c = 0; c < coef.length; c++) {
                winProbability += coef[c];
                if (winProbability > .8) {
                    System.out.print((c+alreadyWon)+" ");
                    break;
                }
            }

            for (int c = 0; c < coef.length; c++)
                meanValue += coef[c] * c;

            for (int c = probWin+1; c < coef.length; c++)
                extraWin += coef[c];

        }

//        System.out.print("Mean: "+meanValue+" ");
//        System.out.print(">80: "+ winProbability);
//        System.out.println(" Wins: "+(probWin+alreadyWon)+" "+((int)(maxCoef*100))+"%");
        System.out.println("Wins: " + meanValue+alreadyWon);
        return new Pair<>(probWin + alreadyWon, extraWin);
    }

    private static PolynomialFunction polyMulti(PolynomialFunction poly, double coef1, double coef2) {
        PolynomialFunction tempPoly = new PolynomialFunction(new double[]{coef1, coef2});
        if (poly==null) poly = tempPoly;
        else poly = poly.multiply(tempPoly);
        return poly;
    }


    static List<ClanStats> getClanStats(String tag) {
            if (getLastUse(tag)) {
                db.resetClanStats(tag);
                clanList = new ArrayList<>();
//                warClans = new ArrayList<>();
                clanList.add(getClan(tag, true));
                setLastUse(tag);
                return clanList;
            }

            return db.getClanStatsList(tag);
    }

    static List<PlayerStats> getPlayerStats(ClanWar clanWar, boolean force) {
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

    static List<PlayerStats> getPlayerStats(String tag, boolean force) {
        if (getLastUse(tag) || force) {
            List<Member> members = getMembers(tag);
            List<ClanWarLog> warLog = getClanWarLog(tag);
            List<PlayerStats> players = new ArrayList<>();
            db.resetCurrentPlayers(tag);

            for (Member member : members)
                    players.add(getPlayer(tag, member, warLog));

            for (ClanWarLog day : warLog)
                db.addWarDay(new WarDay(day.getCreatedDate(), mainTag));

            return players;
        }

        return db.getClanPlayerStats(tag);
    }

    private static PlayerStats getPlayer(String clanTag,
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

    static ClanPlayer getPlayerProfile(String tag, boolean force) {
        if (getLastUse(tag, "prof") || force) {
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

            System.out.println("Count: "+cardCount + " / "+profile.getCards().size());
            int score = (int) ((cardCount / profile
                    .getCards().size()) * 8182.73);

            String role = profile.getClan()==null?"N/A":
                    profile.getClan().getRole();

            player.setRole(role);
            player.setTrophies(profile.getTrophies());
            player.setScore(score);

            db.insertClanPlayer(player);
            setLastUse(tag, "prof");
            setLastForce(1);
            return player;
        }
        return db.getClanPlayer(tag);
    }

    static ClanPlayer getMemberActivity(ClanPlayer player, boolean force) {
        String tag = player.getTag();
        boolean changed = false;

        if(getLastUse(tag, "chest") || force) {
            ChestCycle cc = getPlayerChests(tag);
            player.setSmc(cc.getMegaLightning());
            player.setLegendary(cc.getLegendary());
            player.setMagical(cc.getMagical());
            setLastUse(tag, "chest");
            changed = true;
        }

        if (getLastUse(tag, "batt") || force) {
            List<Battle> battles = getBattles(tag);
            long last = 0;
            if (battles != null)
                for (Battle battle : battles)
                    if (last < battle.getUtcTime())
                        last = battle.getUtcTime();
            player.setLast(last);
            setLastUse(tag, "batt");
            changed = true;
        }

        if (changed) {
            db.insertClanPlayer(player);
            setLastForce(2);
        }
        return player;
    }

    public static PlayerStats getMissingPlayer(Member member, String clanTag) {
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

    private static Document getPage(String page) throws IOException {
        if(pageMap.containsKey(page)) return pageMap.get(page);
        URLConnection clanURL = new URL(page).openConnection();
        clanURL.setRequestProperty("User-Agent", agent);
        clanURL.connect();
        InputStream input = clanURL.getInputStream();
        Document doc =  Jsoup.parse(input,"UTF-8", page);
//        Document doc = Jsoup.connect(page)
//                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
//                .referrer("http://www.google.com")
//                .get();
        if (page.contains("royaleapi")) pageMap.put(page, doc);
        input.close();
        return doc;
    }

    static void clearPages() {
        pageMap.clear();
    }
}


//                double log = wins == 0 ? 0 : Math.log(wins) / Math.log(maxPlayed);
//                double norma = (log + 1) / 2 * ratio;
