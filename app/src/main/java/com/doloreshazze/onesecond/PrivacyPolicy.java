package com.doloreshazze.onesecond;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.ScrollView;
import android.widget.TextView;

/** The policy is available offline; opening the public copy is optional. */
final class PrivacyPolicy {
    static final String URL = "https://github.com/Doloreshazze/one-second/blob/main/docs/privacy.md";

    private PrivacyPolicy() {}

    static void show(MainActivity activity) {
        TextView text = new TextView(activity);
        int padding = Math.round(20 * activity.getResources().getDisplayMetrics().density);
        text.setPadding(padding, padding, padding, padding);
        text.setText(R.string.policy_body);
        text.setTextSize(16);
        text.setTextIsSelectable(true);
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(text);
        activity.setModalVisible(true);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.policy_title)
                .setView(scroll)
                .setPositiveButton(R.string.audio_done, null)
                .setNeutralButton(R.string.policy_online, (dialog, which) -> {
                    try {
                        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL)));
                    } catch (ActivityNotFoundException ignored) {
                        // The complete policy remains available in this offline dialog.
                    }
                })
                .setOnDismissListener(dialog -> activity.setModalVisible(false))
                .show();
    }
}
