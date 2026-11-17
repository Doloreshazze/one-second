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

import static com.doloreshazze.onesecond.PuzzleRound.*;
import static com.doloreshazze.onesecond.StageArt.*;
import static com.doloreshazze.onesecond.WorkshopRound.*;

final class WorkshopArt {
    private static final int BRASS = 0xffbf854e;
    private static final int GLEAM = 0xffffdeb3;
    private static final int WOOD = 0xff5a3226;
    private static final int DARK = 0xff211a22;
    private static final int[] NOTE_TONES = {ICE, GOLD, CORAL};
    private static final String[] NOTE_NUMBERS = {"1", "2", "3"};

    private final Resources resources;
    private final StageArt art;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path path = new Path();
    private final RectF board = new RectF(0, 0, WIDTH, HEIGHT);
    private final SparseArray<Bitmap> pieces = new SparseArray<>();
    private final Shader water = new RadialGradient(-2, -3, 14,
            0xffe1fffa, 0xff388aa9, Shader.TileMode.CLAMP);

    private Level backdropLevel;
    private Bitmap backdrop, dawn;

    WorkshopArt(Resources resources, StageArt art) {
        this.resources = resources;
        this.art = art;
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setTextAlign(Paint.Align.CENTER);
    }

    void background(Canvas canvas, PuzzleRound round) {
        if (backdropLevel != round.level) {
            backdropLevel = round.level;
            backdrop = BitmapFactory.decodeResource(resources, backgroundResource(round.level));
            dawn = null;
        }
        color(0xffffffff);
        canvas.drawBitmap(backdrop, null, board, paint);
        if (round.event == Outcome.CLOCK_STARTED) {
            if (dawn == null) dawn = BitmapFactory.decodeResource(resources, R.drawable.scene_clock_dawn);
            paint.setAlpha(Math.round(255 * smooth(round.effectAge() / .95f)));
            canvas.drawBitmap(dawn, null, board, paint);
            paint.setAlpha(255);
        }
    }

    private int backgroundResource(Level level) {
        switch (level) {
            case DROP: return R.drawable.scene_drop;
            case KEY: return R.drawable.scene_key;
            case GLIDER: return R.drawable.scene_glider;
            case TRAIN: return R.drawable.scene_train;
            case NOTES: return R.drawable.scene_notes;
            case PHOTO: return R.drawable.scene_photo;
            case CLOCK: return R.drawable.scene_clock;
            default: throw new AssertionError(level);
        }
    }

    void scene(Canvas canvas, PuzzleRound round, float ambient) {
        switch (round.level) {
            case DROP: drop(canvas, round); break;
            case KEY: key(canvas, round); break;
            case GLIDER: glider(canvas, round, ambient); break;
            case TRAIN: train(canvas, round); break;
            case NOTES: notes(canvas, round); break;
            case PHOTO: photo(canvas, round, ambient); break;
            case CLOCK: clock(canvas, round); break;
            default: throw new AssertionError(round.level);
        }
    }

    private void drop(Canvas canvas, PuzzleRound round) {
        WorkshopRound motion = round.workshop;
        float traySlide = motion.has(Action.TRAY) ? motion.progress(round.time, .28f) : 0;
        float saucerSlide = motion.has(Action.SAUCER) ? motion.progress(round.time, .26f) : 0;
        float paperX = lerp(DROP_X, 347, traySlide);
        float dishX = lerp(SAUCER_X, DROP_X, saucerSlide);
        float dishY = lerp(SAUCER_Y, DROP_SAUCER_Y + 10, saucerSlide);

        rail(canvas, 178, 484, 356, 484);
        art.shadow(canvas, paperX, 497, 57, 9);
        sprite(canvas, R.drawable.piece_tray, paperX - 3, 483, 135, 0);
        sprite(canvas, R.drawable.piece_paper, paperX, DROP_PAPER_Y, 105, -9);
        art.shelf(canvas, 324, 447, 76, true);
        hardware(canvas, TRAY_X - 17, TRAY_Y - 6, TRAY_X + 13, TRAY_Y + 6);
        line(canvas, TRAY_X + 13, TRAY_Y, paperX - 50, 480, 2.3f, BRASS);
        rail(canvas, dishX - 38, dishY + 9, dishX + 38, dishY + 9);
        sprite(canvas, R.drawable.piece_saucer, dishX, dishY, 91, 0);

        if (round.event == null) {
            float y = motion.dropY(round.time);
            canvas.save();
            canvas.translate(DROP_X, y);
            color(0xffffffff);
            path.reset();
            path.moveTo(0, -16);
            path.cubicTo(3, -7, 11, -1, 10, 6);
            path.cubicTo(9, 18, -10, 18, -10, 6);
            path.cubicTo(-10, 0, -4, -7, 0, -16);
            paint.setShader(water);
            canvas.drawPath(path, paint);
            paint.setShader(null);
            color(0xc8f4fff6);
            canvas.drawOval(-5, -3, -1, 5, paint);
            canvas.restore();
        } else if (round.event == Outcome.INK_BLOT) {
            inkCat(canvas, paperX, DROP_PAPER_Y, round.effectAge());
        } else {
            splash(canvas, DROP_X, round.eventY, round.effectAge(), ICE);
        }
        targets(canvas, round, SAUCER_X, SAUCER_Y, 45, TRAY_X, TRAY_Y, 26);
    }

    private void inkCat(Canvas canvas, float x, float y, float age) {
        float spread = smooth(age / .40f);
        canvas.save();
        canvas.translate(x - 4, y + 1);
        canvas.rotate(-10);
        canvas.scale(.18f + spread * .82f, .18f + spread * .82f);
        color(0xff3b254e);
        canvas.drawOval(-27, -17, 24, 18, paint);
        path.reset();
        path.moveTo(-25, -6);
        path.lineTo(-22, -28);
        path.lineTo(-6, -12);
        path.lineTo(8, -12);
        path.lineTo(23, -26);
        path.lineTo(24, -3);
        path.close();
        canvas.drawPath(path, paint);
        canvas.drawCircle(31, 14, 4, paint);
        canvas.drawCircle(-25, 25, 2.4f, paint);
        color(0xffc0a2c6);
        canvas.drawOval(-11, -4, -6, 1, paint);
        canvas.drawOval(6, -4, 11, 1, paint);
        canvas.restore();
    }

    private void key(Canvas canvas, PuzzleRound round) {
        WorkshopRound motion = round.workshop;
        color(0xff482b1f);
        canvas.drawRoundRect(LEG_X - 16, 40, LEG_X + 15, LEG_BOTTOM - 7, 10, 10, paint);
        color(0xff6f4833);
        canvas.drawRoundRect(LEG_X - 12, 51, LEG_X + 10, LEG_BOTTOM - 12, 6, 6, paint);
        for (int i = 0; i < 3; i++) {
            line(canvas, LEG_X - 10 + i * 6, 110, LEG_X - 9 + i * 6, LEG_BOTTOM - 19,
                    1.6f, 0x468c5b3e);
        }
        color(WOOD);
        canvas.drawRoundRect(LEG_X - 23, LEG_BOTTOM - 16, LEG_X + 21, LEG_BOTTOM, 6, 6, paint);
        color(0xff614961);
        canvas.drawOval(212, 479, 315, 534, paint);
        color(0xff91718a);
        canvas.drawOval(217, 483, 309, 528, paint);
        line(canvas, 288, 584, 390, 465, 15, 0xf0211717);
        line(canvas, 279, 589, 382, 463, 2.5f, 0xafbb7b4b);
        canvas.save();
        canvas.rotate(15, PLATE_X, PLATE_Y);
        hardware(canvas, PLATE_X - 42, PLATE_Y - 7, PLATE_X + 42, PLATE_Y + 7);
        canvas.restore();

        float x = motion.keyX(round.time), y = motion.keyY(round.time);
        float angle = -15 + 94 * round.time;
        if (round.event == Outcome.KEY_SAVED) {
            float settle = smooth(round.effectAge() / .50f);
            x = lerp(round.eventX, 265, settle);
            y = lerp(round.eventY, 501, settle) - 17 * arc(settle);
            angle = lerp(20, -17, settle);
        } else if (round.event == Outcome.KEY_LOST) {
            float pull = smooth(round.effectAge() / .40f);
            sprite(canvas, R.drawable.piece_mouse, 377 - 29 * arc(pull), 527, 81, 9);
            if (pull > .89f) return;
            x = 325 + pull * 48;
            y = 525 + pull * 38;
        }
        sprite(canvas, R.drawable.piece_key, x, y, 68, angle);
        targets(canvas, round, PLATE_X, PLATE_Y, 41, x, y, 35);
    }

    private void glider(Canvas canvas, PuzzleRound round, float ambient) {
        WorkshopRound motion = round.workshop;
        art.shelf(canvas, 293, 286, 91, false);
        art.shadow(canvas, CUP_X, CUP_Y + 43, 62, 10);
        art.anchoredSprite(canvas, piece(R.drawable.piece_cup), CUP_X, CUP_Y + 31, 109);
        art.anchoredSprite(canvas, piece(R.drawable.piece_fan), 149, 474, 123);

        float opened = motion.progress(round.time, .20f);
        canvas.save();
        canvas.rotate(-34 * opened, FAN_X, FAN_Y);
        hardware(canvas, FAN_X - 24, FAN_Y - 8, FAN_X + 24, FAN_Y + 8);
        canvas.restore();
        path.reset();
        for (int i = 0; i < 4; i++) {
            float flow = (ambient * .93f + i * .25f) % 1;
            float from = FAN_X + 29 + flow * 53;
            float y = FAN_Y + (i - 1.5f) * 13 - opened * flow * 59;
            path.moveTo(from, y);
            path.quadTo(from + 26, y - 10 - 16 * opened, from + 54, y - 3 - 28 * opened);
        }
        stroke(1.7f, alpha(ICE, 50 + (int) (78 * opened)));
        canvas.drawPath(path, paint);

        float angle = motion.guidesPlane() ? 18 - 31 * opened : 20 + 21 * round.time;
        if (round.event != Outcome.GLIDER_WET || round.effectAge() < .21f) {
            sprite(canvas, R.drawable.piece_plane, motion.gliderX(round.time),
                    motion.gliderY(round.time), 101, angle);
        }
        if (round.event == Outcome.GLIDER_WET) splash(canvas, CUP_X, CUP_Y - 20, round.effectAge(), GOLD);
        targets(canvas, round, FAN_X, FAN_Y, 31, motion.gliderX(round.time), motion.gliderY(round.time), 43);
    }

    private void train(Canvas canvas, PuzzleRound round) {
        WorkshopRound motion = round.workshop;
        drawTrack(canvas, 0, BRIDGE_X);
        drawTrack(canvas, 314, 405);
        canvas.save();
        canvas.rotate(-42 * (1 - motion.progress(round.time, .28f)), BRIDGE_X, RAIL_Y);
        drawTrack(canvas, BRIDGE_X, 314);
        canvas.restore();
        line(canvas, BRIDGE_X, RAIL_Y + 3, BRIDGE_X, 517, 9, WOOD);
        line(canvas, 315, RAIL_Y + 3, 315, 517, 8, WOOD);
        if (!motion.has(Action.BRIDGE)) hardware(canvas, WEDGE_X - 13, WEDGE_Y - 7, WEDGE_X + 13, WEDGE_Y + 8);

        float x = 68 + 140 * Math.min(round.time, .94f);
        if (round.event == Outcome.TRAIN_CROSSED) x += 133 * smooth(round.effectAge() / .68f);
        float tumble = round.event == Outcome.TRAIN_DERAILED ? smooth(round.effectAge() / .53f) : 0;
        float y = RAIL_Y - 6 + 49 * tumble;
        color(0xffe0cfb6);
        canvas.drawOval(236, 511, 310, 545, paint);
        canvas.drawOval(251, 504, 344, 542, paint);
        canvas.drawOval(303, 513, 370, 542, paint);
        float wagonX = x - 65 + 16 * tumble;
        art.anchoredSprite(canvas, piece(R.drawable.piece_carriage), wagonX - 64, RAIL_Y - 5 + 10 * tumble, 64);
        art.anchoredSprite(canvas, piece(R.drawable.piece_carriage), wagonX, RAIL_Y - 5 + 23 * tumble, 65);
        sprite(canvas, R.drawable.piece_train, x + 47 * tumble, y - 23, 76, 41 * tumble);
        if (tumble > 0) {
            sprite(canvas, R.drawable.piece_duck, 192 + 110 * tumble, 449 + 75 * tumble - 30 * arc(tumble),
                    39, 320 * tumble);
        } else {
            for (int i = 0; i < 3; i++) {
                float rise = (round.time * 1.6f + i * .32f) % 1;
                color(alpha(0xffe7dcc8, (int) (80 * (1 - rise))));
                canvas.drawCircle(x + 15 - 23 * rise, y - 54 - 32 * rise, 4 + 8 * rise, paint);
            }
        }
        if (!round.started) art.focus(canvas, WEDGE_X, WEDGE_Y, 26, GOLD);
    }

    private void drawTrack(Canvas canvas, float from, float to) {
        color(0xff3b2a29);
        canvas.drawRect(from, RAIL_Y - 1, to, RAIL_Y + 14, paint);
        for (float x = from + 2; x + 5 < to; x += 16) {
            color(0xff936048);
            canvas.drawRoundRect(x, RAIL_Y - 7, x + 8, RAIL_Y + 13, 1.5f, 1.5f, paint);
        }
        line(canvas, from, RAIL_Y - 3, to, RAIL_Y - 3, 3.2f, 0xffe4b87c);
        line(canvas, from, RAIL_Y + 7, to, RAIL_Y + 7, 3.2f, 0xffb7855d);
    }

    private void notes(Canvas canvas, PuzzleRound round) {
        WorkshopRound motion = round.workshop;
        rail(canvas, 107, 157, 348, 157);
        for (int i = 0; i < NOTE_X.length; i++) {
            float x = NOTE_X[i], y = NOTE_Y[i];
            float age = motion.noteAge(i, round.time);
            float swing = age < 0 ? 0 : (float) Math.sin(age * 23) * 13 * (1 - clamp(age / .44f));
            line(canvas, x - 11, 158, x - 8, y - 16, 1.5f, 0xffac865f);
            line(canvas, x + 11, 158, x + 8, y - 16, 1.5f, 0xffd0a87a);
            sprite(canvas, R.drawable.piece_chime, x, y, 54, swing);
            if (age >= 0) splash(canvas, x, y, age, NOTE_TONES[i]);
            color(age >= 0 ? NOTE_TONES[i] : 0xff41343f);
            canvas.drawCircle(x, y + 36, 11, paint);
            color(age >= 0 ? DARK : NOTE_TONES[i]);
            paint.setTextSize(13);
            canvas.drawText(NOTE_NUMBERS[i], x, y + 41, paint);
        }
        float open = round.event == Outcome.NOTES_PLAYED ? smooth(round.effectAge() / .49f) : 0;
        art.shadow(canvas, 267, 535, 91, 10);
        color(0xff174046);
        canvas.drawRoundRect(196, 484, 337, 531, 11, 11, paint);
        color(0xffb78b5e);
        canvas.drawRoundRect(199, 525, 334, 532, 4, 4, paint);
        if (open > 0) {
            float turn = (float) Math.cos(round.effectAge() * 8);
            canvas.save();
            canvas.scale(.45f + .55f * Math.abs(turn), 1, 268, 477);
            art.anchoredSprite(canvas, piece(R.drawable.piece_dancer), 268, 513 - 42 * open, 65);
            canvas.restore();
        }
        sprite(canvas, R.drawable.piece_music_box, 267, 488 + 21 * open, 143, 3 * open);
        if (!round.started) art.focus(canvas, NOTE_X[0], NOTE_Y[0], 30, ICE);
    }

    private void photo(Canvas canvas, PuzzleRound round, float ambient) {
        WorkshopRound motion = round.workshop;
        boolean attention = motion.has(Action.BELL) && motion.actionAge(round.time) >= .13f
                && motion.actionAge(round.time) <= .51f;
        if (round.event != null) attention = round.won();
        art.shadow(canvas, 123, 542, 76, 12);
        pose(canvas, attention, 126, 532, 128, ambient);
        art.anchoredSprite(canvas, piece(R.drawable.piece_camera), 330, 535, 118);
        hardware(canvas, PHOTO_BELL_X - 27, PHOTO_BELL_Y + 21, PHOTO_BELL_X + 27, PHOTO_BELL_Y + 28);
        float age = motion.actionAge(round.time);
        art.drawBell(canvas, PHOTO_BELL_X, PHOTO_BELL_Y, 55, motion.has(Action.BELL)
                ? (float) Math.sin(age * 40) * 18 * (1 - clamp(age / .34f)) : 0);

        float shutter = clamp(round.time / SHUTTER_TIME);
        color(0xe32f232e);
        canvas.drawCircle(333, 366, 13, paint);
        stroke(2.1f, GOLD);
        canvas.drawArc(323, 356, 343, 376, -90, 28 + shutter * 332, false, paint);
        if (!round.started) art.focus(canvas, PHOTO_BELL_X, PHOTO_BELL_Y, 37, GOLD);
        if (round.event != null) {
            print(canvas, round.won(), round.effectAge(), ambient);
            float flash = 1 - clamp(round.effectAge() / .19f);
            color(alpha(0xfffff5d3, (int) (193 * flash)));
            canvas.drawRect(board, paint);
        }
    }

    private void pose(Canvas canvas, boolean attentive, float x, float y, float width, float ambient) {
        canvas.save();
        if (!attentive) canvas.scale(-.91f, 1, x, y);
        art.drawCat(canvas, true, x, y, width, ambient);
        canvas.restore();
    }

    private void print(Canvas canvas, boolean success, float age, float ambient) {
        float develop = smooth(age / .49f);
        if (develop <= 0) return;
        canvas.save();
        canvas.translate(36 * (1 - develop), 75 * (1 - develop));
        canvas.rotate(-10 + 18 * (1 - develop), 172, 239);
        canvas.scale(.66f + .34f * develop, .66f + .34f * develop, 172, 239);
        color(0x9a2c1d29);
        canvas.drawRoundRect(79, 157, 269, 331, 8, 8, paint);
        color(0xfff5e4ca);
        canvas.drawRoundRect(74, 151, 262, 324, 6, 6, paint);
        color(success ? 0xff775062 : 0xff4e3e59);
        canvas.drawRect(85, 161, 251, 297, paint);
        canvas.save();
        canvas.clipRect(85, 161, 251, 297);
        color(0xffa67662);
        canvas.drawCircle(117, 188, 21, paint);
        color(0xff5c3a49);
        canvas.drawRect(84, 273, 253, 296, paint);
        pose(canvas, success, success ? 169 : 219, 285, 134, ambient);
        color(alpha(0xfffff0ce, (int) (153 * (1 - develop))));
        canvas.drawRect(85, 161, 251, 297, paint);
        canvas.restore();
        canvas.restore();
    }

    private void clock(Canvas canvas, PuzzleRound round) {
        WorkshopRound motion = round.workshop;
        float rock = motion.has(Action.ROCKER) ? motion.progress(round.time, .34f) : 0;
        float weightY = Math.min(441, 170 + round.time * round.time * 258);
        if (round.event == Outcome.CLOCK_STARTED) weightY -= 25 * smooth(round.effectAge() / .5f);
        line(canvas, WEIGHT_X, 86, WEIGHT_X, weightY - 18, 2, 0xff957057);
        sprite(canvas, R.drawable.piece_weight, WEIGHT_X, weightY, 41, -6 + 6 * rock);

        float swing = rock * (float) Math.sin(7.4f * motion.actionAge(round.time)) * 27;
        canvas.save();
        canvas.rotate(swing, 301, 194);
        line(canvas, 301, 194, 301, 404, 5, BRASS);
        line(canvas, 300, 197, 300, 394, 1.4f, GLEAM);
        sprite(canvas, R.drawable.piece_bob, 301, 402, 65, 0);
        canvas.restore();
        hardware(canvas, 279, 181, 323, 202);

        color(WOOD);
        canvas.drawRoundRect(207, 435, 230, 493, 3, 3, paint);
        canvas.save();
        canvas.rotate(-15 + 20 * rock, 218, 428);
        hardware(canvas, 165, ROCKER_Y - 7, 270, ROCKER_Y + 9);
        canvas.restore();
        if (!round.started) art.focus(canvas, ROCKER_X, ROCKER_Y, 39, GOLD);

        if (round.event == Outcome.CLOCK_JAMMED) {
            splash(canvas, WEIGHT_X, 448, round.effectAge(), CORAL);
        } else if (round.event == Outcome.CLOCK_STARTED) {
            for (int i = 0; i < 2; i++) {
                float pulse = clamp((round.effectAge() - .22f * i) / .73f);
                stroke(1.6f, alpha(GOLD, (int) ((1 - pulse) * 105)));
                canvas.drawCircle(290, 394, 49 + pulse * 183, paint);
            }
        }
    }

    private void targets(Canvas c, PuzzleRound round, float x, float y, float radius,
                         float otherX, float otherY, float otherRadius) {
        if (round.started) return;
        art.focus(c, x, y, radius, GOLD);
        art.focus(c, otherX, otherY, otherRadius, ICE);
    }

    private void splash(Canvas canvas, float x, float y, float age, int tint) {
        float p = clamp(age / .55f);
        if (p >= 1) return;
        stroke(1.4f, alpha(tint, (int) ((1 - p) * 155)));
        canvas.drawOval(x - 6 - 46 * p, y - 2 - 9 * p, x + 6 + 46 * p, y + 2 + 9 * p, paint);
        for (int i = 0; i < 9; i++) {
            float dx = (i - 4) * (3.2f + 7 * p);
            float dy = -arc(p) * (13 + i * 11 % 30);
            color(alpha(tint, (int) ((1 - p) * 220)));
            canvas.drawCircle(x + dx, y + dy, 1.25f + i % 3, paint);
        }
    }

    private static float arc(float position) {
        return (float) Math.sin(position * Math.PI);
    }

    private Bitmap piece(int resource) {
        Bitmap image = pieces.get(resource);
        if (image == null) {
            image = BitmapFactory.decodeResource(resources, resource);
            pieces.put(resource, image);
        }
        return image;
    }

    private void sprite(Canvas canvas, int resource, float x, float y, float width, float angle) {
        art.sprite(canvas, piece(resource), x, y, width, angle);
    }

    private void rail(Canvas canvas, float x1, float y1, float x2, float y2) {
        line(canvas, x1, y1 + 3, x2, y2 + 3, 6, 0xff432927);
        line(canvas, x1, y1, x2, y2, 3, 0xffac7854);
    }

    private void hardware(Canvas canvas, float left, float top, float right, float bottom) {
        color(0xff5a3932);
        canvas.drawRoundRect(left, top + 3, right, bottom + 4, 4, 4, paint);
        color(BRASS);
        canvas.drawRoundRect(left, top, right, bottom, 4, 4, paint);
        line(canvas, left + 4, top + 2, right - 4, top + 2, 1.4f, GLEAM);
        color(0xff734d38);
        canvas.drawCircle(left + 5, (top + bottom) * .5f, 1.8f, paint);
        canvas.drawCircle(right - 5, (top + bottom) * .5f, 1.8f, paint);
    }

    private void line(Canvas canvas, float x1, float y1, float x2, float y2, float width, int tint) {
        stroke(width, tint);
        canvas.drawLine(x1, y1, x2, y2, paint);
    }

    private void stroke(float width, int tint) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(tint);
    }

    private void color(int tint) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(tint);
    }
}
