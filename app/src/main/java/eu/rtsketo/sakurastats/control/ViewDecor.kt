package eu.rtsketo.sakurastats.control

import android.app.Activity
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.AnimationDrawable
import android.util.TypedValue
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.ImageView
import com.qwerjk.better_text.MagicTextView
import eu.rtsketo.sakurastats.R
import eu.rtsketo.sakurastats.hashmaps.SDPMap.Companion.sdp2px

object ViewDecor {
    private lateinit var tf: Typeface
    fun init(res: Resources) {
        tf = Typeface.createFromAsset(res.assets, "" +
                "fonts/somefont.ttf")
    }

    fun animateView(view: ImageView, animate: Boolean) {
        val anim = view.drawable
        if (anim is AnimationDrawable) if (animate) anim.start() else anim.stop()
    }

    fun blinkView(view: ImageView, blink: Boolean) {
        if (blink) {
            val animation: Animation = AlphaAnimation(1f, .5f)
            animation.duration = 400
            animation.interpolator = LinearInterpolator()
            animation.repeatCount = Animation.INFINITE
            animation.repeatMode = Animation.REVERSE
            view.startAnimation(animation)
        } else view.clearAnimation()
    }

    fun decorate(tv: MagicTextView, txt: Int, size: Float)
            = decorate(tv, "" + txt, size, Color.WHITE)
    fun decorate(tv: MagicTextView, txt: Double, size: Float)
            = decorate(tv, "" + txt, size, Color.WHITE)
    fun decorate(tv: MagicTextView, txt: Int, size: Float, color: Int)
            = decorate(tv, "" + txt, size, color)
    fun decorate(tv: MagicTextView, txt: Double, size: Float, color: Int)
            = decorate(tv, "" + txt, size, color)
    fun decorate(tv: MagicTextView, txt: Int, size: Int)
            = decorate(tv, "" + txt, size, Color.WHITE)

    @JvmOverloads
    fun decorate(tv: MagicTextView, txt: String,
                 size: Int, color: Int = Color.WHITE,
                 maxWidth: Int = -1)
            = decorate(tv, txt, size.toFloat(), color, maxWidth)

    fun decorate(et: EditText, size: Int): EditText {
        et.setTextSize(TypedValue.COMPLEX_UNIT_PX, size.toFloat())
        et.setTextColor(Color.BLACK)
        et.typeface = tf
        return et
    }

    @JvmOverloads
    fun decorate(tv: MagicTextView, txt: String,
                 size: Float, color: Int = Color.WHITE,
                 maxWidth: Int = -1): MagicTextView {
        tv.clearOuterShadows()
        tv.text = txt
        tv.typeface = tf
        tv.setTextColor(color)
        tv.setStroke((sdp2px(1) - 2.toFloat()).coerceAtLeast(.8f), Color.BLACK)
        tv.addOuterShadow(
                (sdp2px(1) - 2.toFloat()).coerceAtLeast(.8f),
                (sdp2px(1) - 2.toFloat()).coerceAtLeast(.8f),
                (sdp2px(2) - 2.toFloat()).coerceAtLeast(.8f), Color.BLACK)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
        tv.measure(0, 0)
        if (maxWidth > 0) {
            val width = tv.measuredWidth.toFloat()
            if (width > sdp2px(maxWidth)) return decorate(tv, txt,
                    (size - .5).toFloat(), color, maxWidth)
        }
        return tv
    }

    fun bounce(v: View, a: Activity) {
        v.startAnimation(AnimationUtils.loadAnimation(a, R.anim.bounce))
    }

    fun rotate(v: View, a: Activity) {
        v.startAnimation(AnimationUtils.loadAnimation(a, R.anim.rotate))
    }
}