package com.doloreshazze.onesecond;

import static com.doloreshazze.onesecond.PuzzleRound.*;
import static com.doloreshazze.onesecond.WorkshopRound.smooth;

final class FairRound {
    enum Action {
        WATER_GATE, MAGNET_RELEASE, REMOVE_WEIGHT, ADD_WEIGHT, SHORT_RAIL, FELT_RAIL,
        DRAGON_MIRROR, HARE_MIRROR, BALL_COLLISION, MOUSE_CHASE, MOUSE_CUSHION,
        SPOOL_RELEASE, WATER_IMPACT, ECHO_GATE
    }

    static final float WATER_X = 192, WATER_Y = 375, WHEEL_X = 294, WHEEL_Y = 466;
    static final float MAGNET_LEVER_X = 230, MAGNET_LEVER_Y = 354, BELT_Y = 431;
    static final float SCALE_TAKE_X = 261, SCALE_TAKE_Y = 327, SCALE_ADD_X = 198, SCALE_ADD_Y = 472;
    static final float POST_SHORT_X = 177, POST_SHORT_Y = 298, POST_FELT_X = 181, POST_FELT_Y = 415;
    static final float POST_LATCH_X = 279, POST_LATCH_Y = 359, POST_BASKET_X = 325;
    static final float MIRROR_DRAGON_X = 231, MIRROR_DRAGON_Y = 294, MIRROR_HARE_X = 205, MIRROR_HARE_Y = 409;
    static final float BALL_TWO_X = 215, BALL_TWO_Y = 346, BALL_TWO_RADIUS = 18;
    static final float LOCK_LEFT_X = 133, LOCK_LEFT_Y = 298, LOCK_RIGHT_X = 290, LOCK_RIGHT_Y = 401;
    static final float MOUSE_X = 194, MOUSE_Y = 390, PEDAL_X = 311, PEDAL_Y = 515, PILLOW_X = 97;
    static final float SPOOL_X = 207, SPOOL_Y = 424, VANE_X = 326, VANE_Y = 223;
    static final float CANAL_Y = 441, DOCK_X = 313;
    static final float GATE_X = 193, GATE_Y = 351, ECHO_LOCK_X = 303, ECHO_LOCK_Y = 382;
    static final float ECHO_DELAY = .50f;
    private static final float MAGNET_DROP_TIME = .24f;

    private Action action;
    private float actionTime;
    private float railEntryX, railEntryY;
    private float firstLockAt = -1, secondLockAt = -1;
    private float waveX, waveArrival, waveDirection;
    private float launchVx, launchVy;
    private final Companion extra = new Companion();

    void recordLaunch(float vx, float vy) {
        launchVx = vx;
        launchVy = vy;
    }

    boolean has(Action expected) { return action == expected; }
    Action actionSince(float previousTime) { return actionTime > previousTime ? action : null; }
    float age(float time) { return action == null ? 0 : time - actionTime; }
    float position(float time, float duration) { return action == null ? 0 : smooth(age(time) / duration); }
    boolean leftOpen() { return firstLockAt >= 0; }
    boolean rightOpen() { return secondLockAt >= 0; }
    float auxiliaryX() { return extra.x; }
    float auxiliaryY() { return extra.y; }
    boolean auxiliaryVisible() { return extra.launched; }
    float waveX() { return waveX; }

    float effectDuration(Level level, Outcome event) {
        if (event == Outcome.FAIR_OPEN) return 1.57f;
        if (level == Level.THEATRE || level == Level.LOCKS || level == Level.KITE) return .93f;
        return .73f;
    }

    void update(PuzzleRound r, float dt) {
        switch (r.level) {
            case WATER: water(r); break;
            case MAGNET: magnet(r); break;
            case SCALE: scales(r); break;
            case POST: mail(r); break;
            case THEATRE: theatre(r); break;
            case LOCKS: locks(r, dt); break;
            case MOUSE: mouse(r); break;
            case KITE: kite(r); break;
            case CANAL: canal(r); break;
            case ECHO: echo(r, dt); break;
            default: throw new AssertionError(r.level);
        }
    }

    private void trigger(Action next, PuzzleRound r) {
        action = next;
        actionTime = r.time;
    }

    private void water(PuzzleRound r) {
        if (action == null && r.touches(WATER_X, WATER_Y, 44, 34)) trigger(Action.WATER_GATE, r);
        if (r.time >= .96f) {
            r.finish(waterFeedsWheel() ? Outcome.FLAG_RAISED : Outcome.BOOT_FILLED,
                    waterFeedsWheel() ? WHEEL_X : 108, waterFeedsWheel() ? WHEEL_Y : 468);
        }
    }

    boolean waterFeedsWheel() { return has(Action.WATER_GATE) && actionTime <= .57f; }

    private void magnet(PuzzleRound r) {
        if (action == null && r.time < .74f && r.touches(MAGNET_LEVER_X, MAGNET_LEVER_Y, 41, 32)) {
            trigger(Action.MAGNET_RELEASE, r);
        }
        if (has(Action.MAGNET_RELEASE) && age(r.time) >= MAGNET_DROP_TIME) {
            float x = magnetX(r.time);
            float tokenDistance = Math.abs(x - tokenX(r.time));
            float clipsDistance = Math.abs(x - clipsX(r.time));
            if (tokenDistance <= Math.min(37, clipsDistance)) r.finish(Outcome.TOKEN_CAUGHT, x, BELT_Y);
            else if (clipsDistance <= 43) r.finish(Outcome.CLIPS_CAUGHT, x, BELT_Y);
            else r.finish(Outcome.TOKEN_LOST, x, BELT_Y);
        } else if (r.time >= 1.02f) {
            r.finish(Outcome.TOKEN_LOST, tokenX(r.time), BELT_Y);
        }
    }

    float magnetX(float time) {
        if (!has(Action.MAGNET_RELEASE)) return swingingX(time);
        return swingingX(actionTime) + 30 * position(time, MAGNET_DROP_TIME);
    }

    float magnetY(float time) {
        if (!has(Action.MAGNET_RELEASE)) return swingingY(time);
        return lerp(swingingY(actionTime), BELT_Y - 48, position(time, MAGNET_DROP_TIME));
    }

    private static float swingingX(float time) { return lerp(162, 248, smooth(time / .30f)); }
    private static float swingingY(float time) { return 203 + 49 * arc(clamp(time / .75f)); }
    float tokenX(float time) { return Math.min(377, 248 + 67 * time); }
    float clipsX(float time) { return Math.min(331, tokenX(time) - 56); }

    private void scales(PuzzleRound r) {
        if (action == null && r.time < .65f) {
            if (r.touches(SCALE_TAKE_X, SCALE_TAKE_Y, 43, 34)) trigger(Action.REMOVE_WEIGHT, r);
            else if (r.touches(SCALE_ADD_X, SCALE_ADD_Y, 43, 31)) trigger(Action.ADD_WEIGHT, r);
        }
        if (r.time >= .94f) {
            Outcome result = has(Action.REMOVE_WEIGHT) ? Outcome.WEIGHT_REMOVED
                    : has(Action.ADD_WEIGHT) ? Outcome.WEIGHT_ADDED : Outcome.SCALE_JAMMED;
            r.finish(result, 264, 405);
        }
    }

    private void mail(PuzzleRound r) {
        if (action == null && r.time < .67f) {
            if (r.touches(POST_SHORT_X, POST_SHORT_Y, 40, 29)) enterRail(Action.SHORT_RAIL, r);
            else if (r.touches(POST_FELT_X, POST_FELT_Y, 40, 31)) enterRail(Action.FELT_RAIL, r);
        }
        if (action != null) {
            float parameter = clamp(age(r.time) / railTravelTime());
            float x = lerp(railEntryX, POST_LATCH_X, parameter);
            float y = lerp(railEntryY, POST_LATCH_Y, parameter) + (has(Action.FELT_RAIL) ? 56 * arc(parameter) : 0);
            r.holdMarble(x, y);
        }
        if (parcelAge(r.time) >= .18f) {
            boolean caught = action != null && Math.abs(basketY(r.time) - 353) <= 32;
            r.finish(caught ? Outcome.PARCEL_DELIVERED : Outcome.PARCEL_IN_HAT,
                    POST_BASKET_X, caught ? basketY(r.time) : 486);
        }
    }

    private void enterRail(Action route, PuzzleRound r) {
        trigger(route, r);
        railEntryX = r.marbleX;
        railEntryY = r.marbleY;
    }

    private float railTravelTime() { return has(Action.FELT_RAIL) ? .36f : .11f; }

    float parcelAge(float time) {
        return time - (action == null ? .86f : actionTime + railTravelTime());
    }

    float basketY(float time) { return Math.max(276, 515 - 223 * time); }

    private void theatre(PuzzleRound r) {
        if (action == null && r.time <= .64f) {
            if (r.touches(MIRROR_DRAGON_X, MIRROR_DRAGON_Y, 42, 32)) trigger(Action.DRAGON_MIRROR, r);
            else if (r.touches(MIRROR_HARE_X, MIRROR_HARE_Y, 41, 29)) trigger(Action.HARE_MIRROR, r);
        }
        if (r.time >= .83f) {
            Outcome result = has(Action.DRAGON_MIRROR) ? Outcome.DRAGON_SHADOW
                    : has(Action.HARE_MIRROR) ? Outcome.HARE_SHADOW : Outcome.THEATRE_DARK;
            r.finish(result, 210, 225);
        }
    }

    private void locks(PuzzleRound r, float dt) {
        if (!extra.launched && r.touches(BALL_TWO_X, BALL_TWO_Y, 38, 38)) collide(r);
        if (extra.launched) {
            extra.advance(dt, BALL_TWO_RADIUS);
            if (!leftOpen() && r.touches(LOCK_LEFT_X, LOCK_LEFT_Y, 38, 38)) firstLockAt = r.time;
            if (!rightOpen() && extra.near(LOCK_RIGHT_X, LOCK_RIGHT_Y, 39, 36)) secondLockAt = r.time;
            if (leftOpen() && rightOpen() && Math.abs(firstLockAt - secondLockAt) <= .31f) {
                r.finish(Outcome.BAND_PLAYED, 206, 338);
                return;
            }
        }
        if (r.time >= 1.15f) {
            r.finish(leftOpen() || rightOpen() ? Outcome.ONE_TRUMPET : Outcome.CABINET_SHUT, 207, 338);
        }
    }

    private void collide(PuzzleRound r) {
        float dx = BALL_TWO_X - r.marbleX, dy = BALL_TWO_Y - r.marbleY;
        float distance = (float) Math.hypot(dx, dy);
        if (distance < 1) return;
        float nx = dx / distance, ny = dy / distance;
        float impulse = r.horizontalSpeed() * nx + r.verticalSpeed() * ny;
        if (impulse <= 0) return;

        r.deflect(r.horizontalSpeed() - 1.43f * impulse * nx,
                r.verticalSpeed() - 1.43f * impulse * ny);
        extra.launch(BALL_TWO_X, BALL_TWO_Y, .57f * impulse * nx, .57f * impulse * ny);
        trigger(Action.BALL_COLLISION, r);
    }

    private void mouse(PuzzleRound r) {
        if (action == null && r.time < .54f && r.touches(MOUSE_X, MOUSE_Y, 40, 34)) {
            trigger(r.marbleY < MOUSE_Y + 7 ? Action.MOUSE_CHASE : Action.MOUSE_CUSHION, r);
        }
        if (action != null && age(r.time) >= .58f) {
            r.finish(has(Action.MOUSE_CHASE) ? Outcome.CAT_LIT_SIGN : Outcome.MOUSE_CUDDLED,
                    has(Action.MOUSE_CHASE) ? PEDAL_X : PILLOW_X, PEDAL_Y);
        } else if (r.time >= 1.15f) {
            r.finish(Outcome.CAT_STILL_SLEEPING, 108, 492);
        }
    }

    private void kite(PuzzleRound r) {
        if (action == null && r.time <= .61f && r.touches(SPOOL_X, SPOOL_Y, 43, 32)) {
            trigger(Action.SPOOL_RELEASE, r);
        }
        if (r.time >= .92f) {
            boolean free = has(Action.SPOOL_RELEASE);
            r.finish(free ? Outcome.KITE_FLEW : Outcome.KITE_TANGLED, free ? 274 : VANE_X, free ? 143 : VANE_Y);
        }
    }

    float kiteX(float time) {
        return has(Action.SPOOL_RELEASE) ? lerp(146 + 181 * actionTime, 281, position(time, .53f))
                : Math.min(VANE_X, 146 + 195 * time);
    }

    float kiteY(float time) {
        return has(Action.SPOOL_RELEASE) ? lerp(280 - 62 * actionTime, 130, position(time, .57f))
                : Math.max(VANE_Y, 280 - 65 * time);
    }

    private void canal(PuzzleRound r) {
        if (action == null && r.marbleY >= CANAL_Y - 12 && r.verticalSpeed() > 0) {
            trigger(Action.WATER_IMPACT, r);
            waveX = r.marbleX;
            float distance = driftingBoatX(r.time) - waveX;
            waveArrival = r.time + .07f + Math.min(Math.abs(distance), 112) / 560;
            waveDirection = distance >= 13 && distance < 112 && waveArrival < .79f ? 1
                    : distance <= -12 && distance > -112 ? -1 : 0;
            r.holdMarble(waveX, CANAL_Y + 2);
        }
        if (r.time >= 1.07f) {
            boolean moored = boatX(r.time) >= DOCK_X - 18;
            r.finish(moored ? Outcome.BOAT_DOCKED : Outcome.BOAT_SPUN, boatX(r.time), CANAL_Y);
        }
    }

    private float driftingBoatX(float time) { return Math.max(129, 234 - 75 * time); }

    float boatX(float time) {
        float drift = driftingBoatX(time);
        if (!has(Action.WATER_IMPACT)) return drift;
        float swept = smooth((time - waveArrival) / .41f);
        return waveDirection > 0 ? lerp(drift, DOCK_X, swept) : drift - 46 * swept * -waveDirection;
    }

    private void echo(PuzzleRound r, float dt) {
        if (action == null && r.horizontalSpeed() > 0 && r.touches(GATE_X, GATE_Y, 32, 32)) {
            trigger(Action.ECHO_GATE, r);
            r.reflect(1, 0);
        }
        if (r.time >= ECHO_DELAY) {
            if (!extra.launched) {
                extra.launch(r.level.startX, r.level.startY, launchVx, launchVy);
                extra.advance(r.time - ECHO_DELAY, MARBLE_RADIUS);
            } else {
                extra.advance(dt, MARBLE_RADIUS);
            }
            if (position(r.time, .15f) < .85f && extra.vx > 0 && extra.near(GATE_X, GATE_Y, 32, 34)) {
                extra.vx *= -.74f;
            }
            if (has(Action.ECHO_GATE) && extra.near(ECHO_LOCK_X, ECHO_LOCK_Y, 31, 34)) {
                r.finish(Outcome.FAIR_OPEN, ECHO_LOCK_X, ECHO_LOCK_Y);
                return;
            }
        }
        if (r.time >= 1.18f) r.finish(Outcome.FAIR_SHUT, ECHO_LOCK_X, ECHO_LOCK_Y);
    }

    private static float arc(float fraction) { return (float) Math.sin(Math.PI * fraction); }

    private static final class Companion {
        float x, y, vx, vy;
        boolean launched, resting;

        void launch(float startX, float startY, float horizontal, float vertical) {
            x = startX;
            y = startY;
            vx = horizontal;
            vy = vertical;
            launched = true;
        }

        void advance(float dt, float radius) {
            if (resting) return;
            x += vx * dt;
            y += vy * dt + .5f * GRAVITY * dt * dt;
            vy += GRAVITY * dt;
            if (x < 25 || x > WIDTH - 25) {
                x = Math.max(25, Math.min(WIDTH - 25, x));
                vx *= -.72f;
            }
            if (y < 73) {
                y = 73;
                vy = Math.abs(vy) * .72f;
            }
            if (y >= FLOOR - radius) {
                y = FLOOR - radius;
                resting = true;
            }
        }

        boolean near(float cx, float cy, float rx, float ry) {
            float dx = (x - cx) / rx, dy = (y - cy) / ry;
            return dx * dx + dy * dy < 1;
        }
    }
}
