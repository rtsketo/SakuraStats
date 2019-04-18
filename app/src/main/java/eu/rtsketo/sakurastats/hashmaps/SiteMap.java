package eu.rtsketo.sakurastats.hashmaps;

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
    private static Map<String, Document> pageMap = new HashMap<>();
    private static final String agent = "Mozilla/5.0 " +
            "(Linux; Android 6.0; Nexus 5 Build/MRA58N) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/64.0.3282.186 Mobile Safari/537.36";

    public static String getAgent() { return  agent; }
    public static void clearPages() { pageMap.clear(); }
    public static Document getPage(String page) throws IOException {
        if(pageMap.containsKey(page)) return pageMap.get(page);
        URLConnection clanURL = new URL(page).openConnection();
        clanURL.setRequestProperty("User-Agent", agent);
        clanURL.connect();
        InputStream input = clanURL.getInputStream();
        Document doc =  Jsoup.parse(input,"UTF-8", page);
        if (page.contains("royaleapi")) pageMap.put(page, doc);
        input.close();
        return doc;
    }
}
