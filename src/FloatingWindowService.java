package com.blockblast.solver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FloatingWindowService extends Service {

    // Warna tiap piece
    private static final int COLOR_P1 = Color.parseColor("#89B4FA"); // biru
    private static final int COLOR_P2 = Color.parseColor("#A6E3A1"); // hijau
    private static final int COLOR_P3 = Color.parseColor("#FAB387"); // orange

    // ── Window ────────────────────────────────────────────────────────────
    private WindowManager windowManager;
    private View overlayView;
    private View fabView;
    private boolean isExpanded = true;
    private WindowManager.LayoutParams overlayParams, fabParams;

    // ── Panels ────────────────────────────────────────────────────────────
    private View mainPanel, solvePanel, editGridPanel;
    private GridEditOverlay gridEditOverlay;

    // ── Main panel views ──────────────────────────────────────────────────
    private GridView    gridView;
    private MiniGridView mini1, mini2, mini3;
    private Button      btnClearGrid, btnClearPieces, btnSolve;
    private TextView    tvSolveStatus;

    // ── Solve panel views ─────────────────────────────────────────────────
    private GridView solutionGrid;
    private Button   btnPlayPause, btnNext, btnRestart, btnNextRound;
    private TextView tvStep, tvScore, tvSolveTitle;

    // ── Solver state ──────────────────────────────────────────────────────
    private List<PlacementStep> solution    = new ArrayList<>();
    private boolean[][]         initialGrid = new boolean[8][8];
    private boolean[][]         animGrid    = new boolean[8][8];
    private boolean[][]         finalGrid   = new boolean[8][8];
    private int     currentStep = 0;
    private boolean isPlaying   = false;
    private boolean isAnimating = false;
    private int     animLines   = 0;

    private final Handler       handler  = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final String CHANNEL_ID = "FloatingOverlayChannel";
    private static final int    NOTIF_ID   = 1;

    @Nullable @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundNotification();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        setupOverlay();
        setupFab();
    }

    private void setupOverlay() {
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_main, null);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        overlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        overlayParams.gravity = Gravity.TOP | Gravity.START;
        overlayParams.alpha   = 0.93f;

        windowManager.addView(overlayView, overlayParams);
        bindViews();
    }

    private void setupFab() {
        fabView = LayoutInflater.from(this).inflate(R.layout.overlay_fab, null);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        fabParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        fabParams.gravity = Gravity.TOP | Gravity.END;
        fabParams.x = 16; fabParams.y = 200;

        fabView.setVisibility(View.GONE);
        windowManager.addView(fabView, fabParams);

        fabView.setOnTouchListener(new DragTouchListener(fabView, fabParams, windowManager) {
            @Override protected void onClick() { expand(); }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    private void bindViews() {
        mainPanel       = overlayView.findViewById(R.id.mainPanel);
        solvePanel      = overlayView.findViewById(R.id.solvePanel);
        editGridPanel   = overlayView.findViewById(R.id.editGridPanel);
        gridEditOverlay = overlayView.findViewById(R.id.gridEditOverlay);

        // ── Main panel ────────────────────────────────────────────────────
        gridView       = overlayView.findViewById(R.id.gridView);
        mini1          = overlayView.findViewById(R.id.mini1);
        mini2          = overlayView.findViewById(R.id.mini2);
        mini3          = overlayView.findViewById(R.id.mini3);
        btnClearGrid   = overlayView.findViewById(R.id.btnClearGrid);
        btnClearPieces = overlayView.findViewById(R.id.btnClearPieces);
        btnSolve       = overlayView.findViewById(R.id.btnSolve);
        tvSolveStatus  = overlayView.findViewById(R.id.tvSolveStatus);

        // ── Solve panel ───────────────────────────────────────────────────
        solutionGrid = overlayView.findViewById(R.id.solutionGrid);
        btnPlayPause = overlayView.findViewById(R.id.btnPlayPause);
        btnNext      = overlayView.findViewById(R.id.btnNext);
        btnRestart   = overlayView.findViewById(R.id.btnRestart);
        btnNextRound = overlayView.findViewById(R.id.btnNextRound);
        tvStep       = overlayView.findViewById(R.id.tvStep);
        tvScore      = overlayView.findViewById(R.id.tvScore);
        tvSolveTitle = overlayView.findViewById(R.id.tvSolveTitle);

        // Semua view sudah siap — baru panggil showPanel & applyGridPosition
        showPanel(mainPanel);
        overlayView.post(this::applyGridPosition);

        // Edit grid
        gridEditOverlay.setListener((x, y, w, h) -> applyGridBoundsToView(x, y, w, h));
        overlayView.findViewById(R.id.btnResetGridPos).setOnClickListener(v -> {
            GridPositionManager.reset(this);
            overlayView.post(this::applyGridPosition);
            Toast.makeText(this, "Posisi grid direset", Toast.LENGTH_SHORT).show();
        });
        overlayView.findViewById(R.id.btnSaveGridPos).setOnClickListener(v -> {
            GridPositionManager.save(this,
                    (int) gridEditOverlay.getBoxX(), (int) gridEditOverlay.getBoxY(),
                    (int) gridEditOverlay.getBoxW(), (int) gridEditOverlay.getBoxH());
            applyGridBoundsToView(
                    (int) gridEditOverlay.getBoxX(), (int) gridEditOverlay.getBoxY(),
                    (int) gridEditOverlay.getBoxW(), (int) gridEditOverlay.getBoxH());
            showPanel(mainPanel);
            overlayParams.alpha = 0.93f;
            windowManager.updateViewLayout(overlayView, overlayParams);
            Toast.makeText(this, "Posisi disimpan!", Toast.LENGTH_SHORT).show();
        });

        MiniGridView.OnPieceChangedListener pieceWatcher = hasCells -> updateSolveButton();
        mini1.setOnPieceChangedListener(pieceWatcher);
        mini2.setOnPieceChangedListener(pieceWatcher);
        mini3.setOnPieceChangedListener(pieceWatcher);

        updateSolveButton();

        overlayView.findViewById(R.id.btnMinimize).setOnClickListener(v -> minimize());
        overlayView.findViewById(R.id.btnEditGrid).setOnClickListener(v -> openEditGrid());

        btnClearGrid.setOnClickListener(v -> {
            gridView.clearGrid();
            Toast.makeText(this, "Grid dibersihkan", Toast.LENGTH_SHORT).show();
        });
        btnClearPieces.setOnClickListener(v -> {
            mini1.clearGrid(); mini2.clearGrid(); mini3.clearGrid();
            updateSolveButton();
            Toast.makeText(this, "Piece dibersihkan", Toast.LENGTH_SHORT).show();
        });

        btnSolve.setOnClickListener(v -> runSolver());

        solutionGrid.setInteractive(false);

        overlayView.findViewById(R.id.btnBackFromSolve).setOnClickListener(v -> showPanel(mainPanel));
        overlayView.findViewById(R.id.btnMinimize2).setOnClickListener(v -> minimize());

        btnPlayPause.setOnClickListener(v -> {
            isPlaying = !isPlaying;
            btnPlayPause.setText(isPlaying ? "⏸" : "▶");
            if (isPlaying) scheduleNext();
        });
        btnNext.setOnClickListener(v -> {
            if (isAnimating) return;
            isPlaying = false;
            btnPlayPause.setText("▶");
            handler.removeCallbacksAndMessages(null);
            showNextStep();
        });
        btnRestart.setOnClickListener(v -> {
            isPlaying = false;
            btnPlayPause.setText("▶");
            handler.removeCallbacksAndMessages(null);
            currentStep = 0; animLines = 0;
            for (int r = 0; r < 8; r++) animGrid[r] = initialGrid[r].clone();
            solutionGrid.setGrid(animGrid);
            solutionGrid.clearColors();
            btnNextRound.setVisibility(View.GONE);
            updateStepLabel();
            tvScore.setText("Lines: 0");
        });
        btnNextRound.setOnClickListener(v -> {
            // Grid 8×8 diisi hasil solve, ketiga mini-grid dikosongkan
            gridView.setGrid(finalGrid);
            // Reset animGrid agar solve berikutnya mulai dari state bersih
            for (int r = 0; r < 8; r++) animGrid[r] = finalGrid[r].clone();
            mini1.clearGrid();
            mini2.clearGrid();
            mini3.clearGrid();
            updateSolveButton();
            showPanel(mainPanel);
        });
    }

    // ── Solve button aktif hanya jika ketiga mini-grid berisi ─────────────
    private void updateSolveButton() {
        boolean ready = mini1.hasAnyCells() && mini2.hasAnyCells() && mini3.hasAnyCells();
        btnSolve.setEnabled(ready);
        btnSolve.setAlpha(ready ? 1.0f : 0.4f);
    }

    private void showPanel(View panel) {
        mainPanel.setVisibility(View.GONE);
        solvePanel.setVisibility(View.GONE);
        editGridPanel.setVisibility(View.GONE);
        panel.setVisibility(View.VISIBLE);

        // gridView tampil di mainPanel saja, solutionGrid di solvePanel, keduanya hidden saat edit
        if (panel == solvePanel) {
            gridView.setVisibility(View.GONE);
            solutionGrid.setVisibility(View.VISIBLE);
        } else if (panel == editGridPanel) {
            gridView.setVisibility(View.GONE);  // sembunyikan agar tidak double border dengan GridEditOverlay
            solutionGrid.setVisibility(View.GONE);
        } else { // mainPanel
            gridView.setVisibility(View.VISIBLE);
            solutionGrid.setVisibility(View.GONE);
        }
    }

    private void openEditGrid() {
        overlayParams.alpha = 0.6f;
        windowManager.updateViewLayout(overlayView, overlayParams);
        int[] loc = new int[2];
        gridView.getLocationInWindow(loc);
        gridEditOverlay.setBox(loc[0], loc[1], gridView.getWidth(), gridView.getHeight());
        showPanel(editGridPanel);
    }

    private void applyGridBoundsToView(int x, int y, int w, int h) {
        // gridView & solutionGrid kini ada di root FrameLayout → pakai FrameLayout.LayoutParams
        android.widget.FrameLayout.LayoutParams lp =
                new android.widget.FrameLayout.LayoutParams(w, h);
        lp.leftMargin = x;
        lp.topMargin  = y;
        gridView.setLayoutParams(lp);

        // solutionGrid ikut posisi yang sama
        android.widget.FrameLayout.LayoutParams lp2 =
                new android.widget.FrameLayout.LayoutParams(w, h);
        lp2.leftMargin = x;
        lp2.topMargin  = y;
        solutionGrid.setLayoutParams(lp2);
    }

    private void applyGridPosition() {
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        int sw = dm.widthPixels, sh = dm.heightPixels;
        GridPositionManager.GridBounds b = GridPositionManager.load(this, sw, sh);

        android.widget.FrameLayout.LayoutParams lp =
                new android.widget.FrameLayout.LayoutParams(b.width, b.height);
        lp.leftMargin = b.x;
        lp.topMargin  = b.y;
        gridView.setLayoutParams(lp);

        android.widget.FrameLayout.LayoutParams lp2 =
                new android.widget.FrameLayout.LayoutParams(b.width, b.height);
        lp2.leftMargin = b.x;
        lp2.topMargin  = b.y;
        solutionGrid.setLayoutParams(lp2);

        gridEditOverlay.setBox(b.x, b.y, b.width, b.height);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Solver
    // ══════════════════════════════════════════════════════════════════════
    private void runSolver() {
        // Konversi ketiga mini-grid → Block (bounding box crop otomatis)
        Block b1 = mini1.toBlock("P1", COLOR_P1);
        Block b2 = mini2.toBlock("P2", COLOR_P2);
        Block b3 = mini3.toBlock("P3", COLOR_P3);

        if (b1 == null || b2 == null || b3 == null) {
            Toast.makeText(this, "Isi semua 3 piece dulu!", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Block> pieces = new ArrayList<>();
        pieces.add(b1); pieces.add(b2); pieces.add(b3);

        boolean[][] grid = gridView.getGrid();
        for (int r = 0; r < 8; r++) {
            initialGrid[r] = grid[r].clone();
            animGrid[r]    = grid[r].clone();
        }

        tvSolveStatus.setText("Solving…");
        tvSolveStatus.setVisibility(View.VISIBLE);
        btnSolve.setEnabled(false);

        executor.execute(() -> {
            BlockBlastSolver solver = new BlockBlastSolver(initialGrid, pieces);
            List<PlacementStep> result = solver.solve();
            int linesCleared = solver.getBestLinesCleared();
            int placed       = solver.getBestPiecesPlaced();
            int total        = solver.getTotalPieces();
            boolean timedOut = solver.isTimedOut();

            handler.post(() -> {
                tvSolveStatus.setVisibility(View.GONE);
                btnSolve.setEnabled(true);

                if (result == null || result.isEmpty()) {
                    Toast.makeText(this, "Tidak ada posisi yang muat!", Toast.LENGTH_SHORT).show();
                    return;
                }

                solution = result;
                currentStep = 0; animLines = 0; isPlaying = false;
                solutionGrid.setGrid(animGrid);
                solutionGrid.clearColors();
                btnPlayPause.setText("▶");
                btnNextRound.setVisibility(View.GONE);
                updateStepLabel();
                tvScore.setText("Lines: 0");

                String status = placed < total
                        ? "⚠ " + placed + "/" + total + " piece muat"
                        : "✓ Semua " + total + " piece!";
                if (timedOut) status += " (timeout)";
                tvSolveTitle.setText("Solusi — " + status);

                showPanel(solvePanel);
            });
        });
    }

    private void scheduleNext() {
        handler.postDelayed(() -> { if (isPlaying) showNextStep(); }, 800);
    }

    private void showNextStep() {
        if (currentStep >= solution.size()) {
            isPlaying = false;
            btnPlayPause.setText("▶");
            for (int r = 0; r < 8; r++) finalGrid[r] = animGrid[r].clone();
            btnNextRound.setVisibility(View.VISIBLE);
            return;
        }
        PlacementStep step  = solution.get(currentStep);
        Block         block = step.block;
        for (int r = 0; r < block.getRows(); r++)
            for (int c = 0; c < block.getCols(); c++)
                if (block.getShape()[r][c]) {
                    animGrid[step.row + r][step.col + c] = true;
                    solutionGrid.setCellColor(step.row + r, step.col + c, block.getColor());
                }
        currentStep++;
        updateStepLabel();

        if (hasFullLines()) {
            isAnimating = true;
            handler.postDelayed(() -> {
                int cl = clearFullLines();
                animLines += cl;
                tvScore.setText("Lines: " + animLines);
                isAnimating = false;
                if (isPlaying) {
                    if (currentStep < solution.size()) scheduleNext();
                    else { isPlaying = false; btnPlayPause.setText("▶"); }
                }
            }, 500);
        } else {
            if (isPlaying) {
                if (currentStep < solution.size()) scheduleNext();
                else { isPlaying = false; btnPlayPause.setText("▶"); }
            }
        }
    }

    private boolean hasFullLines() {
        for (int r = 0; r < 8; r++) {
            boolean f = true;
            for (int c = 0; c < 8; c++) if (!animGrid[r][c]) { f = false; break; }
            if (f) return true;
        }
        for (int c = 0; c < 8; c++) {
            boolean f = true;
            for (int r = 0; r < 8; r++) if (!animGrid[r][c]) { f = false; break; }
            if (f) return true;
        }
        return false;
    }

    private int clearFullLines() {
        boolean[] fr = new boolean[8], fc = new boolean[8];
        int n = 0;
        for (int r = 0; r < 8; r++) {
            boolean f = true;
            for (int c = 0; c < 8; c++) if (!animGrid[r][c]) { f = false; break; }
            fr[r] = f;
        }
        for (int c = 0; c < 8; c++) {
            boolean f = true;
            for (int r = 0; r < 8; r++) if (!animGrid[r][c]) { f = false; break; }
            fc[c] = f;
        }
        for (int r = 0; r < 8; r++) if (fr[r]) { for (int c = 0; c < 8; c++) animGrid[r][c] = false; n++; }
        for (int c = 0; c < 8; c++) if (fc[c]) { for (int r = 0; r < 8; r++) animGrid[r][c] = false; n++; }
        solutionGrid.clearColors();
        solutionGrid.setGrid(animGrid);
        return n;
    }

    private void updateStepLabel() {
        tvStep.setText("Step " + currentStep + "/" + solution.size());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Minimize / Expand
    // ══════════════════════════════════════════════════════════════════════
    public void minimize() {
        if (!isExpanded) return;
        isExpanded = false;
        overlayView.setVisibility(View.GONE);
        fabView.setVisibility(View.VISIBLE);
    }

    public void expand() {
        if (isExpanded) return;
        isExpanded = true;
        fabView.setVisibility(View.GONE);
        overlayView.setVisibility(View.VISIBLE);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Foreground notification
    // ══════════════════════════════════════════════════════════════════════
    private void startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Block Blast Overlay", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
        Intent stopIntent = new Intent(this, FloatingWindowService.class);
        stopIntent.setAction("STOP");
        PendingIntent pi = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        // NotificationCompat aman di semua API level
        Notification notif = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Block Blast Solver")
                .setContentText("Overlay aktif")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .addAction(android.R.drawable.ic_delete, "Stop", pi)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIF_ID, notif);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) stopSelf();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Shutdown executor — batalkan task yang pending, tunggu yang sedang berjalan
        executor.shutdownNow();
        handler.removeCallbacksAndMessages(null);
        try { if (overlayView != null) windowManager.removeView(overlayView); }
        catch (Exception ignored) { /* view sudah detached */ }
        try { if (fabView != null) windowManager.removeView(fabView); }
        catch (Exception ignored) { /* view sudah detached */ }
        super.onDestroy();
    }
}
