package com.playeverywhere.onesecond;

import android.app.AlertDialog;
import android.app.LocaleManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;
import java.util.Locale;

final class GameLocale {
    static final String[] TAGS = {"", "en", "ru", "de", "fr", "es", "pt", "it", "pl", "tr", "ar", "hi", "zh-CN"};
    static final String[] NAMES = {"", "English", "Русский", "Deutsch", "Français", "Español", "Português", "Italiano", "Polski", "Türkçe", "العربية", "हिन्दी", "简体中文"};

    static Context wrap(Context base) {
        if (Build.VERSION.SDK_INT >= 33) return base;
        String tag = base.getSharedPreferences("workshop_locale", Context.MODE_PRIVATE).getString("language", "");
        if (tag.isEmpty()) return base;
        Configuration config = new Configuration(base.getResources().getConfiguration());
        Locale locale = Locale.forLanguageTag(tag);
        config.setLocale(locale);
        config.setLayoutDirection(locale);
        return base.createConfigurationContext(config);
    }

    static void choose(MainActivity activity) {
        String[] names = NAMES.clone();
        names[0] = activity.getString(R.string.language_system);
        activity.setModalVisible(true);
        new AlertDialog.Builder(activity).setTitle(R.string.language)
                .setItems(names, (dialog, index) -> {
                    String tag = TAGS[index];
                    if (Build.VERSION.SDK_INT >= 33) {
                        activity.getSystemService(LocaleManager.class).setApplicationLocales(LocaleList.forLanguageTags(tag));
                    } else {
                        activity.getSharedPreferences("workshop_locale", Context.MODE_PRIVATE).edit().putString("language", tag).apply();
                        activity.recreate();
                    }
                }).setOnDismissListener(dialog -> activity.setModalVisible(false)).show();
    }
}
