package com.doloreshazze.onesecond;

import android.app.Activity;
import android.graphics.Insets;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;

public class MainActivity extends Activity {
    private WorkshopSound sound;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        sound = new WorkshopSound(getApplicationContext());
        GameView game = new GameView(this, sound);
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
        sound.resume();
    }

    @Override
    protected void onPause() {
        sound.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        sound.release();
        super.onDestroy();
    }
}
