package com.chess.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.Collections;
import java.util.List;

public final class ShapeCanvasView extends View {
    public interface Listener {
        void onShapeSelected(ShapeModel shape);
        void onShapeDeleted(ShapeModel shape);
        void onShapeMoved(ShapeModel shape);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final float squareSize;
    private final float halfSquare;
    private final float stroke;
    private final int touchSlop;

    private List<ShapeModel> shapes = Collections.emptyList();
    private Listener listener;
    private ShapeModel selected;
    private ShapeModel active;
    private float downX;
    private float downY;
    private boolean dragging;
    private boolean movedBeforeLongPress;
    private long lastTapTime;
    private String lastTapShapeId;

    private final Runnable armDrag = () -> {
        if (active != null && !movedBeforeLongPress) {
            dragging = true;
            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
            if (listener != null) listener.onShapeSelected(active);
        }
    };

    public ShapeCanvasView(Context context) {
        super(context);
        setWillNotDraw(false);
        squareSize = dp(58);
        halfSquare = squareSize / 2f;
        stroke = dp(2.4f);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setShapes(List<ShapeModel> shapes) {
        this.shapes = shapes == null ? Collections.emptyList() : shapes;
        invalidate();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setSelected(ShapeModel selected) {
        this.selected = selected;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (ShapeModel shape : shapes) drawShape(canvas, shape);
    }

    private void drawShape(Canvas canvas, ShapeModel shape) {
        float cx = shape.centerX;
        float cy = shape.centerY;
        float left = cx - halfSquare;
        float top = cy - halfSquare;
        float right = cx + halfSquare;
        float bottom = cy + halfSquare;
        float len = lengthPx(shape.length);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(shape == selected ? stroke * 1.55f : stroke);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(shape.type.color);
        paint.setAlpha(255);

        canvas.drawRect(new RectF(left, top, right, bottom), paint);

        switch (shape.type) {
            case PLUS:
                drawPlus(canvas, cx, cy, left, top, right, bottom, len);
                break;
            case X:
                drawX(canvas, left, top, right, bottom, len);
                break;
            case KNIGHT:
                drawKnight(canvas, cx, cy, left, top, right, bottom, len);
                break;
            case PLUS_X:
                drawPlus(canvas, cx, cy, left, top, right, bottom, len);
                drawX(canvas, left, top, right, bottom, len);
                break;
            case PAWN:
                drawPawn(canvas, left, right, bottom, len);
                break;
            case KING:
                drawKing(canvas, cx, left, top, right, len);
                break;
        }
    }

    private void drawPlus(Canvas c, float cx, float cy, float l, float t, float r, float b, float len) {
        c.drawLine(cx, t, cx, t - len, paint);
        c.drawLine(cx, b, cx, b + len, paint);
        c.drawLine(l, cy, l - len, cy, paint);
        c.drawLine(r, cy, r + len, cy, paint);
    }

    private void drawX(Canvas c, float l, float t, float r, float b, float len) {
        float d = len * 0.70710678f;
        c.drawLine(l, t, l - d, t - d, paint);
        c.drawLine(r, t, r + d, t - d, paint);
        c.drawLine(l, b, l - d, b + d, paint);
        c.drawLine(r, b, r + d, b + d, paint);
    }

    private void drawKnight(Canvas c, float cx, float cy, float l, float t, float r, float b, float len) {
        float straight = len * 0.62f;
        float turn = len * 0.38f;

        c.drawLine(cx, t, cx, t - straight, paint);
        c.drawLine(cx, t - straight, cx + turn, t - straight, paint);

        c.drawLine(r, cy, r + straight, cy, paint);
        c.drawLine(r + straight, cy, r + straight, cy + turn, paint);

        c.drawLine(cx, b, cx, b + straight, paint);
        c.drawLine(cx, b + straight, cx - turn, b + straight, paint);

        c.drawLine(l, cy, l - straight, cy, paint);
        c.drawLine(l - straight, cy, l - straight, cy - turn, paint);
    }

    private void drawPawn(Canvas c, float l, float r, float b, float len) {
        float d = len * 0.70710678f;
        c.drawLine(l, b, l - d, b + d, paint);
        c.drawLine(r, b, r + d, b + d, paint);
    }

    private void drawKing(Canvas c, float cx, float l, float t, float r, float len) {
        float stemEnd = t - len;
        c.drawLine(cx, t, cx, stemEnd, paint);
        float cross = Math.max(dp(10), len * 0.30f);
        c.drawLine(cx - cross, stemEnd + len * 0.18f, cx + cross, stemEnd + len * 0.18f, paint);

        float side = len * 0.72f;
        float d = side * 0.70710678f;
        c.drawLine(l, t, l - d, t - d, paint);
        c.drawLine(r, t, r + d, t - d, paint);
    }

    private float lengthPx(int value) {
        float normalized = (ShapeModel.clampLength(value) - 1f) / 99f;
        return dp(14f + normalized * 210f);
    }

    private ShapeModel hitSquare(float x, float y) {
        for (int i = shapes.size() - 1; i >= 0; i--) {
            ShapeModel s = shapes.get(i);
            if (Math.abs(x - s.centerX) <= halfSquare && Math.abs(y - s.centerY) <= halfSquare) {
                return s;
            }
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                active = hitSquare(event.getX(), event.getY());
                if (active == null) return true;
                downX = event.getX();
                downY = event.getY();
                dragging = false;
                movedBeforeLongPress = false;
                handler.postDelayed(armDrag, 300);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (active == null) return true;
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (!dragging && Math.hypot(dx, dy) > touchSlop) {
                    movedBeforeLongPress = true;
                    handler.removeCallbacks(armDrag);
                }
                if (dragging) {
                    active.centerX = event.getX();
                    active.centerY = event.getY();
                    selected = active;
                    invalidate();
                    if (listener != null) listener.onShapeMoved(active);
                }
                return true;

            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(armDrag);
                if (active == null) return true;
                ShapeModel released = active;
                active = null;

                if (dragging) {
                    dragging = false;
                    if (listener != null) listener.onShapeMoved(released);
                    return true;
                }

                float upDx = event.getX() - downX;
                float upDy = event.getY() - downY;
                if (Math.hypot(upDx, upDy) <= touchSlop) {
                    long now = System.currentTimeMillis();
                    boolean doubleTap = released.id.equals(lastTapShapeId) && now - lastTapTime <= 300;
                    if (doubleTap) {
                        lastTapTime = 0;
                        lastTapShapeId = null;
                        if (listener != null) listener.onShapeDeleted(released);
                    } else {
                        selected = released;
                        invalidate();
                        lastTapTime = now;
                        lastTapShapeId = released.id;
                        if (listener != null) listener.onShapeSelected(released);
                    }
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(armDrag);
                active = null;
                dragging = false;
                return true;
        }
        return true;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
