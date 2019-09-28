package eu.rtsketo.sakurastats.main;

import android.app.Activity;

import com.qwerjk.better_text.MagicTextView;

import static eu.rtsketo.sakurastats.control.ViewDecor.decorate;
import static eu.rtsketo.sakurastats.hashmaps.SDPMap.sdp2px;

public class Console {
    private Activity activity;
    private MagicTextView mtv;
    private static Console instance;
    private StringBuilder log = new StringBuilder();

    private Console(MagicTextView mtv) {
        this.mtv = mtv;
    }

    private synchronized void appendText(String text, boolean newLine) {
        String[] lines  = log.toString().split("\n");
        if (lines.length > 20)
            log.replace(0, lines[0].length()+1, "");

        log.append(text);
        if (newLine) log.append("\n");
        activity.runOnUiThread(()->
                decorate(mtv, log.toString(), sdp2px(8)));
    }
    public static void logln(String text) {
        if (instance != null)
            instance.appendText(text, true);
    }

    public static void log(String text) {
        if (instance != null)
            instance.appendText(
                    text + " ", false);
    }

    public static void create(Activity activity, MagicTextView mtv) {
        instance = new Console(mtv);
        instance.activity = activity;
    }

    public static String convertRole(String role) {
        if (role == null) return "";
        switch (role) {
            case "coLeader": role = "Co-Leader "; break;
            case "leader": role = "Leader     "; break;
            case "elder": role = "Elder        "; break;
            default: role = "Member    ";
        }
        return role;
    }
}
