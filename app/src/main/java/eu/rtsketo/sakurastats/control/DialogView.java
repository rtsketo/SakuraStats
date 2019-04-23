package eu.rtsketo.sakurastats.control;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;

import com.qwerjk.better_text.MagicTextView;

import eu.rtsketo.sakurastats.R;
import eu.rtsketo.sakurastats.main.Interface;
import eu.rtsketo.sakurastats.main.Service;

import static android.support.v4.content.ContextCompat.startActivity;
import static eu.rtsketo.sakurastats.control.ThreadPool.getCachePool;
import static eu.rtsketo.sakurastats.control.ViewDecor.bounce;
import static eu.rtsketo.sakurastats.control.ViewDecor.decorate;
import static eu.rtsketo.sakurastats.hashmaps.SDPMap.sdp2px;

public class DialogView {
    public enum SakuraDialog {INPUT, INFO, CLANQUEST, RATEQUEST}
    public DialogView(SakuraDialog type, Interface acti) { this(type, null, -1, acti); }
    public DialogView(SakuraDialog type, int count, Interface acti) { this(type, null, count, acti); }
    public DialogView(SakuraDialog type, final Dialog prevDia, Interface acti) { this(type, prevDia, -1, acti); }
    public DialogView(SakuraDialog type, final Dialog prevDia, final int count, Interface acti) {
        final View inputView;
        if (type == SakuraDialog.INPUT)
            inputView = LayoutInflater.from(acti)
                    .inflate(R.layout.dialog_input, null);
        else inputView = LayoutInflater.from(acti)
                .inflate(R.layout.dialog_quest, null);

        final Dialog dialog = new Dialog(acti);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawableResource(
                android.R.color.transparent);
        dialog.setContentView(inputView);
        dialog.setCancelable(false);

        final MagicTextView explain = dialog.findViewById(R.id.inputExplaination);
        final MagicTextView explain2 = dialog.findViewById(R.id.inputExplaination2);
        final ImageView confirm = dialog.findViewById(R.id.inputConfirmButton);
        final ImageView cancel = dialog.findViewById(R.id.inputCancelButton);
        final EditText input = dialog.findViewById(R.id.inputClanTag);

        int[] size = { sdp2px(9), sdp2px(8) };
        switch (type) {
            case INPUT:
                final ImageView loadingAnim = dialog.findViewById(R.id.loadingAnim);
                input.addTextChangedListener(new InputCheck(input, acti));
                if (count > 0) decorate(explain,"Enter a", size[0]);
                else decorate(explain, "Enter your", size[0]);
                decorate(explain2,"clan's tag!", size[0]);
                decorate(input, size[0]);

                confirm.setOnClickListener(view -> {
                    bounce(view, acti);
                    loadingAnim.setVisibility(View.VISIBLE);
                    String tag = input.getText().toString();
                    tag = tag.toUpperCase()
                            .replace("O", "0")
                            .replaceAll("[^0289CGJLPQRUVY]", "");
                    cancel.setEnabled(false);
                    confirm.setEnabled(false);
                    cancel.setColorFilter(Color
                            .argb(100,200,200,200));
                    confirm.setColorFilter(Color
                            .argb(100,200,200,200));
                    ((AnimationDrawable)loadingAnim.getDrawable()).start();
                    final String finalTag = tag;
                    getCachePool().execute(() -> {
                        DataFetch df = new DataFetch(acti);
                        if (df.checkClan(finalTag)) {
                            if(count > -1) {
                                acti.setStoredClan(count, finalTag);
                                acti.getSettiFrag().selectClan(count);
                                acti.getSettiFrag().refreshStored(
                                        count,false);
                            } else {
                                acti.setLastClan(finalTag);
                                acti.setStoredClan(0, finalTag);
                                Service.getThread(acti)
                                        .start(finalTag, false, true);
                            }
                            dialog.cancel();
                        } else acti.runOnUiThread(() -> {
                            ((AnimationDrawable)loadingAnim
                                    .getDrawable()).stop();
                            new DialogView(SakuraDialog.INFO, acti);
                            loadingAnim.setVisibility(View.INVISIBLE);
                            confirm.setColorFilter(null);
                            cancel.setColorFilter(null);
                            confirm.setEnabled(true);
                            cancel.setEnabled(true); }); }); });

                cancel.setOnClickListener(view -> {
                    bounce(view, acti);
                    if (count > -1) dialog.cancel();
                    else new DialogView(SakuraDialog.CLANQUEST, dialog, acti); });
                break;

            case CLANQUEST:
                decorate(explain, "Are you sure?", size[0]);
                decorate(explain2, "A random clan" +
                        "\nwill be chosen.", size[1]);

                cancel.setOnClickListener(view -> {
                    bounce(view, acti);
                    dialog.cancel(); });

                confirm.setOnClickListener(view -> {
                    Service.getThread(acti)
                            .start(null, false, true);
                    bounce(view, acti);
                    dialog.cancel();
                    prevDia.cancel(); });
                break;

            case RATEQUEST:
                decorate(explain, "This appears only once!", size[0]);
                decorate(explain2, "\nSince you are using this" +
                        "\napp for sometime now," +
                        "\nplease consider rating it." +
                        "\n\n\nYour feedback is important!", size[1]);

                cancel.setOnClickListener(view -> {
                    bounce(view, acti);
                    dialog.cancel(); });

                confirm.setOnClickListener(view -> {
                    bounce(view, acti);
                    Uri uri = Uri.parse("market://details?id=" + acti.getPackageName());
                    Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                            Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    try {
                        startActivity(acti, goToMarket, null);
                    } catch (ActivityNotFoundException e) {
                        startActivity(acti, new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://play.google.com/store/apps/details?id=" + acti.getPackageName())), null);
                    }
                    dialog.cancel(); });
                break;

            case INFO:
                dialog.setCancelable(true);
                cancel.setVisibility(View.GONE);
                decorate(explain, "Wrong tag!", size[0]);
                decorate(explain2, "Tag doesn't exist" +
                        "\nor bad connection.", size[1]);

                confirm.setOnClickListener(view -> {
                    bounce(view, acti);
                    dialog.cancel(); });
        }
        dialog.show();
    }


    class InputCheck implements TextWatcher {
        private String clanTagInput = "";
        private EditText editText;
        private Activity activity;

        InputCheck(EditText et, Activity acti) {
            editText = et; activity = acti; }

        @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
        @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override public void afterTextChanged(Editable input) {
            String tag = input.toString();
            if (!clanTagInput.equals(tag)) {
                tag = "#"+tag.toUpperCase()
                        .replace("O", "0")
                        .replaceAll("[^0289CGJLPQRUVY]", "");
                clanTagInput = tag;
                final String finalTag = tag;
                activity.runOnUiThread(() -> {
                    editText.setText(finalTag);
                    editText.setSelection(finalTag.length()); });
            }
        }
    }
}
