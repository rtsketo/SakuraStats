package eu.rtsketo.sakurastats.hashmaps;

import android.content.res.Resources;
import android.util.SparseIntArray;

import eu.rtsketo.sakurastats.BuildConfig;

public class SDPMap {
    private static SparseIntArray sdpMap;
    private static Resources res;

    public static int sdp2px(int sdp) {
        if (sdp < 1) return -1;
        if (sdpMap == null) sdpMap = new SparseIntArray();
        int px = sdpMap.get(sdp, 1337);
        if (px == 1337) {
            String pack = BuildConfig.APPLICATION_ID;
            int id = res.getIdentifier("_" +
                    sdp + "sdp", "dimen", pack);
            px = res.getDimensionPixelSize(id);
            sdpMap.put(sdp, px); }
        return px; }

    public static int dp2px(float dp) {
        return Math.round(dp * Resources.getSystem().getDisplayMetrics().density); }

    public static float px2dp(float px) {
        return px / Resources.getSystem().getDisplayMetrics().density; }

    public static float sdp2dp(int sdp) {
        return px2dp(sdp2px(sdp)); }

    public int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels; }

    public int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels; }

    public static void init(Resources res) { SDPMap.res = res; }
}
