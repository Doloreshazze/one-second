package com.doloreshazze.onesecond;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import static com.doloreshazze.onesecond.PuzzleRound.clamp;

final class LivingArt {
    private static final int GRID = 14;
    private final float[] vertices = new float[2 * (GRID + 1) * (GRID + 1)];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    LivingArt() {
        paint.setTypeface(Typeface.create("serif", Typeface.BOLD_ITALIC));
    }

    void sleep(Canvas canvas, Bitmap cat, float x, float bottom, float width, float time) {
        float height = width * cat.getHeight() / cat.getWidth();
        float breath = .5f + .5f * wave(time / 3.75f);
        float twitchPhase = clamp((time % 8.2f - 2.4f) / .38f);
        float twitch = (float) Math.sin(Math.PI * twitchPhase) * wave(twitchPhase * 1.4f);
        int index = 0;
        for (int row = 0; row <= GRID; row++) {
            float v = row / (float) GRID;
            for (int column = 0; column <= GRID; column++) {
                float u = column / (float) GRID;
                float chest = weight(u, v, .68f, .56f, .58f, .54f);
                float ear = weight(u, v, .48f, .18f, .25f, .33f);
                vertices[index++] = x + width * (u - .5f + .016f * breath * chest + .022f * twitch * ear);
                vertices[index++] = bottom + height * (v - 1 - .055f * breath * chest - .045f * twitch * ear);
            }
        }
        paint.setColor(0xffffffff);
        canvas.drawBitmapMesh(cat, GRID, GRID, vertices, 0, null, 0, paint);

        for (int i = 0; i < 2; i++) {
            float floatAway = (time / 3.2f + .42f * i) % 1;
            float opacity = (float) Math.sin(Math.PI * floatAway);
            paint.setColor(StageArt.alpha(StageArt.ICE, Math.round(opacity * 127)));
            paint.setTextSize(8 + 8 * floatAway);
            canvas.drawText("z", x - width * .30f - 11 * floatAway,
                    bottom - height * .77f - 31 * floatAway, paint);
        }
    }

    void aquarium(Canvas canvas, Bitmap bowl, float x, float y, float width, float angle, float time) {
        float height = width * bowl.getHeight() / bowl.getWidth();
        float fan = wave(time * 1.7f);
        float paddle = wave(time * 1.7f - .23f);
        int index = 0;
        for (int row = 0; row <= GRID; row++) {
            float v = row / (float) GRID;
            for (int column = 0; column <= GRID; column++) {
                float u = column / (float) GRID;
                float tail = weight(u, v, .78f, .65f, .19f, .21f);
                float fins = weight(u, v, .59f, .72f, .14f, .22f);
                vertices[index++] = x + width * (u - .5f + tail * fan * .032f);
                vertices[index++] = y + height * (v - .5f + tail * fan * .041f + fins * paddle * .028f);
            }
        }
        paint.setColor(0xffffffff);
        canvas.save();
        canvas.rotate(angle, x, y);
        canvas.drawBitmapMesh(bowl, GRID, GRID, vertices, 0, null, 0, paint);
        canvas.restore();
    }

    private static float weight(float u, float v, float x, float y, float rx, float ry) {
        float dx = (u - x) / rx;
        float dy = (v - y) / ry;
        float influence = clamp(1 - dx * dx - dy * dy);
        return influence * influence;
    }

    private static float wave(float phase) {
        return (float) Math.sin(2 * Math.PI * phase);
    }
}
