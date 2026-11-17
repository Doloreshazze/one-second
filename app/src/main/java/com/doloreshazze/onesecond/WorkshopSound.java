package com.doloreshazze.onesecond;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

final class WorkshopSound {
    private final SoundPool pool;
    private final int[] notes;
    private final int fanfare;

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
        fanfare = pool.load(context, R.raw.fair_fanfare, 1);
    }

    void play(int note) {
        pool.play(notes[note], .7f, .7f, 1, 0, 1);
    }

    void playEnding(PuzzleRound.Outcome outcome) {
        switch (outcome) {
            case BAND_PLAYED:
            case FAIR_OPEN:
                pool.play(fanfare, .68f, .68f, 1, 0, 1);
                break;
            case ONE_TRUMPET:
                pool.play(notes[0], .6f, .6f, 1, 0, .68f);
                break;
            default:
                break;
        }
    }

    void release() {
        pool.release();
    }
}
