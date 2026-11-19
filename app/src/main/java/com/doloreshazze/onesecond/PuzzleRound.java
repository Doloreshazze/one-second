package com.doloreshazze.onesecond;

final class PuzzleRound {
    enum Level {
        VASE(89, 290),
        CAT(89, 423),
        DROP(86, 345),
        KEY(80, 340),
        GLIDER(80, 358),
        TRAIN(88, 300),
        NOTES(70, 294),
        PHOTO(78, 416),
        CLOCK(78, 354),
        WATER(78, 342),
        MAGNET(81, 370),
        SCALE(80, 379),
        POST(76, 351),
        THEATRE(80, 370),
        LOCKS(78, 330),
        MOUSE(82, 383),
        KITE(79, 335),
        CANAL(78, 278),
        ECHO(74, 335);

        final float startX, startY;

        Level(float startX, float startY) {
            this.startX = startX;
            this.startY = startY;
        }
    }

    enum Outcome {
        VASE_SAVED(true),
        VASE_BROKEN(false),
        FISH_SAVED(true),
        CAT_AWAKE(false),
        FISH_FELL(false),
        DROP_CAUGHT(true),
        PAPER_SAVED(true),
        INK_BLOT(false),
        KEY_SAVED(true),
        KEY_LOST(false),
        GLIDER_LANDED(true),
        GLIDER_WET(false),
        TRAIN_CROSSED(true),
        TRAIN_DERAILED(false),
        NOTES_PLAYED(true),
        NOTES_MISSED(false),
        PHOTO_TAKEN(true),
        PHOTO_MISSED(false),
        CLOCK_STARTED(true),
        CLOCK_JAMMED(false),
        FLAG_RAISED(true),
        BOOT_FILLED(false),
        TOKEN_CAUGHT(true),
        CLIPS_CAUGHT(false),
        TOKEN_LOST(false),
        WEIGHT_REMOVED(true),
        WEIGHT_ADDED(true),
        SCALE_JAMMED(false),
        PARCEL_DELIVERED(true),
        PARCEL_IN_HAT(false),
        DRAGON_SHADOW(true),
        HARE_SHADOW(true),
        THEATRE_DARK(false),
        BAND_PLAYED(true),
        ONE_TRUMPET(false),
        CABINET_SHUT(false),
        CAT_LIT_SIGN(true),
        MOUSE_CUDDLED(false),
        CAT_STILL_SLEEPING(false),
        KITE_FLEW(true),
        KITE_TANGLED(false),
        BOAT_DOCKED(true),
        BOAT_SPUN(false),
        FAIR_OPEN(true),
        FAIR_SHUT(false);

        final boolean won;

        Outcome(boolean won) {
            this.won = won;
        }
    }

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

    static final float GRAVITY = 480;
    private static final float SPEED = 500;
    private static final float CUSHION_TOP = 491;
    private static final float VASE_HALF_HEIGHT = 39;
    private static final float BOWL_HALF_HEIGHT = 39;
    private static final float SIM_STEP = 1f / 240;

    final Level level;
    final WorkshopRound workshop;
    final FairRound fair;
    boolean started;
    float time, marbleX, marbleY, eventX, eventY;
    Outcome event;
    private float velocityX, velocityY;
    private float cushionHitAt = -1;
    private float eventAt;
    private boolean resting;

    PuzzleRound(Level level) {
        this.level = level;
        marbleX = level.startX;
        marbleY = level.startY;
        fair = level.ordinal() >= Level.WATER.ordinal() ? new FairRound() : null;
        workshop = level.ordinal() >= Level.DROP.ordinal() && fair == null ? new WorkshopRound() : null;
    }

    void launch(float dx, float dy) {
        float scale = SPEED / (float) Math.hypot(dx, dy);
        velocityX = dx * scale;
        velocityY = dy * scale;
        if (level == Level.ECHO) fair.recordLaunch(velocityX, velocityY);
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
        return started && (event == null || effectAge() < effectDuration());
    }

    private float effectDuration() {
        if (fair != null) return fair.effectDuration(level, event);
        return workshop == null ? EFFECT_DURATION : workshop.effectDuration(level);
    }

    boolean won() {
        return event != null && event.won;
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
                else if (level == Level.CAT) updateBowl();
                else if (workshop != null) workshop.update(this);
                else fair.update(this, dt);
            }
            seconds -= dt;
        }
    }

    float cushionX() {
        if (cushionHitAt < 0) return CUSHION_START_X;
        return lerp(CUSHION_START_X, VASE_X, easeOut((time - cushionHitAt) / .29f));
    }

    boolean cushionHitSince(float previousTime) {
        return cushionHitAt > previousTime;
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

    boolean touches(float cx, float cy, float rx, float ry) {
        float dx = (marbleX - cx) / rx;
        float dy = (marbleY - cy) / ry;
        return dx * dx + dy * dy < 1;
    }

    void finish(Outcome outcome, float x, float y) {
        event = outcome;
        eventAt = time;
        eventX = x;
        eventY = y;
    }

    float horizontalSpeed() {
        return velocityX;
    }

    float verticalSpeed() {
        return velocityY;
    }

    void deflect(float vx, float vy) {
        velocityX = vx;
        velocityY = vy;
    }

    void holdMarble(float x, float y) {
        marbleX = x;
        marbleY = y;
        resting = true;
    }

    void reflect(float normalX, float normalY) {
        float projection = velocityX * normalX + velocityY * normalY;
        if (projection <= 0) return;
        velocityX -= 2 * projection * normalX;
        velocityY -= 2 * projection * normalY;
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
