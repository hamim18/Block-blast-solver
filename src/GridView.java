package com.blockblast.solver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayDeque;
import java.util.Deque;

public class GridView extends View {

    private static final int GRID_SIZE = 8;
    private boolean[][] grid = new boolean[GRID_SIZE][GRID_SIZE];
    private int[][] cellColors = new int[GRID_SIZE][GRID_SIZE];

    private final Deque<boolean[][]> undoStack = new ArrayDeque<>();
    private static final int MAX_UNDO = 30;

    private Block hoverBlock = null;
    private int hoverRow = -1, hoverCol = -1;
    private boolean hoverValid = false;

    private Paint emptyPaint;
    private Paint filledPaint;
    private Paint overlayPaint;
    private Paint borderPaint;       // border luar grid
    private Paint cellBorderPaint;   // border tipis antar cell
    private Paint hoverValidPaint;
    private Paint hoverInvalidPaint;

    private float cellSize;
    // Tipis & sedikit kontras: SEP adalah setengah gap antar cell (tiap sisi)
    private static final float SEP = 1.0f;
    private static final float RAD = 2.5f;

    private boolean interactive = true;
    private boolean dragUndoPushed = false;
    private OnGridChangedListener listener;

    public interface OnGridChangedListener {
        void onGridChanged(boolean[][] grid);
    }

    public GridView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public GridView(Context context) { super(context); init(); }

    private void init() {
        emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        emptyPaint.setColor(Color.parseColor("#1A1A2E"));
        emptyPaint.setStyle(Paint.Style.FILL);

        filledPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        filledPaint.setColor(Color.parseColor("#5C6BC0"));
        filledPaint.setStyle(Paint.Style.FILL);

        overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        overlayPaint.setStyle(Paint.Style.FILL);

        // Border antar cell — tipis, sedikit kontras
        cellBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellBorderPaint.setColor(Color.parseColor("#2E2E4A"));
        cellBorderPaint.setStrokeWidth(0f);
        cellBorderPaint.setStyle(Paint.Style.STROKE);

        borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#45475A"));
        borderPaint.setStrokeWidth(1.5f);
        borderPaint.setStyle(Paint.Style.STROKE);

        hoverValidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hoverValidPaint.setColor(Color.argb(180, 166, 227, 161));
        hoverValidPaint.setStyle(Paint.Style.FILL);

        hoverInvalidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hoverInvalidPaint.setColor(Color.argb(180, 243, 139, 168));
        hoverInvalidPaint.setStyle(Paint.Style.FILL);

        for (int r = 0; r < GRID_SIZE; r++)
            for (int c = 0; c < GRID_SIZE; c++)
                cellColors[r][c] = -1;
    }

    // ─── Hover API ───────────────────────────────────────────────────────────

    public void setHoverPiece(Block block, int row, int col) {
        hoverBlock = block; hoverRow = row; hoverCol = col;
        if (block != null) hoverValid = canPreviewPlace(block, row, col);
        invalidate();
    }

    public void clearHover() { hoverBlock = null; hoverRow = -1; hoverCol = -1; invalidate(); }

    private boolean canPreviewPlace(Block block, int row, int col) {
        if (row < 0 || col < 0) return false;
        if (row + block.getRows() > GRID_SIZE || col + block.getCols() > GRID_SIZE) return false;
        boolean[][] shape = block.getShape();
        for (int r = 0; r < block.getRows(); r++)
            for (int c = 0; c < block.getCols(); c++)
                if (shape[r][c] && grid[row + r][col + c]) return false;
        return true;
    }

    // ─── Undo API ────────────────────────────────────────────────────────────

    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        grid = undoStack.pop();
        invalidate();
        if (listener != null) listener.onGridChanged(grid);
        return true;
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }

    private void pushUndo() {
        if (undoStack.size() >= MAX_UNDO) undoStack.pollLast();
        undoStack.push(copyGrid(grid));
    }

    // ─── Measure & Size ──────────────────────────────────────────────────────

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(w, w);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        cellSize = (float) w / GRID_SIZE;
    }

    // ─── Draw ────────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        boolean[][] hoverMask = new boolean[GRID_SIZE][GRID_SIZE];
        if (hoverBlock != null && hoverRow >= 0 && hoverCol >= 0) {
            boolean[][] shape = hoverBlock.getShape();
            for (int r = 0; r < hoverBlock.getRows(); r++)
                for (int c = 0; c < hoverBlock.getCols(); c++)
                    if (shape[r][c]) {
                        int gr = hoverRow + r, gc = hoverCol + c;
                        if (gr >= 0 && gr < GRID_SIZE && gc >= 0 && gc < GRID_SIZE)
                            hoverMask[gr][gc] = true;
                    }
        }

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                float left   = c * cellSize + SEP;
                float top    = r * cellSize + SEP;
                float right  = (c + 1) * cellSize - SEP;
                float bottom = (r + 1) * cellSize - SEP;
                RectF rect   = new RectF(left, top, right, bottom);

                // Fill
                if (hoverMask[r][c]) {
                    canvas.drawRoundRect(rect, RAD, RAD,
                            hoverValid ? hoverValidPaint : hoverInvalidPaint);
                } else if (cellColors[r][c] != -1) {
                    overlayPaint.setColor(cellColors[r][c]);
                    canvas.drawRoundRect(rect, RAD, RAD, overlayPaint);
                } else if (grid[r][c]) {
                    canvas.drawRoundRect(rect, RAD, RAD, filledPaint);
                } else {
                    canvas.drawRoundRect(rect, RAD, RAD, emptyPaint);
                }

                // Border tipis antar cell
                canvas.drawRoundRect(rect, RAD, RAD, cellBorderPaint);
            }
        }

        // Outer border
        canvas.drawRect(0, 0, getWidth(), getHeight(), borderPaint);
    }

    // ─── Touch ───────────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!interactive) return false;
        int col = (int)(event.getX() / cellSize);
        int row = (int)(event.getY() / cellSize);
        boolean inBounds = row >= 0 && row < GRID_SIZE && col >= 0 && col < GRID_SIZE;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                dragUndoPushed = false;
                if (inBounds) {
                    pushUndo(); dragUndoPushed = true;
                    grid[row][col] = !grid[row][col];
                    invalidate();
                    if (listener != null) listener.onGridChanged(grid);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (inBounds && !grid[row][col]) {
                    if (!dragUndoPushed) { pushUndo(); dragUndoPushed = true; }
                    grid[row][col] = true;
                    invalidate();
                    if (listener != null) listener.onGridChanged(grid);
                }
                break;
            case MotionEvent.ACTION_UP:
                dragUndoPushed = false;
                break;
        }
        return true;
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    public boolean[][] getGrid() {
        boolean[][] copy = new boolean[GRID_SIZE][GRID_SIZE];
        for (int r = 0; r < GRID_SIZE; r++) copy[r] = grid[r].clone();
        return copy;
    }

    public void setGrid(boolean[][] g) {
        for (int r = 0; r < GRID_SIZE; r++) grid[r] = g[r].clone();
        invalidate();
    }

    public void setCellColor(int row, int col, int color) { cellColors[row][col] = color; invalidate(); }

    public void clearColors() {
        for (int r = 0; r < GRID_SIZE; r++)
            for (int c = 0; c < GRID_SIZE; c++)
                cellColors[r][c] = -1;
        invalidate();
    }

    public void setInteractive(boolean interactive) { this.interactive = interactive; }
    public void setOnGridChangedListener(OnGridChangedListener l) { this.listener = l; }

    public void clearGrid() {
        pushUndo();
        grid = new boolean[GRID_SIZE][GRID_SIZE];
        clearColors();
    }

    private boolean[][] copyGrid(boolean[][] g) {
        boolean[][] copy = new boolean[GRID_SIZE][GRID_SIZE];
        for (int r = 0; r < GRID_SIZE; r++) copy[r] = g[r].clone();
        return copy;
    }
}
