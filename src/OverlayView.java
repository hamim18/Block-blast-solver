package com.blockblast.solver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * OverlayView — panduan crop di atas preview kamera.
 *
 * Kotak panduan sedikit portrait (rasio ~1:1.08) sesuai dengan
 * area yang benar-benar di-crop oleh GridDetector.
 * Grid 8×8 tipis di dalamnya membantu user menyelaraskan posisi.
 */
public class OverlayView extends View {

    // Rasio tinggi/lebar kotak panduan — sesuaikan dengan GridDetector
    // Block Blast grid di layar: sedikit lebih tinggi dari lebar
    private static final float GUIDE_RATIO = 1.08f;

    private Paint vignettePaint;
    private Paint borderPaint;
    private Paint cornerPaint;
    private Paint gridLinePaint;
    private Paint labelPaint;

    public OverlayView(Context context) { super(context); init(); }
    public OverlayView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        vignettePaint = new Paint();
        vignettePaint.setColor(Color.argb(160, 0, 0, 0));
        vignettePaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.argb(200, 137, 180, 250));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);

        cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cornerPaint.setColor(Color.parseColor("#89B4FA"));
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(5f);
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);

        gridLinePaint = new Paint();
        gridLinePaint.setColor(Color.argb(80, 137, 180, 250));
        gridLinePaint.setStyle(Paint.Style.STROKE);
        gridLinePaint.setStrokeWidth(1f);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(38f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int W = getWidth();
        int H = getHeight();

        // Lebar kotak = 82% lebar layar; tinggi mengikuti GUIDE_RATIO
        float guideW = W * 0.82f;
        float guideH = guideW * GUIDE_RATIO;

        // Pusatkan secara vertikal, tapi geser sedikit ke atas agar label muat
        float left = (W - guideW) / 2f;
        float top  = (H - guideH) / 2f - 24f;
        float right = left + guideW;
        float bot   = top  + guideH;

        // Vignette
        Path vigPath = new Path();
        vigPath.addRect(0, 0, W, H, Path.Direction.CW);
        vigPath.addRoundRect(new RectF(left, top, right, bot), 14f, 14f, Path.Direction.CCW);
        canvas.drawPath(vigPath, vignettePaint);

        // Border
        canvas.drawRoundRect(new RectF(left, top, right, bot), 12f, 12f, borderPaint);

        // Grid 8×8
        float cellW = guideW / 8f;
        float cellH = guideH / 8f;
        for (int i = 1; i < 8; i++) {
            canvas.drawLine(left + i * cellW, top, left + i * cellW, bot, gridLinePaint);
            canvas.drawLine(left, top + i * cellH, right, top + i * cellH, gridLinePaint);
        }

        // Sudut bercahaya
        float cl = guideW * 0.07f;
        drawCorner(canvas, left,  top,  cl, 0);
        drawCorner(canvas, right, top,  cl, 90);
        drawCorner(canvas, right, bot,  cl, 180);
        drawCorner(canvas, left,  bot,  cl, 270);

        // Label
        canvas.drawText("Arahkan kotak ke grid game", W / 2f, bot + 50f, labelPaint);
    }

    private void drawCorner(Canvas canvas, float cx, float cy, float len, float angleDeg) {
        canvas.save();
        canvas.rotate(angleDeg, cx, cy);
        canvas.drawLine(cx, cy, cx + len, cy, cornerPaint);
        canvas.drawLine(cx, cy, cx, cy + len, cornerPaint);
        canvas.restore();
    }
}
