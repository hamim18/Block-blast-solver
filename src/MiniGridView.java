package com.blockblast.solver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class MiniGridView extends View {

    private static final int SIZE = 5;
    private boolean[][] grid = new boolean[SIZE][SIZE];

    private Paint emptyPaint;
    private Paint filledPaint;
    private Paint cellBorderPaint;
    private Paint borderPaint;
    private Paint borderActivePaint;

    private float cellSize;
    private static final float SEP = 1.0f;
    private static final float RAD = 2.5f;

    private OnPieceChangedListener listener;
    private boolean hasAnyCell = false;

    public interface OnPieceChangedListener {
        void onPieceChanged(boolean hasCells);
    }

    public MiniGridView(Context context) { super(context); init(); }
    public MiniGridView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        emptyPaint.setColor(Color.parseColor("#1A1A2E"));
        emptyPaint.setStyle(Paint.Style.FILL);

        filledPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        filledPaint.setColor(Color.parseColor("#5C6BC0"));
        filledPaint.setStyle(Paint.Style.FILL);

        cellBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellBorderPaint.setColor(Color.parseColor("#2E2E4A"));
        cellBorderPaint.setStrokeWidth(0f);
        cellBorderPaint.setStyle(Paint.Style.STROKE);

        borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#313244"));
        borderPaint.setStrokeWidth(1.5f);
        borderPaint.setStyle(Paint.Style.STROKE);

        borderActivePaint = new Paint();
        borderActivePaint.setColor(Color.parseColor("#89B4FA"));
        borderActivePaint.setStrokeWidth(2f);
        borderActivePaint.setStyle(Paint.Style.STROKE);
    }

    public void setOnPieceChangedListener(OnPieceChangedListener l) { this.listener = l; }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(w, w);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        cellSize = (float) w / SIZE;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                float left   = c * cellSize + SEP;
                float top    = r * cellSize + SEP;
                float right  = (c + 1) * cellSize - SEP;
                float bottom = (r + 1) * cellSize - SEP;
                RectF rect   = new RectF(left, top, right, bottom);

                canvas.drawRoundRect(rect, RAD, RAD, grid[r][c] ? filledPaint : emptyPaint);
                canvas.drawRoundRect(rect, RAD, RAD, cellBorderPaint);
            }
        }
        canvas.drawRect(0, 0, getWidth(), getHeight(),
                hasAnyCell ? borderActivePaint : borderPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int col = (int)(event.getX() / cellSize);
        int row = (int)(event.getY() / cellSize);
        boolean inBounds = row >= 0 && row < SIZE && col >= 0 && col < SIZE;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (inBounds) { grid[row][col] = !grid[row][col]; notifyChanged(); }
                break;
            case MotionEvent.ACTION_MOVE:
                if (inBounds && !grid[row][col]) { grid[row][col] = true; notifyChanged(); }
                break;
        }
        return true;
    }

    private void notifyChanged() {
        hasAnyCell = hasAnyCellFilled();
        if (listener != null) listener.onPieceChanged(hasAnyCell);
        invalidate();
    }

    private boolean hasAnyCellFilled() {
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (grid[r][c]) return true;
        return false;
    }

    public Block toBlock(String name, int color) {
        int minR = SIZE, maxR = -1, minC = SIZE, maxC = -1;
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (grid[r][c]) {
                    if (r < minR) minR = r; if (r > maxR) maxR = r;
                    if (c < minC) minC = c; if (c > maxC) maxC = c;
                }
        if (maxR < 0) return null;
        boolean[][] shape = new boolean[maxR - minR + 1][maxC - minC + 1];
        for (int r = minR; r <= maxR; r++)
            for (int c = minC; c <= maxC; c++)
                shape[r - minR][c - minC] = grid[r][c];
        return new Block(name, color, shape);
    }

    public boolean hasAnyCells() { return hasAnyCell; }

    public void clearGrid() {
        grid = new boolean[SIZE][SIZE];
        hasAnyCell = false;
        if (listener != null) listener.onPieceChanged(false);
        invalidate();
    }

    public boolean[][] getGrid() {
        boolean[][] copy = new boolean[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) copy[r] = grid[r].clone();
        return copy;
    }

    public void setGrid(boolean[][] g) {
        for (int r = 0; r < SIZE; r++) grid[r] = g[r].clone();
        hasAnyCell = hasAnyCellFilled();
        if (listener != null) listener.onPieceChanged(hasAnyCell);
        invalidate();
    }
}
