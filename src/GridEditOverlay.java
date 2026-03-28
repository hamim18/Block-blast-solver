package com.blockblast.solver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * GridEditOverlay — view transparan fullscreen yang di-render di atas
 * semua konten saat mode "Edit Grid".
 *
 * Menampilkan kotak berwarna dengan:
 * - 4 handle bulat di sudut untuk resize
 * - Area tengah untuk drag (pindah posisi)
 * - Grid 8×8 semi-transparan di dalamnya sebagai panduan
 *
 * Memanggil OnGridEditListener saat posisi/ukuran berubah.
 */
public class GridEditOverlay extends View {

    public interface OnGridEditListener {
        void onGridChanged(int x, int y, int w, int h);
    }

    private OnGridEditListener listener;

    // Posisi & ukuran kotak (dalam koordinat view = koordinat layar)
    private float boxX, boxY, boxW, boxH;

    // Handle size
    private static final float HANDLE_R   = 28f;
    private static final float MIN_SIZE   = 100f;

    // Mode drag
    private enum DragMode { NONE, MOVE, RESIZE_TL, RESIZE_TR, RESIZE_BL, RESIZE_BR }
    private DragMode dragMode = DragMode.NONE;
    private float touchStartX, touchStartY;
    private float startBoxX, startBoxY, startBoxW, startBoxH;

    // Paint
    private Paint borderPaint, handlePaint, handleBorderPaint, gridPaint, bgPaint, labelPaint;

    public GridEditOverlay(Context ctx) { super(ctx); init(); }
    public GridEditOverlay(Context ctx, AttributeSet a) { super(ctx, a); init(); }

    private void init() {
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.parseColor("#89B4FA"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);

        handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        handlePaint.setColor(Color.parseColor("#89B4FA"));
        handlePaint.setStyle(Paint.Style.FILL);

        handleBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        handleBorderPaint.setColor(Color.WHITE);
        handleBorderPaint.setStyle(Paint.Style.STROKE);
        handleBorderPaint.setStrokeWidth(2f);

        gridPaint = new Paint();
        gridPaint.setColor(Color.argb(60, 137, 180, 250));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);

        bgPaint = new Paint();
        bgPaint.setColor(Color.argb(30, 137, 180, 250));
        bgPaint.setStyle(Paint.Style.FILL);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(32f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setListener(OnGridEditListener l) { this.listener = l; }

    /** Set posisi & ukuran awal kotak */
    public void setBox(float x, float y, float w, float h) {
        boxX = x; boxY = y; boxW = w; boxH = h;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float r = boxX, t = boxY, ri = boxX + boxW, b = boxY + boxH;

        // Background tipis
        canvas.drawRect(r, t, ri, b, bgPaint);

        // Grid 8×8
        float cw = boxW / 8f, ch = boxH / 8f;
        for (int i = 1; i < 8; i++) {
            canvas.drawLine(r + i*cw, t, r + i*cw, b, gridPaint);
            canvas.drawLine(r, t + i*ch, ri, t + i*ch, gridPaint);
        }

        // Border
        canvas.drawRect(r, t, ri, b, borderPaint);

        // Handle sudut
        drawHandle(canvas, r,  t);   // TL
        drawHandle(canvas, ri, t);   // TR
        drawHandle(canvas, r,  b);   // BL
        drawHandle(canvas, ri, b);   // BR

        // Label
        canvas.drawText("Drag untuk pindah • Sudut untuk resize",
                r + boxW/2f, t - 12f, labelPaint);
    }

    private void drawHandle(Canvas canvas, float cx, float cy) {
        canvas.drawCircle(cx, cy, HANDLE_R, handlePaint);
        canvas.drawCircle(cx, cy, HANDLE_R, handleBorderPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float tx = e.getX(), ty = e.getY();
        float r = boxX, t = boxY, ri = boxX+boxW, b = boxY+boxH;

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = tx; touchStartY = ty;
                startBoxX = boxX; startBoxY = boxY;
                startBoxW = boxW; startBoxH = boxH;

                if      (hit(tx, ty, r,  t))  dragMode = DragMode.RESIZE_TL;
                else if (hit(tx, ty, ri, t))  dragMode = DragMode.RESIZE_TR;
                else if (hit(tx, ty, r,  b))  dragMode = DragMode.RESIZE_BL;
                else if (hit(tx, ty, ri, b))  dragMode = DragMode.RESIZE_BR;
                else if (tx > r && tx < ri && ty > t && ty < b)
                    dragMode = DragMode.MOVE;
                else dragMode = DragMode.NONE;
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = tx - touchStartX, dy = ty - touchStartY;
                switch (dragMode) {
                    case MOVE:
                        boxX = startBoxX + dx;
                        boxY = startBoxY + dy;
                        break;
                    case RESIZE_TL:
                        boxX = Math.min(startBoxX+dx, startBoxX+startBoxW-MIN_SIZE);
                        boxY = Math.min(startBoxY+dy, startBoxY+startBoxH-MIN_SIZE);
                        boxW = Math.max(MIN_SIZE, startBoxW - dx);
                        boxH = Math.max(MIN_SIZE, startBoxH - dy);
                        break;
                    case RESIZE_TR:
                        boxY = Math.min(startBoxY+dy, startBoxY+startBoxH-MIN_SIZE);
                        boxW = Math.max(MIN_SIZE, startBoxW + dx);
                        boxH = Math.max(MIN_SIZE, startBoxH - dy);
                        break;
                    case RESIZE_BL:
                        boxX = Math.min(startBoxX+dx, startBoxX+startBoxW-MIN_SIZE);
                        boxW = Math.max(MIN_SIZE, startBoxW - dx);
                        boxH = Math.max(MIN_SIZE, startBoxH + dy);
                        break;
                    case RESIZE_BR:
                        boxW = Math.max(MIN_SIZE, startBoxW + dx);
                        boxH = Math.max(MIN_SIZE, startBoxH + dy);
                        break;
                }
                notifyListener();
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                dragMode = DragMode.NONE;
                notifyListener();
                return true;
        }
        return false;
    }

    private boolean hit(float tx, float ty, float cx, float cy) {
        float dx = tx-cx, dy = ty-cy;
        return dx*dx + dy*dy <= HANDLE_R * HANDLE_R;
    }

    private void notifyListener() {
        if (listener != null)
            listener.onGridChanged((int)boxX, (int)boxY, (int)boxW, (int)boxH);
    }

    public float getBoxX() { return boxX; }
    public float getBoxY() { return boxY; }
    public float getBoxW() { return boxW; }
    public float getBoxH() { return boxH; }
}
