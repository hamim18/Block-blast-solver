package com.blockblast.solver;

import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * DragTouchListener — bikin view bisa di-drag di WindowManager.
 * Override onClick() untuk handle single tap.
 */
public abstract class DragTouchListener implements View.OnTouchListener {

    private final View                    view;
    private final WindowManager.LayoutParams params;
    private final WindowManager           wm;

    private int   lastX, lastY;
    private int   startParamsX, startParamsY;
    private float startTouchX, startTouchY;
    private boolean isDragging = false;
    private static final int DRAG_THRESHOLD = 10;

    public DragTouchListener(View view, WindowManager.LayoutParams params, WindowManager wm) {
        this.view   = view;
        this.params = params;
        this.wm     = wm;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startTouchX  = event.getRawX();
                startTouchY  = event.getRawY();
                startParamsX = params.x;
                startParamsY = params.y;
                isDragging   = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - startTouchX;
                float dy = event.getRawY() - startTouchY;
                if (!isDragging && (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD))
                    isDragging = true;
                if (isDragging) {
                    params.x = startParamsX + (int) dx;
                    params.y = startParamsY + (int) dy;
                    wm.updateViewLayout(view, params);
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (!isDragging) onClick();
                return true;
        }
        return false;
    }

    protected abstract void onClick();
}
