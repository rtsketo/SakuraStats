package eu.rtsketo.sakurastats.control

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.qwerjk.better_text.MagicTextView
import eu.rtsketo.sakurastats.R
import eu.rtsketo.sakurastats.hashmaps.SDPMap
import eu.rtsketo.sakurastats.main.Interface
import eu.rtsketo.sakurastats.main.Service

class DialogView(type: SakuraDialog, prevDia: Dialog?, count: Int, acti: Interface?) {
    enum class SakuraDialog {
        INPUT, INFO, CLANQUEST, RATEQUEST
    }

    constructor(type: SakuraDialog, acti: Interface?) : this(type, null, -1, acti)
    constructor(type: SakuraDialog, count: Int, acti: Interface?) : this(type, null, count, acti)
    constructor(type: SakuraDialog, prevDia: Dialog?, acti: Interface?) : this(type, prevDia, -1, acti)

    internal inner class InputCheck(private val editText: EditText) : TextWatcher {
        private var clanTagInput = ""
        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
        override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
        override fun afterTextChanged(input: Editable) {
            var tag = input.toString()
            if (clanTagInput != tag) {
                tag = "#" + tag.toUpperCase()
                        .replace("O", "0")
                        .replace("[^0289CGJLPQRUVY]".toRegex(), "")
                clanTagInput = tag
                editText.setText(tag, TextView.BufferType.EDITABLE)
                editText.setSelection(tag.length)
            }
        }

    }

    init {
        val inputView: View
        inputView = if (type == SakuraDialog.INPUT) LayoutInflater.from(acti)
                .inflate(R.layout.dialog_input, null) else LayoutInflater.from(acti)
                .inflate(R.layout.dialog_quest, null)
        val dialog = Dialog(acti!!)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window!!.setBackgroundDrawableResource(
                android.R.color.transparent)
        dialog.setContentView(inputView)
        dialog.setCancelable(false)
        val explain: MagicTextView = dialog.findViewById(R.id.inputExplaination)
        val explain2: MagicTextView = dialog.findViewById(R.id.inputExplaination2)
        val confirm = dialog.findViewById<ImageView>(R.id.inputConfirmButton)
        val cancel = dialog.findViewById<ImageView>(R.id.inputCancelButton)
        val input = dialog.findViewById<EditText>(R.id.inputClanTag)
        val size = intArrayOf(SDPMap.Companion.sdp2px(9), SDPMap.Companion.sdp2px(8))
        when (type) {
            SakuraDialog.INPUT -> {
                val loadingAnim = dialog.findViewById<ImageView>(R.id.loadingAnim)
                input.addTextChangedListener(InputCheck(input))
                if (count > 0) decorate(explain, "Enter a", size[0]) else decorate(explain, "Enter your", size[0])
                decorate(explain2, "clan's tag!", size[0])
                ViewDecor.decorate(input, size[0])
                confirm.setOnClickListener { view: View ->
                    ViewDecor.bounce(view, acti)
                    loadingAnim.visibility = View.VISIBLE
                    var tag = input.text.toString()
                    tag = tag.toUpperCase()
                            .replace("O", "0")
                            .replace("[^0289CGJLPQRUVY]".toRegex(), "")
                    cancel.isEnabled = false
                    confirm.isEnabled = false
                    cancel.setColorFilter(Color
                            .argb(100, 200, 200, 200))
                    confirm.setColorFilter(Color
                            .argb(100, 200, 200, 200))
                    (loadingAnim.drawable as AnimationDrawable).start()
                    val finalTag = tag
                    ThreadPool.getCachePool().execute {
                        val df = DataFetch(acti)
                        if (finalTag.length > 2 && df.checkClan(finalTag)) {
                            if (count > -1) {
                                acti.setStoredClan(count, finalTag)
                                acti.settiFrag.selectClan(count)
                                acti.settiFrag.refreshStored(
                                        count, false)
                            } else {
                                acti.lastClan = finalTag
                                acti.setStoredClan(0, finalTag)
                                Service.Companion.getThread(acti)
                                        .start(finalTag, false, true)
                            }
                            dialog.cancel()
                        } else acti.runOnUiThread {
                            (loadingAnim
                                    .drawable as AnimationDrawable).stop()
                            DialogView(SakuraDialog.INFO, acti)
                            loadingAnim.visibility = View.INVISIBLE
                            confirm.colorFilter = null
                            cancel.colorFilter = null
                            confirm.isEnabled = true
                            cancel.isEnabled = true
                        }
                    }
                }
                cancel.setOnClickListener { view: View ->
                    ViewDecor.bounce(view, acti)
                    if (count > -1) dialog.cancel() else DialogView(SakuraDialog.CLANQUEST, dialog, acti)
                }
            }
            SakuraDialog.CLANQUEST -> {
                decorate(explain, "Are you sure?", size[0])
                decorate(explain2, "A random clan" +
                        "\nwill be chosen.", size[1])
                cancel.setOnClickListener { view: View ->
                    ViewDecor.bounce(view, acti)
                    dialog.cancel()
                }
                confirm.setOnClickListener { view: View ->
                    Service.Companion.getThread(acti)
                            .start(null, false, true)
                    ViewDecor.bounce(view, acti)
                    dialog.cancel()
                    prevDia!!.cancel()
                }
            }
            SakuraDialog.RATEQUEST -> {
                decorate(explain, "This appears only once!", size[0])
                decorate(explain2, "\nSince you are using this" +
                        "\napp for sometime now," +
                        "\nplease consider rating it." +
                        "\n\n\nYour feedback is important!", size[1])
                cancel.setOnClickListener { view: View ->
                    ViewDecor.bounce(view, acti)
                    dialog.cancel()
                }
                confirm.setOnClickListener { view: View ->
                    ViewDecor.bounce(view, acti)
                    val uri = Uri.parse("market://details?id=" + acti.packageName)
                    val goToMarket = Intent(Intent.ACTION_VIEW, uri)
                    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or
                            Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    try {
                        ContextCompat.startActivity(acti, goToMarket, null)
                    } catch (e: ActivityNotFoundException) {
                        ContextCompat.startActivity(acti, Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://play.google.com/store/apps/details?id=" + acti.packageName)), null)
                    }
                    dialog.cancel()
                }
            }
            SakuraDialog.INFO -> {
                dialog.setCancelable(true)
                cancel.visibility = View.GONE
                decorate(explain, "Wrong tag!", size[0])
                decorate(explain2, "Tag doesn't exist" +
                        "\nor bad connection.", size[1])
                confirm.setOnClickListener { view: View ->
                    ViewDecor.bounce(view, acti)
                    dialog.cancel()
                }
            }
        }
        dialog.show()
    }
}