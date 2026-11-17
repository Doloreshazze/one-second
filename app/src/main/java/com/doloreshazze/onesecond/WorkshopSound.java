package com.doloreshazze.onesecond;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

final class WorkshopSound {
    private final SoundPool pool;
    private final int[] notes;

    WorkshopSound(Context context) {
        pool = new SoundPool.Builder().setMaxStreams(3)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .build();
        notes = new int[] {
                pool.load(context, R.raw.note_c, 1),
                pool.load(context, R.raw.note_e, 1),
                pool.load(context, R.raw.note_g, 1)
        };
    }

    void play(int note) {
        pool.play(notes[note], .7f, .7f, 1, 0, 1);
    }

    void release() {
        pool.release();
    }
}
