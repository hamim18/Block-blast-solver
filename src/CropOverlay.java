package com.blockblast.solver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * CropOverlay — overlay di atas foto yang menampilkan:
 * - Titik sudut yang sudah di-tap (lingkaran bercahaya)
 * - Kotak seleksi + grid 8×8 setelah kedua sudut dipilih
 */
public class CropOverlay extends View {

    private PointF vCorner1, vCorner2; // koordinat dalam view-space

    private Paint dotPaint, dotBorderPaint;
    private Paint rectPaint, gridPaint;
    private Paint labelPaint;

    public CropOverlay(Context context) { super(context); init(); }
    public CropOverlay(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.parseColor("#89B4FA"));
        dotPaint.setStyle(Paint.Style.FILL);

        dotBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotBorderPaint.setColor(Color.WHITE);
        dotBorderPaint.setStyle(Paint.Style.STROKE);
        dotBorderPaint.setStrokeWidth(3f);

        rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint.setColor(Color.parseColor("#89B4FA"));
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(3f);

        gridPaint = new Paint();
        gridPaint.setColor(Color.argb(100, 137, 180, 250));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(34f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * Set sudut dalam koordinat bitmap, otomatis dikonversi ke view-space.
     * corner2 boleh null (baru 1 sudut yang dipilih).
     */
    public void setCorners(PointF vc1, PointF vc2, int viewW, int viewH, int bmpW, int bmpH) {
        vCorner1 = vc1;
        vCorner2 = vc2;
        invalidate();
    }

    public void reset() {
        vCorner1 = null;
        vCorner2 = null;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (vCorner1 == null) return;

        float r = 18f;

        // Titik sudut 1
        canvas.drawCircle(vCorner1.x, vCorner1.y, r, dotPaint);
        canvas.drawCircle(vCorner1.x, vCorner1.y, r, dotBorderPaint);
        canvas.drawText("1", vCorner1.x, vCorner1.y - r - 8f, labelPaint);

        if (vCorner2 == null) return;

        // Titik sudut 2
        canvas.drawCircle(vCorner2.x, vCorner2.y, r, dotPaint);
        canvas.drawCircle(vCorner2.x, vCorner2.y, r, dotBorderPaint);
        canvas.drawText("2", vCorner2.x, vCorner2.y - r - 8f, labelPaint);

        // Kotak seleksi
        float left  = Math.min(vCorner1.x, vCorner2.x);
        float top   = Math.min(vCorner1.y, vCorner2.y);
        float right = Math.max(vCorner1.x, vCorner2.x);
        float bot   = Math.max(vCorner1.y, vCorner2.y);
        canvas.drawRect(left, top, right, bot, rectPaint);

        // Grid 8×8
        float cellW = (right - left) / 8f;
        float cellH = (bot   - top)  / 8f;
        for (int i = 1; i < 8; i++) {
            canvas.drawLine(left + i * cellW, top, left + i * cellW, bot, gridPaint);
            canvas.drawLine(left, top + i * cellH, right, top + i * cellH, gridPaint);
        }
    }
}
