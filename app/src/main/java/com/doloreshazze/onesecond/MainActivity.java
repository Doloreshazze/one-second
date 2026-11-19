package com.doloreshazze.onesecond;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;

public class MainActivity extends Activity {
    private WorkshopSound sound;
    private GameView game;
    private boolean resumed, modalVisible;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(GameLocale.wrap(base));
    }

    @Override
    public void onCreate(Bundle state) {
        if (BuildConfig.DEBUG && getIntent().hasExtra("smoke_locale")) {
            Configuration config = new Configuration();
            config.setLocale(java.util.Locale.forLanguageTag(getIntent().getStringExtra("smoke_locale")));
            applyOverrideConfiguration(config);
        }
        super.onCreate(state);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        sound = new WorkshopSound(getApplicationContext());
        game = new GameView(this, sound);
        if (Build.VERSION.SDK_INT >= 35) {
            game.setOnApplyWindowInsetsListener((view, insets) -> {
                Insets safe = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout());
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
                return insets;
            });
        }
        setContentView(game);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        updatePlayback();
    }

    @Override
    protected void onPause() {
        resumed = false;
        updatePlayback();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (sound != null) sound.release();
        super.onDestroy();
    }

    void setModalVisible(boolean visible) {
        modalVisible = visible;
        updatePlayback();
    }

    private void updatePlayback() {
        boolean playing = resumed && !modalVisible;
        if (game != null) game.setPaused(!playing);
        if (sound != null) {
            if (playing) sound.resume(); else sound.pause();
        }
    }
}
