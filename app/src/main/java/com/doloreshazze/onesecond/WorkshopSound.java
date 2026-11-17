package com.doloreshazze.onesecond;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseBooleanArray;

import static com.doloreshazze.onesecond.PuzzleRound.Level;
import static com.doloreshazze.onesecond.PuzzleRound.Outcome;

final class WorkshopSound {
    private static final String MUSIC_ENABLED = "music";
    private static final String EFFECTS_ENABLED = "effects";
    private static final float MUSIC_VOLUME = .18f;
    private static final float SHOT_MUSIC_VOLUME = .085f;
    private static final AudioAttributes MUSIC_ATTRIBUTES = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build();

    private enum Cue {
        LAUNCH(R.raw.sfx_launch, .29f),
        GLASS(R.raw.sfx_glass, .32f),
        WOOD(R.raw.sfx_wood, .32f),
        FELT(R.raw.sfx_felt, .35f),
        METAL(R.raw.sfx_metal, .29f),
        WATER(R.raw.sfx_water, .38f),
        PAPER(R.raw.sfx_paper, .34f),
        ECHO(R.raw.sfx_echo, .30f),
        BELL(R.raw.sfx_bell, .32f),
        SHUTTER(R.raw.sfx_shutter, .30f),
        SUCCESS(R.raw.sfx_success, .31f),
        NOTE_C(R.raw.note_c, .58f),
        NOTE_E(R.raw.note_e, .58f),
        NOTE_G(R.raw.note_g, .58f),
        FANFARE(R.raw.fair_fanfare, .34f);

        final int resource;
        final float volume;

        Cue(int resource, float volume) {
            this.resource = resource;
            this.volume = volume;
        }
    }

    private static final Cue[] NOTES = {Cue.NOTE_C, Cue.NOTE_E, Cue.NOTE_G};
    private final SharedPreferences settings;
    private final AudioManager audioManager;
    private final AudioManager.OnAudioFocusChangeListener focusListener = this::onFocusChanged;
    private final AudioFocusRequest focusRequest;
    private final SoundPool pool;
    private final int[] samples = new int[Cue.values().length];
    private final int[] streams = new int[samples.length];
    private final SparseBooleanArray ready = new SparseBooleanArray();

    private MediaPlayer music;
    private boolean inForeground, focusHeld, focusGranted, musicPlaying;
    private boolean musicEnabled, effectsEnabled, shotActive;
    private Level level;

    WorkshopSound(Context context) {
        settings = context.getSharedPreferences("workshop_audio", Context.MODE_PRIVATE);
        musicEnabled = settings.getBoolean(MUSIC_ENABLED, true);
        effectsEnabled = settings.getBoolean(EFFECTS_ENABLED, true);
        audioManager = context.getSystemService(AudioManager.class);
        focusRequest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(MUSIC_ATTRIBUTES)
                        .setWillPauseWhenDucked(true)
                        .setOnAudioFocusChangeListener(focusListener, new Handler(Looper.getMainLooper()))
                        .build()
                : null;
        music = MediaPlayer.create(context, R.raw.workshop_loop, MUSIC_ATTRIBUTES,
                AudioManager.AUDIO_SESSION_ID_GENERATE);
        if (music != null) {
            music.setLooping(true);
            music.setOnErrorListener((player, what, extra) -> {
                player.release();
                music = null;
                musicPlaying = false;
                abandonFocus();
                return true;
            });
        }

        pool = new SoundPool.Builder().setMaxStreams(4)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .build();
        pool.setOnLoadCompleteListener((soundPool, sample, status) -> {
            if (status == 0) ready.put(sample, true);
        });
        for (Cue cue : Cue.values()) samples[cue.ordinal()] = pool.load(context, cue.resource, 1);
    }

    boolean musicEnabled() { return musicEnabled; }
    boolean effectsEnabled() { return effectsEnabled; }

    void setMusicEnabled(boolean enabled) {
        musicEnabled = enabled;
        settings.edit().putBoolean(MUSIC_ENABLED, enabled).apply();
        refreshPlayback();
    }

    void setEffectsEnabled(boolean enabled) {
        effectsEnabled = enabled;
        settings.edit().putBoolean(EFFECTS_ENABLED, enabled).apply();
        if (!enabled) stopEffects();
    }

    void setLevel(Level level) {
        this.level = level;
        shotActive = false;
        refreshPlayback();
    }

    void launch() {
        shotActive = true;
        updateMusicVolume();
        play(Cue.LAUNCH);
    }

    void finishShot() {
        shotActive = false;
        updateMusicVolume();
    }

    void resume() {
        inForeground = true;
        refreshPlayback();
    }

    void pause() {
        inForeground = false;
        stopEffects();
        refreshPlayback();
    }

    void release() {
        pause();
        if (music != null) {
            music.setOnErrorListener(null);
            music.release();
            music = null;
        }
        pool.setOnLoadCompleteListener(null);
        pool.release();
    }

    private boolean wantsMusic() {
        return inForeground && musicEnabled && music != null && level != Level.NOTES;
    }

    @SuppressWarnings("deprecation")
    private void refreshPlayback() {
        if (wantsMusic() && !focusHeld) {
            int result = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? audioManager.requestAudioFocus(focusRequest)
                    : audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC,
                            AudioManager.AUDIOFOCUS_GAIN);
            focusHeld = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            focusGranted = focusHeld;
        }
        updatePlayback();
        if (!wantsMusic()) abandonFocus();
    }

    @SuppressWarnings("deprecation")
    private void abandonFocus() {
        if (!focusHeld) return;
        focusHeld = false;
        focusGranted = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) audioManager.abandonAudioFocusRequest(focusRequest);
        else audioManager.abandonAudioFocus(focusListener);
    }

    private void onFocusChanged(int change) {
        if (!focusHeld) return;
        focusGranted = change == AudioManager.AUDIOFOCUS_GAIN;
        if (!focusGranted) stopEffects();
        updatePlayback();
        if (change == AudioManager.AUDIOFOCUS_LOSS) abandonFocus();
    }

    private void updatePlayback() {
        if (music == null) return;
        boolean shouldPlay = wantsMusic() && focusGranted;
        if (shouldPlay == musicPlaying) return;
        updateMusicVolume();
        if (shouldPlay) music.start();
        else music.pause();
        musicPlaying = shouldPlay;
    }

    private void updateMusicVolume() {
        if (music == null) return;
        float volume = shotActive ? SHOT_MUSIC_VOLUME : MUSIC_VOLUME;
        music.setVolume(volume, volume);
    }

    private void stopEffects() {
        for (int i = 0; i < streams.length; i++) {
            if (streams[i] == 0) continue;
            pool.stop(streams[i]);
            streams[i] = 0;
        }
    }

    private void play(Cue cue) {
        play(cue, cue.volume, 1f);
    }

    private void play(Cue cue, float volume, float rate) {
        if (!inForeground || !effectsEnabled || (wantsMusic() && !focusGranted)) return;
        int index = cue.ordinal();
        if (!ready.get(samples[index])) return;
        if (streams[index] != 0) pool.stop(streams[index]);
        streams[index] = pool.play(samples[index], volume, volume, 1, 0, rate);
    }

    void playNote(int note) {
        play(NOTES[note]);
    }

    void playEvents(PuzzleRound round, float previousTime, Outcome before) {
        if (round.cushionHitSince(previousTime)) play(Cue.FELT);
        if (round.workshop != null) {
            WorkshopRound.Action action = round.workshop.actionSince(previousTime);
            if (action != null) playWorkshopAction(action);
        }
        if (round.fair != null) {
            FairRound.Action action = round.fair.actionSince(previousTime);
            if (action != null) playFairAction(action);
            if (round.level == Level.ECHO && previousTime < FairRound.ECHO_DELAY
                    && round.fair.auxiliaryVisible()) play(Cue.ECHO);
        }
        if (before == null && round.event != null) playEnding(round.event);
    }

    private void playWorkshopAction(WorkshopRound.Action action) {
        switch (action) {
            case BELL: play(Cue.BELL); break;
            case PLATE: play(Cue.METAL); break;
            case SAUCER: play(Cue.GLASS); break;
            case FAN: play(Cue.PAPER); break;
            case TRAY:
            case BRIDGE:
            case ROCKER: play(Cue.WOOD); break;
        }
    }

    private void playFairAction(FairRound.Action action) {
        switch (action) {
            case WATER_IMPACT: play(Cue.WATER); break;
            case BALL_COLLISION: play(Cue.GLASS); break;
            case FELT_RAIL:
            case MOUSE_CUSHION: play(Cue.FELT); break;
            case SPOOL_RELEASE: play(Cue.PAPER); break;
            case MAGNET_RELEASE:
            case REMOVE_WEIGHT:
            case ADD_WEIGHT:
            case ECHO_GATE: play(Cue.METAL); break;
            case WATER_GATE:
            case SHORT_RAIL:
            case DRAGON_MIRROR:
            case HARE_MIRROR:
            case MOUSE_CHASE: play(Cue.WOOD); break;
        }
    }

    private void playEnding(Outcome outcome) {
        switch (outcome) {
            case BAND_PLAYED:
            case FAIR_OPEN: play(Cue.FANFARE); return;
            case ONE_TRUMPET: play(Cue.NOTE_C, .28f, .68f); return;
            case NOTES_PLAYED:
            case NOTES_MISSED: return;
            case CAT_AWAKE: play(Cue.BELL); return;
            case VASE_BROKEN: play(Cue.GLASS, .36f, .73f); return;
            case FISH_SAVED: play(Cue.GLASS); break;
            case DROP_CAUGHT:
            case FLAG_RAISED:
            case FISH_FELL:
            case INK_BLOT:
            case GLIDER_WET:
            case BOOT_FILLED: play(Cue.WATER); break;
            case PHOTO_TAKEN:
            case PHOTO_MISSED: play(Cue.SHUTTER); break;
            case TOKEN_CAUGHT:
            case CLIPS_CAUGHT: play(Cue.METAL); break;
            default: break;
        }
        if (outcome.won) play(Cue.SUCCESS);
    }
}
