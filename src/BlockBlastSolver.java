package com.blockblast.solver;

import java.util.ArrayList;
import java.util.List;

public class BlockBlastSolver {

    private static final int GRID_SIZE    = 8;
    private static final long TIMEOUT_MS  = 4000; // 4 detik max

    private boolean[][] grid;
    private List<Block> pieces;
    private List<PlacementStep> bestSolution;
    private int bestScore;
    private int bestLinesCleared;
    private int bestPiecesPlaced;
    private long startTime;
    private boolean timedOut;

    public BlockBlastSolver(boolean[][] grid, List<Block> pieces) {
        this.grid = copyGrid(grid);
        this.pieces = pieces;
        this.bestSolution = null;
        this.bestScore = -1;
        this.bestLinesCleared = 0;
        this.bestPiecesPlaced = 0;
        this.timedOut = false;
    }

    public List<PlacementStep> solve() {
        startTime = System.currentTimeMillis();
        boolean[] used = new boolean[pieces.size()];
        List<PlacementStep> current = new ArrayList<>();
        backtrack(copyGrid(grid), used, current, 0, 0);
        return bestSolution;
    }

    public int getBestLinesCleared() { return bestLinesCleared; }
    public int getBestPiecesPlaced() { return bestPiecesPlaced; }
    public int getTotalPieces()      { return pieces.size(); }
    public boolean isTimedOut()      { return timedOut; }

    private void backtrack(boolean[][] g, boolean[] used, List<PlacementStep> current,
                           int linesCleared, int piecesPlaced) {

        // Timeout guard
        if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
            timedOut = true;
            return;
        }

        // Simpan kandidat terbaik
        // Prioritas: (1) lebih banyak piece, (2) lebih banyak lines, (3) lebih banyak cell kosong
        int score = piecesPlaced * 1_000_000 + linesCleared * 1000 + scoreGrid(g);
        if (score > bestScore) {
            bestScore = score;
            bestSolution = new ArrayList<>(current);
            bestLinesCleared = linesCleared;
            bestPiecesPlaced = piecesPlaced;
        }

        if (current.size() == pieces.size()) return;

        // Pruning: hitung sisa empty cells, jika mustahil tempatkan piece apapun → potong
        if (!hasAnyValidPlacement(g, used)) return;

        for (int pi = 0; pi < pieces.size(); pi++) {
            if (used[pi] || timedOut) continue;
            Block piece = pieces.get(pi);
            used[pi] = true;

            for (int r = 0; r <= GRID_SIZE - piece.getRows(); r++) {
                for (int c = 0; c <= GRID_SIZE - piece.getCols(); c++) {
                    if (timedOut) break;
                    if (canPlace(g, piece, r, c)) {
                        boolean[][] newGrid = copyGrid(g);
                        place(newGrid, piece, r, c);
                        int cleared = clearLines(newGrid);

                        current.add(new PlacementStep(piece, pi, r, c));
                        backtrack(newGrid, used, current, linesCleared + cleared, piecesPlaced + 1);
                        current.remove(current.size() - 1);
                    }
                }
            }
            used[pi] = false;
        }
    }

    /** Pruning: cek apakah minimal satu piece yang belum dipakai bisa ditempatkan */
    private boolean hasAnyValidPlacement(boolean[][] g, boolean[] used) {
        for (int pi = 0; pi < pieces.size(); pi++) {
            if (used[pi]) continue;
            Block piece = pieces.get(pi);
            for (int r = 0; r <= GRID_SIZE - piece.getRows(); r++)
                for (int c = 0; c <= GRID_SIZE - piece.getCols(); c++)
                    if (canPlace(g, piece, r, c)) return true;
        }
        return false;
    }

    private boolean canPlace(boolean[][] g, Block piece, int row, int col) {
        boolean[][] shape = piece.getShape();
        for (int r = 0; r < piece.getRows(); r++)
            for (int c = 0; c < piece.getCols(); c++)
                if (shape[r][c] && g[row + r][col + c]) return false;
        return true;
    }

    private void place(boolean[][] g, Block piece, int row, int col) {
        boolean[][] shape = piece.getShape();
        for (int r = 0; r < piece.getRows(); r++)
            for (int c = 0; c < piece.getCols(); c++)
                if (shape[r][c]) g[row + r][col + c] = true;
    }

    private int clearLines(boolean[][] g) {
        // Evaluasi baris dan kolom yang penuh SERENTAK sebelum membersihkan apapun
        // agar sel yang menjadi bagian dari keduanya tetap terhitung benar
        boolean[] fullRow = new boolean[GRID_SIZE];
        boolean[] fullCol = new boolean[GRID_SIZE];
        int cleared = 0;

        for (int r = 0; r < GRID_SIZE; r++) {
            boolean full = true;
            for (int c = 0; c < GRID_SIZE; c++) if (!g[r][c]) { full = false; break; }
            fullRow[r] = full;
            if (full) cleared++;
        }
        for (int c = 0; c < GRID_SIZE; c++) {
            boolean full = true;
            for (int r = 0; r < GRID_SIZE; r++) if (!g[r][c]) { full = false; break; }
            fullCol[c] = full;
            if (full) cleared++;
        }
        // Bersihkan setelah evaluasi selesai
        for (int r = 0; r < GRID_SIZE; r++)
            if (fullRow[r]) for (int c = 0; c < GRID_SIZE; c++) g[r][c] = false;
        for (int c = 0; c < GRID_SIZE; c++)
            if (fullCol[c]) for (int r = 0; r < GRID_SIZE; r++) g[r][c] = false;

        return cleared;
    }

    private int scoreGrid(boolean[][] g) {
        int empty = 0;
        for (int r = 0; r < GRID_SIZE; r++)
            for (int c = 0; c < GRID_SIZE; c++)
                if (!g[r][c]) empty++;
        return empty;
    }

    private boolean[][] copyGrid(boolean[][] g) {
        boolean[][] copy = new boolean[GRID_SIZE][GRID_SIZE];
        for (int r = 0; r < GRID_SIZE; r++) copy[r] = g[r].clone();
        return copy;
    }
}
