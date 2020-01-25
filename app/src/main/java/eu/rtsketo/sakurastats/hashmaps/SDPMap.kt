package eu.rtsketo.sakurastats.hashmaps

import android.content.res.Resources
import android.util.SparseIntArray
import androidx.multidex.BuildConfig

class SDPMap {
    val screenWidth: Int
        get() = Resources.getSystem().displayMetrics.widthPixels

    val screenHeight: Int
        get() = Resources.getSystem().displayMetrics.heightPixels

    companion object {
        private var sdpMap: SparseIntArray? = null
        private var res: Resources? = null
        fun sdp2px(sdp: Int): Int {
            if (sdp < 1) return -1
            if (sdpMap == null) sdpMap = SparseIntArray()
            var px = sdpMap!![sdp, 1337]
            if (px == 1337) {
                val pack = BuildConfig.APPLICATION_ID
                val id = res!!.getIdentifier("_" +
                        sdp + "sdp", "dimen", pack)
                px = res!!.getDimensionPixelSize(id)
                sdpMap!!.put(sdp, px)
            }
            return px
        }

        fun dp2px(dp: Float): Int {
            return Math.round(dp * Resources.getSystem().displayMetrics.density)
        }

        fun px2dp(px: Float): Float {
            return px / Resources.getSystem().displayMetrics.density
        }

        fun sdp2dp(sdp: Int): Float {
            return px2dp(sdp2px(sdp).toFloat())
        }

        fun init(res: Resources?) {
            Companion.res = res
        }
    }
}