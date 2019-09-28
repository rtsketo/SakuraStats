package eu.rtsketo.sakurastats.hashmaps;

import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

public class SiteMap {
    private SiteMap() {}
    private static long lastFestch;
    private static final int delay = 120;
    private static Map<String, Document> pageMap = new HashMap<>();
    private static final String agent = "Mozilla/5.0 " +
            "(Linux; Android 6.0; Nexus 5 Build/MRA58N) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/64.0.3282.186 Mobile Safari/537.36";

    public static String getAgent() { return  agent; }
    public static void clearPages() { pageMap.clear(); }
    public static Document getPage(String page) throws IOException {
        if(pageMap.containsKey(page)) return pageMap.get(page);

        getPermissionToFetch();
        Log.v("SiteFetch", page);

        URLConnection clanURL = new URL(page).openConnection();
        clanURL.setRequestProperty("User-Agent", agent);
        clanURL.connect();
        InputStream input = clanURL.getInputStream();
        Document doc =  Jsoup.parse(input,"UTF-8", page);
        if (page.contains("royaleapi")) pageMap.put(page, doc);
        input.close();
        return doc;
    }

    public static Document getCWPage(String tag) throws IOException {
        if(pageMap.containsKey("CWPage" + tag)) return pageMap.get("CWPage" + tag);
        String page = "https://royaleapi.com/inc/player/cw_history/" + tag;

        getPermissionToFetch();
        Log.v("SiteFetch", page);

        URLConnection clanURL = new URL(page).openConnection();
        clanURL.setRequestProperty("Accept", "*/*");
        clanURL.setRequestProperty("Referer", "https://royaleapi.com/player/" + tag);
        clanURL.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        clanURL.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)" +
                " AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");
        clanURL.setRequestProperty("Sec-Fetch-Mode", "cors");
        clanURL.connect();
        InputStream input = clanURL.getInputStream();
        Document doc =  Jsoup.parse(input,"UTF-8", page);
        pageMap.put("CWPage" + tag, doc);
        input.close();
        return doc;
    }

    private static void getPermissionToFetch() {
        boolean permission = false;

        while(!permission) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            synchronized (SiteMap.class) {
                long currentTime = System.currentTimeMillis();
                if (lastFestch + delay < currentTime) {
                    lastFestch = currentTime;
                    permission = true;
                }
            }
        }

    }

}
