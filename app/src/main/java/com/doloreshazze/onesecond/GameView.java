package com.doloreshazze.onesecond;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import static com.doloreshazze.onesecond.PuzzleRound.*;

final class GameView extends View {
    private static final Level[] LEVELS = Level.values();
    private static final int INK = 0xff151922;
    private static final int PAPER = 0xffffeed9;
    private static final int MUTED = 0xffc5b6b4;
    private static final Typeface TITLE = Typeface.create("sans-serif-black", Typeface.NORMAL);
    private static final Typeface LABEL = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private static final float FRAME_WIDTH = 434;
    private static final float FRAME_HEIGHT = 802;
    private static final float BOARD_X = 17;
    private static final float BOARD_Y = 109;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF board = new RectF(BOARD_X, BOARD_Y, BOARD_X + WIDTH, BOARD_Y + HEIGHT);
    private final RectF action = new RectF(299, 726, 414, 780);
    private final RectF levelMenu = new RectF(333, 20, 417, 62);
    private final Path boardClip = new Path();
    private final Shader frameLight = new RadialGradient(209, 429, 466,
            0xff34333d, INK, Shader.TileMode.CLAMP);
    private final StageArt art;
    private final SharedPreferences progress;
    private final float touchSlop;
    private final float[] aimPoints = new float[24];
    private final float[] trailX = new float[12];
    private final float[] trailY = new float[12];

    private PuzzleRound round;
    private WorkshopSound sound;
    private float scale = 1, frameX, frameY;
    private float downX, downY;
    private int pointer = -1, aimCount, trailCount, playedNotes, unlocked;
    private boolean actionDown, menuDown, resultAnnounced;
    private String chapterCount;
    private long lastFrame;

    GameView(Context context) {
        super(context);
        art = new StageArt(getResources());
        progress = context.getSharedPreferences("workshop_progress", Context.MODE_PRIVATE);
        unlocked = Math.max(0, Math.min(LEVELS.length - 1, progress.getInt("unlocked", 0)));
        touchSlop = Math.max(ViewConfiguration.get(context).getScaledTouchSlop(),
                14 * getResources().getDisplayMetrics().density);
        boardClip.addRoundRect(board, 22, 22, Path.Direction.CW);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        reset(LEVELS[Math.max(0, Math.min(unlocked, progress.getInt("current", 0)))]);
    }

    private void reset(Level level) {
        round = new PuzzleRound(level);
        progress.edit().putInt("current", level.ordinal()).apply();
        chapterCount = (level.ordinal() + 1) + " / " + LEVELS.length;
        aimCount = 0;
        trailCount = 0;
        playedNotes = 0;
        resultAnnounced = false;
        setClickable(false);
        setContentDescription(level.title + ". " + level.objective
                + ". Сделай свайп, чтобы бросить шар.");
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        sound = new WorkshopSound(getContext());
    }

    @Override
    protected void onDetachedFromWindow() {
        if (sound != null) {
            sound.release();
            sound = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = System.nanoTime();
        if (round.active()) {
            rememberMarble();
            round.advance(Math.min(.045f, (now - lastFrame) / 1e9f));
            if (round.level == Level.NOTES && sound != null) {
                while (playedNotes < round.workshop.noteCount()) {
                    sound.play(round.workshop.noteAt(playedNotes++));
                }
            }
        }
        lastFrame = now;

        float w = getWidth() - getPaddingLeft() - getPaddingRight();
        float h = getHeight() - getPaddingTop() - getPaddingBottom();
        if (w <= 0 || h <= 0) return;
        scale = Math.min(w / FRAME_WIDTH, h / FRAME_HEIGHT);
        frameX = getPaddingLeft() + (w - FRAME_WIDTH * scale) / 2;
        frameY = getPaddingTop() + (h - FRAME_HEIGHT * scale) / 2;

        canvas.drawColor(INK);
        canvas.save();
        canvas.translate(frameX, frameY);
        canvas.scale(scale, scale);
        color(0xffffffff);
        paint.setShader(frameLight);
        canvas.drawRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT, paint);
        paint.setShader(null);
        header(canvas);
        stage(canvas, now);
        footer(canvas);
        canvas.restore();

        if (round.started && !round.active() && !resultAnnounced) {
            resultAnnounced = true;
            setClickable(true);
            setContentDescription(round.event.title + ". " + actionLabel() + ".");
            if (round.won() && round.level.ordinal() == unlocked && unlocked < LEVELS.length - 1) {
                progress.edit().putInt("unlocked", ++unlocked).apply();
            }
        }
        if (getWindowVisibility() == VISIBLE) {
            if (round.active()) postInvalidateOnAnimation();
            else if (!round.started || round.level == Level.VASE || round.level == Level.CAT) {
                postInvalidateDelayed(40);
            }
        }
    }

    private void header(Canvas canvas) {
        color(0xff3c3331);
        canvas.drawRoundRect(19, 22, 53, 56, 12, 12, paint);
        color(StageArt.GOLD);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.8f);
        canvas.drawCircle(36, 41, 9.2f, paint);
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(36, 41, 40.8f, 36.5f, paint);
        canvas.drawLine(33.5f, 28, 38.5f, 28, paint);
        paint.setStrokeCap(Paint.Cap.BUTT);
        label(canvas, "ONE SECOND!", 33, PAPER, 63, 52, TITLE, Paint.Align.LEFT);
        color(menuDown ? 0xff685445 : 0xff36363f);
        canvas.drawRoundRect(335, 26, 413, 55, 14, 14, paint);
        label(canvas, chapterCount, 15, StageArt.GOLD, 369, 46, LABEL, Paint.Align.CENTER);
        color(StageArt.GOLD);
        paint.setStrokeWidth(1.4f);
        canvas.drawLine(392, 38, 397, 43, paint);
        canvas.drawLine(397, 43, 402, 38, paint);
        label(canvas, round.level.title, 24, PAPER, 20, 91, LABEL, Paint.Align.LEFT);
    }

    private void stage(Canvas canvas, long timeNanos) {
        color(0xff0e1117);
        canvas.drawRoundRect(board.left - 3, board.top - 3, board.right + 3,
                board.bottom + 6, 24, 24, paint);
        canvas.save();
        canvas.clipPath(boardClip);
        canvas.translate(BOARD_X, BOARD_Y);
        float ambient = (timeNanos % 60_000_000_000L) / 1_000_000_000f;
        art.drawRoom(canvas, round, ambient);
        if (round.active()) drawTrail(canvas);
        if (!round.started) drawAim(canvas);
        art.drawMarble(canvas, round, (timeNanos % 1_480_000_000L) / 1_480_000_000f);
        art.drawImpact(canvas, round);
        timeBadge(canvas);
        canvas.restore();

        color(0x6bc9a77c);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.2f);
        canvas.drawRoundRect(board, 22, 22, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void timeBadge(Canvas canvas) {
        int accent = !round.started ? StageArt.ICE : StageArt.GOLD;
        String text = "Время замерло";
        if (round.started && round.active()) {
            int hundredths = (int) (round.motionTime() * 100);
            text = (hundredths / 100) + "," + (hundredths % 100 < 10 ? "0" : "")
                    + (hundredths % 100) + " с";
        }
        if (round.started && !round.active()) {
            text = round.won() ? "Спасено" : "Промах";
            if (!round.won()) accent = StageArt.CORAL;
        }
        color(0xdf151823);
        canvas.drawRoundRect(119, 17, 281, 53, 18, 18, paint);
        color(StageArt.alpha(accent, 95));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        canvas.drawRoundRect(119, 17, 281, 53, 18, 18, paint);
        color(accent);
        canvas.drawCircle(139, 35, 3, paint);
        label(canvas, text, 14, accent, 205, 40, LABEL, Paint.Align.CENTER);
    }

    private void drawAim(Canvas canvas) {
        for (int i = 0; i < aimCount; i++) {
            color(StageArt.alpha(StageArt.ICE, 238 - 14 * i));
            canvas.drawCircle(aimPoints[2 * i], aimPoints[2 * i + 1], 4.5f - .22f * i, paint);
        }
    }

    private void rememberMarble() {
        System.arraycopy(trailX, 0, trailX, 1, trailX.length - 1);
        System.arraycopy(trailY, 0, trailY, 1, trailY.length - 1);
        trailX[0] = round.marbleX;
        trailY[0] = round.marbleY;
        trailCount = Math.min(trailCount + 1, trailX.length);
    }

    private void drawTrail(Canvas canvas) {
        for (int i = trailCount - 1; i >= 0; i--) {
            if (Math.hypot(trailX[i] - round.marbleX, trailY[i] - round.marbleY) < 5) continue;
            color(StageArt.alpha(StageArt.ICE, (trailX.length - i) * 8));
            canvas.drawCircle(trailX[i], trailY[i], 10 - i * .58f, paint);
        }
    }

    private void footer(Canvas canvas) {
        if (round.started && !round.active()) {
            label(canvas, round.event.title, 21, round.won() ? StageArt.GOLD : StageArt.CORAL,
                    20, 753, TITLE, Paint.Align.LEFT);
            label(canvas, round.event.detail, 13, MUTED, 20, 776, LABEL, Paint.Align.LEFT);
            color(actionDown ? 0xffe6ae65 : StageArt.GOLD);
            canvas.drawRoundRect(action, 17, 17, paint);
            label(canvas, actionLabel(),
                    16, 0xff2b211d, action.centerX(), action.centerY() + 5, LABEL, Paint.Align.CENTER);
            return;
        }

        color(0xff30363f);
        canvas.drawRoundRect(20, 731, 68, 779, 16, 16, paint);
        color(round.started ? StageArt.GOLD : StageArt.ICE);
        paint.setStrokeWidth(2.2f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(35, 761, 52, 745, paint);
        canvas.drawLine(52, 745, 52, 755, paint);
        canvas.drawLine(52, 745, 42, 745, paint);
        paint.setStrokeCap(Paint.Cap.BUTT);
        label(canvas, round.level.objective, 18, PAPER, 81, 751, LABEL, Paint.Align.LEFT);
        label(canvas, round.started ? "Смотри, что произойдёт" : "Задай направление одним свайпом",
                13, MUTED, 81, 773, LABEL, Paint.Align.LEFT);
    }

    private String actionLabel() {
        if (!round.won()) return "Ещё раз";
        return round.level.ordinal() < LEVELS.length - 1 ? "Дальше" : "Сначала";
    }

    private void chooseLevel() {
        String[] titles = new String[unlocked + 1];
        for (int i = 0; i < titles.length; i++) {
            titles[i] = (i + 1) + ". " + LEVELS[i].title;
        }
        new AlertDialog.Builder(getContext(), android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Уровни")
                .setItems(titles, (dialog, which) -> reset(LEVELS[which]))
                .show();
    }

    private void label(Canvas canvas, String text, float size, int tint, float x, float y,
                       Typeface type, Paint.Align align) {
        color(tint);
        paint.setTextSize(size);
        paint.setTypeface(type);
        paint.setTextAlign(align);
        canvas.drawText(text, x, y, paint);
    }

    private void color(int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int index = event.getActionIndex();
        int id = event.getPointerId(index);
        float x = event.getX(index);
        float y = event.getY(index);
        float frameTouchX = (x - frameX) / scale;
        float frameTouchY = (y - frameY) / scale;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                actionDown = resultAnnounced && action.contains(frameTouchX, frameTouchY);
                menuDown = !round.active() && levelMenu.contains(frameTouchX, frameTouchY);
                if (!round.started && board.contains(frameTouchX, frameTouchY)) {
                    pointer = id;
                    downX = x;
                    downY = y;
                    aimCount = 0;
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                int activeIndex = event.findPointerIndex(pointer);
                if (activeIndex >= 0) {
                    float dx = event.getX(activeIndex) - downX;
                    float dy = event.getY(activeIndex) - downY;
                    aimCount = Math.hypot(dx, dy) >= touchSlop ? round.preview(dx, dy, aimPoints) : 0;
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    if (actionDown && action.contains(frameTouchX, frameTouchY)) performClick();
                    if (menuDown && levelMenu.contains(frameTouchX, frameTouchY)) chooseLevel();
                    actionDown = false;
                    menuDown = false;
                    invalidate();
                }
                if (pointer != -1 && pointer == id) {
                    float dx = x - downX, dy = y - downY;
                    if (!round.started && Math.hypot(dx, dy) >= touchSlop) {
                        round.launch(dx, dy);
                        lastFrame = System.nanoTime();
                    }
                    pointer = -1;
                    aimCount = 0;
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                pointer = -1;
                aimCount = 0;
                actionDown = false;
                menuDown = false;
                invalidate();
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        if (resultAnnounced) {
            int next = (round.level.ordinal() + 1) % LEVELS.length;
            reset(round.won() ? LEVELS[next] : round.level);
        }
        return true;
    }
}
