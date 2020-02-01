package eu.rtsketo.sakurastats.main

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import eu.rtsketo.sakurastats.control.DAObject
import eu.rtsketo.sakurastats.control.DataFetch
import eu.rtsketo.sakurastats.control.DataRoom
import eu.rtsketo.sakurastats.control.ThreadPool
import eu.rtsketo.sakurastats.dbobjects.ClanPlayer
import eu.rtsketo.sakurastats.dbobjects.PlayerStats
import eu.rtsketo.sakurastats.hashmaps.PlayerMap
import eu.rtsketo.sakurastats.hashmaps.SiteMap
import jcrapi.model.Member
import jcrapi.model.TopClan
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch

class Service private constructor(private val acti: Interface) {
    private var thread: Thread? = null
    private var df = DataFetch(acti)
    private var force = false
    private var stop = false
    @Synchronized
    fun start(tag: String, force: Boolean, tab: Boolean) {
        ThreadPool.cachePool.execute {
            if (thread != null) try {
                stop = true
                thread!!.join()
                stop = false
            } catch (e: InterruptedException) {
                Log.e(Interface.Companion.TAG, "Join failed", e)
                thread!!.interrupt()
            }

            this.force = force
            thread = Thread(Runnable {
                val tp: MutableList<TopClan> = ArrayList()
                val sakura = TopClan()
                sakura.name = "Sakura Frontier"
                sakura.tag = "2YCJRUC"
                tp.add(sakura)
                if (tag.isNotEmpty()) collectData(tag)
                else if (acti.lastClan.isNotEmpty())
                    collectData(acti.lastClan)
                else {
                    Console.logln(" \nChecking top clans...")
                    tp.addAll(df.topClans)
                    for (clan in tp) {
                        val cTag = clan.tag
                        Console.Companion.logln("\t\t\t\t\t\t-\t\t"
                                + clan.name)
                        if (df.getClanWar(cTag).state == "warDay") {
                            acti.lastClan = cTag
                            collectData(cTag)
                            break
                        }
                    }
                }
            })

            thread!!.priority = Thread.MIN_PRIORITY
            thread!!.start()
        }
    }

    private fun collectData(cTag: String) {
        val pm: PlayerMap = PlayerMap.instance
        val db= DataRoom.instance?.dao
        acti.runOnUiThread {
            acti.progFrag.console.visibility = View.VISIBLE
        }
        acti.progFrag.removeViews()
        SiteMap.clearPages()
        val members: MutableList<Member>
        if (acti.getLastUse(cTag) || force) members = df.getMembers(cTag) else {
            members = ArrayList()
            db?.apply {
                for (player
                in getClanPlayerStats(cTag)) {
                    val member = Member()
                    member.name = player.name
                    member.tag = player.tag
                    members.add(member)
                }
            }
        }
        pm.reset(members.size)
        val psLatch = CountDownLatch(members.size)
        val ps = Collections.synchronizedList(ArrayList<PlayerStats>())
        val cp = Collections.synchronizedList(ArrayList<ClanPlayer>())
        Console.Companion.logln(" \nFetching clan members...")
        for (member in members) ThreadPool.fixedPool.execute {
            if (!stop && member != null) {
                val playerStats = df.getPlayerStats(cTag, member, force)
                pm.put(member.tag, playerStats)
                ps.add(playerStats)
            }
            psLatch.countDown()
            updateLoading((members.size - psLatch.count).toInt(), members.size)
        }
        waitLatch(psLatch)
        Collections.reverse(ps)
        Console.Companion.logln(" \nFetching member stats...")
        val cpLatch = CountDownLatch(ps.size)
        for (playerStats in ps) ThreadPool.fixedPool.execute {
            if (!stop && playerStats != null) {
                val pTag = playerStats.tag
                val clanPlayer = df.getPlayerProfile(pTag, force)
                Console.Companion.logln("\t\t" +
                        Console.Companion.convertRole(clanPlayer.role)
                        + "\t\t" + playerStats.name)
                pm.put(pTag, clanPlayer)
                cp.add(clanPlayer)
            }
            cpLatch.countDown()
            updateLoading((ps.size * 2 - cpLatch.count).toInt(), ps.size)
        }
        waitLatch(cpLatch)
        acti.warFrag.loading = false
        Console.Companion.logln(" \nFetching member activity...")
        val acLatch = CountDownLatch(cp.size)
        @SuppressLint("SimpleDateFormat") val sdf = SimpleDateFormat("MM/dd HH:mm")
        for (tempPlayer in cp) ThreadPool.fixedPool.execute {
            if (!stop && tempPlayer != null) {
                val pTag = tempPlayer.tag
                val clanPlayer = df.getMemberActivity(tempPlayer, force)
                Console.Companion.logln("\t\t" +
                        sdf.format(Date(clanPlayer.last * 1000))
                        + "\t\t" + clanPlayer.tag)
                pm.put(pTag, clanPlayer)
            }
            acLatch.countDown()
            updateLoading((ps.size * 3 - acLatch.count).toInt(), ps.size)
        }
        waitLatch(acLatch)
        pm.completeSubs()
        acti.progFrag.refresh(false)
        acti.settiFrag.refreshStored()
        acti.incUseCount()
    }

    private fun waitLatch(latch: CountDownLatch) {
        try {
            latch.await()
        } catch (e: InterruptedException) {
            Log.e(Interface.Companion.TAG, "Latch failed", e)
            Thread.currentThread().interrupt()
        }
    }

    private fun updateLoading(cur: Int, max: Int) {
        val warMax = 2 * max
        val actMax = 3 * max
        if (cur < warMax) acti.warFrag.updateLoading(cur, warMax)
        acti.actiFrag.updateLoading(cur, actMax)
    }

    companion object {
        private var bth: Service? = null
        fun getThread(): Service {
            if (bth == null) throw NullPointerException(
                    "Service not initialized properly.")
            return bth as Service
        }

        fun getThread(acti: Interface): Service {
            if (bth == null || getThread().acti != acti) bth = Service(acti)
            return bth as Service
        }
    }

}