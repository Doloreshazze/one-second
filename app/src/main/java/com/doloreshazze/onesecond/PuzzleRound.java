package com.doloreshazze.onesecond;

final class PuzzleRound {
    enum Level {
        VASE("Спаси вазу", "Сдвинь подушку под вазу", 89, 290),
        CAT("Не разбуди кота", "Попади в аквариум мимо звонка", 89, 423),
        DROP("Последняя капля", "Спаси рисунок от капли", 86, 345),
        KEY("Ключ под половицей", "Отбей шар от латунной пластины", 80, 340),
        GLIDER("Бумажный пилот", "Разверни поток под самолётик", 80, 358),
        TRAIN("Мост в последний момент", "Опусти мост перед поездом", 88, 300),
        NOTES("Три ноты", "Задень пластины по порядку", 70, 294),
        PHOTO("Снимок с котом", "Позвони перед щелчком камеры", 78, 416),
        CLOCK("Сердце мастерской", "Толкни коромысло под грузом", 78, 354);

        final String title, objective;
        final float startX, startY;

        Level(String title, String objective, float startX, float startY) {
            this.title = title;
            this.objective = objective;
            this.startX = startX;
            this.startY = startY;
        }
    }

    enum Outcome {
        VASE_SAVED(true, "Ваза спасена", "Мягкая посадка"),
        VASE_BROKEN(false, "Ваза разбилась", "Задень подушку шаром"),
        FISH_SAVED(true, "Рыбка спасена", "Кот ничего не заметил"),
        CAT_AWAKE(false, "Кот проснулся!", "Проведи шар выше звонка"),
        FISH_FELL(false, "Аквариум упал", "Подтолкни аквариум на полку"),
        DROP_CAUGHT(true, "Капля в блюдце", "Есть ещё один способ спасти рисунок"),
        PAPER_SAVED(true, "Рисунок сухой", "Лоток ушёл под полку"),
        INK_BLOT(false, "Клякса с ушами", "Попробуй блюдце или лоток"),
        KEY_SAVED(true, "Ключ спасён", "Ключ упал на коврик"),
        KEY_LOST(false, "Ключ у мышки", "Попробуй попасть в пластину снизу"),
        GLIDER_LANDED(true, "Мягкая посадка", "Ветер донёс самолётик до полки"),
        GLIDER_WET(false, "Самолётик в чае", "Задень заслонку вентилятора"),
        TRAIN_CROSSED(true, "Поезд проехал", "Мост встал на место вовремя"),
        TRAIN_DERAILED(false, "Утка на свободе", "Успей сбить упор под мостом"),
        NOTES_PLAYED(true, "Мелодия сыграна", "Шкатулка открылась"),
        NOTES_MISSED(false, "Мелодия оборвалась", "Проведи шар через все три ноты"),
        PHOTO_TAKEN(true, "Кот в кадре", "Взгляд точно в объектив"),
        PHOTO_MISSED(false, "Кот отвернулся", "Поймай момент перед вспышкой"),
        CLOCK_STARTED(true, "Часы пошли", "В мастерской наступило утро"),
        CLOCK_JAMMED(false, "Часы замерли", "Подними опору до падения груза");

        final boolean won;
        final String title, detail;

        Outcome(boolean won, String title, String detail) {
            this.won = won;
            this.title = title;
            this.detail = detail;
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

    private static final float GRAVITY = 480;
    private static final float SPEED = 500;
    private static final float CUSHION_TOP = 491;
    private static final float VASE_HALF_HEIGHT = 39;
    private static final float BOWL_HALF_HEIGHT = 39;
    private static final float SIM_STEP = 1f / 240;

    final Level level;
    final WorkshopRound workshop;
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
        workshop = level.ordinal() >= Level.DROP.ordinal() ? new WorkshopRound() : null;
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
        return started && (event == null || effectAge() < (workshop == null
                ? EFFECT_DURATION : workshop.effectDuration(level)));
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
                else workshop.update(this);
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
