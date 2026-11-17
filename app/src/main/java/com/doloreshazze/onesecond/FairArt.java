package com.doloreshazze.onesecond;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.SparseArray;

import static com.doloreshazze.onesecond.FairRound.*;
import static com.doloreshazze.onesecond.PuzzleRound.*;
import static com.doloreshazze.onesecond.StageArt.*;
import static com.doloreshazze.onesecond.WorkshopRound.smooth;

final class FairArt {
    private static final int BRASS = 0xffb68150;
    private static final int EDGE = 0xfff6c991;
    private static final int WOOD = 0xff54362d;
    private static final int DARK = 0xff281e28;
    private static final int[] FLAGS = {GOLD, CORAL, ICE, 0xffdbabed};
    private static final String SIGN = "ЯРМАРКА";
    private static final int[] BACKGROUNDS = {
            R.drawable.fair_scene_water, R.drawable.fair_scene_magnet, R.drawable.fair_scene_scale,
            R.drawable.fair_scene_post, R.drawable.fair_scene_theatre, R.drawable.fair_scene_locks,
            R.drawable.fair_scene_mouse, R.drawable.fair_scene_kite, R.drawable.fair_scene_canal,
            R.drawable.fair_scene_echo
    };

    private final Resources resources;
    private final StageArt art;
    private final SparseArray<Bitmap> pieces = new SparseArray<>();
    private final RectF bounds = new RectF(0, 0, WIDTH, HEIGHT);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path path = new Path(), dragon = dragonShape(), wing = wingShape();
    private final Shader glass = new RadialGradient(-5, -7, 23,
            0xfff4fff3, 0xff337486, Shader.TileMode.CLAMP);
    private final Shader lanternGlow = new RadialGradient(0, 0, 74,
            0xa9ffd16c, 0x00ffad53, Shader.TileMode.CLAMP);
    private Bitmap background, panorama;
    private Level cachedLevel;

    FairArt(Resources resources, StageArt art) {
        this.resources = resources;
        this.art = art;
        paint.setTypeface(Typeface.create("serif", Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
    }

    void background(Canvas c, PuzzleRound r) {
        if (cachedLevel != r.level) {
            cachedLevel = r.level;
            background = BitmapFactory.decodeResource(resources, BACKGROUNDS[r.level.ordinal() - Level.WATER.ordinal()]);
            panorama = null;
        }
        color(0xffffffff);
        c.drawBitmap(background, null, bounds, paint);
        float reveal = finaleProgress(r);
        if (reveal > 0) {
            if (panorama == null) panorama = BitmapFactory.decodeResource(resources, R.drawable.fair_scene_night_fair);
            c.save();
            float zoom = 1 + .25f * (1 - reveal);
            c.scale(zoom, zoom, 200, 308);
            paint.setAlpha(Math.round(255 * reveal));
            c.drawBitmap(panorama, null, bounds, paint);
            paint.setAlpha(255);
            c.restore();
        }
    }

    void scene(Canvas c, PuzzleRound r, float ambient) {
        switch (r.level) {
            case WATER: wheel(c, r); break;
            case MAGNET: magnet(c, r); break;
            case SCALE: scale(c, r); break;
            case POST: post(c, r); break;
            case THEATRE: theatre(c, r); break;
            case LOCKS: locks(c, r); break;
            case MOUSE: mouse(c, r, ambient); break;
            case KITE: kite(c, r); break;
            case CANAL: canal(c, r); break;
            case ECHO: echo(c, r, ambient); break;
            default: throw new AssertionError(r.level);
        }
    }

    private void wheel(Canvas c, PuzzleRound r) {
        FairRound motion = r.fair;
        float angle = 38 * motion.position(r.time, .22f);
        float flow = clamp((r.time - .57f) / .33f);
        boolean sendsWater = motion.waterFeedsWheel();
        float endX = sendsWater ? WHEEL_X - 28 : 105;
        float endY = sendsWater ? WHEEL_Y - 28 : 473;
        line(c, 62, 232, 183, 368, 12, 0xff324643);
        line(c, 64, 229, 183, 366, 4, 0xff7d9c93);
        line(c, 309, 507, 309, 275, 7, WOOD);
        line(c, 309, 507, 309, 275, 2, BRASS);
        art.shadow(c, WHEEL_X, 522, 56, 11);
        sprite(c, R.drawable.fair_piece_wheel, WHEEL_X, WHEEL_Y, 108,
                sendsWater ? 190 * Math.max(0, r.time - .59f) : 0);
        line(c, 330, 444, 330, 198, 1.8f, EDGE);
        float raise = sendsWater ? smooth((r.time - .78f) / .62f) : 0;
        float flagY = lerp(395, 227, raise);
        pennant(c, 331, flagY, 48, 10 * arc(raise), CORAL);
        line(c, 294, 466, 330, 444, 2.3f, BRASS);
        sprite(c, R.drawable.fair_piece_boot, 113, 487, 100,
                r.event == Outcome.BOOT_FILLED ? -28 * smooth(r.effectAge() / .60f) : 0);
        c.save();
        c.rotate(angle - 26, WATER_X, WATER_Y);
        plate(c, WATER_X, WATER_Y, 65, 15, false);
        c.restore();

        float rain = r.time * 1.75f;
        for (int i = 0; i < 11; i++) {
            float p = (rain + i * .091f) % 1;
            float x = lerp(64, WATER_X, p), y = lerp(222, WATER_Y, p);
            line(c, x - 2.3f, y - 4, x + 1, y + 4, 2.2f, alpha(ICE, 90 + i * 12));
        }
        if (flow > 0) {
            for (int i = 0; i < 13; i++) {
                float p = (r.time * 2 + i * .077f) % 1;
                if (p > flow) continue;
                float x = lerp(WATER_X, endX, p), y = lerp(WATER_Y, endY, p * p);
                line(c, x - 2, y - 2, x + 1.5f, y + 5, 2.5f, alpha(ICE, 105 + i * 9));
            }
        }
        if (r.event != null) ripple(c, endX, endY + 4, r.effectAge(), ICE);
        focus(c, r, WATER_X, WATER_Y, 38, ICE);
    }

    private void magnet(Canvas c, PuzzleRound r) {
        FairRound motion = r.fair;
        float x = motion.magnetX(r.time), y = motion.magnetY(r.time);
        if (r.event == Outcome.TOKEN_CAUGHT || r.event == Outcome.CLIPS_CAUGHT) {
            y -= 129 * smooth(r.effectAge() / .67f);
        }
        line(c, 117, 143, 351, 143, 8, WOOD);
        line(c, 117, 141, 351, 141, 2.8f, EDGE);
        line(c, 253, 143, x, y - 37, 2.4f, 0xffddbb93);
        plate(c, 252, 143, 31, 15, false);
        line(c, 253, 147, MAGNET_LEVER_X, MAGNET_LEVER_Y, 1.55f, 0x7dd4a478);
        plate(c, MAGNET_LEVER_X, MAGNET_LEVER_Y, 52, 18, motion.has(Action.MAGNET_RELEASE));

        color(DARK);
        c.drawRoundRect(133, BELT_Y - 13, 399, BELT_Y + 19, 10, 10, paint);
        for (int i = 0; i < 14; i++) {
            float roller = 140 + (i * 18 + r.time * 77) % 247;
            line(c, roller, BELT_Y - 8, roller + 8, BELT_Y + 14, 2.1f, 0xff765447);
        }
        if (r.event != Outcome.CLIPS_CAUGHT) sprite(c, R.drawable.fair_piece_clips, motion.clipsX(r.time), BELT_Y - 5, 43, 6);
        if (r.event != Outcome.TOKEN_CAUGHT) sprite(c, R.drawable.fair_piece_token, motion.tokenX(r.time), BELT_Y - 7, 38, r.time * 72);
        sprite(c, R.drawable.fair_piece_magnet, x, y, 87, (float) Math.sin(r.time * 8) * 6);
        if (r.event == Outcome.TOKEN_CAUGHT) sprite(c, R.drawable.fair_piece_token, x, y + 43, 39, -12);
        if (r.event == Outcome.CLIPS_CAUGHT) sprite(c, R.drawable.fair_piece_clips, x, y + 46, 79,
                (float) Math.sin(r.effectAge() * 22) * 7);
        focus(c, r, MAGNET_LEVER_X, MAGNET_LEVER_Y, 35, GOLD);
    }

    private void scale(Canvas c, PuzzleRound r) {
        FairRound motion = r.fair;
        boolean decrease = motion.has(Action.REMOVE_WEIGHT), increase = motion.has(Action.ADD_WEIGHT);
        float settle = motion.position(r.time, .28f);
        float beamAngle = 14 * (1 - settle);
        if (r.event == Outcome.SCALE_JAMMED) beamAngle += 5 * arc(clamp(r.effectAge() / .56f));
        color(DARK);
        c.drawRoundRect(264, 431, 380, 550, 10, 10, paint);
        if (r.won()) {
            float open = smooth(r.effectAge() / .59f);
            glow(c, 327, 478, 48 + 30 * open);
            sprite(c, R.drawable.fair_piece_cookie, 322 - 24 * open, 482 - 11 * arc(open), 59, -23 * open);
        }
        float open = r.won() ? smooth(r.effectAge() / .49f) : 0;
        door(c, 265 - 39 * open, 435, 321 - 39 * open, 546);
        door(c, 322 + 39 * open, 435, 377 + 39 * open, 546);

        line(c, 222, 461, 222, 346, 11, WOOD);
        line(c, 222, 461, 222, 346, 3.2f, BRASS);
        plate(c, 222, 462, 69, 12, false);
        c.save();
        c.rotate(beamAngle, 220, 344);
        line(c, 141, 344, 287, 344, 9.2f, WOOD);
        line(c, 142, 342, 286, 342, 3.8f, EDGE);
        pan(c, 143, 344);
        pan(c, 281, 344);
        c.restore();
        sprite(c, R.drawable.fair_piece_weight, decrease ? lerp(SCALE_TAKE_X, 364, settle) : SCALE_TAKE_X,
                decrease ? lerp(SCALE_TAKE_Y, 459, settle) - 17 * arc(settle) : SCALE_TAKE_Y, 58, 49 * (decrease ? settle : 0));
        sprite(c, R.drawable.fair_piece_weight, increase ? lerp(SCALE_ADD_X, 142, settle) : SCALE_ADD_X,
                increase ? lerp(SCALE_ADD_Y, 374, settle) - 35 * arc(settle) : SCALE_ADD_Y, 47, 0);
        focus(c, r, SCALE_TAKE_X, SCALE_TAKE_Y, 39, GOLD);
        focus(c, r, SCALE_ADD_X, SCALE_ADD_Y, 36, ICE);
    }

    private void pan(Canvas c, float x, float pivotY) {
        line(c, x, pivotY, x - 23, pivotY + 41, 1.6f, EDGE);
        line(c, x, pivotY, x + 23, pivotY + 41, 1.6f, EDGE);
        color(0xff765045);
        c.drawOval(x - 31, pivotY + 34, x + 31, pivotY + 48, paint);
        color(EDGE);
        c.drawOval(x - 31, pivotY + 34, x + 31, pivotY + 42, paint);
    }

    private void post(Canvas c, PuzzleRound r) {
        FairRound motion = r.fair;
        line(c, 305, 170, 305, 526, 3.1f, 0xffbd997e);
        line(c, 350, 170, 350, 526, 3.1f, 0xffbd997e);
        rail(c, POST_SHORT_X - 24, POST_SHORT_Y - 12, POST_LATCH_X, POST_LATCH_Y, false);
        rail(c, POST_FELT_X - 26, POST_FELT_Y + 12, POST_LATCH_X, POST_LATCH_Y, true);
        plate(c, POST_LATCH_X, POST_LATCH_Y, 33, 15, motion.parcelAge(r.time) > 0);
        float spill = clamp(motion.parcelAge(r.time) / .18f);
        c.save();
        c.rotate(37 * spill, 295, 292);
        line(c, 292, 292, 366, 292, 6, WOOD);
        line(c, 295, 289, 366, 289, 2.2f, EDGE);
        c.restore();

        float basketY = motion.basketY(r.time);
        line(c, POST_BASKET_X, 181, POST_BASKET_X, basketY - 14, 2.3f, EDGE);
        art.shadow(c, 337, 547, 50, 9);
        art.anchoredSprite(c, piece(R.drawable.fair_piece_merchant), 362, 557, 76);
        sprite(c, R.drawable.fair_piece_basket, POST_BASKET_X, basketY, 93, 0);
        float parcelX = POST_BASKET_X, parcelY = lerp(267, 350, spill * spill);
        if (r.event == Outcome.PARCEL_DELIVERED) parcelY = basketY - 17;
        if (r.event == Outcome.PARCEL_IN_HAT) {
            float fall = smooth(r.effectAge() / .40f);
            parcelX += 36 * fall;
            parcelY = lerp(parcelY, 468, fall);
        }
        sprite(c, R.drawable.fair_piece_parcel, parcelX, parcelY, 62, spill * 14);
        focus(c, r, POST_SHORT_X, POST_SHORT_Y, 32, GOLD);
        focus(c, r, POST_FELT_X, POST_FELT_Y, 35, ICE);
    }

    private void rail(Canvas c, float x, float y, float toX, float toY, boolean felt) {
        path.reset();
        path.moveTo(x, y);
        path.cubicTo(x + 53, y + (felt ? 57 : 13), toX - 43, toY + (felt ? 98 : -24), toX, toY);
        stroke(15, 0xff382d32);
        c.drawPath(path, paint);
        stroke(9, felt ? 0xff9b616e : BRASS);
        c.drawPath(path, paint);
        stroke(2.1f, felt ? 0xffd49a92 : EDGE);
        c.drawPath(path, paint);
    }

    private void theatre(Canvas c, PuzzleRound r) {
        FairRound motion = r.fair;
        boolean dragonChosen = motion.has(Action.DRAGON_MIRROR), hareChosen = motion.has(Action.HARE_MIRROR);
        float mirror = motion.position(r.time, .18f);
        if (dragonChosen || hareChosen) {
            color(alpha(0xffffd99f, (int) (101 * mirror)));
            path.reset();
            path.moveTo(103, 253);
            path.lineTo(204, 365);
            path.lineTo(341, 172);
            path.lineTo(162, 140);
            path.close();
            c.drawPath(path, paint);
        }
        float lift = r.event == null ? 0 : smooth(r.effectAge() / .43f);
        if (r.won() && lift > 0) {
            c.save();
            c.clipRect(43, 106, 367, 360);
            c.translate(200, 242);
            c.scale(.48f + .53f * lift, .48f + .53f * lift);
            if (dragonChosen) shadowDragon(c, r.effectAge());
            else shadowHare(c, r.effectAge());
            c.restore();
        } else if (r.event == Outcome.THEATRE_DARK) {
            color(alpha(0xff402d49, (int) (165 * lift)));
            c.drawRoundRect(43, 112, 364, 360, 30, 30, paint);
        }
        line(c, 208, 425, 208, 367, 5, WOOD);
        c.save();
        c.rotate(-27 + (dragonChosen ? -34 : 68) * mirror, 208, 365);
        plate(c, 208, 365, 88, 16, false);
        line(c, 170, 363, 244, 363, 3.2f, 0xffc7e6df);
        c.restore();
        plate(c, MIRROR_DRAGON_X, MIRROR_DRAGON_Y, 39, 13, dragonChosen);
        plate(c, MIRROR_HARE_X, MIRROR_HARE_Y, 39, 13, hareChosen);
        focus(c, r, MIRROR_DRAGON_X, MIRROR_DRAGON_Y, 33, GOLD);
        focus(c, r, MIRROR_HARE_X, MIRROR_HARE_Y, 33, ICE);
    }

    private void shadowDragon(Canvas c, float age) {
        color(0xf2422848);
        c.drawPath(dragon, paint);
        c.save();
        c.rotate(18 * (float) Math.sin(7 * age), 8, 8);
        c.drawPath(wing, paint);
        c.restore();
        color(0xffefad71);
        c.drawCircle(73, -29, 3.5f, paint);
    }

    private void shadowHare(Canvas c, float age) {
        color(0xf2583551);
        c.drawOval(-82, -1, 59, 70, paint);
        c.drawOval(10, -57, 78, 15, paint);
        c.drawCircle(-81, 27, 18, paint);
        c.drawOval(23, 59, 99, 73, paint);
        c.save();
        c.rotate(-13 + 8 * (float) Math.sin(11 * age), 41, -45);
        c.drawOval(26, -124, 47, -29, paint);
        c.drawOval(46, -129, 66, -31, paint);
        c.restore();
        color(0xffffc78a);
        c.drawCircle(65, -28, 4, paint);
    }

    private void locks(Canvas c, PuzzleRound r) {
        FairRound motion = r.fair;
        float rise = r.won() ? smooth(r.effectAge() / .61f) : 0;
        color(0xff241b27);
        c.drawRoundRect(117, 129, 317, 361, 13, 13, paint);
        if (r.won() || r.event == Outcome.ONE_TRUMPET) {
            int performer = r.won() ? R.drawable.fair_piece_band : R.drawable.fair_piece_trumpeter;
            float out = smooth(r.effectAge() / .46f);
            float bounce = r.won() ? (float) Math.sin(r.effectAge() * 23) * 4 : 0;
            glow(c, 217, 218, 82 * out);
            sprite(c, performer, 216, 338 - 114 * out + bounce, r.won() ? 153 : 81, 0);
        }
        c.save();
        c.clipRect(115, 129, 322, 364);
        door(c, 118 - 100 * rise, 131, 219 - 100 * rise, 361);
        float crack = r.event == Outcome.ONE_TRUMPET ? 53 * smooth(r.effectAge() / .35f) : 0;
        door(c, 221 + 96 * rise + crack, 131, 319 + 96 * rise + crack, 361);
        c.restore();
        line(c, LOCK_LEFT_X, LOCK_LEFT_Y, 121, 241, 2.6f, BRASS);
        line(c, LOCK_RIGHT_X, LOCK_RIGHT_Y, 307, 346, 2.6f, BRASS);
        plate(c, LOCK_LEFT_X, LOCK_LEFT_Y, 43, 18, motion.leftOpen());
        plate(c, LOCK_RIGHT_X, LOCK_RIGHT_Y, 49, 18, motion.rightOpen());
        if (motion.auxiliaryVisible()) {
            ball(c, motion.auxiliaryX(), motion.auxiliaryY(), BALL_TWO_RADIUS, 255);
            float spark = clamp(motion.age(r.time) / .20f);
            stroke(2, alpha(ICE, Math.round(145 * (1 - spark))));
            c.drawCircle(BALL_TWO_X - 17, BALL_TWO_Y - 8, 10 + 28 * spark, paint);
        } else {
            ball(c, BALL_TWO_X, BALL_TWO_Y, BALL_TWO_RADIUS, 255);
            focus(c, r, BALL_TWO_X, BALL_TWO_Y, 31, ICE);
        }
    }

    private void mouse(Canvas c, PuzzleRound r, float ambient) {
        FairRound motion = r.fair;
        boolean chase = motion.has(Action.MOUSE_CHASE), cushion = motion.has(Action.MOUSE_CUSHION);
        float run = motion.position(r.time, .36f);
        if (!chase && !cushion) run = 0;
        float to = chase ? PEDAL_X : PILLOW_X;
        if (r.event == Outcome.CAT_LIT_SIGN) glow(c, 243, 94, 127 * smooth(r.effectAge() / .44f));
        color(r.event == Outcome.CAT_LIT_SIGN ? 0xffffe7a0 : 0xff6e452e);
        paint.setTextSize(27);
        c.drawText(SIGN, 240, 98, paint);
        rail(c, MOUSE_X, MOUSE_Y + 7, PEDAL_X, PEDAL_Y - 19, false);
        rail(c, MOUSE_X, MOUSE_Y + 7, PILLOW_X, PEDAL_Y - 29, true);
        art.shadow(c, PILLOW_X, 528, 61, 11);
        sprite(c, R.drawable.prop_cushion, PILLOW_X, 512, 114, 0);
        plate(c, PEDAL_X, PEDAL_Y, 106, 19, r.event == Outcome.CAT_LIT_SIGN);
        float mouseX = lerp(MOUSE_X, to, run), mouseY = lerp(MOUSE_Y, PEDAL_Y - 30, run);
        sprite(c, R.drawable.fair_piece_mouse, mouseX, mouseY, 66, (float) Math.sin(run * 22) * 5);
        float jump = chase || cushion ? smooth((motion.age(r.time) - .13f) / .41f) : 0;
        float catX = lerp(84, to, jump), catY = lerp(527, PEDAL_Y + 3, jump) - 78 * arc(jump);
        boolean awake = (chase || cushion) && motion.age(r.time) >= .10f && r.event != Outcome.MOUSE_CUDDLED;
        art.drawCat(c, awake, catX, catY, 113, ambient);
        focus(c, r, MOUSE_X, MOUSE_Y, 40, GOLD);
    }

    private void kite(Canvas c, PuzzleRound r) {
        FairRound motion = r.fair;
        float x = motion.kiteX(r.time), y = motion.kiteY(r.time);
        float sway = (float) Math.sin(r.time * 17) * 8;
        sprite(c, R.drawable.fair_piece_vane, VANE_X, VANE_Y + 44, 95, 0);
        art.shadow(c, SPOOL_X, SPOOL_Y + 41, 55, 8);
        color(WOOD);
        c.drawRoundRect(SPOOL_X - 30, SPOOL_Y + 11, SPOOL_X + 32, SPOOL_Y + 40, 7, 7, paint);
        art.shadow(c, SPOOL_X, SPOOL_Y, 27, 25);
        color(BRASS);
        c.drawCircle(SPOOL_X, SPOOL_Y, 27, paint);
        for (int i = 0; i < 3; i++) {
            stroke(2.0f, i % 2 == 0 ? EDGE : WOOD);
            c.drawCircle(SPOOL_X, SPOOL_Y, 18 - i * 5, paint);
        }
        plate(c, SPOOL_X + (motion.has(Action.SPOOL_RELEASE) ? 26 : -19), SPOOL_Y + 5, 28, 12, motion.has(Action.SPOOL_RELEASE));
        path.reset();
        path.moveTo(SPOOL_X + 6, SPOOL_Y - 19);
        path.quadTo((x + SPOOL_X) / 2 + 27, (y + SPOOL_Y) / 2, x - 25, y + 16);
        stroke(2, 0xffaf622e);
        c.drawPath(path, paint);
        sprite(c, R.drawable.fair_piece_kite, x, y, 113, -7 + sway);
        if (r.won()) {
            float unfurl = smooth(r.effectAge() / .77f);
            float from = lerp(x, 44, unfurl);
            line(c, from, 147 + 28 * unfurl, x - 18, y + 20, 1.7f, WOOD);
            for (int i = 0; i < 8; i++) {
                float p = i / 8f;
                pennant(c, lerp(from, x - 18, p), lerp(147 + 28 * unfurl, y + 20, p),
                        19 * unfurl, (float) Math.sin(i + r.effectAge() * 13) * 9, FLAGS[i % FLAGS.length]);
            }
        }
        focus(c, r, SPOOL_X, SPOOL_Y, 40, GOLD);
    }

    private void canal(Canvas c, PuzzleRound r) {
        FairRound motion = r.fair;
        color(0xc222303c);
        c.drawOval(91, CANAL_Y + 12, 155, CANAL_Y + 38, paint);
        stroke(2.2f, 0xff7daeb7);
        c.drawOval(92, CANAL_Y + 13, 154, CANAL_Y + 38, paint);
        for (int i = 0; i < 3; i++) {
            line(c, 102 + i * 13, CANAL_Y + 17, 121 + i * 9, CANAL_Y + 35, 2.6f, 0xff476876);
        }
        color(WOOD);
        c.drawRoundRect(DOCK_X + 13, 404, 400, 469, 5, 5, paint);
        for (int i = 0; i < 5; i++) {
            line(c, DOCK_X + 14, 408 + i * 12, 400, 408 + i * 12, 2.2f, BRASS);
        }
        line(c, DOCK_X + 14, 397, DOCK_X + 14, 473, 7.5f, 0xffc29062);

        float x = motion.boatX(r.time);
        float bob = (float) Math.sin(r.time * 13) * 4;
        float yaw = (float) Math.sin(r.time * 10) * 7;
        if (r.event == Outcome.BOAT_SPUN) {
            float spin = smooth(r.effectAge() / .7f);
            yaw += spin * 35;
            bob += 12 * arc(spin);
        }
        if (motion.has(Action.WATER_IMPACT)) ripple(c, motion.waveX(), CANAL_Y + 3, motion.age(r.time), ICE);
        glow(c, x, CANAL_Y - 25 + bob, 62);
        for (int i = 0; i < 8; i++) {
            int light = 116 - i * 13;
            float y = CANAL_Y + 14 + i * 6;
            float dx = (float) Math.sin(24 * r.time + i) * (2 + i * 1.4f);
            line(c, x + dx - (17 - i), y, x + dx + (17 - i), y, 1.5f, alpha(GOLD, light));
        }
        sprite(c, R.drawable.fair_piece_boat, x, CANAL_Y - 24 + bob, 100, yaw);
        if (!r.started) {
            art.focus(c, x - 59, CANAL_Y, 32, ICE);
            ripple(c, x - 59, CANAL_Y, .27f, ICE);
        }
    }

    private void echo(Canvas c, PuzzleRound r, float ambient) {
        FairRound motion = r.fair;
        float reveal = finaleProgress(r);
        if (reveal >= .57f) {
            float emerge = smooth((reveal - .57f) / .43f);
            if (emerge > 0) {
                glow(c, 203, 435, 74 * emerge);
                sprite(c, R.drawable.fair_piece_band, 195, 455 - 16 * emerge, 105 * emerge, 0);
                sprite(c, R.drawable.fair_piece_horse, 321, 401, 36 * emerge,
                        (float) Math.sin(r.effectAge() * 12) * 6);
                art.drawCat(c, false, 99, 535, 77 * emerge, ambient);
                festival(c, r.effectAge(), emerge);
            }
            return;
        }
        float gate = motion.position(r.time, .19f);
        line(c, GATE_X - 32, 219, GATE_X - 32, 402, 5, BRASS);
        line(c, GATE_X + 31, 219, GATE_X + 31, 402, 5, BRASS);
        line(c, GATE_X - 38, 213, GATE_X + 38, 213, 10, WOOD);
        float bottom = GATE_Y + 25 - gate * 104;
        for (int i = 0; i < 4; i++) {
            plate(c, GATE_X, bottom - 12 - i * 18, 62, 14, gate > .85f);
        }
        line(c, GATE_X, 218, GATE_X, bottom - 73, 2.3f, EDGE);
        sprite(c, R.drawable.fair_piece_lantern, ECHO_LOCK_X, ECHO_LOCK_Y - 68, 47, 2);
        plate(c, ECHO_LOCK_X, ECHO_LOCK_Y, 52, 22, r.won());
        if (motion.auxiliaryVisible()) {
            ball(c, motion.auxiliaryX(), motion.auxiliaryY(), MARBLE_RADIUS, 148);
            for (int i = 1; i <= 3; i++) {
                color(alpha(ICE, 87 - 19 * i));
                c.drawCircle(motion.auxiliaryX() - 9 * i, motion.auxiliaryY() - 2 * i, 8 - i, paint);
            }
        }
        focus(c, r, GATE_X, GATE_Y, 38, GOLD);
        focus(c, r, ECHO_LOCK_X, ECHO_LOCK_Y, 31, ICE);
    }

    static float finaleProgress(PuzzleRound round) {
        return round.event == Outcome.FAIR_OPEN ? smooth((round.effectAge() - .16f) / .92f) : 0;
    }

    private void festival(Canvas c, float time, float appear) {
        for (int i = 0; i < 28; i++) {
            float p = (time * .6f + i * .173f) % 1;
            float x = 31 + (i * 111 % 337) + (float) Math.sin(p * 13 + i) * 13;
            float y = 114 + (i * 59 % 321) - 38 * p;
            color(alpha(FLAGS[i % FLAGS.length], Math.round((1 - p) * 214 * appear)));
            c.save();
            c.rotate(p * 155, x, y);
            c.drawRoundRect(x - 2.3f, y - 3, x + 2.3f, y + 3, 1.5f, 1.5f, paint);
            c.restore();
        }
    }

    private void ball(Canvas c, float x, float y, float radius, int opacity) {
        c.save();
        c.translate(x, y);
        color(alpha(0xffffffff, opacity));
        paint.setShader(glass);
        c.drawCircle(0, 0, radius, paint);
        paint.setShader(null);
        stroke(1.4f, alpha(ICE, opacity));
        c.drawCircle(0, 0, radius, paint);
        c.restore();
    }

    private void door(Canvas c, float left, float top, float right, float bottom) {
        color(0xff522e36);
        c.drawRoundRect(left, top, right, bottom, 7, 7, paint);
        color(0xff774542);
        c.drawRoundRect(left + 5, top + 5, right - 4, bottom - 5, 5, 5, paint);
        stroke(1.9f, BRASS);
        c.drawRoundRect(left + 7, top + 7, right - 6, bottom - 7, 6, 6, paint);
    }

    private void pennant(Canvas c, float x, float y, float width, float wave, int tint) {
        path.reset();
        path.moveTo(x, y);
        path.lineTo(x + width, y + wave);
        path.lineTo(x + width * .29f, y + width * .54f + wave * .27f);
        path.close();
        color(tint);
        c.drawPath(path, paint);
    }

    private void ripple(Canvas c, float x, float y, float age, int tint) {
        for (int i = 0; i < 3; i++) {
            float p = (age - .12f * i) / .68f;
            if (p < 0 || p >= 1) continue;
            float rx = 7 + p * 101, ry = 2 + p * 28;
            stroke(2.2f - p, alpha(tint, (int) (181 * (1 - p))));
            c.drawOval(x - rx, y - ry, x + rx, y + ry, paint);
        }
    }

    private void glow(Canvas c, float x, float y, float radius) {
        if (radius <= 0) return;
        c.save();
        c.translate(x, y);
        c.scale(radius / 74, radius / 74);
        color(0xffffffff);
        paint.setShader(lanternGlow);
        c.drawCircle(0, 0, 74, paint);
        paint.setShader(null);
        c.restore();
    }

    private void focus(Canvas c, PuzzleRound r, float x, float y, float radius, int tint) {
        if (!r.started) art.focus(c, x, y, radius, tint);
    }

    private Bitmap piece(int id) {
        Bitmap image = pieces.get(id);
        if (image == null) {
            image = BitmapFactory.decodeResource(resources, id);
            pieces.put(id, image);
        }
        return image;
    }

    private void sprite(Canvas c, int id, float x, float y, float width, float angle) {
        art.sprite(c, piece(id), x, y, width, angle);
    }

    private void plate(Canvas c, float x, float y, float width, float height, boolean set) {
        float left = x - width / 2, top = y - height / 2;
        color(0xff503c34);
        c.drawRoundRect(left, top + 3, left + width, top + height + 4, 4, 4, paint);
        color(set ? 0xff35897d : BRASS);
        c.drawRoundRect(left, top, left + width, top + height, 4, 4, paint);
        line(c, left + 5, top + 2, left + width - 5, top + 2, 1.25f, set ? ICE : EDGE);
        color(0xff694b42);
        c.drawCircle(left + 6, y, 1.7f, paint);
        c.drawCircle(left + width - 6, y, 1.7f, paint);
    }

    private void line(Canvas c, float x, float y, float toX, float toY, float width, int tint) {
        stroke(width, tint);
        c.drawLine(x, y, toX, toY, paint);
    }

    private void color(int tint) {
        paint.setColor(tint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void stroke(float width, int tint) {
        paint.setColor(tint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    private static float arc(float time) { return (float) Math.sin(Math.PI * time); }

    private static Path dragonShape() {
        Path p = new Path();
        p.moveTo(-115, 10);
        p.cubicTo(-101, 29, -72, 34, -47, 22);
        p.cubicTo(-28, -1, -13, -13, 31, -3);
        p.cubicTo(47, -24, 52, -47, 65, -55);
        p.lineTo(66, -76);
        p.lineTo(83, -52);
        p.lineTo(105, -58);
        p.lineTo(101, -39);
        p.lineTo(121, -31);
        p.lineTo(117, -17);
        p.lineTo(81, -15);
        p.cubicTo(72, 17, 59, 38, 37, 47);
        p.lineTo(53, 64);
        p.lineTo(16, 64);
        p.lineTo(10, 47);
        p.lineTo(-27, 43);
        p.lineTo(-40, 62);
        p.lineTo(-73, 62);
        p.lineTo(-60, 46);
        p.cubicTo(-102, 49, -115, 27, -115, 10);
        p.close();
        return p;
    }

    private static Path wingShape() {
        Path p = new Path();
        p.moveTo(24, 11);
        p.cubicTo(-3, -75, -37, -112, -117, -109);
        p.cubicTo(-74, -80, -93, -53, -77, -24);
        p.cubicTo(-48, -39, -34, -24, -32, -5);
        p.cubicTo(-8, -12, 11, -3, 24, 11);
        p.close();
        return p;
    }
}
