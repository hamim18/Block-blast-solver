package com.blockblast.solver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class PieceView extends View {

    private Block block;
    private Paint cellPaint;
    private Paint borderPaint;
    private Paint bgPaint;
    private float padding = 2f;

    public PieceView(Context context) {
        super(context);
        init();
    }

    public PieceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#313244"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1f);

        bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#1E1E2E"));
        bgPaint.setStyle(Paint.Style.FILL);
    }

    public void setBlock(Block block) {
        this.block = block;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (block == null) return;

        canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);

        int rows = block.getRows();
        int cols = block.getCols();
        int maxDim = Math.max(rows, cols);

        float cellSize = (Math.min(getWidth(), getHeight()) - padding * 2) / maxDim;
        float offsetX = (getWidth() - cols * cellSize) / 2f;
        float offsetY = (getHeight() - rows * cellSize) / 2f;

        boolean[][] shape = block.getShape();
        cellPaint.setColor(block.getColor());

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (shape[r][c]) {
                    float left = offsetX + c * cellSize + padding;
                    float top = offsetY + r * cellSize + padding;
                    float right = offsetX + (c + 1) * cellSize - padding;
                    float bottom = offsetY + (r + 1) * cellSize - padding;
                    RectF rect = new RectF(left, top, right, bottom);
                    canvas.drawRoundRect(rect, 6f, 6f, cellPaint);
                    canvas.drawRoundRect(rect, 6f, 6f, borderPaint);
                }
            }
        }
    }
}
