package eu.rtsketo.sakurastats.control;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.ImageView;

import com.qwerjk.better_text.MagicTextView;

import eu.rtsketo.sakurastats.R;

import static eu.rtsketo.sakurastats.hashmaps.SDPMap.sdp2px;
import static java.lang.Math.max;

public class ViewDecor {
    private static Typeface tf;
    private ViewDecor() {}

    public static void init(Resources res) {
        tf = Typeface.createFromAsset(res.getAssets(),
                    "fonts/Supercell-Magic_5.ttf");
    }

    public static void animateView(ImageView view, boolean animate) {
        Drawable anim = view.getDrawable();
        if (anim instanceof AnimationDrawable)
            if (animate) ((AnimationDrawable) anim).start();
            else ((AnimationDrawable) anim).stop();
    }

    public static void blinkView(ImageView view, boolean blink) {
        if (blink) {
            Animation animation = new AlphaAnimation(1, .5f);
            animation.setDuration(400);
            animation.setInterpolator(new LinearInterpolator());
            animation.setRepeatCount(Animation.INFINITE);
            animation.setRepeatMode(Animation.REVERSE);
            view.startAnimation(animation); }
        else view.clearAnimation(); }

    public static MagicTextView decorate(MagicTextView tv, String txt, float size) {
        return decorate(tv, txt, size, Color.WHITE); }

    public static MagicTextView decorate(MagicTextView tv, int txt, float size) {
        return decorate(tv, ""+txt, size, Color.WHITE); }

    public static MagicTextView decorate(MagicTextView tv, double txt, float size) {
        return decorate(tv, ""+txt, size, Color.WHITE); }

    public static MagicTextView decorate(MagicTextView tv, int txt, float size, int color) {
        return decorate(tv, ""+txt, size, color); }

    public static MagicTextView decorate(MagicTextView tv, double txt, float size, int color) {
        return decorate(tv, ""+txt, size, color); }

    public static EditText decorate(EditText et, int size) {
        et.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        et.setTextColor(Color.BLACK);
        et.setTypeface(tf);
        return et;
    }

    public static MagicTextView decorate(MagicTextView tv, String txt, float size, int color) {
        return decorate(tv, txt, size, color, -1); }

    public static MagicTextView decorate(MagicTextView tv, String txt,
                                         float size, int color, int maxWidth) {
        tv.clearOuterShadows();
        tv.setText(txt);
        tv.setTypeface(tf);
        tv.setTextColor(color);
        tv.setStroke(max(sdp2px(1)-2,.8f), Color.BLACK);
        tv.addOuterShadow(max(sdp2px(1)-2,.8f),
                max(sdp2px(1)-2,.8f),max(sdp2px(2)-2,.8f),Color.BLACK);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        tv.measure(0,0);

        if (maxWidth > 0) {
            float width = tv.getMeasuredWidth();
            if (width > sdp2px(maxWidth))
                return decorate(tv, txt,
                        (float) (size - .5), color, maxWidth);
        }

        return tv;
    }

    public static void bounce(View v, Activity a) {
        v.startAnimation(AnimationUtils.loadAnimation(a, R.anim.bounce)); }

    public static void rotate(View v, Activity a) {
        v.startAnimation(AnimationUtils.loadAnimation(a, R.anim.rotate)); }
}
