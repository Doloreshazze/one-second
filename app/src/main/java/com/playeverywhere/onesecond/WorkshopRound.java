package com.playeverywhere.onesecond;

import static com.playeverywhere.onesecond.PuzzleRound.*;

final class WorkshopRound {
    enum Action { SAUCER, TRAY, PLATE, FAN, BRIDGE, BELL, ROCKER }

    static final float DROP_X = 273, DROP_PAPER_Y = 474, DROP_SAUCER_Y = 416;
    static final float SAUCER_X = 184, SAUCER_Y = 360, TRAY_X = 199, TRAY_Y = 467;
    static final float PLATE_X = 149, PLATE_Y = 448, LEG_X = 207, LEG_BOTTOM = 366;
    static final float FAN_X = 174, FAN_Y = 358, CUP_X = 279, CUP_Y = 442;
    static final float BRIDGE_X = 226, RAIL_Y = 467, WEDGE_X = 231, WEDGE_Y = 418;
    static final float[] NOTE_X = {139, 225, 309}, NOTE_Y = {302, 320, 350};
    static final float PHOTO_BELL_X = 264, PHOTO_BELL_Y = 337, SHUTTER_TIME = .83f;
    static final float ROCKER_X = 209, ROCKER_Y = 417, WEIGHT_X = 248;

    private final int[] notes = new int[3];
    private final float[] noteHitTimes = {-1, -1, -1};
    private int noteCount;
    private Action action;
    private float actionTime;

    void update(PuzzleRound round) {
        switch (round.level) {
            case DROP: updateDrop(round); break;
            case KEY: updateKey(round); break;
            case GLIDER: updateGlider(round); break;
            case TRAIN: updateTrain(round); break;
            case NOTES: updateNotes(round); break;
            case PHOTO: updatePhoto(round); break;
            case CLOCK: updateClock(round); break;
            default: throw new AssertionError(round.level);
        }
    }

    boolean has(Action expected) {
        return action == expected;
    }

    Action actionSince(float previousTime) {
        return actionTime > previousTime ? action : null;
    }

    float actionAge(float time) {
        return action == null ? 0 : time - actionTime;
    }

    float progress(float time, float duration) {
        return action == null ? 0 : smooth((time - actionTime) / duration);
    }

    int noteCount() {
        return noteCount;
    }

    int noteAt(int index) {
        return notes[index];
    }

    float noteAge(int index, float time) {
        return noteHitTimes[index] < 0 ? -1 : time - noteHitTimes[index];
    }

    float effectDuration(Level level) {
        switch (level) {
            case CLOCK: return 1.06f;
            case PHOTO: return .84f;
            case DROP:
            case TRAIN:
            case NOTES: return .68f;
            default: return .52f;
        }
    }

    private void trigger(Action next, float time) {
        action = next;
        actionTime = time;
    }

    private void updateDrop(PuzzleRound round) {
        if (action == null) {
            if (round.touches(SAUCER_X, SAUCER_Y, 42, 33)) {
                trigger(Action.SAUCER, round.time);
            } else if (round.touches(TRAY_X, TRAY_Y, 47, 33)) {
                trigger(Action.TRAY, round.time);
            }
        }
        if (has(Action.SAUCER) && progress(round.time, .26f) > .98f && dropY(round.time) >= DROP_SAUCER_Y) {
            round.finish(Outcome.DROP_CAUGHT, DROP_X, DROP_SAUCER_Y);
        } else if (dropY(round.time) >= DROP_PAPER_Y) {
            boolean safe = has(Action.TRAY) && progress(round.time, .28f) > .95f;
            round.finish(safe ? Outcome.PAPER_SAVED : Outcome.INK_BLOT, DROP_X, DROP_PAPER_Y);
        }
    }

    float dropY(float time) {
        return 122 + 329 * time * time;
    }

    private void updateKey(PuzzleRound round) {
        if (round.marbleX >= LEG_X - 20 && round.marbleX < LEG_X + 5
                && round.marbleY < LEG_BOTTOM && round.horizontalSpeed() > 0) {
            round.reflect(1, 0);
        }
        if (action == null && round.verticalSpeed() > 0
                && round.touches(PLATE_X, PLATE_Y, 39, 24)) {
            trigger(Action.PLATE, round.time);
            round.reflect(-.259f, .966f);
        }
        if (round.touches(keyX(round.time), keyY(round.time), 43, 37)) {
            round.finish(Outcome.KEY_SAVED, keyX(round.time), keyY(round.time));
        } else if (keyY(round.time) >= 526) {
            round.finish(Outcome.KEY_LOST, keyX(round.time), 526);
        }
    }

    float keyX(float time) {
        return lerp(301, 325, clamp(time));
    }

    float keyY(float time) {
        return Math.min(526, 248 + 32 * time + 242 * time * time);
    }

    private void updateGlider(PuzzleRound round) {
        if (action == null && round.touches(FAN_X, FAN_Y, 42, 35)) {
            trigger(Action.FAN, round.time);
        }
        if (round.time >= 1.01f) {
            boolean safe = guidesPlane();
            round.finish(safe ? Outcome.GLIDER_LANDED : Outcome.GLIDER_WET,
                    safe ? 335 : CUP_X, safe ? 265 : CUP_Y - 25);
        }
    }

    boolean guidesPlane() {
        return has(Action.FAN) && actionTime <= .58f;
    }

    float gliderX(float time) {
        if (!guidesPlane()) return Math.min(CUP_X, 108 + 173 * time);
        return lerp(108 + 173 * actionTime, 335, progress(time, .68f));
    }

    float gliderY(float time) {
        if (!guidesPlane()) return Math.min(CUP_Y - 25, glideDown(time));
        float fraction = progress(time, .64f);
        return lerp(glideDown(actionTime), 265, fraction)
                - (float) Math.sin(fraction * Math.PI) * 60;
    }

    private float glideDown(float time) {
        return 194 + 64 * time + 157 * time * time;
    }

    private void updateTrain(PuzzleRound round) {
        if (action == null && round.touches(WEDGE_X, WEDGE_Y, 41, 31)) {
            trigger(Action.BRIDGE, round.time);
        }
        if (round.time >= .94f) {
            boolean safe = has(Action.BRIDGE) && progress(round.time, .28f) > .98f;
            round.finish(safe ? Outcome.TRAIN_CROSSED : Outcome.TRAIN_DERAILED, BRIDGE_X, RAIL_Y);
        }
    }

    private void updateNotes(PuzzleRound round) {
        for (int i = 0; i < NOTE_X.length; i++) {
            if (noteHitTimes[i] < 0 && round.touches(NOTE_X[i], NOTE_Y[i], 29, 27)) {
                noteHitTimes[i] = round.time;
                notes[noteCount++] = i;
            }
        }
        if (noteCount == 3 && notes[0] == 0 && notes[1] == 1 && notes[2] == 2) {
            round.finish(Outcome.NOTES_PLAYED, 265, 460);
        } else if (round.time >= 1.01f) {
            round.finish(Outcome.NOTES_MISSED, 265, 460);
        }
    }

    private void updatePhoto(PuzzleRound round) {
        if (action == null && round.touches(PHOTO_BELL_X, PHOTO_BELL_Y, 40, 38)) {
            trigger(Action.BELL, round.time);
        }
        if (round.time >= SHUTTER_TIME) {
            float attention = actionAge(round.time);
            boolean facingCamera = has(Action.BELL) && attention >= .16f && attention <= .50f;
            round.finish(facingCamera ? Outcome.PHOTO_TAKEN : Outcome.PHOTO_MISSED, 126, 424);
        }
    }

    private void updateClock(PuzzleRound round) {
        if (action == null && round.touches(ROCKER_X, ROCKER_Y, 47, 33)) {
            trigger(Action.ROCKER, round.time);
        }
        if (round.time >= 1.04f) {
            boolean safe = has(Action.ROCKER) && progress(round.time, .34f) > .97f;
            round.finish(safe ? Outcome.CLOCK_STARTED : Outcome.CLOCK_JAMMED, WEIGHT_X, 441);
        }
    }

    static float smooth(float value) {
        float p = clamp(value);
        return p * p * (3 - 2 * p);
    }
}
