package com.doloreshazze.onesecond;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

import static com.doloreshazze.onesecond.PuzzleRound.*;

final class StageArt {
    static final int ICE = 0xffa8f4ee;
    static final int GOLD = 0xffffce86;
    static final int CORAL = 0xffff9a85;
    private static final float THAW_DURATION = .34f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF bounds = new RectF(0, 0, WIDTH, HEIGHT);
    private final RectF destination = new RectF();
    private final Path shape = new Path();
    private final Shader windowShade = new LinearGradient(0, 0, 0, 170,
            0x79081015, 0x00081015, Shader.TileMode.CLAMP);

    private final Bitmap day, night, marble, vase, cushion, catAsleep, catAwake, bell, bowl, fish, shards;
    private final LivingArt living = new LivingArt();
    private final WorkshopArt workshop;

    StageArt(Resources res) {
        day = BitmapFactory.decodeResource(res, R.drawable.stage_day);
        night = BitmapFactory.decodeResource(res, R.drawable.stage_night);
        marble = BitmapFactory.decodeResource(res, R.drawable.prop_marble);
        vase = BitmapFactory.decodeResource(res, R.drawable.prop_vase);
        cushion = BitmapFactory.decodeResource(res, R.drawable.prop_cushion);
        catAsleep = BitmapFactory.decodeResource(res, R.drawable.prop_cat_sleep);
        catAwake = BitmapFactory.decodeResource(res, R.drawable.prop_cat_awake);
        bell = BitmapFactory.decodeResource(res, R.drawable.prop_bell);
        bowl = BitmapFactory.decodeResource(res, R.drawable.prop_bowl);
        fish = BitmapFactory.decodeResource(res, R.drawable.prop_goldfish);
        shards = BitmapFactory.decodeResource(res, R.drawable.prop_shards);
        workshop = new WorkshopArt(res, this);
    }

    void drawRoom(Canvas canvas, PuzzleRound round, float ambient) {
        color(0xffffffff);
        if (round.workshop == null) {
            canvas.drawBitmap(round.level == Level.VASE ? day : night, null, bounds, paint);
        } else {
            workshop.background(canvas, round);
        }
        paint.setShader(windowShade);
        canvas.drawRect(bounds, paint);
        paint.setShader(null);
        float thaw = clamp(round.time / THAW_DURATION);
        if (thaw < 1) {
            color(alpha(0xff13294c, Math.round(62 * (1 - thaw))));
            canvas.drawRect(bounds, paint);
        }
        if (round.started && thaw < 1) {
            float expansion = 1 - (1 - thaw) * (1 - thaw);
            float radius = MARBLE_RADIUS + 96 * expansion;
            color(alpha(ICE, Math.round(135 * (1 - thaw))));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.8f);
            canvas.drawCircle(round.marbleX, round.marbleY, radius, paint);
            color(alpha(GOLD, Math.round(100 * (1 - thaw))));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.2f);
            canvas.drawCircle(round.marbleX, round.marbleY, radius + 10, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        for (int i = 0; i < 26; i++) {
            float x = 22 + (i * 83 % 356) + round.time * (i % 2 == 0 ? 11 : -10);
            float y = 104 + (i * 67 % 404) - round.time * (5 + i % 8);
            color(alpha(round.level == Level.VASE ? GOLD : ICE, 28 + i * 9 % 55));
            canvas.drawCircle(x, y, .7f + (i % 4) * .45f, paint);
        }

        if (round.level == Level.VASE) drawVaseScene(canvas, round, ambient);
        else if (round.level == Level.CAT) drawCatScene(canvas, round, ambient);
        else workshop.scene(canvas, round, ambient);
    }

    private void drawVaseScene(Canvas canvas, PuzzleRound round, float ambient) {
        shelf(canvas, 344, 293, 68, false);
        shadow(canvas, 99, 534, 63, 13);
        drawCat(canvas, false, 99, 532, 118, ambient);
        shadow(canvas, round.cushionX(), 539, 73, 13);
        float squash = round.event == Outcome.VASE_SAVED
                ? (float) Math.sin(clamp(round.effectAge() / .24f) * Math.PI) * 9 : 0;
        sprite(canvas, cushion, round.cushionX(), CUSHION_Y + squash * .5f, 123 + squash, 53 - squash, 0);

        if (round.event == Outcome.VASE_BROKEN) {
            shadow(canvas, VASE_X, 538, 46, 10);
            float hop = (float) Math.sin(clamp(round.effectAge() / .3f) * Math.PI) * 12;
            sprite(canvas, shards, VASE_X, FLOOR - 23 - hop, 71, 51, -12);
        } else {
            if (!round.started) focus(canvas, VASE_X, round.vaseY(), 49, GOLD);
            sprite(canvas, vase, VASE_X, round.vaseY(), 80, round.vaseAngle());
        }
    }

    private void drawCatScene(Canvas canvas, PuzzleRound round, float ambient) {
        shelf(canvas, 338, 402, 75, true);
        shadow(canvas, 96, 538, 72, 18);
        color(0xff43305c);
        canvas.drawOval(26, 505, 162, 548, paint);
        color(0x85765a84);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f);
        canvas.drawOval(29, 508, 159, 544, paint);
        paint.setStyle(Paint.Style.FILL);

        boolean startled = round.event == Outcome.CAT_AWAKE;
        drawCat(canvas, startled, 96, 532, startled ? 126 : 122, ambient);
        book(canvas, 166, 469, 86, 15, 0xff56385d);
        book(canvas, 173, 455, 69, 15, 0xff845664);
        float shake = startled
                ? (float) Math.sin(round.effectAge() * 96) * (1 - clamp(round.effectAge() / .25f)) * 19
                : 0;
        if (!round.started) focus(canvas, BELL_X, BELL_Y, 35, CORAL);
        sprite(canvas, bell, BELL_X, BELL_Y, 60, shake);

        if (round.event == Outcome.FISH_FELL) {
            color(0x874ad6d2);
            canvas.drawOval(245, 521, 377, 545, paint);
            sprite(canvas, bowl, round.bowlX(), FLOOR - 27, 72, -79);
            sprite(canvas, fish, 269, FLOOR - 16, 38, -11);
        } else {
            if (!round.started) focus(canvas, round.bowlX(), round.bowlY(), 47, ICE);
            living.aquarium(canvas, bowl, round.bowlX(), round.bowlY(), 78, round.bowlAngle(), ambient);
        }
    }

    void drawCat(Canvas canvas, boolean awake, float x, float bottom, float width, float ambient) {
        if (awake) anchoredSprite(canvas, catAwake, x, bottom, width);
        else living.sleep(canvas, catAsleep, x, bottom, width, ambient);
    }

    void drawBell(Canvas canvas, float x, float y, float width, float angle) {
        sprite(canvas, bell, x, y, width, angle);
    }

    void drawMarble(Canvas canvas, PuzzleRound round, float pulse) {
        if (!round.started) {
            float radius = MARBLE_RADIUS + 6 + pulse * 14;
            color(alpha(ICE, (int) (135 * (1 - pulse))));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.6f);
            canvas.drawCircle(round.marbleX, round.marbleY, radius, paint);
            color(0xaaaffff3);
            canvas.drawCircle(round.marbleX, round.marbleY, MARBLE_RADIUS + 5, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        shadow(canvas, round.marbleX + 2, round.marbleY + 12, 24, 15);
        float turn = round.started ? (round.marbleX - round.level.startX) * 1.3f : 0;
        sprite(canvas, marble, round.marbleX, round.marbleY, MARBLE_RADIUS * 2.1f, turn);
    }

    void drawImpact(Canvas canvas, PuzzleRound round) {
        if (round.event == null) return;
        float progress = clamp(round.effectAge() / EFFECT_DURATION);
        if (progress >= 1) return;
        int tone = round.won() ? GOLD : CORAL;
        if (round.event == Outcome.CAT_AWAKE) {
            color(alpha(tone, (int) ((1 - progress) * 170)));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.2f);
            canvas.drawCircle(round.eventX, round.eventY, 20 + progress * 86, paint);
            canvas.drawCircle(round.eventX, round.eventY, 34 + progress * 100, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        for (int i = 0; i < 19; i++) {
            double angle = i * 2.39996;
            float dist = 12 + progress * (34 + i * 29 % 83);
            float x = round.eventX + (float) Math.cos(angle) * dist;
            float y = round.eventY + (float) Math.sin(angle) * dist - progress * 20;
            color(alpha(i % 5 == 0 ? ICE : tone, (int) ((1 - progress) * 238)));
            if (i % 3 == 0) diamond(canvas, x, y, 2.7f + i % 4);
            else canvas.drawCircle(x, y, 1.6f + i % 4, paint);
        }
    }

    void focus(Canvas canvas, float x, float y, float radius, int tint) {
        color(alpha(tint, 144));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.25f);
        canvas.drawArc(x - radius, y - radius, x + radius, y + radius, 204, 44, false, paint);
        canvas.drawArc(x - radius, y - radius, x + radius, y + radius, 25, 41, false, paint);
        paint.setStyle(Paint.Style.FILL);
        diamond(canvas, x + radius * .78f, y - radius * .71f, 3.7f);
    }

    void shelf(Canvas canvas, float x, float y, float width, boolean dark) {
        color(dark ? 0xff32263a : 0xff3a2820);
        shape.reset();
        shape.moveTo(x + 17, y + 10);
        shape.lineTo(x + 56, y + 10);
        shape.lineTo(x + 56, y + 43);
        shape.close();
        canvas.drawPath(shape, paint);
        color(dark ? 0xff594060 : 0xff6f4837);
        canvas.drawRoundRect(x, y, x + width, y + 12, 5, 5, paint);
        color(dark ? 0xffa7809d : 0xffc68d61);
        canvas.drawRoundRect(x + 1, y - 4, x + width, y + 3, 4, 4, paint);
        color(0x60ffda9c);
        paint.setStrokeWidth(1);
        canvas.drawLine(x + 7, y + 6, x + width - 4, y + 6, paint);
    }

    private void book(Canvas canvas, float x, float y, float width, float height, int tint) {
        color(tint);
        canvas.drawRoundRect(x, y, x + width, y + height, 4, 4, paint);
        color(0xffc8ae92);
        canvas.drawRoundRect(x + 8, y + 3, x + width - 3, y + height - 4, 2, 2, paint);
        color(0x88492d37);
        paint.setStrokeWidth(1);
        canvas.drawLine(x + 9, y + 7, x + width - 5, y + 7, paint);
    }

    void shadow(Canvas canvas, float x, float y, float rx, float ry) {
        color(0x31060610);
        canvas.drawOval(x - rx, y - ry, x + rx, y + ry, paint);
        color(0x42080611);
        canvas.drawOval(x - rx * .82f, y - ry * .7f, x + rx * .82f, y + ry * .7f, paint);
    }

    void anchoredSprite(Canvas canvas, Bitmap bitmap, float x, float bottom, float width) {
        float height = width * bitmap.getHeight() / bitmap.getWidth();
        sprite(canvas, bitmap, x, bottom - height / 2, width, height, 0);
    }

    void sprite(Canvas canvas, Bitmap bitmap, float x, float y, float width, float angle) {
        sprite(canvas, bitmap, x, y, width, width * bitmap.getHeight() / bitmap.getWidth(), angle);
    }

    private void sprite(Canvas canvas, Bitmap bitmap, float x, float y, float width, float height, float angle) {
        destination.set(x - width / 2, y - height / 2, x + width / 2, y + height / 2);
        color(0xffffffff);
        canvas.save();
        canvas.rotate(angle, x, y);
        canvas.drawBitmap(bitmap, null, destination, paint);
        canvas.restore();
    }

    private void diamond(Canvas canvas, float x, float y, float radius) {
        shape.reset();
        shape.moveTo(x, y - radius * 1.5f);
        shape.lineTo(x + radius, y);
        shape.lineTo(x, y + radius * 1.5f);
        shape.lineTo(x - radius, y);
        shape.close();
        canvas.drawPath(shape, paint);
    }

    private void color(int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
    }

    static int alpha(int color, int alpha) {
        return (color & 0x00ffffff) | (alpha << 24);
    }
}
