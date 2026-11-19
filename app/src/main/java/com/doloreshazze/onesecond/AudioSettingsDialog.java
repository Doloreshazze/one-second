package com.doloreshazze.onesecond;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ScrollView;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

final class AudioSettingsDialog extends Dialog {
    private static final Typeface TITLE = Typeface.create("sans-serif-black", Typeface.NORMAL);
    private static final Typeface LABEL = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private static final int PAPER = 0xffffeed9;
    private static final int INK = 0xff2b211d;
    private final float density;

    AudioSettingsDialog(Context context, WorkshopSound sound, Runnable onChanged) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setTitle(R.string.audio_settings);

        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(22), dp(23), dp(22), dp(20));
        panel.setBackground(panelFrame());

        TextView title = new TextView(context);
        title.setText(R.string.audio_settings);
        title.setTextColor(PAPER);
        title.setTypeface(TITLE);
        title.setTextSize(23);
        title.setGravity(Gravity.CENTER);
        if (Build.VERSION.SDK_INT >= 28) title.setAccessibilityHeading(true);
        panel.addView(title, row(0));

        panel.addView(toggle(R.string.audio_music, sound.musicEnabled(), (view, enabled) -> {
            sound.setMusicEnabled(enabled);
            onChanged.run();
        }), row(18));
        panel.addView(toggle(R.string.audio_effects, sound.effectsEnabled(), (view, enabled) -> {
            sound.setEffectsEnabled(enabled);
            onChanged.run();
        }), row(8));

        MainActivity activity = (MainActivity) context;
        panel.addView(option(R.string.language, () -> GameLocale.choose(activity)), row(8));
        panel.addView(option(R.string.privacy_policy, () -> PrivacyPolicy.show(activity)), row(8));

        Button done = new Button(context);
        done.setText(R.string.audio_done);
        done.setAllCaps(false);
        done.setTextColor(INK);
        done.setTypeface(LABEL);
        done.setTextSize(17);
        done.setMinHeight(dp(48));
        done.setPadding(dp(12), dp(8), dp(12), dp(8));
        done.setSoundEffectsEnabled(false);
        done.setBackgroundTintList(null);
        done.setBackground(pressable(surface(StageArt.GOLD, 0xffdfaa68, 0xfff9d6a1, 14)));
        done.setOnClickListener(view -> dismiss());
        panel.addView(done, row(17));

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.addView(panel);
        setContentView(scroll);
        setCanceledOnTouchOutside(true);
        Window window = getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(.62f);
    }

    @Override
    public void show() {
        super.show();
        int available = getContext().getResources().getDisplayMetrics().widthPixels;
        int height = getContext().getResources().getDisplayMetrics().heightPixels;
        getWindow().setLayout(Math.min(dp(360), available - dp(32)),
                Math.min(dp(640), height - dp(80)));
    }

    private Button option(int text, Runnable action) {
        Button button = new Button(getContext());
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(PAPER);
        button.setTypeface(LABEL);
        button.setTextSize(15);
        button.setMinHeight(dp(48));
        button.setBackgroundTintList(null);
        button.setBackground(pressable(surface(0xff34343d, 0xff2b2a32, 0xff51483f, 13)));
        button.setOnClickListener(view -> {
            dismiss();
            getWindow().getDecorView().post(action);
        });
        return button;
    }

    private Switch toggle(int label, boolean enabled, CompoundButton.OnCheckedChangeListener onChange) {
        Switch control = new Switch(getContext());
        control.setText(label);
        control.setTextColor(PAPER);
        control.setTypeface(LABEL);
        control.setTextSize(17);
        control.setMinHeight(dp(54));
        control.setPaddingRelative(dp(14), dp(8), dp(14), dp(8));
        control.setGravity(Gravity.CENTER_VERTICAL);
        control.setSoundEffectsEnabled(false);
        control.setBackgroundTintList(null);
        control.setBackground(pressable(surface(0xff34343d, 0xff2b2a32, 0xff51483f, 13)));
        control.setShowText(false);
        control.setSplitTrack(false);
        control.setSwitchMinWidth(dp(52));
        control.setSwitchPadding(dp(12));
        control.setTrackTintList(null);
        control.setThumbTintList(null);
        control.setTrackDrawable(byCheckState(
                track(0xff37635c, 0xff7bbba9), track(0xff1c2028, 0xff756657)));
        control.setThumbDrawable(byCheckState(thumb(StageArt.ICE), thumb(0xffb4a39a)));
        control.setChecked(enabled);
        control.setOnCheckedChangeListener(onChange);
        return control;
    }

    private Drawable panelFrame() {
        LayerDrawable frame = new LayerDrawable(new Drawable[] {
                surface(0xff806043, 0xff392b29, 0xffa8845d, 24),
                surface(0xff32313b, 0xff22242e, 0xffbd986c, 19)
        });
        frame.setLayerInset(1, dp(4), dp(4), dp(4), dp(4));
        return frame;
    }

    private GradientDrawable surface(int top, int bottom, int border, int radius) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {top, bottom});
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(dp(1), border);
        return drawable;
    }

    private Drawable track(int fill, int border) {
        GradientDrawable track = surface(fill, fill, border, 14);
        track.setSize(dp(52), dp(28));
        return track;
    }

    private Drawable thumb(int fill) {
        GradientDrawable thumb = new GradientDrawable();
        thumb.setShape(GradientDrawable.OVAL);
        thumb.setColor(fill);
        thumb.setSize(dp(18), dp(18));
        return new InsetDrawable(thumb, dp(5));
    }

    private static Drawable byCheckState(Drawable checked, Drawable unchecked) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_checked}, checked);
        states.addState(new int[0], unchecked);
        return states;
    }

    private static RippleDrawable pressable(Drawable surface) {
        return new RippleDrawable(ColorStateList.valueOf(0x33ffe0b4), surface, null);
    }

    private LinearLayout.LayoutParams row(int gap) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.topMargin = dp(gap);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * density);
    }
}
