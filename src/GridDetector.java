package com.blockblast.solver;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * GridDetector — klasifikasi sel 8×8 dari bitmap yang sudah di-crop user.
 * Karena user menentukan sendiri area grid via tap 2 sudut, kita tidak perlu
 * lagi mencari batas grid secara otomatis. Cukup scale ke 400×400 lalu
 * baca brightness tiap sel.
 */
public class GridDetector {

    private static final int GRID  = 8;
    private static final int WORK  = 400;
    private static final int CELL  = WORK / GRID; // 50px per sel

    public static class DetectionResult {
        public final boolean[][] grid;
        public final boolean success;
        public final String message;
        DetectionResult(boolean[][] g, boolean ok, String msg) { grid=g; success=ok; message=msg; }
    }

    /** Terima bitmap crop area grid langsung dari user. */
    public static DetectionResult detect(Bitmap cropped) {
        if (cropped == null) return fail("Bitmap null");

        Bitmap scaled = Bitmap.createScaledBitmap(cropped, WORK, WORK, true);

        // Threshold adaptif: rata-rata brightness + offset
        double avg = averageBrightness(scaled);
        double threshold = Math.max(50, Math.min(160, avg + 40));

        boolean[][] grid = new boolean[GRID][GRID];
        for (int r = 0; r < GRID; r++)
            for (int c = 0; c < GRID; c++)
                grid[r][c] = isFilled(scaled, r, c, threshold);

        scaled.recycle();
        return new DetectionResult(grid, true, "OK");
    }

    private static boolean isFilled(Bitmap bmp, int row, int col, double threshold) {
        float margin = CELL * 0.2f;
        float x0 = col * CELL + margin, y0 = row * CELL + margin;
        float x1 = (col+1)*CELL - margin, y1 = (row+1)*CELL - margin;
        float sx = (x1-x0)/3f, sy = (y1-y0)/3f;
        double total = 0; int n = 0;
        for (int dy=0; dy<3; dy++) for (int dx=0; dx<3; dx++) {
            int px = Math.min(WORK-1, (int)(x0+dx*sx+sx/2f));
            int py = Math.min(WORK-1, (int)(y0+dy*sy+sy/2f));
            total += brightness(bmp.getPixel(px,py)); n++;
        }
        return n>0 && (total/n) > threshold;
    }

    private static double averageBrightness(Bitmap bmp) {
        long t=0; int c=0;
        for (int y=0; y<WORK; y+=8) for (int x=0; x<WORK; x+=8) { t+=brightness(bmp.getPixel(x,y)); c++; }
        return c>0 ? (double)t/c : 80;
    }

    private static int brightness(int pixel) {
        return (int)(0.2126*Color.red(pixel)+0.7152*Color.green(pixel)+0.0722*Color.blue(pixel));
    }

    private static DetectionResult fail(String m) { return new DetectionResult(null,false,m); }
}
