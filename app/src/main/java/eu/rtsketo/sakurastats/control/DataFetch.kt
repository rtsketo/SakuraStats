package eu.rtsketo.sakurastats.control

import android.annotation.SuppressLint
import android.os.SystemClock
import android.util.Log
import android.util.Pair
import com.google.common.util.concurrent.SimpleTimeLimiter
import com.google.common.util.concurrent.TimeLimiter
import eu.rtsketo.sakurastats.dbobjects.ClanPlayer
import eu.rtsketo.sakurastats.dbobjects.ClanStats
import eu.rtsketo.sakurastats.dbobjects.PlayerStats
import eu.rtsketo.sakurastats.hashmaps.LeagueMap
import eu.rtsketo.sakurastats.hashmaps.SiteMap
import eu.rtsketo.sakurastats.main.Console
import eu.rtsketo.sakurastats.main.Interface
import jcrapi.Api
import jcrapi.model.*
import jcrapi.request.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.URL
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DataFetch(private val acti: Interface?) {
    private val retries = 100
    private var timeout = 10000
    private val clanWars: MutableMap<String?, List<ClanWarLog>?> = HashMap()
    private val db: DAObject = DataRoom.Companion.getInstance().getDao()
    private var clanList: MutableList<ClanStats?>? = null
    private var maxParticipants = 0
    private var internet = false
    private var mainTag: String? = null
    private var lastCheck: Long = 0
    private var sleepTime = 0
    private fun timeout() {
        timeout = Math.max(5000, timeout - 500)
    }

    private fun sleep() {
        while (!hasInternet()) SystemClock.sleep(500)
        sleepTime = if (sleepTime > 5000) 50 else sleepTime + 50
        SystemClock.sleep(sleepTime.toLong())
    }

    private fun cought(e: Exception) {
        Log.w(Interface.Companion.TAG, "Data fetching failed!", e)
        Console.Companion.logln("Fetch failed!")
        acti!!.badConnection()
    }

    fun checkClan(tag: String): Boolean {
        if (api != null) try {
            val limiter: TimeLimiter = SimpleTimeLimiter.create(ThreadPool.getCachePool())
            val clan = limiter.callWithTimeout({ api.getClan(ClanRequest.builder(tag).build()) },
                    timeout.toLong(), TimeUnit.MILLISECONDS)
            if (clan.tag != null) return true
        } catch (ex: Exception) {
            cought(ex)
        }
        try {
            var doc = SiteMap.getPage("https://spy.deckshop.pro/clan/$tag")
            if (!doc!!.select(".text-muted").isEmpty() &&
                    doc.select(".text-muted")[1]
                            .ownText().startsWith("#")) return true
            doc = SiteMap.getPage("https://royaleapi.com/clan/$tag/war")
            val txt = doc!!.selectFirst(".horizontal .item")
            if (txt != null && txt.ownText().trim { it <= ' ' }
                            .startsWith("#")) return true
        } catch (ex: IOException) {
            cought(ex)
        }
        return false
    }

    fun getMembers(tag: String?): MutableList<Member>? {
        var members: MutableList<Member>? = null
        if (api != null) try {
            val limiter: TimeLimiter = SimpleTimeLimiter.create(ThreadPool.getCachePool())
            members = limiter.callWithTimeout({
                api.getClan(ClanRequest.builder(tag).build())
                        .members
            }, timeout.toLong(), TimeUnit.MILLISECONDS)
        } catch (ex: Exception) {
            cought(ex)
            timeout()
        }
        if (members == null) try {
            val mems: MutableList<Member> = ArrayList()
            val doc = SiteMap.getPage("https://spy.deckshop.pro/clan/$tag")
            val small = doc!!.select(".text-muted")
            for (muted in small) {
                val clan = muted.text().replace("✓", "").trim { it <= ' ' }
                if (clan.startsWith("#") && !clan.startsWith("# ")) {
                    val playerTag = clan.replace("#", "")
                    if (playerTag != tag) {
                        val member = Member()
                        val selector = "a[href='/player/$playerTag']"
                        val ele = doc.selectFirst(selector)
                        val playerName = ele.ownText()
                        member.name = playerName
                        member.tag = playerTag
                        mems.add(member)
                    }
                }
            }
            members = mems
        } catch (ex: IOException) {
            cought(ex)
            sleep()
            return getMembers(tag)
        }
        return members
    }

    private fun getWarStats(tag: String): PlayerStats {
        var ps: PlayerStats? = null
        var c = 0
        while (c < retries && ps == null) {
            try {
                val doc = SiteMap.getCWPage(tag)
                val wins = (doc!!.select(".won_all").size - 1
                        + (doc.select(".won_one").size - 1) * .5)
                val missed = doc.select(".missed").size - 1
                val wars = doc.select(".war_hit").size
                val ratio: Double = if (wars == 0) 0 else wins / wars
                val norma = (1.0 + wins) / (2.0 + wars)
                ps = PlayerStats()
                ps.wins = wins.toInt()
                ps.missed = missed
                ps.ratio = ratio
                ps.norma = norma
                ps.wars = wars
                ps.tag = tag
            } catch (ex: Exception) {
                cought(ex)
                sleep()
            }
            c++
        }
        if (ps == null) {
            ps = PlayerStats()
            ps.wins = 0
            ps.missed = 0
            ps.ratio = 0.0
            ps.norma = .5
            ps.wars = 0
            ps.tag = tag
        }
        return ps
    }

    // Deprecated
    private fun lastWin(doc: Document): Int {
        var league = 4
        val eleLeague = doc.select(".won_one .league img," +
                " .won_all .league img").first()
        if (eleLeague != null) {
            val lastLeague = eleLeague
                    .attr("data-cfsrc").toLowerCase()
            if (lastLeague.contains("silver")) league = 3 else if (lastLeague.contains("gold")) league = 2 else if (lastLeague.contains("legend")) league = 1
        }
        return LeagueMap.l2o(league * 10)
    }

    val topClans: List<TopClan>?
        get() {
            var topClans: List<TopClan>? = null
            if (api != null) try {
                val limiter: TimeLimiter = SimpleTimeLimiter.create(ThreadPool.getCachePool())
                val tcp = TopClansRequest.builder().build()
                topClans = limiter.callWithTimeout({ api.getTopClans(tcp) }, timeout.toLong(), TimeUnit.MILLISECONDS)
            } catch (ex: Exception) {
                cought(ex)
                timeout()
            }
            if (topClans == null) try {
                val clans: MutableList<TopClan> = ArrayList()
                val doc = SiteMap.getPage("https://spy.deckshop.pro/top/global/clans")
                val small = doc!!.select(".text-muted")
                val names = doc.select("a.h4")
                var index = 0
                for (muted in small) {
                    val clan = muted.text().trim { it <= ' ' }
                    if (clan.startsWith("#") && !clan.startsWith("# ")) {
                        val topClan = TopClan()
                        topClan.name = names[index++].text()
                        topClan.tag = clan.replace("#", "")
                        clans.add(topClan)
                    }
                }
                topClans = clans
            } catch (ex: IOException) {
                cought(ex)
                sleep()
                return topClans
            }
            return topClans
        }

    private fun getPlayerChests(tag: String): ChestCycle? {
        var chestCycle: ChestCycle? = null
        if (api != null) try {
            val limiter: TimeLimiter = SimpleTimeLimiter.create(ThreadPool.getCachePool())
            chestCycle = limiter.callWithTimeout({ api.getPlayerChests(PlayerChestsRequest.builder(listOf(tag)).build())[0] },
                    timeout.toLong(), TimeUnit.MILLISECONDS)
        } catch (ex: Exception) {
            cought(ex)
            timeout()
        }
        if (chestCycle == null) try {
            val cc = ChestCycle()
            val doc = SiteMap.getPage("https://spy.deckshop.pro/player/$tag")
            var ele = doc!!.selectFirst(".chest-magical ~ span")
            var chestNum = extractNum(ele)
            cc.magical = chestNum.toInt()
            ele = doc.selectFirst(".chest-legendary ~ span")
            chestNum = extractNum(ele)
            cc.legendary = chestNum.toInt()
            ele = doc.selectFirst(".chest-smc ~ span")
            chestNum = extractNum(ele)
            cc.megaLightning = chestNum.toInt()
            chestCycle = cc
        } catch (ex: IOException) {
            cought(ex)
            sleep()
            return getPlayerChests(tag)
        }
        return chestCycle
    }

    private fun extractNum(ele: Element?): String {
        val chestNum: String
        chestNum = if (ele == null) "0" else if (ele.ownText() == "") "0" else ele.ownText()
        return chestNum.replace("+", "")
    }

    fun getClanWar(tag: String): ClanWar? {
        var clanWar: ClanWar? = null
        if (api != null) try {
            var limiter: TimeLimiter = SimpleTimeLimiter.create(ThreadPool.getCachePool())
            clanWar = limiter.callWithTimeout({
                api.getClanWar(ClanWarRequest
                        .builder(tag).build())
            },
                    timeout.toLong(), TimeUnit.MILLISECONDS)
            if (clanWar.clan == null) {
                val clanWarClan = ClanWarClan()
                limiter = SimpleTimeLimiter.create(ThreadPool.getCachePool())
                val clan = limiter.callWithTimeout({
                    api.getClan(ClanRequest.builder(tag)
                            .build())
                }, timeout.toLong(), TimeUnit.MILLISECONDS)
                clanWarClan.badge = clan.badge
                clanWarClan.name = clan.name
                clanWarClan.tag = tag
                clanWar.clan = clanWarClan
            }
        } catch (ex: Exception) {
            cought(ex)
            timeout()
        }
        if (clanWar == null) try {
            val newWar = ClanWar()
            val clan = ClanWarClan()
            val newList: MutableList<ClanWarStanding> = ArrayList()
            val newPars: MutableList<ClanWarParticipant> = ArrayList()
            val doc = SiteMap.getPage("https://royaleapi.com/clan/$tag/war")
            val stats = doc!!.select(".clan_stats > .stats > .value")
            var warState = stats[0].ownText()
            warState = if (warState.contains("War Day")) "warDay" else if (warState.contains("Not")) "notInWar" else "collectionDay"
            newWar.state = warState
            val namae = doc.selectFirst(".p_head_item .header").ownText()
            var badge = doc.selectFirst(".attached .floated")
                    .attr("data-cfsrc").toLowerCase()
            badge = badge.split("/").toTypedArray()[badge.split("/").toTypedArray().size - 1].split("\\.").toTypedArray()[0]
            val clanBadge = Badge()
            clanBadge.name = badge
            clan.badge = clanBadge
            clan.name = namae
            clan.tag = tag
            if (stats.size > 3) {
                val battlesPlayed = stats[2].ownText().toInt()
                val participants = stats[3].ownText().toInt()
                val wTrohpies = stats[5].ownText().toInt()
                val crowns = stats[4].ownText().toInt()
                clan.battlesPlayed = battlesPlayed
                clan.participants = participants
                clan.warTrophies = wTrohpies
                clan.crowns = crowns
                val standings = doc.select(".standings tbody > tr")
                for (standing in standings) {
                    val cws = ClanWarStanding()
                    val clanTag = standing.selectFirst("td:nth-child(2) a")
                            .attr("href").replace("/clan/", "")
                            .replace("/war/", "")
                            .replace("/war", "")
                    val name = standing.selectFirst("td:nth-child(2) a").ownText()
                    val wins = standing.selectFirst(".wins").ownText().toInt()
                    val clanStand = SiteMap.getPage("https://royaleapi.com/clan/$clanTag/war")
                    val clanStats = clanStand!!.select(".clan_stats > .stats > .value")
                    cws.tag = clanTag
                    cws.name = name
                    cws.wins = wins
                    cws.battlesPlayed = clanStats[2].ownText().toInt()
                    cws.participants = clanStats[3].ownText().toInt()
                    cws.warTrophies = clanStats[5].ownText().toInt()
                    cws.crowns = clanStats[4].ownText().toInt()
                    if (clanTag == tag) {
                        clan.tag = clanTag
                        clan.name = name
                        clan.wins = wins
                    }
                    newList.add(cws)
                }
                val parties = doc.select(".roster > tbody tr")
                for (part in parties) {
                    val cwp = ClanWarParticipant()
                    val name = part.selectFirst("td:nth-child(2) a").ownText()
                    val pTag = part.selectFirst("td:nth-child(2) a").attr("href")
                            .replace("/player/", "")
                    var batt = 0
                    var wins = 0
                    val sort = part
                            .selectFirst("td:nth-child(6)")
                            .attr("data-sort-value").toInt()
                    val cards = part
                            .selectFirst("td:nth-child(5)").ownText().toInt()
                    when (sort) {
                        22 -> {
                            wins = 2
                            batt = 2
                        }
                        2 -> batt = 2
                        11 -> {
                            wins = 1
                            batt = 1
                        }
                        1 -> batt = 1
                    }
                    cwp.battlesPlayed = batt
                    cwp.cardsEarned = cards
                    cwp.name = name
                    cwp.tag = pTag
                    cwp.wins = wins
                    newPars.add(cwp)
                }
                newWar.participants = newPars
                newWar.standings = newList
            }
            newWar.clan = clan
            clanWar = newWar
        } catch (ex: Exception) {
            cought(ex)
            sleep()
            clanWar = getClanWar(tag)
        }
        return clanWar
    }

    private fun getClanWarLog(tag: String?): List<ClanWarLog>? {
        if (clanWars.containsKey(tag)) return clanWars[tag]
        var clanWarLogs: List<ClanWarLog>? = null
        if (api != null) try {
            val limiter: TimeLimiter = SimpleTimeLimiter.create(ThreadPool.getCachePool())
            clanWarLogs = limiter.callWithTimeout({
                api.getClanWarLog(ClanWarLogRequest
                        .builder(tag).build())
            },
                    timeout.toLong(), TimeUnit.MILLISECONDS)
        } catch (ex: Exception) {
            cought(ex)
            timeout()
        }
        if (clanWarLogs == null) try {
            val doc = SiteMap.getPage("https://royaleapi.com/clan/$tag/war/log")
            val page = "https://royaleapi.com/clan/$tag/war/analytics/csv"
            val clanURL = URL(page).openConnection()
            clanURL.setRequestProperty("User-Agent", SiteMap.getAgent())
            clanURL.connect()
            val input = clanURL.getInputStream()
            val reader = InputStreamReader(input)
            val records = CSVFormat.DEFAULT.parse(reader).records
            @SuppressLint("SimpleDateFormat") val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmss")
            sdf.timeZone = TimeZone.getTimeZone("GMT")
            val dayMap: MutableMap<String, MutableList<ClanWarLogParticipant>?> = HashMap()
            val logDays: MutableList<ClanWarLog> = ArrayList()
            for (rec in records) if (rec[0] != "name") for (w in 0 until (rec.size() - 5) / 4) if (rec[4 * w + 5] != "" &&
                    !dayMap.containsKey(rec[4 * w + 5])) dayMap[rec[4 * w + 5]] = ArrayList()
            for (rec in records) if (rec[0] != "name") for (w in 0 until (rec.size() - 5) / 4) if (rec[4 * w + 5] != "") {
                val player = ClanWarLogParticipant()
                player.name = rec[0]
                player.tag = rec[1]
                val day = 4 * w + 5
                player.cardsEarned = rec[day + 1].toInt()
                player.battlesPlayed = rec[day + 2].toInt()
                player.wins = rec[day + 3].toInt()
                dayMap[rec[day]]!!.add(player)
            }
            val season = IntArray(10)
            val standList: Array<List<ClanWarLogStanding>> = arrayOfNulls<List<*>>(10)
            var eles = doc!!.select(".ui .inverted .header")
            var counter = 0
            for (ele in eles) if (ele.ownText().startsWith("Season")) season[counter++] = Integer.valueOf(
                    ele.ownText().replace("Season ", ""))
            counter = 0
            eles = doc.select(".standings .unstackable")
            for (ele in eles) {
                val stands = ele.select("tbody tr")
                val cwlsList: MutableList<ClanWarLogStanding> = ArrayList()
                for (stand in stands) {
                    val clanTag = stand.selectFirst("td a")
                            .attr("href")
                            .replace("/clan/", "")
                            .replace("/war/log", "")
                    val warTrophies =
                            stand.selectFirst("td.trophy").ownText().toInt()
                    val trophyChange =
                            stand.selectFirst("td.trophy_change").ownText()
                                    .replace(" ", "")
                                    .replace("+", "").toInt()
                    val cwls = ClanWarLogStanding()
                    cwls.warTrophiesChange = trophyChange
                    cwls.warTrophies = warTrophies
                    cwls.tag = clanTag
                    cwlsList.add(cwls)
                }
                standList[counter++] = cwlsList
            }
            counter = 0
            for ((key, value) in dayMap) {
                val newLog = ClanWarLog()
                val createDate = key.replace(".000Z", "")
                val date = sdf.parse(createDate)
                val time = date.time / 1000 // + 28800;
                newLog.createdDate = time
                newLog.seasonNumber = season[counter]
                newLog.participants = value
                newLog.standings = standList[counter++]
                logDays.add(newLog)
            }
            input.close()
            reader.close()
            clanWarLogs = logDays
        } catch (ex: Exception) {
            cought(ex)
            sleep()
            clanWarLogs = getClanWarLog(tag)
        }
        clanWars[tag] = clanWarLogs
        return clanWarLogs
    }

    private fun getProfile(tag: String): Profile? {
        var profile: Profile? = null
        if (api != null) try {
            val limiter: TimeLimiter = SimpleTimeLimiter.create(ThreadPool.getCachePool())
            profile = limiter.callWithTimeout({
                api.getProfile(ProfileRequest.builder(tag)
                        .build())
            }, timeout.toLong(), TimeUnit.MILLISECONDS)
        } catch (ex: Exception) {
            cought(ex)
            timeout()
        }
        if (profile == null) try {
            val player = Profile()
            val cardList: MutableList<Card> = ArrayList()
            var doc = SiteMap.getPage("https://spy.deckshop.pro/player/$tag/cards")
            val cards = doc!!.select(".card_tag")
            for (element in cards) {
                val card = Card()
                card.maxLevel = 13
                card.displayLevel = element.ownText().replace("Level ", "").toInt()
                cardList.add(card)
            }
            player.cards = cardList
            doc = SiteMap.getPage("https://spy.deckshop.pro/player/$tag")
            val roleElement = doc!!.selectFirst(".clearfix > .text-muted")
            var role: String? = "Member"
            if (roleElement != null) role = roleElement.ownText()
            role = when (role) {
                "Co-leader" -> "coLeader"
                "Leader" -> "leader"
                "Elder" -> "elder"
                else -> "member"
            }
            val pClan = ProfileClan()
            pClan.role = role
            player.clan = pClan
            player.trophies = doc.select(".media-body .text-warning")
                    .text().replace(" ", "").toInt()
            profile = player
        } catch (ex: IOException) {
            cought(ex)
            sleep()
            return getProfile(tag)
        }
        return profile
    }

    private fun getBattles(tag: String): List<Battle>? {
        var battles: List<Battle>? = null
        if (api != null) try {
            val limiter: TimeLimiter = SimpleTimeLimiter.create(ThreadPool.getCachePool())
            battles = limiter.callWithTimeout({
                api.getPlayerBattles(PlayerBattlesRequest.builder(listOf(tag)).build())[0]
            }, timeout.toLong(), TimeUnit.MILLISECONDS)
        } catch (ex: Exception) {
            cought(ex)
            timeout()
        }
        if (battles == null) try {
            val batts: MutableList<Battle> = ArrayList()
            val doc = SiteMap.getPage("https://spy.deckshop.pro/player/$tag/battles")
            val eles = doc!!.select(".timestamp")
            if (eles == null) {
                val battle = Battle()
                battle.utcTime = 0
                batts.add(battle)
            } else for (ele in eles) {
                val battle = Battle()
                val textTime = ele.attributes()["data-timestamp"]
                val time = textTime.toLong()
                battle.utcTime = time
                batts.add(battle)
            }
            battles = batts
        } catch (ex: IOException) {
            cought(ex)
            sleep()
            return getBattles(tag)
        }
        return battles
    }

    private fun getClan(tag: String): ClanStats {
        return getClan(tag, false)
    }

    private fun getClan(tag: String, target: Boolean): ClanStats {
        val clanWar = getClanWar(tag)
        val clan = clanWar!!.clan
        val clanStats = ClanStats()
        clanStats.state = clanWar.state
        clanStats.badge = clan.badge
                .name.toLowerCase()
        clanStats.name = "???"
        clanStats.tag = tag
        Console.Companion.logln("\t\tClan... \t"
                + clan.name)
        clanStats.crowns = 0
        clanStats.remaining = 0
        clanStats.actualWins = 0
        clanStats.estimatedWins = 0
        clanStats.maxParticipants = 0
        clanStats.clan1 = "???"
        clanStats.clan2 = "???"
        clanStats.clan3 = "???"
        clanStats.clan4 = "???"
        clanStats.warTrophies = clan.warTrophies
        clanStats.name = clan.name
        if (clanWar.state == "warDay" && clanWar.clan.warTrophies > 200) {
            if (target) clanStats.remaining = mainClan(clanWar) else clanStats.remaining = maxParticipants - clan.battlesPlayed
            val eew = prognoseWins(clanWar)
            clanStats.maxParticipants = maxParticipants
            clanStats.actualWins = clan.wins
            clanStats.crowns = clan.crowns
            clanStats.estimatedWins = eew.first
            clanStats.extraWins = eew.second
            var counter = 0
            val opponentClan = arrayOfNulls<String>(4)
            for (clanParticipant in clanWar.standings) if (clanParticipant.tag != tag) opponentClan[counter++] = clanParticipant.tag
            clanStats.clan1 = opponentClan[0]
            clanStats.clan2 = opponentClan[1]
            clanStats.clan3 = opponentClan[2]
            clanStats.clan4 = opponentClan[3]
        }
        db.insertClanStats(clanStats)
        return clanStats
    }

    private fun mainClan(mainClan: ClanWar?): Int {
        val standings = mainClan!!.standings
        mainTag = mainClan.clan.tag
        maxParticipants = 0
        for (clan in standings) if (maxParticipants < clan.participants) maxParticipants = clan.participants
        Console.Companion.logln(" \nGetting opposing clans...")
        val latch = CountDownLatch(4)
        for (clan in standings) if (clan.tag != mainClan.clan.tag) ThreadPool.getFixedPool().execute {
            val cs = getClan(clan.tag)
            clanList!!.add(cs)
            latch.countDown()
        }
        try {
            latch.await()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            Log.e(Interface.Companion.TAG, "Latch failed.", e)
        }
        return maxParticipants - mainClan.clan.battlesPlayed
    }

    private fun prognoseWins(clan: ClanWar?): Pair<Int, Double> {
        val playerList = getPlayerStats(clan, false)
        var remainingBattles = maxParticipants
        var poly: PolynomialFunction? = null
        var alreadyWon = 0
        for (player in playerList) {
            if (player.getCurPlay() > 0) {
                remainingBattles -= player.getCurPlay()
                alreadyWon += player.getCurWins()
            } else {
                remainingBattles--
                val winChance = if (player.getPlayed() == 0) .5 else player.getNorma()
                poly = polyMulti(poly, 1 - winChance, winChance)
            }
        }
        for (r in 0 until remainingBattles) poly = polyMulti(poly, .5, .5)
        var probWin = 0
        var maxCoef = 0.0
        var extraWin = 0.0
        var winProbability = 0.0
        if (poly != null) {
            val coef = poly.coefficients
            for (c in coef.indices) if (maxCoef < coef[c]) {
                maxCoef = coef[c]
                probWin = c
            }
            for (c in coef) {
                winProbability += c
                if (winProbability > .8) break
            }
            for (c in probWin + 1 until coef.size) extraWin += coef[c]
        }
        return Pair(probWin + alreadyWon, extraWin)
    }

    private fun polyMulti(poly: PolynomialFunction?, coef1: Double, coef2: Double): PolynomialFunction? {
        var poly = poly
        val tempPoly = PolynomialFunction(doubleArrayOf(coef1, coef2))
        poly = if (poly == null) tempPoly else poly.multiply(tempPoly)
        return poly
    }

    fun getClanStats(tag: String?): List<ClanStats?>? {
        Console.Companion.logln(" \nCalculating forecast...")
        if (acti!!.getLastUse(tag)) {
            db.resetClanStats(tag)
            clanList = ArrayList()
            clanList.add(getClan(tag!!, true))
            acti.setLastUse(tag)
            return clanList
        }
        return db.getClanStatsList(tag)
    }

    private fun getPlayerStats(clanWar: ClanWar?, force: Boolean): List<PlayerStats?> {
        val playerStats = getPlayerStats(clanWar!!.clan.tag, force)
        val currentStats: MutableList<PlayerStats?> = ArrayList()
        for (player in playerStats!!) for (warPlayer in clanWar.participants) if (warPlayer.tag == player.getTag()) {
            player.setCurPlay(warPlayer.battlesPlayed)
            player.setCurWins(warPlayer.wins)
            currentStats.add(player)
        }
        return currentStats
    }

    fun getPlayerStats(clanTag: String?, member: Member, force: Boolean): PlayerStats? {
        val ps: PlayerStats?
        if (acti!!.getLastUse(member.tag, "wstat") || force) {
            Console.Companion.logln("\t\t\t\t\t\t-\t\t" + member.name)
            ps = getWarStats(member.tag)
            ps.name = member.name
            ps.tag = member.tag
            ps.clan = clanTag
            ps.chest = findLastWarWin(ps)
            acti.setLastUse(member.tag, "wstat")
        } else ps = db.getPlayerStats(member.tag)
        ps.setCurrent(true)
        db.insertPlayerStats(ps)
        return ps
    }

    @Deprecated("")
    private fun getPlayerStats(tag: String): List<PlayerStats?>? {
        return getPlayerStats(tag, false)
    }

    private fun getPlayerStats(tag: String, force: Boolean): List<PlayerStats?>? {
        if (acti!!.getLastUse(tag) || force) {
            val members: List<Member>? = getMembers(tag)
            val players: MutableList<PlayerStats?> = ArrayList()
            db.resetCurrentPlayers(tag)
            for (member in members!!) {
                val player = getPlayerStats(tag, member, force)
                player.setCurrent(true)
                players.add(player)
            }
            return players
        }
        return db.getClanPlayerStats(tag)
    }

    private fun findLastWarWin(player: PlayerStats?): Int {
        var chest = 0
        val warLog = getClanWarLog(player.getClan())
        for (dayLog in warLog!!) for (dayPlayer in dayLog.participants) if (dayPlayer.tag == player.getTag()) {
            val warTroph = (dayLog.standings[0].warTrophies
                    - dayLog.standings[0].warTrophiesChange)
            var league = 4
            if (warTroph > 2999) league = 1 else if (warTroph > 1499) league = 2 else if (warTroph > 599) league = 3
            var position = 0
            for (cwls in dayLog.standings) {
                position++
                if (cwls.tag == player.getClan()) break
            }
            val order = LeagueMap.l2o(league * 10 + position)
            val season = if (warLog[0].seasonNumber
                    != dayLog.seasonNumber) 0 else dayLog.seasonNumber
            val newChest = order + season * 100
            if (chest < newChest) chest = newChest
        }
        return chest
    }

    @Deprecated("")
    private fun getPlayer(clanTag: String,
                          member: Member, warLog: List<ClanWarLog>): PlayerStats {
        val player = PlayerStats()
        player.name = member.name
        player.tag = member.tag
        player.clan = clanTag
        player.isCurrent = true
        val playerDB = db.getPlayerStats(member.tag)
        var missed = playerDB?.missed ?: 0
        var played = playerDB?.played ?: 0
        var chest = playerDB?.chest ?: 0
        var cards = playerDB?.cards ?: 0
        var wins = playerDB?.wins ?: 0
        var wars = playerDB?.wars ?: 0
        for (dayLog in warLog) {
            var found = false
            for (dayDB in db.warDays) if (dayDB.warDay == dayLog.createdDate) found = true
            if (!found) for (dayPlayer in dayLog.participants) if (dayPlayer.tag == player.tag) {
                val warTroph = dayLog.standings[0].warTrophies
                if (dayPlayer.battlesPlayed == 0) missed++
                played += dayPlayer.battlesPlayed
                cards += dayPlayer.cardsEarned
                wins += dayPlayer.wins
                wars++
                var league = 4
                if (warTroph > 2999) league = 1 else if (warTroph > 1499) league = 2 else if (warTroph > 599) league = 3
                var position = 0
                for (cwls in dayLog.standings) {
                    position++
                    if (cwls.tag == player.clan) break
                }
                val order = LeagueMap.l2o(league * 10 + position)
                val season = if (warLog[0].seasonNumber
                        != dayLog.seasonNumber) 0 else dayLog.seasonNumber
                val newChest = order + season * 100
                if (chest < newChest) chest = newChest
            }
        }
        val ratio: Double = if (played == 0) 0 else wins.toDouble() / played
        val norma = (1.0 + wins) / (2.0 + played)
        player.missed = missed
        player.played = played
        player.cards = cards
        player.norma = norma
        player.ratio = ratio
        player.chest = chest
        player.wars = wars
        player.wins = wins
        db.insertPlayerStats(player)
        return player
    }

    fun getPlayerProfile(tag: String, force: Boolean): ClanPlayer? {
        var player = db.getClanPlayer(tag)
        if (acti!!.getLastUse(tag, "prof") || force) {
            if (player == null) player = ClanPlayer()
            val profile = getProfile(tag)
            player.clan = mainTag
            player.tag = tag
            var cardCount = 0.0
            for (card in profile!!.cards) {
                val max = card.maxLevel
                val lvl = card.displayLevel
                val dif = max - lvl
                cardCount += if (dif < 1) 1.1 else 1.0 / dif
            }
            val score = (cardCount / profile
                    .cards.size * 8182.73).toInt()
            val role = if (profile.clan == null) "N/A" else profile.clan.role
            player.role = role
            player.trophies = profile.trophies
            player.score = score
            db.insertClanPlayer(player)
            acti.setLastUse(tag, "prof")
            acti.setLastForce(1)
        }
        return player
    }

    fun getMemberActivity(player: ClanPlayer, force: Boolean): ClanPlayer {
        val tag = player.tag
        var changed = false
        if (acti!!.getLastUse(tag, "chest") || force) {
            val cc = getPlayerChests(tag)
            player.smc = cc!!.megaLightning
            player.legendary = cc.legendary
            player.magical = cc.magical
            acti.setLastUse(tag, "chest")
            changed = true
        }
        if (acti.getLastUse(tag, "batt") || force) {
            val battles = getBattles(tag)
            var last: Long = 0
            for (battle in battles!!) if (last < battle.utcTime) last = battle.utcTime
            player.last = last
            acti.setLastUse(tag, "batt")
            changed = true
        }
        if (changed) {
            db.insertClanPlayer(player)
            acti.setLastForce(2)
        }
        return player
    }

    @Deprecated("")
    fun getMissingPlayer(member: Member, clanTag: String?): PlayerStats {
        val newPlayer = PlayerStats()
        newPlayer.tag = member.tag
        newPlayer.name = member.name
        newPlayer.clan = clanTag
        newPlayer.cards = 0
        newPlayer.wars = 0
        newPlayer.curPlay = 0
        newPlayer.curWins = 0
        newPlayer.isCurrent = false
        newPlayer.ratio = 0.0
        newPlayer.norma = .5
        newPlayer.missed = 0
        newPlayer.played = 0
        newPlayer.wins = 0
        db.insertPlayerStats(newPlayer)
        return newPlayer
    }

    @Synchronized
    private fun hasInternet(): Boolean {
        if (System.currentTimeMillis() - lastCheck > 30000) {
            internet = (testSite("www.google.com")
                    || testSite("www.amazon.com")
                    || testSite("www.yahoo.com"))
            lastCheck = System.currentTimeMillis()
        }
        return internet
    }

    private fun testSite(site: String): Boolean {
        try {
            val address = InetAddress.getByName(site)
            return address != ""
        } catch (e: UnknownHostException) {
            Log.e(Interface.Companion.TAG, "No internet!", e)
        }
        return false
    }

    companion object {
        // Make api == null if you don't have a developer key.
// private static Api api = new Api("https://api.royaleapi.com/", devKey);
// It's temporarly null, untile the API is updated to v2.
        private val api: Api? = null
    }

}