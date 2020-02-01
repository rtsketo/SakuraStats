package eu.rtsketo.sakurastats.main

import android.app.Activity
import com.qwerjk.better_text.MagicTextView
import eu.rtsketo.sakurastats.control.ViewDecor.decorate
import eu.rtsketo.sakurastats.hashmaps.SDPMap

class Console private constructor(private val mtv: MagicTextView) {
    private var activity: Activity? = null
    private val log = StringBuilder()

    @Synchronized
    private fun appendText(text: String, newLine: Boolean) {
        val lines = log.toString().split("\n").toTypedArray()
        if (lines.size > 20) log.replace(0, lines[0].length + 1, "")
        log.append(text)
        if (newLine) log.append("\n")
        activity?.runOnUiThread {
            decorate(mtv, log.toString(),
                    SDPMap.sdp2px(8).toFloat()) }
    }

    companion object {
        private var instance: Console? = null
        fun logln(text: String) {
            instance?.appendText(text, true)
        }

        fun log(text: String) {
            instance?.appendText("$text ", false)
        }

        fun create(activity: Activity, mtv: MagicTextView) {
            instance = Console(mtv)
            instance!!.activity = activity
        }

        fun convertRole(role: String): String {
            return when (role) {
                "coLeader" -> "Co-Leader "
                "leader" -> "Leader     "
                "elder" -> "Elder        "
                else -> "Member    "
            }
        }
    }

}