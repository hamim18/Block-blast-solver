package com.blockblast.solver;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * GridPositionManager — simpan & load posisi/ukuran grid overlay
 * ke SharedPreferences.
 */
public class GridPositionManager {

    private static final String PREFS   = "grid_position";
    private static final String KEY_X   = "grid_x";
    private static final String KEY_Y   = "grid_y";
    private static final String KEY_W   = "grid_w";
    private static final String KEY_H   = "grid_h";

    public static class GridBounds {
        public int x, y, width, height;
        public GridBounds(int x, int y, int w, int h) {
            this.x = x; this.y = y; this.width = w; this.height = h;
        }
    }

    /** Simpan posisi & ukuran grid */
    public static void save(Context ctx, int x, int y, int w, int h) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_X, x).putInt(KEY_Y, y)
                .putInt(KEY_W, w).putInt(KEY_H, h)
                .apply();
    }

    /**
     * Load posisi & ukuran grid.
     * Jika belum pernah disimpan, kembalikan default (tengah layar, 80% lebar).
     */
    public static GridBounds load(Context ctx, int screenW, int screenH) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_X)) {
            // Default: persegi 80% lebar, di tengah layar bagian atas
            int size = (int)(screenW * 0.80f);
            int x    = (screenW - size) / 2;
            int y    = (int)(screenH * 0.18f);
            return new GridBounds(x, y, size, size);
        }
        return new GridBounds(
                prefs.getInt(KEY_X, 0),
                prefs.getInt(KEY_Y, 0),
                prefs.getInt(KEY_W, 300),
                prefs.getInt(KEY_H, 300));
    }

    /** Reset ke default */
    public static void reset(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
