package com.doloreshazze.onesecond;

final class PuzzleRound {
    enum Level { VASE, CAT }
    enum Outcome { VASE_SAVED, VASE_BROKEN, FISH_SAVED, CAT_AWAKE, FISH_FELL }

    static final float WIDTH = 400;
    static final float HEIGHT = 600;
    static final float FLOOR = 537;
    static final float MARBLE_RADIUS = 20;
    static final float VASE_X = 318;
    static final float CUSHION_START_X = 208;
    static final float CUSHION_Y = 513;
    static final float BELL_X = 206;
    static final float BELL_Y = 430;
    static final float BOWL_START_X = 315;
    static final float BOWL_REST_X = 354;
    static final float BOWL_REST_Y = 365;
    static final float EFFECT_DURATION = .35f;

    private static final float GRAVITY = 480;
    private static final float SPEED = 500;
    private static final float CUSHION_TOP = 491;
    private static final float VASE_HALF_HEIGHT = 39;
    private static final float BOWL_HALF_HEIGHT = 39;
    private static final float SIM_STEP = 1f / 240;

    final Level level;
    boolean started;
    float time, marbleX, marbleY, eventX, eventY;
    Outcome event;
    private float velocityX, velocityY;
    private float cushionHitAt = -1;
    private float eventAt;
    private boolean resting;

    PuzzleRound(Level level) {
        this.level = level;
        marbleX = 89;
        marbleY = level == Level.VASE ? 290 : 423;
    }

    void launch(float dx, float dy) {
        float scale = SPEED / (float) Math.hypot(dx, dy);
        velocityX = dx * scale;
        velocityY = dy * scale;
        started = true;
    }

    int preview(float dx, float dy, float[] points) {
        PuzzleRound shot = new PuzzleRound(level);
        shot.launch(dx, dy);
        int count = 0;
        for (int i = 0; i < points.length && !shot.resting && shot.event == null
                && shot.cushionHitAt < 0; i += 2) {
            shot.advance(.04f);
            points[i] = shot.marbleX;
            points[i + 1] = shot.marbleY;
            count++;
        }
        return count;
    }

    boolean active() {
        return started && (event == null || effectAge() < EFFECT_DURATION);
    }

    boolean won() {
        return event == Outcome.VASE_SAVED || event == Outcome.FISH_SAVED;
    }

    float effectAge() {
        return time - eventAt;
    }

    float motionTime() {
        return event == null ? time : eventAt;
    }

    void advance(float seconds) {
        while (seconds > 0 && active()) {
            float dt = Math.min(seconds, SIM_STEP);
            time += dt;
            moveMarble(dt);
            if (event == null) {
                if (level == Level.VASE) updateVase();
                else updateBowl();
            }
            seconds -= dt;
        }
    }

    float cushionX() {
        if (cushionHitAt < 0) return CUSHION_START_X;
        return lerp(CUSHION_START_X, VASE_X, easeOut((time - cushionHitAt) / .29f));
    }

    float vaseY() {
        if (event == Outcome.VASE_SAVED) {
            float bounce = (float) Math.sin(easeOut(effectAge() / .23f) * Math.PI);
            return CUSHION_TOP - VASE_HALF_HEIGHT - 10 * bounce;
        }
        return Math.min(FLOOR - VASE_HALF_HEIGHT, 247 + 25 * time + 175 * time * time);
    }

    float vaseAngle() {
        if (event == Outcome.VASE_SAVED) {
            return lerp(24, 0, easeOut(effectAge() / EFFECT_DURATION));
        }
        return -16 + time * 39;
    }

    float bowlX() {
        if (event == Outcome.FISH_SAVED) {
            return lerp(eventX, BOWL_REST_X, easeOut(effectAge() / .28f));
        }
        return BOWL_START_X - 5 * time;
    }

    float bowlY() {
        if (event == Outcome.FISH_SAVED) {
            float progress = clamp(effectAge() / .28f);
            float hop = (float) Math.sin(progress * Math.PI) * 21;
            return lerp(eventY, BOWL_REST_Y, easeOut(progress)) - hop;
        }
        return Math.min(FLOOR - BOWL_HALF_HEIGHT, 326 + 28 * time + 136 * time * time);
    }

    float bowlAngle() {
        if (event == Outcome.FISH_SAVED) {
            return lerp(-23, 0, easeOut(effectAge() / .27f));
        }
        return -18 - time * 28;
    }

    private void moveMarble(float dt) {
        if (resting) return;
        marbleX += velocityX * dt;
        marbleY += velocityY * dt + .5f * GRAVITY * dt * dt;
        velocityY += GRAVITY * dt;
        if (marbleX < 25 || marbleX > WIDTH - 25) {
            marbleX = Math.max(25, Math.min(WIDTH - 25, marbleX));
            velocityX *= -.72f;
        }
        if (marbleY < 73) {
            marbleY = 73;
            velocityY = Math.abs(velocityY) * .72f;
        }
        if (marbleY >= FLOOR - MARBLE_RADIUS) {
            marbleY = FLOOR - MARBLE_RADIUS;
            resting = true;
        }
    }

    private void updateVase() {
        if (cushionHitAt < 0 && velocityY > 0
                && Math.abs(marbleX - CUSHION_START_X) < 64
                && marbleY + MARBLE_RADIUS >= CUSHION_TOP && marbleY < CUSHION_Y) {
            cushionHitAt = time;
            velocityY *= -.46f;
            velocityX *= -.42f;
        }
        if (vaseY() + VASE_HALF_HEIGHT >= CUSHION_TOP
                && Math.abs(cushionX() - VASE_X) < 37) {
            finish(Outcome.VASE_SAVED, VASE_X, CUSHION_TOP);
        } else if (vaseY() + VASE_HALF_HEIGHT >= FLOOR) {
            finish(Outcome.VASE_BROKEN, VASE_X, FLOOR - 18);
        }
    }

    private void updateBowl() {
        if (touches(BELL_X, BELL_Y, 41, 41)) {
            finish(Outcome.CAT_AWAKE, BELL_X, BELL_Y);
            velocityX *= -.6f;
            velocityY = -130;
        } else if (touches(bowlX(), bowlY(), 52, 55)) {
            finish(Outcome.FISH_SAVED, bowlX(), bowlY());
            velocityX *= -.44f;
            velocityY = -100;
        } else if (bowlY() + BOWL_HALF_HEIGHT >= FLOOR) {
            finish(Outcome.FISH_FELL, bowlX(), FLOOR - 14);
        }
    }

    private boolean touches(float cx, float cy, float rx, float ry) {
        float dx = (marbleX - cx) / rx;
        float dy = (marbleY - cy) / ry;
        return dx * dx + dy * dy < 1;
    }

    private void finish(Outcome outcome, float x, float y) {
        event = outcome;
        eventAt = time;
        eventX = x;
        eventY = y;
    }

    static float clamp(float value) {
        return Math.max(0, Math.min(1, value));
    }

    static float lerp(float start, float end, float fraction) {
        return start + (end - start) * fraction;
    }

    private static float easeOut(float value) {
        float remaining = 1 - clamp(value);
        return 1 - remaining * remaining * remaining;
    }
}
