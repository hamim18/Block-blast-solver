package com.blockblast.solver;

import java.util.ArrayList;
import java.util.List;

public class BlockBlastSolver {

    private static final int GRID_SIZE = 8;
    private boolean[][] grid;
    private List<Block> pieces;
    private List<PlacementStep> bestSolution;
    private int bestScore;
    private int bestLinesCleared;
    private int bestPiecesPlaced;

    public BlockBlastSolver(boolean[][] grid, List<Block> pieces) {
        this.grid = copyGrid(grid);
        this.pieces = pieces;
        this.bestSolution = null;
        this.bestScore = -1;
        this.bestLinesCleared = 0;
        this.bestPiecesPlaced = 0;
    }

    public List<PlacementStep> solve() {
        boolean[] used = new boolean[pieces.size()];
        List<PlacementStep> current = new ArrayList<>();
        backtrack(copyGrid(grid), used, current, 0, 0);
        return bestSolution;
    }

    /** Jumlah lines yang akan di-clear dari solusi terbaik */
    public int getBestLinesCleared() {
        return bestLinesCleared;
    }

    /** Berapa piece yang berhasil ditempatkan pada solusi terbaik */
    public int getBestPiecesPlaced() {
        return bestPiecesPlaced;
    }

    /** Total piece yang diminta */
    public int getTotalPieces() {
        return pieces.size();
    }

    private void backtrack(boolean[][] g, boolean[] used, List<PlacementStep> current,
                           int linesCleared, int piecesPlaced) {

        // Simpan sebagai kandidat terbaik jika lebih baik dari sebelumnya
        // Prioritas: (1) lebih banyak piece, (2) lebih banyak lines, (3) lebih banyak cell kosong
        int score = piecesPlaced * 1_000_000 + linesCleared * 1000 + scoreGrid(g);
        if (score > bestScore) {
            bestScore = score;
            bestSolution = new ArrayList<>(current);
            bestLinesCleared = linesCleared;
            bestPiecesPlaced = piecesPlaced;
        }

        // Semua piece sudah ditempatkan
        if (current.size() == pieces.size()) return;

        for (int pi = 0; pi < pieces.size(); pi++) {
            if (used[pi]) continue;
            Block piece = pieces.get(pi);
            used[pi] = true;

            for (int r = 0; r <= GRID_SIZE - piece.getRows(); r++) {
                for (int c = 0; c <= GRID_SIZE - piece.getCols(); c++) {
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
        int cleared = 0;
        for (int r = 0; r < GRID_SIZE; r++) {
            boolean full = true;
            for (int c = 0; c < GRID_SIZE; c++) if (!g[r][c]) { full = false; break; }
            if (full) { for (int c = 0; c < GRID_SIZE; c++) g[r][c] = false; cleared++; }
        }
        for (int c = 0; c < GRID_SIZE; c++) {
            boolean full = true;
            for (int r = 0; r < GRID_SIZE; r++) if (!g[r][c]) { full = false; break; }
            if (full) { for (int r = 0; r < GRID_SIZE; r++) g[r][c] = false; cleared++; }
        }
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
