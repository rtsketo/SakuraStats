package eu.rtsketo.sakurastats.hashmaps

import android.util.Log
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.URL
import java.util.*

object SiteMap {
    private var lastFestch: Long = 0
    private const val delay = 120
    private val pageMap: MutableMap<String, Document?> = HashMap()
    const val agent = "Mozilla/5.0 " +
            "(Linux; Android 6.0; Nexus 5 Build/MRA58N) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/64.0.3282.186 Mobile Safari/537.36"

    fun clearPages() {
        pageMap.clear()
    }

    @Throws(IOException::class)
    fun getPage(page: String): Document? {
        if (pageMap.containsKey(page)) return pageMap[page]
        permissionToFetch
        Log.v("SiteFetch", page)
        val clanURL = URL(page).openConnection()
        clanURL.setRequestProperty("User-Agent", agent)
        clanURL.connect()
        val input = clanURL.getInputStream()
        val doc = Jsoup.parse(input, "UTF-8", page)
        if (page.contains("royaleapi")) pageMap[page] = doc
        input.close()
        return doc
    }

    @Throws(IOException::class)
    fun getCWPage(tag: String): Document? {
        if (pageMap.containsKey("CWPage$tag")) return pageMap["CWPage$tag"]
        val page = "https://royaleapi.com/inc/player/cw_history/$tag"
        permissionToFetch
        Log.v("SiteFetch", page)
        val clanURL = URL(page).openConnection()
        clanURL.setRequestProperty("Accept", "*/*")
        clanURL.setRequestProperty("Referer", "https://royaleapi.com/player/$tag")
        clanURL.setRequestProperty("X-Requested-With", "XMLHttpRequest")
        clanURL.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)" +
                " AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36")
        clanURL.setRequestProperty("Sec-Fetch-Mode", "cors")
        clanURL.connect()
        val input = clanURL.getInputStream()
        val doc = Jsoup.parse(input, "UTF-8", page)
        pageMap["CWPage$tag"] = doc
        input.close()
        return doc
    }

    private val permissionToFetch: Unit
        private get() {
            var permission = false
            while (!permission) {
                try {
                    Thread.sleep(50)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                synchronized(SiteMap::class.java) {
                    val currentTime = System.currentTimeMillis()
                    if (lastFestch + delay < currentTime) {
                        lastFestch = currentTime
                        permission = true
                    }
                }
            }
        }
}