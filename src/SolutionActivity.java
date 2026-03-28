package com.blockblast.solver;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SolutionActivity extends AppCompatActivity {

    private GridView gridView;
    private Button btnPlayPause, btnNext, btnRestart, btnNextRound;
    private TextView tvStatus, tvStep, tvScore;
    private ProgressBar progressBar;

    // Grid akhir setelah semua piece ditempatkan + lines di-clear
    private boolean[][] finalGrid = new boolean[8][8];

    private boolean[][] initialGrid = new boolean[8][8];
    private List<PlacementStep> solution = new ArrayList<>();
    private int currentStep = 0;
    private boolean isPlaying = false;
    private boolean[][] animGrid = new boolean[8][8];

    private Handler handler = new Handler();
    private static final long ANIM_DELAY = 800L;
    private static final long CLEAR_DELAY = 500L;
    private boolean isAnimating = false;

    // Skor animasi — dihitung ulang saat animasi berjalan
    private int animLinesCleared = 0;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solution);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Solution");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        gridView      = findViewById(R.id.solutionGridView);
        btnPlayPause  = findViewById(R.id.btnPlayPause);
        btnNext       = findViewById(R.id.btnNext);
        btnRestart    = findViewById(R.id.btnRestart);
        btnNextRound  = findViewById(R.id.btnNextRound);
        tvStatus      = findViewById(R.id.tvStatus);
        tvStep        = findViewById(R.id.tvStep);
        tvScore       = findViewById(R.id.tvScore);
        progressBar   = findViewById(R.id.progressBar);

        gridView.setInteractive(false);

        String gridStr = getIntent().getStringExtra("grid");
        ArrayList<String> pieceNames = getIntent().getStringArrayListExtra("piece_names");

        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                initialGrid[r][c] = gridStr.charAt(r * 8 + c) == '1';

        for (int r = 0; r < 8; r++) animGrid[r] = initialGrid[r].clone();
        gridView.setGrid(animGrid);

        List<Block> pieces = new ArrayList<>();
        List<Block> all = BlockLibrary.getAllBlocks();
        for (String name : pieceNames) {
            for (Block b : all) {
                if (b.getName().equals(name)) { pieces.add(b); break; }
            }
        }

        tvStatus.setText("Solving...");
        tvScore.setText("");
        progressBar.setVisibility(View.VISIBLE);
        btnPlayPause.setEnabled(false);
        btnNext.setEnabled(false);
        btnRestart.setEnabled(false);

        final List<Block> finalPieces = pieces;
        executor.execute(() -> {
            BlockBlastSolver solver = new BlockBlastSolver(initialGrid, finalPieces);
            List<PlacementStep> result = solver.solve();
            int linesCleared  = solver.getBestLinesCleared();
            int piecesPlaced  = solver.getBestPiecesPlaced();
            int totalPieces   = solver.getTotalPieces();

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (result == null || result.isEmpty()) {
                    tvStatus.setText("Game Over! Tidak ada piece yang bisa ditempatkan.");
                    tvScore.setText("");
                } else {
                    solution = result;

                    // ── Status: tampilkan partial warning jika tidak semua piece muat ──
                    if (piecesPlaced < totalPieces) {
                        tvStatus.setText("⚠ Solusi terbaik: " + piecesPlaced + "/" + totalPieces
                                + " piece (sebagian tidak muat)");
                    } else {
                        tvStatus.setText("✓ Semua " + totalPieces + " piece dapat ditempatkan!");
                    }

                    // ── Skor: tampilkan lines cleared ──
                    updateScoreLabel(linesCleared);

                    btnPlayPause.setEnabled(true);
                    btnNext.setEnabled(true);
                    btnRestart.setEnabled(true);
                    updateStepLabel();
                }
            });
        });

        btnPlayPause.setOnClickListener(v -> {
            isPlaying = !isPlaying;
            btnPlayPause.setText(isPlaying ? "Pause" : "Play");
            if (isPlaying) scheduleNext();
        });

        btnNext.setOnClickListener(v -> {
            if (isAnimating) return;
            isPlaying = false;
            btnPlayPause.setText("Play");
            handler.removeCallbacksAndMessages(null);
            showNextStep();
        });

        btnRestart.setOnClickListener(v -> {
            isPlaying = false;
            btnPlayPause.setText("Play");
            handler.removeCallbacksAndMessages(null);
            currentStep = 0;
            animLinesCleared = 0;
            for (int r = 0; r < 8; r++) animGrid[r] = initialGrid[r].clone();
            gridView.setGrid(animGrid);
            gridView.clearColors();
            btnNextRound.setVisibility(View.GONE);
            updateStepLabel();
            updateScoreLabel(0);
        });

        btnNextRound.setOnClickListener(v -> {
            // Kirim grid akhir kembali ke MainActivity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("final_grid", gridToString(finalGrid));
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void scheduleNext() {
        handler.postDelayed(() -> {
            if (!isPlaying) return;
            showNextStep();
        }, ANIM_DELAY);
    }

    private void showNextStep() {
        if (currentStep >= solution.size()) {
            tvStatus.setText("Semua piece sudah ditempatkan!");
            isPlaying = false;
            btnPlayPause.setText("Play");
            // Simpan state grid final dan tampilkan tombol Next Round
            for (int r = 0; r < 8; r++) finalGrid[r] = animGrid[r].clone();
            btnNextRound.setVisibility(View.VISIBLE);
            return;
        }
        PlacementStep step = solution.get(currentStep);
        Block block = step.block;
        boolean[][] shape = block.getShape();
        int color = block.getColor();

        // Tempatkan piece dan beri warna
        for (int r = 0; r < block.getRows(); r++) {
            for (int c = 0; c < block.getCols(); c++) {
                if (shape[r][c]) {
                    animGrid[step.row + r][step.col + c] = true;
                    gridView.setCellColor(step.row + r, step.col + c, color);
                }
            }
        }

        currentStep++;
        updateStepLabel();
        tvStatus.setText("Placed: " + block.getName() + " at (" + step.row + "," + step.col + ")");

        if (hasFullLines()) {
            isAnimating = true;
            handler.postDelayed(() -> {
                int cleared = clearFullLines();
                animLinesCleared += cleared;
                updateScoreLabel(animLinesCleared);
                isAnimating = false;
                if (isPlaying) {
                    if (currentStep < solution.size()) scheduleNext();
                    else { isPlaying = false; btnPlayPause.setText("Play"); }
                }
            }, CLEAR_DELAY);
        } else {
            if (isPlaying) {
                if (currentStep < solution.size()) scheduleNext();
                else { isPlaying = false; btnPlayPause.setText("Play"); }
            }
        }
    }

    private boolean hasFullLines() {
        for (int r = 0; r < 8; r++) {
            boolean full = true;
            for (int c = 0; c < 8; c++) if (!animGrid[r][c]) { full = false; break; }
            if (full) return true;
        }
        for (int c = 0; c < 8; c++) {
            boolean full = true;
            for (int r = 0; r < 8; r++) if (!animGrid[r][c]) { full = false; break; }
            if (full) return true;
        }
        return false;
    }

    /** Clear semua baris/kolom penuh sekaligus, return jumlah yang di-clear */
    private int clearFullLines() {
        boolean[] fullRows = new boolean[8];
        boolean[] fullCols = new boolean[8];
        int cleared = 0;

        for (int r = 0; r < 8; r++) {
            boolean full = true;
            for (int c = 0; c < 8; c++) if (!animGrid[r][c]) { full = false; break; }
            fullRows[r] = full;
        }
        for (int c = 0; c < 8; c++) {
            boolean full = true;
            for (int r = 0; r < 8; r++) if (!animGrid[r][c]) { full = false; break; }
            fullCols[c] = full;
        }

        for (int r = 0; r < 8; r++)
            if (fullRows[r]) { for (int c = 0; c < 8; c++) animGrid[r][c] = false; cleared++; }
        for (int c = 0; c < 8; c++)
            if (fullCols[c]) { for (int r = 0; r < 8; r++) animGrid[r][c] = false; cleared++; }

        gridView.clearColors();
        gridView.setGrid(animGrid);
        return cleared;
    }

    private void updateStepLabel() {
        tvStep.setText("Step " + currentStep + " / " + solution.size());
    }

    private void updateScoreLabel(int lines) {
        if (lines == 0) {
            tvScore.setText("Lines cleared: 0");
        } else {
            tvScore.setText("Lines cleared: " + lines + "  🔥");
        }
    }

    private String gridToString(boolean[][] g) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                sb.append(g[r][c] ? "1" : "0");
        return sb.toString();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        handler.removeCallbacksAndMessages(null);
    }
}
