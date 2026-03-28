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

    // Undo stack — setiap entry adalah snapshot grid sebelum perubahan
    private final Deque<boolean[][]> undoStack = new ArrayDeque<>();
    private static final int MAX_UNDO = 30;

    // Preview piece hover
    private Block hoverBlock = null;
    private int hoverRow = -1;
    private int hoverCol = -1;
    private boolean hoverValid = false;

    private Paint filledPaint;
    private Paint emptyPaint;
    private Paint gridLinePaint;
    private Paint borderPaint;
    private Paint overlayPaint;
    private Paint hoverValidPaint;
    private Paint hoverInvalidPaint;

    private float cellSize;
    private float padding = 4f;

    private boolean interactive = true;
    private OnGridChangedListener listener;

    public interface OnGridChangedListener {
        void onGridChanged(boolean[][] grid);
    }

    public GridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GridView(Context context) {
        super(context);
        init();
    }

    private void init() {
        filledPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        filledPaint.setColor(Color.parseColor("#5C6BC0"));
        filledPaint.setStyle(Paint.Style.FILL);

        emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        emptyPaint.setColor(Color.parseColor("#1E1E2E"));
        emptyPaint.setStyle(Paint.Style.FILL);

        gridLinePaint = new Paint();
        gridLinePaint.setColor(Color.parseColor("#313244"));
        gridLinePaint.setStrokeWidth(1f);
        gridLinePaint.setStyle(Paint.Style.STROKE);

        borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#CDD6F4"));
        borderPaint.setStrokeWidth(2f);
        borderPaint.setStyle(Paint.Style.STROKE);

        overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        overlayPaint.setStyle(Paint.Style.FILL);

        // Preview hijau transparan = posisi valid
        hoverValidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hoverValidPaint.setColor(Color.argb(140, 166, 227, 161)); // #A6E3A1 transparan
        hoverValidPaint.setStyle(Paint.Style.FILL);

        // Preview merah transparan = posisi tidak valid / tabrakan
        hoverInvalidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hoverInvalidPaint.setColor(Color.argb(140, 243, 139, 168)); // #F38BA8 transparan
        hoverInvalidPaint.setStyle(Paint.Style.FILL);

        for (int r = 0; r < GRID_SIZE; r++)
            for (int c = 0; c < GRID_SIZE; c++)
                cellColors[r][c] = -1;
    }

    // ─── Hover / Preview API ────────────────────────────────────────────────

    /**
     * Tampilkan preview piece di atas grid pada posisi (row, col).
     * Panggil dengan block=null untuk menyembunyikan preview.
     */
    public void setHoverPiece(Block block, int row, int col) {
        hoverBlock = block;
        hoverRow = row;
        hoverCol = col;
        if (block != null) {
            hoverValid = canPreviewPlace(block, row, col);
        }
        invalidate();
    }

    public void clearHover() {
        hoverBlock = null;
        hoverRow = -1;
        hoverCol = -1;
        invalidate();
    }

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

    /** Batalkan perubahan terakhir. Return true jika ada yang bisa di-undo. */
    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        grid = undoStack.pop();
        invalidate();
        if (listener != null) listener.onGridChanged(grid);
        return true;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    private void pushUndo() {
        if (undoStack.size() >= MAX_UNDO) undoStack.pollLast();
        undoStack.push(copyGrid(grid));
    }

    // ─── Draw ────────────────────────────────────────────────────────────────

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, width);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        cellSize = (float) w / GRID_SIZE;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Tentukan sel hover terlebih dahulu agar bisa di-skip gambar normalnya
        boolean[][] hoverMask = new boolean[GRID_SIZE][GRID_SIZE];
        if (hoverBlock != null && hoverRow >= 0 && hoverCol >= 0) {
            boolean[][] shape = hoverBlock.getShape();
            for (int r = 0; r < hoverBlock.getRows(); r++) {
                for (int c = 0; c < hoverBlock.getCols(); c++) {
                    if (shape[r][c]) {
                        int gr = hoverRow + r;
                        int gc = hoverCol + c;
                        if (gr >= 0 && gr < GRID_SIZE && gc >= 0 && gc < GRID_SIZE)
                            hoverMask[gr][gc] = true;
                    }
                }
            }
        }

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                float left   = c * cellSize + padding;
                float top    = r * cellSize + padding;
                float right  = (c + 1) * cellSize - padding;
                float bottom = (r + 1) * cellSize - padding;
                RectF rect = new RectF(left, top, right, bottom);

                if (hoverMask[r][c]) {
                    // Gambar sel hover
                    Paint hp = hoverValid ? hoverValidPaint : hoverInvalidPaint;
                    canvas.drawRoundRect(rect, 8f, 8f, hp);
                } else if (cellColors[r][c] != -1) {
                    overlayPaint.setColor(cellColors[r][c]);
                    canvas.drawRoundRect(rect, 8f, 8f, overlayPaint);
                } else if (grid[r][c]) {
                    canvas.drawRoundRect(rect, 8f, 8f, filledPaint);
                } else {
                    canvas.drawRoundRect(rect, 8f, 8f, emptyPaint);
                }

                canvas.drawRoundRect(rect, 8f, 8f, gridLinePaint);
            }
        }
        canvas.drawRect(padding, padding, getWidth() - padding, getHeight() - padding, borderPaint);
    }

    // ─── Touch ───────────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!interactive) return false;

        int col = (int) (event.getX() / cellSize);
        int row = (int) (event.getY() / cellSize);
        boolean inBounds = row >= 0 && row < GRID_SIZE && col >= 0 && col < GRID_SIZE;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (inBounds) {
                    pushUndo();
                    grid[row][col] = !grid[row][col];
                    invalidate();
                    if (listener != null) listener.onGridChanged(grid);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (inBounds) {
                    if (!grid[row][col]) { // hanya set true saat drag (tidak toggle)
                        grid[row][col] = true;
                        invalidate();
                        if (listener != null) listener.onGridChanged(grid);
                    }
                }
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

    public void setCellColor(int row, int col, int color) {
        cellColors[row][col] = color;
        invalidate();
    }

    public void clearColors() {
        for (int r = 0; r < GRID_SIZE; r++)
            for (int c = 0; c < GRID_SIZE; c++)
                cellColors[r][c] = -1;
        invalidate();
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }

    public void setOnGridChangedListener(OnGridChangedListener listener) {
        this.listener = listener;
    }

    public void clearGrid() {
        pushUndo();
        grid = new boolean[GRID_SIZE][GRID_SIZE];
        undoStack.clear(); // reset undo history setelah clear total
        clearColors();
    }

    private boolean[][] copyGrid(boolean[][] g) {
        boolean[][] copy = new boolean[GRID_SIZE][GRID_SIZE];
        for (int r = 0; r < GRID_SIZE; r++) copy[r] = g[r].clone();
        return copy;
    }
}
